pluginManagement {
    // Convention-плагины проекта. Лежат в исходниках рядом, а не в бинарном артефакте,
    // чтобы конфигурацию сборки можно было прочитать и проверить целиком.
    includeBuild("build-logic")
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    // Автоматическая загрузка нужного JDK, если у контрибьютора его нет.
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "wex-wallet"

include(":app")
include(":core:common")
include(":core:designsystem")
include(":security:mnemonic")
include(":security:keystore")
include(":chain:api")
include(":chain:evm")
include(":chain:bitcoin")
include(":chain:solana")
include(":chain:ton")
include(":core:network")
include(":data:accounts")
include(":data:portfolio")
include(":data:activity")
include(":feature:onboarding")
include(":feature:receive")
include(":feature:home")
include(":feature:send")
include(":feature:activity")
