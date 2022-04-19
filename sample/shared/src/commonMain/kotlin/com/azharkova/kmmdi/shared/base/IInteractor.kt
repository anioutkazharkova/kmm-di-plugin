package com.azharkova.kmmdi.shared.base

import com.azharkova.kmmdi.shared.di.DIManager

interface IInteractor {
    fun setup()

    fun attachView()

    fun detachView()
}
