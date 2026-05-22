package com.example.data.model

import androidx.compose.ui.graphics.Color

data class FighterCharacter(
    val id: String,
    val name: String,
    val title: String,
    val description: String,
    val maxHealth: Int,
    val speed: Float,          // Pixels per frame multiplier
    val baseDamageMult: Float, // Multiplier for attack hits
    val primaryColorHex: String,
    val accentColorHex: String,
    val specialMoveName: String,
    val specialMoveDesc: String
) {
    fun getPrimaryColor() = Color(android.graphics.Color.parseColor(primaryColorHex))
    fun getAccentColor() = Color(android.graphics.Color.parseColor(accentColorHex))

    companion object {
        val Roster = listOf(
            FighterCharacter(
                id = "ignis",
                name = "Ignis",
                title = "Pyromancer",
                description = "Controls explosive thermal currents. Medium pacing, high fire damage output.",
                maxHealth = 100,
                speed = 5.0f,
                baseDamageMult = 1.25f,
                primaryColorHex = "#FF3D00", // Volcanic Red-Orange
                accentColorHex = "#FFC400",  // Radiant Solar Yellow
                specialMoveName = "Firestorm Pulse",
                specialMoveDesc = "Combusts ahead, blasting enemies with searing horizontal flame projectiles."
            ),
            FighterCharacter(
                id = "volt",
                name = "Volt",
                title = "Lightning Assassin",
                description = "Traverses light-speed currents. High speed and swift combos, low health.",
                maxHealth = 85,
                speed = 7.5f,
                baseDamageMult = 0.9f,
                primaryColorHex = "#00D4FF", // Electric Cyan
                accentColorHex = "#FFE500",  // Zap Yellow
                specialMoveName = "Thunder Burst",
                specialMoveDesc = "Charges forward in a surge of neon light, teleporting and hitting instant damage."
            ),
            FighterCharacter(
                id = "terra",
                name = "Terra",
                title = "Stone Vanguard",
                description = "A massive golem formed of stone tectonic plates. Immense armor, slower actions.",
                maxHealth = 135,
                speed = 3.5f,
                baseDamageMult = 1.1f,
                primaryColorHex = "#66BB6A", // Forest Earth Green
                accentColorHex = "#8B5A2B",  // Clay Brown
                specialMoveName = "Aegis Shield",
                specialMoveDesc = "Gains absolute invincibility for 2 seconds while forming a seismic ward."
            )
        )

        fun getDefault() = Roster[0]
        fun getById(id: String) = Roster.find { it.id == id } ?: getDefault()
    }
}
