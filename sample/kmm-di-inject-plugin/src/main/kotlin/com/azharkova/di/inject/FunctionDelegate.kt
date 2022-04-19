package com.azharkova.di.inject

import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.parent
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.util.deepCopyWithSymbols

interface FunctionDelegate {
    val function: IrFunction

    fun buildCall(
        builder: IrBuilderWithScope,
        original: IrCall,
        arguments: List<IrExpression?>,
        message: IrExpression
    ): IrExpression

    fun IrBuilderWithScope.irCallCopy(
        overload: IrSimpleFunctionSymbol,
        original: IrCall,
        arguments: List<IrExpression?>,
        expression: IrExpression
    ): IrExpression {
        return irCall(overload, type = original.type).apply {
            dispatchReceiver = original.dispatchReceiver?.deepCopyWithSymbols(parent)
            extensionReceiver = original.extensionReceiver?.deepCopyWithSymbols(parent)
            for (i in 0 until original.typeArgumentsCount) {
                putTypeArgument(i, original.getTypeArgument(i))
            }
            for ((i, argument) in arguments.withIndex()) {
                putValueArgument(i, argument?.deepCopyWithSymbols(parent))
            }
            putValueArgument(arguments.size, expression)
        }
    }
}

class SimpleFunctionDelegate(
    private val overload: IrSimpleFunctionSymbol
) : FunctionDelegate {
    override val function = overload.owner

    override fun buildCall(
        builder: IrBuilderWithScope,
        original: IrCall,
        arguments: List<IrExpression?>,
        message: IrExpression
    ): IrExpression = builder.irCallCopy(overload, original, arguments, message)
}