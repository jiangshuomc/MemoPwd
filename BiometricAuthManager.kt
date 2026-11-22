package com.example.memopwd.security

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.activity.ComponentActivity
import com.example.memopwd.R

class BiometricAuthManager(private val activity: ComponentActivity) {

    private val executor = ContextCompat.getMainExecutor(activity)

    enum class Availability { READY, NO_HARDWARE, NONE_ENROLLED, UNAVAILABLE }

    fun checkAvailability(): Availability {
        val manager = BiometricManager.from(activity)
        return when (manager.canAuthenticate(Authenticators)) {
            BiometricManager.BIOMETRIC_SUCCESS -> Availability.READY
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> Availability.NO_HARDWARE
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> Availability.UNAVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> Availability.NONE_ENROLLED
            else -> Availability.UNAVAILABLE
        }
    }

    fun authenticate(
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
        onFailed: () -> Unit = {}
    ) {
        when (checkAvailability()) {
            Availability.NONE_ENROLLED -> {
                onError(activity.getString(R.string.no_biometric_hint))
                return
            }
            Availability.NO_HARDWARE, Availability.UNAVAILABLE -> {
                // 无生物识别硬件时直接放行，靠 Keystore 密钥本身兜底
                onSuccess()
                return
            }
            Availability.READY -> Unit
        }

        val prompt = BiometricPrompt(activity, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    // 用户主动取消不算失败，不弹错误
                    if (errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                        errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                        errorCode == BiometricPrompt.ERROR_CANCELED
                    ) return
                    onError(errString.toString())
                }

                override fun onAuthenticationFailed() {
                    onFailed()
                }
            })

        prompt.authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle(activity.getString(R.string.biometric_title))
                .setSubtitle(activity.getString(R.string.biometric_subtitle))
                .setNegativeButtonText(activity.getString(R.string.biometric_cancel))
                .setConfirmationRequired(false)
                .build()
        )
    }

    /** 释放对 Activity 的引用，避免配置变更时泄漏 */
    fun invalidate() {
        executor.execute { /* 排空待执行回调 */ }
    }

    private companion object {
        val Authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
    }
}
