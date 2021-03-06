package com.azharkova.kmmdi.shared.service

import com.azharkova.di.inject.Single
import com.azharkova.kmmdi.shared.data.MoviesList
import com.azharkova.kmmdi.shared.network.Configuration
import com.azharkova.kmmdi.shared.network.NetworkClient

@Single
class MoviesService(private val networkClient: NetworkClient?) {

    suspend fun loadMovies(): MoviesList? {
        val url = "discover/movie?api_key=${Configuration.API_KEY}&page=1&sort_by=popularity.desc"
        return networkClient?.loadContentData(url)
    }

    suspend fun searchMovies(query: String): MoviesList? {
        val url = "search/movie?api_key=${Configuration.API_KEY}&page=1&query=$query"
        return networkClient?.loadContentData(url)
    }
}
