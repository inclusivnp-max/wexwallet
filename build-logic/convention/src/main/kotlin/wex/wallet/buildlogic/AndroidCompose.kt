package wex.wallet.buildlogic

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/** Общая настройка Compose для любого модуля с UI. */
internal fun Project.configureCompose(extension: CommonExtension) {
    extension.buildFeatures.compose = true

    val bom = libs.findLibrary("androidx-compose-bom").get()
    val commonUi = listOf(
        "androidx-compose-ui",
        "androidx-compose-ui-graphics",
        "androidx-compose-ui-tooling-preview",
        "androidx-compose-material3",
        "androidx-lifecycle-runtime-compose",
    )

    dependencies {
        add("implementation", platform(bom))
        add("androidTestImplementation", platform(bom))
        commonUi.forEach { add("implementation", libs.findLibrary(it).get()) }
        add("debugImplementation", libs.findLibrary("androidx-compose-ui-tooling").get())
        add("debugImplementation", libs.findLibrary("androidx-compose-ui-test-manifest").get())
        add("androidTestImplementation", libs.findLibrary("androidx-compose-ui-test-junit4").get())
    }
}
