plugins {
    id("wex.android.library")
}

android {
    namespace = "wex.wallet.chain.bitcoin"
}

dependencies {
    implementation(project(":chain:api"))
    implementation(libs.bitcoin.kmp)
    implementation(libs.secp256k1.android)

    testImplementation(libs.secp256k1.jvm)
}
