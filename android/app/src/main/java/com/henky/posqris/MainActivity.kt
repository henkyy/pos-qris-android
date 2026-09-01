package com.henky.posqris

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.henky.posqris.navigation.PosDestination
import com.henky.posqris.ui.PosResponsiveScaffold

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) { PosAppShell() }
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
            PosNavigationItems(destinations, selectedRoute) { selectedRoute = it }
        },
        content = {
            val selected = destinations.firstOrNull { it.route == selectedRoute }
                ?: PosDestination.Dashboard
            if (selected == PosDestination.Dashboard) DashboardScreen()
            else FeaturePlaceholderScreen(selected.title)
        }
    )
}

private fun navigationSymbol(destination: PosDestination): String = when (destination) {
    PosDestination.Dashboard -> "⌂"
    PosDestination.Pos -> "▣"
    PosDestination.Products -> "□"
    PosDestination.Inventory -> "▥"
    PosDestination.Customers -> "♙"
    PosDestination.Payments -> "Rp"
    PosDestination.Reports -> "▤"
    PosDestination.Settings -> "⚙"
    else -> "•"
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
                icon = { Text(navigationSymbol(destination), fontWeight = FontWeight.Bold) },
                label = { Text(destination.title, maxLines = 1) },
                alwaysShowLabel = false
            )
        }
    } else {
        Row(modifier = Modifier.fillMaxWidth()) {
            destinations.take(4).forEach { destination ->
                val selected = destination.route == selectedRoute
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 3.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (selected) MaterialTheme.colorScheme.secondaryContainer
                            else MaterialTheme.colorScheme.surface
                        )
                        .padding(vertical = 5.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    TextButton(onClick = { onSelect(destination.route) }) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                navigationSymbol(destination),
                                fontWeight = FontWeight.Bold,
                                color = if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                destination.title,
                                maxLines = 1,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardScreen() {
    val isTablet = LocalConfiguration.current.screenWidthDp >= 600
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Selamat datang kembali", style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(4.dp))
                Text("Dashboard", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                Text("Ringkasan operasional toko hari ini", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            StoreStatus()
        }

        SalesHeroCard()

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            QuickAction("Transaksi baru", "Mulai penjualan", "+", Modifier.weight(1f))
            QuickAction("Scan QRIS", "Pembayaran cepat", "QR", Modifier.weight(1f))
            if (isTablet) QuickAction("Tambah produk", "Kelola katalog", "+", Modifier.weight(1f))
        }

        Text("Ringkasan hari ini", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        if (isTablet) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard("Transaksi", "24", "↑ 8,3%", Modifier.weight(1f))
                MetricCard("Pembayaran QRIS", "Rp 875.000", "70% dari penjualan", Modifier.weight(1f))
                MetricCard("Stok menipis", "3", "Perlu perhatian", Modifier.weight(1f))
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MetricCard("Transaksi", "24", "↑ 8,3%", Modifier.weight(1f))
                    MetricCard("Pembayaran QRIS", "Rp 875.000", "70% dari penjualan", Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MetricCard("Stok menipis", "3", "Perlu perhatian", Modifier.weight(1f))
                    MetricCard("Pelanggan", "18", "4 pelanggan baru", Modifier.weight(1f))
                }
            }
        }
        RecentTransactions()
    }
}

@Composable
private fun StoreStatus() {
    Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceVariant)) {
        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(9.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
            SpacerWidth(8.dp)
            Column {
                Text("Toko Demo", fontWeight = FontWeight.SemiBold)
                Text("Online", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun SalesHeroCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.primary)
    ) {
        Column(modifier = Modifier.padding(22.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column {
                    Text("Penjualan hari ini", color = MaterialTheme.colorScheme.onPrimary.copy(alpha = .82f), style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(5.dp))
                    Text("Rp 1.250.000", color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text("↑ 12,5% dibanding kemarin", color = MaterialTheme.colorScheme.onPrimary.copy(alpha = .82f), style = MaterialTheme.typography.bodySmall)
                }
                Text("HARI INI", color = MaterialTheme.colorScheme.onPrimary.copy(alpha = .75f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(18.dp))
            Button(
                onClick = {},
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onPrimary,
                    contentColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(12.dp)
            ) { Text("Lihat laporan") }
        }
    }
}

@Composable
private fun QuickAction(title: String, subtitle: String, symbol: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Box(
                modifier = Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) { Text(symbol, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) }
            Spacer(Modifier.height(10.dp))
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun MetricCard(title: String, value: String, note: String, modifier: Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(18.dp), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(6.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(3.dp))
            Text(note, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun RecentTransactions() {
    val transactions = listOf(
        "TRX-240901-024" to "Rp 125.000",
        "TRX-240901-023" to "Rp 75.000",
        "TRX-240901-022" to "Rp 210.000"
    )
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Transaksi terbaru", fontWeight = FontWeight.Bold)
                    Text("Aktivitas pembayaran hari ini", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextButton(onClick = {}) { Text("Lihat semua") }
            }
            Spacer(Modifier.height(6.dp))
            transactions.forEachIndexed { index, (id, amount) ->
                if (index > 0) HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(38.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer), contentAlignment = Alignment.Center) {
                        Text("QR", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                    }
                    SpacerWidth(12.dp)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(id, fontWeight = FontWeight.SemiBold)
                        Text("QRIS • Berhasil", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                    Text(amount, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun FeaturePlaceholderScreen(title: String) {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(title, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Card(shape = RoundedCornerShape(20.dp)) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Modul $title", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text("Tampilan modul sedang disiapkan dengan data demo dan akan terhubung ke Supabase pada tahap integrasi berikutnya.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun SpacerWidth(width: Dp) { Spacer(modifier = Modifier.width(width)) }
