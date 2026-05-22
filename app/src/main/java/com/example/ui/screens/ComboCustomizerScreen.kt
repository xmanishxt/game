package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CustomCombo
import com.example.data.model.FighterCharacter
import com.example.ui.theme.*
import com.example.viewmodel.GameScreen
import com.example.viewmodel.GameViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComboCustomizerScreen(viewModel: GameViewModel) {
    val combos by viewModel.allCombos.collectAsState()
    val editingCharacterId by viewModel.editingCharacterId.collectAsState()

    // Filter combos loaded for chosen combatant
    val filteredCombos = combos.filter { it.characterId == editingCharacterId }

    // Input States for New Combo Creator
    var comboName by remember { mutableStateOf("") }
    val sequenceList = remember { mutableStateListOf<String>() }
    var selectedColorHex by remember { mutableStateOf("#FF4500") } // Default flame orange

    // Calculations based on custom builder inputs
    val calculatedDamage = remember(sequenceList.size) {
        if (sequenceList.isEmpty()) 0
        else {
            var sum = 0
            sequenceList.forEach { key ->
                sum += when (key) {
                    "L" -> 8   // Light hits
                    "H" -> 14  // Heavy kicks
                    "S" -> 16  // Specials
                    else -> 0
                }
            }
            // Multi-hit bonus additions
            if (sequenceList.size >= 3) sum += 6
            if (sequenceList.size == 4) sum += 10
            sum
        }
    }

    val selectedFighter = FighterCharacter.getById(editingCharacterId)

    // Visual theme based on character
    val brandingColor = when (editingCharacterId) {
        "ignis" -> Color(0xFFFF3D00)
        "volt" -> Color(0xFF00D4FF)
        else -> Color(0xFF66BB6A)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CarbonBlack)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = { viewModel.navigateTo(GameScreen.MENU) },
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.08f), CircleShape)
                        .testTag("back_from_customizer_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }

                Text(
                    text = "COMBO LAB SPECIALIST",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.5.sp
                )

                // Reset to Default Button
                IconButton(
                    onClick = { 
                        viewModel.resetAllCombosToDefault()
                        sequenceList.clear()
                        comboName = ""
                    },
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.08f), CircleShape)
                        .testTag("reset_combos_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reset ALL Combos",
                        tint = ActivePink
                    )
                }
            }

            // Character Tabs Selection Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FighterCharacter.Roster.forEach { fighter ->
                    val isSelected = editingCharacterId == fighter.id
                    val tabColor = if (isSelected) brandingColor else Color.White.copy(alpha = 0.1f)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isSelected) brandingColor.copy(alpha = 0.15f)
                                else DarkGreySurface
                            )
                            .border(1.dp, tabColor, RoundedCornerShape(10.dp))
                            .clickable { 
                                viewModel.setEditingCharacterId(fighter.id)
                                // reset selections
                                sequenceList.clear()
                                comboName = ""
                                selectedColorHex = fighter.primaryColorHex
                            }
                            .testTag("tab_${fighter.id}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = fighter.name.uppercase(),
                            color = if (isSelected) brandingColor else Color.White.copy(alpha = 0.6f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }

            // Split Layout: Left is database active lists, Right is new custom designer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // LEFT: List of active combos
                Column(
                    modifier = Modifier
                        .weight(1.1f)
                        .fillMaxHeight()
                ) {
                    Text(
                        text = "ACTIVE DATABASE COMBOS (${filteredCombos.size})",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f)
                            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp)),
                        color = DarkGreySurface.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (filteredCombos.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No custom combos configured.",
                                    color = Color.White.copy(alpha = 0.4f),
                                    fontSize = 12.sp
                                )
                            }
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(10.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(filteredCombos) { combo ->
                                    val fxColor = Color(android.graphics.Color.parseColor(combo.specialColor))
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color.White.copy(alpha = 0.02f), RoundedCornerShape(8.dp))
                                            .border(1.dp, fxColor.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(8.dp)
                                                        .background(fxColor, CircleShape)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = combo.name,
                                                    color = Color.White,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                if (combo.isDefault) {
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Surface(
                                                        color = Color.White.copy(alpha = 0.12f),
                                                        shape = RoundedCornerShape(4.dp),
                                                        modifier = Modifier.padding(2.dp)
                                                    ) {
                                                        Text(
                                                            text = "PRESET",
                                                            color = Color.White.copy(alpha = 0.5f),
                                                            fontSize = 8.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                            }

                                            // Draw keys
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                                                modifier = Modifier.padding(top = 4.dp)
                                            ) {
                                                combo.sequence.split(",").forEach { key ->
                                                    val keyCol = getButtonThemeColor(key)
                                                    Box(
                                                        modifier = Modifier
                                                            .size(18.dp)
                                                            .background(keyCol.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                                            .border(0.5.dp, keycolBorderColor(key), RoundedCornerShape(4.dp)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            text = key,
                                                            color = keyCol,
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Black
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = "${combo.damage} dmg",
                                                color = fxColor,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Black
                                            )

                                            // Only let them delete non-presets
                                            if (!combo.isDefault) {
                                                IconButton(
                                                    onClick = { viewModel.deleteCustomCombo(combo) },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Delete,
                                                        contentDescription = "Delete Combo",
                                                        tint = ActivePink,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // RIGHT: Interactive creation workshop
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "CREATE CUSTOM COMBO FOR ${selectedFighter.name.uppercase()}",
                        color = brandingColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .background(DarkGreySurface.copy(alpha = 0.9f), RoundedCornerShape(12.dp))
                            .border(1.dp, brandingColor.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Combo name text input
                        TextField(
                            value = comboName,
                            onValueChange = { textVal -> if (textVal.length <= 16) comboName = textVal },
                            label = { Text("Combo Battle Name") },
                            placeholder = { Text("eg. Volcanic Fury") },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = CarbonBlack,
                                unfocusedContainerColor = CarbonBlack,
                                focusedIndicatorColor = brandingColor,
                                unfocusedIndicatorColor = Color.White.copy(alpha = 0.1f),
                                focusedLabelColor = brandingColor,
                                unfocusedLabelColor = Color.White.copy(alpha = 0.4f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedPlaceholderColor = Color.White.copy(alpha = 0.3f),
                                unfocusedPlaceholderColor = Color.White.copy(alpha = 0.3f)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .testTag("combo_name_input")
                        )

                        // Input Keys Builder Row
                        Text(
                            text = "INPUT SEQUENCE (TAP TO BUILD - MAX 4 STEPS)",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )

                        // Visual preview of current sequence building
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .background(CarbonBlack, RoundedCornerShape(8.dp))
                                .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (sequenceList.isEmpty()) {
                                Text(
                                    text = "Sequence empty. Tap buttons below...",
                                    color = Color.White.copy(alpha = 0.3f),
                                    fontSize = 11.sp
                                )
                            } else {
                                sequenceList.forEach { key ->
                                    val keyCol = getButtonThemeColor(key)
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .background(keyCol.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                            .border(1.dp, keycolBorderColor(key), RoundedCornerShape(6.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = key,
                                            color = keyCol,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                    }
                                }
                            }
                        }

                        // Button controls to insert keys L, H, S
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ComboWorkshopButton("L", "Light Punch", ElectricTeal) {
                                if (sequenceList.size < 4) sequenceList.add("L")
                            }
                            ComboWorkshopButton("H", "Heavy Kick", ActivePink) {
                                if (sequenceList.size < 4) sequenceList.add("H")
                            }
                            ComboWorkshopButton("S", "Special Move", SolarAmber) {
                                if (sequenceList.size < 4) sequenceList.add("S")
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = { sequenceList.clear() },
                                colors = ButtonDefaults.textButtonColors(contentColor = ActivePink)
                            ) {
                                Text("CLEAR SEQUENCE", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // FX Color Selector
                        Text(
                            text = "SELECT PARTICLES EFFECT TRAIL COLOR",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val hexCodes = listOf("#FF4500", "#00E5FF", "#FFFF00", "#9C27B0", "#FF1493")
                            hexCodes.forEach { hex ->
                                val colorValue = Color(android.graphics.Color.parseColor(hex))
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .background(colorValue, CircleShape)
                                        .border(
                                            width = if (selectedColorHex == hex) 2.dp else 0.dp,
                                            color = Color.White,
                                            shape = CircleShape
                                        )
                                        .clickable { selectedColorHex = hex }
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                        }

                        // Damage preview metrics
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "CALCULATED COMBO POWER:",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "$calculatedDamage DMG",
                                color = Color(android.graphics.Color.parseColor(selectedColorHex)),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Save custom combo Button
                    Button(
                        onClick = {
                            if (comboName.isNotBlank() && sequenceList.isNotEmpty()) {
                                viewModel.saveCustomCombo(
                                    characterId = editingCharacterId,
                                    name = comboName,
                                    sequence = sequenceList.joinToString(","),
                                    damage = calculatedDamage,
                                    color = selectedColorHex
                                )
                                // Clear inputs
                                comboName = ""
                                sequenceList.clear()
                            }
                        },
                        enabled = comboName.isNotBlank() && sequenceList.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = brandingColor,
                            contentColor = Color.Black,
                            disabledContainerColor = Color.White.copy(alpha = 0.08f),
                            disabledContentColor = Color.White.copy(alpha = 0.2f)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("save_combo_btn")
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = "Save")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "SAVE COMBO TO DISK",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// Helpers for button colours
fun getButtonThemeColor(input: String): Color {
    return when(input) {
        "L" -> ElectricTeal
        "H" -> ActivePink
        "S" -> SolarAmber
        else -> Color.Gray
    }
}

fun keycolBorderColor(input: String): Color {
    return getButtonThemeColor(input).copy(alpha = 0.6f)
}

@Composable
fun RowScope.ComboWorkshopButton(key: String, desc: String, color: Color, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .weight(1f)
            .height(56.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.08f))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = key, color = color, fontSize = 16.sp, fontWeight = FontWeight.Black)
        Text(text = desc, color = Color.White.copy(alpha = 0.5f), fontSize = 8.sp, fontWeight = FontWeight.Bold)
    }
}

// Restored standard state helpers
