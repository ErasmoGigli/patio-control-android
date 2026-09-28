package com.patiocontrol.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

/**
 * Paleta pensada para tela de celular sob sol de pátio: contraste alto,
 * cor usada como sinal e não como enfeite. Verde é operação em andamento,
 * âmbar é espera, vermelho é avaria ou impedimento.
 */
private val AzulProfundo = Color(0xFF12314F)
private val AzulClaro = Color(0xFF2E6FA7)
private val Ambar = Color(0xFFF2A23C)
private val Verde = Color(0xFF1E8E5A)
private val Vermelho = Color(0xFFC0392B)

private val ClaroFundo = Color(0xFFF4F6F9)
private val ClaroSuperficie = Color(0xFFFFFFFF)
private val ClaroTexto = Color(0xFF16202B)

private val EscuroFundo = Color(0xFF0E1822)
private val EscuroSuperficie = Color(0xFF16232F)
private val EscuroTexto = Color(0xFFE8EDF2)

private val EsquemaClaro = lightColorScheme(
    primary = AzulProfundo,
    onPrimary = Color.White,
    primaryContainer = AzulClaro,
    onPrimaryContainer = Color.White,
    secondary = Ambar,
    onSecondary = Color(0xFF3A2708),
    tertiary = Verde,
    onTertiary = Color.White,
    error = Vermelho,
    onError = Color.White,
    background = ClaroFundo,
    onBackground = ClaroTexto,
    surface = ClaroSuperficie,
    onSurface = ClaroTexto,
    surfaceVariant = Color(0xFFE6EBF1),
    onSurfaceVariant = Color(0xFF52606D),
    outline = Color(0xFFC3CCD6)
)

private val EsquemaEscuro = darkColorScheme(
    primary = Color(0xFF8FC0EA),
    onPrimary = Color(0xFF07121C),
    primaryContainer = AzulProfundo,
    onPrimaryContainer = Color.White,
    secondary = Ambar,
    onSecondary = Color(0xFF2A1C05),
    tertiary = Color(0xFF5CC292),
    onTertiary = Color(0xFF042415),
    error = Color(0xFFEF8377),
    onError = Color(0xFF2A0906),
    background = EscuroFundo,
    onBackground = EscuroTexto,
    surface = EscuroSuperficie,
    onSurface = EscuroTexto,
    surfaceVariant = Color(0xFF223140),
    onSurfaceVariant = Color(0xFFA9B7C4),
    outline = Color(0xFF32475A)
)

/** Cores de estado, fora do esquema porque não são papéis do Material. */
object CoresEstado {
    val operando: Color @Composable get() = MaterialTheme.colorScheme.tertiary
    val espera: Color @Composable get() = MaterialTheme.colorScheme.secondary
    val alerta: Color @Composable get() = MaterialTheme.colorScheme.error
    val neutro: Color @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant
}

private val tipografia = Typography(
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 32.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.8.sp
    )
)

@Composable
fun PatioTheme(
    escuro: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val esquema = if (escuro) EsquemaEscuro else EsquemaClaro
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val janela = (view.context as Activity).window
            janela.statusBarColor = esquema.primary.toArgb()
            WindowCompat.getInsetsController(janela, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = esquema,
        typography = tipografia,
        content = content
    )
}
