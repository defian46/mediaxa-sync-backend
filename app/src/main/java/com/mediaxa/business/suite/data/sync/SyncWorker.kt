package com.mediaxa.business.suite.data.sync

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

private const val TAG = "SyncWorker"

/**
 * WorkManager [CoroutineWorker] that delegates sync processing to [SyncEngine].
 *
 * WorkManager handles:
 * - Retry scheduling (with exponential backoff configured in [SyncScheduler])
 * - Battery and network constraints
 * - Persistence across app restarts and device reboots
 *
 * Returns:
 * - [Result.success] when queue is fully drained or was already empty
 * - [Result.retry] when sync had failures — WorkManager will retry with backoff
 * - [Result.failure] on unexpected exceptions — WorkManager will not retry
 */
class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            Log.d(TAG, "SyncWorker started (runAttemptCount=$runAttemptCount)")

            val application = applicationContext as? com.mediaxa.business.suite.MainApplication
                ?: run {
                    Log.e(TAG, "Could not resolve MainApplication — aborting sync.")
                    return Result.failure()
                }

            val syncResult = application.syncEngine.processQueue()

            Log.d(TAG, "SyncWorker finished: $syncResult")

            return if (syncResult.hasFailures) {
                // WorkManager will retry this worker with exponential backoff
                Result.retry()
            } else {
                Result.success()
            }
        } catch (e: Exception) {
            Log.e(TAG, "SyncWorker unexpected exception: ${e.message}", e)
            // Unexpected exception — do not retry to avoid infinite crash loops
            Result.failure()
        }
    }
}
