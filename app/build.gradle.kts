plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt) // ✅ Add this
}

android {
    namespace = "com.aidbud"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.aidbud"
        minSdk = 31
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        mlModelBinding = true
    }

    ksp {
        arg("room.schemaLocation", "$projectDir/schemas")
    }
}

dependencies {
    // Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // Compose libraries (explicit versions now, no BOM)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.coil.compose)
    implementation(libs.androidx.compose.foundation)


    // Room
    implementation(libs.androidx.room.runtime) // Note the dot notation for version catalogs
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.room.ktx)

    // CameraX
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.androidx.camera.extensions)
    implementation(libs.androidx.camera.video)

    // Lifecycle
    implementation(libs.androidx.lifecycle.viewmodel.ktx)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // Data
    implementation(libs.gson)
    implementation(libs.androidx.datastore)
    implementation(libs.androidx.datastore.preferences)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler) // Hilt's annotation processor
    implementation(libs.androidx.hilt.navigation.compose) // For hiltViewModel()

    // Google AI Edge
    implementation(libs.google.mediapipe.tasks.vision)
    implementation(libs.google.mediapipe.tasks.text)
    implementation(libs.google.mediapipe.tasks.audio)
    implementation(libs.google.mediapipe.tasks)
    implementation(libs.google.localagents)
    implementation(libs.google.guava)

    // Misc
    implementation(libs.kotlinx.guava)
    implementation(libs.jna)
    implementation(libs.vosk.android)

    // Tests
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)

    // Debug tools
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}