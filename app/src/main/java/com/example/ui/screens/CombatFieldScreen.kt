package com.example.ui.screens

import android.view.MotionEvent
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FighterCharacter
import com.example.ui.theme.*
import com.example.viewmodel.FighterInstance
import com.example.viewmodel.FighterState
import com.example.viewmodel.GameMode
import com.example.viewmodel.GameScreen
import com.example.viewmodel.GameViewModel
import kotlin.math.sin

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun CombatFieldScreen(viewModel: GameViewModel) {
    val p1State by viewModel.p1State.collectAsState()
    val p2State by viewModel.p2State.collectAsState()
    val particles by viewModel.particles.collectAsState()
    val announcement by viewModel.announcement.collectAsState()
    val screenShake by viewModel.screenShake.collectAsState()
    val winMessage by viewModel.winMessage.collectAsState()
    val matchTimer by viewModel.matchTimer.collectAsState()
    val gameMode by viewModel.gameMode.collectAsState()

    // Screen Shake Offset Calculation
    val shakeX = if (screenShake > 0) (sin(System.currentTimeMillis().toDouble() * 0.15) * screenShake).toFloat() else 0f
    val shakeY = if (screenShake > 0) (sin(System.currentTimeMillis().toDouble() * 0.22) * screenShake).toFloat() else 0f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CarbonBlack)
    ) {
        // MAIN FIGHT AREA (Canvas filling the background)
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .offset(x = shakeX.dp, y = shakeY.dp)
        ) {
            val width = constraints.maxWidth.toFloat()
            val height = constraints.maxHeight.toFloat()

            // Scale factor relative to game standard coordinate system (1000 x 400)
            val scaleX = width / 1000f
            val scaleY = height / 450f // compressed slightly to accommodate bottom status safe area

            Canvas(modifier = Modifier.fillMaxSize()) {
                // Draw Arena Background Stage
                drawArenaBackground(width, height)

                // Draw Players
                val p1 = p1State
                if (p1 != null) {
                    drawFighter(p1, scaleX, scaleY)
                }

                val p2 = p2State
                if (p2 != null) {
                    drawFighter(p2, scaleX, scaleY)
                }

                // Draw Spark Particles
                particles.forEach { p ->
                    val cX = p.x * scaleX
                    val cY = p.y * scaleY
                    val maxRadius = p.size * scaleX
                    val currentRadius = maxRadius * (p.life.toFloat() / p.maxLife)
                    if (currentRadius > 0.1f) {
                        drawCircle(
                            color = p.color,
                            radius = currentRadius,
                            center = Offset(cX, cY)
                        )
                    }
                }
            }

            // HUD OVERLAYS (Top HUD health bars and Timer)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // PLAYER 1 HEALTH CARD (LEFT)
                    p1State?.let { fighter ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .border(1.5.dp, ElectricTeal.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                .padding(3.dp)
                        ) {
                            // Sub-health (Red Draining under-bar)
                            val subProgress = fighter.displaySubHealth / fighter.maxHealth.toFloat()
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(subProgress.coerceIn(0f, 1f))
                                    .background(SolidRed.copy(alpha = 0.7f), RoundedCornerShape(5.dp))
                            )
                            // Main Green health bar
                            val mainProgress = fighter.health / fighter.maxHealth.toFloat()
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(mainProgress.coerceIn(0f, 1f))
                                    .background(AcidGreen, RoundedCornerShape(5.dp))
                            )

                            // Name Label overlay
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "P1: ${fighter.character.name.uppercase()}",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Text(
                                    text = "${fighter.health.toInt()}/${fighter.maxHealth}",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // CENTRAL TIMER (ROUND SHIELD)
                    Surface(
                        modifier = Modifier
                            .padding(horizontal = 14.dp)
                            .size(50.dp)
                            .border(2.dp, Color.White, CircleShape)
                            .shadow(8.dp, CircleShape),
                        color = DarkGreySurface,
                        shape = CircleShape
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "$matchTimer",
                                color = if (matchTimer <= 15) ActivePink else Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    // PLAYER 2 HEALTH CARD (RIGHT)
                    p2State?.let { fighter ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .border(
                                    1.5.dp, 
                                    if (gameMode == GameMode.PVP) ActivePink.copy(alpha = 0.4f) else SolarAmber.copy(alpha = 0.4f), 
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(3.dp)
                        ) {
                            // Sub-health (Red Draining under-bar) Is flipped symmetrically? No, simple left-to-right bar is robust
                            val subProgress = fighter.displaySubHealth / fighter.maxHealth.toFloat()
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(subProgress.coerceIn(0f, 1f))
                                    .background(SolidRed.copy(alpha = 0.7f), RoundedCornerShape(5.dp))
                            )
                            // Main Green health bar
                            val mainProgress = fighter.health / fighter.maxHealth.toFloat()
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(mainProgress.coerceIn(0f, 1f))
                                    .background(
                                        if (gameMode == GameMode.PVP) ActivePink else SolarAmber,
                                        RoundedCornerShape(5.dp)
                                    )
                            )

                            // Name Label overlay
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (gameMode == GameMode.PVP) "P2: ${fighter.character.name.uppercase()}" else "CPU: ${fighter.character.name.uppercase()}",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Text(
                                    text = "${fighter.health.toInt()}/${fighter.maxHealth}",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // COMBO ANNOLNCEMENT BANNER
                AnimatedVisibility(
                    visible = announcement != null,
                    enter = slideInVertically { -it } + fadeIn(),
                    exit = slideOutVertically { -it } + fadeOut(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                ) {
                    announcement?.let { announce ->
                        val fxColor = Color(android.graphics.Color.parseColor(announce.color))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Black.copy(alpha = 0.82f), RoundedCornerShape(10.dp))
                                .border(1.5.dp, fxColor, RoundedCornerShape(10.dp))
                                .padding(vertical = 10.dp, horizontal = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = announce.title,
                                color = fxColor,
                                fontSize = 21.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 2.sp,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = announce.subtitle,
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
            }
        }

        // TRANSPARENT CONTROLLERS OVERLAYS (Flanking bottom left & right corners)
        // Only render controllers if match is NOT ended to maximize focus
        if (winMessage == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                // PLAYER 1 CONTROLLER (BOTTOM-LEFT CORNER)
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .width(260.dp)
                        .height(130.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    // Directional D-PAD (L-R-U-D) Symmetrically
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Jump Button
                        TriggerPadButton(
                            label = "▲",
                            testTag = "p1_btn_jump",
                            color = ElectricTeal,
                            onTouch = { isPressed ->
                                if (isPressed) viewModel.triggerP1Jump()
                            }
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            // Walk Left
                            TriggerPadButton(
                                label = "◀",
                                testTag = "p1_btn_left",
                                color = ElectricTeal,
                                onTouch = { isPressed ->
                                    if (isPressed) viewModel.moveP1Left()
                                }
                            )

                            // Crouch
                            TriggerPadButton(
                                label = "▼",
                                testTag = "p1_btn_crouch",
                                color = ElectricTeal,
                                onTouch = { isPressed ->
                                    viewModel.triggerP1Crouch(isPressed)
                                }
                            )

                            // Walk Right
                            TriggerPadButton(
                                label = "▶",
                                testTag = "p1_btn_right",
                                color = ElectricTeal,
                                onTouch = { isPressed ->
                                    if (isPressed) viewModel.moveP1Right()
                                }
                            )
                        }
                    }

                    // Action Buttons Cluster: Light (L), Heavy (H), Special (S)
                    Box(modifier = Modifier.size(110.dp)) {
                        // Light Key
                        Box(modifier = Modifier.offset(x = 0.dp, y = 50.dp)) {
                            TriggerPadButton(
                                label = "L",
                                testTag = "p1_btn_light",
                                color = ElectricTeal,
                                onTouch = { isPressed ->
                                    if (isPressed) viewModel.triggerP1Attack("L")
                                }
                            )
                        }

                        // Heavy Key
                        Box(modifier = Modifier.offset(x = 55.dp, y = 10.dp)) {
                            TriggerPadButton(
                                label = "H",
                                testTag = "p1_btn_heavy",
                                color = ActivePink,
                                onTouch = { isPressed ->
                                    if (isPressed) viewModel.triggerP1Attack("H")
                                }
                            )
                        }

                        // Special/Block Key
                        Box(modifier = Modifier.offset(x = 55.dp, y = 70.dp)) {
                            TriggerPadButton(
                                label = "S",
                                testTag = "p1_btn_special",
                                color = SolarAmber,
                                onTouch = { isPressed ->
                                    if (isPressed) viewModel.triggerP1Special()
                                }
                            )
                        }
                    }
                }

                // PLAYER 2 CONTROLLER (BOTTOM-RIGHT CORNER) - Hide/Disable if against BOT
                if (gameMode == GameMode.PVP) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .width(260.dp)
                            .height(130.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        // Action Buttons Cluster (P2 uses L, H, S)
                        Box(modifier = Modifier.size(110.dp)) {
                            // Light Key
                            Box(modifier = Modifier.offset(x = 0.dp, y = 10.dp)) {
                                TriggerPadButton(
                                    label = "L",
                                    testTag = "p2_btn_light",
                                    color = ElectricTeal,
                                    onTouch = { isPressed ->
                                        if (isPressed) viewModel.triggerP2Attack("L")
                                    }
                                )
                            }

                            // Heavy Key
                            Box(modifier = Modifier.offset(x = 0.dp, y = 70.dp)) {
                                TriggerPadButton(
                                    label = "H",
                                    testTag = "p2_btn_heavy",
                                    color = ActivePink,
                                    onTouch = { isPressed ->
                                        if (isPressed) viewModel.triggerP2Attack("H")
                                    }
                                )
                            }

                            // Special/Shield Key
                            Box(modifier = Modifier.offset(x = 55.dp, y = 50.dp)) {
                                TriggerPadButton(
                                    label = "S",
                                    testTag = "p2_btn_special",
                                    color = SolarAmber,
                                    onTouch = { isPressed ->
                                        if (isPressed) viewModel.triggerP2Special()
                                    }
                                )
                            }
                        }

                        // Directional Buttons P2
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // Jump
                            TriggerPadButton(
                                label = "▲",
                                testTag = "p2_btn_jump",
                                color = ActivePink,
                                onTouch = { isPressed ->
                                    if (isPressed) viewModel.triggerP2Jump()
                                }
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                // Left
                                TriggerPadButton(
                                    label = "◀",
                                    testTag = "p2_btn_left",
                                    color = ActivePink,
                                    onTouch = { isPressed ->
                                        if (isPressed) viewModel.moveP2Left()
                                    }
                                )

                                // Crouch
                                TriggerPadButton(
                                    label = "▼",
                                    testTag = "p2_btn_crouch",
                                    color = ActivePink,
                                    onTouch = { isPressed ->
                                        viewModel.triggerP2Crouch(isPressed)
                                    }
                                )

                                // Right
                                TriggerPadButton(
                                    label = "▶",
                                    testTag = "p2_btn_right",
                                    color = ActivePink,
                                    onTouch = { isPressed ->
                                        if (isPressed) viewModel.moveP2Right()
                                    }
                                )
                            }
                        }
                    }
                } else {
                    // Solo Training Tips: Displays current active combo chains helper at the bottom right corner of the brawler
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .width(220.dp)
                            .background(DarkGreySurface.copy(alpha = 0.7f), RoundedCornerShape(10.dp))
                            .border(1.dp, ElectricTeal.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "TRAINING CHEAT SHEET",
                            color = ElectricTeal,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Execute your customizable combo inputs by taping keys in exact sequences:",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 8.sp,
                            lineHeight = 11.sp
                        )
                        p1State?.let { fighter ->
                            Text(
                                text = "Active character: ${fighter.character.name}",
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // MATCH COMPLETED WINNER BANNER COVER OVERLAY
        AnimatedVisibility(
            visible = winMessage != null,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .width(340.dp)
                        .background(DarkGreySurface, RoundedCornerShape(18.dp))
                        .border(2.dp, ElectricTeal, RoundedCornerShape(18.dp))
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Surface(
                        color = ElectricTeal.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(bottom = 4.dp)
                    ) {
                        Text(
                            text = "ROUND CLEAR",
                            color = ElectricTeal,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }

                    Text(
                        text = winMessage ?: "KO",
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Rematch CTA
                    Button(
                        onClick = { viewModel.navigateTo(GameScreen.FIGHT) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ElectricTeal,
                            contentColor = Color.Black
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("rematch_btn"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Rematch")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "REMATCH COMBAT",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    // Exit to menu
                    OutlinedButton(
                        onClick = { viewModel.navigateTo(GameScreen.MENU) },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("exit_to_menu_btn"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Home, contentDescription = "Home")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "EXIT TO MAIN MENU",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

// 48DP ACCESSIBILITY COMPLIANT TOUCH TRANSPARENT CONTROLLERS BUTTON
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun TriggerPadButton(
    label: String,
    testTag: String,
    color: Color,
    onTouch: (isPressed: Boolean) -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .size(48.dp) // Accessibility standard minimum Touch target
            .clip(CircleShape)
            .background(
                if (isPressed) color.copy(alpha = 0.5f)
                else color.copy(alpha = 0.15f)
            )
            .border(2.dp, color, CircleShape)
            .pointerInteropFilter { event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        isPressed = true
                        onTouch(true)
                        true
                    }

                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        isPressed = false
                        onTouch(false)
                        true
                    }

                    else -> false
                }
            }
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Black
        )
    }
}

// STAGE RENDERING SYSTEM
fun DrawScope.drawArenaBackground(w: Float, h: Float) {
    val floorY = 350f * (h / 450f)

    // Deep space horizon gradient overlay
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFF0F0F12), Color(0xFF1E1E28), Color(0xFF15151A))
        ),
        size = Size(w, h)
    )

    // Giant neon moon/sun on background
    drawCircle(
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFFFF0055).copy(alpha = 0.25f), Color(0xFFFF9F00).copy(alpha = 0.01f))
        ),
        radius = h * 0.42f,
        center = Offset(w / 2f, h * 0.40f)
    )

    // Grid Floor
    drawRect(
        color = Color(0xFF1E132D),
        topLeft = Offset(0f, floorY),
        size = Size(w, h - floorY)
    )

    // Perspective floor lines of the arena brawler field
    val horizonLineCount = 14
    for (i in 0..horizonLineCount) {
        val ratio = i.toFloat() / horizonLineCount
        val lineX = ratio * w
        drawLine(
            color = Color(0xFF4A1E8C).copy(alpha = 0.35f),
            start = Offset(w/2f + (ratio - 0.5f)*w*0.2f, floorY),
            end = Offset(lineX, h),
            strokeWidth = 2f
        )
    }

    // Floor edge neon divide line
    drawLine(
        brush = Brush.horizontalGradient(
            colors = listOf(Color.Transparent, ElectricTeal, ActivePink, Color.Transparent)
        ),
        start = Offset(0f, floorY),
        end = Offset(w, floorY),
        strokeWidth = 3f
    )
}

// FIGHTER RENDERING PIPELINE
fun DrawScope.drawFighter(f: FighterInstance, sX: Float, sY: Float) {
    val fX = f.x * sX
    val fY = f.y * sY

    val primaryCol = f.character.getPrimaryColor()
    val accentCol = f.character.getAccentColor()

    // 1. FLOOR REFLECTIVE SHADOW
    val shadowWidth = f.width * sX
    val shadowHeight = 12f * sY
    drawOval(
        color = Color.Black.copy(alpha = 0.45f),
        topLeft = Offset(fX - shadowWidth / 2f, 350f * sY - shadowHeight),
        size = Size(shadowWidth, shadowHeight)
    )

    // Dead animation skip
    if (f.state == FighterState.DEFEATED) {
        // Draw character lying down flat
        drawRoundRect(
            color = primaryCol.copy(alpha = 0.5f),
            topLeft = Offset(fX - 60f * sX, 350f * sY - 30f * sY),
            size = Size(120f * sX, 22f * sY),
            cornerRadius = CornerRadius(6f * sX, 6f * sY)
        )
        return
    }

    // Blink effect if invulnerable (getting hit frames)
    if (f.invincibilityFrames > 0 && (f.stateFrame / 3) % 2 == 0) {
        return
    }

    // Scaling bounds
    val widthPx = f.width * sX
    val heightPx = f.height * sY

    // 2. STATE GLOW EFFECTS (Glow on Special moves or Invulnerable Shield states)
    if (f.state == FighterState.ATTACKING_SPECIAL) {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(accentCol.copy(alpha = 0.45f), Color.Transparent)
            ),
            radius = widthPx * 1.5f,
            center = Offset(fX, fY - heightPx / 2f)
        )
    }

    // 3. CAPSLLE CORE BODY SHAPE
    val bodyRadius = 14f * sX
    val bodyLeft = fX - widthPx * 0.4f
    val bodyTop = fY - heightPx * 0.75f
    val bodyWidth = widthPx * 0.8f
    val bodyHeight = heightPx * 0.65f

    drawRoundRect(
        color = primaryCol,
        topLeft = Offset(bodyLeft, bodyTop),
        size = Size(bodyWidth, bodyHeight),
        cornerRadius = CornerRadius(bodyRadius, bodyRadius)
    )

    // Inner stylized core glowing chest piece (like iron man reactor)
    drawRoundRect(
        color = accentCol,
        topLeft = Offset(bodyLeft + bodyWidth * 0.25f, bodyTop + bodyHeight * 0.25f),
        size = Size(bodyWidth * 0.5f, bodyHeight * 0.35f),
        cornerRadius = CornerRadius(bodyRadius * 0.6f, bodyRadius * 0.6f)
    )

    // 4. CIRCULAR HEAD
    val headRadius = 24f * sX
    val headCenterY = fY - heightPx * 0.84f
    drawCircle(
        color = Color(0xFFE0E0E0), // stylized white helmet or head
        radius = headRadius,
        center = Offset(fX, headCenterY)
    )

    // 4b. UNIQUE CHARACTER HEADGEAR EXTRAS
    when (f.character.id) {
        "ignis" -> {
            // Pyro flaming bandana tails
            val path = Path().apply {
                moveTo(fX, headCenterY - headRadius)
                lineTo(fX - headRadius * 1.4f * (if (f.faceRight) 1f else -1f), headCenterY - headRadius * 0.2f)
                lineTo(fX, headCenterY + headRadius * 0.1f)
                close()
            }
            drawPath(path, color = accentCol)
        }
        "volt" -> {
            // Neon glowing lightning visor strip across helmet eyes
            val visorLeft = fX - headRadius * 0.8f
            val visorTop = headCenterY - headRadius * 0.2f
            drawRoundRect(
                color = ElectricTeal,
                topLeft = Offset(visorLeft, visorTop),
                size = Size(headRadius * 1.6f, headRadius * 0.4f),
                cornerRadius = CornerRadius(3f * sX, 3f * sY)
            )
        }
        "terra" -> {
            // Massive horns/crown protruding from stone helmet
            val pathL = Path().apply {
                moveTo(fX - headRadius * 0.5f, headCenterY - headRadius * 0.7f)
                lineTo(fX - headRadius * 1.1f, headCenterY - headRadius * 1.5f)
                lineTo(fX - headRadius * 0.1f, headCenterY - headRadius * 0.9f)
                close()
            }
            val pathR = Path().apply {
                moveTo(fX + headRadius * 0.1f, headCenterY - headRadius * 0.9f)
                lineTo(fX + headRadius * 1.1f, headCenterY - headRadius * 1.5f)
                lineTo(fX + headRadius * 0.5f, headCenterY - headRadius * 0.7f)
                close()
            }
            drawPath(pathL, color = accentCol)
            drawPath(pathR, color = accentCol)
        }
    }

    // 4c. AGITATION EYES (SqLints focused in combat angle facing direction)
    val eyeOffsetX = if (f.faceRight) headRadius * 0.35f else -headRadius * 0.65f
    val eyeWidth = headRadius * 0.3f
    val eyeHeight = 5f * sY
    // Draw left and right eyes
    drawRect(
        color = if (f.state == FighterState.HIT) SolidRed else Color.Black,
        topLeft = Offset(fX + eyeOffsetX, headCenterY - 6f * sY),
        size = Size(eyeWidth, eyeHeight)
    )
    drawRect(
        color = if (f.state == FighterState.HIT) SolidRed else Color.Black,
        topLeft = Offset(fX + eyeOffsetX + headRadius * 0.3f, headCenterY - 6f * sY),
        size = Size(eyeWidth, eyeHeight)
    )

    // 5. HANDS AND ARMS (ANIMATED EXTENSION ON STRIKING)
    val fistRadius = 13f * sX
    val facingDir = if (f.faceRight) 1f else -1f

    when (f.state) {
        FighterState.ATTACKING_LIGHT -> {
            // Fast extended light punch strike
            val punchReach = widthPx * 0.85f * facingDir
            val armY = fY - heightPx * 0.5f
            drawLine(
                color = primaryCol,
                start = Offset(fX, armY),
                end = Offset(fX + punchReach, armY),
                strokeWidth = 10f * sX
            )
            drawCircle(
                color = accentCol,
                radius = fistRadius * 1.1f,
                center = Offset(fX + punchReach, armY)
            )
        }
        FighterState.ATTACKING_HEAVY -> {
            // Huge heavy landing boot kick slash
            val kickReach = widthPx * 1.05f * facingDir
            val kickY = fY - heightPx * 0.2f
            drawLine(
                color = primaryCol,
                start = Offset(fX, fY - heightPx * 0.4f),
                end = Offset(fX + kickReach, kickY),
                strokeWidth = 14f * sX
            )
            drawCircle(
                color = accentCol,
                radius = fistRadius * 1.3f,
                center = Offset(fX + kickReach, kickY)
            )
        }
        FighterState.BLOCKING -> {
            // Draws cross guarded arms defensively covering face block angles
            val shieldX = fX + headRadius * 0.8f * facingDir
            drawCircle(
                color = accentCol,
                radius = widthPx * 0.6f,
                center = Offset(fX, fY - heightPx / 2f),
                style = Stroke(width = 3f * sX)
            )
            drawCircle(
                color = primaryCol,
                radius = fistRadius * 1.1f,
                center = Offset(shieldX, fY - heightPx * 0.5f)
            )
        }
        else -> {
            // Idle arms bouncing slowly inside frame
            val bounceOffset = sin(System.currentTimeMillis().toDouble() * 0.007) * 4f * sY
            val leftHandX = fX - widthPx * 0.45f
            val rightHandX = fX + widthPx * 0.45f
            val handY = fY - heightPx * 0.45f + bounceOffset.toFloat()

            drawCircle(
                color = primaryCol,
                radius = fistRadius,
                center = Offset(leftHandX, handY)
            )
            drawCircle(
                color = primaryCol,
                radius = fistRadius,
                center = Offset(rightHandX, handY)
            )
        }
    }
}
