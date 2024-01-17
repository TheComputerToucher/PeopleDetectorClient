package org.badlyprogrammedtech.peopledetector_client

import android.util.Log
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
        return getMillisBetween() * 1000 * 60 * 60
    }

    fun getMillisBetween(): Long {
        return when (last.time - first.time) {
            in Long.MIN_VALUE..0 -> 0
            else -> (last.time - first.time)
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

    fun getSlotFromTime(time: Date, slotCount: Int): Long {
        val minutes = msToMinutes(time.time)
        val localMinutes = minutes - msToMinutes(first.time)

        return try {
            val minutesPerSlot = Math.floorDiv(getMinutesBetween(), localMinutes)
            Math.floorDiv(localMinutes, minutesPerSlot)
        } catch (e: Exception) {
            Log.e("TimeRange", "getSlotFromTime($time, $slotCount) failed with error ${e.localizedMessage}, returning 0 as a fallback")
            0
        }
    }
}
