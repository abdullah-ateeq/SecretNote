package com.example.secretnote.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateFormatter {
    private val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    private val dateTimeFormat = SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault())
    private val shortDateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())

    fun formatRelativeOrFull(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp

        if (diff < 0L) {
            return dateTimeFormat.format(Date(timestamp))
        }

        val seconds = diff / 1000L
        if (seconds < 60L) {
            return "Just now"
        }

        val minutes = seconds / 60L
        if (minutes < 60L) {
            return "$minutes min${if (minutes > 1) "s" else ""} ago"
        }

        val calNow = Calendar.getInstance()
        val calDate = Calendar.getInstance().apply { timeInMillis = timestamp }

        val isToday = calNow.get(Calendar.YEAR) == calDate.get(Calendar.YEAR) &&
                calNow.get(Calendar.DAY_OF_YEAR) == calDate.get(Calendar.DAY_OF_YEAR)
        if (isToday) {
            return "Today at ${timeFormat.format(Date(timestamp))}"
        }

        val isYesterday = calNow.get(Calendar.YEAR) == calDate.get(Calendar.YEAR) &&
                calNow.get(Calendar.DAY_OF_YEAR) - calDate.get(Calendar.DAY_OF_YEAR) == 1
        if (isYesterday) {
            return "Yesterday at ${timeFormat.format(Date(timestamp))}"
        }

        return if (calNow.get(Calendar.YEAR) == calDate.get(Calendar.YEAR)) {
            dateTimeFormat.format(Date(timestamp))
        } else {
            shortDateFormat.format(Date(timestamp))
        }
    }

    fun formatFullDateTime(timestamp: Long): String {
        return dateTimeFormat.format(Date(timestamp))
    }
}
