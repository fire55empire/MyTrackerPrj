package com.example.mytracker.data

import java.time.LocalDate

data class Goal(
    val name: String,
    val totalDays: Int,
    val startDate: LocalDate,
    val markedDates: Set<LocalDate> = emptySet()
) {
    val progress: Float
        get() = if (totalDays > 0) {
            markedDates.size.toFloat() / totalDays.toFloat()
        } else {
            0f
        }
    
    val progressPercent: Int
        get() = (progress * 100).toInt()
    
    val daysCompleted: Int
        get() = markedDates.size
}

