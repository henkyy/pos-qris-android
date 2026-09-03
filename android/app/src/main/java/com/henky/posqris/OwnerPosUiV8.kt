@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.henky.posqris

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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

private val client get() = SupabaseClientProvider.client
private val navy = Color(0xFF071A33)
private val blue = Color(0xFF2563EB)
private val bg = Color(0xFFF4F7FB)
private val muted = Color(0xFF617089)
private val danger = Color(0xFFC53B3B)

private val menus = listOf(
    "Dashboard", "Penjualan", "Pesanan", "Produk", "Stok",
    "Pelanggan", "Supplier", "Pembelian", "Piutang", "Laporan",
    "Pembayaran", "Pengaturan"
)

private fun str(o: JsonObject, key: String) = o[key]?.jsonPrimitive?.contentOrNull.orEmpty()
private fun long(o: JsonObject, key: String) = o[key]?.jsonPrimitive?.longOrNull ?: 0L
private fun num(o: JsonObject, key: String) = o[key]?.jsonPrimitive?.doubleOrNull ?: 0.0
private fun money(v: Long): String = "Rp " + v.toString().reversed().chunked(3).joinToString(".").reversed()

private fun safeMessage(t: Throwable): String {
    val raw = t.message.orEmpty()
    return when {
        raw.contains("stock", true) -> "Stok tidak mencukupi."
        raw.contains("price", true) -> "Harga produk berubah. Muat ulang katalog."
        raw.contains("qris", true) -> "QRIS belum dikonfigurasi."
        raw.contains("network", true) || raw.contains("timeout", true) -> "Koneksi ke server gagal."
        else -> "Operasi gagal. Periksa data dan coba lagi."
    }
}

private fun iconFor(label: String) = when (label) {
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
fun OwnerPosAppV8() {
    var page by remember { mutableStateOf("Dashboard") }
    var more by remember { mutableStateOf(false) }
    val tablet = LocalConfiguration.current.screenWidthDp >= 600
    MaterialTheme(colorScheme = lightColorScheme(primary = blue, background = bg, surface = Color.White)) {
        Surface(Modifier.fillMaxSize(), color = bg) {
            if (tablet) {
                Row(Modifier.fillMaxSize()) {
                    SidebarV8(page) { page = it }
                    Box(Modifier.weight(1f).fillMaxSize()) { ScreenV8(page) }
                }
            } else {
                Column(Modifier.fillMaxSize()) {
                    Box(Modifier.weight(1f).fillMaxWidth()) { ScreenV8(page) }
                    BottomNavV8(page, { page = it }) { more = true }
                    if (more) MoreV8(page, { page = it; more = false }) { more = false }
                }
            }
        }
    }
}

@Composable private fun SidebarV8(selected: String, onSelect: (String) -> Unit) {
    Surface(Modifier.width(238.dp).fillMaxHeight(), color = navy) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(Modifier.padding(8.dp, 8.dp, 8.dp, 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.QrCode2, null, tint = Color.White, modifier = Modifier.size(36.dp))
                Column(Modifier.padding(start = 10.dp)) {
                    Text("POS QRIS", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                    Text("Owner workspace", color = Color(0xFFA9BAD0), fontSize = 11.sp)
                }
            }
            menus.forEach { item ->
                val active = item == selected
                Surface(
                    Modifier.fillMaxWidth().clickable { onSelect(item) },
                    RoundedCornerShape(12.dp),
                    color = if (active) blue else Color.Transparent
                ) {
                    Row(Modifier.padding(horizontal = 11.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(iconFor(item), null, Modifier.size(19.dp), tint = if (active) Color.White else Color(0xFFB7C6D9))
                        Text(item, Modifier.padding(start = 10.dp), color = if (active) Color.White else Color(0xFFE7EEF7), fontWeight = if (active) FontWeight.Bold else FontWeight.Medium)
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            Text("LIVE • Supabase", color = Color(0xFFA9BAD0), fontSize = 11.sp, modifier = Modifier.padding(8.dp))
        }
    }
}

@Composable private fun BottomNavV8(selected: String, onSelect: (String) -> Unit, onMore: () -> Unit) {
    val main = listOf("Dashboard", "Penjualan", "Produk", "Stok")
    Surface(Modifier.fillMaxWidth().navigationBarsPadding(), color = Color.White, shadowElevation = 12.dp) {
        Row(Modifier.padding(7.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            main.forEach { item -> NavItemV8(item, selected == item, Modifier.weight(1f)) { onSelect(item) } }
            NavItemV8("Lainnya", selected !in main, Modifier.weight(1f), onMore)
        }
    }
}

@Composable private fun NavItemV8(label: String, active: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Surface(modifier.clickable { onClick() }, RoundedCornerShape(12.dp), color = if (active) Color(0xFFEAF2FF) else Color.Transparent) {
        Column(Modifier.padding(vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(if (label == "Lainnya") Icons.Default.MoreHoriz else iconFor(label), null, Modifier.size(19.dp), tint = if (active) blue else muted)
            Text(label, fontSize = 10.sp, color = if (active) blue else muted)
        }
    }
}

@Composable private fun MoreV8(selected: String, onSelect: (String) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(18.dp)) {
            Text("Menu Owner", color = navy, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
            menus.drop(4).forEach { item ->
                Surface(Modifier.fillMaxWidth().clickable { onSelect(item) }, RoundedCornerShape(12.dp), color = if (item == selected) Color(0xFFEAF2FF) else Color.Transparent) {
                    Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(iconFor(item), null, tint = blue)
                        Text(item, Modifier.padding(start = 12.dp), color = navy)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable private fun HeaderV8(title: String, subtitle: String, loading: Boolean, refresh: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, color = navy, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
            Text(subtitle, color = muted, fontSize = 12.sp)
        }
        OutlinedButton(onClick = refresh, enabled = !loading, shape = RoundedCornerShape(11.dp)) {
            Icon(Icons.Default.Refresh, null, Modifier.size(17.dp))
            Spacer(Modifier.width(5.dp))
            Text(if (loading) "Memuat" else "Refresh")
        }
    }
}

@Composable private fun DataCardV8(modifier: Modifier = Modifier, title: String? = null, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier, RoundedCornerShape(17.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.padding(15.dp)) {
            if (title != null) Text(title, color = navy, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(bottom = 8.dp))
            content()
        }
    }
}

@Composable private fun ErrorBannerV8(message: String) {
    Surface(color = Color(0xFFFFEEEE), shape = RoundedCornerShape(12.dp)) { Text(message, color = danger, fontSize = 12.sp, modifier = Modifier.padding(12.dp)) }
}

@Composable private fun EmptyV8(message: String) { Text(message, color = muted, fontSize = 12.sp, modifier = Modifier.padding(vertical = 18.dp)) }

@Composable private fun MetricV8(label: String, value: String, caption: String, modifier: Modifier) {
    DataCardV8(modifier) {
        Text(label, color = muted, fontSize = 11.sp)
        Text(value, color = navy, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold)
        Text(caption, color = muted, fontSize = 10.sp)
    }
}

@Composable private fun ScreenV8(page: String) {
    when (page) {
        "Dashboard" -> DashboardV8()
        "Penjualan" -> SalesV8()
        "Produk" -> CrudV8("Produk", "products", listOf("name", "sku", "description", "category_id", "base_unit_id", "min_stock", "current_cost", "image_path"))
        "Stok" -> StockV8()
        "Pelanggan" -> CrudV8("Pelanggan", "customers", listOf("name", "code", "phone", "email", "address"))
        "Supplier" -> CrudV8("Supplier", "suppliers", listOf("name", "code", "phone", "email", "address"))
        "Pesanan" -> DetailListV8("Pesanan", "sales", "sale_no", "total_amount", "status")
        "Pembelian" -> DetailListV8("Pembelian", "purchase_orders", "order_no", "total_amount", "status")
        "Piutang" -> DetailListV8("Piutang", "receivables", "invoice_no", "outstanding_amount", "status")
        "Pembayaran" -> DetailListV8("Pembayaran", "payments", "payment_no", "amount", "status")
        "Laporan" -> ReportsV8()
        "Pengaturan" -> SettingsV8()
    }
}

@Composable private fun DashboardV8() {
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }
    var business by remember { mutableStateOf<JsonObject?>(null) }
    var sales by remember { mutableStateOf<List<JsonObject>>(emptyList()) }
    var payments by remember { mutableStateOf<List<JsonObject>>(emptyList()) }
    var products by remember { mutableStateOf<List<JsonObject>>(emptyList()) }
    val scope = rememberCoroutineScope()

    suspend fun load() {
        loading = true; error = ""
        runCatching {
            business = client.from("businesses").select { filter { eq("code", "TOKO_MAJU_JAYA") } }.decodeList<JsonObject>().firstOrNull()
            sales = client.from("sales").select().decodeList()
            payments = client.from("payments").select().decodeList()
            products = client.from("products").select { filter { eq("is_active", true) } }.decodeList()
        }.onFailure { error = safeMessage(it) }
        loading = false
    }
    LaunchedEffect(Unit) { load() }
    val completed = sales.filter { str(it, "status").uppercase() == "COMPLETED" }
    val paid = payments.filter { str(it, "status").uppercase() in setOf("PAID", "COMPLETED") }

    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        HeaderV8("Dashboard", "${str(business ?: buildJsonObject {}, "name")} • data Supabase", loading) { scope.launch { load() } }
        if (error.isNotBlank()) ErrorBannerV8(error)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MetricV8("Omzet", money(completed.sumOf { long(it, "total_amount") }), "transaksi selesai", Modifier.weight(1.2f))
                    MetricV8("Transaksi", completed.size.toString(), "selesai", Modifier.weight(1f))
                    MetricV8("Payment", paid.size.toString(), "berhasil", Modifier.weight(1f))
                    MetricV8("Produk", products.size.toString(), "aktif", Modifier.weight(1f))
                }
            }
            item {
                DataCardV8(Modifier.fillMaxWidth(), "Transaksi terbaru") {
                    if (sales.isEmpty()) EmptyV8("Belum ada transaksi.")
                    sales.sortedByDescending { str(it, "sale_date") }.take(8).forEach {
                        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(str(it, "sale_no"), color = navy, fontWeight = FontWeight.Bold)
                                Text(str(it, "sale_date").replace("T", " ").take(16), color = muted, fontSize = 10.sp)
                            }
                            Text(money(long(it, "total_amount")), color = navy, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable private fun CrudV8(title: String, table: String, fields: List<String>) {
    var rows by remember { mutableStateOf<List<JsonObject>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }
    var showForm by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<JsonObject?>(null) }
    val scope = rememberCoroutineScope()

    suspend fun load() {
        loading = true; error = ""
        runCatching { rows = client.from(table).select().decodeList<JsonObject>() }.onFailure { error = safeMessage(it) }
        loading = false
    }
    LaunchedEffect(table) { load() }

    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, color = navy, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
                Text("${rows.size} data dari Supabase", color = muted, fontSize = 12.sp)
            }
            Button(onClick = { editing = null; showForm = true }) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(5.dp)); Text("Tambah") }
        }
        if (error.isNotBlank()) ErrorBannerV8(error)
        if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
        if (!loading && rows.isEmpty()) EmptyV8("Belum ada data.")
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(rows) { row ->
                DataCardV8(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(str(row, "name").ifBlank { str(row, "code") }, color = navy, fontWeight = FontWeight.Bold)
                            Text(fields.drop(1).map { "$it: ${str(row, it)}" }.filter { !it.endsWith(": ") }.take(2).joinToString(" • "), color = muted, fontSize = 11.sp)
                        }
                        IconButton(onClick = { editing = row; showForm = true }) { Icon(Icons.Default.Edit, null) }
                    }
                }
            }
        }
    }
    if (showForm) CrudFormV8(title, table, fields, editing, { showForm = false; scope.launch { load() } }, { showForm = false })
}

@Composable private fun CrudFormV8(title: String, table: String, fields: List<String>, existing: JsonObject?, onSaved: () -> Unit, onCancel: () -> Unit) {
    val values = remember(existing) { mutableStateMapOf<String, String>().also { map -> fields.forEach { map[it] = str(existing ?: buildJsonObject {}, it) } } }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var uploadingImage by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { imageUri = it }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(if (existing == null) "Tambah $title" else "Edit $title") },
        text = {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (error.isNotBlank()) ErrorBannerV8(error)
                if (table == "products") {
                    OutlinedButton(onClick = { imagePicker.launch("image/*") }, enabled = !saving && !uploadingImage, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Image, null)
                        Spacer(Modifier.width(6.dp))
                        Text(if (imageUri == null) "Pilih gambar produk" else "Gambar dipilih")
                    }
                }
                fields.forEach { field ->
                    OutlinedTextField(values[field].orEmpty(), { values[field] = it }, Modifier.fillMaxWidth(), label = { Text(field) }, singleLine = field != "description")
                }
            }
        },
        confirmButton = {
            Button(enabled = !saving, onClick = {
                scope.launch {
                    saving = true; error = ""
                    runCatching {
                        var uploadedPath = values["image_path"].orEmpty()
                        val uri = imageUri
                        if (table == "products" && uri != null) {
                            uploadingImage = true
                            val bytes = withContext(Dispatchers.IO) {
                                context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: error("Gambar tidak dapat dibaca.")
                            }
                            val ext = context.contentResolver.getType(uri)?.substringAfterLast('/')?.lowercase().orEmpty().ifBlank { "jpg" }
                            uploadedPath = "${demoBusinessId()}/${UUID.randomUUID()}.$ext"
                            client.storage.from("product-images").upload(uploadedPath, bytes) { upsert = false }
                            uploadingImage = false
                        }
                        val payload = buildJsonObject {
                            fields.forEach { field ->
                                val v = values[field].orEmpty()
                                when (field) {
                                    "min_stock" -> put(field, v.toDoubleOrNull() ?: 0.0)
                                    "current_cost" -> put(field, v.toLongOrNull() ?: 0L)
                                    "image_path" -> put(field, if (uploadedPath.isBlank()) v else uploadedPath)
                                    else -> put(field, v)
                                }
                            }
                            if (existing == null) {
                                put("business_id", demoBusinessId())
                                if (table == "products" && values["base_unit_id"].orEmpty().isBlank()) {
                                    val unit = client.from("units").select { filter { eq("business_id", demoBusinessId()) } }.decodeList<JsonObject>().firstOrNull()
                                    if (unit != null) put("base_unit_id", str(unit, "id"))
                                }
                            }
                        }
                        if (existing == null) client.from(table).insert(payload)
                        else client.from(table).update(payload) { filter { eq("id", str(existing, "id")) } }
                    }.onSuccess { onSaved() }.onFailure { error = safeMessage(it) }
                    uploadingImage = false
                    saving = false
                }
            }) { Text(if (saving || uploadingImage) "Menyimpan" else "Simpan") }
        },
        dismissButton = { TextButton(onClick = onCancel) { Text("Batal") } }
    )
}

private suspend fun demoBusinessId(): String =
    client.from("businesses").select { filter { eq("code", "TOKO_MAJU_JAYA") } }.decodeList<JsonObject>().first().let { str(it, "id") }

@Composable private fun StockV8() {
    var rows by remember { mutableStateOf<List<JsonObject>>(emptyList()) }
    var products by remember { mutableStateOf<Map<String, JsonObject>>(emptyMap()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    suspend fun load() {
        loading = true; error = ""
        runCatching {
            rows = client.from("stock_balances").select().decodeList()
            products = client.from("products").select().decodeList<JsonObject>().associateBy { str(it, "id") }
        }.onFailure { error = safeMessage(it) }
        loading = false
    }
    LaunchedEffect(Unit) { load() }
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        HeaderV8("Stok", "Saldo, minimum stock, dan status", loading) { scope.launch { load() } }
        if (error.isNotBlank()) ErrorBannerV8(error)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (!loading && rows.isEmpty()) item { EmptyV8("Belum ada saldo stok.") }
            items(rows) { row ->
                val product = products[str(row, "product_id")]
                val qty = num(row, "qty_base")
                val min = num(product ?: buildJsonObject {}, "min_stock")
                val status = when { qty <= 0 -> "HABIS"; qty <= min -> "MENIPIS"; else -> "AMAN" }
                DataCardV8(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(str(product ?: buildJsonObject {}, "name").ifBlank { str(row, "product_id") }, color = navy, fontWeight = FontWeight.Bold)
                            Text("Saldo ${qty.toLong()} • minimum ${min.toLong()}", color = muted, fontSize = 11.sp)
                        }
                        Text(status, color = if (status == "AMAN") Color(0xFF218838) else danger, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

private data class CartLineV8(val productId: String, val name: String, val sku: String, val unitId: String, val price: Long, val qty: Long)

@Composable private fun SalesV8() {
    var products by remember { mutableStateOf<List<JsonObject>>(emptyList()) }
    var prices by remember { mutableStateOf<List<JsonObject>>(emptyList()) }
    var cart by remember { mutableStateOf<List<CartLineV8>>(emptyList()) }
    var search by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }
    var paying by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    suspend fun load() {
        loading = true; error = ""
        runCatching {
            val b = client.from("businesses").select { filter { eq("code", "TOKO_MAJU_JAYA") } }.decodeList<JsonObject>().first()
            products = client.from("products").select { filter { eq("business_id", str(b, "id")); eq("is_active", true) } }.decodeList()
            val list = client.from("price_lists").select { filter { eq("business_id", str(b, "id")); eq("is_default", true); eq("is_active", true) } }.decodeList<JsonObject>().firstOrNull()
            prices = if (list == null) emptyList() else client.from("product_prices").select { filter { eq("price_list_id", str(list, "id")) } }.decodeList()
        }.onFailure { error = safeMessage(it) }
        loading = false
    }
    LaunchedEffect(Unit) { load() }
    fun priceFor(p: JsonObject): Long = prices.filter { str(it, "product_id") == str(p, "id") }.maxByOrNull { num(it, "min_qty") }?.let { long(it, "price") } ?: 0L
    val shown = products.filter { search.isBlank() || str(it, "name").contains(search, true) || str(it, "sku").contains(search, true) }
    val total = cart.sumOf { it.price * it.qty }
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        HeaderV8("Penjualan", "POS nyata • checkout server-side", loading) { scope.launch { load() } }
        if (error.isNotBlank()) ErrorBannerV8(error)
        OutlinedTextField(search, { search = it }, Modifier.fillMaxWidth(), placeholder = { Text("Cari produk / SKU") }, singleLine = true)
        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            LazyVerticalGrid(columns = GridCells.Adaptive(155.dp), modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(shown) { p ->
                    DataCardV8(Modifier.fillMaxWidth().clickable {
                        val id = str(p, "id")
                        val old = cart.firstOrNull { it.productId == id }
                        cart = if (old == null) cart + CartLineV8(id, str(p, "name"), str(p, "sku"), str(p, "base_unit_id"), priceFor(p), 1)
                        else cart.map { if (it.productId == id) it.copy(qty = it.qty + 1) else it }
                    }) {
                        Text(str(p, "name"), color = navy, fontWeight = FontWeight.Bold)
                        Text(str(p, "sku"), color = muted, fontSize = 10.sp)
                        Text(money(priceFor(p)), color = blue, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 7.dp))
                    }
                }
            }
            DataCardV8(Modifier.widthIn(min = 250.dp, max = 340.dp).fillMaxHeight(), "Keranjang") {
                if (cart.isEmpty()) EmptyV8("Keranjang kosong.")
                cart.forEach { line ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) { Text(line.name, color = navy, fontWeight = FontWeight.Bold, maxLines = 1); Text("${line.qty} × ${money(line.price)}", color = muted, fontSize = 10.sp) }
                        IconButton(onClick = { cart = cart.filterNot { it.productId == line.productId } }) { Icon(Icons.Default.Delete, null) }
                    }
                }
                Spacer(Modifier.weight(1f))
                Text("Total", color = muted, fontSize = 11.sp)
                Text(money(total), color = navy, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                Button(enabled = cart.isNotEmpty() && !paying, onClick = { paying = true }, modifier = Modifier.fillMaxWidth()) { Text("Bayar") }
            }
        }
    }
    if (paying) PaymentDialogV8(total, cart, onDone = { paying = false; cart = emptyList(); scope.launch { load() } }, onCancel = { paying = false })
}

@Composable private fun PaymentDialogV8(total: Long, cart: List<CartLineV8>, onDone: () -> Unit, onCancel: () -> Unit) {
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Pembayaran") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text("Total ${money(total)}", color = navy, fontWeight = FontWeight.Bold)
                Text("Metode demo: CASH", color = muted, fontSize = 12.sp)
                if (error.isNotBlank()) ErrorBannerV8(error)
            }
        },
        confirmButton = {
            Button(enabled = !saving, onClick = {
                scope.launch {
                    saving = true; error = ""
                    runCatching {
                        val b = client.from("businesses").select { filter { eq("code", "TOKO_MAJU_JAYA") } }.decodeList<JsonObject>().first()
                        val branch = client.from("branches").select { filter { eq("business_id", str(b, "id")); eq("is_active", true) } }.decodeList<JsonObject>().first()
                        val loc = client.from("locations").select { filter { eq("branch_id", str(branch, "id")); eq("is_active", true) } }.decodeList<JsonObject>().first()
                        val method = client.from("payment_methods").select { filter { eq("business_id", str(b, "id")); eq("code", "CASH"); eq("is_active", true) } }.decodeList<JsonObject>().first()
                        client.postgrest.rpc("checkout_sale_multi_payment", buildJsonObject {
                            put("p_branch_id", str(branch, "id"))
                            put("p_location_id", str(loc, "id"))
                            put("p_customer_id", JsonNull)
                            put("p_idempotency_key", UUID.randomUUID().toString())
                            put("p_items", buildJsonArray {
                                cart.forEach { item ->
                                    add(buildJsonObject {
                                        put("product_id", item.productId); put("unit_id", item.unitId); put("sku", item.sku); put("name", item.name)
                                        put("qty", item.qty); put("conversion_to_base", 1); put("unit_price", item.price); put("hpp_unit", 0)
                                    })
                                }
                            })
                            put("p_payments", buildJsonArray { add(buildJsonObject { put("payment_method_id", str(method, "id")); put("amount", total); put("provider", "CASH") }) })
                        })
                    }.onSuccess { onDone() }.onFailure { error = safeMessage(it) }
                    saving = false
                }
            }) { Text(if (saving) "Memproses" else "Konfirmasi") }
        },
        dismissButton = { TextButton(onClick = onCancel) { Text("Batal") } }
    )
}

@Composable private fun DetailListV8(title: String, table: String, numberField: String, amountField: String, statusField: String) {
    var rows by remember { mutableStateOf<List<JsonObject>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<JsonObject?>(null) }
    val scope = rememberCoroutineScope()
    suspend fun load() { loading = true; error = ""; runCatching { rows = client.from(table).select().decodeList<JsonObject>() }.onFailure { error = safeMessage(it) }; loading = false }
    LaunchedEffect(table) { load() }
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        HeaderV8(title, "Daftar dan detail dari Supabase", loading) { scope.launch { load() } }
        if (error.isNotBlank()) ErrorBannerV8(error)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (!loading && rows.isEmpty()) item { EmptyV8("Belum ada data.") }
            items(rows) { row ->
                DataCardV8(Modifier.fillMaxWidth().clickable { selected = row }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) { Text(str(row, numberField), color = navy, fontWeight = FontWeight.Bold); Text(str(row, statusField), color = blue, fontSize = 11.sp) }
                        Text(money(long(row, amountField)), color = navy, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
    selected?.let { row ->
        AlertDialog(onDismissRequest = { selected = null }, title = { Text("$title detail") }, text = {
            LazyColumn { item { row.entries.forEach { (k, v) -> Text("$k: ${v.toString().trim('"')}", color = navy, fontSize = 12.sp, modifier = Modifier.padding(vertical = 3.dp)) } } }
        }, confirmButton = { TextButton(onClick = { selected = null }) { Text("Tutup") } })
    }
}

@Composable private fun ReportsV8() {
    var sales by remember { mutableStateOf<List<JsonObject>>(emptyList()) }
    var payments by remember { mutableStateOf<List<JsonObject>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    suspend fun load() { loading = true; error = ""; runCatching { sales = client.from("sales").select().decodeList(); payments = client.from("payments").select().decodeList() }.onFailure { error = safeMessage(it) }; loading = false }
    LaunchedEffect(Unit) { load() }
    val done = sales.filter { str(it, "status").uppercase() == "COMPLETED" }
    val paid = payments.filter { str(it, "status").uppercase() in setOf("PAID", "COMPLETED") }
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        HeaderV8("Laporan", "Ringkasan omzet, transaksi, dan payment", loading) { scope.launch { load() } }
        if (error.isNotBlank()) ErrorBannerV8(error)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricV8("Omzet", money(done.sumOf { long(it, "total_amount") }), "selesai", Modifier.weight(1f))
            MetricV8("Transaksi", done.size.toString(), "selesai", Modifier.weight(1f))
            MetricV8("Payment", money(paid.sumOf { long(it, "amount") }), "berhasil", Modifier.weight(1f))
            MetricV8("Margin", money(done.sumOf { long(it, "margin_amount") }), "tercatat", Modifier.weight(1f))
        }
        DataCardV8(Modifier.fillMaxWidth(), "Rekap status") { sales.groupingBy { str(it, "status").uppercase() }.eachCount().forEach { (status, count) -> Text("$status: $count", color = navy, modifier = Modifier.padding(vertical = 3.dp)) } }
    }
}

@Composable private fun SettingsV8() {
    var business by remember { mutableStateOf<JsonObject?>(null) }
    var branches by remember { mutableStateOf<List<JsonObject>>(emptyList()) }
    var qris by remember { mutableStateOf<List<JsonObject>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    suspend fun load() {
        loading = true; error = ""
        runCatching {
            business = client.from("businesses").select { filter { eq("code", "TOKO_MAJU_JAYA") } }.decodeList<JsonObject>().firstOrNull()
            val id = str(business ?: buildJsonObject {}, "id")
            branches = client.from("branches").select { filter { eq("business_id", id) } }.decodeList()
            qris = client.from("qris_configurations").select { filter { eq("business_id", id) } }.decodeList()
        }.onFailure { error = safeMessage(it) }
        loading = false
    }
    LaunchedEffect(Unit) { load() }
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        HeaderV8("Pengaturan", "Toko, cabang, lokasi, dan QRIS", loading) { scope.launch { load() } }
        if (error.isNotBlank()) ErrorBannerV8(error)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { DataCardV8(Modifier.fillMaxWidth(), "Toko") { Text(str(business ?: buildJsonObject {}, "name"), color = navy, fontSize = 18.sp, fontWeight = FontWeight.Bold); Text("${str(business ?: buildJsonObject {}, "phone")} • ${str(business ?: buildJsonObject {}, "email")}", color = muted, fontSize = 12.sp); Text(str(business ?: buildJsonObject {}, "address"), color = muted, fontSize = 12.sp) } }
            item { DataCardV8(Modifier.fillMaxWidth(), "Cabang") { if (branches.isEmpty()) EmptyV8("Belum ada cabang."); branches.forEach { Text("${str(it, "code")} • ${str(it, "name")}", color = navy, modifier = Modifier.padding(vertical = 4.dp)) } } }
            item { DataCardV8(Modifier.fillMaxWidth(), "QRIS") { if (qris.isEmpty()) EmptyV8("QRIS belum dikonfigurasi."); qris.forEach { Text("${str(it, "display_name")} • ${str(it, "provider")} • ${str(it, "mode")}", color = navy, modifier = Modifier.padding(vertical = 4.dp)) } } }
        }
    }
}
