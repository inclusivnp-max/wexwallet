plugins {
    id("wex.android.feature")
}

android {
    namespace = "wex.wallet.feature.receive"
}

dependencies {
    implementation(project(":core:designsystem"))
    implementation(project(":data:accounts"))
    implementation(project(":chain:api"))
}
