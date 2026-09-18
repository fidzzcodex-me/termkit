package com.termkit.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.termkit.app.data.model.ConnectionStatus
import com.termkit.app.ui.theme.ErrorColor
import com.termkit.app.ui.theme.SuccessColor
import com.termkit.app.ui.theme.WarningColor

@Composable
fun StatusChip(status: ConnectionStatus, modifier: Modifier = Modifier) {
    val (color, label) = when (status) {
        is ConnectionStatus.Offline -> MaterialTheme.colorScheme.outline to "Offline"
        is ConnectionStatus.Connecting -> WarningColor to "Menghubungkan"
        is ConnectionStatus.Connected -> SuccessColor to "Terhubung"
        is ConnectionStatus.Reconnecting -> WarningColor to "Menyambung ulang"
        is ConnectionStatus.Error -> ErrorColor to "Error"
    }
    Row(
        modifier = modifier
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(8.dp).background(color, CircleShape))
        Spacer(modifier = Modifier.size(6.dp))
        Text(text = label, color = color, style = MaterialTheme.typography.labelMedium)
    }
}
