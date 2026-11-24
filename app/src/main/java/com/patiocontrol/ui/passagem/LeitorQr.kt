package com.patiocontrol.ui.passagem

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.LuminanceSource
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.patiocontrol.domain.Passagem
import com.patiocontrol.util.Qr
import kotlinx.coroutines.flow.MutableSharedFlow
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Leitura do QR da passagem de turno pela câmera.
 *
 * Existe porque o aparelho corporativo tem o compartilhamento restringido
 * pela política de dispositivos: com dois celulares lado a lado na troca de
 * turno, apontar a câmera é o único caminho que não depende de outro app
 * aceitar um arquivo.
 *
 * A decodificação é do próprio zxing que já gera o QR deste app, rodando
 * sobre o quadro cru da câmera. O conteúdo de um turno cheio não cabe num
 * QR só, então [Qr.fatiar] parte em pedaços numerados e aqui eles são
 * colhidos até fechar o conjunto — a ordem da leitura não importa, e ler o
 * mesmo pedaço duas vezes não atrapalha.
 */
@Composable
fun LeitorQr(aoFechar: () -> Unit, aoCompletar: (String) -> Unit) {
    val contexto = LocalContext.current
    val dono = LocalLifecycleOwner.current
    val completar by rememberUpdatedState(aoCompletar)

    var permitido by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(contexto, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var negado by remember { mutableStateOf(false) }

    val pedirCamera = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedida ->
        permitido = concedida
        negado = !concedida
    }

    LaunchedEffect(Unit) {
        if (!permitido) pedirCamera.launch(Manifest.permission.CAMERA)
    }

    // O analisador roda em thread própria; o fluxo é a ponte segura até a
    // composição, que é quem pode mexer na lista de partes.
    val lidos = remember { MutableSharedFlow<String>(extraBufferCapacity = 16) }
    val partes = remember { mutableStateListOf<String>() }
    var intruso by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        lidos.collect { texto ->
            if (!pareceNosso(texto)) {
                intruso = true
                return@collect
            }
            intruso = false
            if (partes.contains(texto)) return@collect
            partes += texto
            Qr.juntar(partes)?.let { completar(it) }
        }
    }

    Dialog(
        onDismissRequest = aoFechar,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            if (permitido) {
                CameraDeLeitura(dono) { lidos.tryEmit(it) }
            }

            Column(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.72f))
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    when {
                        !permitido && negado ->
                            "Sem acesso à câmera não dá para ler o QR. Libere a permissão " +
                                "ou receba o turno por arquivo."
                        !permitido -> "Aguardando a permissão de câmera."
                        intruso -> "Esse QR não é de passagem de turno. Aponte para o QR " +
                            "gerado pelo app do outro aparelho."
                        partes.isEmpty() -> "Aponte para o QR na tela do outro aparelho."
                        else -> {
                            val total = totalDePartes(partes)
                            if (total <= 1) "Lendo…"
                            else "Lidas ${partes.size} de $total partes. " +
                                "Passe para a próxima no outro aparelho."
                        }
                    },
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(12.dp))
                if (!permitido && negado) {
                    Button(
                        onClick = { negado = false; pedirCamera.launch(Manifest.permission.CAMERA) },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Pedir de novo") }
                    Spacer(Modifier.height(8.dp))
                }
                OutlinedButton(onClick = aoFechar, modifier = Modifier.fillMaxWidth()) {
                    Text("Fechar", color = Color.White)
                }
            }
        }
    }
}

/** A câmera traseira ligada ao ciclo de vida, com um analisador por quadro. */
@Composable
private fun CameraDeLeitura(
    dono: LifecycleOwner,
    aoLer: (String) -> Unit
) {
    val contexto = LocalContext.current
    val executor: ExecutorService = remember { Executors.newSingleThreadExecutor() }
    val leitura by rememberUpdatedState(aoLer)

    // O vínculo é com o ciclo de vida da tela inteira, que continua viva
    // quando o leitor fecha. Sem soltar a câmera aqui, ela ficaria ligada
    // consumindo bateria atrás de uma tela que já saiu.
    DisposableEffect(Unit) {
        onDispose {
            runCatching {
                val futuro = ProcessCameraProvider.getInstance(contexto)
                if (futuro.isDone) futuro.get().unbindAll()
            }
            executor.shutdown()
        }
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            val vista = PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            }
            val futuro = ProcessCameraProvider.getInstance(ctx)
            futuro.addListener({
                val provedor = runCatching { futuro.get() }.getOrNull()
                    ?: return@addListener

                val previa = Preview.Builder().build()
                    .also { it.setSurfaceProvider(vista.surfaceProvider) }

                val analise = ImageAnalysis.Builder()
                    // Só o quadro mais recente interessa: acumular fila faz a
                    // leitura responder ao que a câmera via segundos atrás.
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { it.setAnalyzer(executor, AnalisadorQr { texto -> leitura(texto) }) }

                runCatching {
                    provedor.unbindAll()
                    provedor.bindToLifecycle(
                        dono, CameraSelector.DEFAULT_BACK_CAMERA, previa, analise
                    )
                }
            }, ContextCompat.getMainExecutor(ctx))
            vista
        }
    )
}

/**
 * Decodifica o QR direto do plano de luminância do quadro.
 *
 * O primeiro plano do YUV já é a imagem em tons de cinza, que é exatamente
 * o que o zxing quer: não há conversão de cor nem bitmap intermediário no
 * caminho, e por isso a leitura acompanha a câmera mesmo em aparelho fraco.
 */
private class AnalisadorQr(private val aoLer: (String) -> Unit) : ImageAnalysis.Analyzer {

    private val leitor = MultiFormatReader().apply {
        setHints(mapOf(DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE)))
    }

    override fun analyze(imagem: ImageProxy) {
        try {
            val plano = imagem.planes.firstOrNull() ?: return
            val buffer = plano.buffer
            val dados = ByteArray(buffer.remaining())
            buffer.get(dados)

            val passo = plano.rowStride
            if (passo <= 0) return

            // O passo da linha costuma ser maior que a largura útil, e a
            // última linha pode vir incompleta. Medir pelo que chegou evita
            // estourar o vetor em aparelho que alinha o buffer.
            val alturaDoBuffer = dados.size / passo
            val largura = minOf(imagem.width, passo)
            val altura = minOf(imagem.height, alturaDoBuffer)
            if (largura <= 0 || altura <= 0) return

            val fonte = PlanarYUVLuminanceSource(
                dados, passo, alturaDoBuffer, 0, 0, largura, altura, false
            )

            val texto = decodificar(fonte) ?: decodificar(fonte.invert())
            if (texto != null) aoLer(texto)
        } catch (e: IllegalStateException) {
            // Quadro descartado no meio da leitura; o próximo vem logo.
        } finally {
            imagem.close()
        }
    }

    private fun decodificar(fonte: LuminanceSource): String? {
        val resultado = runCatching {
            leitor.decodeWithState(BinaryBitmap(HybridBinarizer(fonte)))
        }.getOrNull()
        leitor.reset()
        return resultado?.text
    }
}

/** Só aceita o que saiu deste app: um QR qualquer não vira parte do turno. */
private fun pareceNosso(texto: String): Boolean =
    texto.startsWith("PARTE/") || texto.startsWith(Passagem.CABECALHO)

/** Total declarado no cabeçalho de qualquer parte já lida; 1 se não houver. */
private fun totalDePartes(partes: List<String>): Int =
    partes.firstNotNullOfOrNull { parte ->
        parte.lineSequence().firstOrNull()
            ?.split('/')
            ?.takeIf { it.size == 3 && it[0] == "PARTE" }
            ?.get(2)
            ?.toIntOrNull()
    } ?: 1
