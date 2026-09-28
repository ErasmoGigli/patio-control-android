package com.patiocontrol.util

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

/**
 * Geração do QR da passagem de turno.
 *
 * No app híbrido o codificador de QR foi escrito à mão — Reed-Solomon,
 * máscaras e tudo — porque o navegador não traz um. Aqui é uma chamada de
 * biblioteca madura, e o código que sobra é só o fatiamento: acima de um
 * certo tamanho o QR fica denso demais para a câmera de um celular de pátio
 * ler, então o conteúdo sai em partes numeradas.
 */
object Qr {

    /** Tamanho por parte que uma câmera comum lê sem esforço. */
    const val LIMITE_POR_PARTE = 1100

    /** Cabeçalho de cada parte: PARTE/<n>/<total> seguido do pedaço. */
    fun fatiar(texto: String): List<String> {
        if (texto.length <= LIMITE_POR_PARTE) return listOf(texto)
        val pedacos = texto.chunked(LIMITE_POR_PARTE)
        return pedacos.mapIndexed { indice, pedaco ->
            "PARTE/${indice + 1}/${pedacos.size}\n$pedaco"
        }
    }

    /** Junta as partes lidas; devolve null se ainda falta alguma. */
    fun juntar(partes: Collection<String>): String? {
        if (partes.isEmpty()) return null
        if (partes.size == 1 && !partes.first().startsWith("PARTE/")) return partes.first()

        val mapa = sortedMapOf<Int, String>()
        var total = -1
        partes.forEach { parte ->
            val quebra = parte.indexOf('\n')
            if (quebra < 0) return@forEach
            val cabecalho = parte.substring(0, quebra).split('/')
            if (cabecalho.size != 3 || cabecalho[0] != "PARTE") return@forEach
            val n = cabecalho[1].toIntOrNull() ?: return@forEach
            total = cabecalho[2].toIntOrNull() ?: return@forEach
            mapa[n] = parte.substring(quebra + 1)
        }

        if (total <= 0 || mapa.size != total) return null
        return mapa.values.joinToString("")
    }

    fun bitmap(texto: String, tamanho: Int = 720): Bitmap {
        val hints = mapOf(
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.L,
            EncodeHintType.MARGIN to 1,
            EncodeHintType.CHARACTER_SET to "UTF-8"
        )
        val matriz = QRCodeWriter().encode(texto, BarcodeFormat.QR_CODE, tamanho, tamanho, hints)
        val bmp = Bitmap.createBitmap(matriz.width, matriz.height, Bitmap.Config.RGB_565)
        for (x in 0 until matriz.width) {
            for (y in 0 until matriz.height) {
                bmp.setPixel(x, y, if (matriz[x, y]) Color.BLACK else Color.WHITE)
            }
        }
        return bmp
    }
}
