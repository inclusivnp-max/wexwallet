plugins {
    id("wex.android.library")
}

android {
    namespace = "wex.wallet.security.mnemonic"
}

dependencies {
    implementation(project(":core:common"))
    implementation(libs.bitcoin.kmp)
    implementation(libs.secp256k1.android)
    implementation(libs.bouncycastle)

    testImplementation(libs.junit4)
    testImplementation(libs.secp256k1.jvm)
}
