@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.henky.posqris

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.storage.storage
import io.github.jan.supabase.storage.upload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import java.util.UUID

private val v9Client get() = SupabaseClientProvider.client
private val v9Navy = Color(0xFF071A33)
private val v9Blue = Color(0xFF2563EB)
private val v9Bg = Color(0xFFF4F7FB)
private val v9Muted = Color(0xFF617089)
private val v9Danger = Color(0xFFC53B3B)
private val v9Menus = listOf("Dashboard", "Penjualan", "Pesanan", "Produk", "Stok", "Pelanggan", "Supplier", "Pembelian", "Piutang", "Laporan", "Pembayaran", "Pengaturan")

private fun v9str(o: JsonObject, key: String) = o[key]?.jsonPrimitive?.contentOrNull.orEmpty()
private fun v9long(o: JsonObject, key: String) = o[key]?.jsonPrimitive?.longOrNull ?: 0L
private fun v9num(o: JsonObject, key: String) = o[key]?.jsonPrimitive?.doubleOrNull ?: 0.0
private fun v9money(v: Long) = "Rp " + v.toString().reversed().chunked(3).joinToString(".").reversed()
private fun v9Safe(t: Throwable): String {
    val raw = t.message.orEmpty()
    return when {
        raw.contains("stock", true) -> "Stok tidak mencukupi."
        raw.contains("price", true) -> "Harga produk berubah. Muat ulang katalog."
        raw.contains("barcode", true) -> "Barcode sudah digunakan atau tidak valid."
        raw.contains("network", true) || raw.contains("timeout", true) -> "Koneksi ke server gagal."
        else -> "Operasi gagal. Periksa data dan coba lagi."
    }
}
private fun v9Icon(label: String) = when (label) {
    "Dashboard" -> Icons.Default.Home
    "Penjualan" -> Icons.Default.ShoppingCart
    "Pesanan" -> Icons.Default.Receipt
    "Produk" -> Icons.Default.Inventory2
    "Stok" -> Icons.Default.Warehouse
    "Pelanggan" -> Icons.Default.Person
    "Supplier" -> Icons.Default.LocalShipping
    "Pembelian" -> Icons.Default.ShoppingCart
    "Piutang" -> Icons.Default.AccountBalance
    "Laporan" -> Icons.Default.Assessment
    "Pembayaran" -> Icons.Default.Payments
    else -> Icons.Default.Settings
}

@Composable
fun OwnerPosAppV9() {
    var page by rememberSaveable { mutableStateOf("Dashboard") }
    var more by rememberSaveable { mutableStateOf(false) }
    val tablet = LocalConfiguration.current.screenWidthDp >= 600
    MaterialTheme(colorScheme = lightColorScheme(primary = v9Blue, background = v9Bg, surface = Color.White)) {
        Surface(Modifier.fillMaxSize(), color = v9Bg) {
            if (tablet) {
                Row(Modifier.fillMaxSize()) {
                    V9Sidebar(page) { page = it }
                    Box(Modifier.weight(1f).fillMaxSize()) { V9Screen(page) }
                }
            } else {
                Column(Modifier.fillMaxSize()) {
                    Box(Modifier.weight(1f).fillMaxWidth()) { V9Screen(page) }
                    V9BottomNav(page, { page = it }) { more = true }
                    if (more) V9More(page, { page = it; more = false }) { more = false }
                }
            }
        }
    }
}

@Composable private fun V9Sidebar(selected: String, onSelect: (String) -> Unit) {
    Surface(Modifier.width(238.dp).fillMaxHeight(), color = v9Navy) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(Modifier.padding(8.dp, 8.dp, 8.dp, 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.QrCode2, null, tint = Color.White, modifier = Modifier.size(36.dp))
                Column(Modifier.padding(start = 10.dp)) {
                    Text("POS QRIS", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                    Text("Owner workspace", color = Color(0xFFA9BAD0), fontSize = 11.sp)
                }
            }
            v9Menus.forEach { item ->
                val active = item == selected
                Surface(Modifier.fillMaxWidth().clickable { onSelect(item) }, RoundedCornerShape(12.dp), color = if (active) v9Blue else Color.Transparent) {
                    Row(Modifier.padding(horizontal = 11.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(v9Icon(item), null, Modifier.size(19.dp), tint = if (active) Color.White else Color(0xFFB7C6D9))
                        Text(item, Modifier.padding(start = 10.dp), color = if (active) Color.White else Color(0xFFE7EEF7), fontWeight = if (active) FontWeight.Bold else FontWeight.Medium)
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            Text("LIVE • Supabase", color = Color(0xFFA9BAD0), fontSize = 11.sp, modifier = Modifier.padding(8.dp))
        }
    }
}

@Composable private fun V9BottomNav(selected: String, onSelect: (String) -> Unit, onMore: () -> Unit) {
    val main = listOf("Dashboard", "Penjualan", "Produk", "Stok")
    Surface(Modifier.fillMaxWidth().navigationBarsPadding(), color = Color.White, shadowElevation = 12.dp) {
        Row(Modifier.padding(7.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            main.forEach { item ->
                NavigationBarItem(selected == item, onClick = { onSelect(item) }, icon = { Icon(v9Icon(item), null) }, label = { Text(item, fontSize = 10.sp) })
            }
            NavigationBarItem(selected !in main, onClick = onMore, icon = { Icon(Icons.Default.MoreHoriz, null) }, label = { Text("Lainnya", fontSize = 10.sp) })
        }
    }
}

@Composable private fun V9More(selected: String, onSelect: (String) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(18.dp)) {
            Text("Menu Owner", color = v9Navy, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
            v9Menus.drop(4).forEach { item ->
                Surface(Modifier.fillMaxWidth().clickable { onSelect(item) }, RoundedCornerShape(12.dp), color = if (item == selected) Color(0xFFEAF2FF) else Color.Transparent) {
                    Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(v9Icon(item), null, tint = v9Blue)
                        Text(item, Modifier.padding(start = 12.dp), color = v9Navy)
                    }
                }
            }
        }
    }
}

@Composable private fun V9Header(title: String, subtitle: String, loading: Boolean, refresh: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, color = v9Navy, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
            Text(subtitle, color = v9Muted, fontSize = 12.sp)
        }
        OutlinedButton(onClick = refresh, enabled = !loading, shape = RoundedCornerShape(11.dp)) {
            Icon(Icons.Default.Refresh, null, Modifier.size(17.dp)); Spacer(Modifier.width(5.dp)); Text(if (loading) "Memuat" else "Refresh")
        }
    }
}

@Composable private fun V9Card(modifier: Modifier = Modifier, title: String? = null, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier, RoundedCornerShape(17.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.padding(15.dp)) { if (title != null) Text(title, color = v9Navy, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(bottom = 8.dp)); content() }
    }
}
@Composable private fun V9Error(message: String) { Surface(color = Color(0xFFFFEEEE), shape = RoundedCornerShape(12.dp)) { Text(message, color = v9Danger, fontSize = 12.sp, modifier = Modifier.padding(12.dp)) } }
@Composable private fun V9Empty(message: String) { Text(message, color = v9Muted, fontSize = 12.sp, modifier = Modifier.padding(vertical = 18.dp)) }

@Composable private fun V9Screen(page: String) {
    when (page) {
        "Dashboard" -> V9Dashboard()
        "Penjualan" -> V9Sales()
        "Produk" -> V9Products()
        else -> V9SimpleList(page)
    }
}

@Composable private fun V9Dashboard() {
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }
    var business by remember { mutableStateOf<JsonObject?>(null) }
    var sales by remember { mutableStateOf<List<JsonObject>>(emptyList()) }
    val scope = rememberCoroutineScope()
    suspend fun load() {
        loading = true; error = ""
        runCatching {
            business = v9Client.from("businesses").select { filter { eq("code", "TOKO_MAJU_JAYA") } }.decodeList<JsonObject>().firstOrNull()
            sales = v9Client.from("sales").select().decodeList()
        }.onFailure { error = v9Safe(it) }
        loading = false
    }
    LaunchedEffect(Unit) { load() }
    val completed = sales.filter { v9str(it, "status").uppercase() == "COMPLETED" }
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        V9Header("Dashboard", "${v9str(business ?: buildJsonObject {}, "name")} • data Supabase", loading) { scope.launch { load() } }
        if (error.isNotBlank()) V9Error(error)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                V9Card(Modifier.weight(1f)) { Text("Omzet", color = v9Muted, fontSize = 11.sp); Text(v9money(completed.sumOf { v9long(it, "total_amount") }), color = v9Navy, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold) }
                V9Card(Modifier.weight(1f)) { Text("Transaksi", color = v9Muted, fontSize = 11.sp); Text(completed.size.toString(), color = v9Navy, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold) }
            } }
            item { V9Card(Modifier.fillMaxWidth(), "Transaksi terbaru") { sales.sortedByDescending { v9str(it, "sale_date") }.take(8).forEach { Text("${v9str(it, "sale_no")} • ${v9money(v9long(it, "total_amount"))}", color = v9Navy, modifier = Modifier.padding(vertical = 5.dp)) } } }
        }
    }
}

private data class V9CartState(val productId: String, val qty: Long)
private val v9CartSaver: Saver<List<V9CartState>, Any> = listSaver(save = { list -> list.flatMap { listOf(it.productId, it.qty) } }, restore = { flat -> flat.chunked(2).mapNotNull { if (it.size == 2) V9CartState(it[0] as String, it[1] as Long) else null } })

@Composable private fun V9Sales() {
    var products by remember { mutableStateOf<List<JsonObject>>(emptyList()) }
    var categories by remember { mutableStateOf<Map<String, JsonObject>>(emptyMap()) }
    var units by remember { mutableStateOf<Map<String, JsonObject>>(emptyMap()) }
    var prices by remember { mutableStateOf<List<JsonObject>>(emptyList()) }
    var search by rememberSaveable { mutableStateOf("") }
    var cart by rememberSaveable(saver = v9CartSaver) { mutableStateOf(emptyList<V9CartState>()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }
    var paying by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    suspend fun load() {
        loading = true; error = ""
        runCatching {
            val b = v9Client.from("businesses").select { filter { eq("code", "TOKO_MAJU_JAYA") } }.decodeList<JsonObject>().first()
            val bid = v9str(b, "id")
            products = v9Client.from("products").select { filter { eq("business_id", bid); eq("is_active", true) } }.decodeList()
            categories = v9Client.from("categories").select { filter { eq("business_id", bid); eq("is_active", true) } }.decodeList<JsonObject>().associateBy { v9str(it, "id") }
            units = v9Client.from("units").select { filter { eq("business_id", bid) } }.decodeList<JsonObject>().associateBy { v9str(it, "id") }
            val list = v9Client.from("price_lists").select { filter { eq("business_id", bid); eq("is_default", true); eq("is_active", true) } }.decodeList<JsonObject>().firstOrNull()
            prices = if (list == null) emptyList() else v9Client.from("product_prices").select { filter { eq("price_list_id", v9str(list, "id")) } }.decodeList()
        }.onFailure { error = v9Safe(it) }
        loading = false
    }
    LaunchedEffect(Unit) { load() }

    fun priceFor(product: JsonObject, qty: Long): Long = prices.filter { v9str(it, "product_id") == v9str(product, "id") && v9num(it, "min_qty") <= qty }.maxByOrNull { v9num(it, "min_qty") }?.let { v9long(it, "price") } ?: 0L
    fun matches(p: JsonObject): Boolean {
        if (search.isBlank()) return true
        val q = search.trim()
        val category = categories[v9str(p, "category_id")]
        return listOf(v9str(p, "name"), v9str(p, "short_name"), v9str(p, "sku"), v9str(p, "barcode"), v9str(category ?: buildJsonObject {}, "name"), v9str(category ?: buildJsonObject {}, "code")).any { it.contains(q, true) }
    }
    val shown = products.filter(::matches)
    val cartRows = cart.mapNotNull { state -> products.firstOrNull { v9str(it, "id") == state.productId }?.let { it to state } }
    val total = cartRows.sumOf { (p, state) -> priceFor(p, state.qty) * state.qty }

    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        V9Header("Penjualan", "Cari nama, SKU, barcode, atau kategori", loading) { scope.launch { load() } }
        OutlinedTextField(search, { search = it }, Modifier.fillMaxWidth(), placeholder = { Text("Cari produk / SKU / barcode / kategori") }, singleLine = true, leadingIcon = { Icon(Icons.Default.Search, null) })
        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            LazyVerticalGrid(columns = GridCells.Adaptive(165.dp), modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(shown) { p ->
                    val state = cart.firstOrNull { it.productId == v9str(p, "id") }
                    val qty = state?.qty ?: 0L
                    val unit = units[v9str(p, "base_unit_id")]
                    V9Card(Modifier.fillMaxWidth().clickable {
                        val id = v9str(p, "id")
                        cart = if (state == null) cart + V9CartState(id, 1) else cart.map { if (it.productId == id) it.copy(qty = it.qty + 1) else it }
                    }) {
                        Text(v9str(p, "name"), color = v9Navy, fontWeight = FontWeight.Bold, maxLines = 2)
                        Text("${v9str(p, "sku")} • ${v9str(unit ?: buildJsonObject {}, "name")}", color = v9Muted, fontSize = 10.sp)
                        Text(v9money(priceFor(p, maxOf(1L, qty))), color = v9Blue, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 7.dp))
                        if (qty > 0) Text("Di keranjang: $qty", color = v9Blue, fontSize = 10.sp)
                    }
                }
                if (!loading && shown.isEmpty()) item { V9Empty(if (search.isBlank()) "Belum ada produk aktif." else "Produk tidak ditemukan. Katalog tetap tersedia saat pencarian dikosongkan.") }
            }
            V9Card(Modifier.widthIn(min = 270.dp, max = 360.dp).fillMaxHeight(), "Keranjang") {
                if (cartRows.isEmpty()) V9Empty("Keranjang kosong.")
                cartRows.forEach { (p, state) ->
                    val unitPrice = priceFor(p, state.qty)
                    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(v9str(p, "name"), color = v9Navy, fontWeight = FontWeight.Bold, maxLines = 1)
                            Text("${state.qty} × ${v9money(unitPrice)}", color = v9Muted, fontSize = 10.sp)
                        }
                        IconButton(onClick = { cart = if (state.qty <= 1) cart.filterNot { it.productId == state.productId } else cart.map { if (it.productId == state.productId) it.copy(qty = it.qty - 1) else it } }) { Icon(Icons.Default.Remove, null) }
                        IconButton(onClick = { cart = cart.filterNot { it.productId == state.productId } }) { Icon(Icons.Default.Delete, null) }
                    }
                }
                Spacer(Modifier.weight(1f))
                Text("Total", color = v9Muted, fontSize = 11.sp)
                Text(v9money(total), color = v9Navy, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                Button(enabled = cartRows.isNotEmpty() && !paying, onClick = { paying = true }, modifier = Modifier.fillMaxWidth()) { Text("Bayar") }
            }
        }
    }
    if (paying) V9PaymentDialog(total, cartRows, onDone = { paying = false; cart = emptyList(); scope.launch { load() } }, onCancel = { paying = false })
}

@Composable private fun V9PaymentDialog(total: Long, cart: List<Pair<JsonObject, V9CartState>>, onDone: () -> Unit, onCancel: () -> Unit) {
    var receivedText by rememberSaveable(total) { mutableStateOf(total.toString()) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    val received = receivedText.toLongOrNull() ?: 0L
    val change = received - total
    val scope = rememberCoroutineScope()
    AlertDialog(onDismissRequest = { if (!saving) onCancel() }, title = { Text("Pembayaran") }, text = {
        Column(Modifier.fillMaxWidth().heightIn(max = 420.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            V9Card(Modifier.fillMaxWidth()) {
                Text("Total tagihan", color = v9Muted, fontSize = 11.sp)
                Text(v9money(total), color = v9Navy, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
            }
            Text("Item: ${cart.sumOf { it.second.qty }}", color = v9Muted, fontSize = 12.sp)
            OutlinedTextField(receivedText, { receivedText = it.filter(Char::isDigit) }, Modifier.fillMaxWidth(), label = { Text("Uang diterima") }, prefix = { Text("Rp ") }, singleLine = true)
            V9Card(Modifier.fillMaxWidth()) {
                Text(if (change >= 0) "Kembalian" else "Kurang bayar", color = v9Muted, fontSize = 11.sp)
                Text(v9money(kotlin.math.abs(change)), color = if (change >= 0) v9Blue else v9Danger, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
            }
            Text("Metode pembayaran: CASH", color = v9Muted, fontSize = 12.sp)
            if (error.isNotBlank()) V9Error(error)
        }
    }, confirmButton = {
        Button(enabled = !saving && received >= total, onClick = {
            scope.launch {
                saving = true; error = ""
                runCatching {
                    val b = v9Client.from("businesses").select { filter { eq("code", "TOKO_MAJU_JAYA") } }.decodeList<JsonObject>().first()
                    val branch = v9Client.from("branches").select { filter { eq("business_id", v9str(b, "id")); eq("is_active", true) } }.decodeList<JsonObject>().first()
                    val loc = v9Client.from("locations").select { filter { eq("branch_id", v9str(branch, "id")); eq("is_active", true) } }.decodeList<JsonObject>().first()
                    val method = v9Client.from("payment_methods").select { filter { eq("business_id", v9str(b, "id")); eq("code", "CASH"); eq("is_active", true) } }.decodeList<JsonObject>().first()
                    v9Client.postgrest.rpc("checkout_sale_multi_payment", buildJsonObject {
                        put("p_branch_id", v9str(branch, "id")); put("p_location_id", v9str(loc, "id")); put("p_customer_id", JsonNull); put("p_idempotency_key", UUID.randomUUID().toString())
                        put("p_items", buildJsonArray { cart.forEach { (p, state) -> add(buildJsonObject { put("product_id", v9str(p, "id")); put("unit_id", v9str(p, "base_unit_id")); put("sku", v9str(p, "sku")); put("name", v9str(p, "name")); put("qty", state.qty); put("conversion_to_base", 1); put("unit_price", 0); put("hpp_unit", 0) }) } })
                        put("p_payments", buildJsonArray { add(buildJsonObject { put("payment_method_id", v9str(method, "id")); put("amount", total); put("provider", "CASH") }) })
                    })
                }.onSuccess { onDone() }.onFailure { error = v9Safe(it) }
                saving = false
            }
        }) { Text(if (saving) "Memproses" else "Konfirmasi") }
    }, dismissButton = { TextButton(enabled = !saving, onClick = onCancel) { Text("Batal") } })
}

@Composable private fun V9Products() {
    var products by remember { mutableStateOf<List<JsonObject>>(emptyList()) }
    var categories by remember { mutableStateOf<List<JsonObject>>(emptyList()) }
    var units by remember { mutableStateOf<List<JsonObject>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf<JsonObject?>(null) }
    var showForm by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    suspend fun load() {
        loading = true; error = ""
        runCatching {
            val b = v9Client.from("businesses").select { filter { eq("code", "TOKO_MAJU_JAYA") } }.decodeList<JsonObject>().first()
            val bid = v9str(b, "id")
            products = v9Client.from("products").select { filter { eq("business_id", bid) } }.decodeList()
            categories = v9Client.from("categories").select { filter { eq("business_id", bid); eq("is_active", true) } }.decodeList()
            units = v9Client.from("units").select { filter { eq("business_id", bid) } }.decodeList()
        }.onFailure { error = v9Safe(it) }
        loading = false
    }
    LaunchedEffect(Unit) { load() }
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { Text("Produk", color = v9Navy, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold); Text("Form bisnis, bukan form SQL", color = v9Muted, fontSize = 12.sp) }
            Button(onClick = { editing = null; showForm = true }) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(5.dp)); Text("Tambah produk") }
        }
        if (error.isNotBlank()) V9Error(error)
        if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(products) { p ->
                val category = categories.firstOrNull { v9str(it, "id") == v9str(p, "category_id") }
                val unit = units.firstOrNull { v9str(it, "id") == v9str(p, "base_unit_id") }
                V9Card(Modifier.fillMaxWidth().clickable { editing = p; showForm = true }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(v9str(p, "name"), color = v9Navy, fontWeight = FontWeight.Bold)
                            Text("SKU ${v9str(p, "sku")} • Barcode ${v9str(p, "barcode").ifBlank { "belum diisi" }}", color = v9Muted, fontSize = 11.sp)
                            Text("${v9str(category ?: buildJsonObject {}, "name").ifBlank { "Tanpa kategori" }} • ${v9str(unit ?: buildJsonObject {}, "name").ifBlank { "Tanpa satuan" }}", color = v9Muted, fontSize = 11.sp)
                        }
                        Icon(Icons.Default.Edit, null, tint = v9Blue)
                    }
                }
            }
            if (!loading && products.isEmpty()) item { V9Empty("Belum ada produk.") }
        }
    }
    if (showForm) V9ProductForm(editing, categories, units, { showForm = false; scope.launch { load() } }, { showForm = false })
}

@Composable private fun V9Dropdown(label: String, selected: String, options: List<Pair<String, String>>, onSelected: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp), shape = RoundedCornerShape(10.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text(if (selected.isBlank()) "Pilih $label" else selected, Modifier.weight(1f), color = if (selected.isBlank()) v9Muted else v9Navy); Icon(Icons.Default.ArrowDropDown, null) }
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { (id, name) -> DropdownMenuItem(text = { Text(name) }, onClick = { onSelected(id); open = false }) }
        }
    }
}

@Composable private fun V9ProductForm(existing: JsonObject?, categories: List<JsonObject>, units: List<JsonObject>, onSaved: () -> Unit, onCancel: () -> Unit) {
    val values = remember(existing) { mutableStateMapOf<String, String>().also { m -> listOf("name", "sku", "barcode", "description", "category_id", "base_unit_id", "min_stock", "current_cost", "image_path").forEach { m[it] = v9str(existing ?: buildJsonObject {}, it) } } }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var uploading by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { imageUri = it }
    val scope = rememberCoroutineScope()
    val categoryName = categories.firstOrNull { v9str(it, "id") == values["category_id"] }?.let { v9str(it, "name") }.orEmpty()
    val unitName = units.firstOrNull { v9str(it, "id") == values["base_unit_id"] }?.let { v9str(it, "name") }.orEmpty()
    AlertDialog(onDismissRequest = { if (!saving) onCancel() }, title = { Text(if (existing == null) "Tambah produk" else "Edit produk") }, text = {
        Column(Modifier.fillMaxWidth().heightIn(max = 560.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            if (error.isNotBlank()) V9Error(error)
            OutlinedTextField(values["name"].orEmpty(), { values["name"] = it }, Modifier.fillMaxWidth(), label = { Text("Nama produk") }, singleLine = true)
            OutlinedTextField(values["sku"].orEmpty(), { values["sku"] = it }, Modifier.fillMaxWidth(), label = { Text("SKU") }, singleLine = true)
            OutlinedTextField(values["barcode"].orEmpty(), { values["barcode"] = it }, Modifier.fillMaxWidth(), label = { Text("Barcode") }, singleLine = true)
            V9Dropdown("kategori", categoryName, categories.map { v9str(it, "id") to v9str(it, "name") }) { values["category_id"] = it }
            V9Dropdown("satuan", unitName, units.map { v9str(it, "id") to "${v9str(it, "name")} (${v9str(it, "symbol")})" }) { values["base_unit_id"] = it }
            OutlinedTextField(values["min_stock"].orEmpty(), { values["min_stock"] = it.filter(Char::isDigit) }, Modifier.fillMaxWidth(), label = { Text("Minimum stok") }, singleLine = true)
            OutlinedTextField(values["current_cost"].orEmpty(), { values["current_cost"] = it.filter(Char::isDigit) }, Modifier.fillMaxWidth(), label = { Text("Harga modal") }, prefix = { Text("Rp ") }, singleLine = true)
            OutlinedTextField(values["description"].orEmpty(), { values["description"] = it }, Modifier.fillMaxWidth(), label = { Text("Deskripsi") }, minLines = 2)
            OutlinedButton(onClick = { picker.launch("image/*") }, enabled = !saving && !uploading, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Image, null); Spacer(Modifier.width(6.dp)); Text(if (imageUri == null) "Pilih foto produk" else "Foto dipilih") }
        }
    }, confirmButton = {
        Button(enabled = !saving && !uploading, onClick = {
            scope.launch {
                saving = true; error = ""
                runCatching {
                    var imagePath = values["image_path"].orEmpty()
                    imageUri?.let { uri ->
                        uploading = true
                        val bytes = withContext(Dispatchers.IO) { context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: error("Foto tidak dapat dibaca.") }
                        val ext = context.contentResolver.getType(uri)?.substringAfterLast('/')?.lowercase().orEmpty().ifBlank { "jpg" }
                        imagePath = "${demoBusinessIdV9()}/${UUID.randomUUID()}.$ext"
                        v9Client.storage.from("product-images").upload(imagePath, bytes) { upsert = false }
                        uploading = false
                    }
                    val payload = buildJsonObject {
                        put("name", values["name"].orEmpty()); put("sku", values["sku"].orEmpty()); put("barcode", values["barcode"].orEmpty())
                        put("description", values["description"].orEmpty()); put("category_id", values["category_id"].orEmpty().ifBlank { null }); put("base_unit_id", values["base_unit_id"].orEmpty().ifBlank { null })
                        put("min_stock", values["min_stock"].orEmpty().toDoubleOrNull() ?: 0.0); put("current_cost", values["current_cost"].orEmpty().toLongOrNull() ?: 0L); put("image_path", imagePath)
                        if (existing == null) put("business_id", demoBusinessIdV9())
                    }
                    if (existing == null) v9Client.from("products").insert(payload) else v9Client.from("products").update(payload) { filter { eq("id", v9str(existing, "id")) } }
                }.onSuccess { onSaved() }.onFailure { error = v9Safe(it) }
                uploading = false; saving = false
            }
        }) { Text(if (saving || uploading) "Menyimpan" else "Simpan") }
    }, dismissButton = { TextButton(enabled = !saving, onClick = onCancel) { Text("Batal") } })
}

private suspend fun demoBusinessIdV9(): String = v9Client.from("businesses").select { filter { eq("code", "TOKO_MAJU_JAYA") } }.decodeList<JsonObject>().first().let { v9str(it, "id") }

@Composable private fun V9SimpleList(page: String) {
    val table = when (page) { "Pesanan" -> "sales"; "Pembelian" -> "purchase_orders"; "Piutang" -> "receivables"; "Pembayaran" -> "payments"; "Stok" -> "stock_balances"; "Pelanggan" -> "customers"; "Supplier" -> "suppliers"; else -> "businesses" }
    var rows by remember { mutableStateOf<List<JsonObject>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    suspend fun load() { loading = true; error = ""; runCatching { rows = v9Client.from(table).select().decodeList() }.onFailure { error = v9Safe(it) }; loading = false }
    LaunchedEffect(table) { load() }
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        V9Header(page, "Data Supabase", loading) { scope.launch { load() } }
        Spacer(Modifier.height(12.dp)); if (error.isNotBlank()) V9Error(error)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) { if (!loading && rows.isEmpty()) item { V9Empty("Belum ada data.") }; items(rows) { row -> V9Card(Modifier.fillMaxWidth()) { Text(v9str(row, "name").ifBlank { v9str(row, "sale_no").ifBlank { v9str(row, "id") } }, color = v9Navy, fontWeight = FontWeight.Bold); Text(row.entries.take(3).joinToString(" • ") { "${it.key}: ${it.value.toString().take(40)}" }, color = v9Muted, fontSize = 10.sp) } } }
    }
}
