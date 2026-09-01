package com.henky.posqris

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.henky.posqris.navigation.PosDestination
import com.henky.posqris.ui.PosResponsiveScaffold

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    PosAppShell()
                }
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
    val isTablet = LocalConfiguration.current.screenWidthDp >= 600

    if (isTablet) {
        destinations.forEach { destination ->
            NavigationRailItem(
                selected = destination.route == selectedRoute,
                onClick = { onSelect(destination.route) },
                icon = {
                    Text(
                        text = destination.title.take(1),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                label = { Text(destination.title, maxLines = 1) },
                alwaysShowLabel = false
            )
        }
    } else {
        NavigationBar {
            destinations.take(4).forEach { destination ->
                val selected = destination.route == selectedRoute
                TextButton(
                    onClick = { onSelect(destination.route) },
                    modifier = Modifier.weight(1f)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = destination.title.take(1),
                            fontWeight = FontWeight.Bold,
                            color = if (selected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                        Text(
                            text = destination.title,
                            maxLines = 1,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardScreen() {
    val cards = listOf(
        "Penjualan Hari Ini" to "Rp 1.250.000",
        "Transaksi" to "24",
        "Pembayaran QRIS" to "Rp 875.000",
        "Stok Menipis" to "3"
    )

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Dashboard", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("Ringkasan operasional toko hari ini", style = MaterialTheme.typography.bodyMedium)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                )
                SpacerWidth(8.dp)
                Text("Toko Demo • Online", style = MaterialTheme.typography.labelLarge)
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Penjualan hari ini", style = MaterialTheme.typography.labelLarge)
                    Text("Rp 1.250.000", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("+12,5% dibanding kemarin", style = MaterialTheme.typography.bodySmall)
                }
                TextButton(onClick = {}) { Text("Lihat laporan") }
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 190.dp),
            modifier = Modifier.fillMaxWidth().weight(1f),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(cards) { (title, value) ->
                SummaryCard(title, value)
            }
        }
    }
}

@Composable
private fun SummaryCard(title: String, value: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("Data demo", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun FeaturePlaceholderScreen(title: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(
            "Modul sedang disiapkan dengan data demo. Struktur layar dibuat responsif untuk ponsel dan tablet.",
            style = MaterialTheme.typography.bodyLarge
        )
        Card(shape = RoundedCornerShape(16.dp)) {
            Text(
                "Konten modul akan terhubung ke Supabase pada tahap integrasi data.",
                modifier = Modifier.padding(18.dp)
            )
        }
    }
}

@Composable
private fun SpacerWidth(width: Int) {
    Box(modifier = Modifier.width(width.dp).height(1.dp))
}
