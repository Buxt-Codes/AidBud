package com.aidbud.ui.components.cameraoverlay

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.CountDownTimer
import android.provider.MediaStore
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
import androidx.camera.video.VideoRecordEvent
import androidx.camera.video.VideoCapture
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.aidbud.data.cache.draftmessage.GlobalCacheViewModel
import com.aidbud.data.settings.SettingsViewModel
import com.aidbud.ui.components.cameraoverlay.camerabar.CameraBar
import com.aidbud.ui.components.cameraoverlay.camerabar.FlashMode
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.compose.ui.unit.dp

@Composable
fun CameraOverlay(
    modifier: Modifier = Modifier,
    conversationId: Long,
    settingsViewModel: SettingsViewModel,
    cacheViewModel: GlobalCacheViewModel,
    onPhotoTaken: (Uri) -> Unit,
    onVideoTaken: (Uri) -> Unit,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // No custom cameraExecutor is needed. CameraX manages its own internal threads
    // and for callbacks that interact with the UI, ContextCompat.getMainExecutor(context)
    // is the recommended and safest choice.

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
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

    // CameraX Use Cases are remembered to ensure stability across recompositions.
    // They are lifecycle-aware when bound.
    val preview = remember { Preview.Builder().build() }
    val imageCapture = remember { ImageCapture.Builder().build() }
    val recorder = remember {
        Recorder.Builder()
            .setQualitySelector(androidx.camera.video.QualitySelector.from(androidx.camera.video.Quality.HD))
            .build()
    }
    val videoCapture = remember(recorder) { VideoCapture.withOutput(recorder) }

    // State for current camera selector (front/back)
    var cameraSelector by remember { mutableStateOf(CameraSelector.DEFAULT_BACK_CAMERA) }
    // State for current flash mode
    var currentFlashMode by remember { mutableStateOf(FlashMode.Off) }

    // State for video recording
    var recording: Recording? by remember { mutableStateOf(null) }
    var recordingTimer: CountDownTimer? by remember { mutableStateOf(null) }
    var isRecording by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope() // Useful if you need to launch coroutines within callbacks

    // --- CameraX Binding Logic ---
    DisposableEffect(lifecycleOwner, cameraSelector, hasCameraPermission) {
        // Only proceed with camera binding if permissions are granted
        if (hasCameraPermission) {
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                try {
                    // Unbind all use cases before rebinding, crucial for camera flip or re-initialization
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

                    Log.d("CameraOverlay", "Camera bound successfully.")

                } catch (exc: Exception) {
                    // Log the error. In production, consider a more robust error handling
                    // like guiding the user to app settings or a retry mechanism.
                    Log.e("CameraOverlay", "Use case binding failed", exc)
                    Toast.makeText(context, "Failed to start camera: ${exc.message}", Toast.LENGTH_LONG).show()
                }
            }, ContextCompat.getMainExecutor(context)) // Use main executor for binding callbacks

            // onDispose block for cleaning up when the composable leaves the composition
            onDispose {
                Log.d("CameraOverlay", "CameraOverlay onDispose called.")
                // Stop any ongoing recording explicitly if it exists before unbinding
                recording?.stop()
                recording = null
                recordingTimer?.cancel()
                recordingTimer = null
                isRecording = false

                // Attempt to unbind all use cases when composable is disposed
                try {
                    val cameraProvider = ProcessCameraProvider.getInstance(context).get()
                    cameraProvider.unbindAll()
                    Log.d("CameraOverlay", "Camera unbound on dispose.")
                } catch (e: Exception) {
                    Log.e("CameraOverlay", "Error unbinding camera during dispose: ${e.message}")
                }
                // No custom executor to shut down here, as we're using MainExecutor for callbacks.
            }
        } else {
            // If permissions are not granted, ensure no camera binding attempts are made
            onDispose {
                Log.d("CameraOverlay", "CameraOverlay onDispose called, but permissions not granted, so no camera cleanup.")
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
                    // Attach the Preview use case to the PreviewView's surfaceProvider
                    preview.setSurfaceProvider(this.surfaceProvider)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // --- Implementations for CameraBar Callbacks ---

        // Function to handle taking a photo
        val onTakePhoto: () -> Unit = takePhoto@{
            if (!hasCameraPermission) {
                Toast.makeText(context, "Camera permission not granted.", Toast.LENGTH_SHORT).show()
                return@takePhoto // Use return@label to exit the lambda
            }

            val name = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US).format(System.currentTimeMillis())

            // Create ContentValues for the MediaStore entry
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, "IMG_${name}.jpg")
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/AidBud") // Save to a specific subfolder
            }

            // Build the OutputFileOptions to save directly to MediaStore
            val outputOptions = ImageCapture.OutputFileOptions.Builder(
                context.contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, // Use EXTERNAL_CONTENT_URI for public gallery
                contentValues
            ).build()

            imageCapture.takePicture(
                outputOptions,
                // Use the main executor for the callback to ensure thread safety for UI updates
                ContextCompat.getMainExecutor(context),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onError(exc: ImageCaptureException) {
                        Log.e("CameraOverlay", "Photo capture failed: ${exc.message}", exc)
                        Toast.makeText(
                            context,
                            "Photo capture failed: ${exc.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                        // CameraX usually attempts recovery for transient errors.
                        // For persistent errors, a full camera re-initialization might be needed,
                        // but this is often handled by CameraX's internal error recovery or
                        // by the DisposableEffect re-binding if the camera state truly changes.
                    }

                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                        val savedUri = output.savedUri
                        if (savedUri != null) {
                            val msg = "Photo capture succeeded: $savedUri"
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            Log.d("CameraOverlay", msg)
                            onPhotoTaken(savedUri) // Pass the actual saved URI
                        } else {
                            val msg = "Photo capture succeeded but URI is null."
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            Log.w("CameraOverlay", msg)
                        }
                    }
                }
            )
        }

        // Function to handle starting video recording
        val onStartVideo: () -> Unit = startVideo@{
            if (!hasCameraPermission) {
                Toast.makeText(context, "Camera and Audio permissions not granted.", Toast.LENGTH_SHORT).show()
                return@startVideo
            }

            // Crucial: Stop any *active* recording before starting a new one.
            // This ensures resources from a previous, potentially failed or interrupted,
            // recording are fully released. The Finalize event will clean up the state.
            recording?.stop()
            recording = null
            recordingTimer?.cancel()
            recordingTimer = null
            isRecording = false // Reset state before starting a new one

            val durationLeftMillis = cacheViewModel.getDurationLeft(conversationId)
            if (durationLeftMillis <= 0L) {
                Toast.makeText(context, "You have reached the video recording limit.", Toast.LENGTH_SHORT).show()
                return@startVideo
            }

            val name = SimpleDateFormat(
                "yyyy-MM-dd-HH-mm-ss-SSS",
                Locale.US
            ).format(System.currentTimeMillis())

            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, "VID_${name}.mp4")
                put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Movies/AidBud") // Save videos to Movies/AidBud
            }

            val mediaStoreOutputOptions = MediaStoreOutputOptions.Builder(
                context.contentResolver,
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI // Use EXTERNAL_CONTENT_URI for public gallery
            )
                .setContentValues(contentValues)
                .build()

            // Start the recording and observe its state
            recording = videoCapture.output
                .prepareRecording(context, mediaStoreOutputOptions)
                .withAudioEnabled()
                .start(ContextCompat.getMainExecutor(context)) { recordEvent -> // Use main executor for event callbacks
                    when (recordEvent) {
                        is VideoRecordEvent.Start -> {
                            Log.d("CameraOverlay", "Video recording started.")
                            isRecording = true // Set state when recording officially starts
                            onStartRecording() // Notify external callback

                            // Start countdown timer
                            recordingTimer = object : CountDownTimer(durationLeftMillis, 1000) {
                                override fun onTick(millisUntilFinished: Long) {
                                    // Optionally update UI for remaining time
                                }

                                override fun onFinish() {
                                    Log.d("CameraOverlay", "Video recording reached max duration. Stopping.")
                                    // Automatically stop if duration is met. This will trigger Finalize.
                                    recording?.stop()
                                }
                            }.start()
                        }
                        is VideoRecordEvent.Finalize -> {
                            Log.d("CameraOverlay", "VideoRecordEvent.Finalize received.")
                            // This block handles both successful completion and errors for video recording.
                            // All state cleanup related to the recording session should happen here.
                            isRecording = false // Recording has finalized (stopped or errored)
                            recordingTimer?.cancel() // Cancel any active timer
                            recordingTimer = null
                            recording = null // Clear the reference to the Recording object

                            if (recordEvent.hasError()) {
                                val errorMessage = "Video capture failed: ${recordEvent.error} - ${recordEvent.cause?.message}"
                                Log.e("CameraOverlay", errorMessage, recordEvent.cause) // Log the cause for better debugging
                                Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                            } else {
                                val savedUri = recordEvent.outputResults.outputUri
                                if (savedUri != null) {
                                    val msg = "Video capture succeeded: $savedUri"
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                    Log.d("CameraOverlay", msg)
                                    onVideoTaken(savedUri) // Pass the actual saved URI
                                } else {
                                    val msg = "Video capture succeeded but URI is null."
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                    Log.w("CameraOverlay", msg)
                                }
                            }
                            onStopRecording() // Notify external callback that recording has finalized (success or error)
                        }
                        else -> {
                            // Handle other events like Pause, Resume, Status if needed
                            Log.d("CameraOverlay", "VideoRecordEvent: $recordEvent")
                        }
                    }
                }
            // If 'start()' returns null, it means it failed to prepare/start immediately.
            if (recording == null) {
                isRecording = false
                onStopRecording()
                Toast.makeText(context, "Failed to start video recording immediately.", Toast.LENGTH_SHORT).show()
            }
        }

        // Function to handle stopping video recording
        val onStopVideo: () -> Unit = {
            if (recording != null && isRecording) { // Only attempt to stop if recording is active
                Log.d("CameraOverlay", "Attempting to stop video recording from onStopVideo.")
                recording?.stop() // This will asynchronously trigger the VideoRecordEvent.Finalize callback
                // IMPORTANT: Do NOT null out `recording`, `recordingTimer`, or reset `isRecording` here.
                // Let the `VideoRecordEvent.Finalize` callback handle the final state cleanup.
                // This prevents race conditions and ensures cleanup happens after the recording is truly finalized.
            } else {
                Toast.makeText(context, "No active recording to stop.", Toast.LENGTH_SHORT).show()
                Log.d("CameraOverlay", "onStopVideo called but no active recording.")
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
            Toast.makeText(context, "Flash mode set to ${mode.name}", Toast.LENGTH_SHORT).show()
        }

        // Function to handle camera flip
        val onCameraFlipClick: () -> Unit = {
            cameraSelector = if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
                CameraSelector.DEFAULT_FRONT_CAMERA
            } else {
                CameraSelector.DEFAULT_BACK_CAMERA
            }
            Toast.makeText(context, "Camera flipped!", Toast.LENGTH_SHORT).show()
            // The DisposableEffect will re-trigger binding automatically due to cameraSelector change.
        }

        // Only show the CameraBar if permissions are granted
        if (hasCameraPermission) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 10.dp) // or 16.dp if you prefer
            ) {
                CameraBar(
                    isRecording = isRecording,
                    onTakePhoto = onTakePhoto,
                    onStartVideo = onStartVideo,
                    onStopVideo = onStopVideo,
                    onFlashModeChange = onFlashModeChange,
                    onCameraFlipClick = onCameraFlipClick
                )
            }
        }
    }
}
