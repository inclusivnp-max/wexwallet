package wex.wallet.buildlogic

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project

/**
 * Настройки, общие для каждого Android-модуля.
 *
 * Собраны в одном месте намеренно: модуль не должен иметь возможность
 * незаметно уехать на другой minSdk или другой уровень Java — для кошелька
 * это вопрос безопасности, а не только единообразия.
 *
 * Обращения идут через свойства, а не через блоки `compileOptions { }`:
 * блочные функции DSL генерирует Gradle для .kts-скриптов, в коде
 * convention-плагина их нет.
 */
internal fun Project.configureAndroidCommon(extension: CommonExtension) = with(extension) {
    compileSdk = libs.intVersion("compileSdk")

    defaultConfig.minSdk = libs.intVersion("minSdk")
    defaultConfig.testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

    // Built-in Kotlin в AGP 9 берёт jvmTarget отсюда — отдельно его задавать не нужно.
    val java = JavaVersion.toVersion(libs.version("javaTarget"))
    compileOptions.sourceCompatibility = java
    compileOptions.targetCompatibility = java

    // Включаем точечно там, где действительно нужно.
    buildFeatures.buildConfig = false
    buildFeatures.viewBinding = false
    buildFeatures.aidl = false
    buildFeatures.shaders = false

    lint.abortOnError = true
    lint.checkDependencies = true
    lint.sarifReport = true

    packaging.resources.excludes.addAll(
        setOf(
            "/META-INF/{AL2.0,LGPL2.1}",
            "/META-INF/LICENSE*",
            "/META-INF/NOTICE*",
        ),
    )
}
