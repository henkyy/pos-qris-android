package com.henky.posqris.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationRail
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp

@Composable
fun PosResponsiveScaffold(
    navigation: @Composable () -> Unit,
    content: @Composable () -> Unit
) {
    val isTablet = LocalConfiguration.current.screenWidthDp >= 600

    if (isTablet) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            NavigationRail(modifier = Modifier.navigationBarsPadding()) {
                navigation()
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                content()
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                content()
            }
            NavigationBar(modifier = Modifier.navigationBarsPadding()) {
                navigation()
            }
        }
    }
}
