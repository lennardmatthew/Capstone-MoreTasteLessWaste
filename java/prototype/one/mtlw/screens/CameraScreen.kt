package prototype.one.mtlw.screens

import android.Manifest
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.NavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import prototype.one.mtlw.components.RequestPermission
import prototype.one.mtlw.components.CharacterOverlay
import prototype.one.mtlw.R
import prototype.one.mtlw.utils.UltraFastExpiryDetector
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Composable
fun CameraScreen(
    navController: NavController,
    onError: (String) -> Unit,
    onBack: () -> Unit
) {
    var hasPermission by remember { mutableStateOf(false) }
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    
    // Use the new ultra-fast detector
    val expiryDetector = remember { UltraFastExpiryDetector() }
    
    // UI state
    var bubbleLabel by remember { mutableStateOf("Point camera at date") }
    var bubblePrimaryText by remember { mutableStateOf<String?>(null) }
    var bubbleSecondaryText by remember { mutableStateOf<String?>(null) }
    var bubbleOnPrimary: (() -> Unit)? by remember { mutableStateOf(null) }
    var bubbleOnSecondary: (() -> Unit)? by remember { mutableStateOf(null) }
    var isProcessing by remember { mutableStateOf(false) }

    // Ensure initial state is set when screen loads
    LaunchedEffect(Unit) {
        bubbleLabel = "Point camera at date"
        bubblePrimaryText = null
        bubbleSecondaryText = null
        bubbleOnPrimary = null
        bubbleOnSecondary = null
    }

    RequestPermission(
        permission = Manifest.permission.CAMERA,
        rationaleMessage = "This app needs camera access to scan expiry dates. Please grant camera permission.",
        deniedMessage = "Camera permission is required to scan expiry dates. Please enable it in your device settings.",
        onGranted = { hasPermission = true }
    )

    if (hasPermission) {
        CameraPreview(
            lifecycleOwner = lifecycleOwner,
            onImageCaptured = { bitmap ->
                scope.launch {
                    try {
                        isProcessing = true
                        bubbleLabel = "Processing image..."
                        bubblePrimaryText = null
                        bubbleSecondaryText = null
                        bubbleOnPrimary = null
                        bubbleOnSecondary = null
                        
                        // Use the ultra-fast detector
                        val detectedDate = expiryDetector.detectExpiryDate(bitmap)
                        
                        if (detectedDate != null) {
                            // Success - show detected date in Month/Day/Year format
                            val formatter = java.time.format.DateTimeFormatter.ofPattern("MMM dd, yyyy")
                            val formattedDate = detectedDate.format(formatter)
                            bubbleLabel = "Detected: $formattedDate"
                            bubblePrimaryText = "Save"
                            bubbleSecondaryText = "Back"
                            bubbleOnPrimary = {
                                // Save the detected date
                                navController.currentBackStackEntry?.savedStateHandle?.set("detectedDate", detectedDate.toString())
                                onError("") // Clear any previous errors
                            }
                            bubbleOnSecondary = { onBack() }
                        } else {
                            // No date detected
                            bubbleLabel = "No date detected"
                            bubblePrimaryText = "Try again"
                            bubbleSecondaryText = "Enter manually"
                            bubbleOnPrimary = { 
                                // Reset for another attempt
                                bubbleLabel = "Point camera at date"
                                bubblePrimaryText = null
                                bubbleSecondaryText = null
                                bubbleOnPrimary = null
                                bubbleOnSecondary = null
                            }
                            bubbleOnSecondary = { onBack() }
                        }
                    } catch (e: Exception) {
                        bubbleLabel = "Error: ${e.message}"
                        bubblePrimaryText = "Try again"
                        bubbleSecondaryText = "Enter manually"
                        bubbleOnPrimary = { 
                            bubbleLabel = "Point camera at date"
                            bubblePrimaryText = null
                            bubbleSecondaryText = null
                            bubbleOnPrimary = null
                            bubbleOnSecondary = null
                        }
                        bubbleOnSecondary = { onBack() }
                    } finally {
                        isProcessing = false
                    }
                }
            },
            onError = onError,
            onBack = onBack
        )
    }

    // Character overlay with thought bubble
    CharacterOverlay(
        modifier = Modifier.fillMaxSize(),
        characterResId = R.drawable.repo_main,
        label = bubbleLabel,
        visible = true,
        showSmall = true,
        onPrimary = bubbleOnPrimary,
        onSecondary = bubbleOnSecondary,
        primaryText = bubblePrimaryText,
        secondaryText = bubbleSecondaryText,
        cloudScale = 0.7f // Made even smaller to ensure it doesn't block the scanning area
    )

    DisposableEffect(Unit) {
        onDispose {
            expiryDetector.close()
        }
    }
}

@Composable
private fun CameraPreview(
    lifecycleOwner: LifecycleOwner,
    onImageCaptured: (Bitmap) -> Unit,
    onError: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val cameraExecutor: ExecutorService = remember { Executors.newSingleThreadExecutor() }
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    var isCapturing by remember { mutableStateOf(false) }
    var camera: Camera? by remember { mutableStateOf(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        // Camera preview (NO live analysis - just preview)
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                cameraProviderFuture.addListener({
                    try {
                        val cameraProvider = cameraProviderFuture.get()
                        
                        // Configure preview only (no live analysis)
                        val preview = Preview.Builder()
                            .build()
                            .also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }

                        // Configure image capture for high quality
                        imageCapture = ImageCapture.Builder()
                            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                            .setTargetRotation(previewView.display.rotation)
                            .build()

                        // Configure camera selector
                        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                        try {
                            cameraProvider.unbindAll()
                            camera = cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                imageCapture
                            )

                            // Configure camera for best quality
                            camera?.let { cam ->
                                // Auto focus
                                cam.cameraControl.enableTorch(false)
                                
                                // Set focus mode to continuous
                                cam.cameraControl.cancelFocusAndMetering()
                                cam.cameraControl.startFocusAndMetering(
                                    FocusMeteringAction.Builder(
                                        previewView.meteringPointFactory.createPoint(
                                            previewView.width / 2f,
                                            previewView.height / 2f
                                        ),
                                        FocusMeteringAction.FLAG_AF
                                    ).build()
                                )
                            }
                        } catch (_: Exception) {
                            onError("Failed to start camera.")
                        }
                    } catch (_: Exception) {
                        onError("Failed to initialize camera.")
                    }
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // Focus guide overlay
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerX = size.width / 2
            val centerY = size.height / 2
            val focusSize = size.width * 0.6f

            // Draw focus rectangle
            drawRect(
                color = Color.White.copy(alpha = 0.3f),
                topLeft = Offset(centerX - focusSize/2, centerY - focusSize/2),
                size = androidx.compose.ui.geometry.Size(focusSize, focusSize),
                style = Stroke(width = 2f)
            )
        }

        // Back button
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .padding(16.dp)
                .align(Alignment.TopStart)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color.White
            )
        }

        // Capture button
        IconButton(
            onClick = {
                if (!isCapturing) {
                    isCapturing = true
                    val photoFile = File(
                        context.cacheDir,
                        "IMG_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.jpg"
                    )

                    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

                    imageCapture?.takePicture(
                        outputOptions,
                        ContextCompat.getMainExecutor(context),
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                scope.launch {
                                    try {
                                        val bitmap = withContext(Dispatchers.Default) {
                                            BitmapFactory.decodeFile(photoFile.absolutePath)
                                        }
                                        onImageCaptured(bitmap)
                                    } catch (_: Exception) {
                                        onError("Failed to process image.")
                                    } finally {
                                        photoFile.delete()
                                        isCapturing = false
                                    }
                                }
                            }

                            override fun onError(exception: ImageCaptureException) {
                                onError("Failed to capture image: ${exception.message}")
                                isCapturing = false
                            }
                        }
                    )
                }
            },
            modifier = Modifier
                .padding(16.dp)
                .align(Alignment.BottomCenter)
        ) {
            if (isCapturing) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            } else {
                Icon(
                    imageVector = Icons.Default.Camera,
                    contentDescription = "Capture",
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
            }
        }
    }

    // Cleanup
    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }
}



