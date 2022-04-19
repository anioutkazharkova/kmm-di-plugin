package com.azharkova.kmmdi.shared.movieslist

import com.azharkova.di.inject.InjectService
import com.azharkova.kmmdi.shared.base.BaseInteractor
import com.azharkova.kmmdi.shared.config.ConfigurationApp
import com.azharkova.kmmdi.shared.di.DIManager
import com.azharkova.kmmdi.shared.network.NetworkClient
import com.azharkova.kmmdi.shared.service.MoviesService
import com.azharkova.kmmdi.shared.util.uiDispatcher
import kotlinx.coroutines.launch

interface IMoviesListInteractor : com.azharkova.kmmdi.shared.base.IInteractor {
    var presenter: IMoviesListPresenter?
    fun loadMovies()
}

class MoviesListInteractor :
    BaseInteractor<com.azharkova.kmmdi.shared.movieslist.IMoviesListView>(uiDispatcher),
    IMoviesListInteractor {

   @InjectService private fun moviesService():MoviesService? = null

    override var presenter: IMoviesListPresenter? = null

    private var moviesList: ArrayList<com.azharkova.kmmdi.shared.data.MoviesItem> = arrayListOf()


    override fun setup() {

    }

    override fun loadMovies() {
        scope.launch {
            val result = moviesService()?.loadMovies()?.results
            moviesList = arrayListOf()
            moviesList.addAll(result ?: arrayListOf())
            presenter?.setup(moviesList)
        }
    }
}
