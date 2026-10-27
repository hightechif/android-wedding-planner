plugins {
    alias(libs.plugins.wp.android.feature)
}

android {
    namespace = "app.weddingplanner.feature.vendor"
}

dependencies {
    implementation(projects.core.data)
}
