package wex.wallet

import android.app.Application
import wex.wallet.core.common.WexLog

class WexApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        WexLog.info(TAG, "Запуск ${BuildConfig.APPLICATION_ID} ${BuildConfig.VERSION_NAME}")
    }

    private companion object {
        const val TAG = "WexApplication"
    }
}
