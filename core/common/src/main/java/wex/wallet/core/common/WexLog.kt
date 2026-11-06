package wex.wallet.core.common

import android.util.Log

/**
 * Единственный разрешённый способ логирования в проекте.
 *
 * Причина существования: в кошельке цена случайного `Log.d(TAG, seed)` —
 * потеря средств пользователя. Поэтому:
 *  - принимаются только строки, а не произвольные объекты, у которых
 *    `toString()` может незаметно раскрыть содержимое;
 *  - `debug` и `verbose` полностью исчезают в релизной сборке (R8 вырезает
 *    вызовы по правилу из `keepRules/rules.keep`);
 *  - для адресов и любых идентификаторов есть [redact].
 *
 * Секреты (seed, приватные ключи, PIN) не логируются никогда и ни на каком
 * уровне — для них нет и не будет перегрузки в этом API.
 */
object WexLog {

    fun debug(tag: String, message: String) {
        Log.d(tag, message)
    }

    fun info(tag: String, message: String) {
        Log.i(tag, message)
    }

    fun warn(tag: String, message: String, error: Throwable? = null) {
        Log.w(tag, message, error)
    }

    fun error(tag: String, message: String, error: Throwable? = null) {
        Log.e(tag, message, error)
    }

    /**
     * Сокращает идентификатор до вида `0x1234…cdef`.
     *
     * Нужен для диагностики: по такой строке можно сопоставить события между
     * собой, но нельзя восстановить адрес целиком, если лог куда-то утечёт.
     */
    fun redact(value: String, visiblePrefix: Int = 6, visibleSuffix: Int = 4): String {
        val minLengthToRedact = visiblePrefix + visibleSuffix + 1
        if (value.length < minLengthToRedact) return "…"
        return value.take(visiblePrefix) + "…" + value.takeLast(visibleSuffix)
    }
}
