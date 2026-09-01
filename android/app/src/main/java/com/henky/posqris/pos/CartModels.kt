package com.henky.posqris.pos

data class CartItem(
    val productId: String,
    val name: String,
    val quantity: Long,
    val unitPrice: Long,
    val discount: Long = 0
) {
    val lineTotal: Long get() = (quantity * unitPrice - discount).coerceAtLeast(0)
}

data class CartState(
    val items: List<CartItem> = emptyList(),
    val discount: Long = 0,
    val tax: Long = 0
) {
    val subtotal: Long get() = items.sumOf { it.lineTotal }
    val total: Long get() = (subtotal - discount + tax).coerceAtLeast(0)
}

enum class PaymentState { IDLE, PENDING, PAID, FAILED, EXPIRED, EXCEPTION }

data class CheckoutState(
    val cart: CartState = CartState(),
    val paymentState: PaymentState = PaymentState.IDLE,
    val transactionId: String? = null,
    val paymentReference: String? = null,
    val error: String? = null
)
