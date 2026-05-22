package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "custom_combos")
data class CustomCombo(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val characterId: String,       // "ignis", "volt" or "terra"
    val name: String,
    val sequence: String,          // Comma-separated: "L,L,H" (Light, Light, Heavy)
    val damage: Int = 20,          // Calculated damage of the full combo
    val specialColor: String = "#FF4500", // Hex color of visual effect trails
    val isDefault: Boolean = false // If pre-loaded default combo
)
