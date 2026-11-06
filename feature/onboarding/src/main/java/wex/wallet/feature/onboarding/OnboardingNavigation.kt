package wex.wallet.feature.onboarding

import kotlinx.serialization.Serializable

@Serializable
data object WelcomeRoute

@Serializable
data object CreateWalletRoute

@Serializable
data object ImportWalletRoute

@Serializable
data object VerifyMnemonicRoute

@Serializable
data object SetPinRoute

@Serializable
data object OnboardingCompleteRoute
