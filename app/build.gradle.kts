plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.dominik.Gecko2Chat"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.dominik.gecko2_chat_app"
        minSdk = 34
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        manifestPlaceholders["appAuthRedirectScheme"] = "com.dominik.gecko2_chat_app"
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
}

dependencies {
    implementation("net.openid:appauth:0.11.1")
    implementation("com.squareup.retrofit2:retrofit:3.0.0")
    implementation("com.squareup.retrofit2:converter-gson:3.0.0")
    implementation("androidx.lifecycle:lifecycle-livedata:2.10.0")
    implementation("androidx.lifecycle:lifecycle-process:2.10.0")
    implementation("com.github.NaikSoftware:StompProtocolAndroid:1.6.6")
    implementation("com.github.LottieFiles:dotlottie-android:0.5.0")
    // RxJava required by the above library
    implementation("io.reactivex.rxjava2:rxjava:2.2.5")
    implementation("io.reactivex.rxjava2:rxandroid:2.1.0")
    implementation(libs.activity)
    implementation(libs.constraintlayout)

    val room_version = "2.8.4"
    implementation("androidx.room:room-runtime:$room_version")
    annotationProcessor("androidx.room:room-compiler:$room_version")
    implementation("androidx.room:room-rxjava2:$room_version")

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.play.services.auth)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}