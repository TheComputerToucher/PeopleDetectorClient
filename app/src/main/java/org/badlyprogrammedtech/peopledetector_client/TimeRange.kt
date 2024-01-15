package org.badlyprogrammedtech.peopledetector_client

import android.util.Log
import java.util.Calendar
import java.util.Date

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
        return when (last.time - first.time) {
            in Long.MIN_VALUE..0 -> 0
            else -> (last.time - first.time) * 1000 * 60 * 60
        }
    }

    fun getSubRangeFromStartingPointAndMinutes(start: Date, length: Int): TimeRange {
        val endTime = start.time + ( length * 1000 * 60 ) // Get the start epoch time and add the length converted from Minutes to Milliseconds
        val end = Date(endTime)

        return TimeRange(start, end)
    }

    fun msToMinutes(ms: Long): Long {
        return Math.floorDiv(ms, minuteToMs)
    }

    fun minutesToMs(minutes: Long): Long {
        return minutes * minuteToMs
    }

    fun getSlotFromTime(time: Date, slotCount: Int): Int {
        val minutes = msToMinutes(time.time)
        val localMinutes = minutes - msToMinutes(first.time)

        try {
            return Math.floorDiv(localMinutes.toInt(), slotCount)
        } catch (e: Exception) {
            Log.e("TimeRange", "getSlotFromTime($time, $slotCount) failed with error ${e.localizedMessage}, returning 0 as a fallback")
            return 0
        }
    }
}
