package com.henky.posqris

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.henky.posqris.navigation.PosDestination
import com.henky.posqris.ui.PosResponsiveScaffold

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                PosAppShell()
            }
        }
    }
}

@Composable
private fun PosAppShell() {
    val destinations = listOf(
        PosDestination.Dashboard,
        PosDestination.Pos,
        PosDestination.Products,
        PosDestination.Inventory,
        PosDestination.Customers,
        PosDestination.Payments,
        PosDestination.Reports,
        PosDestination.Settings
    )
    var selectedRoute by rememberSaveable { mutableStateOf(PosDestination.Dashboard.route) }

    PosResponsiveScaffold(
        navigation = {
            PosNavigationItems(
                destinations = destinations,
                selectedRoute = selectedRoute,
                onSelect = { selectedRoute = it }
            )
        },
        content = {
            val selected = destinations.firstOrNull { it.route == selectedRoute }
                ?: PosDestination.Dashboard

            when (selected) {
                PosDestination.Dashboard -> DashboardScreen()
                else -> FeaturePlaceholderScreen(selected.title)
            }
        }
    )
}

@Composable
private fun PosNavigationItems(
    destinations: List<PosDestination>,
    selectedRoute: String,
    onSelect: (String) -> Unit
) {
    val isCompact = LocalConfiguration.current.screenWidthDp < 600

    if (isCompact) {
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            destinations.take(4).forEach { destination ->
                TextButton(onClick = { onSelect(destination.route) }) {
                    Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                        Text(
                            text = destination.title.take(1),
                            style = MaterialTheme.typography.labelLarge
                        )
                        Text(
                            text = destination.title,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }
    } else {
        destinations.forEach { destination ->
            NavigationRailItem(
                selected = destination.route == selectedRoute,
                onClick = { onSelect(destination.route) },
                icon = { Text(destination.title.take(1)) },
                label = { Text(destination.title) }
            )
        }
    }
}

@Composable
private fun DashboardScreen() {
    val cards = listOf(
        "Penjualan Hari Ini" to "Rp 0",
        "Transaksi" to "0",
        "Pembayaran QRIS" to "Rp 0",
        "Stok Menipis" to "0"
    )

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Dashboard", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Ringkasan operasional toko",
            style = MaterialTheme.typography.bodyMedium
        )

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 180.dp),
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(cards) { (title, value) ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(title, style = MaterialTheme.typography.labelLarge)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(value, style = MaterialTheme.typography.headlineSmall)
                    }
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Status sistem", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Aplikasi POS siap dihubungkan ke autentikasi dan Supabase.")
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = {}) {
                    Text("Buka Penjualan")
                }
            }
        }
    }
}

@Composable
private fun FeaturePlaceholderScreen(title: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(title, style = MaterialTheme.typography.headlineMedium)
        Text(
            "Modul ini sudah masuk ke shell aplikasi dan akan dihubungkan ke repository serta Supabase pada tahap berikutnya.",
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
