package com.example.mytracker.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.mytracker.data.GoalRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.util.Calendar

class AlarmReceiver : BroadcastReceiver() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    override fun onReceive(context: Context, intent: Intent) {
        val goalRepository = GoalRepository(context)
        val notificationHelper = NotificationHelper(context)
        val notificationScheduler = NotificationScheduler(context)
        
        scope.launch {
            val goal = goalRepository.goalFlow.first()
            
            if (goal != null) {
                val today = LocalDate.now()
                val isMarkedToday = goal.markedDates.contains(today)
                
                // Если цель не отмечена сегодня - отправляем напоминание
                if (!isMarkedToday) {
                    val message = NotificationMessages.getRandomReminder()
                    notificationHelper.showReminderNotification(message, goal.name)
                }
            }
            
            // Перепланируем на следующий день
            // Определяем, какое время было запланировано по request code
            val requestCode = intent.getIntExtra("request_code", -1)
            if (requestCode != -1) {
                val hour = when (requestCode) {
                    NotificationScheduler.REQUEST_CODE_14 -> 14
                    NotificationScheduler.REQUEST_CODE_17 -> 17
                    NotificationScheduler.REQUEST_CODE_20 -> 20
                    else -> return@launch
                }
                scheduleNext(context, hour, 0, requestCode)
            } else {
                // Если request code не передан, перепланируем все
                notificationScheduler.scheduleDailyReminders()
            }
        }
    }
    
    private fun scheduleNext(context: Context, hour: Int, minute: Int, requestCode: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
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
            add(Calendar.DAY_OF_MONTH, 1) // На следующий день
        }
        
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
}

