package org.badlyprogrammedtech.peopledetector_client

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import org.tensorflow.lite.task.vision.detector.Detection

class ObjectDetectionOverlay constructor(context: Context?, attributeSet: AttributeSet?) :
    View(context, attributeSet) {
    private val faceBounds: MutableList<RectF> = mutableListOf()
    private val paint = Paint().apply {
        style = Paint.Style.STROKE
        color = ContextCompat.getColor(context!!, android.R.color.black)
        strokeWidth = 10f
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        faceBounds.forEach { canvas.drawRect(it, paint) }
    }

    fun drawObjectBounds(detections: List<RectF>) {
        this.faceBounds.clear()
        this.faceBounds.addAll(detections)
        invalidate()
    }
}
