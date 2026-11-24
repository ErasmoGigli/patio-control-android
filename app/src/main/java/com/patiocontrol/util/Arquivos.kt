package com.patiocontrol.util

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Marca de ordem de bytes: sem ela o Excel em português lê acento como lixo.
 *
 * Escrita como escape e não como o caractere literal de propósito — o
 * caractere é invisível e não sobrevive a copiar e colar entre editores.
 */
private const val BOM = "\uFEFF"

/**
 * Empacota a exportação num único .zip.
 *
 * Um arquivo só em vez de três CSV mais N fotos soltas: nada se perde no
 * caminho e a foto continua ligada ao registro pelo nome.
 */
object Pacote {

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
 * Grava uma cópia do pacote onde o conferente escolher, pelo seletor de
 * arquivos do próprio Android.
 *
 * Existe porque compartilhar depende de haver outro app disposto a receber,
 * e nem sempre há: aparelho corporativo com compartilhamento restringido
 * pela política de dispositivos, ou aparelho recém-formatado. O seletor de
 * arquivos faz parte do sistema, então esta saída nunca fica indisponível.
 */
object Gravar {

    fun copiarPara(contexto: Context, origem: File, destino: Uri): Boolean {
        return try {
            val saida = contexto.contentResolver.openOutputStream(destino) ?: return false
            saida.use { origem.inputStream().use { entrada -> entrada.copyTo(saida) } }
            true
        } catch (e: IOException) {
            false
        } catch (e: SecurityException) {
            false
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

    /**
     * @return true se a folha de compartilhamento abriu. Quem chama só deve
     *   dar o registro por entregue depois de receber true.
     */
    fun arquivo(contexto: Context, arquivo: File, tipo: String, titulo: String): Boolean {
        val uri: Uri = try {
            FileProvider.getUriForFile(
                contexto,
                "${contexto.packageName}.fileprovider",
                arquivo
            )
        } catch (e: IllegalArgumentException) {
            return false
        }

        val envio = Intent(Intent.ACTION_SEND).apply {
            type = tipo
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, titulo)
            // O ClipData não é enfeite: é por ele que a folha de
            // compartilhamento recebe a permissão de leitura do URI.
            clipData = ClipData.newUri(contexto.contentResolver, arquivo.name, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val folha = Intent.createChooser(envio, titulo).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        // Aparelho sem nenhum app que aceite o tipo — emulador cru, ou
        // celular corporativo com compartilhamento restringido pelo MDM.
        // Deixar estourar aqui fecha o aplicativo na cara do conferente.
        return try {
            contexto.startActivity(folha)
            true
        } catch (e: ActivityNotFoundException) {
            false
        } catch (e: SecurityException) {
            false
        }
    }
}