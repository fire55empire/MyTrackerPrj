package com.example.mytracker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.mytracker.data.Goal

@Composable
fun ProgressScreen(
    goal: Goal,
    onMarkToday: () -> Unit,
    onDeleteGoal: () -> Unit,
    modifier: Modifier = Modifier
) {
    val today = java.time.LocalDate.now()
    val isMarkedToday = goal.markedDates.contains(today)
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = goal.name,
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.padding(bottom = 32.dp)
            )
            
            Text(
                text = "${goal.progressPercent}%",
                style = MaterialTheme.typography.displayMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            Text(
                text = "Выполнено: ${goal.daysCompleted} из ${goal.totalDays} дней",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(bottom = 48.dp)
            )
            
            Button(
                onClick = onMarkToday,
                enabled = !isMarkedToday,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(
                    text = if (isMarkedToday) "Уже отмечено сегодня" else "Отметить сегодня"
                )
            }
        }
        
        Button(
            onClick = onDeleteGoal,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Text("Удалить цель")
        }
    }
}

