package com.patiocontrol.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.patiocontrol.ui.theme.PatioTheme

/**
 * Única Activity do app.
 *
 * Toda a navegação acontece em Compose, dentro do NavHost. Uma Activity só
 * é o padrão recomendado: menos ciclo de vida para coordenar e um único
 * ponto de entrada para o estado.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            PatioTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PatioNavHost()
                }
            }
        }
    }
}
