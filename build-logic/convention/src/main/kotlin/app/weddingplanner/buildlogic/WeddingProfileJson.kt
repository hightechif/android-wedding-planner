package app.weddingplanner.buildlogic

import groovy.json.JsonSlurper
import org.gradle.api.GradleException
import java.io.File

/**
 * Parsed, validated view of `wedding-profile.json` (schema: docs/05-DATA-SCHEMA.md §7).
 * Required keys fail the build; optional-but-useful keys only produce warnings.
 */
data class WeddingProfileJson(
    val displayTitle: String,
    val cppName: String,
    val cpwName: String,
    val timeZone: String,
    val woName: String?,
    val events: Map<String, String>,
    val prodId: String,
    val devTestCopyId: String,
    val webClientId: String,
    val tabIds: Map<String, Long>,
    val accounts: List<Account>,
    val warnings: List<String>,
) {
    data class Account(val email: String, val side: String?, val role: String, val label: String?)

    companion object {
        private val EVENT_KEYS = mapOf(
            "lamaran" to "LAMARAN",
            "preWedding" to "PRE_WEDDING",
            "pengajianSiraman" to "PENGAJIAN_SIRAMAN",
            "wedding" to "WEDDING",
        )
        private val ISO_DATE = Regex("""\d{4}-\d{2}-\d{2}""")

        fun load(file: File): WeddingProfileJson {
            if (!file.exists()) {
                throw GradleException(
                    "Personal profile not found: ${file.path}. Create it as described in docs/05-DATA-SCHEMA.md §7, " +
                        "or point -Pwp.profile=<path> at it.",
                )
            }
            @Suppress("UNCHECKED_CAST")
            val root = try {
                JsonSlurper().parse(file) as Map<String, Any?>
            } catch (e: Exception) {
                throw GradleException("wedding-profile.json is not valid JSON: ${e.message}", e)
            }
            val warnings = mutableListOf<String>()

            fun node(path: String): Any? = path.split('.').fold(root as Any?) { acc, key -> (acc as? Map<*, *>)?.get(key) }
            fun required(path: String): String =
                (node(path) as? String)?.takeIf { it.isNotBlank() }
                    ?: throw GradleException("wedding-profile.json: required key `$path` is missing or empty.")
            fun optional(path: String): String {
                val value = (node(path) as? String).orEmpty()
                if (value.isBlank()) warnings += "`$path` is empty"
                return value
            }

            val events = EVENT_KEYS.mapNotNull { (key, enumName) ->
                val value = node("events.$key") as? String ?: return@mapNotNull null
                if (!ISO_DATE.matches(value)) throw GradleException("wedding-profile.json: `events.$key` must be yyyy-MM-dd, was `$value`.")
                enumName to value
            }.toMap()
            if ("WEDDING" !in events) throw GradleException("wedding-profile.json: required key `events.wedding` is missing.")

            val tabIds = (node("spreadsheet.tabs") as? Map<*, *>).orEmpty()
                .filterKeys { it is String && !it.startsWith("_") }
                .mapNotNull { (k, v) -> (v as? Number)?.let { k as String to it.toLong() } }
                .toMap()

            val accounts = (node("accounts") as? List<*>).orEmpty().mapIndexed { i, raw ->
                val map = raw as? Map<*, *> ?: throw GradleException("wedding-profile.json: `accounts[$i]` must be an object.")
                val role = map["role"] as? String
                if (role !in setOf("OWNER", "EDITOR", "VIEWER")) {
                    throw GradleException("wedding-profile.json: `accounts[$i].role` must be OWNER, EDITOR or VIEWER.")
                }
                val side = map["side"] as? String
                if (side != null && side !in setOf("CPP", "CPW")) {
                    throw GradleException("wedding-profile.json: `accounts[$i].side` must be CPP, CPW or null.")
                }
                val email = (map["email"] as? String).orEmpty().trim().lowercase()
                if (email.isBlank()) warnings += "`accounts[$i].email` is empty (it won't be seeded into _APP_CONFIG)"
                Account(email, side, role!!, map["label"] as? String)
            }

            return WeddingProfileJson(
                displayTitle = required("profile.displayTitle"),
                cppName = required("profile.cpp.name"),
                cpwName = required("profile.cpw.name"),
                timeZone = required("profile.timeZone"),
                woName = (node("people.wo.name") as? String)?.takeIf { it.isNotBlank() },
                events = events,
                prodId = required("spreadsheet.prodId"),
                devTestCopyId = optional("spreadsheet.devTestCopyId"),
                webClientId = optional("google.webClientId"),
                tabIds = tabIds,
                accounts = accounts,
                warnings = warnings,
            )
        }
    }
}

/** Kotlin/Java string literal with escaping, for generated code and BuildConfig fields. */
fun String.asLiteral(): String = buildString {
    append('"')
    for (c in this@asLiteral) {
        when (c) {
            '\\' -> append("\\\\")
            '"' -> append("\\\"")
            '$' -> append("\\$")
            '\n' -> append("\\n")
            else -> append(c)
        }
    }
    append('"')
}
