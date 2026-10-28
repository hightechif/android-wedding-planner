package app.weddingplanner.buildlogic

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

/** Copies the emulator's local data (an .xlsx export of the sheet) into the dev assets as `local-sheet/local.xlsx`. */
abstract class CopyLocalSheetTask : DefaultTask() {
    @get:InputFile
    @get:Optional
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val xlsxFile: RegularFileProperty

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun copy() {
        val out = outputDir.get().asFile.resolve("local-sheet").apply {
            deleteRecursively()
            mkdirs()
        }
        val xlsx = xlsxFile.orNull?.asFile?.takeIf { it.exists() }
        if (xlsx == null) {
            logger.warn("w: local sheet: no xlsx found; the emulator will have no local data")
            return
        }
        xlsx.copyTo(out.resolve("local.xlsx"))
    }
}
