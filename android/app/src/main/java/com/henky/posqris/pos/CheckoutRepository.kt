package com.henky.posqris.pos

/**
 * Kontrak checkout. Implementasi produksi akan memanggil Supabase RPC
 * dan mengirim idempotency key agar retry tidak membuat transaksi ganda.
 */
interface CheckoutRepository {
    suspend fun createPendingSale(
        branchId: String,
        locationId: String,
        items: List<CartItem>,
        discount: Long,
        tax: Long,
        idempotencyKey: String
    ): Result<String>

    suspend fun confirmPayment(
        transactionId: String,
        paymentMethod: String,
        amount: Long,
        paymentReference: String?,
        idempotencyKey: String
    ): Result<Unit>
}
