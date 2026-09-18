package com.termkit.app.data.model

import java.util.UUID

data class Host(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val hostname: String,
    val port: Int = 22,
    val username: String,
    val authMethod: AuthMethod,
    val password: String = "",
    val privateKey: String = "",
    val passphrase: String = "",
    val isFavorite: Boolean = false,
    val lastConnectedAt: Long = 0L
)
