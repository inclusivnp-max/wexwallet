import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import wex.wallet.buildlogic.libs

/**
 * Всё, что нужно любому :feature:* модулю. Feature-модули не повторяют
 * один и тот же список зависимостей и не знают о слоях ниже :data.
 */
class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("wex.android.library")
        pluginManager.apply("wex.android.compose")

        dependencies {
            add("implementation", project(":core:designsystem"))
            add("implementation", project(":core:common"))
            add("implementation", libs.findLibrary("androidx-lifecycle-viewmodel-compose").get())
            add("implementation", libs.findLibrary("androidx-lifecycle-runtime-ktx").get())
            add("implementation", libs.findLibrary("androidx-navigation-compose").get())
        }
    }
}
