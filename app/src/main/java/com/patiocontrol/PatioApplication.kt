package com.patiocontrol

import android.app.Application
import com.patiocontrol.di.AppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class PatioApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)

        // O código do aparelho precisa existir antes da primeira carreta,
        // porque ele é o prefixo do identificador dela.
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            runCatching { container.repositorio.garantirAparelho() }
        }
    }
}
