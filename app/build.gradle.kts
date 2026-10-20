
plugins {
    id("com.android.application")
    
}

android {
    namespace = "com.example.myapplication"
    compileSdk = 36
    
    defaultConfig {
        applicationId = "com.example.myapplication"
        minSdk = 31
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        
        vectorDrawables { 
            useSupportLibrary = true
        }
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    buildFeatures {
        viewBinding = true
        
    }
    
}

dependencies {


    implementation("androidx.constraintlayout:constraintlayout:2.2.1")
    implementation("androidx.core:core:1.17.0")
    implementation("com.google.android.material:material:1.13.0")
    implementation("androidx.annotation:annotation:1.9.1")
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("androidx.gridlayout:gridlayout:1.0.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.google.code.gson:gson:2.11.0")
    implementation("androidx.preference:preference:1.2.1")   // for settings later
}
