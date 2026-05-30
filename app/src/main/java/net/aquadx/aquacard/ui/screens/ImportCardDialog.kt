package net.aquadx.aquacard.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.aquadx.aquacard.crypto.CardFormat

/**
 * Импорт существующей карты AquaDX по Serial Number (IDm) ИЛИ Access Code —
 * в том же формате, что принимает форма AquaDX «Link Card».
 */
@Composable
fun ImportCardDialog(
    onDismiss: () -> Unit,
    onImport: (name: String, idm: String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Импорт карты") },
        text = {
            Column {
                Text(
                    "Вставьте Serial Number (02:FE:…) или Access Code (20 цифр) вашей карты AquaDX. " +
                        "Эмуляция доступна только для карт с IDm 02FE.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Название карты") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = code,
                    onValueChange = {
                        code = it
                        error = null
                    },
                    label = { Text("Serial Number или Access Code") },
                    singleLine = true,
                    isError = error != null,
                    modifier = Modifier.fillMaxWidth(),
                )
                error?.let {
                    Spacer(Modifier.height(4.dp))
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val idm = CardFormat.idmFromSerial(code) ?: CardFormat.idmFromAccessCode(code)
                if (idm == null) {
                    error = "Не похоже на Serial Number или Access Code."
                } else {
                    onImport(name.ifBlank { "Импортированная карта" }, idm)
                }
            }) { Text("Импортировать") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        },
    )
}
