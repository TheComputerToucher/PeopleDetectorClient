package org.badlyprogrammedtech.peopledetector_client

import android.content.Context
import android.graphics.Bitmap
import android.os.Environment
import android.util.Log
import android.util.TimeFormatException
import android.widget.TextView
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.task.core.BaseOptions
import org.tensorflow.lite.task.vision.detector.Detection
import org.tensorflow.lite.task.vision.detector.ObjectDetector
import java.io.FileOutputStream
import java.io.FileWriter
import java.lang.IndexOutOfBoundsException
import java.nio.ByteBuffer
import java.sql.Time
import java.time.Instant
import java.util.Calendar
import java.util.Date
import java.util.Timer
import java.util.TimerTask

class TensorFlowAnalyzer(private val context: Context, private val detectionsTextWidget: TextView, private val listener: (List<Detection>) -> Int) : ImageAnalysis.Analyzer {
    private var isInitialized = false
    private var test = false
    private val calendar = Calendar.getInstance()
    private lateinit var objectDetector: ObjectDetector
    var timeSlots = ArrayList<HumanTimeSlot>()
    private val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
    private var frameCount: Int = 0
    private var tempHumanCounts = ArrayList<Long>()
    private lateinit var currentTimeRange: TimeRange

    var timerTask: TimerTask = object : TimerTask() {
        override fun run() {
            Log.i("TensorFlowAnalyzer", "Log task running")

            try {
                val time = calendar.time

                val fileNameTime = time.toString()
                    .replace(" ", "")
                    .replace(":", "")

                val fileWriter = FileWriter("$downloadsDir/scanner-$fileNameTime.csv")

                for (humanInTimeSlot in timeSlots) {
                    val slotTime = humanInTimeSlot.time
                    val slotPeople = humanInTimeSlot.humans
                    fileWriter.write("$slotTime,$slotPeople\n")
                }
                fileWriter.close()
                Log.d(
                    "TensorFlowAnalyzer",
                    "Log task complete"
                ) // "Log task complete (file empty, so not written)"
            } catch (e: Error) {
                Log.e("TensorFlowAnalyzer", "Log task failed with error $e")
            }

//            Log.i("TensorFlowAnalyzer", "Log task complete")
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

        val delay: Long = 1 * 60 * 60 * 1000

        timer.schedule(timerTask, delay) // Convert from Hours to Milliseconds

        val delayedTime = Date(calendar.timeInMillis + delay)

        currentTimeRange = TimeRange(calendar.time, delayedTime)
    }

    override fun analyze(image: ImageProxy) {
        if(!isInitialized) init()

        val bitmap = image.toBitmap()

        image.close()

//        if (!test) {
//            val stream = FileOutputStream("$downloadsDir/crap.png")
//            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
//            stream.close()
//            test = true
//            Log.i("shit", "done")
//        }

        val tensorImage = TensorImage.fromBitmap(bitmap)

        val detections = objectDetector.detect(tensorImage)

        val detectionsText: String = if (detections.isEmpty()) "Nothing was detected" else "Detections: $detections"

        detectionsTextWidget.post {
            detectionsTextWidget.text = detectionsText
        }

        Log.i("frameCount", frameCount.toString())

        if (frameCount < 25) {
            var humanCount: Long = 0
            detections.forEach { detection ->
                if (detection.toString().contains("person")) { // WHAT THE FUCK DID I JUST DO
                    humanCount++
                }
                Log.i("TensorFlowAnalyzer", "Detection categories: ${detection.categories}")
            }
            tempHumanCounts.add(frameCount, humanCount)
            frameCount++
        } else {
            printDetections()
            var index = 0
            var maxNumber: Long = 0

            while (index < tempHumanCounts.size) {
                val countAtIndex: Long = tempHumanCounts.get(index)

                if (maxNumber < countAtIndex)
                    maxNumber = countAtIndex

                index++
            }

            val slotNumber = currentTimeRange.getSlotFromTime(calendar.time, (currentTimeRange.getMillisBetween() / 30 * 60 * 1000).toInt())


            try {
                val slot = timeSlots[slotNumber]

                slot.humans += maxNumber

                timeSlots[slotNumber] = slot
            } catch (e: IndexOutOfBoundsException) {
                // Probably hasn't been initialized, so let's do that now
                val currentTime = Calendar
                    .getInstance()
                    .time

                if (timeSlots.size <= slotNumber) {
                    var i = timeSlots.size
                    while (i <= slotNumber) {
                        val slotTimeMs = currentTime.time + (30 * 60 * 1000 * slotNumber)
                        val slotTime = Date(slotTimeMs)
                        val slotHumans = if (timeSlots.size == slotNumber) maxNumber else 0

                        val slot = HumanTimeSlot(slotTime, slotHumans)

                        timeSlots.add(slot)
                        i++
                    }
                }
            }

            frameCount = 0

            tempHumanCounts.clear()
        }

        listener(detections)
    }

    fun printDetections() {
        Log.i("TensorFlowAnalyzer", "Detections: $timeSlots")
        Log.i("TensorFlowAnalyzer", "Temp Detections: $tempHumanCounts")
    }
}