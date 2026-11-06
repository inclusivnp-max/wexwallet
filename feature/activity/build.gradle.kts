plugins {
    id("wex.android.feature")
}

android {
    namespace = "wex.wallet.feature.activity"
}

dependencies {
    implementation(project(":core:designsystem"))
    implementation(project(":data:activity"))
    implementation(libs.androidx.compose.material.icons)
}
