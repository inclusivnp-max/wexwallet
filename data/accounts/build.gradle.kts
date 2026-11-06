plugins {
    id("wex.android.library")
}

android {
    namespace = "wex.wallet.data.accounts"
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":chain:api"))
    implementation(project(":security:mnemonic"))
    implementation(project(":security:keystore"))

    testImplementation(libs.secp256k1.jvm)
    testImplementation(project(":chain:evm"))
    testImplementation(project(":chain:bitcoin"))
    testImplementation(project(":chain:solana"))
}
