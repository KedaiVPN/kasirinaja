package com.poskedai.core.utils

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

object FormatUtils {
    private val indonesianSymbols = DecimalFormatSymbols(Locale("id", "ID")).apply {
        groupingSeparator = '.'
        decimalSeparator = ','
    }

    // Reuse thread-local formatter to eliminate expensive DecimalFormat allocations on every frame/recomposition
    private val currencyFormatter = ThreadLocal.withInitial {
        DecimalFormat("#,##0", indonesianSymbols)
    }

    fun formatCurrency(amount: Long): String {
        val formatted = currencyFormatter.get()?.format(amount) ?: amount.toString()
        return "Rp $formatted"
    }

    fun formatCurrency(amountStr: String): String {
        val amount = amountStr.toLongOrNull() ?: 0L
        return formatCurrency(amount)
    }
}
