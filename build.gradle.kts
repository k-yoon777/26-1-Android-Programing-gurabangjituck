plugins {
    id("com.android.application")

    // 🔥 [이 한 줄을 추가하세요!]
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.example.gurabangjituck"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.example.gurabangjituck"
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)

    implementation("com.google.android.gms:play-services-location:21.0.1") //260518 직접 추가

    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.cardview:cardview:1.0.0")


    val composeBom = platform("androidx.compose:compose-bom:2024.02.02") // 안정적인 컴포즈 버전 묶음팩
    implementation(composeBom)

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3") // 세련된 UI를 위한 Material 3
    implementation("androidx.activity:activity-compose:1.8.2") // Activity와 컴포즈 연결 다리


    // 🔥 [네이버 지도 Compose 전용 라이브러리 추가]
    implementation("com.naver.maps:map-sdk:3.23.2")
    implementation("io.github.fornewid:naver-map-compose:1.8.0")

    // 🔥 [화면 분리 및 이동을 위한 컴포즈 내비게이션 추가!]
    implementation("androidx.navigation:navigation-compose:2.8.5")

    // 아이콘 적용을 위한 추가 (2026.06.09)
    implementation("androidx.compose.material:material-icons-extended")

    // 🔥 [Gemini API 사용을 위한 라이브러리 추가]
    implementation("com.google.ai.client.generativeai:generativeai:0.9.0")
}