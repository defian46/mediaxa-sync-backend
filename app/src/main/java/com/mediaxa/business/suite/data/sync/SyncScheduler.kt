package com.mediaxa.business.suite.data.sync

import android.content.Context
import android.util.Log
import androidx.work.*
import java.util.concurrent.TimeUnit

private const val TAG = "SyncScheduler"
private const val PERIODIC_SYNC_WORK = "mediaxa_periodic_sync"
private const val ONE_TIME_SYNC_WORK = "mediaxa_onetime_sync"

/**
 * Manages WorkManager scheduling for background sync operations.
 *
 * Three trigger types:
 * 1. **Periodic** — runs every 15 minutes when network is available (always-on background sync)
 * 2. **One-time** — triggered immediately after a successful transaction or user action
 * 3. **Manual** — triggered by "Force Sync" button in [SyncMonitorScreen]
 *
 * All requests use [NetworkType.CONNECTED] constraint — sync never runs offline.
 */
object SyncScheduler {

    private val networkConstraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    /**
     * Schedule periodic background sync.
     * Safe to call multiple times — WorkManager deduplicates using [PERIODIC_SYNC_WORK] name.
     *
     * Should be called once in [MainApplication.onCreate].
     */
    fun schedulePeriodicSync(context: Context) {
        val periodicRequest = PeriodicWorkRequestBuilder<SyncWorker>(
            repeatInterval = 15,
            repeatIntervalTimeUnit = TimeUnit.MINUTES
        )
            .setConstraints(networkConstraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_SYNC_WORK,
            ExistingPeriodicWorkPolicy.KEEP, // Don't reset if already scheduled
            periodicRequest
        )

        Log.d(TAG, "Periodic sync scheduled (15min interval).")
    }

    /**
     * Trigger a one-time immediate sync.
     * Used after a POS checkout or any other critical data mutation.
     */
    fun triggerImmediateSync(context: Context) {
        val oneTimeRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(networkConstraints)
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            ONE_TIME_SYNC_WORK,
            ExistingWorkPolicy.REPLACE, // Replace any pending one-time sync
            oneTimeRequest
        )

        Log.d(TAG, "Immediate sync triggered.")
    }

    /**
     * Force a sync regardless of any existing schedule.
     * Used by the "Force Sync" button in [SyncMonitorScreen].
     */
    fun forceSync(context: Context) {
        val forceRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(networkConstraints)
            .build()

        WorkManager.getInstance(context).enqueue(forceRequest)
        Log.d(TAG, "Force sync triggered by user.")
    }

    /** Cancel all scheduled sync work (e.g., during logout or store switch). */
    fun cancelAll(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(PERIODIC_SYNC_WORK)
        WorkManager.getInstance(context).cancelUniqueWork(ONE_TIME_SYNC_WORK)
        Log.d(TAG, "All sync work cancelled.")
    }
}
