package com.example.memopwd.data.password

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 敏感字段（账号/密码/备注）在写入前由 CryptoManager 一次性打包加密，
 * 因此共用同一个 iv。解密时一次 decrypt 拿到整块 JSON 再拆开。
 */
@Entity(tableName = "passwords")
data class PasswordEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val category: String = "默认",
    val cipherText: String,
    val iv: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
