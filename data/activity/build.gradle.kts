plugins {
    id("wex.android.library")
}

android {
    namespace = "wex.wallet.data.activity"
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:network"))
    implementation(project(":chain:evm"))
    implementation(project(":data:portfolio"))
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.kotlinx.coroutines.test)
}
