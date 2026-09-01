package com.henky.posqris.settings

data class ReceiptSettings(
    val storeName: String,
    val address: String? = null,
    val phone: String? = null,
    val logoPath: String? = null,
    val headerText: String? = null,
    val footerText: String? = null,
    val showCashier: Boolean = true,
    val showCustomer: Boolean = true,
    val showPaymentReference: Boolean = true,
    val showBarcode: Boolean = false,
    val paperWidthMm: Int = 80
)
