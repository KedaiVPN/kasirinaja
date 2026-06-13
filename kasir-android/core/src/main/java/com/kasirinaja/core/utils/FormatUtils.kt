package com.kasirinaja.core.utils

import java.text.NumberFormat
import java.util.Locale

object FormatUtils {
    fun formatCurrency(amount: Long): String {
        val format = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
        format.maximumFractionDigits = 0
        val formatted = format.format(amount)
        return formatted.replace("Rp", "Rp ").replace(",00", "")
    }

    fun formatCurrency(amountStr: String): String {
        val amount = amountStr.toLongOrNull() ?: 0L
        return formatCurrency(amount)
    }
}
