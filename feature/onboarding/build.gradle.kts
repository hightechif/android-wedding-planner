plugins {
    alias(libs.plugins.wp.android.feature)
}

android {
    namespace = "app.weddingplanner.feature.onboarding"
}

dependencies {
    implementation(projects.core.auth)
    implementation(projects.core.datastore)
    implementation(projects.core.sheets)
    implementation(libs.androidx.activity.compose)
}
