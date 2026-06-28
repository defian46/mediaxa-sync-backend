package com.mediaxa.business.suite.presentation.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mediaxa.business.suite.data.export.ExportResult

@Composable
fun ExportSuccessDialog(
    exportResult: ExportResult,
    onDismissRequest: () -> Unit,
    onOpenFileClick: () -> Unit,
    onShareFileClick: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(
                text = "Laporan berhasil diexport",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(text = "Nama File: ${exportResult.fileName}", fontWeight = FontWeight.SemiBold)
                Text(text = "Lokasi: ${exportResult.filePath}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(text = "Format: ${exportResult.format}", fontSize = 14.sp)
                val sizeKb = String.format("%.2f KB", exportResult.sizeBytes / 1024.0)
                Text(text = "Ukuran: $sizeKb", fontSize = 14.sp)
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onOpenFileClick,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Buka File")
                }
                Button(
                    onClick = onShareFileClick,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Bagikan")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismissRequest,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Tutup", modifier = Modifier.padding(vertical = 4.dp))
            }
        }
    )
}
