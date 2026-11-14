package app.ji

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import app.ji.ui.onboarding.OnboardingScreen
import app.ji.ui.privacy.PrivacyScreen
import app.ji.ui.today.TodayScreen

@Composable
fun MainNavigation(
    sharedInitialText: String? = null,
    onClearSharedText: () -> Unit = {}
) {
    val backStack = rememberNavBackStack(TodayKey)

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryProvider =
            entryProvider {
                entry<TodayKey> {
                    TodayScreen(
                        onNavigateToOnboarding = { backStack.add(OnboardingKey) },
                        onNavigateToPrivacy = { backStack.add(PrivacyKey) },
                        sharedInitialText = sharedInitialText,
                        onClearSharedText = onClearSharedText
                    )
                }
                entry<OnboardingKey> {
                    OnboardingScreen()
                }
                entry<PrivacyKey> {
                    PrivacyScreen(
                        onNavigateBack = { backStack.removeLastOrNull() }
                    )
                }
                entry<Main> {
                    TodayScreen(
                        onNavigateToOnboarding = { backStack.add(OnboardingKey) },
                        onNavigateToPrivacy = { backStack.add(PrivacyKey) },
                        sharedInitialText = sharedInitialText,
                        onClearSharedText = onClearSharedText
                    )
                }
            },
    )
}
