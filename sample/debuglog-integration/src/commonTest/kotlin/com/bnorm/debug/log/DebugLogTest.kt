

package com.bnorm.debug.log

import com.azharkova.di.AppConfig
import com.azharkova.di.MoviesService
import kotlin.test.Test

class DebugLogTest {
  @Test
  fun testDebugLog() {
    greet(name = "Integration")
    val s = provideService()
   print("myservice: ${s?.toString()} ${s?.name}")
    print("myservice: ${provideService()?.toString()} ${provideService()?.name}")
    print("testservice: ${AppConfig.DI.service2()}")
    //hello(name = "Provide")
  }

  @DebugLog
  private fun greet(greeting: String = "Hello", name: String = "World"): String {
    return "$greeting, $name!"
  }

  @Provide
  fun provideService(): MoviesService? = null
}
