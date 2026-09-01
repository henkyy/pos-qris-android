package com.henky.posqris.pos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class CheckoutViewModel(private val repository: CheckoutRepository) : ViewModel() {
    private val _state = MutableStateFlow(CheckoutState())
    val state: StateFlow<CheckoutState> = _state.asStateFlow()

    fun setCart(cart: CartState) {
        _state.value = _state.value.copy(cart = cart, error = null)
    }

    fun checkout(branchId: String, locationId: String) {
        val current = _state.value
        if (current.cart.items.isEmpty()) {
            _state.value = current.copy(error = "Keranjang masih kosong")
            return
        }
        viewModelScope.launch {
            _state.value = current.copy(paymentState = PaymentState.PENDING, error = null)
            repository.createPendingSale(
                branchId = branchId,
                locationId = locationId,
                items = current.cart.items,
                discount = current.cart.discount,
                tax = current.cart.tax,
                idempotencyKey = UUID.randomUUID().toString()
            ).onSuccess { id ->
                _state.value = _state.value.copy(transactionId = id)
            }.onFailure { e ->
                _state.value = _state.value.copy(paymentState = PaymentState.FAILED, error = e.message)
            }
        }
    }
}
