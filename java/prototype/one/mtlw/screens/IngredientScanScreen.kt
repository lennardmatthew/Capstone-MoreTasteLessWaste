package prototype.one.mtlw.screens

import android.Manifest
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import prototype.one.mtlw.components.CharacterOverlay
import prototype.one.mtlw.components.RequestPermission
import prototype.one.mtlw.utils.TFLiteFoodClassifier
// import prototype.one.mtlw.utils.YuvToRgbConverter // Not needed for ImageCapture
import prototype.one.mtlw.R
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject

@OptIn(ExperimentalGetImage::class)
@Composable
fun IngredientScanScreen(navController: NavController) {
    var hasPermission by remember { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    
    // Scanners
    val barcodeScanner = remember {
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_EAN_13,
                Barcode.FORMAT_EAN_8,
                Barcode.FORMAT_UPC_A,
                Barcode.FORMAT_UPC_E,
                Barcode.FORMAT_CODE_128,
                Barcode.FORMAT_QR_CODE
            )
            .build()
        BarcodeScanning.getClient(options)
    }
    val context = LocalContext.current
	val foodClassifier = remember { TFLiteFoodClassifier(context) }
    // val yuvConverter = remember { YuvToRgbConverter() } // Not needed for ImageCapture
    
    // UI state for thought bubble
    var bubbleLabel by remember { mutableStateOf("Point camera at \n food or Barcode") }

    var bubblePrimaryText by remember { mutableStateOf<String?>(null) }
    var bubbleSecondaryText by remember { mutableStateOf<String?>(null) }
    var bubbleOnPrimary: (() -> Unit)? by remember { mutableStateOf(null) }
    var bubbleOnSecondary: (() -> Unit)? by remember { mutableStateOf(null) }
    var isProcessing by remember { mutableStateOf(false) }
    
    // Detection results
    var detectedItem by remember { mutableStateOf<String?>(null) }
    var detectedCalories by remember { mutableStateOf<Int?>(null) }
    var detectionType by remember { mutableStateOf<DetectionType?>(null) }
    
    // Ensure initial state is set when screen loads
    LaunchedEffect(Unit) {
        bubbleLabel = "Point camera at \n food or Barcode"
        bubblePrimaryText = null
        bubbleSecondaryText = null
        bubbleOnPrimary = null
        bubbleOnSecondary = null
    }

	RequestPermission(
		permission = Manifest.permission.CAMERA,
        rationaleMessage = "This app needs camera access to scan food items and barcodes. Please grant camera permission.",
        deniedMessage = "Camera permission is required to scan food items and barcodes. Please enable it in your device settings.",
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
                        
                        // Try barcode detection first (faster)
                        Log.d("IngredientScan", "Starting barcode detection...")
                        val barcodeResult = detectBarcode(bitmap, barcodeScanner)
                        Log.d("IngredientScan", "Barcode result: $barcodeResult")
                        if (barcodeResult != null) {
                            // Barcode detected - fetch product info
                            val productInfo = fetchProductInfo(barcodeResult)
                            if (productInfo != null) {
                                detectedItem = productInfo.first
                                detectedCalories = productInfo.second
                                detectionType = DetectionType.BARCODE
                                
                                bubbleLabel = "Barcode detected: ${productInfo.first}"
                                bubblePrimaryText = "Use in Recipe"
                                bubbleSecondaryText = "Scan Again"
                                bubbleOnPrimary = {
                                    // Pass ingredient to recipe generator
                                    navController.previousBackStackEntry?.savedStateHandle?.set(
                                        "ingredientName",
                                        productInfo.first
                                    )
                                    navController.popBackStack()
                                }
                                bubbleOnSecondary = {
                                    // Reset for another scan
                                    bubbleLabel = "Point camera at \n food or Barcode"
                                    bubblePrimaryText = null
                                    bubbleSecondaryText = null
                                    bubbleOnPrimary = null
                                    bubbleOnSecondary = null
                                    detectedItem = null
                                    detectedCalories = null
                                    detectionType = null
                                }
                            } else {
                                bubbleLabel = "no product info found"
                                bubblePrimaryText = "Back"
                                bubbleSecondaryText = "Search bar"
                                bubbleOnPrimary = {
                                    bubbleLabel = "Point camera at \n food or Barcode"
                                    bubblePrimaryText = null
                                    bubbleSecondaryText = null
                                    bubbleOnPrimary = null
                                    bubbleOnSecondary = null
                                }
                                bubbleOnSecondary = { navController.popBackStack() }
                            }
                        } else {
                            // No barcode - try food classification
                            Log.d("IngredientScan", "Starting food classification...")
                            val foodResult = foodClassifier.classify(bitmap)
                            Log.d("IngredientScan", "Food classification result: $foodResult")
                            if (foodResult != null) {
                                detectedItem = foodResult.first
                                detectedCalories = null // TFLite doesn't provide calories
                                detectionType = DetectionType.FOOD
                                
                                bubbleLabel = "Food detected: ${foodResult.first}"
                                bubblePrimaryText = "Use in Recipe"
                                bubbleSecondaryText = "Scan Again"
                                bubbleOnPrimary = {
                                    // Pass ingredient to recipe generator
                                    navController.previousBackStackEntry?.savedStateHandle?.set(
                                        "ingredientName",
                                        foodResult.first
                                    )
                                    navController.popBackStack()
                                }
                                bubbleOnSecondary = {
                                    // Reset for another scan
                                    bubbleLabel = "Point camera at food item or barcode"
                                    bubblePrimaryText = null
                                    bubbleSecondaryText = null
                                    bubbleOnPrimary = null
                                    bubbleOnSecondary = null
                                    detectedItem = null
                                    detectedCalories = null
                                    detectionType = null
                                }
                            } else {
                                // Nothing detected
                                bubbleLabel = "No food item or \n barcode detected"
                                bubblePrimaryText = "Back"
                                bubbleSecondaryText = "Search bar"
                                bubbleOnPrimary = {
                                    bubbleLabel = "Point camera at \n food or Barcode"
                                    bubblePrimaryText = null
                                    bubbleSecondaryText = null
                                    bubbleOnPrimary = null
                                    bubbleOnSecondary = null
                                }
                                bubbleOnSecondary = { navController.popBackStack() }
                            }
                        }
                    } catch (e: Exception) {
                        bubbleLabel = "Error: ${e.message}"
                        bubblePrimaryText = "Back"
                        bubbleSecondaryText = "Search bar"
                        bubbleOnPrimary = {
                            bubbleLabel = "Point camera at \n food or Barcode"
                            bubblePrimaryText = null
                            bubbleSecondaryText = null
                            bubbleOnPrimary = null
                            bubbleOnSecondary = null
                        }
                        bubbleOnSecondary = { navController.popBackStack() }
                    } finally {
                        isProcessing = false
                    }
                }
            },
            onError = { error ->
                bubbleLabel = "Camera error: $error"
                bubblePrimaryText = "Back"
                bubbleSecondaryText = "Search bar"
                bubbleOnPrimary = {
                    bubbleLabel = "Point camera at \n food or Barcode"
                    bubblePrimaryText = null
                    bubbleSecondaryText = null
                    bubbleOnPrimary = null
                    bubbleOnSecondary = null
                }
                bubbleOnSecondary = { navController.popBackStack() }
            },
            onBack = { navController.popBackStack() }
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
        cloudScale = 0.7f
    )

    DisposableEffect(Unit) {
        onDispose {
            barcodeScanner.close()
            foodClassifier.close()
        }
    }
}

@Composable
private fun CameraPreview(
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    onImageCaptured: (Bitmap) -> Unit,
    onError: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val cameraExecutor: ExecutorService = remember { Executors.newSingleThreadExecutor() }
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    var isCapturing by remember { mutableStateOf(false) }
    var camera: androidx.camera.core.Camera? by remember { mutableStateOf(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        // Camera preview
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
                                    androidx.camera.core.FocusMeteringAction.Builder(
                                        previewView.meteringPointFactory.createPoint(
                                            previewView.width / 2f,
                                            previewView.height / 2f
                                        ),
                                        androidx.camera.core.FocusMeteringAction.FLAG_AF
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

// Helper functions
private suspend fun detectBarcode(bitmap: Bitmap, scanner: com.google.mlkit.vision.barcode.BarcodeScanner): String? {
    return withContext(Dispatchers.Default) {
        try {
            Log.d("IngredientScan", "Creating InputImage from bitmap...")
            val inputImage = InputImage.fromBitmap(bitmap, 0)
            Log.d("IngredientScan", "Processing barcode with scanner...")
            val result = scanner.process(inputImage).await()
            Log.d("IngredientScan", "Barcode processing complete, found ${result.size} results")
            result.firstOrNull()?.rawValue
        } catch (e: Exception) {
            Log.e("IngredientScan", "Barcode detection error: ${e.message}")
            e.printStackTrace()
            null
        }
    }
}

private suspend fun fetchProductInfo(barcode: String): Pair<String, Int>? {
    return withContext(Dispatchers.IO) {
        try {
            Log.d("IngredientScan", "Fetching product info for barcode: $barcode")
            val url = URL("https://world.openfoodfacts.org/api/v0/product/$barcode.json")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            
            val responseCode = connection.responseCode
            if (responseCode == 200) {
                val response = connection.inputStream.bufferedReader().readText()
                val json = JSONObject(response)
                val product = json.optJSONObject("product")
                
                if (product != null) {
                    val name = product.optString("product_name", null)
                    val nutriments = product.optJSONObject("nutriments")
                    val kcal = nutriments?.optDouble("energy-kcal_100g", -1.0) ?: -1.0
                    
                    if (name != null && kcal > 0) {
                        Pair(name, kcal.toInt())
                    } else null
                } else null
            } else null
        } catch (e: Exception) {
            Log.e("IngredientScan", "Product info fetch error: ${e.message}")
            null
        }
    }
}

enum class DetectionType {
    BARCODE,
    FOOD
} 