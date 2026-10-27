import app.weddingplanner.buildlogic.GenerateWeddingProfileTask
import app.weddingplanner.buildlogic.WeddingProfileJson
import app.weddingplanner.buildlogic.asLiteral
import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.register

/**
 * Reads the couple's `wedding-profile.json` and turns it into build-time defaults:
 * `BuildConfig.DEFAULT_SHEET_ID` / `GOOGLE_WEB_CLIENT_ID` per flavour, and a generated
 * `WeddingProfileDefaults` object. The JSON itself never ships in the APK (TRD §3.5).
 */
class WeddingProfileConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        val path = (findProperty("wp.profile") as String?) ?: "wedding-profile.json"
        val profileFile = rootProject.file(path)
        val profile = WeddingProfileJson.load(profileFile)
        profile.warnings.forEach { logger.warn("w: wedding-profile.json: $it") }

        extensions.configure<ApplicationExtension> {
            buildFeatures.buildConfig = true
            productFlavors.configureEach {
                val sheetId = if (name == "prod") profile.prodId else profile.devTestCopyId
                buildConfigField("String", "DEFAULT_SHEET_ID", sheetId.asLiteral())
                buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", profile.webClientId.asLiteral())
            }
        }

        extensions.configure<ApplicationAndroidComponentsExtension> {
            onVariants { variant ->
                val taskName = "generate${variant.name.replaceFirstChar { it.uppercase() }}WeddingProfile"
                val task = tasks.register<GenerateWeddingProfileTask>(taskName) {
                    this.profileFile.set(profileFile)
                    outputDir.set(layout.buildDirectory.dir("generated/wp-profile/${variant.name}"))
                }
                variant.sources.java?.addGeneratedSourceDirectory(task, GenerateWeddingProfileTask::outputDir)
            }
        }
    }
}
