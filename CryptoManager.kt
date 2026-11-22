package com.example.memopwd.security

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.util.Base64
import org.json.JSONObject
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * 基于 AndroidKeyStore 的 AES-256-GCM 加密。
 *
 * 设计要点：
 * 1. 密钥由系统 Keystore 保管，App 进程内无法导出，root 也拿不到明文密钥。
 * 2. 一条记录只调用一次 encrypt()，账号/密码/备注打包成一个 JSON 密文，
 *    与单个 iv 对应。避免每条字段各用一个 iv 导致解密错乱。
 * 3. setInvalidatedByBiometricEnrollment(false)：用户增删指纹后密钥不会作废，
 *    否则已存的密码会永久无法解密。
 */
class CryptoManager(private val context: Context) {

    private val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

    private fun getOrCreateKey(): SecretKey {
        (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.secretKey?.let { return it }

        val builder = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setRandomizedEncryptionRequired(true)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            builder
                .setUserAuthenticationRequired(true)
                .setInvalidatedByBiometricEnrollment(false)
                .setUserAuthenticationParameters(0, KeyProperties.AUTH_BIOMETRIC_STRONG)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            @Suppress("DEPRECATION")
            builder.setUserAuthenticationRequired(true)
        }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        keyGenerator.init(builder.build())
        return keyGenerator.generateKey()
    }

    /** 密钥是否可用（生物识别已解锁后才返回 true） */
    fun isKeyReady(): Boolean {
        return try {
            val key = getOrCreateKey()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, key)
            true
        } catch (e: KeyPermanentlyInvalidatedException) {
            false
        } catch (e: Exception) {
            false
        }
    }

    /** 加密，返回 (Base64密文, Base64 IV) */
    fun encrypt(plainText: String): Pair<String, String> {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val encrypted = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        return Pair(
            Base64.encodeToString(encrypted, Base64.NO_WRAP),
            Base64.encodeToString(cipher.iv, Base64.NO_WRAP)
        )
    }

    fun decrypt(cipherText: String, iv: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, Base64.decode(iv, Base64.NO_WRAP))
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), spec)
        val plain = cipher.doFinal(Base64.decode(cipherText, Base64.NO_WRAP))
        return String(plain, Charsets.UTF_8)
    }

    /** 把账号/密码/备注打包加密 */
    fun encryptFields(username: String, password: String, note: String): Pair<String, String> {
        val json = JSONObject().apply {
            put("u", username)
            put("p", password)
            put("n", note)
        }
        return encrypt(json.toString())
    }

    /** 解开密文块；失败返回空明文而不是抛异常，避免整页崩溃 */
    fun decryptFields(cipherText: String, iv: String): Triple<String, String, String> {
        return try {
            val json = JSONObject(decrypt(cipherText, iv))
            Triple(
                json.optString("u"),
                json.optString("p"),
                json.optString("n")
            )
        } catch (e: Exception) {
            Triple("", "", "")
        }
    }

    /** 密钥报废后清空全部密码数据，避免留下一堆解不开的脏数据 */
    fun resetKey() {
        try {
            keyStore.deleteEntry(KEY_ALIAS)
        } catch (e: Exception) {
            // ignore
        }
    }

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "memopwd_master_key"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH = 128
    }
}
