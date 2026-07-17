package com.example.aloka

import android.Manifest
import android.graphics.Paint
import android.os.Bundle
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.aloka.ui.AlertLevel
import com.example.aloka.ui.MainViewModel
import com.example.aloka.ui.theme.AlokaTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AlokaTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    CameraScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraScreen() {
    val camPerm = rememberPermissionState(Manifest.permission.CAMERA)
    LaunchedEffect(Unit) { if (!camPerm.status.isGranted) camPerm.launchPermissionRequest() }
    
    if (camPerm.status.isGranted) {
        CameraPreviewWithAI()
    } else {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Izin kamera dibutuhkan.")
        }
    }
}

private val BOX_COLORS = listOf(
    Color(0xFFFF3333), Color(0xFF33FF57), Color(0xFF3357FF), Color(0xFFFF33F5),
    Color(0xFFFFBF00), Color(0xFF00FFFF), Color(0xFFFF6600), Color(0xFF9900FF),
    Color(0xFF00FF99), Color(0xFFFF0099)
)

private val DANGER_CLASSES = setOf(1, 2, 4, 5, 8, 9, 10, 11, 12, 14, 16, 17, 19, 20)

@Composable
fun CameraPreviewWithAI(viewModel: MainViewModel = viewModel()) {
    val lifecycleOwner = LocalLifecycleOwner.current
    
    val alertState by viewModel.alertState.collectAsState()
    val isBlackScreen by viewModel.isBlackScreen.collectAsState()
    val isFlashlightOn by viewModel.isFlashlightOn.collectAsState()

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.06f, label = "pulseScale",
        animationSpec = infiniteRepeatable(tween(500), RepeatMode.Reverse)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { viewModel.toggleBlackScreen() },
                    onLongPress = { viewModel.toggleFlashlight() }
                )
            }
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                }

                ProcessCameraProvider.getInstance(ctx).addListener({
                    val provider = ProcessCameraProvider.getInstance(ctx).get()
                    val preview = Preview.Builder().build().also { 
                        it.setSurfaceProvider(previewView.surfaceProvider) 
                    }
                    val analysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()

                    analysis.setAnalyzer(ContextCompat.getMainExecutor(ctx)) { imageProxy ->
                        viewModel.analyzeImage(imageProxy)
                    }

                    try {
                        provider.unbindAll()
                        val camera = provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
                        
                        // Observe flashlight state and control torch
                        lifecycleOwner.lifecycleScope.launch {
                            viewModel.isFlashlightOn.collect { isOn ->
                                camera.cameraControl.enableTorch(isOn)
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("MainActivity", "Error binding camera: $e")
                    }
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            }
        )

        if (!isBlackScreen) {
            Canvas(Modifier.fillMaxSize()) {
                val cW = size.width; val cH = size.height
                
                // RESTORE EXACT GACOR COORDINATE MAPPING (Logic from your original code)
                val visFrac = (cW / cH) / (3f / 4f)
                val cropOff = (1f - visFrac) / 2f
                val rx = { x: Float -> ((x - cropOff) / visFrac) * cW }

                alertState.detectedObjects.forEach { obj ->
                    val l = rx(obj.left);  val t = obj.top    * cH
                    val r = rx(obj.right); val b = obj.bottom * cH
                    val isDanger = obj.classId in DANGER_CLASSES
                    val boxCol = if (isDanger) Color(0xFFFF3333) else BOX_COLORS[obj.classId % BOX_COLORS.size]

                    drawRect(
                        color = boxCol,
                        topLeft = Offset(l, t),
                        size = Size(r - l, b - t),
                        style = Stroke(if (isDanger) 8f else 4f)
                    )

                    drawIntoCanvas { canvas ->
                        val bgPaint = Paint().apply {
                            color = android.graphics.Color.argb(180, 0, 0, 0)
                            style = Paint.Style.FILL
                        }
                        val txtPaint = Paint().apply {
                            color = if (isDanger) android.graphics.Color.RED else android.graphics.Color.WHITE
                            textSize = 30f
                            isFakeBoldText = isDanger
                            isAntiAlias = true
                        }
                        val txt = "${obj.label} ${(obj.confidence * 100).toInt()}%"
                        val tw = txtPaint.measureText(txt)
                        canvas.nativeCanvas.drawRoundRect(l, t - 36f, l + tw + 12f, t, 6f, 6f, bgPaint)
                        canvas.nativeCanvas.drawText(txt, l + 6f, t - 10f, txtPaint)
                    }
                }
            }

            if (alertState.level == AlertLevel.DANGER) {
                Box(modifier = Modifier.fillMaxSize().scale(pulseScale)) {
                    Canvas(Modifier.fillMaxSize()) {
                        drawRect(
                            color = Color(0xAAFF0000),
                            topLeft = Offset.Zero,
                            size = size,
                            style = Stroke(width = 16f)
                        )
                    }
                }
            }

            if (alertState.primaryLabel.isNotEmpty()) {
                val panelColor = when (alertState.level) {
                    AlertLevel.DANGER -> Color(0xEECC0000)
                    AlertLevel.INFO -> Color(0xDD0D1B2A)
                    AlertLevel.CLEAR -> Color.Transparent
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(16.dp)
                        .background(panelColor, RoundedCornerShape(16.dp))
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (alertState.level == AlertLevel.DANGER) {
                            Text("⚠️ PERHATIAN", color = Color.Yellow,
                                fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                        }
                        Text(
                            text = alertState.primaryLabel.uppercase(),
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 28.sp,
                            textAlign = TextAlign.Center
                        )
                        val subtext = buildString {
                            if (alertState.jarak.isNotEmpty()) append(alertState.jarak)
                            if (alertState.posisi.isNotEmpty()) {
                                if (isNotEmpty()) append(" · ")
                                append("di ${alertState.posisi}")
                            }
                        }
                        if (subtext.isNotEmpty()) {
                            Text(subtext, color = Color(0xCCFFFFFF),
                                fontSize = 15.sp, textAlign = TextAlign.Center)
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 36.dp)
                    .background(Color(0xCC000000), RoundedCornerShape(20.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val isReady = viewModel.isDetectorReady.collectAsState().value
                Box(modifier = Modifier.size(8.dp).background(
                    if (isReady) Color(0xFF4CAF50) else Color(0xFFFF3333), CircleShape))
                Text(
                    text = if (isReady) "AI AKTIF" else "AI ERROR",
                    color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            )
        }
    }
}
