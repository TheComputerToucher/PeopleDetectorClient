package org.badlyprogrammedtech.peopledetector_client

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.os.Environment
import android.util.Log
import android.widget.TextView
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.task.core.BaseOptions
import org.tensorflow.lite.task.vision.detector.Detection
import org.tensorflow.lite.task.vision.detector.ObjectDetector
import java.io.FileOutputStream
import java.io.FileWriter
import java.nio.ByteBuffer
import java.time.Instant
import java.util.Calendar
import java.util.Date
import java.util.LinkedHashMap
import java.util.Timer
import java.util.TimerTask

class TensorFlowAnalyzer(private val context: Context, private val detectionsTextWidget: TextView, private val listener: (List<Detection>) -> Int) : ImageAnalysis.Analyzer {
    private var isInitialized = false
    private var test = false
    private lateinit var objectDetector: ObjectDetector
    var humanList = ArrayList<Human>()
    private val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)

    var timerTask: TimerTask = object : TimerTask() {
        override fun run() {
            Log.i("TensorFlowAnalyzer", "Log task running")

            try {
                if (humanList.size >= 1) {
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

                    var slotCount: Int = 0
                    try {
                        slotCount = timeRange.getMinutesBetween().floorDiv(timeBetweenSlots).toInt()
                    } catch (e: Exception) {
                        Log.e("TensorFlowAnalyzer", "slotCount failed with error ${e.localizedMessage}")
                    }
                    val baseTime = timeRange.first

                    val timeSlots = ArrayList<HumanTimeSlot>()

                    val humansInTimeSlots = ArrayList<Long>()

                    humanList.forEach {
                        val index = timeRange.getSlotFromTime(it.timeDetected, slotCount)
                        if (index < humansInTimeSlots.size) humansInTimeSlots[index]++
                        else humansInTimeSlots.add(index, 1)
                    }

                    val calendar = Calendar.getInstance()

                    for (slot in 0..slotCount) {
                        calendar.time = baseTime
                        calendar.add(Calendar.MINUTE, slot * 30)
                        timeSlots.add(HumanTimeSlot(calendar.time, humansInTimeSlots.get(slot)))
                    }

                    for (humanInTimeSlot in humansInTimeSlots) {
                        val slotTime = Date(timeRange.minutesToMs(humanInTimeSlot) + timeRange.first.time)
                        fileWriter.write("$slotTime,$humanInTimeSlot\n")
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

        timer = Timer()

        timer.schedule(timerTask, 15 * 1000) // Convert from Seconds to Milliseconds
    }

    override fun analyze(image: ImageProxy) {
        if(!isInitialized) init()

        val bitmap = image.toBitmap()

        image.close()

        if (!test) {
            val stream = FileOutputStream("$downloadsDir/crap.png")
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.close()
            test = true
            Log.i("shit", "done")
        }

        val tensorImage = TensorImage.fromBitmap(bitmap)

        val detections = objectDetector.detect(tensorImage)
        val detectionStr = detections.toString()

        val detectionsText: String = if (detections.isEmpty()) "Nothing was detected" else "Detections: $detections"

        detectionsTextWidget.post {
            detectionsTextWidget.text = detectionsText
        }

        val calendar = Calendar.getInstance()
        detections.forEach { detection ->
//            val isHuman = detection.categories.any {
//                it.displayName == "person"
//            }
            val isHuman = true

            if (isHuman) {
                val human = Human(calendar.time, "")
                humanList.add(human)
            }
        }

        listener(detections)
    }
}