package com.henky.posqris

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.UUID

private val uiClient = createSupabaseClient(BuildConfig.SUPABASE_URL, BuildConfig.SUPABASE_PUBLISHABLE_KEY) { install(Postgrest) }
private val UiNavy = Color(0xFF021024)
private val UiBlue = Color(0xFF1E63D8)
private val UiSoftBlue = Color(0xFFEAF2FF)
private val UiBg = Color(0xFFF6F9FD)
private val UiMuted = Color(0xFF718096)
private val UiSuccess = Color(0xFF159957)
private val UiWarning = Color(0xFFD99500)

@Serializable private data class PProduct(val id: String, val sku: String, val name: String, val category_id: String? = null, val base_unit_id: String, val current_cost: Long = 0, val min_stock: Double = 0.0)
@Serializable private data class PCategory(val id: String, val name: String)
@Serializable private data class PPrice(val product_id: String, val price: Long, val min_qty: Double = 1.0)
@Serializable private data class PBranch(val id: String, val name: String)
@Serializable private data class PLocation(val id: String, val name: String)
@Serializable private data class PMethod(val id: String, val code: String, val name: String, val method_type: String)
@Serializable private data class PStock(val location_id: String, val product_id: String, val qty_base: Double = 0.0)
@Serializable private data class PResult(val sale_id: String, val sale_no: String, val total_amount: Long, val paid_amount: Long, val change_amount: Long, val sale_status: String)
private data class PCart(val product: PProduct, val price: Long, val qty: Int)
private data class PPayment(val methodId: String, val amount: Long, val cashReceived: Long, val reference: String?)

private fun money(value: Long): String = "Rp " + value.toString().reversed().chunked(3).joinToString(".").reversed()
private fun PPrice.matches(p: PProduct) = product_id == p.id

class PosUiActivityV2 : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContent { PosUiRoot() } }
}

@Composable private fun PosUiRoot() {
    var tab by remember { mutableStateOf("Penjualan") }
    val tablet = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp >= 600
    Surface(Modifier.fillMaxSize(), color = UiBg) {
        if (tablet) {
            Row(Modifier.fillMaxSize()) {
                SideMenu(tab) { tab = it }
                Box(Modifier.weight(1f).fillMaxHeight()) { if (tab == "Penjualan") SalesScreen(true) else OtherScreen(tab) }
            }
        } else {
            Column(Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f).fillMaxWidth()) { if (tab == "Penjualan") SalesScreen(false) else OtherScreen(tab) }
                BottomMenu(tab) { tab = it }
            }
        }
    }
}

@Composable private fun SideMenu(selected: String, onSelect: (String) -> Unit) {
    Surface(Modifier.width(214.dp).fillMaxHeight(), color = UiNavy) {
        Column(Modifier.fillMaxSize().padding(15.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.padding(7.dp, 7.dp, 7.dp, 17.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(36.dp).clip(RoundedCornerShape(11.dp)).background(UiBlue), contentAlignment = Alignment.Center) { Text("P", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold) }
                Column(Modifier.padding(start = 9.dp)) { Text("POS QRIS", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp); Text("Toko Demo", color = Color(0xFFB8C7D9), fontSize = 11.sp) }
            }
            listOf("Beranda", "Penjualan", "Pesanan", "Produk", "Stok", "Pelanggan", "Supplier", "Pembelian", "Piutang", "Laporan", "Pengaturan").forEach { item ->
                val active = selected == item
                Surface(Modifier.fillMaxWidth().clickable { onSelect(item) }, shape = RoundedCornerShape(11.dp), color = if (active) UiBlue else Color.Transparent) {
                    Row(Modifier.padding(horizontal = 11.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) { Text(iconFor(item), Modifier.width(27.dp), color = if (active) Color.White else Color(0xFFB8C7D9)); Text(item, color = if (active) Color.White else Color(0xFFE3EAF2), fontSize = 13.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Medium) }
                }
            }
            Spacer(Modifier.weight(1f))
            Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFF0A1D35)) { Row(Modifier.padding(11.dp), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(34.dp).clip(RoundedCornerShape(17.dp)).background(Color.White), contentAlignment = Alignment.Center) { Text("A", color = UiNavy, fontWeight = FontWeight.Bold) }; Column(Modifier.padding(start = 9.dp)) { Text("Admin", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp); Text("Toko Utama", color = Color(0xFF9EB0C4), fontSize = 10.sp) } } }
        }
    }
}

private fun iconFor(s: String) = when (s) { "Beranda" -> "⌂"; "Penjualan" -> "▣"; "Pesanan" -> "≡"; "Produk" -> "□"; "Stok" -> "◫"; "Pelanggan" -> "♙"; "Supplier" -> "♙"; "Pembelian" -> "▱"; "Piutang" -> "▤"; "Laporan" -> "⌁"; else -> "⚙" }

@Composable private fun BottomMenu(selected: String, onSelect: (String) -> Unit) {
    Surface(Modifier.fillMaxWidth().navigationBarsPadding(), color = Color.White, shadowElevation = 10.dp) {
        Row(Modifier.fillMaxWidth().padding(6.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf("Beranda", "Penjualan", "Produk", "Lainnya").forEach { item ->
                val active = selected == item || (item == "Lainnya" && selected !in listOf("Beranda", "Penjualan", "Produk"))
                Surface(Modifier.weight(1f).clickable { onSelect(if (item == "Lainnya") "Stok" else item) }, shape = RoundedCornerShape(12.dp), color = if (active) UiSoftBlue else Color.White) { Column(Modifier.padding(vertical = 7.dp), horizontalAlignment = Alignment.CenterHorizontally) { Text(if (item == "Lainnya") "•••" else iconFor(item), color = if (active) UiBlue else UiMuted, fontSize = 16.sp); Text(item, color = if (active) UiBlue else UiMuted, fontSize = 10.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Normal) } }
            }
        }
    }
}

@Composable private fun SalesScreen(tablet: Boolean) {
    var products by remember { mutableStateOf<List<PProduct>>(emptyList()) }
    var categories by remember { mutableStateOf<List<PCategory>>(emptyList()) }
    var prices by remember { mutableStateOf<List<PPrice>>(emptyList()) }
    var stocks by remember { mutableStateOf<List<PStock>>(emptyList()) }
    var methods by remember { mutableStateOf<List<PMethod>>(emptyList()) }
    var branch by remember { mutableStateOf<PBranch?>(null) }
    var location by remember { mutableStateOf<PLocation?>(null) }
    var cart by remember { mutableStateOf<List<PCart>>(emptyList()) }
    var search by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Semua") }
    var showCart by remember { mutableStateOf(false) }
    var showPayment by remember { mutableStateOf(false) }
    var success by remember { mutableStateOf<PResult?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        runCatching {
            val b = uiClient.from("businesses").select { filter { eq("is_active", true) } }.decodeList<Business>().first()
            val br = uiClient.from("branches").select { filter { eq("business_id", b.id); eq("is_active", true) } }.decodeList<Branch>().first()
            val loc = uiClient.from("locations").select { filter { eq("branch_id", br.id); eq("is_active", true) } }.decodeList<Location>().first()
            branch = PBranch(br.id, br.name); location = PLocation(loc.id, loc.name)
            products = uiClient.from("products").select { filter { eq("business_id", b.id); eq("is_active", true) } }.decodeList<PProduct>()
            categories = uiClient.from("categories").select { filter { eq("business_id", b.id); eq("is_active", true) } }.decodeList<PCategory>()
            val pl = uiClient.from("price_lists").select { filter { eq("business_id", b.id); eq("is_default", true); eq("is_active", true) } }.decodeList<PriceList>().firstOrNull()
            if (pl != null) prices = uiClient.from("product_prices").select { filter { eq("price_list_id", pl.id) } }.decodeList<PPrice>()
            methods = uiClient.from("payment_methods").select { filter { eq("business_id", b.id); eq("is_active", true) } }.decodeList<PMethod>()
            stocks = uiClient.from("stock_balances").select().decodeList<PStock>()
        }.onFailure { error = it.message ?: "Gagal memuat data" }
    }

    fun productPrice(p: PProduct): Long = prices.filter { it.matches(p) }.minByOrNull { it.min_qty }?.price ?: 0L
    fun productStock(p: PProduct): Double = stocks.firstOrNull { it.location_id == location?.id && it.product_id == p.id }?.qty_base ?: 0.0
    val visible = products.filter { p -> (search.isBlank() || p.name.contains(search, true) || p.sku.contains(search, true)) && (selectedCategory == "Semua" || categories.firstOrNull { it.id == p.category_id }?.name == selectedCategory) }
    val total = cart.sumOf { it.price * it.qty }
    val itemCount = cart.sumOf { it.qty }

    Column(Modifier.fillMaxSize().padding(if (tablet) 22.dp else 14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { Text("Penjualan Baru", fontSize = if (tablet) 24.sp else 20.sp, fontWeight = FontWeight.ExtraBold, color = UiNavy); Text("${location?.name ?: "Toko Utama"} • Pelanggan Umum", fontSize = 12.sp, color = UiMuted) }
            if (tablet) Surface(shape = RoundedCornerShape(11.dp), color = Color.White, border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE1E8F0))) { Text("▣  Scan Barcode", Modifier.padding(horizontal = 14.dp, vertical = 10.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = UiNavy) }
        }
        if (tablet) {
            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Catalog(visible, categories, selectedCategory, search, { search = it }, { selectedCategory = it }, { p -> cart = addCart(cart, p, productPrice(p)) }, ::productStock, Modifier.weight(1.55f))
                CartPanel(cart, total, { cart = changeCart(cart, it, -1) }, { cart = changeCart(cart, it, 1) }, { showPayment = true }, Modifier.weight(0.9f))
            }
        } else {
            Catalog(visible, categories, selectedCategory, search, { search = it }, { selectedCategory = it }, { p -> cart = addCart(cart, p, productPrice(p)) }, ::productStock, Modifier.weight(1f))
            if (cart.isNotEmpty()) Surface(Modifier.fillMaxWidth().clickable { showCart = true }, shape = RoundedCornerShape(16.dp), color = UiBlue, shadowElevation = 8.dp) { Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(Color.White.copy(alpha = .15f)), contentAlignment = Alignment.Center) { Text(itemCount.toString(), color = Color.White, fontWeight = FontWeight.ExtraBold) }; Column(Modifier.weight(1f).padding(start = 10.dp)) { Text("Keranjang", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold); Text(money(total), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold) }; Text("Detail pesanan  ›", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold) } }
        }
    }

    if (showCart) CartDialog(cart, total, { cart = changeCart(cart, it, -1) }, { cart = changeCart(cart, it, 1) }, { showCart = false; showPayment = true }, { showCart = false })
    if (showPayment) PaymentDialog(total, methods, { showPayment = false }) { payments ->
        val br = branch; val loc = location
        if (br == null || loc == null) error = "Data toko belum siap" else scope.launch {
            runCatching {
                val result = uiClient.postgrest.rpc("checkout_sale_multi_payment", buildJsonObject {
                    put("p_branch_id", br.id); put("p_location_id", loc.id); put("p_customer_id", null as String?)
                    put("p_items", buildJsonArray { cart.forEach { line -> add(buildJsonObject { put("product_id", line.product.id); put("unit_id", line.product.base_unit_id); put("sku", line.product.sku); put("name", line.product.name); put("qty", line.qty); put("conversion_to_base", 1); put("unit_price", line.price); put("hpp_unit", line.product.current_cost) }) } })
                    put("p_payments", buildJsonArray { payments.forEach { pay -> add(buildJsonObject { put("payment_method_id", pay.methodId); put("amount", pay.amount); put("cash_received", pay.cashReceived); put("reference", pay.reference); put("qris_confirmed", false) }) } })
                    put("p_idempotency_key", UUID.randomUUID().toString())
                }).decodeSingle<PResult>()
                success = result; cart = emptyList(); showPayment = false
            }.onFailure { error = it.message ?: "Checkout gagal" }
        }
    }
    success?.let { CheckoutDialog(it) { success = null } }
    error?.let { AlertDialog(onDismissRequest = { error = null }, confirmButton = { TextButton({ error = null }) { Text("OK") } }, title = { Text("Perhatian") }, text = { Text(it) }) }
}

@Composable private fun Catalog(products: List<PProduct>, categories: List<PCategory>, selected: String, search: String, onSearch: (String) -> Unit, onCategory: (String) -> Unit, onAdd: (PProduct) -> Unit, stockOf: (PProduct) -> Double, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(search, onSearch, Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(12.dp), placeholder = { Text("Cari produk / scan barcode", fontSize = 12.sp) })
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) { listOf("Semua").plus(categories.map { it.name }).forEach { c -> val active = c == selected; Surface(Modifier.clickable { onCategory(c) }, shape = RoundedCornerShape(10.dp), color = if (active) UiBlue else Color.White, border = if (!active) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE1E8F0)) else null) { Text(c, Modifier.padding(horizontal = 13.dp, vertical = 8.dp), fontSize = 11.sp, color = if (active) Color.White else UiMuted, fontWeight = if (active) FontWeight.Bold else FontWeight.Medium) } } }
        if (products.isEmpty()) EmptyCard("Produk tidak ditemukan", "Coba pencarian atau kategori lain.") else LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(9.dp), horizontalArrangement = Arrangement.spacedBy(9.dp)) { items(products, key = { it.id }) { ProductCard(it, stockOf(it), onAdd) } }
    }
}

@Composable private fun ProductCard(p: PProduct, stock: Double, onAdd: (PProduct) -> Unit) {
    Card(Modifier.fillMaxWidth().clickable { onAdd(p) }, shape = RoundedCornerShape(15.dp), colors = CardDefaults.cardColors(Color.White)) {
        Column(Modifier.padding(10.dp)) {
            Box(Modifier.fillMaxWidth().height(96.dp).clip(RoundedCornerShape(12.dp)).background(UiSoftBlue), contentAlignment = Alignment.Center) { Text(p.name.take(1).uppercase(), fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = UiBlue); Box(Modifier.align(Alignment.BottomEnd).padding(7.dp).size(29.dp).clip(RoundedCornerShape(9.dp)).background(UiBlue), contentAlignment = Alignment.Center) { Text("+", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold) } }
            Spacer(Modifier.height(8.dp)); Text(p.name, maxLines = 1, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = UiNavy); Text(p.sku, fontSize = 9.sp, color = UiMuted); Text("Stok ${stock.toInt()}", fontSize = 9.sp, color = if (stock <= p.min_stock) UiWarning else UiMuted)
        }
    }
}

@Composable private fun CartPanel(cart: List<PCart>, total: Long, onMinus: (PCart) -> Unit, onPlus: (PCart) -> Unit, onPay: () -> Unit, modifier: Modifier) {
    Card(modifier.fillMaxHeight(), shape = RoundedCornerShape(17.dp), colors = CardDefaults.cardColors(Color.White)) {
        Column(Modifier.fillMaxSize().padding(15.dp)) { Text("Keranjang", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = UiNavy); Text("${cart.sumOf { it.qty }} item", fontSize = 11.sp, color = UiMuted); HorizontalDivider(Modifier.padding(vertical = 10.dp)); if (cart.isEmpty()) EmptyCard("Keranjang kosong", "Tambahkan produk dari katalog.") else { LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(9.dp)) { items(cart, key = { it.product.id }) { CartRow(it, onMinus, onPlus) } }; HorizontalDivider(Modifier.padding(vertical = 10.dp)); Summary(total, 0, 0); Spacer(Modifier.height(10.dp)); Button(onClick = onPay, Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = UiBlue)) { Text("Bayar • ${money(total)}", fontWeight = FontWeight.ExtraBold) } } }
    }
}

@Composable private fun CartDialog(cart: List<PCart>, total: Long, onMinus: (PCart) -> Unit, onPlus: (PCart) -> Unit, onPay: () -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) { Surface(Modifier.fillMaxWidth().heightIn(max = 680.dp), shape = RoundedCornerShape(22.dp), color = Color.White) { Column(Modifier.fillMaxWidth().padding(18.dp)) { Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("Detail Pesanan", fontSize = 21.sp, fontWeight = FontWeight.ExtraBold, color = UiNavy); Text("${cart.sumOf { it.qty }} item", fontSize = 11.sp, color = UiMuted) }; TextButton(onClick = onDismiss) { Text("Tutup") } }; HorizontalDivider(Modifier.padding(vertical = 8.dp)); LazyColumn(Modifier.heightIn(max = 330.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { items(cart, key = { it.product.id }) { CartRow(it, onMinus, onPlus) } }; HorizontalDivider(Modifier.padding(vertical = 10.dp)); Summary(total, 0, 0); Card(Modifier.fillMaxWidth().padding(top = 8.dp), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(UiSoftBlue)) { Column(Modifier.padding(11.dp)) { Detail("Dibayar", "Rp 0"); Detail("Kembalian", "Rp 0") } }; Spacer(Modifier.height(12.dp)); Button(onClick = onPay, Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = UiBlue)) { Text("Lanjut Pembayaran", fontWeight = FontWeight.ExtraBold) } } } }
}

@Composable private fun CartRow(line: PCart, onMinus: (PCart) -> Unit, onPlus: (PCart) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(46.dp).clip(RoundedCornerShape(11.dp)).background(UiSoftBlue), contentAlignment = Alignment.Center) { Text(line.product.name.take(1).uppercase(), color = UiBlue, fontWeight = FontWeight.ExtraBold) }; Column(Modifier.weight(1f).padding(start = 9.dp)) { Text(line.product.name, maxLines = 1, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = UiNavy); Text(money(line.price), fontSize = 10.sp, color = UiMuted) }; Row(verticalAlignment = Alignment.CenterVertically) { QtyButton("−") { onMinus(line) }; Text(line.qty.toString(), Modifier.width(27.dp), textAlign = TextAlign.Center, fontSize = 12.sp, fontWeight = FontWeight.Bold); QtyButton("+") { onPlus(line) } }; Text(money(line.price * line.qty), Modifier.width(72.dp), textAlign = TextAlign.End, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = UiNavy) }
}

@Composable private fun QtyButton(label: String, onClick: () -> Unit) { Surface(Modifier.size(29.dp).clickable { onClick() }, shape = RoundedCornerShape(8.dp), color = Color(0xFFF1F4F8)) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(label, color = UiNavy, fontWeight = FontWeight.Bold) } } }

@Composable private fun Summary(total: Long, paid: Long, change: Long) { Detail("Subtotal", money(total)); Detail("Diskon", "Rp 0"); Detail("Pajak (0%)", "Rp 0"); Spacer(Modifier.height(4.dp)); Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("TOTAL", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = UiNavy); Text(money(total), fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = UiNavy) }; if (paid > 0) { Detail("Dibayar", money(paid)); Detail("Kembalian", money(change)) } }
@Composable private fun Detail(label: String, value: String) { Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) { Text(label, fontSize = 11.sp, color = UiMuted); Text(value, fontSize = 11.sp, color = UiNavy, fontWeight = FontWeight.SemiBold) } }

@Composable private fun PaymentDialog(total: Long, methods: List<PMethod>, onDismiss: () -> Unit, onSubmit: (List<PPayment>) -> Unit) {
    var selected by remember(methods) { mutableStateOf(methods.firstOrNull { it.code == "CASH" } ?: methods.firstOrNull()) }
    var amount by remember(total) { mutableStateOf(total.toString()) }
    var received by remember(total) { mutableStateOf(total.toString()) }
    var reference by remember { mutableStateOf("") }
    var payments by remember { mutableStateOf<List<PPayment>>(emptyList()) }
    val paid = payments.sumOf { it.amount }; val remaining = (total - paid).coerceAtLeast(0L); val current = amount.toLongOrNull() ?: 0L; val cash = received.toLongOrNull() ?: 0L; val change = if (selected?.code == "CASH") (cash - current).coerceAtLeast(0L) else 0L
    Dialog(onDismissRequest = onDismiss) { Surface(Modifier.fillMaxWidth().heightIn(max = 720.dp), shape = RoundedCornerShape(22.dp), color = Color.White) { Column(Modifier.fillMaxWidth().padding(18.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("Pembayaran", fontSize = 21.sp, fontWeight = FontWeight.ExtraBold, color = UiNavy); Text("Selesaikan transaksi", fontSize = 11.sp, color = UiMuted) }; TextButton(onClick = onDismiss) { Text("Batal") } }
        Card(Modifier.fillMaxWidth().padding(vertical = 10.dp), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(UiSoftBlue)) { Column(Modifier.padding(14.dp)) { Text("TOTAL PEMBAYARAN", fontSize = 10.sp, color = UiMuted, fontWeight = FontWeight.Bold); Text(money(total), fontSize = 27.sp, fontWeight = FontWeight.ExtraBold, color = UiNavy); Detail("Sisa", money(remaining)) } }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) { methods.forEach { m -> val active = selected?.id == m.id; Surface(Modifier.clickable { selected = m }, shape = RoundedCornerShape(11.dp), color = if (active) UiBlue else Color.White, border = if (!active) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE1E8F0)) else null) { Column(Modifier.padding(horizontal = 14.dp, vertical = 9.dp), horizontalAlignment = Alignment.CenterHorizontally) { Text(paymentGlyph(m.code), color = if (active) Color.White else UiBlue, fontSize = 17.sp); Text(m.name, color = if (active) Color.White else UiNavy, fontSize = 10.sp, fontWeight = FontWeight.Bold) } } } }
        if (selected?.code == "QRIS") { QrBox(remaining); Text("QRIS tetap pending sampai pembayaran dikonfirmasi.", Modifier.padding(top = 7.dp), fontSize = 11.sp, color = UiWarning, fontWeight = FontWeight.Medium) } else {
            OutlinedTextField(amount, { amount = it.filter(Char::isDigit) }, Modifier.fillMaxWidth().padding(top = 10.dp), label = { Text("Nominal pembayaran") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, shape = RoundedCornerShape(11.dp))
            if (selected?.code == "CASH") { OutlinedTextField(received, { received = it.filter(Char::isDigit) }, Modifier.fillMaxWidth().padding(top = 8.dp), label = { Text("Uang diterima") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, shape = RoundedCornerShape(11.dp)); Card(Modifier.fillMaxWidth().padding(top = 8.dp), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(Color(0xFFEAF8F0))) { Column(Modifier.padding(12.dp)) { Text("KEMBALIAN", fontSize = 10.sp, color = UiSuccess, fontWeight = FontWeight.Bold); Text(money(change), fontSize = 21.sp, color = UiSuccess, fontWeight = FontWeight.ExtraBold) } } }
            if (selected?.code == "TRANSFER") OutlinedTextField(reference, { reference = it }, Modifier.fillMaxWidth().padding(top = 8.dp), label = { Text("Referensi transfer") }, singleLine = true, shape = RoundedCornerShape(11.dp))
        }
        if (payments.isNotEmpty()) { HorizontalDivider(Modifier.padding(vertical = 10.dp)); Text("Pembayaran ditambahkan", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = UiNavy); payments.forEach { p -> Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) { Text(methods.firstOrNull { it.id == p.methodId }?.name ?: "Payment", fontSize = 11.sp); Text(money(p.amount), fontSize = 11.sp, fontWeight = FontWeight.Bold) } } }
        Spacer(Modifier.height(10.dp)); Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Dibayar", fontSize = 12.sp, color = UiMuted); Text(money(paid), fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = UiNavy) }
        Spacer(Modifier.height(10.dp)); Button(onClick = { if (remaining == 0L) onSubmit(payments) else { val m = selected; val v = current.coerceAtMost(remaining); if (m != null && v > 0) { payments = payments + PPayment(m.id, v, if (m.code == "CASH") cash else v, reference.ifBlank { null }); amount = (remaining - v).toString(); received = (remaining - v).toString(); reference = "" } } }, enabled = selected != null, Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = UiBlue)) { Text(if (remaining == 0L) "Selesaikan Transaksi" else if (selected?.code == "QRIS") "Tambahkan QRIS" else "Tambahkan Pembayaran", fontWeight = FontWeight.ExtraBold) }
    } } }
}

private fun paymentGlyph(code: String) = when (code) { "CASH" -> "▣"; "QRIS" -> "▦"; "TRANSFER" -> "⇄"; else -> "₱" }

@Composable private fun QrBox(amount: Long) { Card(Modifier.fillMaxWidth().padding(top = 10.dp), shape = RoundedCornerShape(15.dp), colors = CardDefaults.cardColors(Color.White)) { Column(Modifier.fillMaxWidth().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) { Text("Scan untuk membayar", fontSize = 11.sp, color = UiMuted); Spacer(Modifier.height(6.dp)); Box(Modifier.size(185.dp).border(1.dp, Color(0xFFE1E8F0)), contentAlignment = Alignment.Center) { FakeQr("DEMO-${amount}") }; Spacer(Modifier.height(6.dp)); Text(money(amount), fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = UiNavy); Text("OPEN • Menunggu pembayaran", fontSize = 10.sp, color = UiWarning, fontWeight = FontWeight.Bold) } } }

@Composable private fun FakeQr(seed: String) { androidx.compose.foundation.Canvas(Modifier.size(160.dp)) { drawFakeQr(this, seed) } }
private fun drawFakeQr(draw: DrawScope, seed: String) { val n = 25; val cell = draw.size.minDimension / n; val h = seed.hashCode().toLong(); fun on(x: Int, y: Int): Boolean { var v = h xor (x * 1103515245L) xor (y * 12345L); v = v xor (v ushr 17); return (v and 1L) == 0L }; fun finder(x0: Int, y0: Int) { for (y in 0..6) for (x in 0..6) { if (x == 0 || y == 0 || x == 6 || y == 6 || (x in 2..4 && y in 2..4)) draw.drawRect(Color.Black, androidx.compose.ui.geometry.Offset((x0 + x) * cell, (y0 + y) * cell), androidx.compose.ui.geometry.Size(cell, cell)) } }; finder(0, 0); finder(18, 0); finder(0, 18); for (y in 0 until n) for (x in 0 until n) { val reserved = (x < 8 && y < 8) || (x >= 17 && y < 8) || (x < 8 && y >= 17); if (!reserved && on(x, y)) draw.drawRect(Color.Black, androidx.compose.ui.geometry.Offset(x * cell, y * cell), androidx.compose.ui.geometry.Size(cell, cell)) } }

@Composable private fun CheckoutDialog(result: PResult, onDone: () -> Unit) { AlertDialog(onDismissRequest = onDone, title = { Text("Pembayaran Berhasil", fontWeight = FontWeight.ExtraBold) }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { Text(result.sale_no, color = UiBlue, fontWeight = FontWeight.Bold); Text(money(result.total_amount), fontSize = 25.sp, color = UiNavy, fontWeight = FontWeight.ExtraBold); Status(result.sale_status); Detail("Dibayar", money(result.paid_amount)); Detail("Kembalian", money(result.change_amount)); Text("Struk siap untuk dicetak.", fontSize = 11.sp, color = UiMuted) } }, confirmButton = { Button(onClick = onDone, colors = ButtonDefaults.buttonColors(containerColor = UiBlue)) { Text("Transaksi Baru") } }) }
@Composable private fun Status(s: String) { val c = if (s.uppercase() in listOf("COMPLETED", "PAID")) UiSuccess else UiWarning; Surface(shape = RoundedCornerShape(18.dp), color = c.copy(alpha = .12f)) { Text(s, Modifier.padding(horizontal = 10.dp, vertical = 5.dp), color = c, fontSize = 10.sp, fontWeight = FontWeight.Bold) } }
@Composable private fun EmptyCard(title: String, subtitle: String) { Card(Modifier.fillMaxWidth().height(170.dp), shape = RoundedCornerShape(15.dp), colors = CardDefaults.cardColors(Color.White)) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("□", fontSize = 30.sp, color = UiBlue); Text(title, fontWeight = FontWeight.Bold, color = UiNavy); Text(subtitle, fontSize = 11.sp, color = UiMuted) } } } }
@Composable private fun OtherScreen(title: String) { Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { Text(title, fontSize = 25.sp, fontWeight = FontWeight.ExtraBold, color = UiNavy); Text("Modul POS tetap menggunakan struktur data yang sudah ada.", color = UiMuted); EmptyCard("Modul $title", "UI detail modul berikutnya akan mengikuti design system yang sama.") } }

private fun addCart(cart: List<PCart>, product: PProduct, price: Long): List<PCart> = if (cart.any { it.product.id == product.id }) cart.map { if (it.product.id == product.id) it.copy(qty = it.qty + 1) else it } else cart + PCart(product, price, 1)
private fun changeCart(cart: List<PCart>, line: PCart, delta: Int): List<PCart> = cart.mapNotNull { if (it.product.id != line.product.id) it else { val q = it.qty + delta; if (q <= 0) null else it.copy(qty = q) } }

@Serializable private data class Business(val id: String, val code: String, val name: String, val is_active: Boolean = true)
@Serializable private data class Branch(val id: String, val business_id: String, val code: String, val name: String, val is_active: Boolean = true)
@Serializable private data class Location(val id: String, val branch_id: String, val code: String, val name: String, val is_active: Boolean = true)
@Serializable private data class PriceList(val id: String, val business_id: String, val name: String, val is_default: Boolean = false, val is_active: Boolean = true)
