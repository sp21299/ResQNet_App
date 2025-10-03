plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android") version "2.2.20"
    id("org.jetbrains.kotlin.kapt")   // No version here
    id("com.google.gms.google-services")
}


android {
    namespace = "com.example.resqnet_app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.resqnet_app"
        minSdk = 24
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
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlin {
        jvmToolchain(21)
    }

    buildFeatures {
        viewBinding = true
    }

}

dependencies {
    // ---------- ROOM DATABASE ----------
//    val roomVersion = "2.8.1"




    // Core Room
    implementation(libs.androidx.room.runtime.v281)
    kapt(libs.androidx.room.compiler.v281)       // for annotation processing
    implementation(libs.androidx.room.ktx) // Kotlin extensions (Coroutines support)

    // Optional Room features
    implementation(libs.androidx.room.rxjava2)
    implementation(libs.androidx.room.rxjava3)
    implementation(libs.androidx.room.guava)
    implementation(libs.androidx.room.paging)
    testImplementation(libs.androidx.room.testing)


    // Your existing dependencies
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.firebase.auth)
    implementation(libs.credentials)
    implementation(libs.credentials.play.services.auth)
    implementation(libs.googleid)
    implementation(libs.firebase.database)
    implementation(libs.firebase.firestore)
    implementation(libs.annotation)
    implementation(libs.lifecycle.livedata.ktx)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    implementation(libs.play.services.maps)

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
