package com.henky.posqris

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp

fun BorderStroke(width: Dp, color: Color): androidx.compose.foundation.BorderStroke =
    androidx.compose.foundation.BorderStroke(width, Brush.solidColor(color))
