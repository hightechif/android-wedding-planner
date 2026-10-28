package app.weddingplanner.core.auth

import android.os.Build

/**
 * Dev builds running on an emulator skip Google: "Masuk dengan Google" signs in as [EMAIL] and the
 * app reads the local sheet built from `dummy.xlsx` instead of the Sheets API.
 */
data class LocalDataMode(val enabled: Boolean) {
    companion object {
        const val EMAIL = "emulator@local.test"
        /** Long enough for SpreadsheetIdParser (≥ 20 characters of [A-Za-z0-9_-]). */
        const val SHEET_ID = "local-emulator-sheet-dummy-xlsx"
        const val SHEET_TITLE = "dummy.xlsx (data lokal emulator)"
    }
}

/** Heuristics for the Android Emulator (goldfish/ranchu kernels, SDK system images). */
object Emulator {
    fun isEmulator(): Boolean =
        Build.HARDWARE in setOf("goldfish", "ranchu") ||
            Build.FINGERPRINT.startsWith("generic") ||
            Build.FINGERPRINT.contains("emulator") ||
            Build.PRODUCT.contains("sdk_gphone") ||
            Build.PRODUCT.startsWith("sdk") ||
            Build.MODEL.contains("Android SDK built for") ||
            Build.MODEL.startsWith("sdk_gphone")
}
