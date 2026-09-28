package com.patiocontrol.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.patiocontrol.domain.Situacao

/**
 * Cor por situação. Verde é operação acontecendo, âmbar é fila, azul é
 * concluído, vermelho é impedimento. O conferente aprende a ler o pátio
 * pela cor antes de ler o texto.
 */
@Composable
fun corDaSituacao(situacao: Situacao): Color = when (situacao) {
    Situacao.EM_DESCARGA,
    Situacao.EM_CARREGAMENTO_AG -> MaterialTheme.colorScheme.tertiary

    Situacao.AGUARDANDO_ENTRADA,
    Situacao.AGUARDANDO_DESCARGA,
    Situacao.AGUARDANDO_CARREGAMENTO_AG,
    Situacao.AGUARDANDO_LIBERACAO -> MaterialTheme.colorScheme.secondary

    Situacao.PROGRAMADA,
    Situacao.LIBERADA,
    Situacao.FINALIZADA,
    Situacao.TRANSFERIDA -> MaterialTheme.colorScheme.primary

    Situacao.SAIDA_SEM_AG,
    Situacao.REGISTRO_INCOMPLETO -> MaterialTheme.colorScheme.error
}
