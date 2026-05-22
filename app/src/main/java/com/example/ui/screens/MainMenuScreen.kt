package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CarbonBlack
import com.example.ui.theme.DarkGreySurface
import com.example.ui.theme.ElectricTeal
import com.example.ui.theme.ActivePink
import com.example.ui.theme.SolarAmber
import com.example.viewmodel.GameMode
import com.example.viewmodel.GameScreen
import com.example.viewmodel.GameViewModel

@Composable
fun MainMenuScreen(viewModel: GameViewModel) {
    val gameMode by viewModel.gameMode.collectAsState()

    // Infinite animation for synthwave scanning grid
    val infiniteTransition = rememberInfiniteTransition(label = "grid_anim")
    val gridOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 60f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "grid_offset"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CarbonBlack)
    ) {
        // Neon Synthwave Retro Grid Background
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // Background subtle radial glow
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF1F1235), CarbonBlack),
                    center = Offset(width / 2f, height / 2f),
                    radius = height * 0.9f
                )
            )

            // Draw horizontal perspective brawler lines (expanding outwards at bottom)
            val horizonY = height * 0.35f
            val gridColor = Color(0xFF3F1B6B)
            
            // Perspective lines radiating from center horizon
            val lineCount = 18
            val centerX = width / 2f
            for (i in 0..lineCount) {
                val ratio = i.toFloat() / lineCount
                val startX = centerX + (ratio - 0.5f) * width * 0.15f
                val endX = centerX + (ratio - 0.5f) * width * 1.6f
                drawLine(
                    color = gridColor,
                    start = Offset(startX, horizonY),
                    end = Offset(endX, height),
                    strokeWidth = 2f
                )
            }

            // Moving Horizontal lines representing forward motion grid
            var yOffset = gridOffset
            while (yOffset < height - horizonY) {
                // progressive spacing calculation for simulated perspective depth
                val progress = yOffset / (height - horizonY)
                val currentY = horizonY + progress * (height - horizonY)
                drawLine(
                    color = gridColor.copy(alpha = 1f - progress),
                    start = Offset(0f, currentY),
                    end = Offset(width, currentY),
                    strokeWidth = 1.5f + (progress * 2.5f)
                )
                yOffset += 40f + (progress * 50f)
            }

            // Sunset grid horizon neon divider
            drawLine(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color.Transparent, ElectricTeal, ActivePink, Color.Transparent)
                ),
                start = Offset(0f, horizonY),
                end = Offset(width, horizonY),
                strokeWidth = 3f
            )
        }

        // Main Title and Menu controls Card
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header Zone: Arcade Game Title
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 20.dp)
            ) {
                Surface(
                    color = Color.Black.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .border(1.5.dp, ElectricTeal, RoundedCornerShape(12.dp))
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "ARCADE RETRO SERIES",
                        color = ElectricTeal,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 3.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Title with stacked drop-shadow neon glow
                Box {
                    Text(
                        text = "CLASH ARENA 2D",
                        color = ActivePink,
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.offset(x = 3.dp, y = 3.dp)
                    )
                    Text(
                        text = "CLASH ARENA 2D",
                        color = Color.White,
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center
                    )
                }

                Text(
                    text = "A Fighting Game with Customizable Combo Engines",
                    color = Color.White.copy(alpha = 0.65f),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // Center Zone: Multi Player Mode Selector and Launchers
            Column(
                modifier = Modifier
                    .widthIn(max = 480.dp)
                    .background(DarkGreySurface.copy(alpha = 0.85f), RoundedCornerShape(18.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(18.dp))
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "SELECT FIGHT TYPE",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Mode Row Selection (PVP vs CPU)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                        .padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Local PvP Option
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (gameMode == GameMode.PVP) ElectricTeal else Color.Transparent)
                            .clickable { viewModel.selectGameMode(GameMode.PVP) }
                            .testTag("mode_pvp_btn"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "LOCAL 2-PLAYER",
                            color = if (gameMode == GameMode.PVP) Color.Black else Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Bot AI Option
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (gameMode == GameMode.CPU) SolarAmber else Color.Transparent)
                            .clickable { viewModel.selectGameMode(GameMode.CPU) }
                            .testTag("mode_cpu_btn"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "VS PRACTICE BOT",
                            color = if (gameMode == GameMode.CPU) Color.Black else Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Enter Match Button
                Button(
                    onClick = { viewModel.navigateTo(GameScreen.CHAR_SELECT) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (gameMode == GameMode.PVP) ElectricTeal else SolarAmber,
                        contentColor = Color.Black
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .shadow(12.dp, RoundedCornerShape(12.dp))
                        .testTag("play_game_btn"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Fight Icon",
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ENTER COMBAT FIELD",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Combo Customizer Button
                OutlinedButton(
                    onClick = { viewModel.navigateTo(GameScreen.COMBO_CUSTOMIZER) },
                    border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.5.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("combo_customizer_btn"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Build,
                        contentDescription = "Build Icon",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "CUSTOMIZE CHARACTER COMBOS",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Footer Zone: Info / Tip
            Text(
                text = "💡 Tap the Customizer to define combo sequences with unique names like 'Pyre Rush' (L-L-H) and unleash them inside the arena!",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                lineHeight = 16.sp,
                modifier = Modifier.widthIn(max = 500.dp)
            )
        }
    }
}
