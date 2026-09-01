package com.henky.posqris.qris

import com.henky.posqris.pos.PaymentState

data class QrisPaymentState(
    val transactionId: String,
    val amount: Long,
    val state: PaymentState = PaymentState.PENDING,
    val reference: String? = null,
    val message: String? = null
)

fun QrisPaymentState.isFinal(): Boolean = state in setOf(
    PaymentState.PAID,
    PaymentState.FAILED,
    PaymentState.EXPIRED,
    PaymentState.EXCEPTION
)
