package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.ui.screens.CharacterSelectScreen
import com.example.ui.screens.CombatFieldScreen
import com.example.ui.screens.ComboCustomizerScreen
import com.example.ui.screens.MainMenuScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.GameScreen
import com.example.viewmodel.GameViewModel

class MainActivity : ComponentActivity() {
    private val gameViewModel: GameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    val currentScreen by gameViewModel.currentScreenText.collectAsState()

                    // Margin is automatically handled within game screens, but we support Scaffold padding bounding gracefully
                    Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                        when (currentScreen) {
                            GameScreen.MENU -> MainMenuScreen(viewModel = gameViewModel)
                            GameScreen.CHAR_SELECT -> CharacterSelectScreen(viewModel = gameViewModel)
                            GameScreen.COMBO_CUSTOMIZER -> ComboCustomizerScreen(viewModel = gameViewModel)
                            GameScreen.FIGHT -> CombatFieldScreen(viewModel = gameViewModel)
                        }
                    }
                }
            }
        }
    }
}
