package com.azharkova.di
import com.bnorm.debug.log.*

class AppConfig {
  var keeper = HashMap<String,Any>()
  @Provide  fun service2(): TestService? = null
  companion object {
    val DI = AppConfig()
  }

  fun resolve(name: String):Any? {
    return  keeper.get(name)
  }
}

class MoviesService {
  val name = "super movie"
}

class TestService() {}
