package com.example.smartglassesai.ui

import android.app.Application
import android.os.VibrationEffect
import android.os.Vibrator
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.camera.core.ImageProxy
import androidx.lifecycle.AndroidViewModel
import com.example.smartglassesai.ml.DetectedObject
import com.example.smartglassesai.ml.YoloDetector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.Locale
import java.util.concurrent.Executors

enum class AlertLevel { CLEAR, INFO, DANGER }

data class AlertState(
    val level: AlertLevel = AlertLevel.CLEAR,
    val primaryLabel: String = "",
    val posisi: String = "",
    val jarak: String = "",
    val detectedObjects: List<DetectedObject> = emptyList(),
)

class MainViewModel(application: Application) : AndroidViewModel(application), TextToSpeech.OnInitListener {
    private val _alertState = MutableStateFlow(AlertState())
    val alertState: StateFlow<AlertState> = _alertState.asStateFlow()

    private val _isBlackScreen = MutableStateFlow(value = false)
    val isBlackScreen: StateFlow<Boolean> = _isBlackScreen.asStateFlow()

    private val _isDetectorReady = MutableStateFlow(false)
    val isDetectorReady: StateFlow<Boolean> = _isDetectorReady.asStateFlow()

    private val detector: YoloDetector? by lazy {
        try { 
            val d = YoloDetector(application, "yolo26n_int8.tflite")
            _isDetectorReady.value = true
            d
        } catch (e: Exception) { 
            Log.e("MainViewModel", "Gagal load model: ${e.message}")
            _isDetectorReady.value = false
            null 
        }
    }

    private val tts: TextToSpeech = TextToSpeech(application, this)
    private val vibrator = application.getSystemService(Vibrator::class.java)
    private val analysisExecutor = Executors.newSingleThreadExecutor()

    private var lastSpokenLabel = ""
    private var lastSpokenPos = ""
    private var lastSpokenTime = 0L
    private var lastAnalyzeTime = 0L

    private val dangerClasses = setOf(1, 2, 4, 5, 8, 9, 10, 11, 12, 14, 16, 17, 19, 20)

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val localeId = Locale.forLanguageTag("id-ID")
            val res = tts.setLanguage(localeId)
            if ((res == TextToSpeech.LANG_MISSING_DATA) || (res == TextToSpeech.LANG_NOT_SUPPORTED)) {
                tts.language = Locale.US
            }
            tts.speak(" ", TextToSpeech.QUEUE_FLUSH, null, "warmup")
        }
    }

    fun toggleBlackScreen() {
        _isBlackScreen.update { !it }
        val msg = if (_isBlackScreen.value) "Mode hemat baterai aktif" else "Layar menyala"
        speak(msg, TextToSpeech.QUEUE_FLUSH)
    }

    fun analyzeImage(imageProxy: ImageProxy) {
        val currentTime = System.currentTimeMillis()
        // RESTORE EXACT GACOR THROTTLE (200ms = 5 FPS)
        val throttleInterval = if (_isBlackScreen.value) 1000L else 200L
        
        if ((currentTime - lastAnalyzeTime) < throttleInterval) {
            imageProxy.close()
            return
        }
        lastAnalyzeTime = currentTime

        analysisExecutor.execute {
            try {
                val detections = detector?.analyze(imageProxy) ?: emptyList()
                processDetections(detections)
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error analyzing image: ${e.message}")
            } finally {
                imageProxy.close()
            }
        }
    }

    private fun processDetections(detections: List<DetectedObject>) {
        val now = System.currentTimeMillis()
        
        val danger = detections.asSequence()
            .filter { it.classId in dangerClasses && (it.bottom - it.top) > 0.35f }
            .maxByOrNull { it.confidence }

        val top = detections.maxByOrNull { it.confidence }

        val newAlert: AlertState
        val textToSpeak: String?
        val newLabel: String
        val newPos: String

        if (danger != null) {
            val cx = (danger.left + danger.right) / 2f
            val pos = getPosisi(cx)
            val jarak = getJarak(danger.bottom - danger.top)
            newAlert = AlertState(AlertLevel.DANGER, danger.label, pos, jarak, detections)
            newLabel = danger.label
            newPos = pos

            val labelOrPosChanged = newLabel != lastSpokenLabel || newPos != lastSpokenPos
            val repeatDue = (now - lastSpokenTime) > 2000L

            textToSpeak = when {
                labelOrPosChanged -> "Awas! ${danger.label} $pos"
                repeatDue && !tts.isSpeaking -> "Awas! ${danger.label} $pos"
                else -> null
            }
        } else if (top != null) {
            val cx = (top.left + top.right) / 2f
            val pos = getPosisi(cx)
            newAlert = AlertState(AlertLevel.INFO, top.label, pos, "", detections)
            newLabel = top.label
            newPos = pos

            val labelOrPosChanged = newLabel != lastSpokenLabel || newPos != lastSpokenPos
            val repeatDue = (now - lastSpokenTime) > 5000L

            textToSpeak = if ((labelOrPosChanged || repeatDue) && !tts.isSpeaking) {
                "${top.label}, $pos"
            } else null
        } else {
            newAlert = AlertState(AlertLevel.CLEAR, detectedObjects = detections)
            newLabel = ""
            newPos = ""
            textToSpeak = null
        }

        _alertState.update { newAlert }

        if (textToSpeak != null) {
            val queueMode = if (newAlert.level == AlertLevel.DANGER)
                TextToSpeech.QUEUE_FLUSH
            else
                TextToSpeech.QUEUE_ADD

            speak(textToSpeak, queueMode)

            if (newAlert.level == AlertLevel.DANGER) {
                vibrator?.vibrate(VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE))
            }

            lastSpokenLabel = newLabel
            lastSpokenPos = newPos
            lastSpokenTime = now
        }
    }

    private fun getPosisi(cx: Float) = when {
        cx < 0.33f -> "kiri"
        cx > 0.67f -> "kanan"
        else -> "depan"
    }

    private fun getJarak(boxH: Float) = when {
        boxH > 0.70f -> "sangat dekat"
        boxH > 0.45f -> "dekat"
        else -> ""
    }

    private fun speak(text: String, queueMode: Int) {
        tts.speak(text, queueMode, null, "tts")
    }

    override fun onCleared() {
        super.onCleared()
        detector?.close()
        tts.stop()
        tts.shutdown()
        analysisExecutor.shutdown()
    }
}
