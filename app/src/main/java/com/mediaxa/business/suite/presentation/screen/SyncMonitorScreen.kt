package com.mediaxa.business.suite.presentation.screen

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mediaxa.business.suite.data.local.entity.SyncQueueItem
import com.mediaxa.business.suite.data.local.entity.SyncQueueStatus
import com.mediaxa.business.suite.presentation.viewmodel.SyncMonitorUiState
import com.mediaxa.business.suite.presentation.viewmodel.SyncMonitorViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncMonitorScreen(
    viewModel: SyncMonitorViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    // Snackbar for sync messages
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.syncMessage) {
        uiState.syncMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Sinkronisasi Cloud", fontWeight = FontWeight.Bold)
                        Text(
                            "Mediaxa Business Suite",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── Status Overview Card ─────────────────────────────────────────
            item {
                SyncStatusCard(uiState)
            }

            // ── Action Buttons ────────────────────────────────────────────────
            item {
                SyncActionButtons(
                    isSyncing = uiState.isSyncing,
                    hasFailures = uiState.failedCount > 0,
                    onForceSync = viewModel::forceSync,
                    onRetryFailed = viewModel::retryFailed
                )
            }

            // ── KPI Row ───────────────────────────────────────────────────────
            item {
                SyncKpiRow(uiState)
            }

            // ── Pending Queue Section ─────────────────────────────────────────
            if (uiState.pendingItems.isNotEmpty()) {
                item {
                    SectionHeader("Antrian Tertunda", uiState.pendingCount)
                }
                items(uiState.pendingItems, key = { it.localId }) { item ->
                    SyncQueueItemCard(item, isError = false)
                }
            }

            // ── Sync Errors Section ───────────────────────────────────────────
            if (uiState.failedItems.isNotEmpty()) {
                item {
                    SectionHeader("Error Sinkronisasi", uiState.failedCount, isError = true)
                }
                items(uiState.failedItems, key = { it.localId }) { item ->
                    SyncQueueItemCard(item, isError = true)
                }
            }

            // ── Empty State ───────────────────────────────────────────────────
            if (uiState.pendingItems.isEmpty() && uiState.failedItems.isEmpty() && !uiState.isSyncing) {
                item {
                    SyncEmptyState(uiState.lastSyncedAt)
                }
            }

            // ── Diagnostics Section (Requirement 8) ───────────────────────────
            item {
                Spacer(modifier = Modifier.height(16.dp))
                DiagnosticsCard(uiState)
            }
        }
    }
}

@Composable
private fun SyncStatusCard(state: SyncMonitorUiState) {
    val isHealthy = state.failedCount == 0
    val gradient = if (isHealthy) {
        Brush.linearGradient(listOf(Color(0xFF1B5E20), Color(0xFF2E7D32)))
    } else {
        Brush.linearGradient(listOf(Color(0xFFB71C1C), Color(0xFFC62828)))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(gradient)
                .padding(20.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isHealthy) Icons.Default.CloudDone else Icons.Default.CloudOff,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (isHealthy) "Sinkronisasi Normal" else "Terdapat Error",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        state.lastSyncedAt?.let {
                            Text(
                                text = "Terakhir sinkron: ${formatTimestamp(it)}",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 12.sp
                            )
                        } ?: Text(
                            text = "Belum pernah sinkron",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 12.sp
                        )
                    }
                }

                if (state.isSyncing) {
                    Spacer(Modifier.height(12.dp))
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color.White,
                        trackColor = Color.White.copy(alpha = 0.3f)
                    )
                    Text(
                        "Menyinkronkan data...",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SyncKpiRow(state: SyncMonitorUiState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SyncKpiCard(
            modifier = Modifier.weight(1f),
            label = "Tertunda",
            value = state.pendingCount.toString(),
            color = MaterialTheme.colorScheme.primary
        )
        SyncKpiCard(
            modifier = Modifier.weight(1f),
            label = "Total Queue",
            value = state.totalQueueSize.toString(),
            color = MaterialTheme.colorScheme.secondary
        )
        SyncKpiCard(
            modifier = Modifier.weight(1f),
            label = "Gagal",
            value = state.failedCount.toString(),
            color = if (state.failedCount > 0) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.outline
        )
    }
}

@Composable
private fun SyncKpiCard(modifier: Modifier, label: String, value: String, color: Color) {
    Card(modifier = modifier, shape = RoundedCornerShape(12.dp)) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = color)
            Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        }
    }
}

@Composable
private fun SyncActionButtons(
    isSyncing: Boolean,
    hasFailures: Boolean,
    onForceSync: () -> Unit,
    onRetryFailed: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = onForceSync,
            enabled = !isSyncing,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("Force Sync")
        }
        if (hasFailures) {
            OutlinedButton(
                onClick = onRetryFailed,
                enabled = !isSyncing,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Replay, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Retry Gagal")
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, count: Int, isError: Boolean = false) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Text(
            title,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.titleSmall
        )
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(
                    if (isError) MaterialTheme.colorScheme.errorContainer
                    else MaterialTheme.colorScheme.primaryContainer
                )
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Text(
                count.toString(),
                fontSize = 11.sp,
                color = if (isError) MaterialTheme.colorScheme.onErrorContainer
                        else MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun SyncQueueItemCard(item: SyncQueueItem, isError: Boolean) {
    val statusColor = when (SyncQueueStatus.valueOf(item.status)) {
        SyncQueueStatus.PENDING -> MaterialTheme.colorScheme.primary
        SyncQueueStatus.IN_PROGRESS -> MaterialTheme.colorScheme.secondary
        SyncQueueStatus.SYNCED -> Color(0xFF2E7D32)
        SyncQueueStatus.FAILED -> MaterialTheme.colorScheme.error
        SyncQueueStatus.CANCELLED -> MaterialTheme.colorScheme.outline
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isError)
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(statusColor)
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        item.entityType,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        item.operation,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    "UUID: ${item.uuid.take(12)}...",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                if (isError && item.errorMessage != null) {
                    Text(
                        item.errorMessage,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.error,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                    if (item.retryCount > 0) {
                        Text(
                            "Retry ${item.retryCount}/${item.maxRetries}",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SyncEmptyState(lastSyncedAt: Long?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.CloudDone,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = Color(0xFF2E7D32)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "Semua data tersinkronkan",
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.titleMedium
        )
        lastSyncedAt?.let {
            Text(
                "Terakhir: ${formatTimestamp(it)}",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

private fun formatTimestamp(millis: Long): String {
    return SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID")).format(Date(millis))
}

@Composable
private fun DiagnosticsCard(state: SyncMonitorUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Diagnostics & Debug Info",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
            
            DiagnosticRow("Store UUID", state.currentStoreUuid)
            DiagnosticRow("User UUID", state.currentUserUuid)
            DiagnosticRow("Base URL", state.backendBaseUrl)
            
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
            
            DiagnosticRow("Local Transactions", state.localTransactionsCount.toString())
            DiagnosticRow("Local Items Sold", state.localTransactionItemsCount.toString())
            DiagnosticRow("Local Payments", state.localPaymentsCount.toString())
            DiagnosticRow("Local Stock Movements", state.localStockMovementsCount.toString())
            DiagnosticRow("Pending Sync Queue", state.pendingCount.toString())
            
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
            
            Text(
                text = "Last Checkout Error:",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = state.lastCheckoutError ?: "None",
                style = MaterialTheme.typography.bodyMedium,
                color = if (state.lastCheckoutError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Last Login Error:",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = state.lastLoginError ?: "None",
                style = MaterialTheme.typography.bodyMedium,
                color = if (state.lastLoginError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun DiagnosticRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
