package com.termkit.app.data.model

enum class AppTheme {
    LIGHT,
    DARK,
    SYSTEM
}

enum class TerminalFontSize(val sp: Int) {
    SMALL(12),
    NORMAL(14),
    LARGE(18)
}

data class AppSettings(
    val theme: AppTheme = AppTheme.SYSTEM,
    val terminalFontSize: TerminalFontSize = TerminalFontSize.NORMAL,
    val keepScreenOnWhileConnected: Boolean = true
)
