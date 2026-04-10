// This is the app-level build file — it lists all the libraries we use.
// Think of it like a Python requirements.txt, but with more config options.
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.localllm"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.localllm"
        minSdk = 26         // Android 8.0 — covers almost all modern phones
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildFeatures {
        // ViewBinding = auto-generates type-safe handles to every UI element by ID
        // So instead of findViewById(R.id.btnSend), we write binding.btnSend
        viewBinding = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.activity:activity-ktx:1.9.3")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")

    // This is the key library — Google's on-device AI inference engine.
    // It handles all the heavy math (matrix multiplications, quantization, GPU/CPU dispatch).
    // We just call its simple API, like calling a Python function.
    implementation("com.google.mediapipe:tasks-genai:0.10.22")
}
