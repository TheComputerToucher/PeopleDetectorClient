package org.badlyprogrammedtech.peopledetector_client

import java.util.Date

data class Human(val timeDetected: Date, val uuid: String) {
    fun toCsvLine(): String {
        return "$timeDetected,$uuid"
    }
}

data class HumanTimeSlot(val time: Date, var humans: Long)
