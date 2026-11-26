plugins {
 id("com.android.application") version "8.7.3"
 id("org.jetbrains.kotlin.android") version "2.0.21"
}
android {
 namespace = "dev.air2023.gazeg2"
 compileSdk = 35
 defaultConfig { applicationId = "dev.air2023.gazeg2"; minSdk = 31; targetSdk = 33; versionCode = 3; versionName = "1.2-G2R1"; ndk { abiFilters += "arm64-v8a" } }
 compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
 kotlinOptions { jvmTarget = "17" }
}
dependencies {
 implementation("androidx.core:core-ktx:1.15.0")
 implementation("androidx.lifecycle:lifecycle-service:2.8.7")
 implementation("androidx.camera:camera-core:1.4.1")
 implementation("androidx.camera:camera-camera2:1.4.1")
 implementation("androidx.camera:camera-lifecycle:1.4.1")
 implementation("com.google.mediapipe:tasks-vision:0.10.14")
 testImplementation("junit:junit:4.13.2")
}
