package com.henky.posqris.receipt

import com.henky.posqris.settings.ReceiptSettings

data class ReceiptLine(val name: String, val qty: String, val amount: Long)
data class ReceiptData(
    val transactionNo: String,
    val dateTime: String,
    val cashier: String?,
    val customer: String?,
    val lines: List<ReceiptLine>,
    val subtotal: Long,
    val discount: Long,
    val tax: Long,
    val total: Long,
    val paymentMethod: String,
    val paid: Long,
    val change: Long,
    val paymentReference: String? = null
)

class ReceiptFormatter {
    fun format(data: ReceiptData, settings: ReceiptSettings): ByteArray {
        // ESC/POS encoding is intentionally isolated here so the POS flow stays printer-agnostic.
        val text = buildString {
            appendLine(settings.storeName)
            settings.address?.let { appendLine(it) }
            settings.phone?.let { appendLine(it) }
            settings.headerText?.let { appendLine(it) }
            appendLine("Transaksi: ${data.transactionNo}")
            appendLine(data.dateTime)
            if (settings.showCashier) data.cashier?.let { appendLine("Kasir: $it") }
            if (settings.showCustomer) data.customer?.let { appendLine("Pelanggan: $it") }
            appendLine("------------------------------")
            data.lines.forEach { appendLine("${it.name}  ${it.qty}  ${it.amount}") }
            appendLine("------------------------------")
            appendLine("Subtotal: ${data.subtotal}")
            appendLine("Diskon: ${data.discount}")
            appendLine("Pajak: ${data.tax}")
            appendLine("TOTAL: ${data.total}")
            appendLine("Bayar: ${data.paymentMethod} ${data.paid}")
            appendLine("Kembali: ${data.change}")
            if (settings.showPaymentReference) data.paymentReference?.let { appendLine("Ref: $it") }
            settings.footerText?.let { appendLine(it) }
        }
        return text.toByteArray(Charsets.UTF_8)
    }
}
