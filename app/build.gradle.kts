plugins {
    alias(libs.plugins.wp.android.application)
    alias(libs.plugins.wp.android.compose)
    alias(libs.plugins.wp.hilt)
    alias(libs.plugins.wp.profile)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "app.weddingplanner"

    defaultConfig {
        applicationId = "app.weddingplanner"
        versionCode = 2
        versionName = "0.2.0-m1"
    }

    flavorDimensions += "env"
    productFlavors {
        create("dev") {
            dimension = "env"
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev"
        }
        create("prod") {
            dimension = "env"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
}

dependencies {
    implementation(projects.core.model)
    implementation(projects.core.designsystem)
    implementation(projects.core.datastore)
    implementation(projects.core.auth)
    implementation(projects.core.sheets)
    implementation(projects.core.data)
    implementation(projects.feature.onboarding)
    implementation(projects.feature.home)
    implementation(projects.feature.budget)
    implementation(projects.feature.vendor)
    "devImplementation"(projects.feature.sheetsetup)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.kotlinx.serialization.json)
}
