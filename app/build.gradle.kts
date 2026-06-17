plugins {
    alias(libs.plugins.android.application)
    // alias(libs.plugins.kotlin.android) // AGP 9.0+ 已內建支持，無需此插件
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.myTools"
    compileSdk = 36 // 建議保持與 targetSdk 一致或更高

    defaultConfig {
        applicationId = "com.example.ruler"
        minSdk = 31
        targetSdk = 36
        versionCode = 1
        versionName = "1.3"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        externalNativeBuild {
            cmake {
                cppFlags += ""
            }
        }
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

    // 在 AGP 9.0+ 中，若使用了內置 Kotlin 支持，
    // 通常不再需要顯式定義 kotlinOptions 或 compilerOptions，
    // 它會繼承 compileOptions 的設置。

    buildFeatures {
        compose = true
        buildConfig = true
    }
    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.foundation.layout)
    implementation(libs.androidx.compose.foundation)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // 1. 農曆算法庫
    implementation(libs.lunar)
    // 2. 擴展圖標庫
    implementation(libs.androidx.compose.material.icons.extended)
    // 3. Gson
    implementation(libs.gson)

    // 4. CarSpeed Location Services  定位服務
    implementation(libs.play.services.location)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
}
