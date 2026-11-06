plugins {
    id("wex.android.library")
}

android {
    namespace = "wex.wallet.chain.solana"
}

dependencies {
    implementation(project(":chain:api"))
}
