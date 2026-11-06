plugins {
    id("wex.android.feature")
}

android {
    namespace = "wex.wallet.feature.home"
}

dependencies {
    implementation(project(":core:designsystem"))
    implementation(project(":data:portfolio"))
    implementation(project(":chain:api"))
    implementation(libs.androidx.compose.material.icons)
}
