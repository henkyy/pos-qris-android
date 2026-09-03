@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.henky.posqris

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
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

@Composable
fun OwnerPosAppV10() {
    var page by rememberSaveable { mutableStateOf("Penjualan") }
    val tablet = LocalConfiguration.current.screenWidthDp >= 600
    MaterialTheme(colorScheme = lightColorScheme(primary = v10Blue, background = v10Bg, surface = Color.White)) {
        Surface(Modifier.fillMaxSize(), color = v10Bg) {
            if (tablet) {
                Row(Modifier.fillMaxSize()) {
                    V10Sidebar(page) { page = it }
                    Box(Modifier.weight(1f).fillMaxSize()) { V10Screen(page) }
                }
            } else {
                Column(Modifier.fillMaxSize()) {
                    Box(Modifier.weight(1f).fillMaxWidth()) { V10Screen(page) }
                    NavigationBar {
                        listOf("Dashboard", "Penjualan", "Produk", "Stok").forEach { item ->
                            NavigationBarItem(
                                selected = page == item,
                                onClick = { page = item },
                                icon = { Icon(Icons.Default.Circle, contentDescription = null) },
                                label = { Text(item, fontSize = 10.sp) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun V10Sidebar(selected: String, onSelect: (String) -> Unit) {
    Surface(Modifier.width(238.dp).fillMaxHeight(), color = v10Navy) {
        LazyColumn(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            item { Text("POS QRIS", color = Color.White, fontSize = 20.sp, modifier = Modifier.padding(10.dp)) }
            items(v10Menus) { item ->
                NavigationDrawerItem(
                    label = { Text(item) },
                    selected = selected == item,
                    onClick = { onSelect(item) },
                    icon = { Icon(Icons.Default.Circle, contentDescription = null) }
                )
            }
        }
    }
}

@Composable
private fun V10Screen(page: String) {
    when (page) {
        "Penjualan" -> V10Sales()
        "Produk" -> V10Products()
        else -> V10Placeholder(page)
    }
}

@Composable
private fun V10Placeholder(page: String) {
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Text(page, color = v10Navy, fontSize = 26.sp)
        Text("Modul tersedia. Fokus perubahan saat ini ada di Penjualan dan Produk.", color = v10Muted, modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
private fun V10Sales() {
    var products by remember { mutableStateOf<List<JsonObject>>(emptyList()) }
    var categories by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var prices by remember { mutableStateOf<List<JsonObject>>(emptyList()) }
    var search by rememberSaveable { mutableStateOf("") }
    var cart by remember { mutableStateOf(emptyList<V10Cart>()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }
    var paying by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    suspend fun load() {
        loading = true
        error = ""
        runCatching {
            val business = v10Client.from("businesses").select { filter { eq("code", "TOKO_MAJU_JAYA") } }.decodeList<JsonObject>().first()
            val businessId = v10s(business, "id")
            products = v10Client.from("products").select { filter { eq("business_id", businessId); eq("is_active", true) } }.decodeList()
            categories = v10Client.from("categories").select { filter { eq("business_id", businessId); eq("is_active", true) } }.decodeList<JsonObject>().associate { v10s(it, "id") to v10s(it, "name") }
            val priceList = v10Client.from("price_lists").select { filter { eq("business_id", businessId); eq("is_default", true); eq("is_active", true) } }.decodeList<JsonObject>().firstOrNull()
            prices = if (priceList == null) emptyList() else v10Client.from("product_prices").select { filter { eq("price_list_id", v10s(priceList, "id")) } }.decodeList()
        }.onFailure { error = v10Error(it) }
        loading = false
    }

    LaunchedEffect(Unit) { load() }

    fun priceFor(product: JsonObject, qty: Long): Long = prices
        .filter { v10s(it, "product_id") == v10s(product, "id") && v10d(it, "min_qty") <= qty }
        .maxByOrNull { v10d(it, "min_qty") }
        ?.let { v10l(it, "price") } ?: 0L

    val q = search.trim()
    val shown = products.filter { product ->
        q.isBlank() || listOf(
            v10s(product, "name"),
            v10s(product, "short_name"),
            v10s(product, "sku"),
            v10s(product, "barcode"),
            categories[v10s(product, "category_id")] ?: ""
        ).any { it.contains(q, true) }
    }
    val rows = cart.mapNotNull { c -> products.firstOrNull { v10s(it, "id") == c.productId }?.let { it to c } }
    val total = rows.sumOf { (product, item) -> priceFor(product, item.qty) * item.qty }

    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Penjualan", color = v10Navy, fontSize = 26.sp)
                Text("Cari berdasarkan nama, SKU, barcode, atau kategori", color = v10Muted, fontSize = 12.sp)
            }
            OutlinedButton(onClick = { scope.launch { load() } }, enabled = !loading) { Text("Refresh") }
        }
        OutlinedTextField(
            value = search,
            onValueChange = { search = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Cari produk") },
            placeholder = { Text("Nasi Goreng / FD001 / barcode / Makanan") },
            singleLine = true
        )
        if (error.isNotBlank()) Text(error, color = v10Danger)

        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Card(Modifier.weight(1f).fillMaxHeight()) {
                LazyColumn(Modifier.fillMaxSize().padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(shown) { product ->
                        val id = v10s(product, "id")
                        val item = cart.firstOrNull { it.productId == id }
                        val qty = item?.qty ?: 0L
                        Card(Modifier.fillMaxWidth().clickable {
                            cart = if (item == null) {
                                cart + V10Cart(id, 1)
                            } else {
                                cart.map { if (it.productId == id) it.copy(qty = it.qty + 1) else it }
                            }
                        }) {
                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(v10s(product, "name"), color = v10Navy, maxLines = 2)
                                Text("SKU ${v10s(product, "sku")}", color = v10Muted, fontSize = 10.sp)
                                Text(categories[v10s(product, "category_id")] ?: "Tanpa kategori", color = v10Muted, fontSize = 10.sp)
                                Text(v10money(priceFor(product, maxOf(1L, qty))), color = v10Blue)
                                if (qty > 0) Text("Di keranjang: $qty", color = v10Blue, fontSize = 10.sp)
                            }
                        }
                    }
                    if (!loading && shown.isEmpty()) {
                        item { Text(if (q.isBlank()) "Belum ada produk aktif." else "Produk tidak ditemukan.", color = v10Muted, modifier = Modifier.padding(20.dp)) }
                    }
                }
            }

            Card(Modifier.widthIn(min = 270.dp, max = 360.dp).fillMaxHeight()) {
                Column(Modifier.padding(14.dp).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Keranjang", color = v10Navy, fontSize = 18.sp)
                    rows.forEach { row ->
                        val product = row.first
                        val item = row.second
                        val price = priceFor(product, item.qty)
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(v10s(product, "name"), color = v10Navy)
                                Text("${item.qty} × ${v10money(price)}", color = v10Muted, fontSize = 10.sp)
                            }
                            IconButton(onClick = {
                                cart = if (item.qty <= 1) cart.filterNot { it.productId == item.productId }
                                else cart.map { if (it.productId == item.productId) it.copy(qty = it.qty - 1) else it }
                            }) { Icon(Icons.Default.Remove, contentDescription = "Kurangi") }
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    Text("Total", color = v10Muted, fontSize = 11.sp)
                    Text(v10money(total), color = v10Navy, fontSize = 20.sp)
                    Button(enabled = rows.isNotEmpty() && !paying, onClick = { paying = true }, modifier = Modifier.fillMaxWidth()) { Text("Bayar") }
                }
            }
        }
    }

    if (paying) V10Payment(total, rows, prices, { paying = false; cart = emptyList(); scope.launch { load() } }, { paying = false })
}

@Composable
private fun V10Payment(total: Long, cart: List<Pair<JsonObject, V10Cart>>, prices: List<JsonObject>, onDone: () -> Unit, onCancel: () -> Unit) {
    var receivedText by rememberSaveable(total) { mutableStateOf(total.toString()) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    val received = receivedText.toLongOrNull() ?: 0L
    val change = received - total
    val scope = rememberCoroutineScope()

    fun priceFor(product: JsonObject, qty: Long): Long = prices
        .filter { v10s(it, "product_id") == v10s(product, "id") && v10d(it, "min_qty") <= qty }
        .maxByOrNull { v10d(it, "min_qty") }
        ?.let { v10l(it, "price") } ?: 0L

    AlertDialog(
        onDismissRequest = { if (!saving) onCancel() },
        title = { Text("Pembayaran") },
        text = {
            Column(Modifier.fillMaxWidth().heightIn(max = 430.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Total ${v10money(total)}", color = v10Navy, fontSize = 22.sp)
                Text("Item ${cart.sumOf { it.second.qty }}", color = v10Muted)
                cart.forEach { (product, item) -> Text("${item.qty} × ${v10s(product, "name")} = ${v10money(priceFor(product, item.qty) * item.qty)}", color = v10Muted, fontSize = 12.sp) }
                OutlinedTextField(
                    value = receivedText,
                    onValueChange = { receivedText = it.filter(Char::isDigit) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Uang diterima") },
                    prefix = { Text("Rp ") },
                    singleLine = true
                )
                Text(
                    if (change >= 0) "Kembalian ${v10money(change)}" else "Kurang bayar ${v10money(-change)}",
                    color = if (change >= 0) v10Blue else v10Danger,
                    fontSize = 16.sp
                )
                if (error.isNotBlank()) Text(error, color = v10Danger)
            }
        },
        confirmButton = {
            Button(enabled = !saving && received >= total, onClick = {
                scope.launch {
                    saving = true
                    error = ""
                    runCatching {
                        val business = v10Client.from("businesses").select { filter { eq("code", "TOKO_MAJU_JAYA") } }.decodeList<JsonObject>().first()
                        val businessId = v10s(business, "id")
                        val branch = v10Client.from("branches").select { filter { eq("business_id", businessId); eq("is_active", true) } }.decodeList<JsonObject>().first()
                        val location = v10Client.from("locations").select { filter { eq("branch_id", v10s(branch, "id")); eq("is_active", true) } }.decodeList<JsonObject>().first()
                        val method = v10Client.from("payment_methods").select { filter { eq("business_id", businessId); eq("code", "CASH"); eq("is_active", true) } }.decodeList<JsonObject>().first()
                        val itemArray = JsonArray(cart.map { (product, item) ->
                            buildJsonObject {
                                put("product_id", v10s(product, "id"))
                                put("unit_id", v10s(product, "base_unit_id"))
                                put("sku", v10s(product, "sku"))
                                put("name", v10s(product, "name"))
                                put("qty", item.qty)
                                put("conversion_to_base", 1)
                                put("unit_price", priceFor(product, item.qty))
                                put("hpp_unit", 0)
                            }
                        })
                        val paymentArray = JsonArray(listOf(buildJsonObject {
                            put("payment_method_id", v10s(method, "id"))
                            put("amount", total)
                            put("provider", "CASH")
                        }))
                        v10Client.postgrest.rpc("checkout_sale_multi_payment", buildJsonObject {
                            put("p_branch_id", v10s(branch, "id"))
                            put("p_location_id", v10s(location, "id"))
                            put("p_customer_id", JsonNull)
                            put("p_idempotency_key", UUID.randomUUID().toString())
                            put("p_items", itemArray)
                            put("p_payments", paymentArray)
                        })
                    }.onSuccess { onDone() }.onFailure { error = v10Error(it) }
                    saving = false
                }
            }) { Text(if (saving) "Memproses" else "Konfirmasi") }
        },
        dismissButton = { TextButton(enabled = !saving, onClick = onCancel) { Text("Batal") } }
    )
}

@Composable
private fun V10Products() {
    var products by remember { mutableStateOf<List<JsonObject>>(emptyList()) }
    var categories by remember { mutableStateOf<List<JsonObject>>(emptyList()) }
    var units by remember { mutableStateOf<List<JsonObject>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf<JsonObject?>(null) }
    var showForm by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    suspend fun load() {
        loading = true
        runCatching {
            val business = v10Client.from("businesses").select { filter { eq("code", "TOKO_MAJU_JAYA") } }.decodeList<JsonObject>().first()
            val businessId = v10s(business, "id")
            products = v10Client.from("products").select { filter { eq("business_id", businessId) } }.decodeList()
            categories = v10Client.from("categories").select { filter { eq("business_id", businessId); eq("is_active", true) } }.decodeList()
            units = v10Client.from("units").select { filter { eq("business_id", businessId) } }.decodeList()
        }.onFailure { error = v10Error(it) }
        loading = false
    }

    LaunchedEffect(Unit) { load() }

    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Produk", color = v10Navy, fontSize = 26.sp, modifier = Modifier.weight(1f))
            Button(onClick = { editing = null; showForm = true }) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(6.dp)); Text("Tambah produk") }
        }
        if (error.isNotBlank()) Text(error, color = v10Danger)
        if (loading) Text("Memuat katalog...", color = v10Muted)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(products) { product ->
                val category = categories.firstOrNull { v10s(it, "id") == v10s(product, "category_id") }
                val unit = units.firstOrNull { v10s(it, "id") == v10s(product, "base_unit_id") }
                Card(Modifier.fillMaxWidth().clickable { editing = product; showForm = true }) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(v10s(product, "name"), color = v10Navy, fontSize = 16.sp)
                        Text("SKU ${v10s(product, "sku")} • Barcode ${v10s(product, "barcode").ifBlank { "belum diisi" }}", color = v10Muted, fontSize = 11.sp)
                        Text("${v10s(category ?: buildJsonObject {}, "name").ifBlank { "Tanpa kategori" }} • ${v10s(unit ?: buildJsonObject {}, "name").ifBlank { "Tanpa satuan" }}", color = v10Muted, fontSize = 11.sp)
                    }
                }
            }
        }
    }

    if (showForm) V10ProductForm(editing, categories, units, { showForm = false; scope.launch { load() } }, { showForm = false })
}

@Composable
private fun V10Drop(label: String, selected: String, options: List<Pair<String, String>>, onSelected: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(if (selected.isBlank()) "Pilih $label" else selected, Modifier.weight(1f))
                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
            }
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { pair ->
                DropdownMenuItem(text = { Text(pair.second) }, onClick = { onSelected(pair.first); open = false })
            }
        }
    }
}

@Composable
private fun V10ProductForm(
    existing: JsonObject?,
    categories: List<JsonObject>,
    units: List<JsonObject>,
    onSaved: () -> Unit,
    onCancel: () -> Unit
) {
    var name by remember(existing) { mutableStateOf(v10s(existing ?: buildJsonObject {}, "name")) }
    var sku by remember(existing) { mutableStateOf(v10s(existing ?: buildJsonObject {}, "sku")) }
    var barcode by remember(existing) { mutableStateOf(v10s(existing ?: buildJsonObject {}, "barcode")) }
    var categoryId by remember(existing) { mutableStateOf(v10s(existing ?: buildJsonObject {}, "category_id")) }
    var unitId by remember(existing) { mutableStateOf(v10s(existing ?: buildJsonObject {}, "base_unit_id")) }
    var minStock by remember(existing) { mutableStateOf(v10l(existing ?: buildJsonObject {}, "min_stock").toString()) }
    var costPrice by remember(existing) { mutableStateOf(v10l(existing ?: buildJsonObject {}, "cost_price").toString()) }
    var description by remember(existing) { mutableStateOf(v10s(existing ?: buildJsonObject {}, "description")) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val categoryName = categories.firstOrNull { v10s(it, "id") == categoryId }?.let { v10s(it, "name") }.orEmpty()
    val unitName = units.firstOrNull { v10s(it, "id") == unitId }?.let { v10s(it, "name") }.orEmpty()

    AlertDialog(
        onDismissRequest = { if (!saving) onCancel() },
        title = { Text(if (existing == null) "Tambah produk" else "Edit produk") },
        text = {
            Column(Modifier.fillMaxWidth().heightIn(max = 520.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text("Nama produk") }, singleLine = true)
                OutlinedTextField(sku, { sku = it }, Modifier.fillMaxWidth(), label = { Text("SKU") }, singleLine = true)
                OutlinedTextField(barcode, { barcode = it }, Modifier.fillMaxWidth(), label = { Text("Barcode") }, singleLine = true)
                V10Drop("kategori", categoryName, categories.map { v10s(it, "id") to v10s(it, "name") }) { categoryId = it }
                V10Drop("satuan", unitName, units.map { v10s(it, "id") to v10s(it, "name") }) { unitId = it }
                OutlinedTextField(minStock, { minStock = it.filter(Char::isDigit) }, Modifier.fillMaxWidth(), label = { Text("Minimum stok") }, singleLine = true)
                OutlinedTextField(costPrice, { costPrice = it.filter(Char::isDigit) }, Modifier.fillMaxWidth(), label = { Text("Harga modal") }, prefix = { Text("Rp ") }, singleLine = true)
                OutlinedTextField(description, { description = it }, Modifier.fillMaxWidth(), label = { Text("Deskripsi") }, minLines = 3)
                if (error.isNotBlank()) Text(error, color = v10Danger)
            }
        },
        confirmButton = {
            Button(enabled = !saving && name.isNotBlank() && sku.isNotBlank() && unitId.isNotBlank(), onClick = {
                scope.launch {
                    saving = true
                    error = ""
                    runCatching {
                        val business = v10Client.from("businesses").select { filter { eq("code", "TOKO_MAJU_JAYA") } }.decodeList<JsonObject>().first()
                        val businessId = v10s(business, "id")
                        val payload = buildJsonObject {
                            put("business_id", businessId)
                            put("name", name.trim())
                            put("short_name", name.trim())
                            put("sku", sku.trim())
                            put("barcode", barcode.trim())
                            if (categoryId.isBlank()) put("category_id", JsonNull) else put("category_id", categoryId)
                            put("base_unit_id", unitId)
                            put("min_stock", minStock.toLongOrNull() ?: 0L)
                            put("cost_price", costPrice.toLongOrNull() ?: 0L)
                            put("description", description.trim())
                            put("is_active", true)
                        }
                        if (existing == null) {
                            v10Client.from("products").insert(payload)
                        } else {
                            v10Client.from("products").update(payload) { filter { eq("id", v10s(existing, "id")) } }
                        }
                    }.onSuccess { onSaved() }.onFailure { error = v10Error(it) }
                    saving = false
                }
            }) { Text(if (saving) "Menyimpan" else "Simpan") }
        },
        dismissButton = { TextButton(enabled = !saving, onClick = onCancel) { Text("Batal") } }
    )
}
