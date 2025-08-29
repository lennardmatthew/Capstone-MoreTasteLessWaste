package prototype.one.mtlw.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import prototype.one.mtlw.MainActivity
import prototype.one.mtlw.R
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class ExpiryNotificationWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    companion object {
        private const val CHANNEL_ID = "expiry_notifications"
        private const val CHANNEL_NAME = "Expiry Notifications"
        private const val CHANNEL_DESCRIPTION = "Notifications for expiring items"
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override suspend fun doWork(): Result {
        try {
            val itemName = inputData.getString("itemName") ?: return Result.failure()
            val expiryDateStr = inputData.getString("expiryDate") ?: return Result.failure()
            val daysUntilExpiry = inputData.getString("daysUntilExpiry")?.toLongOrNull() ?: 3L

            // Parse the expiry date
            val expiryDate = LocalDate.parse(expiryDateStr)
            val formattedDate = expiryDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))

            // Create notification channel
            createNotificationChannel()

            // Create intent for notification tap
            val intent = Intent(applicationContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("navigate_to", "tracker")
            }
            val pendingIntent = PendingIntent.getActivity(
                applicationContext,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            // Build notification
            val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
                .setSmallIcon(R.drawable.repo_main)
                .setContentTitle("Expiry Reminder")
                .setContentText("$itemName expires in $daysUntilExpiry days ($formattedDate)")
                .setStyle(NotificationCompat.BigTextStyle()
                    .bigText("$itemName expires in $daysUntilExpiry days ($formattedDate)\nTap to view in the app."))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

            // Show notification
            NotificationManagerCompat.from(applicationContext)
                .notify(itemName.hashCode(), notification)

            return Result.success()
        } catch (e: Exception) {
            return Result.failure()
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = CHANNEL_DESCRIPTION
            enableLights(true)
            enableVibration(true)
        }

        NotificationManagerCompat.from(applicationContext)
            .createNotificationChannel(channel)
    }
} 