package app.weddingplanner.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import app.weddingplanner.debug.DebugTools
import app.weddingplanner.feature.budget.BudgetItemRoute
import app.weddingplanner.feature.budget.BudgetListRoute
import app.weddingplanner.feature.budget.budgetScreens
import app.weddingplanner.feature.home.HomeRoute
import app.weddingplanner.feature.home.homeScreen
import app.weddingplanner.feature.onboarding.LinkSheetRoute
import app.weddingplanner.feature.onboarding.SignInRoute
import app.weddingplanner.feature.onboarding.WelcomeRoute
import app.weddingplanner.feature.onboarding.onboardingScreens
import app.weddingplanner.feature.vendor.VendorRoute
import app.weddingplanner.feature.vendor.vendorScreen
import kotlin.reflect.KClass

private data class TopLevel(val route: Any, val type: KClass<*>, val label: String, val icon: ImageVector)

private val TOP_LEVEL = listOf(
    TopLevel(HomeRoute, HomeRoute::class, "Beranda", Icons.Rounded.Home),
    TopLevel(BudgetListRoute, BudgetListRoute::class, "Budget", Icons.Rounded.Payments),
    TopLevel(VendorRoute, VendorRoute::class, "Vendor", Icons.Rounded.Storefront),
)

/** Navigation graph (docs/03-APP-FLOW.md §1): onboarding → Beranda / Budget / Vendor, plus dev-only tools. */
@Composable
fun WeddingNavHost(isLinked: Boolean, navController: NavHostController = rememberNavController()) {
    val start: Any = remember { if (isLinked) HomeRoute else WelcomeRoute }
    val entry by navController.currentBackStackEntryAsState()
    val destination = entry?.destination
    val showBar = TOP_LEVEL.any { top -> destination?.hasRoute(top.type) == true }

    Scaffold(
        bottomBar = {
            if (showBar) {
                NavigationBar {
                    TOP_LEVEL.forEach { top ->
                        NavigationBarItem(
                            selected = destination?.hierarchy?.any { it.hasRoute(top.type) } == true,
                            onClick = {
                                navController.navigate(top.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(top.icon, contentDescription = null) },
                            label = { Text(top.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding).consumeWindowInsets(padding)) {
            NavHost(navController = navController, startDestination = start) {
                onboardingScreens(
                    onStart = { navController.navigate(SignInRoute) },
                    onSignedIn = { navController.navigate(LinkSheetRoute) },
                    onLinked = { navController.navigateClearingBackStack(HomeRoute) },
                )
                homeScreen(
                    onOpenBudgetItem = { navController.navigate(BudgetItemRoute(it)) },
                    onOpenSheetSetup = DebugTools.sheetSetupRoute?.let { route -> { navController.navigate(route) } },
                    onSignedOut = { navController.navigateClearingBackStack(WelcomeRoute) },
                )
                budgetScreens(
                    onOpenItem = { navController.navigate(BudgetItemRoute(it)) },
                    onBack = { navController.popBackStack() },
                )
                vendorScreen()
                with(DebugTools) { debugScreens(navController) }
            }
        }
    }
}

private fun NavHostController.navigateClearingBackStack(route: Any) {
    navigate(route) {
        popUpTo(graph.id) { inclusive = true }
        launchSingleTop = true
    }
}
