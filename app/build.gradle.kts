plugins {
    alias(libs.plugins.android.application)
    // ADD THIS LINE
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.dietplanner"
    // Note: I corrected the syntax for compileSdk and targetSdk
    // Usually these are just numbers (e.g., 34 or 35).
    // Ensure "36" is supported by your current SDK manager.
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.dietplanner"
        minSdk = 24
        targetSdk = 34
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
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation("com.android.volley:volley:1.2.1")
    implementation("androidx.biometric:biometric:1.1.0")

    // FIREBASE SECTION
    // Import the Firebase BoM
    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
    // No need for version numbers when using the BoM
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-database")

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}