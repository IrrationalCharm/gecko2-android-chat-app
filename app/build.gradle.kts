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

    implementation("com.github.NaikSoftware:StompProtocolAndroid:1.6.6")
// RxJava (Required by the library above)
    implementation("io.reactivex.rxjava2:rxjava:2.2.5")
    implementation("io.reactivex.rxjava2:rxandroid:2.1.0")

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.play.services.auth)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}