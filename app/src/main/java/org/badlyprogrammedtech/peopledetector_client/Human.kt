package org.badlyprogrammedtech.peopledetector_client

import android.os.Build.VERSION_CODES
import androidx.annotation.RequiresApi
import java.time.Instant
import java.util.Date

data class Human(val timeDetected: Date, val uuid: String) {
    fun toCsvLine(): String {
        return "$timeDetected,$uuid"
    }
}

data class HumanTimeSlot(val time: Date, val humans: Long)
