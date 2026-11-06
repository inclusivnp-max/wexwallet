plugins {
    id("wex.android.library")
}

android {
    namespace = "wex.wallet.data.portfolio"
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:network"))
    implementation(project(":chain:api"))
    implementation(project(":chain:evm"))
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
}
