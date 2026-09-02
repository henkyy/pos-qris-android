package com.henky.posqris

import androidx.compose.foundation.layout.RowScope as ComposeRowScope
import androidx.compose.foundation.layout.ColumnScope as ComposeColumnScope
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp

typealias RowScope = ComposeRowScope
typealias ColumnScope = ComposeColumnScope

fun Modifier.heightIn(max: Dp): Modifier = height(max)
