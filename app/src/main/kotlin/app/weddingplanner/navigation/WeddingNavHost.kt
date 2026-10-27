package app.weddingplanner.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import app.weddingplanner.debug.DebugTools
import app.weddingplanner.feature.home.HomeRoute
import app.weddingplanner.feature.home.homeScreen
import app.weddingplanner.feature.onboarding.LinkSheetRoute
import app.weddingplanner.feature.onboarding.SignInRoute
import app.weddingplanner.feature.onboarding.WelcomeRoute
import app.weddingplanner.feature.onboarding.onboardingScreens

/** Navigation graph for M0 (docs/03-APP-FLOW.md §1): onboarding → Beranda, plus dev-only tools. */
@Composable
fun WeddingNavHost(isLinked: Boolean, navController: NavHostController = rememberNavController()) {
    val start: Any = remember { if (isLinked) HomeRoute else WelcomeRoute }
    NavHost(navController = navController, startDestination = start) {
        onboardingScreens(
            onStart = { navController.navigate(SignInRoute) },
            onSignedIn = { navController.navigate(LinkSheetRoute) },
            onLinked = { navController.navigateClearingBackStack(HomeRoute) },
        )
        homeScreen(
            onOpenSheetSetup = DebugTools.sheetSetupRoute?.let { route -> { navController.navigate(route) } },
            onSignedOut = { navController.navigateClearingBackStack(WelcomeRoute) },
        )
        with(DebugTools) { debugScreens(navController) }
    }
}

private fun NavHostController.navigateClearingBackStack(route: Any) {
    navigate(route) {
        popUpTo(graph.id) { inclusive = true }
        launchSingleTop = true
    }
}
