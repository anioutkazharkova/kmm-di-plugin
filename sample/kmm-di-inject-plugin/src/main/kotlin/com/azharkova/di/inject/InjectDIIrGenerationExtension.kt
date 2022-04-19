

package com.azharkova.di.inject

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.name.FqName

class InjectDIIrGenerationExtension : IrGenerationExtension {
  override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
    val typeAnyNullable = pluginContext.irBuiltIns.anyNType

    val injectServiceAnnotation = pluginContext.referenceClass(FqName("com.azharkova.di.inject.InjectService"))!!

    moduleFragment.transform(InjectTransformer(pluginContext, injectServiceAnnotation), null)
  }
}
