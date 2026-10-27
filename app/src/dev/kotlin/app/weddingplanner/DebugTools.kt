package app.weddingplanner.debug

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import app.weddingplanner.feature.sheetsetup.SheetSetupRoute
import app.weddingplanner.feature.sheetsetup.sheetSetupScreen

/** dev flavour: exposes the "Penyiapan Sheet" migration screen. */
object DebugTools {
    val sheetSetupRoute: Any? = SheetSetupRoute

    fun NavGraphBuilder.debugScreens(navController: NavHostController) {
        sheetSetupScreen(onBack = { navController.popBackStack() })
    }
}
