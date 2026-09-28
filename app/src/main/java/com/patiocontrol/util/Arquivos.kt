package com.patiocontrol.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** Marca de ordem de bytes: sem ela o Excel em português lê acento como lixo. */
private const val BOM = "﻿"

/**
 * Empacota a exportação num único .zip.
 *
 * Um arquivo só em vez de três CSV mais N fotos soltas: nada se perde no
 * caminho e a foto continua ligada ao registro pelo nome. Aqui é a
 * biblioteca padrão do Java que faz o trabalho — no app híbrido isso era
 * código escrito à mão porque o navegador não tem um empacotador.
 */
object Pacote {

    /**
     * @param textos arquivos de texto, gravados em UTF-8 com BOM
     * @param binarios arquivos já existentes em disco, copiados como estão
     */
    fun montar(
        contexto: Context,
        nome: String,
        textos: Map<String, String>,
        binarios: Map<String, File>
    ): File {
        val pasta = File(contexto.cacheDir, "pacotes").apply { if (!exists()) mkdirs() }
        val destino = File(pasta, nome)

        ZipOutputStream(FileOutputStream(destino).buffered()).use { zip ->
            textos.forEach { (caminho, conteudo) ->
                zip.putNextEntry(ZipEntry(caminho))
                zip.write((BOM + conteudo).toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
            binarios.forEach { (caminho, arquivo) ->
                if (!arquivo.exists()) return@forEach
                zip.putNextEntry(ZipEntry(caminho))
                arquivo.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
            }
        }

        return destino
    }

    /** Remove pacotes antigos do cache; o compartilhado já foi entregue. */
    fun limparAntigos(contexto: Context, manter: String? = null) {
        val pasta = File(contexto.cacheDir, "pacotes")
        pasta.listFiles()?.forEach { arquivo ->
            if (arquivo.name != manter) runCatching { arquivo.delete() }
        }
    }
}

/**
 * Entrega um arquivo a outro app pela folha de compartilhamento do Android.
 *
 * O URI vem do FileProvider e o Intent carrega FLAG_GRANT_READ_URI_PERMISSION:
 * desde o Android 7 passar um caminho de arquivo direto lança
 * FileUriExposedException.
 */
object Compartilhar {

    fun arquivo(contexto: Context, arquivo: File, tipo: String, titulo: String) {
        val uri: Uri = FileProvider.getUriForFile(
            contexto,
            "${contexto.packageName}.fileprovider",
            arquivo
        )

        val envio = Intent(Intent.ACTION_SEND).apply {
            type = tipo
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, titulo)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        contexto.startActivity(
            Intent.createChooser(envio, titulo).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}
