@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.henky.posqris

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch
import kotlinx.serialization.json.*
import java.util.UUID

private val v10Client get() = SupabaseClientProvider.client
private val v10Blue = Color(0xFF2563EB)
private val v10Navy = Color(0xFF071A33)
private val v10Bg = Color(0xFFF4F7FB)
private val v10Muted = Color(0xFF617089)
private val v10Danger = Color(0xFFC53B3B)
private val v10Menus = listOf("Dashboard", "Penjualan", "Pesanan", "Produk", "Stok", "Pelanggan", "Supplier", "Pembelian", "Piutang", "Laporan", "Pembayaran", "Pengaturan")

private fun v10s(o: JsonObject, key: String) = o[key]?.jsonPrimitive?.contentOrNull.orEmpty()
private fun v10l(o: JsonObject, key: String) = o[key]?.jsonPrimitive?.longOrNull ?: 0L
private fun v10d(o: JsonObject, key: String) = o[key]?.jsonPrimitive?.doubleOrNull ?: 0.0
private fun v10money(v: Long) = "Rp " + v.toString().reversed().chunked(3).joinToString(".").reversed()
private fun v10Error(t: Throwable) = when {
    t.message.orEmpty().contains("stock", true) -> "Stok tidak mencukupi."
    t.message.orEmpty().contains("price", true) -> "Harga produk berubah. Muat ulang katalog."
    t.message.orEmpty().contains("barcode", true) -> "Barcode sudah digunakan atau tidak valid."
    else -> "Operasi gagal. Periksa data dan koneksi."
}

private data class V10Cart(val productId: String, val qty: Long)
private val v10CartSaver: Saver<List<V10Cart>, Any> = listSaver(
    save = { list -> list.flatMap { listOf(it.productId, it.qty) } },
    restore = { saved ->
        saved.chunked(2).mapNotNull { pair ->
            val id = pair.getOrNull(0) as? String
            val qty = (pair.getOrNull(1) as? Number)?.toLong()
            if (id != null && qty != null) V10Cart(id, qty) else null
        }
    }
)

@Composable
fun OwnerPosAppV10() {
    var page by rememberSaveable { mutableStateOf("Penjualan") }
    val tablet = LocalConfiguration.current.screenWidthDp >= 600
    MaterialTheme(colorScheme = lightColorScheme(primary = v10Blue, background = v10Bg, surface = Color.White)) {
        Surface(Modifier.fillMaxSize(), color = v10Bg) {
            if (tablet) Row(Modifier.fillMaxSize()) {
                V10Sidebar(page) { page = it }
                Box(Modifier.weight(1f).fillMaxSize()) { V10Screen(page) }
            } else Column(Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f).fillMaxWidth()) { V10Screen(page) }
                V10Bottom(page) { page = it }
            }
        }
    }
}

@Composable private fun V10Sidebar(selected: String, onSelect: (String) -> Unit) {
    Surface(Modifier.width(238.dp).fillMaxHeight(), color = v10Navy) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("POS QRIS", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(10.dp))
            v10Menus.forEach { item ->
                Surface(Modifier.fillMaxWidth().clickable { onSelect(item) }, shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp), color = if (item == selected) v10Blue else Color.Transparent) {
                    Row(Modifier.padding(11.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(item, color = if (item == selected) Color.White else Color(0xFFE7EEF7), fontWeight = if (item == selected) FontWeight.Bold else FontWeight.Medium)
                    }
                }
            }
        }
    }
}

@Composable private fun V10Bottom(selected: String, onSelect: (String) -> Unit) {
    NavigationBar {
        listOf("Dashboard", "Penjualan", "Produk", "Stok").forEach { item ->
            NavigationBarItem(selected == item, { onSelect(item) }, icon = { Icon(Icons.Default.Circle, null) }, label = { Text(item, fontSize = 10.sp) })
        }
    }
}

@Composable private fun V10Screen(page: String) {
    when (page) {
        "Penjualan" -> V10Sales()
        "Produk" -> V10Products()
        else -> V10Placeholder(page)
    }
}

@Composable private fun V10Placeholder(page: String) {
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Text(page, color = v10Navy, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
        Text("Modul ini tetap tersedia. Fokus perbaikan saat ini ada di Penjualan dan Produk.", color = v10Muted, modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable private fun V10Sales() {
    var products by remember { mutableStateOf<List<JsonObject>>(emptyList()) }
    var categories by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var units by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var prices by remember { mutableStateOf<List<JsonObject>>(emptyList()) }
    var search by rememberSaveable { mutableStateOf("") }
    var cart by rememberSaveable(saver = v10CartSaver) { mutableStateOf(emptyList<V10Cart>()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }
    var paying by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    suspend fun load() {
        loading = true; error = ""
        runCatching {
            val business = v10Client.from("businesses").select { filter { eq("code", "TOKO_MAJU_JAYA") } }.decodeList<JsonObject>().first()
            val bid = v10s(business, "id")
            products = v10Client.from("products").select { filter { eq("business_id", bid); eq("is_active", true) } }.decodeList()
            categories = v10Client.from("categories").select { filter { eq("business_id", bid); eq("is_active", true) } }.decodeList<JsonObject>().associate { v10s(it, "id") to v10s(it, "name") }
            units = v10Client.from("units").select { filter { eq("business_id", bid) } }.decodeList<JsonObject>().associate { v10s(it, "id") to v10s(it, "name") }
            val list = v10Client.from("price_lists").select { filter { eq("business_id", bid); eq("is_default", true); eq("is_active", true) } }.decodeList<JsonObject>().firstOrNull()
            prices = if (list == null) emptyList() else v10Client.from("product_prices").select { filter { eq("price_list_id", v10s(list, "id")) } }.decodeList()
        }.onFailure { error = v10Error(it) }
        loading = false
    }
    LaunchedEffect(Unit) { load() }

    fun priceFor(p: JsonObject, qty: Long): Long = prices
        .filter { v10s(it, "product_id") == v10s(p, "id") && v10d(it, "min_qty") <= qty }
        .maxByOrNull { v10d(it, "min_qty") }
        ?.let { v10l(it, "price") } ?: 0L

    val q = search.trim()
    val shown = products.filter { p ->
        if (q.isBlank()) true else {
            val cat = categories[v10s(p, "category_id")] ?: ""
            listOf(v10s(p, "name"), v10s(p, "short_name"), v10s(p, "sku"), v10s(p, "barcode"), cat).any { it.contains(q, true) }
        }
    }
    val rows = cart.mapNotNull { c -> products.firstOrNull { v10s(it, "id") == c.productId }?.let { it to c } }
    val total = rows.sumOf { (p, c) -> priceFor(p, c.qty) * c.qty }

    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Penjualan", color = v10Navy, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
                Text("Nama, SKU, barcode, atau kategori", color = v10Muted, fontSize = 12.sp)
            }
            OutlinedButton(onClick = { scope.launch { load() } }, enabled = !loading) { Text("Refresh") }
        }
        OutlinedTextField(search, { search = it }, Modifier.fillMaxWidth(), label = { Text("Cari produk") }, placeholder = { Text("Nasi Goreng / FD001 / barcode / Makanan") }, singleLine = true)
        if (error.isNotBlank()) Text(error, color = v10Danger)
        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            LazyVerticalGrid(GridCells.Adaptive(165.dp), Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(shown) { p ->
                    val id = v10s(p, "id")
                    val cartItem = cart.firstOrNull { it.productId == id }
                    val qty = cartItem?.qty ?: 0L
                    Card(Modifier.fillMaxWidth().clickable { cart = if (cartItem == null) cart + V10Cart(id, 1) else cart.map { if (it.productId == id) it.copy(qty = it.qty + 1) else it } }) {
                        Column(Modifier.padding(14.dp)) {
                            Text(v10s(p, "name"), color = v10Navy, fontWeight = FontWeight.Bold, maxLines = 2)
                            Text("SKU ${v10s(p, "sku")}", color = v10Muted, fontSize = 10.sp)
                            Text(categories[v10s(p, "category_id")] ?: "Tanpa kategori", color = v10Muted, fontSize = 10.sp)
                            Text(v10money(priceFor(p, maxOf(1, qty))), color = v10Blue, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 7.dp))
                            if (qty > 0) Text("Di keranjang: $qty", color = v10Blue, fontSize = 10.sp)
                        }
                    }
                }
                if (!loading && shown.isEmpty()) item { Text(if (q.isBlank()) "Belum ada produk aktif." else "Produk tidak ditemukan.", color = v10Muted, modifier = Modifier.padding(20.dp)) }
            }
            Card(Modifier.widthIn(min = 270.dp, max = 360.dp).fillMaxHeight()) {
                Column(Modifier.padding(14.dp).fillMaxHeight()) {
                    Text("Keranjang", color = v10Navy, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                    if (rows.isEmpty()) Text("Keranjang kosong.", color = v10Muted, modifier = Modifier.padding(vertical = 14.dp))
                    rows.forEach { (p, c) ->
                        val unitPrice = priceFor(p, c.qty)
                        Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) { Text(v10s(p, "name"), color = v10Navy, fontWeight = FontWeight.Bold); Text("${c.qty} × ${v10money(unitPrice)}", color = v10Muted, fontSize = 10.sp) }
                            IconButton({ cart = if (c.qty <= 1) cart.filterNot { it.productId == c.productId } else cart.map { if (it.productId == c.productId) it.copy(qty = it.qty - 1) else it } }) { Icon(Icons.Default.Remove, null) }
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    Text("Total", color = v10Muted, fontSize = 11.sp)
                    Text(v10money(total), color = v10Navy, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                    Button(enabled = rows.isNotEmpty() && !paying, onClick = { paying = true }, Modifier.fillMaxWidth()) { Text("Bayar") }
                }
            }
        }
    }
    if (paying) V10Payment(total, rows, { paying = false; cart = emptyList(); scope.launch { load() } }, { paying = false })
}

@Composable private fun V10Payment(total: Long, cart: List<Pair<JsonObject, V10Cart>>, onDone: () -> Unit, onCancel: () -> Unit) {
    var receivedText by rememberSaveable(total) { mutableStateOf(total.toString()) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    val received = receivedText.toLongOrNull() ?: 0L
    val change = received - total
    val scope = rememberCoroutineScope()
    AlertDialog(onDismissRequest = { if (!saving) onCancel() }, title = { Text("Pembayaran") }, text = {
        Column(Modifier.fillMaxWidth().heightIn(max = 430.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Total ${v10money(total)}", color = v10Navy, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
            Text("Item ${cart.sumOf { it.second.qty }}", color = v10Muted)
            OutlinedTextField(receivedText, { receivedText = it.filter(Char::isDigit) }, Modifier.fillMaxWidth(), label = { Text("Uang diterima") }, prefix = { Text("Rp ") }, singleLine = true)
            Text(if (change >= 0) "Kembalian ${v10money(change)}" else "Kurang bayar ${v10money(-change)}", color = if (change >= 0) v10Blue else v10Danger, fontWeight = FontWeight.Bold)
            if (error.isNotBlank()) Text(error, color = v10Danger)
        }
    }, confirmButton = {
        Button(enabled = !saving && received >= total, onClick = {
            scope.launch {
                saving = true; error = ""
                runCatching {
                    val b = v10Client.from("businesses").select { filter { eq("code", "TOKO_MAJU_JAYA") } }.decodeList<JsonObject>().first()
                    val bid = v10s(b, "id")
                    val branch = v10Client.from("branches").select { filter { eq("business_id", bid); eq("is_active", true) } }.decodeList<JsonObject>().first()
                    val loc = v10Client.from("locations").select { filter { eq("branch_id", v10s(branch, "id")); eq("is_active", true) } }.decodeList<JsonObject>().first()
                    val method = v10Client.from("payment_methods").select { filter { eq("business_id", bid); eq("code", "CASH"); eq("is_active", true) } }.decodeList<JsonObject>().first()
                    v10Client.postgrest.rpc("checkout_sale_multi_payment", buildJsonObject {
                        put("p_branch_id", v10s(branch, "id")); put("p_location_id", v10s(loc, "id")); put("p_customer_id", JsonNull); put("p_idempotency_key", UUID.randomUUID().toString())
                        put("p_items", buildJsonArray { cart.forEach { (p, c) ->
                            val unitPrice = v10PriceForCheckout(p, c.qty)
                            add(buildJsonObject { put("product_id", v10s(p, "id")); put("unit_id", v10s(p, "base_unit_id")); put("sku", v10s(p, "sku")); put("name", v10s(p, "name")); put("qty", c.qty); put("conversion_to_base", 1); put("unit_price", unitPrice); put("hpp_unit", 0) })
                        } })
                        put("p_payments", buildJsonArray { add(buildJsonObject { put("payment_method_id", v10s(method, "id")); put("amount", total); put("provider", "CASH") }) })
                    })
                }.onSuccess { onDone() }.onFailure { error = v10Error(it) }
                saving = false
            }
        }) { Text(if (saving) "Memproses" else "Konfirmasi") }
    }, dismissButton = { TextButton(enabled = !saving, onClick = onCancel) { Text("Batal") } })
}

private fun v10PriceForCheckout(product: JsonObject, qty: Long): Long {
    return v10ClientPriceCache[product to qty] ?: 0L
}
private val v10ClientPriceCache = mutableMapOf<Pair<JsonObject, Long>, Long>()

@Composable private fun V10Products() {
    var products by remember { mutableStateOf<List<JsonObject>>(emptyList()) }
    var categories by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var units by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    suspend fun load() {
        loading = true; error = ""
        runCatching {
            val b = v10Client.from("businesses").select { filter { eq("code", "TOKO_MAJU_JAYA") } }.decodeList<JsonObject>().first()
            val bid = v10s(b, "id")
            products = v10Client.from("products").select { filter { eq("business_id", bid) } }.decodeList()
            categories = v10Client.from("categories").select { filter { eq("business_id", bid); eq("is_active", true) } }.decodeList<JsonObject>().associate { v10s(it, "id") to v10s(it, "name") }
            units = v10Client.from("units").select { filter { eq("business_id", bid) } }.decodeList<JsonObject>().associate { v10s(it, "id") to v10s(it, "name") }
        }.onFailure { error = v10Error(it) }
        loading = false
    }
    LaunchedEffect(Unit) { load() }
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { Text("Produk", color = v10Navy, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold); Text("Kategori dan satuan ditampilkan dengan nama", color = v10Muted, fontSize = 12.sp) }
            OutlinedButton(onClick = { scope.launch { load() } }) { Text("Refresh") }
        }
        if (error.isNotBlank()) Text(error, color = v10Danger)
        if (loading) LinearProgressIndicator(Modifier.fillMaxWidth().padding(vertical = 8.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(products) { p ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp)) {
                        Text(v10s(p, "name"), color = v10Navy, fontWeight = FontWeight.Bold)
                        Text("SKU ${v10s(p, "sku")} • Barcode ${v10s(p, "barcode").ifBlank { "belum diisi" }}", color = v10Muted, fontSize = 11.sp)
                        Text("${categories[v10s(p, "category_id")] ?: "Tanpa kategori"} • ${units[v10s(p, "base_unit_id")] ?: "Tanpa satuan"}", color = v10Muted, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}
