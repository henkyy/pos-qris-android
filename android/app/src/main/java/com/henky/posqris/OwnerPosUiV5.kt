@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.henky.posqris

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

private val client = createSupabaseClient(BuildConfig.SUPABASE_URL, BuildConfig.SUPABASE_PUBLISHABLE_KEY) { install(Postgrest) }
private val Bg = Color(0xFFF4F7FB)
private val Navy = Color(0xFF071A33)
private val Blue = Color(0xFF2563EB)
private val Soft = Color(0xFFEAF2FF)
private val Muted = Color(0xFF617089)
private val Green = Color(0xFF14966B)
private val Amber = Color(0xFFF2A900)
private val Red = Color(0xFFD94A4A)
private val menus = listOf("Dashboard", "Penjualan", "Pesanan", "Produk", "Stok", "Pelanggan", "Supplier", "Pembelian", "Piutang", "Laporan", "Pembayaran", "Pengaturan")

private fun str(o: JsonObject, key: String) = o[key]?.jsonPrimitive?.contentOrNull.orEmpty()
private fun long(o: JsonObject, key: String) = o[key]?.jsonPrimitive?.longOrNull ?: 0L
private fun dbl(o: JsonObject, key: String) = o[key]?.jsonPrimitive?.doubleOrNull ?: 0.0
private fun money(v: Long): String = "Rp " + v.toString().reversed().chunked(3).joinToString(".").reversed()
private fun statusColor(value: String) = when (value.uppercase()) {
    "PAID", "COMPLETED", "ACTIVE", "RECEIVED" -> Green
    "OPEN", "PENDING", "PARTIAL", "LOW", "DRAFT" -> Amber
    "FAILED", "EXPIRED", "CANCELLED", "OVERDUE", "VOID" -> Red
    else -> Muted
}
private fun iconFor(name: String) = when (name) {
    "Dashboard" -> Icons.Default.Home
    "Penjualan" -> Icons.Default.ShoppingCart
    "Pesanan" -> Icons.Default.Receipt
    "Produk" -> Icons.Default.List
    "Stok" -> Icons.Default.List
    "Pelanggan" -> Icons.Default.Person
    "Supplier" -> Icons.Default.List
    "Pembelian" -> Icons.Default.ShoppingCart
    "Piutang" -> Icons.Default.List
    "Laporan" -> Icons.Default.List
    "Pembayaran" -> Icons.Default.List
    else -> Icons.Default.Settings
}

@Composable
fun OwnerPosAppV5() {
    var page by remember { mutableStateOf("Dashboard") }
    var more by remember { mutableStateOf(false) }
    val tablet = LocalConfiguration.current.screenWidthDp >= 600
    MaterialTheme(colorScheme = lightColorScheme(primary = Blue, background = Bg, surface = Color.White)) {
        Surface(modifier = Modifier.fillMaxSize(), color = Bg) {
            if (tablet) {
                Row(modifier = Modifier.fillMaxSize()) {
                    Sidebar(selected = page, onSelect = { page = it })
                    Box(modifier = Modifier.weight(1f).fillMaxSize()) { Page(page) }
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) { Page(page) }
                    BottomNav(selected = page, onSelect = { page = it }, onMore = { more = true })
                }
                if (more) MoreSheet(selected = page, onSelect = { page = it; more = false }, onDismiss = { more = false })
            }
        }
    }
}

@Composable private fun Sidebar(selected: String, onSelect: (String) -> Unit) {
    Surface(modifier = Modifier.width(240.dp).fillMaxHeight(), color = Navy) {
        Column(modifier = Modifier.fillMaxHeight().padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(modifier = Modifier.padding(7.dp, 9.dp, 7.dp, 18.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(42.dp).background(Blue, RoundedCornerShape(13.dp)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = Color.White)
                }
                Column(modifier = Modifier.padding(start = 10.dp)) {
                    Text("POS QRIS", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                    Text("Owner workspace", color = Color(0xFFA9BAD0), fontSize = 11.sp)
                }
            }
            menus.forEach { item ->
                val active = item == selected
                Surface(modifier = Modifier.fillMaxWidth().clickable { onSelect(item) }, shape = RoundedCornerShape(12.dp), color = if (active) Blue else Color.Transparent) {
                    Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(iconFor(item), contentDescription = null, modifier = Modifier.size(19.dp), tint = if (active) Color.White else Color(0xFFB7C6D9))
                        Text(item, modifier = Modifier.padding(start = 11.dp), color = if (active) Color.White else Color(0xFFE7EEF7), fontWeight = if (active) FontWeight.Bold else FontWeight.Medium)
                    }
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            Surface(shape = RoundedCornerShape(15.dp), color = Color(0xFF0D2542)) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Settings, contentDescription = null, tint = Blue, modifier = Modifier.size(28.dp))
                    Column(modifier = Modifier.padding(start = 9.dp)) {
                        Text("OWNER", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text("Live Supabase", color = Color(0xFFA9BAD0), fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

@Composable private fun BottomNav(selected: String, onSelect: (String) -> Unit, onMore: () -> Unit) {
    val main = listOf("Dashboard", "Penjualan", "Produk", "Stok")
    Surface(modifier = Modifier.fillMaxWidth().navigationBarsPadding(), color = Color.White, shadowElevation = 14.dp) {
        Row(modifier = Modifier.fillMaxWidth().padding(7.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            main.forEach { item -> NavItem(item, selected == item, Modifier.weight(1f)) { onSelect(item) } }
            NavItem("Lainnya", selected !in main, Modifier.weight(1f)) { onMore() }
        }
    }
}

@Composable private fun NavItem(label: String, active: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Surface(modifier = modifier.clickable { onClick() }, shape = RoundedCornerShape(13.dp), color = if (active) Soft else Color.Transparent) {
        Column(modifier = Modifier.padding(vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(if (label == "Lainnya") Icons.Default.MoreHoriz else iconFor(label), contentDescription = null, modifier = Modifier.size(19.dp), tint = if (active) Blue else Muted)
            Text(label, fontSize = 10.sp, color = if (active) Blue else Muted, fontWeight = if (active) FontWeight.Bold else FontWeight.Medium)
        }
    }
}

@Composable private fun MoreSheet(selected: String, onSelect: (String) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(18.dp)) {
            Text("Menu Owner", color = Navy, fontSize = 23.sp, fontWeight = FontWeight.ExtraBold)
            Text("Modul membaca data langsung dari Supabase", color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 3.dp, bottom = 13.dp))
            menus.drop(4).forEach { item ->
                Surface(modifier = Modifier.fillMaxWidth().clickable { onSelect(item) }, shape = RoundedCornerShape(12.dp), color = if (item == selected) Soft else Color.Transparent) {
                    Row(modifier = Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(iconFor(item), contentDescription = null, modifier = Modifier.size(20.dp), tint = Blue)
                        Text(item, modifier = Modifier.padding(start = 12.dp), color = Navy, fontWeight = if (item == selected) FontWeight.Bold else FontWeight.Medium)
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }
    }
}

@Composable private fun Page(page: String) {
    when (page) {
        "Dashboard" -> Dashboard()
        "Penjualan" -> Sales()
        "Produk" -> Products()
        "Stok" -> Stock()
        "Laporan" -> Reports()
        "Pengaturan" -> Settings()
        else -> DataModule(page)
    }
}

@Composable private fun Header(title: String, subtitle: String, refresh: () -> Unit, loading: Boolean) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Navy, fontSize = 27.sp, fontWeight = FontWeight.ExtraBold)
            Text(subtitle, color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 3.dp))
        }
        OutlinedButton(onClick = refresh, enabled = !loading, shape = RoundedCornerShape(12.dp)) {
            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(17.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(if (loading) "Memuat" else "Refresh")
        }
    }
}

@Composable private fun CardBox(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = modifier, shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Column(modifier = Modifier.padding(15.dp), content = content)
    }
}

@Composable private fun Metric(label: String, value: String, caption: String, modifier: Modifier) {
    CardBox(modifier) {
        Text(label, color = Muted, fontSize = 11.sp)
        Text(value, color = Navy, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 4.dp))
        Text(caption, color = Muted, fontSize = 10.sp, modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable private fun Badge(value: String) {
    val c = statusColor(value)
    Surface(shape = RoundedCornerShape(8.dp), color = c.copy(alpha = .11f)) {
        Text(value.ifBlank { "-" }.uppercase(), color = c, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
    }
}

@Composable private fun Dashboard() {
    var business by remember { mutableStateOf<JsonObject?>(null) }
    var branch by remember { mutableStateOf<JsonObject?>(null) }
    var products by remember { mutableStateOf<List<JsonObject>>(emptyList()) }
    var customers by remember { mutableStateOf<List<JsonObject>>(emptyList()) }
    var sales by remember { mutableStateOf<List<JsonObject>>(emptyList()) }
    var stock by remember { mutableStateOf<List<JsonObject>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    suspend fun load() {
        loading = true
        try {
            business = client.from("businesses").select { filter { eq("is_active", true) } }.decodeList<JsonObject>().firstOrNull()
            val bid = business?.let { str(it, "id") }
            if (!bid.isNullOrBlank()) branch = client.from("branches").select { filter { eq("business_id", bid); eq("is_active", true) } }.decodeList<JsonObject>().firstOrNull()
            products = client.from("products").select { filter { eq("is_active", true) } }.decodeList<JsonObject>()
            customers = client.from("customers").select { filter { eq("is_active", true) } }.decodeList<JsonObject>()
            sales = client.from("sales").select().decodeList<JsonObject>()
            stock = client.from("stock_balances").select().decodeList<JsonObject>()
        } finally { loading = false }
    }
    LaunchedEffect(Unit) { load(); while (true) { delay(10000); runCatching { load() } } }
    val today = LocalDate.now(ZoneId.of("Asia/Jakarta")).toString()
    val todaySales = sales.filter { str(it, "sale_date").startsWith(today) && str(it, "status").uppercase() == "COMPLETED" }
    val low = stock.count { row -> products.firstOrNull { str(it, "id") == str(row, "product_id") }?.let { dbl(row, "qty_base") <= dbl(it, "min_stock") } == true }
    Column(modifier = Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Header("Beranda", "${str(business ?: buildJsonObject {}, "name")} • ${str(branch ?: buildJsonObject {}, "name")} • Owner", { scope.launch { runCatching { load() } } }, loading)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Metric("Penjualan hari ini", money(todaySales.sumOf { long(it, "total_amount") }), "${todaySales.size} transaksi", Modifier.weight(1.3f))
                    Metric("Produk", products.size.toString(), "SKU aktif", Modifier.weight(1f))
                    Metric("Pelanggan", customers.size.toString(), "aktif", Modifier.weight(1f))
                    Metric("Stok menipis", low.toString(), "perlu perhatian", Modifier.weight(1f))
                }
            }
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    CardBox(modifier = Modifier.weight(1.4f)) {
                        Text("Transaksi terbaru", color = Navy, fontWeight = FontWeight.ExtraBold)
                        if (sales.isEmpty()) Text("Belum ada transaksi", color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 18.dp))
                        sales.sortedByDescending { str(it, "sale_date") }.take(8).forEach { row ->
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) { Text(str(row, "sale_no"), color = Navy, fontWeight = FontWeight.SemiBold); Text(str(row, "sale_date").replace("T", " ").take(16), color = Muted, fontSize = 10.sp) }
                                Text(money(long(row, "total_amount")), color = Navy, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.width(8.dp)); Badge(str(row, "status"))
                            }
                        }
                    }
                    CardBox(modifier = Modifier.weight(1f)) {
                        Text("Database live", color = Navy, fontWeight = FontWeight.ExtraBold)
                        Text("Ringkasan dari tabel Supabase", color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 5.dp, bottom = 8.dp))
                        LiveLine("Sales", sales.size.toString()); LiveLine("Products", products.size.toString()); LiveLine("Customers", customers.size.toString()); LiveLine("Stock", stock.size.toString())
                    }
                }
            }
        }
    }
}

@Composable private fun LiveLine(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(8.dp).background(Blue, RoundedCornerShape(50)))
        Text(label, modifier = Modifier.weight(1f).padding(start = 8.dp), color = Muted, fontSize = 12.sp)
        Text(value, color = Navy, fontWeight = FontWeight.Bold)
    }
}

private data class CartLine(val productId: String, val name: String, val sku: String, val unitId: String, val price: Long, val qty: Long)

@Composable private fun Sales() {
    var products by remember { mutableStateOf<List<JsonObject>>(emptyList()) }
    var prices by remember { mutableStateOf<List<JsonObject>>(emptyList()) }
    var cart by remember { mutableStateOf<List<CartLine>>(emptyList()) }
    var query by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var cartOpen by remember { mutableStateOf(false) }
    var paymentOpen by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    suspend fun load() {
        loading = true
        try {
            val b = client.from("businesses").select { filter { eq("is_active", true) } }.decodeList<JsonObject>().firstOrNull() ?: return
            products = client.from("products").select { filter { eq("business_id", str(b, "id")); eq("is_active", true) } }.decodeList<JsonObject>()
            val pl = client.from("price_lists").select { filter { eq("business_id", str(b, "id")); eq("is_default", true); eq("is_active", true) } }.decodeList<JsonObject>().firstOrNull()
            prices = if (pl == null) emptyList() else client.from("product_prices").select { filter { eq("price_list_id", str(pl, "id")) } }.decodeList<JsonObject>()
        } finally { loading = false }
    }
    LaunchedEffect(Unit) { load() }
    fun price(product: JsonObject): Long = prices.filter { str(it, "product_id") == str(product, "id") }.minByOrNull { dbl(it, "min_qty") }?.let { long(it, "price") } ?: long(product, "selling_price")
    val shown = products.filter { query.isBlank() || str(it, "name").contains(query, true) || str(it, "sku").contains(query, true) }
    val total = cart.sumOf { it.price * it.qty }
    Column(modifier = Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Header("Penjualan Baru", "Kasir • live Supabase", { scope.launch { runCatching { load() } } }, loading)
        OutlinedTextField(value = query, onValueChange = { query = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, placeholder = { Text("Cari produk atau SKU") }, leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }, shape = RoundedCornerShape(13.dp))
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (shown.isEmpty()) Text("Tidak ada produk dari database", color = Muted, modifier = Modifier.align(Alignment.Center))
            else LazyVerticalGrid(columns = GridCells.Adaptive(155.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 88.dp)) {
                items(shown) { product ->
                    CardBox(modifier = Modifier.fillMaxWidth().clickable {
                        val id = str(product, "id")
                        val old = cart.firstOrNull { it.productId == id }
                        cart = if (old == null) cart + CartLine(id, str(product, "name"), str(product, "sku"), str(product, "base_unit_id"), price(product), 1) else cart.map { if (it.productId == id) it.copy(qty = it.qty + 1) else it }
                    }) {
                        Box(modifier = Modifier.fillMaxWidth().height(76.dp).background(Soft, RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) { Icon(Icons.Default.List, contentDescription = null, modifier = Modifier.size(34.dp), tint = Blue) }
                        Text(str(product, "name"), color = Navy, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 10.dp))
                        Text(money(price(product)), color = Blue, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                    }
                }
            }
        }
        Surface(modifier = Modifier.fillMaxWidth().clickable { cartOpen = true }, shape = RoundedCornerShape(17.dp), color = Navy) {
            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = Color.White, modifier = Modifier.size(26.dp))
                Column(modifier = Modifier.weight(1f).padding(start = 10.dp)) { Text("Keranjang • ${cart.sumOf { it.qty }} item", color = Color.White, fontWeight = FontWeight.Bold); Text("Total ${money(total)}", color = Color(0xFFBFD0E5), fontSize = 11.sp) }
                Text("Bayar", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
            }
        }
        if (cartOpen) CartSheet(cart = cart, total = total, setCart = { cart = it }, dismiss = { cartOpen = false }, pay = { cartOpen = false; paymentOpen = true })
        if (paymentOpen) PaymentSheet(total = total, cart = cart, dismiss = { paymentOpen = false }, done = { paymentOpen = false; cart = emptyList(); scope.launch { runCatching { load() } } })
    }
}

@Composable private fun CartSheet(cart: List<CartLine>, total: Long, setCart: (List<CartLine>) -> Unit, dismiss: () -> Unit, pay: () -> Unit) {
    ModalBottomSheet(onDismissRequest = dismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(18.dp)) {
            Text("Detail Pesanan", color = Navy, fontSize = 23.sp, fontWeight = FontWeight.ExtraBold)
            LazyColumn(modifier = Modifier.heightIn(max = 390.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(cart) { line ->
                    CardBox {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) { Text(line.name, color = Navy, fontWeight = FontWeight.Bold); Text(money(line.price), color = Muted, fontSize = 10.sp) }
                            IconButton(onClick = { val q = line.qty - 1; setCart(if (q <= 0) cart.filterNot { it.productId == line.productId } else cart.map { if (it.productId == line.productId) it.copy(qty = q) else it }) }) { Icon(Icons.Default.Remove, contentDescription = null) }
                            Text(line.qty.toString(), fontWeight = FontWeight.Bold)
                            IconButton(onClick = { setCart(cart.map { if (it.productId == line.productId) it.copy(qty = it.qty + 1) else it }) }) { Icon(Icons.Default.Add, contentDescription = null) }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp)); Text("TOTAL", color = Muted, fontSize = 11.sp); Text(money(total), color = Navy, fontSize = 25.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(vertical = 4.dp))
            Button(onClick = pay, enabled = cart.isNotEmpty(), modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp)) { Text("Lanjut Pembayaran", fontWeight = FontWeight.ExtraBold) }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable private fun PaymentSheet(total: Long, cart: List<CartLine>, dismiss: () -> Unit, done: () -> Unit) {
    var methods by remember { mutableStateOf<List<JsonObject>>(emptyList()) }
    var selected by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf(total.toString()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) { runCatching { methods = client.from("payment_methods").select { filter { eq("is_active", true) } }.decodeList<JsonObject>(); selected = methods.firstOrNull()?.let { str(it, "id") }.orEmpty() }.onFailure { error = it.message ?: "Gagal memuat metode pembayaran" } }
    val method = methods.firstOrNull { str(it, "id") == selected }
    val code = str(method ?: buildJsonObject {}, "code").uppercase()
    val payAmount = amount.toLongOrNull() ?: 0L
    ModalBottomSheet(onDismissRequest = { if (!loading) dismiss() }) {
        Column(modifier = Modifier.fillMaxWidth().padding(18.dp)) {
            Text("Pembayaran", color = Navy, fontSize = 23.sp, fontWeight = FontWeight.ExtraBold)
            Text("Total ${money(total)}", color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 3.dp, bottom = 12.dp))
            Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) { methods.forEach { m -> FilterChip(selected = selected == str(m, "id"), onClick = { selected = str(m, "id") }, label = { Text(str(m, "name")) }) } }
            OutlinedTextField(value = amount, onValueChange = { amount = it.filter(Char::isDigit) }, modifier = Modifier.fillMaxWidth().padding(top = 10.dp), singleLine = true, label = { Text("Nominal pembayaran") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), shape = RoundedCornerShape(13.dp))
            if (code == "QRIS") {
                Surface(modifier = Modifier.fillMaxWidth().height(180.dp).padding(top = 10.dp), shape = RoundedCornerShape(16.dp), color = Color(0xFFF7FAFF)) { Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) { Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(60.dp), tint = Navy); Text("QRIS", color = Navy, fontWeight = FontWeight.Bold); Text("Menunggu konfirmasi", color = Muted, fontSize = 11.sp) } }
            }
            if (error.isNotBlank()) Text(error, color = Red, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
            Button(onClick = {
                scope.launch {
                    loading = true; error = ""
                    runCatching {
                        val b = client.from("businesses").select { filter { eq("is_active", true) } }.decodeList<JsonObject>().first()
                        val branch = client.from("branches").select { filter { eq("business_id", str(b, "id")); eq("is_active", true) } }.decodeList<JsonObject>().first()
                        val location = client.from("locations").select { filter { eq("branch_id", str(branch, "id")); eq("is_active", true) } }.decodeList<JsonObject>().first()
                        client.postgrest.rpc("checkout_sale_multi_payment", buildJsonObject {
                            put("p_branch_id", str(branch, "id")); put("p_location_id", str(location, "id")); put("p_customer_id", null as String?)
                            put("p_items", buildJsonArray { cart.forEach { line -> add(buildJsonObject { put("product_id", line.productId); put("unit_id", line.unitId); put("sku", line.sku); put("name", line.name); put("qty", line.qty); put("conversion_to_base", 1); put("unit_price", line.price); put("hpp_unit", 0) }) } })
                            put("p_payments", buildJsonArray { add(buildJsonObject { put("payment_method_id", selected); put("amount", payAmount); put("cash_received", payAmount); put("reference", ""); put("qris_confirmed", code == "QRIS") }) })
                            put("p_idempotency_key", UUID.randomUUID().toString())
                        })
                    }.onFailure { error = it.message ?: "Checkout gagal" }.onSuccess { done() }
                    loading = false
                }
            }, enabled = !loading && cart.isNotEmpty() && selected.isNotBlank() && payAmount > 0, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp)) { Text(if (loading) "Memproses..." else "Proses Pembayaran", fontWeight = FontWeight.ExtraBold) }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable private fun Products() {
    var rows by remember { mutableStateOf<List<JsonObject>>(emptyList()) }
    var query by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var addOpen by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    suspend fun load() { loading = true; try { rows = client.from("products").select { filter { eq("is_active", true) } }.decodeList<JsonObject>() } finally { loading = false } }
    LaunchedEffect(Unit) { load() }
    val filtered = rows.filter { query.isBlank() || str(it, "name").contains(query, true) || str(it, "sku").contains(query, true) }
    Column(modifier = Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Header("Produk", "Master produk • live Supabase", { scope.launch { runCatching { load() } } }, loading)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(value = query, onValueChange = { query = it }, modifier = Modifier.weight(1f), singleLine = true, placeholder = { Text("Cari nama atau SKU") }, leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }, shape = RoundedCornerShape(13.dp))
            Button(onClick = { addOpen = true }, shape = RoundedCornerShape(12.dp)) { Icon(Icons.Default.Add, contentDescription = null); Spacer(modifier = Modifier.width(5.dp)); Text("Produk") }
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) { items(filtered) { row -> CardBox { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.List, contentDescription = null, tint = Blue, modifier = Modifier.size(38.dp)); Column(modifier = Modifier.weight(1f).padding(start = 11.dp)) { Text(str(row, "name"), color = Navy, fontWeight = FontWeight.Bold); Text(str(row, "sku"), color = Muted, fontSize = 10.sp) }; Text(money(long(row, "current_cost")), color = Blue, fontWeight = FontWeight.ExtraBold) } } } }
        if (addOpen) AddProduct(dismiss = { addOpen = false }, saved = { scope.launch { runCatching { load() } } })
    }
}

@Composable private fun AddProduct(dismiss: () -> Unit, saved: () -> Unit) {
    var name by remember { mutableStateOf("") }; var sku by remember { mutableStateOf("") }; var saving by remember { mutableStateOf(false) }; var error by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    ModalBottomSheet(onDismissRequest = { if (!saving) dismiss() }) {
        Column(modifier = Modifier.fillMaxWidth().padding(18.dp)) {
            Text("Produk Baru", color = Navy, fontSize = 23.sp, fontWeight = FontWeight.ExtraBold)
            OutlinedTextField(value = name, onValueChange = { name = it }, modifier = Modifier.fillMaxWidth().padding(top = 10.dp), label = { Text("Nama produk") }, singleLine = true)
            OutlinedTextField(value = sku, onValueChange = { sku = it }, modifier = Modifier.fillMaxWidth().padding(top = 9.dp), label = { Text("SKU") }, singleLine = true)
            if (error.isNotBlank()) Text(error, color = Red, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
            Button(onClick = { scope.launch { saving = true; error = ""; runCatching { val b = client.from("businesses").select { filter { eq("is_active", true) } }.decodeList<JsonObject>().first(); client.from("products").insert(buildJsonObject { put("id", UUID.randomUUID().toString()); put("business_id", str(b, "id")); put("sku", sku.trim()); put("name", name.trim()); put("product_type", "GOODS"); put("track_batch", false); put("track_expiry", false); put("min_stock", 0); put("reorder_point", 0); put("cost_method", "AVERAGE"); put("last_purchase_cost", 0); put("current_cost", 0); put("is_active", true) } ) }.onFailure { error = it.message ?: "Gagal menyimpan" }.onSuccess { saved(); dismiss() }; saving = false } }, enabled = !saving && name.isNotBlank() && sku.isNotBlank(), modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp)) { Text(if (saving) "Menyimpan..." else "Simpan ke Supabase", fontWeight = FontWeight.ExtraBold) }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable private fun Stock() {
    var products by remember { mutableStateOf<List<JsonObject>>(emptyList()) }; var stock by remember { mutableStateOf<List<JsonObject>>(emptyList()) }; var loading by remember { mutableStateOf(false) }; val scope = rememberCoroutineScope()
    suspend fun load() { loading = true; try { products = client.from("products").select { filter { eq("is_active", true) } }.decodeList<JsonObject>(); stock = client.from("stock_balances").select().decodeList<JsonObject>() } finally { loading = false } }
    LaunchedEffect(Unit) { load() }
    Column(modifier = Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { Header("Stok", "Saldo stok • live Supabase", { scope.launch { runCatching { load() } } }, loading); LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) { items(products) { product -> val row = stock.firstOrNull { str(it, "product_id") == str(product, "id") }; val qty = dbl(row ?: buildJsonObject {}, "qty_base"); val low = qty <= dbl(product, "min_stock"); CardBox { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.List, contentDescription = null, tint = if (low) Red else Blue, modifier = Modifier.size(38.dp)); Column(modifier = Modifier.weight(1f).padding(start = 10.dp)) { Text(str(product, "name"), color = Navy, fontWeight = FontWeight.Bold); Text(str(product, "sku"), color = Muted, fontSize = 10.sp) }; Text(qty.toInt().toString(), color = Navy, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold) } } } } }
}

@Composable private fun DataModule(title: String) {
    val table = when (title) { "Pesanan" -> "sales"; "Pelanggan" -> "customers"; "Supplier" -> "suppliers"; "Pembelian" -> "purchase_orders"; "Piutang" -> "receivables"; "Pembayaran" -> "payments"; else -> "sales" }
    var rows by remember { mutableStateOf<List<JsonObject>>(emptyList()) }; var loading by remember { mutableStateOf(false) }; var query by remember { mutableStateOf("") }; val scope = rememberCoroutineScope()
    suspend fun load() { loading = true; try { rows = client.from(table).select().decodeList<JsonObject>() } finally { loading = false } }
    LaunchedEffect(Unit) { load() }
    val filtered = rows.filter { query.isBlank() || it.entries.any { e -> e.value.toString().contains(query, true) } }
    Column(modifier = Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { Header(title, "$table • live Supabase", { scope.launch { runCatching { load() } } }, loading); OutlinedTextField(value = query, onValueChange = { query = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, placeholder = { Text("Cari data") }, leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }, shape = RoundedCornerShape(13.dp)); LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) { items(filtered) { row -> CardBox { row.entries.take(5).forEach { (key, value) -> Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) { Text(key, modifier = Modifier.weight(1f), color = Muted, fontSize = 10.sp); Text(value.toString().removeSurrounding("\""), color = Navy, fontSize = 11.sp) } } } } } }
}

@Composable private fun Reports() {
    var sales by remember { mutableStateOf<List<JsonObject>>(emptyList()) }; var payments by remember { mutableStateOf<List<JsonObject>>(emptyList()) }; var loading by remember { mutableStateOf(false) }; val scope = rememberCoroutineScope()
    suspend fun load() { loading = true; try { sales = client.from("sales").select().decodeList<JsonObject>(); payments = client.from("payments").select().decodeList<JsonObject>() } finally { loading = false } }
    LaunchedEffect(Unit) { load() }; val completed = sales.filter { str(it, "status").uppercase() == "COMPLETED" }
    Column(modifier = Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { Header("Laporan", "Ringkasan dari sales dan payments", { scope.launch { runCatching { load() } } }, loading); LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) { item { Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) { Metric("Omzet", money(completed.sumOf { long(it, "total_amount") }), "sales completed", Modifier.weight(1.3f)); Metric("Transaksi", completed.size.toString(), "completed", Modifier.weight(1f)); Metric("Payments", payments.size.toString(), "record", Modifier.weight(1f)) } }; item { CardBox { Text("Payment mix", color = Navy, fontWeight = FontWeight.ExtraBold); payments.groupBy { str(it, "provider").ifBlank { "OTHER" } }.forEach { (key, value) -> LiveLine(key, value.size.toString()) } } } } }
}

@Composable private fun Settings() {
    val tables = listOf("businesses" to "Bisnis", "branches" to "Cabang", "locations" to "Lokasi", "payment_methods" to "Metode pembayaran", "qris_configurations" to "QRIS", "price_lists" to "Price list")
    var data by remember { mutableStateOf<Map<String, List<JsonObject>>>(emptyMap()) }; var loading by remember { mutableStateOf(false) }; val scope = rememberCoroutineScope()
    suspend fun load() { loading = true; try { data = tables.associate { (table, _) -> table to client.from(table).select().decodeList<JsonObject>() } } finally { loading = false } }
    LaunchedEffect(Unit) { load() }
    Column(modifier = Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { Header("Pengaturan", "Konfigurasi bisnis dan pembayaran • live Supabase", { scope.launch { runCatching { load() } } }, loading); LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) { items(tables) { (table, label) -> CardBox { Text(label, color = Navy, fontWeight = FontWeight.ExtraBold); val rows = data[table].orEmpty(); if (rows.isEmpty()) Text("Belum ada data", color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 12.dp)) else rows.take(5).forEach { row -> Text(row.entries.take(5).joinToString(" • ") { (k, v) -> "$k=${v.toString().removeSurrounding("\"")}" }, color = Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 8.dp)) } } } } }
}
