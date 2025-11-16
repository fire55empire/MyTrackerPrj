package com.example.mytracker

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.mytracker.data.Goal
import com.example.mytracker.data.GoalRepository
import com.example.mytracker.notifications.NotificationHelper
import com.example.mytracker.notifications.NotificationMessages
import com.example.mytracker.notifications.NotificationScheduler
import com.example.mytracker.ui.screens.CreateGoalScreen
import com.example.mytracker.ui.screens.ProgressScreen
import com.example.mytracker.ui.theme.MyTrackerTheme
import kotlinx.coroutines.launch
import java.time.LocalDate

class MainActivity : ComponentActivity() {
    private lateinit var goalRepository: GoalRepository
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        // Разрешение получено или отклонено
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        goalRepository = GoalRepository(this)
        
        // Запрашиваем разрешение на уведомления для Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        
        enableEdgeToEdge()
        setContent {
            MyTrackerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MyTrackerApp(goalRepository)
                }
            }
        }
    }
}

@Composable
fun MyTrackerApp(goalRepository: GoalRepository) {
    val goal by goalRepository.goalFlow.collectAsState(initial = null)
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    val notificationHelper = remember { NotificationHelper(context) }
    val notificationScheduler = remember { NotificationScheduler(context) }
    
    LaunchedEffect(goal) {
        if (goal != null) {
            // Планируем уведомления при создании цели
            notificationScheduler.scheduleDailyReminders()
        } else {
            // Отменяем уведомления при удалении цели
            notificationScheduler.cancelAllReminders()
        }
    }
    
    if (goal == null) {
        CreateGoalScreen(
            onCreateGoal = { name, days ->
                scope.launch {
                    val newGoal = Goal(
                        name = name,
                        totalDays = days,
                        startDate = LocalDate.now()
                    )
                    goalRepository.saveGoal(newGoal)
                }
            }
        )
    } else {
        ProgressScreen(
            goal = goal!!,
            onMarkToday = {
                scope.launch {
                    val success = goalRepository.markToday()
                    if (success) {
                        // Отправляем уведомление-похвалу при успешной отметке
                        val goalName = goalRepository.getGoalName() ?: goal!!.name
                        val message = NotificationMessages.getRandomPraise()
                        notificationHelper.showPraiseNotification(message, goalName)
                    }
                }
            },
            onDeleteGoal = {
                scope.launch {
                    goalRepository.deleteGoal()
                }
            }
        )
    }
}