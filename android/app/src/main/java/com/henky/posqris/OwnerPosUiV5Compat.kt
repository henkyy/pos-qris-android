package com.henky.posqris

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Compatibility overload for BottomNav's String callback. */
@Composable
internal fun NavItem(
    label: String,
    active: Boolean,
    modifier: Modifier,
    onSelect: (String) -> Unit
) {
    NavItem(label, active, modifier) { onSelect(label) }
}
