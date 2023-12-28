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

data class TimeRange(var first: Date, var last: Date) {
    fun checkAndUpdateFirst(date: Date): Boolean {
        return if (date > first) {
            first = date
            true
        } else {
            false
        }
    }

    fun checkAndUpdateLast(date: Date): Boolean {
        return if (date < last) {
            last = date
            true
        } else {
            false
        }
    }

    fun getMinutesBetween(): Long {
        when (last.time - first.time) {
            in Long.MIN_VALUE..0 -> return 0
            else -> return (last.time - first.time) * 1000 * 60 * 60
        }
    }

    fun getSubRangeFromStartingPointAndMinutes(start: Date, length: Int): TimeRange {
        val endTime = start.time + ( length * 1000 * 60 * 60 ) // Get the start epoch time and add the length converted from Minutes to Milliseconds
        val end = Date(endTime)

        return TimeRange(start, end)
    }
}

data class HumanTimeSlot(val time: Date, val humans: Long)
