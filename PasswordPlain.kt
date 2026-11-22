package com.example.memopwd.data.password

/** 解密后的明文，仅存在于内存，不落库 */
data class PasswordPlain(
    val username: String = "",
    val password: String = "",
    val note: String = ""
)
