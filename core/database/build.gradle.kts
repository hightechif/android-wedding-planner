plugins {
    alias(libs.plugins.wp.android.library)
    alias(libs.plugins.wp.hilt)
}

android {
    namespace = "app.weddingplanner.core.database"
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    api(projects.core.model)
    api(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
}
