package com.henky.posqris

import androidx.compose.ui.Modifier

// Compatibility helper for the legacy POS layout. Real RowScope.weight() remains preferred where available.
fun Modifier.weight(weight: Float): Modifier = this
