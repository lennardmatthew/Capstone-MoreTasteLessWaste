package prototype.one.mtlw.notifications

import android.content.Context
import androidx.work.*
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.TimeUnit

object ExpiryNotificationHelper {
    private const val NOTIFICATION_DAYS_BEFORE = 3L
    private const val NOTIFICATION_CHANNEL_ID = "expiry_notifications"
    private const val NOTIFICATION_CHANNEL_NAME = "Expiry Notifications"

    fun scheduleExpiryNotification(
        context: Context,
        expiryDateId: String,
        itemName: String,
        expiryDate: LocalDate
    ) {
        // Schedule 7 days before
        scheduleNotificationForDaysBefore(context, expiryDateId + "_7", itemName, expiryDate, 7L)
        // Schedule 3 days before
        scheduleNotificationForDaysBefore(context, expiryDateId + "_3", itemName, expiryDate, 3L)
    }

    private fun scheduleNotificationForDaysBefore(
        context: Context,
        notificationId: String,
        itemName: String,
        expiryDate: LocalDate,
        daysBefore: Long
    ) {
        val notifyTime = expiryDate.minusDays(daysBefore)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        val delay = notifyTime - System.currentTimeMillis()
        if (delay <= 0 || expiryDate.isBefore(LocalDate.now())) {
            return
        }
        val data = workDataOf(
            "itemName" to itemName,
            "expiryDateId" to notificationId,
            "expiryDate" to expiryDate.toString(),
            "daysUntilExpiry" to daysBefore.toString()
        )
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .setRequiresBatteryNotLow(true)
            .build()
        val workRequest = OneTimeWorkRequestBuilder<ExpiryNotificationWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .setConstraints(constraints)
            .addTag(notificationId)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "expiry_notify_$notificationId",
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }

    fun cancelExpiryNotification(context: Context, expiryDateId: String) {
        WorkManager.getInstance(context).cancelUniqueWork("expiry_notify_${expiryDateId}_7")
        WorkManager.getInstance(context).cancelUniqueWork("expiry_notify_${expiryDateId}_3")
    }

    fun rescheduleAllNotifications(context: Context, expiryDates: List<Pair<String, LocalDate>>) {
        expiryDates.forEach { (id, date) ->
            scheduleExpiryNotification(context, id, "Item", date)
        }
    }
} 