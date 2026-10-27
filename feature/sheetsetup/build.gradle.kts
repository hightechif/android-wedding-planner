plugins {
    alias(libs.plugins.wp.android.feature)
}

android {
    namespace = "app.weddingplanner.feature.sheetsetup"
}

dependencies {
    implementation(projects.core.datastore)
    implementation(projects.core.sheets)
}
