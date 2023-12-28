package org.badlyprogrammedtech.peopledetector_client

import android.content.Context
import android.os.Environment
import android.util.Log
import android.widget.TextView
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.task.core.BaseOptions
import org.tensorflow.lite.task.vision.detector.Detection
import org.tensorflow.lite.task.vision.detector.ObjectDetector
import java.io.FileWriter
import java.nio.ByteBuffer
import java.util.Calendar
import java.util.Date
import java.util.Timer
import java.util.TimerTask

class TensorFlowAnalyzer(private val context: Context, private val detectionsTextWidget: TextView, private val listener: (List<Detection>) -> Int) : ImageAnalysis.Analyzer {
    private var isInitialized = false
    private lateinit var objectDetector: ObjectDetector
    private var humanList = ArrayList<Human>()
    private val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
    private var timerTask: TimerTask = object : TimerTask() {
        override fun run() {
            Log.i("TensorFlowAnalyzer", "Log task running")



            try {
                if (humanList.size > 1) {
                    val time = Calendar.getInstance().time

                    val fileNameTime = time.toString()
                        .replace(" ", "")
                        .replace(":", "")

                    val fileWriter = FileWriter("$downloadsDir/scanner-$fileNameTime.csv")
                    val timeRange = TimeRange(time, time)

                    humanList.forEach {
                        // Check minimum
                        timeRange.checkAndUpdateFirst(it.timeDetected)

                        // Check maximum
                        timeRange.checkAndUpdateLast(it.timeDetected)
                    }

                    val slotCount = timeRange.getMinutesBetween().floorDiv(30).toInt()

                    val timeSlots = ArrayList<HumanTimeSlot>()

                    for (human in humanList) {
                        human.timeDetected
                    }

                    for (booptupeDevice in humanList) {
//                        val deviceMacAddress = booptupeDevice.macaroniAddress
                        val deviceTimeDetected = booptupeDevice.timeDetected
//                        val deviceRssi = booptupeDevice.rssi
                        fileWriter.write("$deviceTimeDetected\n")
                    }
                    fileWriter.close()
                    humanList.clear()
                    Log.d("TensorFlowAnalyzer", "Log task complete") // "Log task complete (file empty, so not written)"
                } else {
                    Log.d("TensorFlowAnalyzer", "Log task complete (file empty, so not written)")
                }
            } catch (e: Error) {
                Log.e("TensorFlowAnalyzer", "Log task failed with error $e")
            }

            Log.i("TensorFlowAnalyzer", "Log task complete")
        }
    }
    private lateinit var timer: Timer

    private fun ByteBuffer.toByteArray(): ByteArray {
        rewind()    // Rewind the buffer to zero
        val data = ByteArray(remaining())
        get(data)   // Copy the buffer into a byte array
        return data // Return the byte array
    }

    private fun init() {
        Log.i("TensorFlowAnalyzer", "Initializing")

        val modelName = "kollmodel.tflite"

        val optionsBuilder =
            ObjectDetector.ObjectDetectorOptions.builder()
                .setScoreThreshold(0.75f)
                .setMaxResults(64)

        objectDetector =
            ObjectDetector.createFromFileAndOptions(
                context, modelName, optionsBuilder.build())

        val baseOptionsBuilder = BaseOptions.builder().setNumThreads(4)

        try {
            baseOptionsBuilder.useGpu()
        } catch (e: Exception) {
            Log.e("Failed to init GPU, using CPU", e.toString())
        }

        optionsBuilder.setBaseOptions(baseOptionsBuilder.build())

        try {
            objectDetector = ObjectDetector.createFromFileAndOptions(context, modelName, optionsBuilder.build())
        } catch (e: Exception) {
            Log.e("failed to init the object detector :(", e.toString())
        }

        isInitialized = true

        timer.schedule(timerTask, 15 * 1000) // Convert from Seconds to Milliseconds
    }

    override fun analyze(image: ImageProxy) {
        if(!isInitialized) init()

        val bitmap = image.toBitmap()

        image.close()

        val tensorImage = TensorImage.fromBitmap(bitmap)

        val detections = objectDetector.detect(tensorImage)
        val detectionStr = detections.toString()

        val detectionsText: String = if (detections.isEmpty()) "Nothing was detected" else "Detections: $detections"

        detectionsTextWidget.post {
            detectionsTextWidget.text = detectionsText
        }

        listener(detections)
    }
}