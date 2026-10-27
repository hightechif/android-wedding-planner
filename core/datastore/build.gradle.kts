plugins {
    alias(libs.plugins.wp.android.library)
    alias(libs.plugins.wp.hilt)
}

android {
    namespace = "app.weddingplanner.core.datastore"
}

dependencies {
    api(projects.core.model)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.android)
}
