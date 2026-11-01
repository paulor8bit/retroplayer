#!/bin/sh
set -eu
cd "$(dirname "$0")/.."
mkdir -p tests/classes
TEST_JSON_JAR="${TEST_JSON_JAR:-tools/test-json.jar}"
if [ ! -f "$TEST_JSON_JAR" ]; then
  printf '%s\n' 'Set TEST_JSON_JAR to an org.json JAR (org.json:json:20240303).'
  exit 1
fi
javac -cp "$TEST_JSON_JAR" -d tests/classes tests/stubs/android/content/res/AssetFileDescriptor.java tests/stubs/br/com/retroplayer/PlayerService.java app/src/main/java/br/com/retroplayer/Io.java app/src/main/java/br/com/retroplayer/RoomServer.java tests/stubs/br/com/retroplayer/ServerIntegration.java
java -cp "tests/classes:$TEST_JSON_JAR" br.com.retroplayer.ServerIntegration
node tests/sync-test.js
javac -cp "$TEST_JSON_JAR" -d tests/classes app/src/main/java/br/com/retroplayer/MediaFiles.java app/src/main/java/br/com/retroplayer/BookmarkStore.java tests/stubs/br/com/retroplayer/LibraryIntegration.java
java -cp "tests/classes:$TEST_JSON_JAR" br.com.retroplayer.LibraryIntegration
