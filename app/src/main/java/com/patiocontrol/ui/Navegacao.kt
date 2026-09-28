package com.patiocontrol.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.patiocontrol.PatioApplication
import com.patiocontrol.data.repository.PatioRepository
import com.patiocontrol.ui.ag.AgItemScreen
import com.patiocontrol.ui.ag.AgListaScreen
import com.patiocontrol.ui.ajustes.AjustesScreen
import com.patiocontrol.ui.avaria.AvariaItemScreen
import com.patiocontrol.ui.avaria.AvariaListaScreen
import com.patiocontrol.ui.carreta.CarretaScreen
import com.patiocontrol.ui.carreta.CarretaViewModel
import com.patiocontrol.ui.carreta.CargaScreen
import com.patiocontrol.ui.carreta.IdentificacaoScreen
import com.patiocontrol.ui.carreta.NovaCarretaScreen
import com.patiocontrol.ui.encerrar.EncerrarScreen
import com.patiocontrol.ui.exportar.ExportarScreen
import com.patiocontrol.ui.passagem.PassagemScreen
import com.patiocontrol.ui.patio.EncerradasScreen
import com.patiocontrol.ui.patio.PatioScreen

/**
 * Rotas do app. São strings simples de propósito: rota tipada exigiria
 * kotlinx.serialization só para três argumentos, e o ganho não paga a
 * dependência extra num app deste tamanho.
 */
object Rotas {
    const val PATIO = "patio"
    const val NOVA = "nova"
    const val ENCERRADAS = "encerradas"
    const val EXPORTAR = "exportar"
    const val PASSAGEM = "passagem"
    const val AJUSTES = "ajustes"

    const val ARG_CARRETA = "carretaId"
    const val ARG_ITEM = "itemId"

    const val CARRETA = "carreta/{$ARG_CARRETA}"
    const val IDENTIFICACAO = "carreta/{$ARG_CARRETA}/identificacao"
    const val CARGA = "carreta/{$ARG_CARRETA}/carga"
    const val AG = "carreta/{$ARG_CARRETA}/ag"
    const val AG_ITEM = "carreta/{$ARG_CARRETA}/ag/{$ARG_ITEM}"
    const val AVARIAS = "carreta/{$ARG_CARRETA}/avarias"
    const val AVARIA_ITEM = "carreta/{$ARG_CARRETA}/avarias/{$ARG_ITEM}"
    const val ENCERRAR = "carreta/{$ARG_CARRETA}/encerrar"

    fun carreta(id: String) = "carreta/$id"
    fun identificacao(id: String) = "carreta/$id/identificacao"
    fun carga(id: String) = "carreta/$id/carga"
    fun ag(id: String) = "carreta/$id/ag"
    fun agItem(id: String, itemId: Long) = "carreta/$id/ag/$itemId"
    fun avarias(id: String) = "carreta/$id/avarias"
    fun avariaItem(id: String, itemId: Long) = "carreta/$id/avarias/$itemId"
    fun encerrar(id: String) = "carreta/$id/encerrar"
}

/** Acesso ao repositório dentro de uma factory de ViewModel. */
val CreationExtras.repositorio: PatioRepository
    get() = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as PatioApplication)
        .container.repositorio

@Composable
fun PatioNavHost(navController: NavHostController = rememberNavController()) {

    NavHost(navController = navController, startDestination = Rotas.PATIO) {

        composable(Rotas.PATIO) {
            PatioScreen(
                aoAbrirNova = { navController.navigate(Rotas.NOVA) },
                aoAbrirCarreta = { id -> navController.navigate(Rotas.carreta(id)) },
                aoIr = { rota -> navController.navigate(rota) }
            )
        }

        composable(Rotas.NOVA) {
            NovaCarretaScreen(
                aoVoltar = { navController.popBackStack() },
                aoAbrir = { id ->
                    navController.popBackStack()
                    navController.navigate(Rotas.carreta(id))
                }
            )
        }

        composable(Rotas.ENCERRADAS) {
            EncerradasScreen(
                aoVoltar = { navController.popBackStack() },
                aoAbrirCarreta = { id -> navController.navigate(Rotas.carreta(id)) }
            )
        }

        composable(Rotas.EXPORTAR) {
            ExportarScreen(aoVoltar = { navController.popBackStack() })
        }

        composable(Rotas.PASSAGEM) {
            PassagemScreen(aoVoltar = { navController.popBackStack() })
        }

        composable(Rotas.AJUSTES) {
            AjustesScreen(aoVoltar = { navController.popBackStack() })
        }

        composable(
            route = Rotas.CARRETA,
            arguments = listOf(navArgument(Rotas.ARG_CARRETA) { type = NavType.StringType })
        ) { entrada ->
            val id = entrada.arguments?.getString(Rotas.ARG_CARRETA).orEmpty()
            CarretaScreen(
                vm = carretaViewModel(id),
                aoVoltar = { navController.popBackStack() },
                aoIdentificacao = { navController.navigate(Rotas.identificacao(id)) },
                aoCarga = { navController.navigate(Rotas.carga(id)) },
                aoAg = { navController.navigate(Rotas.ag(id)) },
                aoAvarias = { navController.navigate(Rotas.avarias(id)) },
                aoEncerrar = { navController.navigate(Rotas.encerrar(id)) }
            )
        }

        composable(
            route = Rotas.IDENTIFICACAO,
            arguments = listOf(navArgument(Rotas.ARG_CARRETA) { type = NavType.StringType })
        ) { entrada ->
            val id = entrada.arguments?.getString(Rotas.ARG_CARRETA).orEmpty()
            IdentificacaoScreen(
                vm = carretaViewModel(id),
                aoVoltar = { navController.popBackStack() }
            )
        }

        composable(
            route = Rotas.CARGA,
            arguments = listOf(navArgument(Rotas.ARG_CARRETA) { type = NavType.StringType })
        ) { entrada ->
            val id = entrada.arguments?.getString(Rotas.ARG_CARRETA).orEmpty()
            CargaScreen(
                vm = carretaViewModel(id),
                aoVoltar = { navController.popBackStack() }
            )
        }

        composable(
            route = Rotas.AG,
            arguments = listOf(navArgument(Rotas.ARG_CARRETA) { type = NavType.StringType })
        ) { entrada ->
            val id = entrada.arguments?.getString(Rotas.ARG_CARRETA).orEmpty()
            AgListaScreen(
                vm = carretaViewModel(id),
                aoVoltar = { navController.popBackStack() },
                aoAbrirItem = { itemId -> navController.navigate(Rotas.agItem(id, itemId)) }
            )
        }

        composable(
            route = Rotas.AG_ITEM,
            arguments = listOf(
                navArgument(Rotas.ARG_CARRETA) { type = NavType.StringType },
                navArgument(Rotas.ARG_ITEM) { type = NavType.LongType }
            )
        ) { entrada ->
            val id = entrada.arguments?.getString(Rotas.ARG_CARRETA).orEmpty()
            val itemId = entrada.arguments?.getLong(Rotas.ARG_ITEM) ?: 0L
            AgItemScreen(
                vm = carretaViewModel(id),
                itemId = itemId,
                aoVoltar = { navController.popBackStack() }
            )
        }

        composable(
            route = Rotas.AVARIAS,
            arguments = listOf(navArgument(Rotas.ARG_CARRETA) { type = NavType.StringType })
        ) { entrada ->
            val id = entrada.arguments?.getString(Rotas.ARG_CARRETA).orEmpty()
            AvariaListaScreen(
                vm = carretaViewModel(id),
                aoVoltar = { navController.popBackStack() },
                aoAbrirItem = { itemId -> navController.navigate(Rotas.avariaItem(id, itemId)) }
            )
        }

        composable(
            route = Rotas.AVARIA_ITEM,
            arguments = listOf(
                navArgument(Rotas.ARG_CARRETA) { type = NavType.StringType },
                navArgument(Rotas.ARG_ITEM) { type = NavType.LongType }
            )
        ) { entrada ->
            val id = entrada.arguments?.getString(Rotas.ARG_CARRETA).orEmpty()
            val itemId = entrada.arguments?.getLong(Rotas.ARG_ITEM) ?: 0L
            AvariaItemScreen(
                vm = carretaViewModel(id),
                itemId = itemId,
                aoVoltar = { navController.popBackStack() }
            )
        }

        composable(
            route = Rotas.ENCERRAR,
            arguments = listOf(navArgument(Rotas.ARG_CARRETA) { type = NavType.StringType })
        ) { entrada ->
            val id = entrada.arguments?.getString(Rotas.ARG_CARRETA).orEmpty()
            EncerrarScreen(
                vm = carretaViewModel(id),
                aoVoltar = { navController.popBackStack() },
                aoEncerrado = {
                    navController.popBackStack(Rotas.PATIO, inclusive = false)
                }
            )
        }
    }
}

/**
 * Uma instância de CarretaViewModel por carreta, compartilhada entre a tela
 * da carreta e as suas filhas através da chave.
 */
@Composable
private fun carretaViewModel(carretaId: String): CarretaViewModel = viewModel(
    key = "carreta-$carretaId",
    factory = viewModelFactory {
        initializer { CarretaViewModel(repositorio, carretaId) }
    }
)
