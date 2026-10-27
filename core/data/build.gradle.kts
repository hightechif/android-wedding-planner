plugins {
    alias(libs.plugins.wp.android.library)
    alias(libs.plugins.wp.hilt)
}

android {
    namespace = "app.weddingplanner.core.data"
}

dependencies {
    api(projects.core.model)
    api(projects.core.database)
    implementation(projects.core.sheets)
    implementation(projects.core.auth)
    implementation(projects.core.datastore)
    implementation(libs.kotlinx.coroutines.android)
}
