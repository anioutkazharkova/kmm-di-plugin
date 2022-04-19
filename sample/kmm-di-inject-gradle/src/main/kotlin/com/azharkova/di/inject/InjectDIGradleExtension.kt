

package com.azharkova.di.inject

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property

open class InjectDIGradleExtension(objects: ObjectFactory) {
  private val enabledProperty: Property<Boolean> = objects.property(Boolean::class.java)
    .apply { convention(true) }

  var enabled: Boolean
    get() = enabledProperty.get()
    set(value) = enabledProperty.set(value)
}
