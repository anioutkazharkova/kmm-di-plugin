

package com.azharkova.di.inject

import com.google.auto.service.AutoService
import org.jetbrains.kotlin.compiler.plugin.AbstractCliOption
import org.jetbrains.kotlin.compiler.plugin.CliOption
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey

@AutoService(CommandLineProcessor::class)
class InjectDICommandLineProcessor : CommandLineProcessor {
  companion object {
    private const val OPTION_ENABLED = "enabled"

    val ARG_ENABLED = CompilerConfigurationKey<Boolean>(OPTION_ENABLED)
  }

  override val pluginId: String = "com.azharkova.di.inject.kmm-di-inject"

  override val pluginOptions: Collection<CliOption> = listOf(
    CliOption(
      optionName = OPTION_ENABLED,
      valueDescription = "bool <true | false>",
      description = "If the DebugLog annotation should be applied",
      required = false,
    ),
  )

  override fun processOption(
    option: AbstractCliOption,
    value: String,
    configuration: CompilerConfiguration
  ) {
    return when (option.optionName) {
      OPTION_ENABLED -> configuration.put(ARG_ENABLED, value.toBoolean())
      else -> throw IllegalArgumentException("Unexpected config option ${option.optionName}")
    }
  }
}
