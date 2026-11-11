package br.com.retroplayer;

public class MusicIntegration {
  static int checks;
  static void check(boolean condition, String name) {
    if (!condition) throw new AssertionError(name);
    checks++;
  }
  public static void main(String[] args) {
    Lyrics l = new Lyrics("[ar:Artist]\n[offset:100]\n[00:01.5][00:03.050]Refrão\n[00:02]Verso\n[00:04.123]\n[00:05]<00:05.00>Palavra <00:05.50>final");
    check(l.lines.size() == 5, "Repeated timestamps and instrumental marker");
    check(l.at(1399) == -1, "Before first line");
    check(l.lines.get(0).time == 1400, "Tenths and offset");
    check(l.at(1900) == 1 && l.lines.get(1).text.equals("Verso"), "Sorted and exact boundary");
    check(l.lines.get(2).time == 2950, "Milliseconds");
    check(l.lines.get(3).text.isEmpty(), "Instrumental clears previous line");
    check(l.lines.get(4).text.equals("Palavra final"), "Enhanced word tags stripped");
    check(new Lyrics("[offset:99999999999999999999999999]\n[00:01]Oi").lines.get(0).time == 1000, "Malformed offset ignored");
    check(new Lyrics("Letra sem tempo").plain.equals("Letra sem tempo"), "Plain lyrics");
    check(new Lyrics("[00:00.01]Oi").lines.get(0).time == 10, "Hundredths");
    check(new Lyrics("[00:00.001]Oi").lines.get(0).time == 1, "Milliseconds preserved");
    check(new Lyrics("[offset:1000]\n[00:00.5]Oi").lines.get(0).time == 0, "Negative clamped");
    check(YouTubeLink.id("https://music.youtube.com/watch?v=dQw4w9WgXcQ&list=abc").equals("dQw4w9WgXcQ"), "Music link");
    check(YouTubeLink.id("https://youtu.be/dQw4w9WgXcQ?t=30").equals("dQw4w9WgXcQ"), "Short link");
    check(YouTubeLink.id("https://www.youtube.com/shorts/dQw4w9WgXcQ").equals("dQw4w9WgXcQ"), "Shorts link");
    check(YouTubeLink.id("https://youtube.com.evil.test/watch?v=dQw4w9WgXcQ").isEmpty(), "Lookalike rejected");
    check(YouTubeLink.id("https://youtube.com@evil.test/watch?v=dQw4w9WgXcQ").isEmpty(), "Userinfo rejected");
    check(YouTubeLink.id("https://youtube.com/watch?v=bad").isEmpty(), "Invalid ID rejected");
    check(YouTubeLink.id("http://youtube.com/watch?v=dQw4w9WgXcQ").isEmpty(), "HTTPS required");
    System.out.println("PASS: " + checks + " LRC, timing and YouTube link checks");
  }
}
