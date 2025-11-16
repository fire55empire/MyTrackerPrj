package com.example.mytracker.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "goal_preferences")

class GoalRepository(private val context: Context) {
    private val goalNameKey = stringPreferencesKey("goal_name")
    private val totalDaysKey = stringPreferencesKey("total_days")
    private val startDateKey = stringPreferencesKey("start_date")
    private val markedDatesKey = stringPreferencesKey("marked_dates")
    
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    
    val goalFlow: Flow<Goal?> = context.dataStore.data.map { preferences ->
        val name = preferences[goalNameKey]
        val totalDaysStr = preferences[stringPreferencesKey("total_days")]
        
        if (name == null || totalDaysStr == null) {
            null
        } else {
            val totalDays = totalDaysStr.toIntOrNull() ?: return@map null
            val startDateStr = preferences[startDateKey] ?: LocalDate.now().format(dateFormatter)
            val startDate = LocalDate.parse(startDateStr, dateFormatter)
            
            val markedDatesStr = preferences[markedDatesKey] ?: ""
            val markedDates = if (markedDatesStr.isEmpty()) {
                emptySet()
            } else {
                markedDatesStr.split(",")
                    .mapNotNull { dateStr ->
                        try {
                            LocalDate.parse(dateStr.trim(), dateFormatter)
                        } catch (e: Exception) {
                            null
                        }
                    }
                    .toSet()
            }
            
            Goal(name, totalDays, startDate, markedDates)
        }
    }
    
    suspend fun saveGoal(goal: Goal) {
        context.dataStore.edit { preferences ->
            preferences[goalNameKey] = goal.name
            preferences[stringPreferencesKey("total_days")] = goal.totalDays.toString()
            preferences[startDateKey] = goal.startDate.format(dateFormatter)
            preferences[markedDatesKey] = goal.markedDates
                .map { it.format(dateFormatter) }
                .joinToString(",")
        }
    }
    
    suspend fun markToday(): Boolean {
        val today = LocalDate.now()
        val preferences = context.dataStore.data.first()
        
        val name = preferences[goalNameKey] ?: return false
        val totalDaysStr = preferences[stringPreferencesKey("total_days")] ?: return false
        val totalDays = totalDaysStr.toIntOrNull() ?: return false
        
        val startDateStr = preferences[startDateKey] ?: LocalDate.now().format(dateFormatter)
        val startDate = LocalDate.parse(startDateStr, dateFormatter)
        
        val markedDatesStr = preferences[markedDatesKey] ?: ""
        val markedDates = if (markedDatesStr.isEmpty()) {
            emptySet()
        } else {
            markedDatesStr.split(",")
                .mapNotNull { dateStr ->
                    try {
                        LocalDate.parse(dateStr.trim(), dateFormatter)
                    } catch (e: Exception) {
                        null
                    }
                }
                .toSet()
        }
        
        if (markedDates.contains(today)) {
            return false // Уже отмечено сегодня
        }
        
        val updatedGoal = Goal(name, totalDays, startDate, markedDates + today)
        saveGoal(updatedGoal)
        return true
    }
    
    suspend fun getGoalName(): String? {
        val preferences = context.dataStore.data.first()
        return preferences[goalNameKey]
    }
    
    suspend fun deleteGoal() {
        context.dataStore.edit { preferences ->
            preferences.remove(goalNameKey)
            preferences.remove(stringPreferencesKey("total_days"))
            preferences.remove(startDateKey)
            preferences.remove(markedDatesKey)
        }
    }
}

