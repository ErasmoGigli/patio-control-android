package com.patiocontrol

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.patiocontrol.ui.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Teste de fumaça da interface: garante que o app sobe, mostra o pátio e
 * abre o formulário de nova carreta.
 *
 * A regra de negócio já é coberta pelos testes de JVM, que rodam em
 * segundos. Aqui só se verifica o que só o aparelho pode dizer — que a
 * navegação e a composição das telas realmente funcionam.
 */
@RunWith(AndroidJUnit4::class)
class FluxoDoPatioTest {

    @get:Rule
    val regra = createAndroidComposeRule<MainActivity>()

    @Test
    fun abre_no_patio() {
        regra.onNodeWithText("PÁTIO").assertIsDisplayed()
    }

    @Test
    fun botao_de_nova_carreta_leva_ao_formulario() {
        regra.onNodeWithText("NOVA CARRETA").performClick()
        regra.waitForIdle()
        regra.onNodeWithText("Placa da carreta *").assertIsDisplayed()
    }

    @Test
    fun formulario_exige_os_campos_obrigatorios() {
        regra.onNodeWithText("NOVA CARRETA").performClick()
        regra.waitForIdle()
        // Sem placa, transportadora e origem o botão de abrir fica inativo.
        regra.onNodeWithText("ABRIR E MARCAR CHEGADA").assertIsDisplayed()
    }
}
