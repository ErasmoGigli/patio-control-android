package com.patiocontrol.di

import android.content.Context
import com.patiocontrol.data.ConfiguracaoStore
import com.patiocontrol.data.FotoStore
import com.patiocontrol.data.local.PatioDatabase
import com.patiocontrol.data.repository.PatioRepository

/**
 * Injeção de dependência manual.
 *
 * O Google documenta este padrão para apps pequenos, e é uma escolha
 * deliberada: Hilt resolveria o mesmo grafo — um banco, três DAO, dois
 * armazenamentos e um repositório — ao custo de um processador de anotação,
 * geração de código e uma camada a mais para quem chega no projeto. Quando o
 * grafo crescer a ponto de doer, trocar por Hilt é mecânico, porque nada
 * fora daqui sabe como as dependências são construídas.
 */
class AppContainer(contexto: Context) {

    private val banco by lazy { PatioDatabase.obter(contexto) }

    val repositorio: PatioRepository by lazy {
        PatioRepository(
            carretaDao = banco.carretaDao(),
            ativoGiroDao = banco.ativoGiroDao(),
            avariaDao = banco.avariaDao(),
            configuracaoStore = ConfiguracaoStore(contexto),
            fotos = FotoStore(contexto)
        )
    }
}
