package com.azharkova.di.inject
import kotlin.time.TimeSource.Monotonic
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irCatch
import org.jetbrains.kotlin.backend.common.lower.irThrow
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.declarations.buildVariable
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irConcat
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetObject
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.builders.irTemporary
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrValueDeclaration
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrReturn
import org.jetbrains.kotlin.ir.expressions.impl.IrTryImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.statements
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

class DebugLogTransformer(
  private val pluginContext: IrPluginContext,
  private val debugLogAnnotation: IrClassSymbol,
  private val logFunction: IrSimpleFunctionSymbol,
) : IrElementTransformerVoidWithContext() {
  private val typeUnit = pluginContext.irBuiltIns.unitType
  private val typeThrowable = pluginContext.irBuiltIns.throwableType

  private val classMonotonic =
    pluginContext.referenceClass(FqName("kotlin.time.TimeSource.Monotonic"))!!

  private val funMarkNow =
    pluginContext.referenceFunctions(FqName("kotlin.time.TimeSource.markNow"))
      .single()

  private val funElapsedNow =
    pluginContext.referenceFunctions(FqName("kotlin.time.TimeMark.elapsedNow"))
      .single()

  override fun visitFunctionNew(declaration: IrFunction): IrStatement {
    val body = declaration.body
    if (body != null && declaration.hasAnnotation(debugLogAnnotation)) {
      declaration.body = irDebug(declaration, body)
    }
    return super.visitFunctionNew(declaration)
  }

  private fun irDebug(
    function: IrFunction,
    body: IrBody
  ): IrBlockBody {
    return DeclarationIrBuilder(pluginContext, function.symbol).irBlockBody {
      +irDebugEnter(function)

      val startTime = irTemporary(irCall(funMarkNow).also { call ->
        call.dispatchReceiver = irGetObject(classMonotonic)
      })

      val tryBlock = irBlock(resultType = function.returnType) {
        for (statement in body.statements) +statement
        if (function.returnType == typeUnit) +irDebugExit(function, startTime)
      }.transform(DebugLogReturnTransformer(function, startTime), null)

      val throwable = buildVariable(
        scope.getLocalDeclarationParent(), startOffset, endOffset, IrDeclarationOrigin.CATCH_PARAMETER,
        Name.identifier("t"), typeThrowable
      )

      +IrTryImpl(startOffset, endOffset, tryBlock.type).also { irTry ->
        irTry.tryResult = tryBlock
        irTry.catches += irCatch(throwable, irBlock {
          +irDebugExit(function, startTime, irGet(throwable))
          +irThrow(irGet(throwable))
        })
      }
    }
  }

  inner class DebugLogReturnTransformer(
    private val function: IrFunction,
    private val startTime: IrVariable
  ) : IrElementTransformerVoidWithContext() {
    override fun visitReturn(expression: IrReturn): IrExpression {
      if (expression.returnTargetSymbol != function.symbol) return super.visitReturn(expression)

      return DeclarationIrBuilder(pluginContext, function.symbol).irBlock {
        if (expression.value.type == typeUnit) {
          +irDebugExit(function, startTime)
          +expression
        } else {
          val result = irTemporary(expression.value)
          +irDebugExit(function, startTime, irGet(result))
          +expression.apply {
            value = irGet(result)
          }
        }
      }
    }
  }

  private fun IrBuilderWithScope.irDebugEnter(
    function: IrFunction
  ): IrCall {
    val concat = irConcat()
    concat.addArgument(irString("⇢ ${function.name}("))
    for ((index, valueParameter) in function.valueParameters.withIndex()) {
      if (index > 0) concat.addArgument(irString(", "))
      concat.addArgument(irString("${valueParameter.name}="))
      concat.addArgument(irGet(valueParameter))
    }
    concat.addArgument(irString(")"))

    return irCall(logFunction).also { call ->
      call.putValueArgument(0, concat)
    }
  }

  private fun IrBuilderWithScope.irDebugExit(
    function: IrFunction,
    startTime: IrValueDeclaration,
    result: IrExpression? = null
  ): IrCall {
    val concat = irConcat()
    concat.addArgument(irString("⇠ ${function.name} ["))
    concat.addArgument(irCall(funElapsedNow).also { call ->
      call.dispatchReceiver = irGet(startTime)
    })
    if (result != null) {
      concat.addArgument(irString("] = "))
      concat.addArgument(result)
    } else {
      concat.addArgument(irString("]"))
    }

    return irCall(logFunction).also { call ->
      call.putValueArgument(0, concat)
    }
  }
}
