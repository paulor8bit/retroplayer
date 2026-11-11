package br.com.retroplayer;
import android.content.res.AssetFileDescriptor;
import java.io.*;
import java.util.*;
import org.json.*;
public class PlayerService {
  public String token = "secret-session", state = "{\"playing\":true}", url = "";
  public boolean room = true;
  PcmBroadcast capture;
  String sharedQueue="[]";
  RoomUploads uploads;
  File lastUpload;
  String lastLink="";
  int index = 0;
  long generation = 1;
  public final ArrayList<Track> tracks = new ArrayList<>();
  final ArrayList<JSONObject> messages = new ArrayList<>();
  final File media, assets;
  public PlayerService(File f, File a) {
    media = f;
    uploads=new RoomUploads(new File("tests/upload-fixture"));
    assets = a;
    tracks.add(new Track(new MediaUri("file://fixture"), "fixture", "audio/mpeg", f.length()));
  }
  static class MediaUri {
    String value;
    MediaUri(String s) {
      value = s;
    }
    public String getScheme() {
      return value.startsWith("https:") ? "https" : "file";
    }
    public String toString() {
      return value;
    }
  }
  static class Track {
    MediaUri uri;
    String name, mime;
    long size;
    Track(MediaUri u, String n, String m, long s) {
      uri = u;
      name = n;
      mime = m;
      size = s;
    }
  }
  public PlayerService getContentResolver() {
    return this;
  }
  public AssetFileDescriptor openAssetFileDescriptor(MediaUri u, String mode) {
    return new AssetFileDescriptor(media);
  }
  public PlayerService getAssets() {
    return this;
  }
  public InputStream open(String name) throws IOException {
    return new FileInputStream(new File(assets, name));
  }
  public InputStream openThemeAsset(String name) throws IOException {
    return new FileInputStream(name.equals("cover.png")
            ? new File("app/src/main/res/drawable-nodpi/album_art.png")
            : new File(assets, "theme/" + name));
  }
  boolean enqueueRoomLink(String link,String title,String author,String roomToken){
    String valid=RoomLink.media(link);
    if(valid.isEmpty()||!room||!token.equals(roomToken))return false;
    lastLink=valid;
    try{sharedQueue=new JSONArray(sharedQueue).put(new JSONObject().put("name",title).put("author",author)).toString();}catch(Exception e){throw new RuntimeException(e);}
    return true;
  }
  void acceptUpload(File file,String name,String author,String roomToken){if(room&&token.equals(roomToken))lastUpload=file;else file.delete();}
  public synchronized void chat(String name, String text) {
    try {
      messages.add(new JSONObject().put("name", name).put("text", text));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
  public synchronized String chatJson() {
    return new JSONArray(messages).toString();
  }
}
