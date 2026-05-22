package com.example.viewmodel

import android.app.Application
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.ComboDatabase
import com.example.data.model.CustomCombo
import com.example.data.model.FighterCharacter
import com.example.data.repo.ComboRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

enum class GameScreen {
    MENU,
    CHAR_SELECT,
    FIGHT,
    COMBO_CUSTOMIZER
}

enum class GameMode {
    PVP, // Local same-screen Player vs Player
    CPU  // Player vs Bot AI Practice Mode
}

enum class FighterState {
    IDLE,
    WALKING,
    JUMPING,
    SQUATTING,
    ATTACKING_LIGHT,
    ATTACKING_HEAVY,
    ATTACKING_SPECIAL, // Combo sequence unleashed
    HIT,               // Stunned when hit
    BLOCKING,          // Blocks incoming damage
    DODGING,
    DEFEATED
}

// Spark particle of light
data class Particle(
    val id: String = UUID.randomUUID().toString(),
    var x: Float,
    var y: Float,
    val vx: Float,
    val vy: Float,
    val color: Color,
    val size: Float,
    val maxLife: Int,
    var life: Int = maxLife
)

// Active fighter simulation model
data class FighterInstance(
    val character: FighterCharacter,
    var x: Float,
    var y: Float,
    var vx: Float = 0f,
    var vy: Float = 0f,
    var state: FighterState = FighterState.IDLE,
    var stateFrame: Int = 0, // Tick counter for animations/cooldowns
    var maxHealth: Int = character.maxHealth,
    var health: Float = character.maxHealth.toFloat(),
    var displaySubHealth: Float = character.maxHealth.toFloat(), // Slow drain effect
    var isP2: Boolean = false,
    var faceRight: Boolean = true,
    var inputBuffer: MutableList<String> = mutableListOf(),
    var lastInputTime: Long = 0L,
    var isShieldActive: Boolean = false,
    var shieldDuration: Int = 0,
    var hitStunFrames: Int = 0,
    var invincibilityFrames: Int = 0
) {
    // Height & Width bounds relative to arena standard 1000f x 400f coordinate space
    val width = 90f
    val height: Float
        get() = if (state == FighterState.SQUATTING) 85f else 135f

    // Check hit rectangle (AABB)
    fun getHitbox(): HitboxRect {
        return HitboxRect(
            left = x - width / 2f,
            right = x + width / 2f,
            top = y - height,
            bottom = y
        )
    }

    // Hitbox for attack strikes
    fun getAttackBox(reach: Float): HitboxRect {
        return if (faceRight) {
            HitboxRect(
                left = x + width / 2f,
                right = x + width / 2f + reach,
                top = y - height * 0.8f,
                bottom = y - height * 0.2f
            )
        } else {
            HitboxRect(
                left = x - width / 2f - reach,
                right = x - width / 2f,
                top = y - height * 0.8f,
                bottom = y - height * 0.2f
            )
        }
    }
}

data class HitboxRect(val left: Float, val right: Float, val top: Float, val bottom: Float) {
    fun intersects(other: HitboxRect): Boolean {
        return left < other.right && right > other.left && top < other.bottom && bottom > other.top
    }
}

// Combat log announcement
data class CombatAnnouncement(
    val title: String,
    val subtitle: String,
    val color: String,
    val timestamp: Long = System.currentTimeMillis()
)

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ComboRepository
    
    // UI Screen navigation
    private val _currentScreen = MutableStateFlow(GameScreen.MENU)
    val currentScreenText: StateFlow<GameScreen> = _currentScreen.asStateFlow()

    // Game mode PVP vs CPU Bot
    private val _gameMode = MutableStateFlow(GameMode.PVP)
    val gameMode: StateFlow<GameMode> = _gameMode.asStateFlow()

    // Database flow of combos
    val allCombos: StateFlow<List<CustomCombo>>

    // Selected fighter characters
    private val _p1Character = MutableStateFlow(FighterCharacter.Roster[0])
    val p1Character: StateFlow<FighterCharacter> = _p1Character.asStateFlow()

    private val _p2Character = MutableStateFlow(FighterCharacter.Roster[1])
    val p2Character: StateFlow<FighterCharacter> = _p2Character.asStateFlow()

    // Customizable state: currently editing custom combo for selection
    private val _editingCharacterId = MutableStateFlow("ignis")
    val editingCharacterId: StateFlow<String> = _editingCharacterId.asStateFlow()

    // In-match stats (Running only on game loop)
    private val _p1State = MutableStateFlow<FighterInstance?>(null)
    val p1State: StateFlow<FighterInstance?> = _p1State.asStateFlow()

    private val _p2State = MutableStateFlow<FighterInstance?>(null)
    val p2State: StateFlow<FighterInstance?> = _p2State.asStateFlow()

    // Particle FX
    private val _particles = MutableStateFlow<List<Particle>>(emptyList())
    val particles: StateFlow<List<Particle>> = _particles.asStateFlow()

    // Alerts/banners on screen (combo triggers)
    private val _announcement = MutableStateFlow<CombatAnnouncement?>(null)
    val announcement: StateFlow<CombatAnnouncement?> = _announcement.asStateFlow()

    // Screen shake value
    private val _screenShake = MutableStateFlow(0f)
    val screenShake: StateFlow<Float> = _screenShake.asStateFlow()

    // Match State Variables
    private val _winMessage = MutableStateFlow<String?>(null)
    val winMessage: StateFlow<String?> = _winMessage.asStateFlow()

    private val _matchTimer = MutableStateFlow(99)
    val matchTimer: StateFlow<Int> = _matchTimer.asStateFlow()

    private var gameLoopJob: Job? = null
    private var botUpdateCounter = 0
    private var combatSequenceHistory = mutableListOf<String>()

    init {
        val database = ComboDatabase.getDatabase(application, viewModelScope)
        repository = ComboRepository(database.comboDao())
        
        allCombos = repository.allCombos.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    // Navigation and Menu actions
    fun navigateTo(screen: GameScreen) {
        _currentScreen.value = screen
        if (screen == GameScreen.FIGHT) {
            initMatch()
        } else {
            stopGameLoop()
        }
    }

    fun selectGameMode(mode: GameMode) {
        _gameMode.value = mode
    }

    fun selectP1Character(character: FighterCharacter) {
        _p1Character.value = character
    }

    fun selectP2Character(character: FighterCharacter) {
        _p2Character.value = character
    }

    fun setEditingCharacterId(id: String) {
        _editingCharacterId.value = id
    }

    // Room DB Combo Operations
    fun saveCustomCombo(characterId: String, name: String, sequence: String, damage: Int, color: String) {
        viewModelScope.launch {
            val combo = CustomCombo(
                characterId = characterId,
                name = name,
                sequence = sequence,
                damage = damage,
                specialColor = color,
                isDefault = false
            )
            repository.insertCombo(combo)
        }
    }

    fun deleteCustomCombo(combo: CustomCombo) {
        if (!combo.isDefault) {
            viewModelScope.launch {
                repository.deleteCombo(combo)
            }
        }
    }

    fun resetAllCombosToDefault() {
        viewModelScope.launch {
            repository.clearCustomCombos()
        }
    }

    // Initialize New Match
    private fun initMatch() {
        _winMessage.value = null
        _matchTimer.value = 99
        _screenShake.value = 0f
        _announcement.value = null
        _particles.value = emptyList()

        val p1Char = _p1Character.value
        val p2Char = _p2Character.value

        _p1State.value = FighterInstance(
            character = p1Char,
            x = 200f,
            y = 350f,
            faceRight = true,
            isP2 = false
        )

        _p2State.value = FighterInstance(
            character = p2Char,
            x = 800f,
            y = 350f,
            faceRight = false,
            isP2 = true
        )

        startGameLoop()
    }

    // Start 60 FPS update loop
    private fun startGameLoop() {
        gameLoopJob?.cancel()
        gameLoopJob = viewModelScope.launch {
            var lastTimerTick = System.currentTimeMillis()
            while (_currentScreen.value == GameScreen.FIGHT) {
                val p1 = _p1State.value
                val p2 = _p2State.value

                if (p1 != null && p2 != null) {
                    // Tick duration update
                    val now = System.currentTimeMillis()
                    if (now - lastTimerTick >= 1000L && _winMessage.value == null) {
                        if (_matchTimer.value > 0) {
                            _matchTimer.value -= 1
                            if (_matchTimer.value == 0) {
                                determineTimeoutWinner(p1, p2)
                            }
                        }
                        lastTimerTick = now
                    }

                    // Bot AI Controller Tick
                    if (_gameMode.value == GameMode.CPU && _winMessage.value == null) {
                        updateBotAI(p2, p1)
                    }

                    // Update physics & combat
                    updateFightingGameState(p1, p2)
                }

                delay(16L) // ~60 FPS
            }
        }
    }

    private fun stopGameLoop() {
        gameLoopJob?.cancel()
        gameLoopJob = null
    }

    // Match Over Logic
    private fun declareWinner(winnerName: String) {
        _winMessage.value = "$winnerName WINS!"
        _screenShake.value = 15f
        viewModelScope.launch {
            delay(150L)
            _screenShake.value = 0f
        }
    }

    private fun determineTimeoutWinner(p1: FighterInstance, p2: FighterInstance) {
        if (p1.health > p2.health) {
            declareWinner(p1.character.name)
        } else if (p2.health > p1.health) {
            declareWinner(p2.character.name)
        } else {
            _winMessage.value = "DRAW MATCH!"
        }
    }

    // BOT STATE MACHINE (CPU Practice AI)
    private fun updateBotAI(bot: FighterInstance, target: FighterInstance) {
        botUpdateCounter++
        
        // Skip dead AI updates
        if (bot.state == FighterState.DEFEATED || bot.state == FighterState.HIT) return

        val dist = target.x - bot.x
        val absDist = abs(dist)

        // Turn to face enemy
        bot.faceRight = dist > 0

        // Periodic decision making
        if (botUpdateCounter % 15 == 0) {
            val decisionSeed = Random.nextFloat()

            if (absDist > 240f) {
                // Chase
                if (dist > 0) moveP2Right() else moveP2Left()
            } else if (absDist > 110f) {
                // Closer, walk or attempt combo initiation
                if (decisionSeed < 0.3f) {
                    bot.vx = if (dist > 0) 1.5f else -1.5f
                    bot.state = FighterState.WALKING
                } else if (decisionSeed < 0.6f) {
                    // Try triggers input combo
                    triggerP2Attack("L")
                } else {
                    // jump attack option
                    triggerP2Jump()
                }
            } else {
                // Close range: Combo Brawling or Block
                bot.vx = 0f
                if (target.state == FighterState.ATTACKING_LIGHT || target.state == FighterState.ATTACKING_HEAVY || target.state == FighterState.ATTACKING_SPECIAL) {
                    if (decisionSeed < 0.65f) {
                        // Intelligent Block!
                        triggerP2Special()
                    } else {
                        triggerP2Attack("L")
                    }
                } else {
                    // Attack
                    when {
                        decisionSeed < 0.4f -> triggerP2Attack("L")
                        decisionSeed < 0.7f -> triggerP2Attack("H")
                        else -> triggerP2Special() // This kicks off input sequence trigger
                    }
                }
            }
        }
    }

    // GAME LOOP CORE ENGINE
    private fun updateFightingGameState(p1: FighterInstance, p2: FighterInstance) {
        // Apply Gravity, friction and update fighter frames
        updateFighterPhysics(p1)
        updateFighterPhysics(p2)

        // Lock boundaries
        p1.x = max(50f, min(950f, p1.x))
        p2.x = max(50f, min(950f, p2.x))

        // Collision pushback so they don't walk right through each other completely
        val p1Hitbox = p1.getHitbox()
        val p2Hitbox = p2.getHitbox()
        if (p1Hitbox.intersects(p2Hitbox)) {
            val centerDiff = p1.x - p2.x
            val pushAmt = 4.5f
            if (centerDiff > 0) {
                p1.x += pushAmt
                p2.x -= pushAmt
            } else {
                p1.x -= pushAmt
                p2.x += pushAmt
            }
        }

        // Handle attacks and hitchecks
        processAttacks(p1, p2)
        processAttacks(p2, p1)

        // Simulating sub-health bar lag (Smooth decay of red health bar background)
        decaySubHealth(p1)
        decaySubHealth(p2)

        // Update Particle system
        val currentParticles = _particles.value.toMutableList()
        val iterator = currentParticles.iterator()
        while(iterator.hasNext()){
            val particle = iterator.next()
            particle.x += particle.vx
            particle.y += particle.vy
            particle.life--
            if(particle.life <= 0){
                iterator.remove()
            }
        }
        _particles.value = currentParticles

        // Slow Screen shake decay
        if (_screenShake.value > 0) {
            _screenShake.value = max(0f, _screenShake.value - 0.7f)
        }

        // Propagate updates to UI Flow triggers
        _p1State.value = p1.copy()
        _p2State.value = p2.copy()
    }

    private fun updateFighterPhysics(f: FighterInstance) {
        // State expiration and animation frames
        f.stateFrame++

        if (f.invincibilityFrames > 0) f.invincibilityFrames--

        // Handle specific states timers
        when (f.state) {
            FighterState.HIT -> {
                f.hitStunFrames--
                if (f.hitStunFrames <= 0) {
                    f.state = FighterState.IDLE
                }
            }
            FighterState.ATTACKING_LIGHT -> {
                if (f.stateFrame >= 14) { // 14 frames light recovery
                    f.state = FighterState.IDLE
                }
            }
            FighterState.ATTACKING_HEAVY -> {
                if (f.stateFrame >= 24) { // 24 frames heavy recovery
                    f.state = FighterState.IDLE
                }
            }
            FighterState.ATTACKING_SPECIAL -> {
                if (f.stateFrame >= 48) { // Special move sequence lockdown
                    unleashSpecialBlast(f)
                    f.state = FighterState.IDLE
                }
            }
            FighterState.BLOCKING -> {
                f.shieldDuration--
                if (f.shieldDuration <= 0) {
                    f.state = FighterState.IDLE
                    f.isShieldActive = false
                }
            }
            else -> {}
        }

        // Horizontal velocity
        if (f.state != FighterState.HIT) {
            f.x += f.vx
            // Apply air resistance / sliding friction
            f.vx *= 0.82f
            if (abs(f.vx) < 0.1f) {
                f.vx = 0f
                if (f.state == FighterState.WALKING) f.state = FighterState.IDLE
            }
        } else {
            // Knockback slide
            f.x += f.vx
            f.vx *= 0.90f
        }

        // Vertical elements (Gravity)
        if (f.y < 350f) {
            f.vy += 0.85f // gravity pull
            f.y += f.vy
            f.state = FighterState.JUMPING
        } else {
            f.y = 350f
            f.vy = 0f
            if (f.state == FighterState.JUMPING) {
                f.state = FighterState.IDLE
            }
        }
    }

    private fun decaySubHealth(f: FighterInstance) {
        if (f.displaySubHealth > f.health) {
            f.displaySubHealth -= 0.65f
            if (f.displaySubHealth < f.health) {
                f.displaySubHealth = f.health
            }
        }
    }

    // UNLEASH FIREBALL PROJECTILES OR SHOCKWAVES
    private fun unleashSpecialBlast(f: FighterInstance) {
        // Spawns burst particles in frontline
        val directionMult = if (f.faceRight) 1f else -1f
        val colorHex = if (f.character.id == "ignis") Color(0xFFFF5722) else if (f.character.id == "volt") Color(0xFF00E5FF) else Color(0xFF4CAF50)
        
        spawnExplosion(f.x + directionMult * 80f, f.y - 70f, colorHex, count = 18)
    }

    // PROCESS COOLDOWNS & ATTACK HITBOX REGISTRATION
    private fun processAttacks(attacker: FighterInstance, defender: FighterInstance) {
        if (defender.state == FighterState.DEFEATED) return

        val frame = attacker.stateFrame
        
        // Attack hit confirmation frame: Light punches active at mid windup (frame 4-7); Heavy kicks active at frame 8-12
        var isAttackActive = false
        var reach = 60f
        var rawDamage = 0f
        var knockback = 0f
        var statusColor = Color.White

        when (attacker.state) {
            FighterState.ATTACKING_LIGHT -> {
                if (frame in 3..6) {
                    isAttackActive = true
                    reach = 75f
                    rawDamage = 6f * attacker.character.baseDamageMult
                    knockback = 5f
                    statusColor = attacker.character.getPrimaryColor()
                }
            }
            FighterState.ATTACKING_HEAVY -> {
                if (frame in 7..11) {
                    isAttackActive = true
                    reach = 95f
                    rawDamage = 13f * attacker.character.baseDamageMult
                    knockback = 12f
                    statusColor = attacker.character.getAccentColor()
                }
            }
            FighterState.ATTACKING_SPECIAL -> {
                // High frame combo multihit
                if (frame in 10..30 && frame % 4 == 0) {
                    isAttackActive = true
                    reach = 140f
                    rawDamage = 8f * attacker.character.baseDamageMult
                    knockback = 4f
                    statusColor = Color.Magenta
                }
            }
            else -> {}
        }

        if (isAttackActive && defender.invincibilityFrames <= 0) {
            val attackBox = attacker.getAttackBox(reach)
            val defenderBox = defender.getHitbox()

            if (attackBox.intersects(defenderBox)) {
                // Determine block direction or shield invulnerable states
                val isBlocked = defender.state == FighterState.BLOCKING ||
                        (defender.character.id == "terra" && defender.invincibilityFrames > 0)

                if (isBlocked) {
                    // Blocked! Negligible chip damage
                    val chipDamage = rawDamage * 0.10f
                    defender.health = max(0f, defender.health - chipDamage)
                    
                    // Spawn shield spark particle blocks
                    val impactX = if (attacker.faceRight) defenderBox.left else defenderBox.right
                    spawnExplosion(impactX, defender.y - defender.height/2f, Color.White, count = 4)
                    
                    // Minor knockback push on block
                    attacker.vx += if (attacker.faceRight) -2.5f else 2.5f
                    defender.vx += if (attacker.faceRight) 3.5f else -3.5f
                    
                    // Prevent infinite damage recursion on active frames
                    defender.invincibilityFrames = 8 
                } else {
                    // Solid Hit! Deplete health
                    defender.health = max(0f, defender.health - rawDamage)
                    
                    // Sledge-hammer knockback slide
                    val dirMult = if (attacker.faceRight) 1f else -1f
                    defender.vx = dirMult * knockback
                    defender.vy = -knockback * 0.35f // slight pop-up
                    
                    defender.state = FighterState.HIT
                    defender.hitStunFrames = if (attacker.state == FighterState.ATTACKING_SPECIAL) 18 else 12
                    defender.invincibilityFrames = 10 // briefly avoid multi-collision on same move

                    _screenShake.value = if (attacker.state == FighterState.ATTACKING_SPECIAL) 10f else 6f

                    // Spawn blood-spark debris explosions
                    val impactX = (attacker.x + defender.x) / 2f
                    val impactY = defender.y - defender.height * 0.6f
                    spawnExplosion(impactX, impactY, statusColor, count = 12)

                    // Defeated state
                    if (defender.health <= 0f) {
                        defender.state = FighterState.DEFEATED
                        declareWinner(attacker.character.name)
                    }
                }
            }
        }
    }

    // PARTICLE SPARK SYSTEM
    private fun spawnExplosion(x: Float, y: Float, color: Color, count: Int) {
        val sparkles = mutableListOf<Particle>()
        for (i in 0 until count) {
            sparkles.add(
                Particle(
                    x = x,
                    y = y,
                    vx = Random.nextFloat() * 14f - 7f,
                    vy = Random.nextFloat() * 12f - 6f,
                    color = color,
                    size = Random.nextFloat() * 15f + 5f,
                    maxLife = Random.nextInt(15) + 12
                )
            )
        }
        _particles.value = _particles.value + sparkles
    }

    // COMBO BUFFER LOGIC (PLAYER INPUTS PARSING)
    private fun registerFighterInput(f: FighterInstance, input: String) {
        val now = System.currentTimeMillis()
        
        // Reset combo queue if timeline gap too long (> 1.2s between keystrokes)
        if (now - f.lastInputTime > 1200L) {
            f.inputBuffer.clear()
        }
        
        f.inputBuffer.add(input)
        if (f.inputBuffer.size > 4) {
            f.inputBuffer.removeAt(0)
        }
        f.lastInputTime = now

        // Check if input stream matches any known custom combo or defaults
        val currentBufferString = f.inputBuffer.joinToString(",")
        
        // Match starting from longest suffixes
        val listCombos = allCombos.value.filter { it.characterId == f.character.id }
        
        var triggeredCombo: CustomCombo? = null
        for (combo in listCombos) {
            if (currentBufferString.endsWith(combo.sequence)) {
                triggeredCombo = combo
                break
            }
        }

        if (triggeredCombo != null) {
            // TRIGGER COMBO ULTIMATE SEQUENCE!
            triggerComboUltimate(f, triggeredCombo)
            f.inputBuffer.clear() // clear to avoid instant loop triggers
        }
    }

    private fun triggerComboUltimate(f: FighterInstance, combo: CustomCombo) {
        f.state = FighterState.ATTACKING_SPECIAL
        f.stateFrame = 0
        f.vx = if (f.faceRight) 16f else -16f // quick rush thrust

        // If Terra forms his shield, grant absolute invincibility
        if (f.character.id == "terra" && combo.sequence.contains("S")) {
            f.invincibilityFrames = 60 // 1s full protection
        }

        // Large high-impact alerts
        _announcement.value = CombatAnnouncement(
            title = combo.name.uppercase(),
            subtitle = "P${if (f.isP2) "2" else "1"} CHAIN: ${combo.sequence.replace(",", " ➔ ")} (${combo.damage} DMG!)",
            color = combo.specialColor
        )

        // Reset text flash after 2 seconds
        viewModelScope.launch {
            delay(1800L)
            if (_announcement.value?.title == combo.name.uppercase()) {
                _announcement.value = null
            }
        }
    }

    // CONTROLLER HANDLERS FOR PLAYER 1
    fun moveP1Left() {
        val p1 = _p1State.value ?: return
        if (p1.state == FighterState.HIT || p1.state == FighterState.DEFEATED) return
        p1.vx = -p1.character.speed
        p1.faceRight = false
        p1.state = FighterState.WALKING
    }

    fun moveP1Right() {
        val p1 = _p1State.value ?: return
        if (p1.state == FighterState.HIT || p1.state == FighterState.DEFEATED) return
        p1.vx = p1.character.speed
        p1.faceRight = true
        p1.state = FighterState.WALKING
    }

    fun triggerP1Jump() {
        val p1 = _p1State.value ?: return
        if (p1.state == FighterState.HIT || p1.state == FighterState.DEFEATED) return
        if (p1.y >= 350f) {
            p1.vy = -18f
            p1.state = FighterState.JUMPING
        }
    }

    fun triggerP1Crouch(isCrouching: Boolean) {
        val p1 = _p1State.value ?: return
        if (p1.state == FighterState.HIT || p1.state == FighterState.DEFEATED) return
        if (isCrouching) {
            p1.state = FighterState.SQUATTING
            p1.vx = 0f
        } else if (p1.state == FighterState.SQUATTING) {
            p1.state = FighterState.IDLE
        }
    }

    fun triggerP1Attack(type: String) {
        val p1 = _p1State.value ?: return
        if (p1.state == FighterState.HIT || p1.state == FighterState.DEFEATED) return
        
        p1.stateFrame = 0
        p1.state = if (type == "H") FighterState.ATTACKING_HEAVY else FighterState.ATTACKING_LIGHT
        p1.vx = if (p1.faceRight) 3f else -3f // minor attack lunge
        
        registerFighterInput(p1, type)
    }

    fun triggerP1Special() {
        val p1 = _p1State.value ?: return
        if (p1.state == FighterState.HIT || p1.state == FighterState.DEFEATED) return
        
        p1.state = FighterState.BLOCKING
        p1.isShieldActive = true
        p1.shieldDuration = 40 // ~2/3 of a second block pose
        p1.stateFrame = 0
        p1.vx = 0f
        
        registerFighterInput(p1, "S")
    }

    // CONTROLLER HANDLERS FOR PLAYER 2
    fun moveP2Left() {
        val p2 = _p2State.value ?: return
        if (p2.state == FighterState.HIT || p2.state == FighterState.DEFEATED) return
        p2.vx = -p2.character.speed
        p2.faceRight = false
        p2.state = FighterState.WALKING
    }

    fun moveP2Right() {
        val p2 = _p2State.value ?: return
        if (p2.state == FighterState.HIT || p2.state == FighterState.DEFEATED) return
        p2.vx = p2.character.speed
        p2.faceRight = true
        p2.state = FighterState.WALKING
    }

    fun triggerP2Jump() {
        val p2 = _p2State.value ?: return
        if (p2.state == FighterState.HIT || p2.state == FighterState.DEFEATED) return
        if (p2.y >= 350f) {
            p2.vy = -18f
            p2.state = FighterState.JUMPING
        }
    }

    fun triggerP2Crouch(isCrouching: Boolean) {
        val p2 = _p2State.value ?: return
        if (p2.state == FighterState.HIT || p2.state == FighterState.DEFEATED) return
        if (isCrouching) {
            p2.state = FighterState.SQUATTING
            p2.vx = 0f
        } else if (p2.state == FighterState.SQUATTING) {
            p2.state = FighterState.IDLE
        }
    }

    fun triggerP2Attack(type: String) {
        val p2 = _p2State.value ?: return
        if (p2.state == FighterState.HIT || p2.state == FighterState.DEFEATED) return
        
        p2.stateFrame = 0
        p2.state = if (type == "H") FighterState.ATTACKING_HEAVY else FighterState.ATTACKING_LIGHT
        p2.vx = if (p2.faceRight) 3f else -3f
        
        registerFighterInput(p2, type)
    }

    fun triggerP2Special() {
        val p2 = _p2State.value ?: return
        if (p2.state == FighterState.HIT || p2.state == FighterState.DEFEATED) return
        
        p2.state = FighterState.BLOCKING
        p2.isShieldActive = true
        p2.shieldDuration = 40
        p2.stateFrame = 0
        p2.vx = 0f
        
        registerFighterInput(p2, "S")
    }

    override fun onCleared() {
        super.onCleared()
        stopGameLoop()
    }
}
