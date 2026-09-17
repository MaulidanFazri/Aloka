package com.example.aloka.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
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
        "Bangku",                     // 0: Bench
        "Sepeda",                     // 1: Bike
        "Bangunan",                   // 2: Building
        "Bus",                        // 3: Bus
        "Mobil",                      // 4: Car
        "Kursi",                      // 5: Chair
        "Anjing",                     // 6: Dog
        "Tempat Sampah",              // 7: Dustbin
        "Tiang Listrik",              // 8: Electrical Pole
        "Hidran Kebakaran",           // 9: Fire hydrant
        "Pagar Pengaman",             // 10: Guard rail
        "Sepeda Motor",               // 11: Motorcycle
        "Penyeberangan Pejalan Kaki", // 12: Pedestrian crosswalk
        "Orang",                      // 13: Person
        "Pot Tanaman",                // 14: Plant Pot
        "Tangga",                     // 15: Stairs
        "Kerucut Lalu Lintas",        // 16: Traffic Cone
        "Rambu Lalu Lintas",          // 17: Traffic sign
        "Pohon",                      // 18: Tree
        "Truk",                       // 19: Truck
        "Lubang Jalan",               // 20: Pothole
        "Pintu"                       // 21: Door
    )

    // Reusable buffers to eliminate GC allocations during frame analysis
    private val inputBuffer: ByteBuffer = ByteBuffer.allocateDirect(1 * inputSize * inputSize * 3 * 4).apply {
        order(ByteOrder.nativeOrder())
    }
    private val pixelArray = IntArray(inputSize * inputSize)
    private val normTable = FloatArray(256) { it / 255f }

    private val letterboxBitmap: Bitmap = Bitmap.createBitmap(inputSize, inputSize, Bitmap.Config.ARGB_8888)
    private val letterboxCanvas: Canvas = Canvas(letterboxBitmap)
    private val matrix = Matrix()
    private val paint = Paint(Paint.FILTER_BITMAP_FLAG)

    init {
        val options = Interpreter.Options().apply {
            val numCores = Runtime.getRuntime().availableProcessors().coerceIn(2, 4)
            setNumThreads(numCores)
            setUseXNNPACK(true)
        }
        interpreter = Interpreter(loadModelFile(context, modelName), options)
    }

    private fun loadModelFile(context: Context, modelName: String): MappedByteBuffer {
        val fd = context.assets.openFd(modelName)
        val inputStream = FileInputStream(fd.fileDescriptor)
        val fileChannel = inputStream.channel
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, fd.startOffset, fd.declaredLength)
    }

    fun analyze(imageProxy: ImageProxy): List<DetectedObject> {
        val orig = imageProxy.toBitmap()
        val rotationDegrees = imageProxy.imageInfo.rotationDegrees

        val isSwapped = rotationDegrees == 90 || rotationDegrees == 270
        val rotW = if (isSwapped) orig.height else orig.width
        val rotH = if (isSwapped) orig.width else orig.height

        val scale = inputSize.toFloat() / maxOf(rotW, rotH)
        val sw = rotW * scale
        val sh = rotH * scale
        val px = (inputSize - sw) / 2f
        val py = (inputSize - sh) / 2f

        matrix.reset()
        matrix.postTranslate(-orig.width / 2f, -orig.height / 2f)
        matrix.postRotate(rotationDegrees.toFloat())
        matrix.postScale(scale, scale)
        matrix.postTranslate(px + sw / 2f, py + sh / 2f)

        letterboxCanvas.drawColor(Color.BLACK)
        letterboxCanvas.drawBitmap(orig, matrix, paint)

        letterboxBitmap.getPixels(pixelArray, 0, inputSize, 0, 0, inputSize, inputSize)

        inputBuffer.rewind()
        val totalPixels = inputSize * inputSize
        for (i in 0 until totalPixels) {
            val v = pixelArray[i]
            inputBuffer.putFloat(normTable[(v shr 16) and 0xFF])
            inputBuffer.putFloat(normTable[(v shr 8) and 0xFF])
            inputBuffer.putFloat(normTable[v and 0xFF])
        }

        val startTime = System.currentTimeMillis()
        interpreter.run(inputBuffer, outputArray)
        val inferenceTime = System.currentTimeMillis() - startTime
        android.util.Log.d("AlokaDebug", "Inference Speed: ${inferenceTime}ms")

        val padXRatio = px / inputSize.toFloat()
        val padYRatio = py / inputSize.toFloat()
        val cW = 1f - 2f * padXRatio
        val cH = 1f - 2f * padYRatio

        val detectedObjects = ArrayList<DetectedObject>()

        for (i in 0 until 300) {
            val conf = outputArray[0][i][4]
            if (conf > 0.45f) {
                var x1 = outputArray[0][i][0]
                var y1 = outputArray[0][i][1]
                var x2 = outputArray[0][i][2]
                var y2 = outputArray[0][i][3]

                if (x1 > 1.0f || x2 > 1.0f || y1 > 1.0f || y2 > 1.0f) {
                    x1 /= inputSize.toFloat()
                    y1 /= inputSize.toFloat()
                    x2 /= inputSize.toFloat()
                    y2 /= inputSize.toFloat()
                }

                val cid = outputArray[0][i][5].toInt().coerceIn(0, labels.size - 1)

                val left = ((x1 - padXRatio) / cW).coerceIn(0f, 1f)
                val top = ((y1 - padYRatio) / cH).coerceIn(0f, 1f)
                val right = ((x2 - padXRatio) / cW).coerceIn(0f, 1f)
                val bottom = ((y2 - padYRatio) / cH).coerceIn(0f, 1f)

                if (right > left && bottom > top) {
                    detectedObjects.add(
                        DetectedObject(
                            label = labels[cid],
                            confidence = conf,
                            left = left,
                            top = top,
                            right = right,
                            bottom = bottom,
                            classId = cid
                        )
                    )
                }
            }
        }

        return detectedObjects
    }

    override fun close() {
        interpreter.close()
        if (!letterboxBitmap.isRecycled) {
            letterboxBitmap.recycle()
        }
    }
}
