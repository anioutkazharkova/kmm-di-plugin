package com.azharkova.di.inject

import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol

class LambdaFunctionDelegate(
    private val overload: IrSimpleFunctionSymbol,
    private val messageParameter: IrValueParameter
) : FunctionDelegate {
    override val function = overload.owner

    override fun buildCall(
        builder: IrBuilderWithScope,
        original: IrCall,
        arguments: List<IrExpression?>,
        message: IrExpression
    ): IrExpression = with(builder) {
        val expression = irLambda(context.irBuiltIns.stringType, messageParameter.type) {
            +irReturn(message)
        }
        irCallCopy(overload, original, arguments, expression)
    }
}
