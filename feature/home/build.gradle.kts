plugins {
    alias(libs.plugins.wp.android.feature)
}

android {
    namespace = "app.weddingplanner.feature.home"
}

dependencies {
    implementation(projects.core.auth)
    implementation(projects.core.datastore)
    implementation(projects.core.data)
}
