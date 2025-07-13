package com.aidbud.ui.components.cameraoverlay

import android.os.CountDownTimer
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import android.content.ContentValues
import android.provider.MediaStore
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.padding

import com.aidbud.ui.components.cameraoverlay.camerabar.FlashMode
import com.aidbud.ui.components.cameraoverlay.camerabar.CameraBar
import com.aidbud.data.settings.SettingsViewModel
import com.aidbud.data.cache.draftmessage.GlobalCacheViewModel

fun saveToAlbum(context: Context, sourceUri: Uri, displayName: String, isVideo: Boolean) {
    val resolver = context.contentResolver
    val folderName = "Pictures/AidBud"
    val mimeType = if (isVideo) "video/mp4" else "image/jpeg"

    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
        put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
        put(MediaStore.MediaColumns.RELATIVE_PATH, folderName)
        put(MediaStore.MediaColumns.IS_PENDING, 1)
    }

    val collectionUri = if (isVideo) {
        MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
    } else {
        MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
    }

    val destUri = resolver.insert(collectionUri, contentValues) ?: return

    resolver.openInputStream(sourceUri).use { inputStream ->
        resolver.openOutputStream(destUri).use { outputStream ->
            if (inputStream != null && outputStream != null) {
                inputStream.copyTo(outputStream)
            }
        }
    }

    contentValues.clear()
    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
    resolver.update(destUri, contentValues, null, null)
}


@Composable
fun CameraOverlay(
    modifier: Modifier = Modifier,
    settingsViewModel: SettingsViewModel,
    cacheViewModel: GlobalCacheViewModel,
    onPhotoTaken: (Uri) -> Unit,
    onVideoTaken: (Uri) -> Unit
) {
    val currentConversationId: Int = cacheViewModel.getCurrentConversationId()!!
    val saveInAlbum by settingsViewModel.saveInAlbum.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor: ExecutorService = remember { Executors.newSingleThreadExecutor() }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.RECORD_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED
        )
    }

    // Launcher for requesting camera and audio permissions
    val requestPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasCameraPermission = permissions[Manifest.permission.CAMERA] == true &&
                permissions[Manifest.permission.RECORD_AUDIO] == true
        if (!hasCameraPermission) {
            Toast.makeText(context, "Camera and Audio permissions are required.", Toast.LENGTH_LONG).show()
        }
    }

    // Request permissions when the composable first appears
    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            requestPermissionLauncher.launch(
                arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
            )
        }
    }

    // CameraX Use Cases
    val preview = remember { Preview.Builder().build() }
    val imageCapture = remember { ImageCapture.Builder().build() }
    val recorder = remember { Recorder.Builder().setQualitySelector(androidx.camera.video.QualitySelector.from(androidx.camera.video.Quality.HD)).build() }
    val videoCapture = remember { VideoCapture.withOutput(recorder) }

    // State for current camera selector (front/back)
    var cameraSelector by remember { mutableStateOf(CameraSelector.DEFAULT_BACK_CAMERA) }
    // State for current flash mode
    var currentFlashMode by remember { mutableStateOf(FlashMode.Off) }

    // State for video recording
    var recording: Recording? by remember { mutableStateOf(null) }
    var recordingTimer: CountDownTimer? = null
    var isRecording by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // --- CameraX Binding Logic ---
    DisposableEffect(lifecycleOwner, cameraSelector, hasCameraPermission) {
        // Only proceed with camera binding if permissions are granted
        if (hasCameraPermission) {
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                try {
                    // Unbind all use cases before rebinding
                    cameraProvider.unbindAll()

                    // Bind use cases to camera
                    val camera = cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageCapture,
                        videoCapture
                    )

                    // Set initial flash mode
                    imageCapture.flashMode = when (currentFlashMode) {
                        FlashMode.Auto -> ImageCapture.FLASH_MODE_AUTO
                        FlashMode.On -> ImageCapture.FLASH_MODE_ON
                        FlashMode.Off -> ImageCapture.FLASH_MODE_OFF
                    }

                } catch (exc: Exception) {
                    Log.e("CameraScreen", "Use case binding failed", exc)
                    Toast.makeText(context, "Failed to start camera: ${exc.message}", Toast.LENGTH_LONG).show()
                }
            }, ContextCompat.getMainExecutor(context))
        }

        // This onDispose block is always the last expression returned by DisposableEffect.
        // Its content depends on whether permissions were granted.
        onDispose {
            // Only attempt to shutdown executor and unbind camera if permissions were granted
            // and camera was potentially bound.
            if (hasCameraPermission) { // Check hasCameraPermission again here for safety
                cameraExecutor.shutdown()
                try {
                    // Getting the cameraProvider might throw if it's not initialized,
                    // but it should be safe here if hasCameraPermission is true.
                    val cameraProvider = ProcessCameraProvider.getInstance(context).get()
                    cameraProvider.unbindAll()
                } catch (e: Exception) {
                    Log.e("CameraScreen", "Error unbinding camera during dispose: ${e.message}")
                }
            }
        }
    }

    // --- Camera Preview UI ---
    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                // Create a PreviewView to display the camera feed
                PreviewView(ctx).apply {
                    this.scaleType = PreviewView.ScaleType.FILL_CENTER
                    preview.setSurfaceProvider(this.surfaceProvider)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // --- Implementations for CameraBar Callbacks ---
        // These functions will be passed down to your CameraBar Composable
        // and handle the actual camera operations.

        // Function to handle taking a photo
        val onTakePhoto: () -> Unit = takePhoto@{
            if (!hasCameraPermission) {
                Toast.makeText(context, "Camera permission not granted.", Toast.LENGTH_SHORT).show()
                return@takePhoto
            } else {

                val name = SimpleDateFormat(
                    "yyyy-MM-dd-HH-mm-ss-SSS",
                    Locale.US
                ).format(System.currentTimeMillis())
                val photoFile = File(
                    context.filesDir,
                    "IMG_${name}.jpg"
                ) // Using internal storage for simplicity
                val photoUri = Uri.fromFile(photoFile)

                val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

                imageCapture.takePicture(
                    outputOptions,
                    cameraExecutor,
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onError(exc: ImageCaptureException) {
                            Log.e("CameraScreen", "Photo capture failed: ${exc.message}", exc)
                            Toast.makeText(
                                context,
                                "Photo capture failed: ${exc.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                        override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                            val msg = "Photo capture succeeded: ${output.savedUri}"
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            Log.d("CameraScreen", msg)
                            onPhotoTaken(photoUri)
                            if (saveInAlbum) {
                                saveToAlbum(context, photoUri, name, false)
                            }
                        }
                    }
                )
            }
        }

        // Function to handle starting video recording
        val onStartVideo: () -> Unit = startVideo@{
            if (!hasCameraPermission) {
                Toast.makeText(context, "Camera and Audio permissions not granted.", Toast.LENGTH_SHORT).show()
                return@startVideo
            } else {

                // Stop any existing recording
                recording?.stop()
                recording = null
                recordingTimer?.cancel()
                recordingTimer = null
                isRecording = false

                val durationLeftMillis = cacheViewModel.getDurationLeft(currentConversationId)
                if (durationLeftMillis <= 0L) {
                    Toast.makeText(context, "You have reached the video recording limit.", Toast.LENGTH_SHORT).show()
                    return@startVideo
                }

                val name = SimpleDateFormat(
                    "yyyy-MM-dd-HH-mm-ss-SSS",
                    Locale.US
                ).format(System.currentTimeMillis())
                val videoFile = File(
                    context.filesDir,
                    "VID_${name}.mp4"
                ) // Using internal storage for simplicity
                val videoUri = Uri.fromFile(videoFile)

                val mediaStoreOutput = MediaStoreOutputOptions.Builder(
                    context.contentResolver,
                    android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI // Use external for easier access
                )
                    .setContentValues(
                        android.content.ContentValues().apply {
                            put(
                                android.provider.MediaStore.Video.Media.DISPLAY_NAME,
                                "VID_${name}.mp4"
                            )
                        }
                    )
                    .build()

                isRecording = true
                recording = videoCapture.output
                    .prepareRecording(context, mediaStoreOutput)
                    .withAudioEnabled() // Enable audio recording
                    .start(ContextCompat.getMainExecutor(context)) { recordEvent ->
                        when (recordEvent) {
                            is VideoRecordEvent.Start -> {
                                Log.d("CameraScreen", "Video recording started.")
                                Toast.makeText(context, "Recording started!", Toast.LENGTH_SHORT)
                                    .show()
                                recordingTimer = object : CountDownTimer(durationLeftMillis, 1000) {
                                    override fun onTick(millisUntilFinished: Long) {
                                        // Optional: update UI with remaining seconds
                                    }

                                    override fun onFinish() {
                                        // Time's up - stop recording automatically
                                        if (isRecording) {
                                            Toast.makeText(context, "Max recording duration reached. Stopping...", Toast.LENGTH_SHORT).show()
                                            recording?.stop()
                                            recordingTimer = null
                                        }
                                    }
                                }.start()
                            }

                            is VideoRecordEvent.Finalize -> {
                                if (!recordEvent.hasError()) {
                                    isRecording = false
                                    val msg =
                                        "Video capture succeeded: ${recordEvent.outputResults.outputUri}"
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                    Log.d("CameraScreen", msg)
                                    onVideoTaken(videoUri)
                                    if (saveInAlbum) {
                                        saveToAlbum(context, videoUri, name, true)
                                    }
                                } else {
                                    isRecording = false
                                    recording?.close()
                                    recording = null
                                    val msg = "Video capture failed: ${recordEvent.error}"
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                    Log.e("CameraScreen", msg)
                                }
                            }

                            else -> {
                                // Handle other events like Pause, Resume, Status
                            }
                        }
                }
            }
        }

        // Function to handle stopping video recording
        val onStopVideo: () -> Unit = {
            if (recording != null) {
                recording?.stop()
                recording = null
                Log.d("CameraScreen", "Video recording stopped.")
                Toast.makeText(context, "Recording stopped!", Toast.LENGTH_SHORT).show()
                // onStopVideo() is called inside Finalize event for success/failure
            } else {
                Toast.makeText(context, "No active recording to stop.", Toast.LENGTH_SHORT).show()
            }
        }

        // Function to handle flash mode change
        val onFlashModeChange: (FlashMode) -> Unit = { mode ->
            currentFlashMode = mode // Update the state with the enum value
            imageCapture.flashMode = when (mode) { // Map enum to CameraX int constant
                FlashMode.Auto -> ImageCapture.FLASH_MODE_AUTO
                FlashMode.On -> ImageCapture.FLASH_MODE_ON
                FlashMode.Off -> ImageCapture.FLASH_MODE_OFF
            }
            Toast.makeText(context, "Flash mode set to ${mode.name}", Toast.LENGTH_SHORT).show() // Use enum name for display
        }

        // Function to handle camera flip
        val onCameraFlipClick: () -> Unit = {
            cameraSelector = if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
                CameraSelector.DEFAULT_FRONT_CAMERA
            } else {
                CameraSelector.DEFAULT_BACK_CAMERA
            }
            Toast.makeText(context, "Camera flipped!", Toast.LENGTH_SHORT).show()
        }

        CameraBar(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(10.dp),
            isRecording = isRecording,
            onTakePhoto = onTakePhoto,
            onStartVideo = onStartVideo,
            onStopVideo = onStopVideo,
            onFlashModeChange = onFlashModeChange,
            onCameraFlipClick = onCameraFlipClick
        )
    }
}
