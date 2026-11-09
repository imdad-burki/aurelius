plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.imdadburki.aurelius"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.imdadburki.aurelius"
        minSdk = 26
        targetSdk = 36
        versionCode = 12
        versionName = "2.0.8"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // YouTube Data API v3 key — default from gradle.properties (his own key, restricted
        // to YouTube Data API v3 only). Settings can override it at runtime. Never log it.
        val ytKey: String = project.findProperty("YOUTUBE_API_KEY") as String? ?: ""
        buildConfigField("String", "YOUTUBE_API_KEY", "\"$ytKey\"")
    }

    signingConfigs {
        create("release") {
            storeFile = rootProject.file("aurelius-release.keystore")
            storePassword = project.findProperty("AURELIUS_STORE_PASSWORD") as String?
            keyAlias = project.findProperty("AURELIUS_KEY_ALIAS") as String?
            keyPassword = project.findProperty("AURELIUS_KEY_PASSWORD") as String?
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            applicationIdSuffix = ".debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        jvmToolchain(17)
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }
}

dependencies {
    // Core + Compose
    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.activity:activity-compose:1.11.0")
    implementation(platform("androidx.compose:compose-bom:2026.09.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.4")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.4")
    implementation("androidx.navigation:navigation-compose:2.9.5")

    // Media3 — playback
    implementation("androidx.media3:media3-exoplayer:1.11.1")
    implementation("androidx.media3:media3-session:1.11.1")
    implementation("androidx.media3:media3-common:1.11.1")
    implementation("androidx.media3:media3-ui:1.11.1")

    // Room — playlists, favorites, history, queue
    implementation("androidx.room:room-runtime:2.8.5")
    implementation("androidx.room:room-ktx:2.8.5")
    ksp("androidx.room:room-compiler:2.8.5")

    // DataStore — settings
    implementation("androidx.datastore:datastore-preferences:1.1.7")

    // Coil — artwork loading (Coil 3: group id io.coil-kt.coil3).
    // coil-network-okhttp is REQUIRED: Coil 3.x ships NO network
    // support by default — without it every network image fails and
    // AsyncImage falls back to the placeholder forever. That is why
    // every YouTube thumbnail showed the gold logo in v2.0.0–v2.0.4
    // (proven on the Appetize emulator 2026-11-08): the URLs were
    // right, Coil simply could not fetch them.
    implementation("io.coil-kt.coil3:coil-compose:3.3.0")
    implementation("io.coil-kt.coil3:coil-network-okhttp:3.3.0")

    // Networking — YouTube Data API v3 + LRCLIB
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-kotlinx-serialization:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")

    // Tests
    testImplementation("junit:junit:4.13.2")
    testImplementation("io.mockk:mockk:1.14.4")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
    androidTestImplementation(platform("androidx.compose:compose-bom:2026.09.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
