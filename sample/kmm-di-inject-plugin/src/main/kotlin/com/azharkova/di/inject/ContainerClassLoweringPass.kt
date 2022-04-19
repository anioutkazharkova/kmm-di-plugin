package com.azharkova.di.inject

import org.jetbrains.kotlin.backend.common.ClassLoweringPass
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.allParameters
import org.jetbrains.kotlin.backend.common.ir.copyTo
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.backend.js.utils.asString
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.descriptors.IrBasedClassDescriptor
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrFunctionExpressionImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

class ContainerClassLoweringPass(val pluginContext: IrPluginContext) : ClassLoweringPass {
    private val containerClass = pluginContext.referenceClass(container)!!
    private val resolve =
        pluginContext.referenceFunctions(FqName("com.azharkova.kmmdi.shared.config.ConfigurationApp.resolve"))
            ?.firstOrNull()?.owner
    private val addToScopeFunc =
        pluginContext.referenceFunctions(FqName("com.azharkova.kmmdi.shared.config.ConfigurationApp.add"))
            .firstOrNull()?.owner
    private val setupFunc =
        pluginContext.referenceFunctions(FqName("com.azharkova.kmmdi.shared.config.ConfigurationApp.setup2"))
            .firstOrNull()?.owner
    private val conf =
        pluginContext.referenceClass(FqName("com.azharkova.kmmdi.shared.config.ConfigurationApp"))!!
    private val setupFunc3 =
        pluginContext.referenceFunctions(FqName("com.azharkova.kmmdi.shared.config.ConfigurationApp.setup3"))
            .firstOrNull()?.owner

    private val scopeType =
        pluginContext.referenceClass(FqName("com.azharkova.di.scope.ScopeType.Container"))!!

    val funPrintln = pluginContext.referenceFunctions(FqName("kotlin.io.println"))
        .single {
            val parameters = it.owner.valueParameters
            parameters.size == 1 && parameters[0].type == pluginContext.irBuiltIns.anyNType
        }

    companion object {
        var index: Int = 0
    }

    override fun lower(irClass: IrClass) {
        if (!IrBasedClassDescriptor(irClass).isSingle()) {
            return
        }

        addDataToSetup(irClass)
    }

    fun addDataToSetup(irClass: IrClass) {
        val body = setupFunc?.body
        setupFunc?.body = createAddBody(
            irClass,
            setupFunc!!,
            body = body
        )
    }

    private fun createAddBody(clazz: IrClass, function: IrFunction, body: IrBody?): IrBlockBody {
        return DeclarationIrBuilder(pluginContext, function.symbol).irBlockBody {
            body?.let {
                for (statement in body.statements) {
                    +statement
                }
            }
            +irCall(funPrintln).also {
                it.putValueArgument(0,irString(setupFunc!!.dump()))
                it.dispatchReceiver = funPrintln.
            }
            val classDescriptor = pluginContext.referenceClass(
                clazz.fqNameWhenAvailable!!
            )
            val classDescriptorType =
                classDescriptor!!.createType(hasQuestionMark = false, arguments = emptyList())

            val lambda = buildLambda(classDescriptorType, funApply = {
                +irReturn(
                    irCallConstructor(
                        clazz.primaryConstructor!!.symbol,
                        emptyList()
                    ).also {
                        clazz.primaryConstructor?.valueParameters?.forEachIndexed { index, irValueParameter ->
                            val result = irTemporary(
                                irCall(resolve!!.symbol, irValueParameter.type).also { call ->
                                    call.dispatchReceiver =
                                        irGetObjectValue(conf.createType(false, emptyList()), conf)
                                    call.putTypeArgument(0, pluginContext.irBuiltIns.anyType)
                                    call.putValueArgument(
                                        0,
                                        irString(irValueParameter.type.classFqName!!.asString())
                                    )
                                })
                            it.putValueArgument(index, irGet(result))
                        }
                    }
                )
            })

            +irCall(addToScopeFunc!!.symbol).also { call ->
                call.putValueArgument(0, irString(clazz.kotlinFqName.asString()))
                call.putValueArgument(1, lambdaArgument(lambda))
                call.dispatchReceiver = irGetObjectValue(conf.createType(false, emptyList()), conf)
            }
        }
    }

    inline fun buildLambda(
        returnType: IrType,
        funBuilder: IrFunctionBuilder.() -> Unit = {},
        funApply: IrBlockBodyBuilder.() -> Unit
    ): IrSimpleFunction = pluginContext.irFactory.buildFun {
        name = Name.special("<anonymous>")
        this.returnType = returnType
        this.origin = IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA
        this.visibility = DescriptorVisibilities.LOCAL
    }.apply {
        body = DeclarationIrBuilder(pluginContext, symbol).irBlockBody(body = funApply)
    }

    fun lambdaArgument(
        lambda: IrSimpleFunction,
        type: IrType = run {
            val base = if (lambda.isSuspend)
                pluginContext.irBuiltIns.suspendFunctionN(lambda.allParameters.size)
            else
                pluginContext.irBuiltIns.functionN(lambda.allParameters.size)

            base.typeWith(lambda.allParameters.map { it.type } + lambda.returnType)
        }
    ) = IrFunctionExpressionImpl(
        UNDEFINED_OFFSET,
        UNDEFINED_OFFSET,
        type,
        lambda,
        IrStatementOrigin.LAMBDA
    )

    inner class ReturnTransformer(
        private val function: IrFunction,
        private val clazz: IrClass
    ) : IrElementTransformerVoidWithContext() {
        override fun visitReturn(expression: IrReturn): IrExpression {
            if (expression.returnTargetSymbol != function.symbol) return super.visitReturn(
                expression
            )

            return DeclarationIrBuilder(pluginContext, function.symbol).irBlock {
                +expression
                +irCall(funPrintln).also {
                    var concat = irConcat()
                    index += 1
                    concat.addArgument(irString("output class kmm: ${index} ${clazz.kotlinFqName.asString()}"))
                    it.putValueArgument(0, concat)
                }
            }
        }
    }
}