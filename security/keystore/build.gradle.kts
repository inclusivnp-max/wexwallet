plugins {
    id("wex.android.library")
}

android {
    namespace = "wex.wallet.security.keystore"
}

dependencies {
    implementation(project(":core:common"))
    implementation(libs.argon2kt)
    implementation(libs.androidx.biometric)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.annotation)

    testImplementation(libs.junit4)
    testImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.test.runner)
}
