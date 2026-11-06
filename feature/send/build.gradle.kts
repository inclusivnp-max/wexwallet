plugins {
    id("wex.android.feature")
}

android {
    namespace = "wex.wallet.feature.send"
}

dependencies {
    implementation(libs.androidx.compose.material.icons)
    implementation(project(":core:designsystem"))
    implementation(project(":core:network"))
    implementation(project(":data:portfolio"))
    implementation(project(":data:accounts"))
    implementation(project(":chain:api"))
    implementation(project(":chain:evm"))
    implementation(project(":security:mnemonic"))
}
