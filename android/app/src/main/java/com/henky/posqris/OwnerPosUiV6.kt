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

private val v6Client = createSupabaseClient(BuildConfig.SUPABASE_URL, BuildConfig.SUPABASE_PUBLISHABLE_KEY) { install(Postgrest) }
private val V6Bg = Color(0xFFF4F7FB)
private val V6Navy = Color(0xFF071A33)
private val V6Blue = Color(0xFF2563EB)
private val V6Soft = Color(0xFFEAF2FF)
private val V6Muted = Color(0xFF617089)
private val V6Green = Color(0xFF14966B)
private val V6Amber = Color(0xFFF2A900)
private val V6Red = Color(0xFFD94A4A)
private val V6Menus = listOf("Dashboard", "Penjualan", "Pesanan", "Produk", "Stok", "Pelanggan", "Supplier", "Pembelian", "Piutang", "Laporan", "Pembayaran", "Pengaturan")

private fun v6s(o: JsonObject, key: String) = o[key]?.jsonPrimitive?.contentOrNull.orEmpty()
private fun v6l(o: JsonObject, key: String) = o[key]?.jsonPrimitive?.longOrNull ?: 0L
private fun v6d(o: JsonObject, key: String) = o[key]?.jsonPrimitive?.doubleOrNull ?: 0.0
private fun v6Money(value: Long) = "Rp " + value.toString().reversed().chunked(3).joinToString(".").reversed()
private fun v6Icon(name: String) = when (name) {
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
fun OwnerPosAppV6() {
    var page by remember { mutableStateOf("Dashboard") }
    var more by remember { mutableStateOf(false) }
    val tablet = LocalConfiguration.current.screenWidthDp >= 600
    MaterialTheme(colorScheme = lightColorScheme(primary = V6Blue, background = V6Bg, surface = Color.White)) {
        Surface(Modifier.fillMaxSize(), color = V6Bg) {
            if (tablet) Row(Modifier.fillMaxSize()) {
                V6Sidebar(page) { page = it }
                Box(Modifier.weight(1f).fillMaxSize()) { V6Page(page) }
            } else Column(Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f).fillMaxWidth()) { V6Page(page) }
                V6BottomNav(page, { page = it }) { more = true }
                if (more) V6MoreSheet(page, { page = it; more = false }) { more = false }
            }
        }
    }
}

@Composable private fun V6Sidebar(selected: String, onSelect: (String) -> Unit) {
    Surface(Modifier.width(240.dp).fillMaxHeight(), color = V6Navy) {
        Column(Modifier.fillMaxHeight().padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(Modifier.padding(7.dp, 9.dp, 7.dp, 18.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(42.dp).background(V6Blue, RoundedCornerShape(13.dp)), Alignment.Center) { Icon(Icons.Default.QrCode2, null, tint = Color.White) }
                Column(Modifier.padding(start = 10.dp)) { Text("POS QRIS", Color.White, 18.sp, FontWeight.ExtraBold); Text("Owner workspace", Color(0xFFA9BAD0), 11.sp) }
            }
            V6Menus.forEach { item ->
                val active = item == selected
                Surface(Modifier.fillMaxWidth().clickable { onSelect(item) }, RoundedCornerShape(12.dp), if (active) V6Blue else Color.Transparent) {
                    Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(v6Icon(item), null, Modifier.size(19.dp), if (active) Color.White else Color(0xFFB7C6D9))
                        Text(item, Modifier.padding(start = 11.dp), if (active) Color.White else Color(0xFFE7EEF7), fontWeight = if (active) FontWeight.Bold else FontWeight.Medium)
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            Surface(RoundedCornerShape(15.dp), color = Color(0xFF0D2542)) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.CloudDone, null, tint = V6Blue, Modifier.size(28.dp)); Column(Modifier.padding(start = 9.dp)) { Text("LIVE", Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp); Text("Supabase", Color(0xFFA9BAD0), fontSize = 10.sp) } }
            }
        }
    }
}

@Composable private fun V6BottomNav(selected: String, onSelect: (String) -> Unit, onMore: () -> Unit) {
    val main = listOf("Dashboard", "Penjualan", "Produk", "Stok")
    Surface(Modifier.fillMaxWidth().navigationBarsPadding(), Color.White, shadowElevation = 14.dp) {
        Row(Modifier.fillMaxWidth().padding(7.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            main.forEach { item -> V6NavItem(item, selected == item, Modifier.weight(1f)) { onSelect(item) } }
            V6NavItem("Lainnya", selected !in main, Modifier.weight(1f), onMore)
        }
    }
}

@Composable private fun V6NavItem(label: String, active: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Surface(modifier.clickable { onClick() }, RoundedCornerShape(13.dp), if (active) V6Soft else Color.Transparent) {
        Column(Modifier.padding(vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) { Icon(if (label == "Lainnya") Icons.Default.MoreHoriz else v6Icon(label), null, Modifier.size(19.dp), if (active) V6Blue else V6Muted); Text(label, fontSize = 10.sp, color = if (active) V6Blue else V6Muted, fontWeight = if (active) FontWeight.Bold else FontWeight.Medium) }
    }
}

@Composable private fun V6MoreSheet(selected: String, onSelect: (String) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) { Column(Modifier.fillMaxWidth().padding(18.dp)) {
        Text("Menu Owner", V6Navy, 23.sp, FontWeight.ExtraBold)
        Text("Modul membaca data langsung dari Supabase", V6Muted, 12.sp, modifier = Modifier.padding(top = 3.dp, bottom = 13.dp))
        V6Menus.drop(4).forEach { item -> Surface(Modifier.fillMaxWidth().clickable { onSelect(item) }, RoundedCornerShape(12.dp), if (item == selected) V6Soft else Color.Transparent) { Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) { Icon(v6Icon(item), null, Modifier.size(20.dp), tint = V6Blue); Text(item, Modifier.padding(start = 12.dp), V6Navy, fontWeight = if (item == selected) FontWeight.Bold else FontWeight.Medium) } } }
        Spacer(Modifier.height(14.dp))
    } }
}

@Composable private fun V6Header(title: String, subtitle: String, refresh: () -> Unit, loading: Boolean) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) { Text(title, V6Navy, 27.sp, FontWeight.ExtraBold); Text(subtitle, V6Muted, 12.sp, modifier = Modifier.padding(top = 3.dp)) }
        OutlinedButton(onClick = refresh, enabled = !loading, shape = RoundedCornerShape(12.dp)) { Icon(Icons.Default.Refresh, null, Modifier.size(17.dp)); Spacer(Modifier.width(6.dp)); Text(if (loading) "Memuat" else "Refresh") }
    }
}

@Composable private fun V6Card(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) = Card(modifier, RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(Color.White), elevation = CardDefaults.cardElevation(1.dp)) { Column(Modifier.padding(15.dp), content = content) }

@Composable private fun V6Page(page: String) = when (page) {
    "Dashboard" -> V6Dashboard()
    "Penjualan" -> V6Sales()
    "Produk" -> V6Products()
    "Stok" -> V6Stock()
    "Laporan" -> V6Reports()
    else -> V6Generic(page)
}

@Composable private fun V6Dashboard() {
    var business by remember { mutableStateOf<JsonObject?>(null) }; var products by remember { mutableStateOf<List<JsonObject>>(emptyList()) }; var sales by remember { mutableStateOf<List<JsonObject>>(emptyList()) }; var stock by remember { mutableStateOf<List<JsonObject>>(emptyList()) }; var loading by remember { mutableStateOf(false) }; var error by remember { mutableStateOf("") }; val scope = rememberCoroutineScope()
    suspend fun load() { loading = true; error = ""; runCatching { business = v6Client.from("businesses").select { filter { eq("is_active", true) } }.decodeList<JsonObject>().firstOrNull(); products = v6Client.from("products").select { filter { eq("is_active", true) } }.decodeList(); sales = v6Client.from("sales").select().decodeList(); stock = v6Client.from("stock_balances").select().decodeList() }.onFailure { error = it.message ?: "Gagal memuat data" }; loading = false }
    LaunchedEffect(Unit) { load() }
    val completed = sales.filter { v6s(it, "status").uppercase() == "COMPLETED" }
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        V6Header("Beranda", "${v6s(business ?: buildJsonObject {}, "name")} • Owner • live", { scope.launch { load() } }, loading)
        if (error.isNotBlank()) Text(error, V6Red, 12.sp)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) { V6Metric("Omzet", v6Money(completed.sumOf { v6l(it, "total_amount") }), "completed", Modifier.weight(1.3f)); V6Metric("Transaksi", completed.size.toString(), "completed", Modifier.weight(1f)); V6Metric("Produk", products.size.toString(), "SKU aktif", Modifier.weight(1f)); V6Metric("Saldo stok", stock.size.toString(), "baris", Modifier.weight(1f)) } }
            item { V6Card(Modifier.fillMaxWidth()) { Text("Transaksi terbaru", V6Navy, fontWeight = FontWeight.ExtraBold); if (sales.isEmpty()) Text("Belum ada transaksi", V6Muted, 12.sp, modifier = Modifier.padding(top = 16.dp)); sales.sortedByDescending { v6s(it, "sale_date") }.take(8).forEach { row -> Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(v6s(row, "sale_no"), V6Navy, fontWeight = FontWeight.Bold); Text(v6s(row, "sale_date").replace("T", " ").take(16), V6Muted, 10.sp) }; Text(v6Money(v6l(row, "total_amount")), V6Navy, fontWeight = FontWeight.Bold) } } } }
        }
    }
}

@Composable private fun V6Metric(label: String, value: String, caption: String, modifier: Modifier) { V6Card(modifier) { Text(label, V6Muted, 11.sp); Text(value, V6Navy, 21.sp, FontWeight.ExtraBold, modifier = Modifier.padding(top = 4.dp)); Text(caption, V6Muted, 10.sp, modifier = Modifier.padding(top = 2.dp)) } }

private data class V6Cart(val productId: String, val name: String, val sku: String, val unitId: String, val price: Long, val qty: Long)

@Composable private fun V6Sales() {
    var products by remember { mutableStateOf<List<JsonObject>>(emptyList()) }; var prices by remember { mutableStateOf<List<JsonObject>>(emptyList()) }; var cart by remember { mutableStateOf<List<V6Cart>>(emptyList()) }; var query by remember { mutableStateOf("") }; var loading by remember { mutableStateOf(false) }; var paymentOpen by remember { mutableStateOf(false) }; val scope = rememberCoroutineScope()
    suspend fun load() { loading = true; runCatching { val b = v6Client.from("businesses").select { filter { eq("is_active", true) } }.decodeList<JsonObject>().first(); products = v6Client.from("products").select { filter { eq("business_id", v6s(b, "id")); eq("is_active", true) } }.decodeList(); val pl = v6Client.from("price_lists").select { filter { eq("business_id", v6s(b, "id")); eq("is_default", true); eq("is_active", true) } }.decodeList<JsonObject>().firstOrNull(); prices = if (pl == null) emptyList() else v6Client.from("product_prices").select { filter { eq("price_list_id", v6s(pl, "id")) } }.decodeList() }.also { loading = false } }
    LaunchedEffect(Unit) { load() }
    fun price(product: JsonObject, qty: Long = 1): Long = prices.filter { v6s(it, "product_id") == v6s(product, "id") && v6d(it, "min_qty") <= qty }.maxByOrNull { v6d(it, "min_qty") }?.let { v6l(it, "price") } ?: v6l(product, "selling_price")
    val shown = products.filter { query.isBlank() || v6s(it, "name").contains(query, true) || v6s(it, "sku").contains(query, true) }; val total = cart.sumOf { it.price * it.qty }
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        V6Header("Penjualan Baru", "POS • harga dari default price list", { scope.launch { load() } }, loading)
        OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), singleLine = true, placeholder = { Text("Cari produk atau SKU") }, leadingIcon = { Icon(Icons.Default.Search, null) }, shape = RoundedCornerShape(13.dp))
        Box(Modifier.weight(1f).fillMaxWidth()) { if (shown.isEmpty()) Text("Tidak ada produk", V6Muted, modifier = Modifier.align(Alignment.Center)) else LazyVerticalGrid(GridCells.Adaptive(155.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 84.dp)) { items(shown) { p -> V6Card(Modifier.fillMaxWidth().clickable { val id = v6s(p, "id"); val old = cart.firstOrNull { it.productId == id }; cart = if (old == null) cart + V6Cart(id, v6s(p, "name"), v6s(p, "sku"), v6s(p, "base_unit_id"), price(p), 1) else cart.map { if (it.productId == id) { val q = it.qty + 1; it.copy(qty = q, price = price(p, q)) } else it } }) { Box(Modifier.fillMaxWidth().height(76.dp).background(V6Soft, RoundedCornerShape(14.dp)), Alignment.Center) { Icon(Icons.Default.Fastfood, null, Modifier.size(34.dp), tint = V6Blue) }; Text(v6s(p, "name"), V6Navy, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 10.dp)); Text(v6Money(price(p)), V6Blue, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp) } } } }
        Surface(Modifier.fillMaxWidth().clickable { paymentOpen = true }, RoundedCornerShape(17.dp), V6Navy) { Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.ShoppingCart, null, Color.White, Modifier.size(26.dp)); Column(Modifier.weight(1f).padding(start = 10.dp)) { Text("Keranjang • ${cart.sumOf { it.qty }} item", Color.White, fontWeight = FontWeight.Bold); Text(v6Money(total), Color(0xFFBFD0E5), 11.sp) }; Text("Bayar", Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp) } }
        if (paymentOpen) V6PaymentSheet(total, cart, { paymentOpen = false }, { cart = emptyList(); paymentOpen = false; scope.launch { load() } })
    }
}

@Composable private fun V6PaymentSheet(total: Long, cart: List<V6Cart>, dismiss: () -> Unit, completed: () -> Unit) {
    var methods by remember { mutableStateOf<List<JsonObject>>(emptyList()) }; var selected by remember { mutableStateOf("") }; var amount by remember { mutableStateOf(total.toString()) }; var loading by remember { mutableStateOf(false) }; var message by remember { mutableStateOf("") }; var pending by remember { mutableStateOf(false) }; val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) { runCatching { methods = v6Client.from("payment_methods").select { filter { eq("is_active", true) } }.decodeList(); selected = methods.firstOrNull()?.let { v6s(it, "id") }.orEmpty() }.onFailure { message = it.message ?: "Gagal memuat metode pembayaran" } }
    val method = methods.firstOrNull { v6s(it, "id") == selected }; val code = v6s(method ?: buildJsonObject {}, "code").uppercase(); val payAmount = amount.toLongOrNull() ?: 0L
    ModalBottomSheet(onDismissRequest = { if (!loading) dismiss() }) { Column(Modifier.fillMaxWidth().padding(18.dp)) {
        Text("Pembayaran", V6Navy, 23.sp, FontWeight.ExtraBold); Text("Total ${v6Money(total)}", V6Muted, 12.sp, modifier = Modifier.padding(top = 3.dp, bottom = 12.dp))
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) { methods.forEach { m -> FilterChip(selected == v6s(m, "id"), { selected = v6s(m, "id"); message = ""; pending = false }, label = { Text(v6s(m, "name")) }) } }
        OutlinedTextField(amount, { amount = it.filter(Char::isDigit) }, Modifier.fillMaxWidth().padding(top = 10.dp), singleLine = true, label = { Text("Nominal pembayaran") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), shape = RoundedCornerShape(13.dp))
        if (payAmount < total) Text("Nominal pembayaran belum mencukupi total transaksi.", V6Red, 12.sp, modifier = Modifier.padding(top = 7.dp))
        if (code == "QRIS") { Surface(Modifier.fillMaxWidth().padding(top = 10.dp), RoundedCornerShape(16.dp), Color(0xFFF7FAFF)) { Column(Modifier.fillMaxWidth().padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Default.QrCode2, null, Modifier.size(72.dp), tint = V6Navy); Text(if (pending) "Pembayaran QRIS PENDING" else "QRIS", V6Navy, fontWeight = FontWeight.Bold); Text("QRIS harus dikonfigurasi aktif di cabang", V6Muted, 11.sp) } } }
        if (message.isNotBlank()) Text(message, if (pending) V6Amber else V6Red, 12.sp, modifier = Modifier.padding(top = 8.dp))
        Button(onClick = { scope.launch { loading = true; message = ""; runCatching {
            require(payAmount >= total) { "Nominal pembayaran harus minimal ${v6Money(total)}" }
            val b = v6Client.from("businesses").select { filter { eq("is_active", true) } }.decodeList<JsonObject>().first(); val branch = v6Client.from("branches").select { filter { eq("business_id", v6s(b, "id")); eq("is_active", true) } }.decodeList<JsonObject>().first(); val location = v6Client.from("locations").select { filter { eq("branch_id", v6s(branch, "id")); eq("is_active", true) } }.decodeList<JsonObject>().first()
            val result = v6Client.postgrest.rpc("checkout_sale_multi_payment", buildJsonObject {
                put("p_branch_id", v6s(branch, "id")); put("p_location_id", v6s(location, "id")); put("p_customer_id", JsonNull)
                put("p_items", buildJsonArray { cart.forEach { line -> add(buildJsonObject { put("product_id", line.productId); put("unit_id", line.unitId); put("sku", line.sku); put("name", line.name); put("qty", line.qty); put("conversion_to_base", 1); put("unit_price", line.price); put("hpp_unit", 0) }) } })
                put("p_payments", buildJsonArray { add(buildJsonObject { put("payment_method_id", selected); put("amount", payAmount); put("cash_received", payAmount); put("reference", ""); put("qris_confirmed", false) }) }); put("p_idempotency_key", UUID.randomUUID().toString())
            })
            val row = runCatching { result.decodeSingle<JsonObject>() }.getOrNull(); val status = v6s(row ?: buildJsonObject {}, "sale_status").uppercase()
            if (status == "COMPLETED") completed() else { pending = true; message = "Transaksi tersimpan dengan status ${if (status.isBlank()) "PENDING" else status}. Keranjang tidak dihapus." }
        }.onFailure { message = it.message ?: "Checkout gagal" }; loading = false } }, enabled = !loading && !pending && cart.isNotEmpty() && selected.isNotBlank() && payAmount >= total, Modifier.fillMaxWidth().height(52.dp), RoundedCornerShape(14.dp)) { Text(if (loading) "Memproses..." else if (pending) "Menunggu status" else "Proses Pembayaran", fontWeight = FontWeight.ExtraBold) }
        Spacer(Modifier.height(12.dp))
    } }
}

@Composable private fun V6Products() { V6Generic("Produk") }
@Composable private fun V6Stock() { V6Generic("Stok") }
@Composable private fun V6Reports() { V6Generic("Laporan") }

@Composable private fun V6Generic(title: String) {
    val table = when (title) { "Pesanan" -> "sales"; "Produk" -> "products"; "Stok" -> "stock_balances"; "Pelanggan" -> "customers"; "Supplier" -> "suppliers"; "Pembelian" -> "purchase_orders"; "Piutang" -> "receivables"; "Pembayaran" -> "payments"; "Laporan" -> "sales"; else -> "sales" }
    var rows by remember { mutableStateOf<List<JsonObject>>(emptyList()) }; var loading by remember { mutableStateOf(false) }; var error by remember { mutableStateOf("") }; val scope = rememberCoroutineScope()
    suspend fun load() { loading = true; error = ""; runCatching { rows = v6Client.from(table).select().decodeList() }.onFailure { error = it.message ?: "Gagal memuat data" }; loading = false }
    LaunchedEffect(Unit) { load() }
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { V6Header(title, "$table • live Supabase", { scope.launch { load() } }, loading); if (error.isNotBlank()) Text(error, V6Red, 12.sp); LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) { items(rows.take(100)) { row -> V6Card { row.entries.take(6).forEach { (key, value) -> Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) { Text(key, Modifier.weight(1f), V6Muted, 10.sp); Text(value.toString().removeSurrounding("\""), V6Navy, 11.sp) } } } } } }
}
