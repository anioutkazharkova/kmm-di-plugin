import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  id("java-gradle-plugin")
  kotlin("jvm")
  id("maven-publish")
}

group = "com.azharkova.di.inject"
version = "0.1.1"

dependencies {
  implementation(kotlin("stdlib"))
  implementation(kotlin("gradle-plugin-api"))
}


tasks.withType<KotlinCompile> {
  kotlinOptions.jvmTarget = "1.8"
}

gradlePlugin {
  plugins {
    create("kmm-di-inject") {
      id = "kmm-di-inject"
      displayName = "Kotlin Debug Log compiler plugin"
      description = "Kotlin compiler plugin to add debug logging to functions"
      implementationClass = "com.azharkova.di.inject.InjectDIGradlePlugin"
    }
  }
}

publishing {
  publications {
    create<MavenPublication>("default") {
      from(components["java"])
      artifact(tasks.kotlinSourcesJar)
    }
  }
}
