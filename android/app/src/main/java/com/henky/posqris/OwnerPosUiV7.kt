@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.henky.posqris

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch
import kotlinx.serialization.json.*
import java.util.UUID

private val posClient = createSupabaseClient(BuildConfig.SUPABASE_URL, BuildConfig.SUPABASE_PUBLISHABLE_KEY) { install(Postgrest) }
private val bg = Color(0xFFF4F7FB)
private val navy = Color(0xFF071A33)
private val blue = Color(0xFF2563EB)
private val soft = Color(0xFFEAF2FF)
private val muted = Color(0xFF617089)
private val red = Color(0xFFD94A4A)
private val menus = listOf("Dashboard", "Penjualan", "Pesanan", "Produk", "Stok", "Pelanggan", "Supplier", "Pembelian", "Piutang", "Laporan", "Pembayaran", "Pengaturan")
private fun s(o: JsonObject, k: String) = o[k]?.jsonPrimitive?.contentOrNull.orEmpty()
private fun l(o: JsonObject, k: String) = o[k]?.jsonPrimitive?.longOrNull ?: 0L
private fun d(o: JsonObject, k: String) = o[k]?.jsonPrimitive?.doubleOrNull ?: 0.0
private fun rupiah(v: Long) = "Rp " + v.toString().reversed().chunked(3).joinToString(".").reversed()
private fun icon(name: String) = when (name) {
    "Dashboard" -> Icons.Default.Home
    "Penjualan" -> Icons.Default.ShoppingCart
    "Pesanan" -> Icons.Default.Receipt
    "Produk" -> Icons.Default.List
    "Stok" -> Icons.Default.Inventory
    "Pelanggan" -> Icons.Default.Person
    "Supplier" -> Icons.Default.Business
    "Pembelian" -> Icons.Default.ShoppingCart
    "Piutang" -> Icons.Default.AccountBalance
    "Laporan" -> Icons.Default.Assessment
    "Pembayaran" -> Icons.Default.Payments
    else -> Icons.Default.Settings
}

@Composable
fun OwnerPosAppV7() {
    var page by remember { mutableStateOf("Dashboard") }
    var more by remember { mutableStateOf(false) }
    val tablet = LocalConfiguration.current.screenWidthDp >= 600
    MaterialTheme(colorScheme = lightColorScheme(primary = blue, background = bg, surface = Color.White)) {
        Surface(modifier = Modifier.fillMaxSize(), color = bg) {
            if (tablet) Row(Modifier.fillMaxSize()) {
                SidebarV7(page) { page = it }
                Box(Modifier.weight(1f).fillMaxSize()) { PageV7(page) }
            } else Column(Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f).fillMaxWidth()) { PageV7(page) }
                BottomNavV7(page, { page = it }) { more = true }
                if (more) MoreV7(page, { page = it; more = false }) { more = false }
            }
        }
    }
}

@Composable private fun SidebarV7(selected: String, onSelect: (String) -> Unit) {
    Surface(modifier = Modifier.width(240.dp).fillMaxHeight(), color = navy) {
        Column(Modifier.fillMaxHeight().padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(Modifier.padding(7.dp, 9.dp, 7.dp, 18.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(42.dp).background(blue, RoundedCornerShape(13.dp)), contentAlignment = Alignment.Center) { Icon(imageVector = Icons.Default.QrCode2, contentDescription = null, tint = Color.White) }
                Column(Modifier.padding(start = 10.dp)) { Text(text = "POS QRIS", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold); Text(text = "Owner workspace", color = Color(0xFFA9BAD0), fontSize = 11.sp) }
            }
            menus.forEach { item ->
                val active = item == selected
                Surface(modifier = Modifier.fillMaxWidth().clickable { onSelect(item) }, shape = RoundedCornerShape(12.dp), color = if (active) blue else Color.Transparent) {
                    Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) { Icon(imageVector = icon(item), contentDescription = null, modifier = Modifier.size(19.dp), tint = if (active) Color.White else Color(0xFFB7C6D9)); Text(text = item, modifier = Modifier.padding(start = 11.dp), color = if (active) Color.White else Color(0xFFE7EEF7), fontWeight = if (active) FontWeight.Bold else FontWeight.Medium) }
                }
            }
            Spacer(Modifier.weight(1f))
            Surface(shape = RoundedCornerShape(15.dp), color = Color(0xFF0D2542)) { Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) { Icon(imageVector = Icons.Default.CloudDone, contentDescription = null, tint = blue, modifier = Modifier.size(28.dp)); Column(Modifier.padding(start = 9.dp)) { Text(text = "LIVE", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp); Text(text = "Supabase", color = Color(0xFFA9BAD0), fontSize = 10.sp) } } }
        }
    }
}

@Composable private fun BottomNavV7(selected: String, onSelect: (String) -> Unit, onMore: () -> Unit) {
    val main = listOf("Dashboard", "Penjualan", "Produk", "Stok")
    Surface(Modifier.fillMaxWidth().navigationBarsPadding(), color = Color.White, shadowElevation = 14.dp) { Row(Modifier.fillMaxWidth().padding(7.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) { main.forEach { item -> NavV7(item, selected == item, Modifier.weight(1f)) { onSelect(item) } }; NavV7("Lainnya", selected !in main, Modifier.weight(1f), onMore) } }
}

@Composable private fun NavV7(label: String, active: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Surface(modifier = modifier.clickable { onClick() }, shape = RoundedCornerShape(13.dp), color = if (active) soft else Color.Transparent) { Column(Modifier.padding(vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) { Icon(imageVector = if (label == "Lainnya") Icons.Default.MoreHoriz else icon(label), contentDescription = null, modifier = Modifier.size(19.dp), tint = if (active) blue else muted); Text(text = label, fontSize = 10.sp, color = if (active) blue else muted, fontWeight = if (active) FontWeight.Bold else FontWeight.Medium) } }
}

@Composable private fun MoreV7(selected: String, onSelect: (String) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) { Column(Modifier.fillMaxWidth().padding(18.dp)) { Text(text = "Menu Owner", color = navy, fontSize = 23.sp, fontWeight = FontWeight.ExtraBold); Text(text = "Modul live dari Supabase", color = muted, fontSize = 12.sp, modifier = Modifier.padding(top = 3.dp, bottom = 13.dp)); menus.drop(4).forEach { item -> Surface(Modifier.fillMaxWidth().clickable { onSelect(item) }, RoundedCornerShape(12.dp), if (item == selected) soft else Color.Transparent) { Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) { Icon(imageVector = icon(item), contentDescription = null, modifier = Modifier.size(20.dp), tint = blue); Text(text = item, modifier = Modifier.padding(start = 12.dp), color = navy, fontWeight = if (item == selected) FontWeight.Bold else FontWeight.Medium) } } }; Spacer(Modifier.height(12.dp)) } }
}

@Composable private fun HeaderV7(title: String, subtitle: String, refresh: () -> Unit, loading: Boolean) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(text = title, color = navy, fontSize = 27.sp, fontWeight = FontWeight.ExtraBold); Text(text = subtitle, color = muted, fontSize = 12.sp, modifier = Modifier.padding(top = 3.dp)) }; OutlinedButton(onClick = refresh, enabled = !loading, shape = RoundedCornerShape(12.dp)) { Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(17.dp)); Spacer(Modifier.width(6.dp)); Text(text = if (loading) "Memuat" else "Refresh") } }
}

@Composable private fun CardV7(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) { Card(modifier = modifier, shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) { Column(Modifier.padding(15.dp), content = content) } }

@Composable private fun MetricV7(label: String, value: String, caption: String, modifier: Modifier) { CardV7(modifier) { Text(text = label, color = muted, fontSize = 11.sp); Text(text = value, color = navy, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 4.dp)); Text(text = caption, color = muted, fontSize = 10.sp, modifier = Modifier.padding(top = 2.dp)) } }

@Composable private fun PageV7(page: String) = when (page) {
    "Dashboard" -> DashboardV7()
    "Penjualan" -> SalesV7()
    "Produk" -> GenericV7("Produk", "products")
    "Stok" -> GenericV7("Stok", "stock_balances")
    "Laporan" -> GenericV7("Laporan", "sales")
    "Pesanan" -> GenericV7("Pesanan", "sales")
    "Pelanggan" -> GenericV7("Pelanggan", "customers")
    "Supplier" -> GenericV7("Supplier", "suppliers")
    "Pembelian" -> GenericV7("Pembelian", "purchase_orders")
    "Piutang" -> GenericV7("Piutang", "receivables")
    "Pembayaran" -> GenericV7("Pembayaran", "payments")
    else -> GenericV7(page, "businesses")
}

@Composable private fun DashboardV7() {
    var business by remember { mutableStateOf<JsonObject?>(null) }; var products by remember { mutableStateOf<List<JsonObject>>(emptyList()) }; var sales by remember { mutableStateOf<List<JsonObject>>(emptyList()) }; var loading by remember { mutableStateOf(false) }; var error by remember { mutableStateOf("") }; val scope = rememberCoroutineScope()
    suspend fun load() { loading = true; error = ""; runCatching { business = posClient.from("businesses").select { filter { eq("is_active", true) } }.decodeList<JsonObject>().firstOrNull(); products = posClient.from("products").select { filter { eq("is_active", true) } }.decodeList(); sales = posClient.from("sales").select().decodeList() }.onFailure { error = it.message ?: "Gagal memuat data" }; loading = false }
    LaunchedEffect(Unit) { load() }
    val completed = sales.filter { s(it, "status").uppercase() == "COMPLETED" }
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) { HeaderV7("Beranda", "${s(business ?: buildJsonObject {}, "name")} • Owner • live", { scope.launch { load() } }, loading); if (error.isNotBlank()) Text(text = error, color = red, fontSize = 12.sp); LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) { item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) { MetricV7("Omzet", rupiah(completed.sumOf { l(it, "total_amount") }), "completed", Modifier.weight(1.3f)); MetricV7("Transaksi", completed.size.toString(), "completed", Modifier.weight(1f)); MetricV7("Produk", products.size.toString(), "SKU aktif", Modifier.weight(1f)) } }; item { CardV7(Modifier.fillMaxWidth()) { Text(text = "Transaksi terbaru", color = navy, fontWeight = FontWeight.ExtraBold); if (sales.isEmpty()) Text(text = "Belum ada transaksi", color = muted, fontSize = 12.sp, modifier = Modifier.padding(top = 16.dp)); sales.sortedByDescending { s(it, "sale_date") }.take(8).forEach { row -> Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(text = s(row, "sale_no"), color = navy, fontWeight = FontWeight.Bold); Text(text = s(row, "sale_date").replace("T", " ").take(16), color = muted, fontSize = 10.sp) }; Text(text = rupiah(l(row, "total_amount")), color = navy, fontWeight = FontWeight.Bold) } } } } } }
}

private data class CartV7(val productId: String, val name: String, val sku: String, val unitId: String, val price: Long, val qty: Long)

@Composable private fun SalesV7() {
    var products by remember { mutableStateOf<List<JsonObject>>(emptyList()) }; var prices by remember { mutableStateOf<List<JsonObject>>(emptyList()) }; var cart by remember { mutableStateOf<List<CartV7>>(emptyList()) }; var query by remember { mutableStateOf("") }; var loading by remember { mutableStateOf(false) }; var payment by remember { mutableStateOf(false) }; val scope = rememberCoroutineScope()
    suspend fun load() { loading = true; runCatching { val b = posClient.from("businesses").select { filter { eq("is_active", true) } }.decodeList<JsonObject>().first(); products = posClient.from("products").select { filter { eq("business_id", s(b, "id")); eq("is_active", true) } }.decodeList(); val pl = posClient.from("price_lists").select { filter { eq("business_id", s(b, "id")); eq("is_default", true); eq("is_active", true) } }.decodeList<JsonObject>().firstOrNull(); prices = if (pl == null) emptyList() else posClient.from("product_prices").select { filter { eq("price_list_id", s(pl, "id")) } }.decodeList() }.also { loading = false } }
    LaunchedEffect(Unit) { load() }
    fun price(p: JsonObject, qty: Long = 1) = prices.filter { s(it, "product_id") == s(p, "id") && d(it, "min_qty") <= qty }.maxByOrNull { d(it, "min_qty") }?.let { l(it, "price") } ?: l(p, "selling_price")
    val shown = products.filter { query.isBlank() || s(it, "name").contains(query, true) || s(it, "sku").contains(query, true) }; val total = cart.sumOf { it.price * it.qty }
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { HeaderV7("Penjualan Baru", "POS • server validates price and payment", { scope.launch { load() } }, loading); OutlinedTextField(value = query, onValueChange = { query = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, placeholder = { Text(text = "Cari produk atau SKU") }, leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) }, shape = RoundedCornerShape(13.dp)); Box(Modifier.weight(1f).fillMaxWidth()) { if (shown.isEmpty()) Text(text = "Tidak ada produk", color = muted, modifier = Modifier.align(Alignment.Center)) else LazyVerticalGrid(columns = GridCells.Adaptive(155.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 84.dp)) { items(shown) { p -> CardV7(Modifier.fillMaxWidth().clickable { val id = s(p, "id"); val old = cart.firstOrNull { it.productId == id }; cart = if (old == null) cart + CartV7(id, s(p, "name"), s(p, "sku"), s(p, "base_unit_id"), price(p), 1) else cart.map { if (it.productId == id) { val q = it.qty + 1; it.copy(qty = q, price = price(p, q)) } else it } }) { Box(Modifier.fillMaxWidth().height(76.dp).background(soft, RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) { Icon(imageVector = Icons.Default.Fastfood, contentDescription = null, modifier = Modifier.size(34.dp), tint = blue) }; Text(text = s(p, "name"), color = navy, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 10.dp)); Text(text = rupiah(price(p)), color = blue, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp) } } } }; Surface(Modifier.fillMaxWidth().clickable { payment = true }, RoundedCornerShape(17.dp), color = navy) { Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) { Icon(imageVector = Icons.Default.ShoppingCart, contentDescription = null, tint = Color.White, modifier = Modifier.size(26.dp)); Column(Modifier.weight(1f).padding(start = 10.dp)) { Text(text = "Keranjang • ${cart.sumOf { it.qty }} item", color = Color.White, fontWeight = FontWeight.Bold); Text(text = rupiah(total), color = Color(0xFFBFD0E5), fontSize = 11.sp) }; Text(text = "Bayar", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp) } }; if (payment) PaymentV7(total, cart, { payment = false }) { cart = emptyList(); payment = false; scope.launch { load() } } }
}

@Composable private fun PaymentV7(total: Long, cart: List<CartV7>, dismiss: () -> Unit, completed: () -> Unit) {
    var methods by remember { mutableStateOf<List<JsonObject>>(emptyList()) }; var selected by remember { mutableStateOf("") }; var amount by remember { mutableStateOf(total.toString()) }; var loading by remember { mutableStateOf(false) }; var message by remember { mutableStateOf("") }; var pending by remember { mutableStateOf(false) }; val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) { runCatching { methods = posClient.from("payment_methods").select { filter { eq("is_active", true) } }.decodeList(); selected = methods.firstOrNull()?.let { s(it, "id") }.orEmpty() }.onFailure { message = it.message ?: "Gagal memuat metode pembayaran" } }
    val method = methods.firstOrNull { s(it, "id") == selected }; val code = s(method ?: buildJsonObject {}, "code").uppercase(); val paid = amount.toLongOrNull() ?: 0L
    ModalBottomSheet(onDismissRequest = { if (!loading) dismiss() }) { Column(Modifier.fillMaxWidth().padding(18.dp)) { Text(text = "Pembayaran", color = navy, fontSize = 23.sp, fontWeight = FontWeight.ExtraBold); Text(text = "Total ${rupiah(total)}", color = muted, fontSize = 12.sp, modifier = Modifier.padding(top = 3.dp, bottom = 12.dp)); Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) { methods.forEach { m -> FilterChip(selected = selected == s(m, "id"), onClick = { selected = s(m, "id"); message = ""; pending = false }, label = { Text(text = s(m, "name")) }) } }; OutlinedTextField(value = amount, onValueChange = { amount = it.filter(Char::isDigit) }, modifier = Modifier.fillMaxWidth().padding(top = 10.dp), singleLine = true, label = { Text(text = "Nominal pembayaran") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), shape = RoundedCornerShape(13.dp)); if (paid < total) Text(text = "Nominal harus minimal ${rupiah(total)}", color = red, fontSize = 12.sp, modifier = Modifier.padding(top = 7.dp)); if (code == "QRIS") Surface(Modifier.fillMaxWidth().padding(top = 10.dp), RoundedCornerShape(16.dp), color = Color(0xFFF7FAFF)) { Column(Modifier.fillMaxWidth().padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) { Icon(imageVector = Icons.Default.QrCode2, contentDescription = null, modifier = Modifier.size(72.dp), tint = navy); Text(text = if (pending) "QRIS PENDING" else "QRIS", color = navy, fontWeight = FontWeight.Bold); Text(text = "Konfigurasi QRIS cabang harus aktif", color = muted, fontSize = 11.sp) } }; if (message.isNotBlank()) Text(text = message, color = if (pending) Color(0xFFF2A900) else red, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp)); Button(onClick = { scope.launch { loading = true; message = ""; runCatching { require(paid >= total) { "Nominal pembayaran belum mencukupi" }; val b = posClient.from("businesses").select { filter { eq("is_active", true) } }.decodeList<JsonObject>().first(); val branch = posClient.from("branches").select { filter { eq("business_id", s(b, "id")); eq("is_active", true) } }.decodeList<JsonObject>().first(); val location = posClient.from("locations").select { filter { eq("branch_id", s(branch, "id")); eq("is_active", true) } }.decodeList<JsonObject>().first(); val result = posClient.postgrest.rpc("checkout_sale_multi_payment", buildJsonObject { put("p_branch_id", s(branch, "id")); put("p_location_id", s(location, "id")); put("p_customer_id", JsonNull); put("p_items", buildJsonArray { cart.forEach { item -> add(buildJsonObject { put("product_id", item.productId); put("unit_id", item.unitId); put("sku", item.sku); put("name", item.name); put("qty", item.qty); put("conversion_to_base", 1); put("unit_price", item.price); put("hpp_unit", 0) }) } }); put("p_payments", buildJsonArray { add(buildJsonObject { put("payment_method_id", selected); put("amount", paid); put("cash_received", paid); put("reference", ""); put("qris_confirmed", false) }) }); put("p_idempotency_key", UUID.randomUUID().toString()) }); val rows = result.decodeList<JsonObject>(); val status = s(rows.firstOrNull() ?: buildJsonObject {}, "sale_status").uppercase(); if (status == "COMPLETED") completed() else { pending = true; message = "Transaksi tersimpan dengan status ${if (status.isBlank()) "PENDING" else status}. Keranjang tidak dihapus." } }.onFailure { message = it.message ?: "Checkout gagal" }; loading = false } }, enabled = !loading && !pending && cart.isNotEmpty() && selected.isNotBlank() && paid >= total, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp)) { Text(text = if (loading) "Memproses..." else if (pending) "Menunggu status" else "Proses Pembayaran", fontWeight = FontWeight.ExtraBold) }; Spacer(Modifier.height(12.dp)) } }
}

@Composable private fun GenericV7(title: String, table: String) {
    var rows by remember { mutableStateOf<List<JsonObject>>(emptyList()) }; var loading by remember { mutableStateOf(false) }; var error by remember { mutableStateOf("") }; val scope = rememberCoroutineScope()
    suspend fun load() { loading = true; error = ""; runCatching { rows = posClient.from(table).select().decodeList() }.onFailure { error = it.message ?: "Gagal memuat data" }; loading = false }
    LaunchedEffect(Unit) { load() }
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { HeaderV7(title, "$table • live Supabase", { scope.launch { load() } }, loading); if (error.isNotBlank()) Text(text = error, color = red, fontSize = 12.sp); LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) { items(rows.take(100)) { row -> CardV7 { row.entries.take(6).forEach { (k, v) -> Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) { Text(text = k, modifier = Modifier.weight(1f), color = muted, fontSize = 10.sp); Text(text = v.toString().removeSurrounding("\""), color = navy, fontSize = 11.sp) } } } } } }
}
