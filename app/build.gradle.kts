plugins {
    id("wex.android.application")
    id("wex.android.compose")
    id("wex.secrets")
}

android {
    namespace = "wex.wallet"

    defaultConfig {
        applicationId = "wex.wallet"
        versionCode = 1
        versionName = "0.1.0"
        ndk {
            abiFilters += setOf("arm64-v8a", "x86_64")
        }
    }
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:network"))
    implementation(project(":feature:onboarding"))
    implementation(project(":feature:receive"))
    implementation(project(":feature:home"))
    implementation(project(":feature:activity"))
    implementation(project(":feature:send"))
    implementation(project(":chain:api"))
    implementation(project(":chain:evm"))
    implementation(project(":chain:bitcoin"))
    implementation(project(":chain:solana"))
    implementation(project(":chain:ton"))
    implementation(project(":data:accounts"))
    implementation(project(":data:portfolio"))
    implementation(project(":data:activity"))
    implementation(project(":security:mnemonic"))
    implementation(project(":security:keystore"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.navigation.compose)
}
