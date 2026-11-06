package wex.wallet.security.keystore

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.crypto.Cipher
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class BiometricAuth(private val activity: FragmentActivity) {

    fun canAuthenticate(): Boolean {
        val manager = BiometricManager.from(activity)
        return manager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) ==
            BiometricManager.BIOMETRIC_SUCCESS
    }

    suspend fun authenticate(
        title: String,
        subtitle: String,
        negativeButtonText: String,
        cipher: Cipher,
    ): Cipher = suspendCancellableCoroutine { cont ->
        val prompt = BiometricPrompt(
            activity,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    val authedCipher = result.cryptoObject?.cipher
                    if (authedCipher != null) {
                        cont.resume(authedCipher)
                    } else {
                        cont.resumeWithException(BiometricException(-1, "No cipher in result"))
                    }
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    cont.resumeWithException(BiometricException(errorCode, errString.toString()))
                }

                override fun onAuthenticationFailed() {
                    // Individual attempt failed, prompt stays open
                }
            },
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText(negativeButtonText)
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .build()

        prompt.authenticate(promptInfo, BiometricPrompt.CryptoObject(cipher))
        cont.invokeOnCancellation { prompt.cancelAuthentication() }
    }
}

class BiometricException(val errorCode: Int, message: String) : Exception(message)
