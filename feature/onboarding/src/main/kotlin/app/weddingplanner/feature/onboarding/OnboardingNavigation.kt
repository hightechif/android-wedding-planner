package app.weddingplanner.feature.onboarding

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable data object WelcomeRoute
@Serializable data object SignInRoute
@Serializable data object LinkSheetRoute

/** S-01 Welcome → S-02 Masuk → S-03 Hubungkan Sheet (docs/03-APP-FLOW.md §2). */
fun NavGraphBuilder.onboardingScreens(
    onStart: () -> Unit,
    onSignedIn: () -> Unit,
    onLinked: () -> Unit,
) {
    composable<WelcomeRoute> { WelcomeScreen(onStart = onStart) }
    composable<SignInRoute> { SignInScreen(onSignedIn = onSignedIn) }
    composable<LinkSheetRoute> { LinkSheetScreen(onLinked = onLinked) }
}
