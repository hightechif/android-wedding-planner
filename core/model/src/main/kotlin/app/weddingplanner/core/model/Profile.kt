package app.weddingplanner.core.model

import java.time.LocalDate

/** CPP = calon pengantin pria (groom), CPW = calon pengantin wanita (bride). */
enum class Side { CPP, CPW }

enum class Role { OWNER, EDITOR, VIEWER }

enum class EventType { LAMARAN, PRE_WEDDING, PENGAJIAN_SIRAMAN, WEDDING }

/** One row to seed into the `_APP_CONFIG` tab. [side] is null for a VIEWER (e.g. the WO). */
data class AccountSeed(
    val email: String,
    val side: Side?,
    val role: Role,
    val label: String? = null,
)

/**
 * Build-time defaults generated from `wedding-profile.json`.
 * The sheet stays authoritative at runtime (DASHBOARD dates, `_APP_CONFIG` accounts).
 */
data class WeddingProfile(
    val displayTitle: String,
    val cppName: String,
    val cpwName: String,
    val timeZone: String,
    val woName: String?,
    val events: Map<EventType, LocalDate>,
    val tabIds: Map<String, Long>,
    val accounts: List<AccountSeed>,
) {
    val weddingDate: LocalDate get() = events.getValue(EventType.WEDDING)
}
