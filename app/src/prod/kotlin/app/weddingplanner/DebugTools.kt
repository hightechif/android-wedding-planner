package app.weddingplanner.debug

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController

/** prod flavour: no debug tools; the migration screen isn't even compiled in. */
object DebugTools {
    val sheetSetupRoute: Any? = null

    @Suppress("UnusedReceiverParameter", "UNUSED_PARAMETER")
    fun NavGraphBuilder.debugScreens(navController: NavHostController) = Unit
}
