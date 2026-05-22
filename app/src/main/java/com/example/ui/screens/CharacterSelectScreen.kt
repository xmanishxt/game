package com.example.ui.screens

import androidx.compose.animation.*
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CustomCombo
import com.example.data.model.FighterCharacter
import com.example.ui.theme.CarbonBlack
import com.example.ui.theme.DarkGreySurface
import com.example.ui.theme.ElectricTeal
import com.example.ui.theme.ActivePink
import com.example.ui.theme.SolarAmber
import com.example.viewmodel.GameMode
import com.example.viewmodel.GameScreen
import com.example.viewmodel.GameViewModel

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun CharacterSelectScreen(viewModel: GameViewModel) {
    val p1Char by viewModel.p1Character.collectAsState()
    val p2Char by viewModel.p2Character.collectAsState()
    val gameMode by viewModel.gameMode.collectAsState()
    val combos by viewModel.allCombos.collectAsState()

    // Filter combo registers loaded for each character
    val p1Combos = combos.filter { it.characterId == p1Char.id }
    val p2Combos = combos.filter { it.characterId == p2Char.id }

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
            // Header bar
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
                        .testTag("back_to_menu_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }

                Text(
                    text = "SELECT YOUR COMBATANTS",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.5.sp
                )

                // Dummy invisible box to center title perfectly
                Box(modifier = Modifier.size(40.dp))
            }

            // Central content: Symmetrical split screen (P1 Left, P2 Right)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // PLAYER 1 SECTION
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(DarkGreySurface.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                        .border(1.5.dp, ElectricTeal.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = "PLAYER 1 (LEFT)",
                        color = ElectricTeal,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                    
                    Spacer(modifier = Modifier.height(10.dp))

                    // Character Selector Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FighterCharacter.Roster.forEach { fighter ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (p1Char.id == fighter.id) ElectricTeal.copy(alpha = 0.15f)
                                        else Color.Black.copy(alpha = 0.4f)
                                    )
                                    .border(
                                        width = if (p1Char.id == fighter.id) 2.dp else 1.dp,
                                        color = if (p1Char.id == fighter.id) ElectricTeal else Color.White.copy(alpha = 0.1f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { viewModel.selectP1Character(fighter) }
                                    .testTag("p1_select_${fighter.id}"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = fighter.name,
                                    color = if (p1Char.id == fighter.id) ElectricTeal else Color.White.copy(alpha = 0.6f),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Character Details display
                    AnimatedContent(
                        targetState = p1Char,
                        transitionSpec = { fadeIn() with fadeOut() },
                        modifier = Modifier.weight(1f),
                        label = "p1_details"
                    ) { activeChar ->
                        FighterSpecsCard(activeChar, p1Combos, ElectricTeal)
                    }
                }

                // PLAYER 2 (OR CPU) SECTION
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(DarkGreySurface.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                        .border(
                            width = 1.5.dp,
                            color = if (gameMode == GameMode.PVP) ActivePink.copy(alpha = 0.4f) else SolarAmber.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(12.dp)
                ) {
                    Text(
                        text = if (gameMode == GameMode.PVP) "PLAYER 2 (RIGHT)" else "CPU BOT (PRACTICE)",
                        color = if (gameMode == GameMode.PVP) ActivePink else SolarAmber,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Character Selector Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FighterCharacter.Roster.forEach { fighter ->
                            val isSelected = p2Char.id == fighter.id
                            val slotColor = if (gameMode == GameMode.PVP) ActivePink else SolarAmber
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isSelected) slotColor.copy(alpha = 0.15f)
                                        else Color.Black.copy(alpha = 0.4f)
                                    )
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) slotColor else Color.White.copy(alpha = 0.1f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { viewModel.selectP2Character(fighter) }
                                    .testTag("p2_select_${fighter.id}"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = fighter.name,
                                    color = if (isSelected) slotColor else Color.White.copy(alpha = 0.6f),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Character Details display
                    val p2Accent = if (gameMode == GameMode.PVP) ActivePink else SolarAmber
                    AnimatedContent(
                        targetState = p2Char,
                        transitionSpec = { fadeIn() with fadeOut() },
                        modifier = Modifier.weight(1f),
                        label = "p2_details"
                    ) { activeChar ->
                        FighterSpecsCard(activeChar, p2Combos, p2Accent)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action: Lock In & Launch fight
            Button(
                onClick = { viewModel.navigateTo(GameScreen.FIGHT) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = ElectricTeal,
                    contentColor = Color.Black
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .testTag("lock_and_fight_btn")
            ) {
                Text(
                    text = "LOCKED IN: COMMENCE FIGHT!",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                )
            }
        }
    }
}

@Composable
fun FighterSpecsCard(fighter: FighterCharacter, combosList: List<CustomCombo>, mainColor: Color) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.40f), RoundedCornerShape(12.dp))
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        // Character bio heading
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .background(Color(android.graphics.Color.parseColor(fighter.primaryColorHex)), CircleShape)
            )
            Column {
                Text(
                    text = fighter.name.uppercase(),
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = fighter.title,
                    color = mainColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }

        Text(
            text = fighter.description,
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 11.sp,
            lineHeight = 15.sp,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        // Attribute Bars divider
        Text(
            text = "FIGHTER SPECS",
            color = Color.White.copy(alpha = 0.4f),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        // Stat 1: Max Health
        StatBar(
            name = "VIT (HEALTH)",
            value = fighter.maxHealth.toFloat() / 150f, // normalized
            displayVal = "${fighter.maxHealth}",
            color = Color(0xFF4CAF50)
        )

        // Stat 2: Moving Velocity
        StatBar(
            name = "SPD (MOVEMENT)",
            value = fighter.speed / 10f,
            displayVal = "%.1f".format(fighter.speed),
            color = ElectricTeal
        )

        // Stat 3: Attack Damage
        StatBar(
            name = "ATK (PUNCHES)",
            value = fighter.baseDamageMult - 0.4f, // scale multiplier cleanly
            displayVal = "x%.2f".format(fighter.baseDamageMult),
            color = ActivePink
        )

        // Customized Combo List loaded from database
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "LOADED combos",
            color = mainColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        if (combosList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No combos found context database.",
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 11.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(combosList) { combo ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(6.dp))
                            .border(0.5.dp, Color(android.graphics.Color.parseColor(combo.specialColor)).copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = combo.name,
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            // Draw nice sequence keys
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                modifier = Modifier.padding(top = 2.dp)
                            ) {
                                combo.sequence.split(",").forEach { input ->
                                    val keyColor = when(input) {
                                        "L" -> ElectricTeal
                                        "H" -> ActivePink
                                        else -> SolarAmber
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(15.dp)
                                            .background(keyColor.copy(alpha = 0.15f), RoundedCornerShape(3.dp))
                                            .border(0.5.dp, keyColor, RoundedCornerShape(3.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = input,
                                            color = keyColor,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                    }
                                }
                            }
                        }
                        
                        Text(
                            text = "${combo.damage} dmg",
                            color = Color(android.graphics.Color.parseColor(combo.specialColor)),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatBar(name: String, value: Float, displayVal: String, color: Color) {
    Column(modifier = Modifier.padding(vertical = 2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = name, color = Color.White.copy(alpha = 0.5f), fontSize = 8.sp, fontWeight = FontWeight.Bold)
            Text(text = displayVal, color = color, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }
        LinearProgressIndicator(
            progress = value.coerceIn(0f, 1f),
            color = color,
            trackColor = Color.White.copy(alpha = 0.08f),
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
        )
    }
}
