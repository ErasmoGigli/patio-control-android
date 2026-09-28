package com.patiocontrol.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.patiocontrol.domain.Configuracao
import com.patiocontrol.domain.Metas
import com.patiocontrol.domain.TipoAtivoGiro
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.random.Random

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "configuracao")

/**
 * Configuração e cadastros do aparelho.
 *
 * Fica em DataStore, não no banco: é um punhado de valores de leitura
 * constante, sem relação entre si e sem histórico. Listas são guardadas
 * unidas por quebra de linha, que é um caractere que nenhum cadastro tem.
 */
class ConfiguracaoStore(private val contexto: Context) {

    private object Chaves {
        val APARELHO = stringPreferencesKey("aparelho")
        val UNIDADE = stringPreferencesKey("unidade")
        val RESPONSAVEL = stringPreferencesKey("responsavel")
        val TURNO = stringPreferencesKey("turno")
        val SEQUENCIA = intPreferencesKey("sequencia")

        val META_ESPERA_DESCARGA = intPreferencesKey("meta_espera_descarga")
        val META_DESCARGA = intPreferencesKey("meta_descarga")
        val META_ESPERA_AG = intPreferencesKey("meta_espera_ag")
        val META_CARREGAMENTO_AG = intPreferencesKey("meta_carregamento_ag")
        val META_LIBERACAO = intPreferencesKey("meta_liberacao")
        val META_PERMANENCIA = intPreferencesKey("meta_permanencia")
        val META_CICLO = intPreferencesKey("meta_ciclo")
        val META_PRODUTIVO = intPreferencesKey("meta_produtivo_pct")

        val TRANSPORTADORAS = stringPreferencesKey("transportadoras")
        val ORIGENS = stringPreferencesKey("origens")
        val DESTINOS = stringPreferencesKey("destinos")
        val VEICULOS = stringPreferencesKey("veiculos")
        val TURNOS = stringPreferencesKey("turnos")
        val PRODUTOS = stringPreferencesKey("produtos")
        val UNIDADES = stringPreferencesKey("unidades")
        val TIPOS_AG = stringPreferencesKey("tipos_ag")
        val MOTIVOS_SEM_AG = stringPreferencesKey("motivos_sem_ag")
        val ITENS_AVARIA = stringPreferencesKey("itens_avaria")
        val ORIGENS_AVARIA = stringPreferencesKey("origens_avaria")
    }

    val configuracao: Flow<Configuracao> = contexto.dataStore.data.map { prefs ->
        // O código do aparelho é gerado uma única vez, por garantirAparelho().
        // Gerar aqui faria o identificador mudar a cada leitura do fluxo.
        val aparelho = prefs[Chaves.APARELHO] ?: ""
        val padrao = Configuracao.padrao(aparelho)

        padrao.copy(
            aparelho = aparelho,
            unidade = prefs[Chaves.UNIDADE] ?: padrao.unidade,
            responsavel = prefs[Chaves.RESPONSAVEL] ?: padrao.responsavel,
            turno = prefs[Chaves.TURNO] ?: padrao.turno,
            sequencia = prefs[Chaves.SEQUENCIA] ?: 0,
            metas = Metas(
                esperaDescarga = prefs[Chaves.META_ESPERA_DESCARGA],
                descarga = prefs[Chaves.META_DESCARGA],
                esperaAg = prefs[Chaves.META_ESPERA_AG],
                carregamentoAg = prefs[Chaves.META_CARREGAMENTO_AG],
                liberacao = prefs[Chaves.META_LIBERACAO],
                permanencia = prefs[Chaves.META_PERMANENCIA],
                ciclo = prefs[Chaves.META_CICLO],
                produtivo = prefs[Chaves.META_PRODUTIVO]?.let { it / 100.0 }
            ),
            transportadoras = lista(prefs[Chaves.TRANSPORTADORAS], padrao.transportadoras),
            origens = lista(prefs[Chaves.ORIGENS], padrao.origens),
            destinos = lista(prefs[Chaves.DESTINOS], padrao.destinos),
            veiculos = lista(prefs[Chaves.VEICULOS], padrao.veiculos),
            turnos = lista(prefs[Chaves.TURNOS], padrao.turnos),
            produtos = lista(prefs[Chaves.PRODUTOS], padrao.produtos),
            unidades = lista(prefs[Chaves.UNIDADES], padrao.unidades),
            tiposAg = prefs[Chaves.TIPOS_AG]?.let { bruto ->
                bruto.split('\n').filter { it.isNotBlank() }.map { linha ->
                    val partes = linha.split('\u001F')
                    TipoAtivoGiro(partes.getOrElse(0) { "" }, partes.getOrElse(1) { "UNIDADE" })
                }
            } ?: padrao.tiposAg,
            motivosSemAg = lista(prefs[Chaves.MOTIVOS_SEM_AG], padrao.motivosSemAg),
            itensAvaria = lista(prefs[Chaves.ITENS_AVARIA], padrao.itensAvaria),
            origensAvaria = lista(prefs[Chaves.ORIGENS_AVARIA], padrao.origensAvaria)
        )
    }

    suspend fun salvar(config: Configuracao) {
        contexto.dataStore.edit { p ->
            p[Chaves.APARELHO] = config.aparelho
            p[Chaves.UNIDADE] = config.unidade
            p[Chaves.RESPONSAVEL] = config.responsavel
            p[Chaves.TURNO] = config.turno
            p[Chaves.SEQUENCIA] = config.sequencia

            guardarMeta(p, Chaves.META_ESPERA_DESCARGA, config.metas.esperaDescarga)
            guardarMeta(p, Chaves.META_DESCARGA, config.metas.descarga)
            guardarMeta(p, Chaves.META_ESPERA_AG, config.metas.esperaAg)
            guardarMeta(p, Chaves.META_CARREGAMENTO_AG, config.metas.carregamentoAg)
            guardarMeta(p, Chaves.META_LIBERACAO, config.metas.liberacao)
            guardarMeta(p, Chaves.META_PERMANENCIA, config.metas.permanencia)
            guardarMeta(p, Chaves.META_CICLO, config.metas.ciclo)
            guardarMeta(p, Chaves.META_PRODUTIVO, config.metas.produtivo?.let { (it * 100).toInt() })

            p[Chaves.TRANSPORTADORAS] = config.transportadoras.joinToString("\n")
            p[Chaves.ORIGENS] = config.origens.joinToString("\n")
            p[Chaves.DESTINOS] = config.destinos.joinToString("\n")
            p[Chaves.VEICULOS] = config.veiculos.joinToString("\n")
            p[Chaves.TURNOS] = config.turnos.joinToString("\n")
            p[Chaves.PRODUTOS] = config.produtos.joinToString("\n")
            p[Chaves.UNIDADES] = config.unidades.joinToString("\n")
            p[Chaves.TIPOS_AG] = config.tiposAg.joinToString("\n") { "${it.tipo}\u001F${it.unidadePadrao}" }
            p[Chaves.MOTIVOS_SEM_AG] = config.motivosSemAg.joinToString("\n")
            p[Chaves.ITENS_AVARIA] = config.itensAvaria.joinToString("\n")
            p[Chaves.ORIGENS_AVARIA] = config.origensAvaria.joinToString("\n")
        }
    }

    /**
     * Garante que este aparelho tenha um código, criando um na primeira
     * execução. É chamado no início do app, antes de qualquer carreta.
     */
    suspend fun garantirAparelho(): String {
        var codigo = ""
        contexto.dataStore.edit { p ->
            codigo = p[Chaves.APARELHO]?.takeIf { it.isNotBlank() } ?: gerarCodigoAparelho()
            p[Chaves.APARELHO] = codigo
        }
        return codigo
    }

    /** Devolve o próximo número de sequência do aparelho e já o persiste. */
    suspend fun proximaSequencia(): Int {
        var proxima = 1
        contexto.dataStore.edit { p ->
            proxima = (p[Chaves.SEQUENCIA] ?: 0) + 1
            p[Chaves.SEQUENCIA] = proxima
        }
        return proxima
    }

    private fun guardarMeta(p: androidx.datastore.preferences.core.MutablePreferences, chave: Preferences.Key<Int>, valor: Int?) {
        if (valor == null) p.remove(chave) else p[chave] = valor
    }

    private fun lista(bruto: String?, padrao: List<String>): List<String> =
        bruto?.split('\n')?.filter { it.isNotBlank() } ?: padrao

    companion object {
        private const val ALFABETO = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"

        /** Quatro caracteres sem letras ambíguas: é o prefixo do id da
         *  carreta e o que evita dois aparelhos gerando o mesmo número. */
        fun gerarCodigoAparelho(): String =
            (1..4).map { ALFABETO[Random.nextInt(ALFABETO.length)] }.joinToString("")
    }
}
