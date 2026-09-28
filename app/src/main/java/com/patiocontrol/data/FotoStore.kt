package com.patiocontrol.data

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

/**
 * As fotos de avaria ficam em arquivo no armazenamento privado do app, e o
 * banco guarda apenas o nome. Imagem em BLOB incha o banco, atrasa toda
 * consulta que faz SELECT * e não traz vantagem nenhuma aqui.
 */
class FotoStore(private val contexto: Context) {

    private val pasta: File
        get() = File(contexto.filesDir, "fotos").apply { if (!exists()) mkdirs() }

    fun arquivo(nome: String): File = File(pasta, nome)

    fun existe(nome: String): Boolean = nome.isNotBlank() && arquivo(nome).let { it.exists() && it.length() > 0 }

    /**
     * URI com permissão temporária, para entregar à câmera do sistema.
     *
     * O arquivo é criado vazio antes: parte dos aparelhos recusa gravar num
     * destino que ainda não existe, e o app fica com a foto perdida sem
     * nenhum erro visível.
     */
    fun uriPara(nome: String): Uri {
        val destino = arquivo(nome)
        runCatching { if (!destino.exists()) destino.createNewFile() }
        return FileProvider.getUriForFile(
            contexto,
            "${contexto.packageName}.fileprovider",
            destino
        )
    }

    fun apagar(nome: String) {
        if (nome.isBlank()) return
        runCatching { arquivo(nome).delete() }
    }

    fun tamanho(nome: String): Long = runCatching { arquivo(nome).length() }.getOrDefault(0L)

    fun todas(): List<File> = pasta.listFiles()?.toList().orEmpty()
}
