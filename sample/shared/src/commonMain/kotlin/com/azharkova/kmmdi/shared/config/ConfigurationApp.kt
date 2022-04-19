package com.azharkova.kmmdi.shared.config

import com.azharkova.di.inject.Container
import com.azharkova.di.scope.ScopeType
import com.azharkova.kmmdi.shared.di.DIManager
import com.azharkova.kmmdi.shared.network.NetworkClient
import com.azharkova.kmmdi.shared.service.MoviesService
import kotlin.reflect.KClass

@Container
object  ConfigurationApp {
    val appContainer: DIManager =
        DIManager

    init {
        setup2()
    }

    fun setup2() {}

  /*  fun setup() {
        appContainer.addToScope(
            ScopeType.Container,
            NetworkClient::class
        ) {
            NetworkClient()
        }
        appContainer.addToScope(
            ScopeType.Container,
            com.azharkova.kmmdi.shared.service.MoviesService::class
        ) {
            val nc = appContainer.resolveType(NetworkClient::class) as? NetworkClient
            com.azharkova.kmmdi.shared.service.MoviesService(nc)
        }
    }*/


    fun <T : Any> addToScope(type: KClass<T>, fabric: () -> T?) {
        appContainer.addToScope(ScopeType.Container, type,fabric)
    }


    fun <T : Any> add(type: String, fabric: () -> T?) {
        appContainer.addToScope(ScopeType.Entity, type,fabric)
    }

    fun<T:Any> resolve(type: String):T? {
        return  appContainer.resolve<T>(type) as? T
    }

}
