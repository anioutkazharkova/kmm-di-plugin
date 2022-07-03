package com.azharkova.di.inject


import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.addField
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrFunctionExpressionImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

class InjectTransformer(
    private val pluginContext: IrPluginContext,
    private val injectServiceAnnotation: IrClassSymbol
) : IrElementTransformerVoidWithContext() {
    private val lazyFunction by lazy {
        pluginContext.referenceFunctions(FqName("kotlin.lazy")).firstOrNull()?.owner
    }

    private val typeAny = pluginContext.irBuiltIns.anyType
    private val resolveFunc =
        pluginContext.referenceFunctions(FqName("com.azharkova.kmmdi.shared.config.ConfigurationApp.resolve"))
            .firstOrNull()?.owner

    private val resolve =
        pluginContext.referenceClass(FqName("com.azharkova.kmmdi.shared.config.ConfigurationApp"))!!

    //private val appContainer =
      //  pluginContext.referenceProperties(FqName("com.azharkova.kmmdi.shared.config.ConfigurationApp.appContainer")).firstOrNull()?.owner

    val funPrintln = pluginContext.referenceFunctions(FqName("kotlin.io.println"))
        .single {
            val parameters = it.owner.valueParameters
            parameters.size == 1 && parameters[0].type == pluginContext.irBuiltIns.anyNType
        }

    override fun visitFunctionNew(declaration: IrFunction): IrStatement {
        if (declaration.hasAnnotation(injectServiceAnnotation)) {
            declaration.body = makeResolveBody(declaration)
        }
        return super.visitFunctionNew(declaration)
    }


    private fun irDebug(
        function: IrFunction,
        body: IrBody?
    ): IrBlockBody {

        val field = makeLazyField(function, function.parentClassOrNull!!)
        val getValueFunction =
            field.type.getClass()!!.properties.first { it.name.identifier == "value" }.getter!!
        return DeclarationIrBuilder(pluginContext, function.symbol).irBlockBody {
            +irReturn(
                irCall(getValueFunction.symbol, function.returnType).also {
                    it.dispatchReceiver = irGetField(
                        IrGetValueImpl(
                            startOffset,
                            endOffset,
                            (function.dispatchReceiverParameter
                                ?: function.parentClassOrNull!!.thisReceiver)!!.symbol
                        ), field
                    )
                }
            )
        }
    }

    private fun makeResolveBody(
        function: IrFunction
    ): IrBlockBody {

        return DeclarationIrBuilder(pluginContext, function.symbol).irBlockBody {
            val block = irReturn(
                irCall(resolveFunc!!.symbol, function.returnType).also { call ->
                    call.putTypeArgument(0, typeAny)
                    call.putValueArgument(
                        0,
                        irString(function.returnType.classFqName!!.asString())
                    )

                    call.dispatchReceiver =
                        irGetObjectValue(resolve.createType(false, emptyList()), resolve)
                })
            +block
            +irCall(funPrintln).also {
                var concat = irConcat()
                concat.addArgument(irString("Block:\n\n" + block?.dump().orEmpty() + "\n\n"))
                it.putValueArgument(0, concat)
            }
        }
    }


    private fun makeLazyField(
        function: IrFunction,
        module: IrClass
    ): IrField {
        val lazyFunction = lazyFunction
        check(lazyFunction != null) { "kotlin.Lazy not found" }
        val lazyType = lazyFunction.returnType.getClass()!!.typeWith(function.returnType)
        val field = module.addField {
            type = lazyType
            name = Name.identifier("__di_cache__${function.name.asString()}")
            visibility = DescriptorVisibilities.PRIVATE
            startOffset = function.startOffset
            endOffset = function.endOffset
        }
        field.initializer = with(DeclarationIrBuilder(pluginContext, field.symbol)) {
            val factoryFunction = field.factory.buildFun {
                name = Name.special("<internal_injection_initializer>")
                returnType = function.returnType
                visibility = DescriptorVisibilities.LOCAL
                origin = IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA
            }.apply {
                parent = field
                body = DeclarationIrBuilder(pluginContext, symbol).irBlockBody {
                    +irReturn(
                        irCallConstructor(
                            function.returnType.getClass()!!.primaryConstructor!!.symbol,
                            emptyList()
                        )
                    )
                }
            }
            val functionExpression = IrFunctionExpressionImpl(
                startOffset,
                endOffset,
                pluginContext.irBuiltIns.functionN(0).typeWith(function.returnType),
                factoryFunction,
                IrStatementOrigin.LAMBDA
            )
            irExprBody(
                irCall(lazyFunction.symbol, lazyType).also {
                    it.putTypeArgument(0, function.returnType)
                    it.putValueArgument(0, functionExpression)
                }
            )
        }
        return field
    }
}
