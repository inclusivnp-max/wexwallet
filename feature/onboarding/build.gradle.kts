plugins {
    id("wex.android.feature")
}

android {
    namespace = "wex.wallet.feature.onboarding"
}

dependencies {
    implementation(libs.androidx.compose.material.icons)
    implementation(project(":core:designsystem"))
    implementation(project(":security:mnemonic"))
    implementation(project(":security:keystore"))
    implementation(project(":data:accounts"))
    implementation(project(":chain:api"))
}
