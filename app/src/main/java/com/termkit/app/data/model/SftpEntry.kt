package com.termkit.app.data.model

data class SftpEntry(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val size: Long,
    val modifiedAt: Long,
    val permissions: String
)
