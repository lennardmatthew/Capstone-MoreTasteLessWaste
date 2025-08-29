package prototype.one.mtlw.notifications

import android.Manifest
import android.app.Activity
import android.content.Context
import android.os.Build
import androidx.activity.result.ActivityResultLauncher
import prototype.one.mtlw.components.PermissionManager
import androidx.compose.runtime.LaunchedEffect

object NotificationPermissionHelper {
    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            PermissionManager.hasPermission(context, Manifest.permission.POST_NOTIFICATIONS)
        } else {
            true // Permission not required on older versions
        }
    }

    fun requestNotificationPermission(activity: Activity, launcher: ActivityResultLauncher<String>) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
} 