import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.CommonExtension
import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.register
import java.util.Properties

/**
 * Конфигурация приложения для открытого репозитория.
 *
 * Правило проекта: **чистый клон обязан собираться и запускаться без единого ключа.**
 * Ключи только повышают качество данных, но никогда не являются условием сборки.
 *
 * Источники значений, в порядке убывания приоритета:
 *  1. `secrets.properties` в корне проекта — личный файл разработчика, в `.gitignore`;
 *  2. переменная окружения с тем же именем — так ключи подставляет CI;
 *  3. `secrets.defaults.properties` — файл в репозитории, задаёт полный список
 *     допустимых ключей и безопасные публичные значения по умолчанию.
 *
 * Файл значений по умолчанию одновременно играет роль схемы: ключ, которого в нём нет,
 * считается опечаткой и вызывает ошибку сборки. Это дешёвая защита от «тихо не подхватилось».
 */
class SecretsConventionPlugin : Plugin<Project> {

    override fun apply(target: Project) = with(target) {
        val defaultsFile = rootProject.file(DEFAULTS_FILE)
        if (!defaultsFile.exists()) {
            throw GradleException(
                "Не найден $DEFAULTS_FILE в корне проекта. " +
                    "Этот файл коммитится в репозиторий и описывает все ключи конфигурации.",
            )
        }

        val defaults = defaultsFile.readProperties()
        val overridesFile = rootProject.file(OVERRIDES_FILE)
        val overrides = if (overridesFile.exists()) overridesFile.readProperties() else emptyMap()

        val unknownKeys = overrides.keys - defaults.keys
        if (unknownKeys.isNotEmpty()) {
            throw GradleException(
                "В $OVERRIDES_FILE есть ключи, которых нет в $DEFAULTS_FILE: " +
                    "${unknownKeys.sorted()}. Похоже на опечатку. " +
                    "Новый ключ нужно сначала объявить в $DEFAULTS_FILE.",
            )
        }

        val resolved = LinkedHashMap<String, ResolvedSecret>(defaults.size)
        for ((key, defaultValue) in defaults) {
            val fromFile = overrides[key]?.takeIf { it.isNotBlank() }
            val fromEnv = System.getenv(key)?.takeIf { it.isNotBlank() }
            val (value, source) = when {
                fromFile != null -> fromFile to Source.LOCAL_FILE
                fromEnv != null -> fromEnv to Source.ENVIRONMENT
                else -> defaultValue to Source.DEFAULTS
            }
            resolved[key] = ResolvedSecret(value, source)
        }

        applyToAndroidExtension(resolved)
        registerStatusTask(resolved)
    }

    private fun Project.applyToAndroidExtension(resolved: Map<String, ResolvedSecret>) {
        val configure: CommonExtension.() -> Unit = {
            buildFeatures.buildConfig = true
            with(defaultConfig) {
                for ((key, secret) in resolved) {
                    buildConfigField("String", key, secret.value.asJavaStringLiteral())
                }
                // Чтобы код мог честно сказать пользователю «функция недоступна без ключа»,
                // а не падать или молча показывать пустой экран.
                buildConfigField(
                    "java.util.List<String>",
                    "ALL_CONFIG_KEYS",
                    resolved.keys.sorted().asJavaStringList(),
                )
                buildConfigField(
                    "java.util.List<String>",
                    "MISSING_CONFIG_KEYS",
                    resolved.filterValues { it.value.isBlank() }.keys.sorted().asJavaStringList(),
                )
            }
        }

        pluginManager.withPlugin("com.android.application") {
            extensions.configure<ApplicationExtension> { configure() }
        }
        pluginManager.withPlugin("com.android.library") {
            extensions.configure<LibraryExtension> { configure() }
        }
    }

    private fun Project.registerStatusTask(resolved: Map<String, ResolvedSecret>) {
        // Значения не печатаем никогда — только имя ключа и откуда он взялся.
        val report = buildList {
            add("Конфигурация ${project.path}:")
            for ((key, secret) in resolved.entries.sortedBy { it.key }) {
                val state = if (secret.value.isBlank()) "НЕ ЗАДАН" else "задан"
                add("  %-28s %-9s источник: %s".format(key, state, secret.source.label))
            }
            val missing = resolved.count { it.value.value.isBlank() }
            if (missing > 0) {
                add("")
                add("Не задано ключей: $missing. Это не ошибка — соответствующие функции")
                add("деградируют с понятным сообщением. Как задать: см. secrets.properties.example")
            }
        }
        tasks.register("secretsStatus") {
            group = "help"
            description = "Показывает, какие ключи конфигурации заданы и откуда (без значений)."
            doLast { report.forEach { logger.lifecycle(it) } }
        }
    }

    private enum class Source(val label: String) {
        LOCAL_FILE("secrets.properties"),
        ENVIRONMENT("переменная окружения"),
        DEFAULTS("secrets.defaults.properties"),
    }

    private data class ResolvedSecret(val value: String, val source: Source)

    private companion object {
        const val DEFAULTS_FILE = "secrets.defaults.properties"
        const val OVERRIDES_FILE = "secrets.properties"

        fun java.io.File.readProperties(): Map<String, String> =
            Properties().apply { inputStream().use { load(it) } }
                .entries
                .associate { (k, v) -> k.toString() to v.toString().trim() }

        /** Список строк в виде выражения на Java для buildConfigField. */
        fun Collection<String>.asJavaStringList(): String =
            if (isEmpty()) {
                "java.util.Collections.<String>emptyList()"
            } else {
                joinToString(
                    prefix = "java.util.Arrays.asList(",
                    postfix = ")",
                    transform = { it.asJavaStringLiteral() },
                )
            }

        /** Значение попадает в сгенерированный Java-код, поэтому экранируем строго. */
        fun String.asJavaStringLiteral(): String =
            buildString {
                append('"')
                for (ch in this@asJavaStringLiteral) {
                    when (ch) {
                        '\\' -> append("\\\\")
                        '"' -> append("\\\"")
                        '\n' -> append("\\n")
                        '\r' -> append("\\r")
                        '\t' -> append("\\t")
                        else -> append(ch)
                    }
                }
                append('"')
            }
    }
}
