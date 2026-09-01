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
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
    var selectedRoute by remember { mutableStateOf(PosDestination.Dashboard.route) }
    val selected = destinations.firstOrNull { it.route == selectedRoute } ?: PosDestination.Dashboard

    PosResponsiveScaffold(
        navigation = {
            val isCompact = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp < 600
            if (isCompact) {
                destinations.take(4).forEach { destination ->
                    NavigationBarItem(
                        selected = destination.route == selectedRoute,
                        onClick = { selectedRoute = destination.route },
                        icon = { Text(destination.title.take(1)) },
                        label = { Text(destination.title) }
                    )
                }
            } else {
                destinations.forEach { destination ->
                    NavigationRailItem(
                        selected = destination.route == selectedRoute,
                        onClick = { selectedRoute = destination.route },
                        icon = { Text(destination.title.take(1)) },
                        label = { Text(destination.title) }
                    )
                }
            }
        },
        content = {
            when (selected.route) {
                PosDestination.Dashboard.route -> DashboardScreen()
                else -> FeaturePlaceholderScreen(selected.title)
            }
        }
    )
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
                Text("Aplikasi POS siap dikembangkan ke autentikasi dan Supabase.")
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
