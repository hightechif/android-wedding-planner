pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "wedding-planner"

include(":app")
include(":core:model")
include(":core:sheets")
include(":core:designsystem")
include(":core:datastore")
include(":core:database")
include(":core:data")
include(":core:auth")
include(":feature:onboarding")
include(":feature:home")
include(":feature:budget")
include(":feature:vendor")
include(":feature:sheetsetup")
