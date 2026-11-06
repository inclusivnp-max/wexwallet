import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import wex.wallet.buildlogic.configureAndroidCommon
import wex.wallet.buildlogic.libs

class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.android.library")

        extensions.configure<LibraryExtension> {
            configureAndroidCommon(this)
        }

        dependencies {
            add("testImplementation", libs.findLibrary("junit4").get())
        }
    }
}
