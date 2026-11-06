plugins {
    id("wex.android.library")
}

android {
    namespace = "wex.wallet.chain.evm"
}

dependencies {
    implementation(project(":chain:api"))
    implementation(libs.bouncycastle)

    testImplementation(libs.web3j.crypto)
}
