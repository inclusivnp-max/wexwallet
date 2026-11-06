import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import wex.wallet.buildlogic.configureCompose

/**
 * Подключается поверх wex.android.application или wex.android.library.
 * Сам по себе Android-плагин не применяет — порядок применения остаётся явным.
 */
class AndroidComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        // Начиная с Kotlin 2.0 плагин компилятора Compose обязателен.
        pluginManager.apply("org.jetbrains.kotlin.plugin.compose")

        pluginManager.withPlugin("com.android.application") {
            extensions.configure<ApplicationExtension> { configureCompose(this) }
        }
        pluginManager.withPlugin("com.android.library") {
            extensions.configure<LibraryExtension> { configureCompose(this) }
        }
    }
}
