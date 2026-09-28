package com.patiocontrol

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.patiocontrol.data.local.AtivoGiroEntity
import com.patiocontrol.data.local.AvariaEntity
import com.patiocontrol.data.local.CarretaEntity
import com.patiocontrol.data.local.PatioDatabase
import com.patiocontrol.data.local.paraDominio
import com.patiocontrol.domain.Marco
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

@RunWith(AndroidJUnit4::class)
class CarretaDaoTest {

    private lateinit var banco: PatioDatabase

    @Before
    fun criar() {
        banco = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            PatioDatabase::class.java
        ).build()
    }

    @After
    fun fechar() = banco.close()

    private fun carreta(
        id: String = "TST-20260101-001",
        encerrada: Instant? = null,
        exportada: Instant? = null
    ) = CarretaEntity(
        id = id,
        data = LocalDate.of(2026, 1, 1),
        turno = "1º TURNO",
        transportadora = "FROTA PRÓPRIA",
        placa = "ABC1D23",
        placaCavalo = "",
        motorista = "",
        veiculo = "",
        origem = "NÃO INFORMADO",
        destino = "",
        documento = "",
        responsavel = "",
        produto = "",
        notaFiscal = "",
        quantidade = null,
        unidade = "",
        observacaoCarga = "",
        hProgramado = null,
        hChegada = LocalTime.of(7, 5),
        hEntrada = LocalTime.of(7, 40),
        hInicioDescarga = null,
        hFimDescarga = null,
        hInicioAg = null,
        hFimAg = null,
        hLiberacao = null,
        hSaida = null,
        respostaAg = "NAO_RESPONDIDO",
        justificativaSemAg = "",
        observacaoCarregamento = "",
        encerradaEm = encerrada,
        exportadaEm = exportada,
        transferidaEm = null
    )

    @Test
    fun grava_e_le_preservando_os_horarios() = runTest {
        banco.carretaDao().salvar(carreta())

        val lida = banco.carretaDao().buscar("TST-20260101-001")!!.paraDominio()

        assertEquals("ABC1D23", lida.placa)
        assertEquals(LocalTime.of(7, 5), lida.horario(Marco.CHEGADA))
        assertEquals(LocalTime.of(7, 40), lida.horario(Marco.ENTRADA))
        assertNull(lida.horario(Marco.SAIDA))
    }

    @Test
    fun aberta_some_do_patio_ao_encerrar() = runTest {
        banco.carretaDao().salvar(carreta())
        assertEquals(1, banco.carretaDao().observarAbertas().first().size)

        banco.carretaDao().salvar(carreta(encerrada = Instant.now()))
        assertEquals(0, banco.carretaDao().observarAbertas().first().size)
        assertEquals(1, banco.carretaDao().observarEncerradas().first().size)
    }

    @Test
    fun fila_de_exportacao_so_traz_encerrada_e_nao_exportada() = runTest {
        banco.carretaDao().salvar(carreta(id = "A", encerrada = Instant.now()))
        banco.carretaDao().salvar(carreta(id = "B", encerrada = Instant.now(), exportada = Instant.now()))
        banco.carretaDao().salvar(carreta(id = "C"))

        val fila = banco.carretaDao().paraExportar().map { it.carreta.id }
        assertEquals(listOf("A"), fila)
    }

    @Test
    fun marcar_exportadas_tira_da_fila() = runTest {
        banco.carretaDao().salvar(carreta(id = "A", encerrada = Instant.now()))
        banco.carretaDao().marcarExportadas(listOf("A"), Instant.now())
        assertTrue(banco.carretaDao().paraExportar().isEmpty())
    }

    @Test
    fun apagar_carreta_leva_junto_os_itens_filhos() = runTest {
        banco.carretaDao().salvar(carreta())
        banco.ativoGiroDao().inserir(
            AtivoGiroEntity(
                carretaId = "TST-20260101-001",
                tipo = "LITRINHO (300 ML)",
                quantidade = 30.0,
                unidade = "PALETE",
                documento = "",
                destino = "",
                observacao = ""
            )
        )
        banco.avariaDao().inserir(
            AvariaEntity(
                carretaId = "TST-20260101-001",
                item = "PALETE PBR1",
                quantidade = null,
                unidade = "",
                origem = "TRANSPORTE",
                observacao = "",
                foto = ""
            )
        )

        val antes = banco.carretaDao().buscar("TST-20260101-001")!!
        assertEquals(1, antes.ativosGiro.size)
        assertEquals(1, antes.avarias.size)

        banco.carretaDao().apagar("TST-20260101-001")
        assertNull(banco.carretaDao().buscar("TST-20260101-001"))
    }

    @Test
    fun placa_repetida_no_mesmo_dia_e_detectada() = runTest {
        banco.carretaDao().salvar(carreta(id = "A"))
        banco.carretaDao().salvar(carreta(id = "B"))

        val repetidas = banco.carretaDao()
            .contarPlacaNoDia("ABC1D23", LocalDate.of(2026, 1, 1), "A")
        assertEquals(1, repetidas)
    }
}
