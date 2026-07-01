package com.example.smartglassesai.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.RectF
import androidx.camera.core.ImageProxy
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

data class DetectedObject(
    val label: String,
    val confidence: Float,
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
    val classId: Int
)

class YoloDetector(context: Context, modelName: String) : AutoCloseable {
    private val interpreter: Interpreter
    private val inputSize = 640
    private val outputArray = Array(1) { Array(300) { FloatArray(6) } }

    private val labels = arrayOf(
        "Bangku", "Rak Sepeda", "Sepeda", "Bangunan", "Bus", "Mobil", "Kursi", "Anjing",
        "Tong Sampah", "Tiang Listrik", "Hidran", "Pembatas Jalan", "Motor", "Zebra Cross",
        "Orang", "Pot Tanaman", "Tangga", "Kerucut Jalan", "Rambu Lalu Lintas", "Pohon", "Truk"
    )

    init {
        interpreter = Interpreter(loadModelFile(context, modelName))
    }

    private fun loadModelFile(context: Context, modelName: String): MappedByteBuffer {
        val fd = context.assets.openFd(modelName)
        val inputStream = FileInputStream(fd.fileDescriptor)
        val fileChannel = inputStream.channel
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, fd.startOffset, fd.declaredLength)
    }

    fun analyze(imageProxy: ImageProxy): List<DetectedObject> {
        val orig = imageProxy.toBitmap()
        val mat  = Matrix().apply { postRotate(imageProxy.imageInfo.rotationDegrees.toFloat()) }
        val rot  = Bitmap.createBitmap(orig, 0, 0, orig.width, orig.height, mat, true)

        val scale  = 640f / maxOf(rot.width, rot.height)
        val sw     = (rot.width * scale).toInt()
        val sh     = (rot.height * scale).toInt()
        val scaled = Bitmap.createScaledBitmap(rot, sw, sh, true)
        val lb     = Bitmap.createBitmap(640, 640, Bitmap.Config.ARGB_8888)
        val lbC    = android.graphics.Canvas(lb)
        lbC.drawColor(android.graphics.Color.BLACK)
        val px = (640 - sw) / 2f
        val py = (640 - sh) / 2f
        lbC.drawBitmap(scaled, px, py, null)

        val padXRatio = px / 640f
        val padYRatio = py / 640f

        val buf = ByteBuffer.allocateDirect(640 * 640 * 3 * 4).apply { order(ByteOrder.nativeOrder()) }
        val pix = IntArray(640 * 640).also { lb.getPixels(it, 0, 640, 0, 0, 640, 640) }
        var p = 0
        for (i in 0 until 640) for (j in 0 until 640) {
            val v = pix[p++]
            buf.putFloat((v shr 16 and 0xFF) / 255f)
            buf.putFloat((v shr 8  and 0xFF) / 255f)
            buf.putFloat((v        and 0xFF) / 255f)
        }

        interpreter.run(buf, outputArray)

        val raw = mutableListOf<DetectedObject>()
        val cW  = 1f - 2 * padXRatio
        val cH  = 1f - 2 * padYRatio

        for (i in 0 until 300) {
            val conf = outputArray[0][i][4]
            if (conf > 0.45f) {
                val cid = outputArray[0][i][5].toInt().coerceIn(0, labels.size - 1)
                raw.add(DetectedObject(
                    label      = labels[cid],
                    confidence = conf,
                    left       = ((outputArray[0][i][0] - padXRatio) / cW).coerceIn(0f, 1f),
                    top        = ((outputArray[0][i][1] - padYRatio) / cH).coerceIn(0f, 1f),
                    right      = ((outputArray[0][i][2] - padXRatio) / cW).coerceIn(0f, 1f),
                    bottom     = ((outputArray[0][i][3] - padYRatio) / cH).coerceIn(0f, 1f),
                    classId    = cid
                ))
            }
        }
        return applyNMS(raw)
    }

    private fun applyNMS(objects: List<DetectedObject>, iouThreshold: Float = 0.45f): List<DetectedObject> {
        val sorted = objects.sortedByDescending { it.confidence }.toMutableList()
        val result = mutableListOf<DetectedObject>()
        while (sorted.isNotEmpty()) {
            val best = sorted.removeAt(0)
            result.add(best)
            sorted.removeAll { other ->
                val iL = maxOf(best.left, other.left);  val iT = maxOf(best.top, other.top)
                val iR = minOf(best.right, other.right); val iB = minOf(best.bottom, other.bottom)
                val iA = maxOf(0f, iR - iL) * maxOf(0f, iB - iT)
                val uA = (best.right-best.left)*(best.bottom-best.top) +
                        (other.right-other.left)*(other.bottom-other.top) - iA
                if (uA <= 0f) false else (iA / uA) > iouThreshold
            }
        }
        return result
    }

    override fun close() {
        interpreter.close()
    }
}
