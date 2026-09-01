package com.henky.posqris.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun PosResponsiveScaffold(
    navigation: @Composable () -> Unit,
    content: @Composable () -> Unit
) {
    Scaffold { padding ->
        Row(
            modifier = Modifier.fillMaxSize().padding(padding),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            NavigationRail { navigation() }
            content()
        }
    }
}
