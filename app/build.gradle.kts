plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "edu.hitsz.aircraftwar"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "edu.hitsz.aircraftwar"
        minSdk = 30
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        // 三页布局全走 ViewBinding：XML 控件与 Fragment 字段一一对应，findViewById 一处不写
        viewBinding = true
    }
}

dependencies {
    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.material)
    // 单 Activity + Navigation：三页流转与回退栈交给 NavController（AGENTS §2 决策 9）
    implementation(libs.navigation.fragment)
    testImplementation(libs.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
}