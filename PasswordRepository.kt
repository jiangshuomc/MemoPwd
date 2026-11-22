package com.example.memopwd.data.password

import com.example.memopwd.data.AppDatabase
import com.example.memopwd.security.CryptoManager
import kotlinx.coroutines.flow.Flow

class PasswordRepository(
    database: AppDatabase,
    private val cryptoManager: CryptoManager
) {
    private val dao = database.passwordDao()

    fun getAllPasswords(): Flow<List<PasswordEntry>> = dao.getAllPasswords()
    fun searchPasswords(keyword: String): Flow<List<PasswordEntry>> = dao.searchPasswords(keyword)
    fun getAllCategories(): Flow<List<String>> = dao.getAllCategories()

    suspend fun savePassword(
        id: Long,
        title: String,
        category: String,
        plain: PasswordPlain
    ): Long {
        val (cipher, iv) = cryptoManager.encryptFields(plain.username, plain.password, plain.note)
        return if (id == 0L) {
            dao.insertPassword(
                PasswordEntry(
                    title = title,
                    category = category,
                    cipherText = cipher,
                    iv = iv
                )
            )
        } else {
            dao.updatePassword(
                PasswordEntry(
                    id = id,
                    title = title,
                    category = category,
                    cipherText = cipher,
                    iv = iv,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
            )
            id
        }
    }

    suspend fun deletePassword(entry: PasswordEntry) = dao.deletePassword(entry)

    /** 解密一条记录，失败时返回空明文 */
    fun reveal(entry: PasswordEntry): PasswordPlain {
        val (u, p, n) = cryptoManager.decryptFields(entry.cipherText, entry.iv)
        return PasswordPlain(u, p, n)
    }
}
