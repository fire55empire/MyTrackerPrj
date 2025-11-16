package com.example.mytracker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.LocalDate

@Composable
fun CreateGoalScreen(
    onCreateGoal: (String, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var goalName by remember { mutableStateOf("") }
    var daysText by remember { mutableStateOf("") }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Создать цель",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 32.dp)
        )
        
        OutlinedTextField(
            value = goalName,
            onValueChange = { goalName = it },
            label = { Text("Название цели") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            singleLine = true
        )
        
        OutlinedTextField(
            value = daysText,
            onValueChange = { newValue ->
                if (newValue.all { it.isDigit() }) {
                    daysText = newValue
                }
            },
            label = { Text("Количество дней") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            singleLine = true
        )
        
        Button(
            onClick = {
                val days = daysText.toIntOrNull()
                if (goalName.isNotBlank() && days != null && days > 0) {
                    onCreateGoal(goalName, days)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = goalName.isNotBlank() && daysText.toIntOrNull()?.let { it > 0 } == true
        ) {
            Text("Создать")
        }
    }
}

