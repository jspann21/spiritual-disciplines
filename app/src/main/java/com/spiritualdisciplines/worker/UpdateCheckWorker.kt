package com.spiritualdisciplines.worker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.spiritualdisciplines.R
import com.spiritualdisciplines.update.AppUpdate
import com.spiritualdisciplines.update.UpdateCheckResult
import com.spiritualdisciplines.update.UpdateCheckStore
import com.spiritualdisciplines.update.UpdateChecker
import java.util.concurrent.TimeUnit

class UpdateCheckWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result {
        val store = UpdateCheckStore(applicationContext)
        return when (val result = UpdateChecker(applicationContext).check()) {
            is UpdateCheckResult.Available -> {
                if (!store.wasNotified(result.update.versionName)) {
                    if (showNotification(result.update)) {
                        store.markNotified(result.update.versionName)
                    }
                }
                Result.success()
            }
            is UpdateCheckResult.UpToDate -> Result.success()
            is UpdateCheckResult.Error -> Result.success()
        }
    }

    private fun showNotification(update: AppUpdate): Boolean {
        createNotificationChannel()

        val releaseIntent = Intent(Intent.ACTION_VIEW, update.releaseUrl.toUri())
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            releaseIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Spiritual Disciplines update available")
            .setContentText("Version ${update.versionName} is ready to download.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            NotificationManagerCompat.from(applicationContext).notify(NOTIFICATION_ID, notification)
            return true
        }
        return false
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "App Updates",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Notifications when a new app version is available"
        }
        applicationContext.getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }

    companion object {
        private const val WORK_NAME = "app_update_check"
        private const val CHANNEL_ID = "app_updates"
        private const val NOTIFICATION_ID = 2

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val workRequest = PeriodicWorkRequestBuilder<UpdateCheckWorker>(1, TimeUnit.DAYS)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        }
    }
}
