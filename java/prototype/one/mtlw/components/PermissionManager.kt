package prototype.one.mtlw.components

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

object PermissionManager {
    fun hasPermission(context: Context, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    fun shouldShowRationale(activity: Activity, permission: String): Boolean {
        return ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
    }

    fun openAppSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
        }
        context.startActivity(intent)
    }
}

@Composable
fun PermissionHandler(
    permission: String,
    rationaleMessage: String,
    deniedMessage: String,
    onGranted: () -> Unit,
    onDenied: () -> Unit = {}
) {
    val context = LocalContext.current
    val activity = context as? Activity
    var showRationale by remember { mutableStateOf(false) }
    var showDenied by remember { mutableStateOf(false) }
    var permissionRequested by remember { mutableStateOf(false) }
    val permissionGranted = PermissionManager.hasPermission(context, permission)

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            onGranted()
        } else {
            if (activity != null && PermissionManager.shouldShowRationale(activity, permission)) {
                showRationale = true
            } else {
                showDenied = true
                onDenied()
            }
        }
    }

    LaunchedEffect(Unit) {
        if (!permissionGranted && !permissionRequested) {
            permissionRequested = true
            launcher.launch(permission)
        } else if (permissionGranted) {
            onGranted()
        }
    }

    if (showRationale) {
        AlertDialog(
            containerColor = Color.White,
            titleContentColor = Color.Black,
            textContentColor = Color.Black,
            onDismissRequest = { showRationale = false },
            title = { Text("Permission Needed", color = Color.Black) },
            text = { Text(rationaleMessage, color = Color.Black) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRationale = false
                        launcher.launch(permission)
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF222222))
                ) { Text("Allow") }
            },
            dismissButton = {
                TextButton(
                    onClick = { 
                        showRationale = false
                        onDenied()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF222222))
                ) { Text("Cancel") }
            }
        )
    }

    if (showDenied) {
        AlertDialog(
            containerColor = Color.White,
            titleContentColor = Color.Black,
            textContentColor = Color.Black,
            onDismissRequest = { showDenied = false },
            title = { Text("Permission Denied", color = Color.Black) },
            text = { Text(deniedMessage, color = Color.Black) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDenied = false
                        PermissionManager.openAppSettings(context)
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF222222))
                ) { Text("Open Settings") }
            },
            dismissButton = {
                TextButton(
                    onClick = { 
                        showDenied = false
                        onDenied()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF222222))
                ) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun RequestPermission(
    permission: String,
    rationaleMessage: String?,
    deniedMessage: String?,
    onGranted: () -> Unit,
    onDenied: () -> Unit = {}
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val actualPermission = when (permission) {
        Manifest.permission.READ_EXTERNAL_STORAGE ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.READ_MEDIA_IMAGES else Manifest.permission.READ_EXTERNAL_STORAGE
        Manifest.permission.READ_MEDIA_IMAGES ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.READ_MEDIA_IMAGES else Manifest.permission.READ_EXTERNAL_STORAGE
        Manifest.permission.POST_NOTIFICATIONS ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.POST_NOTIFICATIONS else ""
        else -> permission
    }

    val isGranted = PermissionManager.hasPermission(context, actualPermission)
    
    if (isGranted) {
        LaunchedEffect(Unit) {
            onGranted()
        }
    } else if (activity != null && actualPermission.isNotEmpty()) {
        PermissionHandler(
            permission = actualPermission,
            rationaleMessage = rationaleMessage.toString(),
            deniedMessage = deniedMessage.toString(),
            onGranted = onGranted,
            onDenied = onDenied
        )
    } else if (actualPermission.isEmpty()) {
        LaunchedEffect(Unit) {
            onGranted()
        }
    }
} 