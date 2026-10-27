plugins {
    alias(libs.plugins.wp.android.feature)
}

android {
    namespace = "app.weddingplanner.feature.budget"
}

dependencies {
    implementation(projects.core.data)
}
