package br.com.retroplayer;
import java.io.*;
import java.net.*;
import java.net.http.*;
import java.nio.file.*;
import java.util.*;
import org.json.*;

public class RoomFeaturesIntegration {
  static int checks;
  static void check(boolean value,String name){checks++;if(!value)throw new AssertionError(name);}
  static HttpClient client=HttpClient.newBuilder().build();
  static HttpResponse<byte[]> post(String base,String endpoint,String type,byte[] body,String... extra)throws Exception{
    HttpRequest.Builder b=HttpRequest.newBuilder(URI.create(base+endpoint)).header("Content-Type",type);
    for(int i=0;i<extra.length;i+=2)b.header(extra[i],extra[i+1]);
    return client.send(b.POST(HttpRequest.BodyPublishers.ofByteArray(body)).build(),HttpResponse.BodyHandlers.ofByteArray());
  }
  public static void main(String[] args)throws Exception{
    String room="https://example.trycloudflare.com/0123456789abcdef0123456789abcdef/";
    check(RoomLink.parse(room).equals(room),"HTTPS room accepted");
    check(RoomLink.parse("retroplayer://room?url="+URLEncoder.encode(room,"UTF-8")).equals(room),"Native app deep link");
    check(RoomLink.parse("http://example.com/0123456789abcdef0123456789abcdef/").isEmpty(),"Insecure invite rejected");
    check(RoomLink.parse("https://example.com/anything/").isEmpty(),"Invalid room token rejected");
    check(RoomLink.media("https://music.youtube.com/watch?v=dQw4w9WgXcQ").contains("dQw4w9WgXcQ"),"YouTube contribution");
    check(RoomLink.media("https://127.0.0.1/song.mp3").isEmpty(),"Loopback link rejected");
    check(RoomLink.media("https://192.168.1.1/song.mp3").isEmpty(),"Private address rejected");
    check(RoomLink.media("https://example.com/login").isEmpty(),"Nonmedia link rejected");
    check(RoomUploads.name("../../song.mp3").equals("song.mp3"),"Upload filename sanitized");
    check(RoomUploads.name("page.html").isEmpty(),"Nonmedia upload rejected");
    PcmBroadcast pcm=new PcmBroadcast();
    boolean[] slowClosed={false};
    PcmBroadcast.Listener first=pcm.subscribe(()->slowClosed[0]=true);
    for(int i=0;i<25;i++)pcm.publish(new byte[]{1,2,3,4});
    check(slowClosed[0]&&!first.open.get(),"Slow live listener disconnected at bounded backlog");
    PcmBroadcast.Listener[] listeners=new PcmBroadcast.Listener[4];
    for(int i=0;i<4;i++)listeners[i]=pcm.subscribe(()->{});
    check(pcm.subscribe(()->{})==null,"Live listener cap");
    pcm.publish(new byte[]{1,2,3,4});
    check(Arrays.equals(listeners[0].next(),new byte[]{1,2,3,4}),"PCM delivered intact");
    pcm.close();check(!listeners[0].open.get()&&pcm.subscribe(()->{})==null,"Capture close revokes listeners");
    File fixture=new File("tests/room-fixture.bin");Files.write(fixture.toPath(),new byte[]{1,2,3,4});
    PlayerService s=new PlayerService(fixture,new File("app/src/main/assets"));RoomServer server=new RoomServer(s);server.start();
    String base="http://127.0.0.1:"+server.port+"/"+s.token+"/";
    try{
      byte[] link="{\"url\":\"https://music.youtube.com/watch?v=dQw4w9WgXcQ\",\"title\":\"Música do convidado\",\"name\":\"Amigo\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8);
      check(post(base,"queue","application/json",link).statusCode()==202,"Queue accepts YouTube contribution");
      check(s.lastLink.contains("dQw4w9WgXcQ"),"Contribution reaches service");
      HttpResponse<String> queue=client.send(HttpRequest.newBuilder(URI.create(base+"queue")).GET().build(),HttpResponse.BodyHandlers.ofString());
      check(new JSONArray(queue.body()).length()==1,"Shared queue readable");
      check(post(base,"queue","application/json",link,"Origin","https://evil.example").statusCode()==403,"Queue rejects cross origin");
      check(post(base,"queue","application/json","{\"url\":\"https://example.com/login\"}".getBytes()).statusCode()==400,"Server rejects nonmedia link");
      byte[] audio=new byte[32000];new Random(2).nextBytes(audio);
      check(post(base,"upload","application/octet-stream",audio,"X-File-Name","song.mp3","X-Guest-Name","Amigo").statusCode()==202,"Local file accepted");
      check(s.lastUpload!=null&&Arrays.equals(Files.readAllBytes(s.lastUpload.toPath()),audio),"Uploaded bytes persisted intact");
      check(post(base,"upload","application/octet-stream",audio,"X-File-Name","page.html").statusCode()==413,"Invalid file type rejected over HTTP");
      check(post(base,"upload","application/octet-stream",audio,"X-File-Name","song.mp3","Origin","https://evil.example").statusCode()==403,"Upload rejects cross origin");
      check(s.uploads.active==0&&s.uploads.reserved==0,"Upload reservation released");
      check(client.send(HttpRequest.newBuilder(URI.create(base+"live")).GET().build(),HttpResponse.BodyHandlers.ofByteArray()).statusCode()==404,"Inactive capture not exposed");
      s.capture=new PcmBroadcast();
      HttpResponse<InputStream> live=client.send(HttpRequest.newBuilder(URI.create(base+"live")).GET().build(),HttpResponse.BodyHandlers.ofInputStream());
      check(live.statusCode()==200,"Live endpoint opens");
      byte[] packet={0,1,2,3,4,5,6,7};s.capture.publish(packet);
      check(Arrays.equals(live.body().readNBytes(8),packet),"HTTP live PCM stream intact");
      s.capture.close();live.body().close();
      s.room=false;check(post(base,"queue","application/json",link).statusCode()==404,"Closed room rejects contributions");
      check(post(base,"upload","application/octet-stream",audio,"X-File-Name","song.mp3").statusCode()==404,"Closed room rejects uploads");
      System.out.println("PASS: "+checks+" room links, collaborative queue, uploads and live audio checks");
    }finally{server.close();if(s.capture!=null)s.capture.close();if(s.lastUpload!=null)s.lastUpload.delete();fixture.delete();}
  }
}
