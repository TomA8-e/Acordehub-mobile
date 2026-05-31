import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.inputStream().use { load(it) }
    }
}

android {
    namespace = "com.example.acordehub"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.acordehub"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        manifestPlaceholders["redirectSchemeName"] = "acordehub"
        manifestPlaceholders["redirectHostName"] = "callback"

        resValue("string", "spotify_client_id", localProperties.getProperty("spotify.clientId", ""))
        resValue("string", "spotify_client_secret", localProperties.getProperty("spotify.clientSecret", ""))
    }

    buildFeatures {
        viewBinding = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {
    // Firebase BOM
    implementation(platform("com.google.firebase:firebase-bom:33.1.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-storage")   // ← nuevo para fotos

    // Google Sign-In
    implementation("com.google.android.gms:play-services-auth:21.2.0")

    // Material Design 3
    implementation(libs.material)

    // ViewModel + LiveData
    implementation("androidx.lifecycle:lifecycle-viewmodel:2.8.3")
    implementation("androidx.lifecycle:lifecycle-livedata:2.8.3")

    // Navigation Component
    implementation("androidx.navigation:navigation-fragment:2.7.7")
    implementation("androidx.navigation:navigation-ui:2.7.7")

    // Glide (cargar imágenes desde URL)
    implementation("com.github.bumptech.glide:glide:4.16.0")

    // Spotify Auth SDK
    implementation("com.spotify.android:auth:2.1.0")

    // Retrofit para API de Spotify
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.activity)
}
