plugins {
    alias(libs.plugins.wp.android.library)
    alias(libs.plugins.wp.android.compose)
}

android {
    namespace = "app.weddingplanner.core.designsystem"
}

dependencies {
    api(libs.androidx.compose.material.icons.extended)
}
