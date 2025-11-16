package com.example.mytracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.mytracker.data.Goal
import com.example.mytracker.data.GoalRepository
import com.example.mytracker.ui.screens.CreateGoalScreen
import com.example.mytracker.ui.screens.ProgressScreen
import com.example.mytracker.ui.theme.MyTrackerTheme
import kotlinx.coroutines.launch
import java.time.LocalDate

class MainActivity : ComponentActivity() {
    private lateinit var goalRepository: GoalRepository
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        goalRepository = GoalRepository(this)
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
    
    LaunchedEffect(Unit) {
        // Инициализация
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
                    goalRepository.markToday()
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