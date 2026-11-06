plugins {
    id("wex.android.library")
}

android {
    namespace = "wex.wallet.chain.ton"
}

dependencies {
    implementation(project(":chain:api"))
    implementation(libs.ton.kotlin.contract)
    implementation(libs.bouncycastle)
}
