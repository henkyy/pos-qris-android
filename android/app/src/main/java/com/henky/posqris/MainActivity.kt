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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.input.KeyboardType
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

data class DemoProduct(val id: Int, val name: String, val category: String, val price: Long, val stock: Int)
data class DemoCustomer(val id: Int, val name: String, val phone: String)
data class DemoTransaction(val id: String, val total: Long, val method: String, val status: String)

private fun rupiah(value: Long): String {
    val formatted = value.toString().reversed().chunked(3).joinToString(".").reversed()
    return "Rp $formatted"
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
            when (selectedRoute) {
                PosDestination.Dashboard.route -> DashboardScreen { selectedRoute = it }
                PosDestination.Pos.route -> SalesScreen()
                PosDestination.Products.route -> ProductsScreen()
                PosDestination.Inventory.route -> InventoryScreen()
                PosDestination.Customers.route -> CustomersScreen()
                PosDestination.Payments.route -> PaymentsScreen()
                PosDestination.Reports.route -> ReportsScreen()
                PosDestination.Settings.route -> SettingsScreen()
                else -> DashboardScreen { selectedRoute = PosDestination.Dashboard.route }
            }
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
            TextButton(onClick = { onSelect(destination.route) }, modifier = Modifier.fillMaxWidth()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        navigationSymbol(destination),
                        fontWeight = FontWeight.Bold,
                        color = if (destination.route == selectedRoute) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(destination.title, maxLines = 1, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    } else {
        Row(modifier = Modifier.fillMaxWidth()) {
            destinations.take(4).forEach { destination ->
                val selected = destination.route == selectedRoute
                TextButton(onClick = { onSelect(destination.route) }, modifier = Modifier.weight(1f)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(navigationSymbol(destination), fontWeight = FontWeight.Bold,
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(destination.title, maxLines = 1, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun ScreenHeader(title: String, subtitle: String, action: (@Composable () -> Unit)? = null) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        action?.invoke()
    }
}

@Composable
private fun DashboardScreen(onNavigate: (String) -> Unit) {
    val scroll = rememberScrollState()
    Column(modifier = Modifier.fillMaxSize().verticalScroll(scroll), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ScreenHeader("Dashboard", "Ringkasan operasional toko hari ini")
        Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(MaterialTheme.colorScheme.primary)) {
            Column(modifier = Modifier.padding(22.dp)) {
                Text("Penjualan hari ini", color = MaterialTheme.colorScheme.onPrimary.copy(alpha = .8f))
                Text(rupiah(1_250_000), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                Text("↑ 12,5% dibanding kemarin", color = MaterialTheme.colorScheme.onPrimary.copy(alpha = .8f))
                Spacer(Modifier.height(14.dp))
                Button(onClick = { onNavigate(PosDestination.Reports.route) }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onPrimary, contentColor = MaterialTheme.colorScheme.primary)) {
                    Text("Lihat laporan")
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            ActionCard("Transaksi baru", "Mulai penjualan", "+", Modifier.weight(1f)) { onNavigate(PosDestination.Pos.route) }
            ActionCard("Produk", "Kelola katalog", "P", Modifier.weight(1f)) { onNavigate(PosDestination.Products.route) }
            ActionCard("Stok", "Cek persediaan", "S", Modifier.weight(1f)) { onNavigate(PosDestination.Inventory.route) }
        }
        Text("Ringkasan hari ini", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            MetricCard("Transaksi", "24", "↑ 8,3%", Modifier.weight(1f))
            MetricCard("QRIS", rupiah(875_000), "70% penjualan", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            MetricCard("Stok menipis", "3", "Perlu perhatian", Modifier.weight(1f))
            MetricCard("Pelanggan", "18", "4 baru hari ini", Modifier.weight(1f))
        }
        RecentTransactions()
    }
}

@Composable
private fun ActionCard(title: String, subtitle: String, symbol: String, modifier: Modifier, onClick: () -> Unit) {
    Card(modifier = modifier, shape = RoundedCornerShape(18.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Button(onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) { Text(symbol) }
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun MetricCard(title: String, value: String, note: String, modifier: Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(18.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(note, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun SalesScreen() {
    val products = remember { listOf(
        DemoProduct(1, "Kopi Susu", "Minuman", 18_000, 20),
        DemoProduct(2, "Nasi Goreng", "Makanan", 25_000, 12),
        DemoProduct(3, "Es Teh", "Minuman", 8_000, 30),
        DemoProduct(4, "Mie Goreng", "Makanan", 20_000, 15),
        DemoProduct(5, "Air Mineral", "Minuman", 5_000, 40),
        DemoProduct(6, "Roti Bakar", "Snack", 15_000, 8)
    ) }
    var cart by remember { mutableStateOf(mapOf<Int, Int>()) }
    var paid by remember { mutableStateOf(false) }
    val total = cart.entries.sumOf { entry -> products.first { it.id == entry.key }.price * entry.value }
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ScreenHeader("Penjualan", "Pilih produk untuk membuat transaksi")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Text("Keranjang: ${cart.values.sum()} item", fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Text(rupiah(total), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(products) { product ->
                Card(shape = RoundedCornerShape(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(product.name, fontWeight = FontWeight.Bold)
                            Text(product.category, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(rupiah(product.price), color = MaterialTheme.colorScheme.primary)
                        }
                        OutlinedButton(onClick = { cart = cart + (product.id to ((cart[product.id] ?: 0) + 1)) }) { Text("+ Tambah") }
                    }
                }
            }
        }
        if (cart.isNotEmpty()) {
            Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(MaterialTheme.colorScheme.secondaryContainer)) {
                Column(modifier = Modifier.padding(14.dp)) {
                    cart.forEach { (id, qty) ->
                        val product = products.first { it.id == id }
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text("${product.name} × $qty", modifier = Modifier.weight(1f))
                            TextButton(onClick = { cart = if (qty <= 1) cart - id else cart + (id to qty - 1) }) { Text("−") }
                            TextButton(onClick = { cart = cart + (id to qty + 1) }) { Text("+") }
                        }
                    }
                    Button(onClick = { paid = true }, modifier = Modifier.fillMaxWidth(), enabled = total > 0) { Text("Bayar ${rupiah(total)}") }
                    if (paid) Text("Pembayaran demo berhasil. Transaksi tersimpan sebagai demo.", color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp))
                }
            }
        }
    }
}

@Composable
private fun ProductsScreen() {
    var products by remember { mutableStateOf(listOf(
        DemoProduct(1, "Kopi Susu", "Minuman", 18_000, 20),
        DemoProduct(2, "Nasi Goreng", "Makanan", 25_000, 12),
        DemoProduct(3, "Es Teh", "Minuman", 8_000, 30),
        DemoProduct(4, "Mie Goreng", "Makanan", 20_000, 15)
    )) }
    var name by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var showForm by remember { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ScreenHeader("Produk", "Kelola katalog produk toko") {
            Button(onClick = { showForm = !showForm }) { Text(if (showForm) "Tutup" else "+ Produk") }
        }
        if (showForm) {
            Card(shape = RoundedCornerShape(18.dp)) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(name, { name = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Nama produk") })
                    OutlinedTextField(price, { price = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Harga") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    Button(onClick = {
                        val parsed = price.toLongOrNull()
                        if (name.isNotBlank() && parsed != null && parsed > 0) {
                            products = products + DemoProduct((products.maxOfOrNull { it.id } ?: 0) + 1, name, "Umum", parsed, 10)
                            name = ""; price = ""; showForm = false
                        }
                    }) { Text("Simpan produk") }
                }
            }
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(products, key = { it.id }) { product ->
                Card(shape = RoundedCornerShape(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(product.name, fontWeight = FontWeight.Bold)
                            Text(product.category, style = MaterialTheme.typography.labelSmall)
                            Text(rupiah(product.price), color = MaterialTheme.colorScheme.primary)
                        }
                        TextButton(onClick = { products = products.filterNot { it.id == product.id } }) { Text("Hapus") }
                    }
                }
            }
        }
    }
}

@Composable
private fun InventoryScreen() {
    var products by remember { mutableStateOf(listOf(
        DemoProduct(1, "Kopi Susu", "Minuman", 18_000, 20),
        DemoProduct(2, "Nasi Goreng", "Makanan", 25_000, 12),
        DemoProduct(3, "Es Teh", "Minuman", 8_000, 30),
        DemoProduct(4, "Roti Bakar", "Snack", 15_000, 3)
    )) }
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ScreenHeader("Persediaan", "Pantau dan ubah stok produk")
        Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(MaterialTheme.colorScheme.tertiaryContainer)) {
            Text("${products.count { it.stock <= 5 }} produk perlu perhatian", modifier = Modifier.padding(16.dp), fontWeight = FontWeight.Bold)
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(products, key = { it.id }) { product ->
                Card(shape = RoundedCornerShape(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(product.name, fontWeight = FontWeight.Bold)
                            Text(if (product.stock <= 5) "Stok menipis" else "Stok aman", color = if (product.stock <= 5) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                        }
                        Text("${product.stock}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        TextButton(onClick = { products = products.map { if (it.id == product.id) it.copy(stock = (it.stock - 1).coerceAtLeast(0)) else it }) }) { Text("−") }
                        TextButton(onClick = { products = products.map { if (it.id == product.id) it.copy(stock = it.stock + 1) else it }) }) { Text("+") }
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomersScreen() {
    var customers by remember { mutableStateOf(listOf(
        DemoCustomer(1, "Budi Santoso", "081234567890"),
        DemoCustomer(2, "Siti Aminah", "082345678901"),
        DemoCustomer(3, "Andi Wijaya", "083456789012")
    )) }
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var showForm by remember { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ScreenHeader("Pelanggan", "Kelola data pelanggan") {
            Button(onClick = { showForm = !showForm }) { Text(if (showForm) "Tutup" else "+ Pelanggan") }
        }
        if (showForm) {
            Card(shape = RoundedCornerShape(18.dp)) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(name, { name = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Nama") })
                    OutlinedTextField(phone, { phone = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Nomor HP") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
                    Button(onClick = {
                        if (name.isNotBlank()) {
                            customers = customers + DemoCustomer((customers.maxOfOrNull { it.id } ?: 0) + 1, name, phone)
                            name = ""; phone = ""; showForm = false
                        }
                    }) { Text("Simpan pelanggan") }
                }
            }
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(customers, key = { it.id }) { customer ->
                Card(shape = RoundedCornerShape(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(42.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer), contentAlignment = Alignment.Center) { Text(customer.name.take(1), fontWeight = FontWeight.Bold) }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) { Text(customer.name, fontWeight = FontWeight.Bold); Text(customer.phone, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        TextButton(onClick = { customers = customers.filterNot { it.id == customer.id } }) { Text("Hapus") }
                    }
                }
            }
        }
    }
}

@Composable
private fun PaymentsScreen() {
    var transactions by remember { mutableStateOf(listOf(
        DemoTransaction("TRX-024", 125_000, "QRIS", "Berhasil"),
        DemoTransaction("TRX-023", 75_000, "Tunai", "Berhasil"),
        DemoTransaction("TRX-022", 210_000, "QRIS", "Menunggu")
    )) }
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ScreenHeader("Pembayaran", "Pantau transaksi dan simulasi status QRIS")
        Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(MaterialTheme.colorScheme.primaryContainer)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Pembayaran QRIS", fontWeight = FontWeight.Bold)
                Text(rupiah(transactions.filter { it.method == "QRIS" && it.status == "Berhasil" }.sumOf { it.total }), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("Data demo, belum transaksi produksi")
            }
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(transactions, key = { it.id }) { transaction ->
                Card(shape = RoundedCornerShape(16.dp)) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column { Text(transaction.id, fontWeight = FontWeight.Bold); Text(transaction.method, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                            Text(rupiah(transaction.total), fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(transaction.status, color = if (transaction.status == "Berhasil") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error, modifier = Modifier.weight(1f))
                            if (transaction.status == "Menunggu") OutlinedButton(onClick = { transactions = transactions.map { if (it.id == transaction.id) it.copy(status = "Berhasil") else it } }) { Text("Simulasikan sukses") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportsScreen() {
    var period by remember { mutableStateOf("Hari ini") }
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        ScreenHeader("Laporan", "Ringkasan performa penjualan")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Hari ini", "7 hari", "30 hari").forEach { option ->
                if (period == option) Button(onClick = { period = option }) { Text(option) }
                else OutlinedButton(onClick = { period = option }) { Text(option) }
            }
        }
        MetricCard("Omzet", rupiah(if (period == "Hari ini") 1_250_000 else if (period == "7 hari") 8_450_000 else 32_800_000), "Data demo", Modifier.fillMaxWidth())
        MetricCard("Transaksi", if (period == "Hari ini") "24" else if (period == "7 hari") "168" else "642", "Total transaksi", Modifier.fillMaxWidth())
        MetricCard("QRIS", rupiah(if (period == "Hari ini") 875_000 else if (period == "7 hari") 5_900_000 else 22_700_000), "Pembayaran QRIS", Modifier.fillMaxWidth())
        Card(shape = RoundedCornerShape(20.dp)) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text("Produk terlaris", fontWeight = FontWeight.Bold)
                listOf("Kopi Susu" to 42, "Nasi Goreng" to 31, "Es Teh" to 28).forEach { (name, count) ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) { Text(name); Text("$count terjual", fontWeight = FontWeight.SemiBold) }
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen() {
    var storeOpen by remember { mutableStateOf(true) }
    var autoPrint by remember { mutableStateOf(false) }
    var sound by remember { mutableStateOf(true) }
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ScreenHeader("Pengaturan", "Konfigurasi operasional POS")
        SettingToggle("Status toko", if (storeOpen) "Toko sedang buka" else "Toko sedang tutup", storeOpen) { storeOpen = it }
        SettingToggle("Cetak struk otomatis", if (autoPrint) "Aktif" else "Nonaktif", autoPrint) { autoPrint = it }
        SettingToggle("Suara transaksi", if (sound) "Aktif" else "Nonaktif", sound) { sound = it }
        Card(shape = RoundedCornerShape(20.dp)) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Toko Demo", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("POS QRIS", color = MaterialTheme.colorScheme.onSurfaceVariant)
                HorizontalDivider()
                Text("Mode: Demo / tanpa login")
                Text("Supabase: konfigurasi tersedia")
                Button(onClick = {}) { Text("Simpan pengaturan") }
            }
        }
    }
}

@Composable
private fun SettingToggle(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Card(shape = RoundedCornerShape(18.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) { Text(title, fontWeight = FontWeight.Bold); Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            Switch(checked = checked, onCheckedChange = onChange)
        }
    }
}

@Composable
private fun RecentTransactions() {
    val transactions = listOf("TRX-024" to "Rp 125.000", "TRX-023" to "Rp 75.000", "TRX-022" to "Rp 210.000")
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Transaksi terbaru", fontWeight = FontWeight.Bold)
                TextButton(onClick = {}) { Text("Lihat semua") }
            }
            transactions.forEachIndexed { index, (id, amount) ->
                if (index > 0) HorizontalDivider()
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(36.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer), contentAlignment = Alignment.Center) { Text("QR", fontWeight = FontWeight.Bold) }
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) { Text(id, fontWeight = FontWeight.SemiBold); Text("QRIS • Berhasil", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary) }
                    Text(amount, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
