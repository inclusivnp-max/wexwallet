import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import wex.wallet.buildlogic.configureAndroidCommon
import wex.wallet.buildlogic.intVersion
import wex.wallet.buildlogic.libs

class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.android.application")

        extensions.configure<ApplicationExtension> {
            configureAndroidCommon(this)

            defaultConfig {
                targetSdk = libs.intVersion("targetSdk")
            }

            buildTypes {
                release {
                    // R8 обязателен в релизе: без него в APK остаются имена классов
                    // и отладочные строки. Правила проекта AGP 9 берёт из src/main/keepRules/.
                    optimization {
                        enable = true
                    }
                }
                debug {
                    // Отладочная и релизная сборки должны уживаться на одном устройстве.
                    applicationIdSuffix = ".debug"
                }
            }
        }

        dependencies {
            add("testImplementation", libs.findLibrary("junit4").get())
            add("androidTestImplementation", libs.findLibrary("androidx-test-junit").get())
            add("androidTestImplementation", libs.findLibrary("androidx-test-runner").get())
        }
    }
}
