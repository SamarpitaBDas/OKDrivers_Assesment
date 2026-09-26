package com.example.okdrivers.service

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.okdrivers.data.repository.SyncQueueRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import android.util.Log

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val syncQueueRepository: SyncQueueRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val unsyncedItems = syncQueueRepository.getUnsyncedItems()
            if (unsyncedItems.isEmpty()) {
                return Result.success()
            }

            Log.d("OKDRIVER_SYNC", "SyncWorker: Starting sync for ${unsyncedItems.size} items.")

            for (item in unsyncedItems) {
                // Simulate backend sync delay / success
                kotlinx.coroutines.delay(100L)
                syncQueueRepository.markSynced(item.id)
                Log.d("OKDRIVER_SYNC", "Synced entity [${item.entityType}]: ${item.entityId}")
            }

            val remaining = syncQueueRepository.getUnsyncedItems()
            if (remaining.isEmpty()) {
                Result.success()
            } else {
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e("OKDRIVER_SYNC", "SyncWorker failed", e)
            Result.retry()
        }
    }
}
