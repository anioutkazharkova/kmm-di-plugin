

package com.azharkova.di.inject


import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption

class InjectDIGradlePlugin : KotlinCompilerPluginSupportPlugin {

  override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean = true

  override fun getCompilerPluginId(): String = "com.azharkova.di.inject.kmm-di-inject-plugin"

  override fun getPluginArtifact(): SubpluginArtifact = SubpluginArtifact(
    groupId = "com.azharkova.di.inject",
    artifactId = "kmm-di-inject-plugin",
    version = "0.1.1"
  )

  override fun getPluginArtifactForNative(): SubpluginArtifact = SubpluginArtifact(
    groupId = "com.azharkova.di.inject",
    artifactId = "kmm-di-inject-plugin" + "-native",
    version = "0.1.1"
  )


  override fun apply(target: Project) {
    super.apply(target)
      target.extensions.create("inject-di", InjectDIGradleExtension::class.java)
      target.afterEvaluate { project ->
        project.configurations.filter { it.name.endsWith("implementation", ignoreCase = true) }.forEach {
          project.dependencies.add(it.name, project.dependencies.create("com.azharkova.di.inject:kmm-di-inject-annotations:$version"))
        }
      }
  }

  override fun applyToCompilation(kotlinCompilation: KotlinCompilation<*>): Provider<List<SubpluginOption>> =
    kotlinCompilation.target.project.provider { emptyList() }


  companion object {
    private const val version = "0.1.1"
  }
}