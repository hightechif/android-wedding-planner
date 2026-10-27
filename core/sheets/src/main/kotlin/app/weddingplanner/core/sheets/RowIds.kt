package app.weddingplanner.core.sheets

import java.security.SecureRandom
import kotlin.random.Random
import kotlin.random.asKotlinRandom

/** Row IDs such as `BUD-7K2M9QXA`: a table prefix plus 8 Crockford base32 characters (TRD §5.3). */
object RowIds {
    private const val ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ"
    val PATTERN = Regex("^[A-Z]{3}-[0-9A-HJKMNP-TV-Z]{8}$")

    fun isValid(text: String): Boolean = PATTERN.matches(text)

    /** Generates an ID not contained in [taken]. */
    fun generate(prefix: String, taken: Set<String>, random: Random = secureRandom): String {
        require(prefix.length == 3 && prefix.all { it in 'A'..'Z' }) { "Prefix must be 3 letters: $prefix" }
        while (true) {
            val id = prefix + "-" + (1..8).map { ALPHABET[random.nextInt(ALPHABET.length)] }.joinToString("")
            if (id !in taken) return id
        }
    }

    private val secureRandom: Random = SecureRandom().asKotlinRandom()
}

/** Accepts a raw spreadsheet ID or a full `docs.google.com/spreadsheets/d/<id>/…` URL. */
object SpreadsheetIdParser {
    private val URL_ID = Regex("""/spreadsheets/d/([a-zA-Z0-9_-]{20,})""")
    private val RAW_ID = Regex("""^[a-zA-Z0-9_-]{20,}$""")

    fun parse(input: String): String? {
        val trimmed = input.trim()
        URL_ID.find(trimmed)?.let { return it.groupValues[1] }
        return trimmed.takeIf { RAW_ID.matches(it) }
    }
}
