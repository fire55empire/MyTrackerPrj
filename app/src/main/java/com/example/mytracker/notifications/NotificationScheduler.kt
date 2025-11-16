package com.example.mytracker.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.Calendar

class NotificationScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    
    companion object {
        const val REQUEST_CODE_14 = 1001
        const val REQUEST_CODE_17 = 1002
        const val REQUEST_CODE_20 = 1003
    }
    
    fun scheduleDailyReminders() {
        // Планируем уведомления на 14:00, 17:00 и 20:00
        scheduleReminder(14, 0, REQUEST_CODE_14)
        scheduleReminder(17, 0, REQUEST_CODE_17)
        scheduleReminder(20, 0, REQUEST_CODE_20)
    }
    
    private fun scheduleReminder(hour: Int, minute: Int, requestCode: Int) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("request_code", requestCode)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            
            // Если время уже прошло сегодня, планируем на завтра
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_MONTH, 1)
            }
        }
        
        // Используем setExact для точного времени
        // Для повторения каждый день нужно перепланировать в AlarmReceiver
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        }
    }
    
    fun cancelAllReminders() {
        cancelReminder(REQUEST_CODE_14)
        cancelReminder(REQUEST_CODE_17)
        cancelReminder(REQUEST_CODE_20)
    }
    
    private fun cancelReminder(requestCode: Int) {
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
}

