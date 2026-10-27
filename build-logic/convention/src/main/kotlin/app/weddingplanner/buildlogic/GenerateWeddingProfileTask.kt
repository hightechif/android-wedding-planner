package app.weddingplanner.buildlogic

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

@CacheableTask
abstract class GenerateWeddingProfileTask : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val profileFile: RegularFileProperty

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun generate() {
        val p = WeddingProfileJson.load(profileFile.get().asFile)
        val dir = outputDir.get().asFile.resolve("app/weddingplanner/profile").apply {
            deleteRecursively()
            mkdirs()
        }
        val events = p.events.entries.joinToString("") { (k, v) ->
            "            EventType.$k to LocalDate.parse(${v.asLiteral()}),\n"
        }
        val tabs = p.tabIds.entries.joinToString("") { (k, v) -> "            ${k.asLiteral()} to ${v}L,\n" }
        val accounts = p.accounts.joinToString("") { a ->
            val side = a.side?.let { "Side.$it" } ?: "null"
            val label = a.label?.asLiteral() ?: "null"
            "            AccountSeed(email = ${a.email.asLiteral()}, side = $side, role = Role.${a.role}, label = $label),\n"
        }
        dir.resolve("WeddingProfileDefaults.kt").writeText(
            """
            |// Generated from wedding-profile.json by GenerateWeddingProfileTask. Do not edit.
            |package app.weddingplanner.profile
            |
            |import app.weddingplanner.core.model.AccountSeed
            |import app.weddingplanner.core.model.EventType
            |import app.weddingplanner.core.model.Role
            |import app.weddingplanner.core.model.Side
            |import app.weddingplanner.core.model.WeddingProfile
            |import java.time.LocalDate
            |
            |object WeddingProfileDefaults {
            |    val profile = WeddingProfile(
            |        displayTitle = ${p.displayTitle.asLiteral()},
            |        cppName = ${p.cppName.asLiteral()},
            |        cpwName = ${p.cpwName.asLiteral()},
            |        timeZone = ${p.timeZone.asLiteral()},
            |        woName = ${p.woName?.asLiteral() ?: "null"},
            |        events = mapOf(
            $events            |        ),
            |        tabIds = mapOf(
            $tabs            |        ),
            |        accounts = listOf(
            $accounts            |        ),
            |    )
            |}
            |""".trimMargin()
                // Interpolated entries keep the raw-string indentation; normalise them to 12 spaces.
                .lines().joinToString("\n") { if (it.startsWith(" ".repeat(24))) it.substring(12) else it },
        )
    }
}
