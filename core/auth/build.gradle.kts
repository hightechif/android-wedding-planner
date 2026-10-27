plugins {
    alias(libs.plugins.wp.android.library)
    alias(libs.plugins.wp.hilt)
}

android {
    namespace = "app.weddingplanner.core.auth"
}

dependencies {
    api(projects.core.sheets)
    implementation(projects.core.datastore)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)
    implementation(libs.play.services.auth)
    implementation(libs.kotlinx.coroutines.android)
}
