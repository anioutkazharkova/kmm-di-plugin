package com.azharkova.kmmdi.shared.di

import com.azharkova.di.container.DIContainer
import com.azharkova.di.scope.ScopeType
import com.azharkova.kmmdi.shared.base.IView
import com.azharkova.kmmdi.shared.factory.ModuleConfig
import kotlin.jvm.JvmField
import kotlin.reflect.KClass

object DIManager {

    val appContainer: DIContainer by lazy { DIContainer() }

    fun test() {
        println("Hello")
    }

    fun <T : Any> register(type: String? = "", fabric: () -> T?) {
        appContainer.register(type, ScopeType.Graph, fabric)
    }

    fun <T : Any> resolve(type: String?): Any? {
        return appContainer.resolve(type)
    }

    fun <T : Any> addToScope(scope: ScopeType, type: String? = "", fabric: () -> T?) {
        appContainer.register(type, scope, fabric)
    }

    fun <T : Any> resolveType(type: KClass<T>): Any? {
        return appContainer.resolve(type)
    }

    fun <T : Any> addToScope(scope: ScopeType, type: KClass<T>, fabric: () -> T?) {
        appContainer.register(type, scope, fabric)
    }

    fun <T : Any> addToContainer(type: KClass<T>, fabric: () -> T?) {
        appContainer.register(type, ScopeType.Graph, fabric)
    }

    fun <T : com.azharkova.kmmdi.shared.base.IView> resolveView(view: T): Any? {
        val interactor = com.azharkova.kmmdi.shared.factory.ModuleConfig.instance.config(view)
        interactor?.setup()
        return interactor
    }
}
