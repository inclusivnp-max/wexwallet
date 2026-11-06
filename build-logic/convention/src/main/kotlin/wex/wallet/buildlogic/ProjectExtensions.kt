package wex.wallet.buildlogic

import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType

/** Доступ к каталогу версий из convention-плагинов. */
internal val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

internal fun VersionCatalog.version(name: String): String =
    findVersion(name)
        .orElseThrow { IllegalStateException("Версия '$name' не найдена в gradle/libs.versions.toml") }
        .requiredVersion

internal fun VersionCatalog.intVersion(name: String): Int = version(name).toInt()
