plugins { id("com.android.application") }
android {
 namespace = "br.com.retroplayer"
 compileSdk = 34
 defaultConfig { applicationId = "br.com.retroplayer"; minSdk = 26; targetSdk = 34; versionCode = 3; versionName = "1.2"; ndk { abiFilters += "arm64-v8a" } }
 compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
 packaging { jniLibs { useLegacyPackaging = true; keepDebugSymbols += "**/*.so" } }
}
