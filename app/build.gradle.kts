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
    implementation(libs.appauth)
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.lifecycle.livedata)
    implementation(libs.lifecycle.process)
    implementation(libs.stompprotocolandroid)
    // RxJava required by the above library
    implementation(libs.rxjava)
    implementation(libs.rxandroid)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.blurview)
    implementation(libs.tink.android)

    implementation(libs.glide)
    annotationProcessor(libs.compiler)

    implementation(libs.room.runtime)
    annotationProcessor(libs.room.compiler)
    implementation(libs.room.rxjava2)

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.play.services.auth)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}