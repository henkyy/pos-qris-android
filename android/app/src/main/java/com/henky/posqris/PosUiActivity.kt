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
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

private val posSupabase = createSupabaseClient(BuildConfig.SUPABASE_URL, BuildConfig.SUPABASE_PUBLISHABLE_KEY) { install(Postgrest) }

private val Navy = Color(0xFF021024)
private val Blue = Color(0xFF1E63D8)
private val BlueSoft = Color(0xFFEAF2FF)
private val PageBg = Color(0xFFF6F9FD)
private val TextMuted = Color(0xFF718096)
private val Success = Color(0xFF159957)
private val Warning = Color(0xFFD99500)
private val Danger = Color(0xFFD84A4A)

@Serializable private data class UiProduct(
    val id: String,
    val sku: String,
    val name: String,
    val category_id: String? = null,
    val base_unit_id: String,
    val current_cost: Long = 0,
    val min_stock: Double = 0.0
)
@Serializable private data class UiCategory(val id: String, val code: String, val name: String)
@Serializable private data class UiPrice(val product_id: String, val price: Long, val min_qty: Double = 1.0)
@Serializable private data class UiBranch(val id: String, val name: String)
@Serializable private data class UiLocation(val id: String, val name: String)
@Serializable private data class UiPaymentMethod(val id: String, val code: String, val name: String, val method_type: String)
@Serializable private data class UiCustomer(val id: String, val code: String, val name: String)
@Serializable private data class UiStock(val location_id: String, val product_id: String, val qty_base: Double = 0.0)
@Serializable private data class UiCheckoutResult(val sale_id: String, val sale_no: String, val total_amount: Long, val paid_amount: Long, val change_amount: Long, val sale_status: String)

private data class UiCartLine(val product: UiProduct, val price: Long, val qty: Int)
private data class UiPaymentDraft(val methodId: String, val amount: Long, val cashReceived: Long, val reference: String?)

private fun idr(value: Long): String = "Rp " + value.toString().reversed().chunked(3).joinToString(".").reversed()
private fun UiPrice.forProduct(product: UiProduct) = product_id == product.id

class PosUiActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { PosUiApp() }
    }
}

@Composable
private fun PosUiApp() {
    var tab by remember { mutableStateOf("Penjualan") }
    val tablet = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp >= 600
    Surface(Modifier.fillMaxSize(), color = PageBg) {
        if (tablet) {
            Row(Modifier.fillMaxSize()) {
                UiSidebar(tab) { tab = it }
                Box(Modifier.weight(1f).fillMaxHeight()) {
                    if (tab == "Penjualan") PosHome(tablet = true) else PlaceholderPage(tab)
                }
            }
        } else {
            Column(Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    if (tab == "Penjualan") PosHome(tablet = false) else PlaceholderPage(tab)
                }
                UiBottomNav(tab) { tab = it }
            }
        }
    }
}

@Composable
private fun UiSidebar(selected: String, onSelect: (String) -> Unit) {
    Surface(Modifier.width(214.dp).fillMaxHeight(), color = Navy) {
        Column(Modifier.fillMaxSize().padding(15.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(Modifier.padding(7.dp, 7.dp, 7.dp, 17.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(36.dp).clip(RoundedCornerShape(11.dp)).background(Blue), contentAlignment = Alignment.Center) {
                    Text("P", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                }
                Column(Modifier.padding(start = 9.dp)) {
                    Text("POS QRIS", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
                    Text("Toko Demo", color = Color(0xFFB8C7D9), fontSize = 11.sp)
                }
            }
            listOf("Beranda", "Penjualan", "Pesanan", "Produk", "Stok", "Pelanggan", "Supplier", "Pembelian", "Piutang", "Laporan", "Pengaturan").forEach { item ->
                val active = item == selected
                Surface(Modifier.fillMaxWidth().clickable { onSelect(item) }, shape = RoundedCornerShape(11.dp), color = if (active) Blue else Color.Transparent) {
                    Row(Modifier.padding(horizontal = 11.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(uiIcon(item), color = if (active) Color.White else Color(0xFFB8C7D9), modifier = Modifier.width(27.dp), fontSize = 15.sp)
                        Text(item, color = if (active) Color.White else Color(0xFFE3EAF2), fontSize = 13.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Medium)
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFF0A1D35)) {
                Row(Modifier.padding(11.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(34.dp).clip(RoundedCornerShape(17.dp)).background(Color.White), contentAlignment = Alignment.Center) { Text("A", color = Navy, fontWeight = FontWeight.Bold) }
                    Column(Modifier.padding(start = 9.dp)) { Text("Admin", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp); Text("Toko Utama", color = Color(0xFF9EB0C4), fontSize = 10.sp) }
                }
            }
        }
    }
}

private fun uiIcon(label: String) = when (label) {
    "Beranda" -> "⌂"
    "Penjualan" -> "▣"
    "Pesanan" -> "≡"
    "Produk" -> "□"
    "Stok" -> "◫"
    "Pelanggan" -> "♙"
    "Supplier" -> "♙"
    "Pembelian" -> "▱"
    "Piutang" -> "▤"
    "Laporan" -> "⌁"
    else -> "⚙"
}

@Composable
private fun UiBottomNav(selected: String, onSelect: (String) -> Unit) {
    Surface(Modifier.fillMaxWidth().navigationBarsPadding(), color = Color.White, shadowElevation = 10.dp) {
        Row(Modifier.fillMaxWidth().padding(6.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            listOf("Beranda", "Penjualan", "Produk", "Lainnya").forEach { item ->
                val active = selected == item || (item == "Lainnya" && selected !in listOf("Beranda", "Penjualan", "Produk"))
                Surface(Modifier.weight(1f).clickable { onSelect(if (item == "Lainnya") "Stok" else item) }, shape = RoundedCornerShape(12.dp), color = if (active) BlueSoft else Color.White) {
                    Column(Modifier.padding(vertical = 7.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(if (item == "Lainnya") "•••" else uiIcon(item), color = if (active) Blue else TextMuted, fontSize = 16.sp)
                        Text(item, color = if (active) Blue else TextMuted, fontSize = 10.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }
        }
    }
}

@Composable
private fun PosHome(tablet: Boolean) {
    var products by remember { mutableStateOf<List<UiProduct>>(emptyList()) }
    var categories by remember { mutableStateOf<List<UiCategory>>(emptyList()) }
    var prices by remember { mutableStateOf<List<UiPrice>>(emptyList()) }
    var stocks by remember { mutableStateOf<List<UiStock>>(emptyList()) }
    var methods by remember { mutableStateOf<List<UiPaymentMethod>>(emptyList()) }
    var branch by remember { mutableStateOf<UiBranch?>(null) }
    var location by remember { mutableStateOf<UiLocation?>(null) }
    var customer by remember { mutableStateOf<UiCustomer?>(null) }
    var cart by remember { mutableStateOf<List<UiCartLine>>(emptyList()) }
    var search by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Semua") }
    var cartOpen by remember { mutableStateOf(false) }
    var paymentOpen by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<UiCheckoutResult?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        runCatching {
            val business = posSupabase.from("businesses").select { filter { eq("is_active", true) } }.decodeList<Business>().first()
            val br = posSupabase.from("branches").select { filter { eq("business_id", business.id); eq("is_active", true) } }.decodeList<Branch>().first()
            val loc = posSupabase.from("locations").select { filter { eq("branch_id", br.id); eq("is_active", true) } }.decodeList<Location>().first()
            branch = UiBranch(br.id, br.name)
            location = UiLocation(loc.id, loc.name)
            products = posSupabase.from("products").select { filter { eq("business_id", business.id); eq("is_active", true) } }.decodeList<UiProduct>()
            categories = posSupabase.from("categories").select { filter { eq("business_id", business.id); eq("is_active", true) } }.decodeList<UiCategory>()
            val pl = posSupabase.from("price_lists").select { filter { eq("business_id", business.id); eq("is_default", true); eq("is_active", true) } }.decodeList<PriceList>().firstOrNull()
            if (pl != null) prices = posSupabase.from("product_prices").select { filter { eq("price_list_id", pl.id) } }.decodeList<UiPrice>()
            methods = posSupabase.from("payment_methods").select { filter { eq("business_id", business.id); eq("is_active", true) } }.decodeList<UiPaymentMethod>()
            customer = posSupabase.from("customers").select { filter { eq("business_id", business.id); eq("code", "CUS001") } }.decodeList<UiCustomer>().firstOrNull()
            stocks = posSupabase.from("stock_balances").select().decodeList<UiStock>()
        }.onFailure { error = it.message ?: "Gagal memuat data" }
    }

    fun priceOf(p: UiProduct): Long = prices.filter { it.forProduct(p) }.minByOrNull { it.min_qty }?.price ?: 0L
    fun stockOf(p: UiProduct): Double = stocks.firstOrNull { it.location_id == location?.id && it.product_id == p.id }?.qty_base ?: 0.0
    val filtered = products.filter { p ->
        (search.isBlank() || p.name.contains(search, true) || p.sku.contains(search, true)) &&
            (category == "Semua" || categories.firstOrNull { it.id == p.category_id }?.name == category)
    }
    val total = cart.sumOf { it.price * it.qty }
    val itemCount = cart.sumOf { it.qty }

    Column(Modifier.fillMaxSize().padding(if (tablet) 22.dp else 14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(if (tablet) "Penjualan Baru" else "Penjualan Baru", fontSize = if (tablet) 24.sp else 20.sp, fontWeight = FontWeight.ExtraBold, color = Navy)
                Text("${location?.name ?: "Toko Utama"} • ${customer?.name ?: "Pelanggan Umum"}", color = TextMuted, fontSize = 12.sp)
            }
            if (tablet) {
                Surface(shape = RoundedCornerShape(11.dp), color = Color.White, border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE3EAF2))) {
                    Text("▣  Scan Barcode", Modifier.padding(horizontal = 14.dp, vertical = 10.dp), fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                }
            }
        }

        if (tablet) {
            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                ProductArea(filtered, categories, category, search, ::Unit, { search = it }, { category = it }, { p -> cart = addLine(cart, p, priceOf(p)) }, ::stockOf, Modifier.weight(1.6f))
                CartPanel(cart, total, { cart = changeQty(cart, it, -1) }, { cart = changeQty(cart, it, 1) }, { paymentOpen = true }, Modifier.weight(0.9f))
            }
        } else {
            ProductArea(filtered, categories, category, search, ::Unit, { search = it }, { category = it }, { p -> cart = addLine(cart, p, priceOf(p)) }, ::stockOf, Modifier.weight(1f))
            if (cart.isNotEmpty()) {
                Surface(Modifier.fillMaxWidth().clickable { cartOpen = true }, shape = RoundedCornerShape(16.dp), color = Blue, shadowElevation = 8.dp) {
                    Row(Modifier.padding(horizontal = 15.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(Color.White.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) { Text("$itemCount", color = Color.White, fontWeight = FontWeight.ExtraBold) }
                        Column(Modifier.weight(1f).padding(start = 10.dp)) { Text("Keranjang", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp); Text(idr(total), color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp) }
                        Text("Lihat pesanan  ›", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }

    if (cartOpen) CartDetailSheet(cart, total, { cart = changeQty(cart, it, -1) }, { cart = changeQty(cart, it, 1) }, { cartOpen = false; paymentOpen = true }, { cartOpen = false })
    if (paymentOpen) PaymentSheet(total, methods, onDismiss = { paymentOpen = false }) { payments ->
        val br = branch
        val loc = location
        if (br == null || loc == null) error = "Data toko belum siap" else scope.launch {
            runCatching {
                val out = posSupabase.postgrest.rpc("checkout_sale_multi_payment", buildJsonObject {
                    put("p_branch_id", br.id)
                    put("p_location_id", loc.id)
                    put("p_customer_id", customer?.id)
                    put("p_items", buildJsonArray { cart.forEach { line -> add(buildJsonObject { put("product_id", line.product.id); put("unit_id", line.product.base_unit_id); put("sku", line.product.sku); put("name", line.product.name); put("qty", line.qty); put("conversion_to_base", 1); put("unit_price", line.price); put("hpp_unit", line.product.current_cost) }) } })
                    put("p_payments", buildJsonArray { payments.forEach { pay -> add(buildJsonObject { put("payment_method_id", pay.methodId); put("amount", pay.amount); put("cash_received", pay.cashReceived); put("reference", pay.reference); put("qris_confirmed", false) }) } })
                    put("p_idempotency_key", UUID.randomUUID().toString())
                }).decodeSingle<UiCheckoutResult>()
                result = out
                cart = emptyList()
                paymentOpen = false
            }.onFailure { error = it.message ?: "Checkout gagal" }
        }
    }
    result?.let { CheckoutSuccess(it, onDone = { result = null }) }
    error?.let { msg -> AlertDialog(onDismissRequest = { error = null }, confirmButton = { TextButton({ error = null }) { Text("OK") } }, title = { Text("Perhatian") }, text = { Text(msg) }) }
}

@Composable
private fun ProductArea(
    filtered: List<UiProduct>, categories: List<UiCategory>, selectedCategory: String, search: String,
    dummy: () -> Unit, onSearch: (String) -> Unit, onCategory: (String) -> Unit,
    onAdd: (UiProduct) -> Unit, stockOf: (UiProduct) -> Double, modifier: Modifier
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(search, onSearch, Modifier.weight(1f), placeholder = { Text("Cari produk atau scan barcode", fontSize = 12.sp) }, singleLine = true, shape = RoundedCornerShape(12.dp))
            Surface(shape = RoundedCornerShape(12.dp), color = Color.White, border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE1E8F0))) { Text("⌕", Modifier.padding(horizontal = 13.dp, vertical = 13.dp), fontSize = 18.sp, color = TextMuted) }
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            listOf("Semua").plus(categories.map { it.name }).forEach { label ->
                val active = selectedCategory == label
                Surface(Modifier.clickable { onCategory(label) }, shape = RoundedCornerShape(10.dp), color = if (active) Blue else Color.White, border = if (!active) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE3EAF2)) else null) {
                    Text(label, Modifier.padding(horizontal = 13.dp, vertical = 8.dp), fontSize = 11.sp, color = if (active) Color.White else TextMuted, fontWeight = if (active) FontWeight.Bold else FontWeight.Medium)
                }
            }
        }
        if (filtered.isEmpty()) {
            EmptyState("Produk tidak ditemukan", "Coba kata pencarian atau kategori lain.")
        } else {
            LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(9.dp), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                items(filtered, key = { it.id }) { p -> ProductTile(p, stockOf(p), onAdd) }
            }
        }
    }
}

@Composable
private fun ProductTile(product: UiProduct, stock: Double, onAdd: (UiProduct) -> Unit) {
    val price = remember(product.id) { product }
    Card(Modifier.fillMaxWidth().clickable { onAdd(product) }, shape = RoundedCornerShape(15.dp), colors = CardDefaults.cardColors(Color.White), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Column(Modifier.padding(10.dp)) {
            Box(Modifier.fillMaxWidth().height(94.dp).clip(RoundedCornerShape(12.dp)).background(BlueSoft), contentAlignment = Alignment.Center) {
                Text(product.name.take(1).uppercase(), fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = Blue)
                Box(Modifier.align(Alignment.BottomEnd).padding(7.dp).size(29.dp).clip(RoundedCornerShape(9.dp)).background(Blue), contentAlignment = Alignment.Center) { Text("+", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp) }
            }
            Spacer(Modifier.height(8.dp))
            Text(product.name, maxLines = 1, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Navy)
            Text(product.sku, fontSize = 9.sp, color = TextMuted)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                Text("Stok ${stock.toInt()}", fontSize = 9.sp, color = if (stock <= product.min_stock) Warning else TextMuted)
                Text("Tap +", fontSize = 9.sp, color = Blue, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun CartPanel(cart: List<UiCartLine>, total: Long, onMinus: (UiCartLine) -> Unit, onPlus: (UiCartLine) -> Unit, onPay: () -> Unit, modifier: Modifier) {
    Card(modifier.fillMaxHeight(), shape = RoundedCornerShape(17.dp), colors = CardDefaults.cardColors(Color.White), elevation = CardDefaults.cardElevation(1.dp)) {
        Column(Modifier.fillMaxSize().padding(15.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { Text("Keranjang", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Navy); Text("${cart.sumOf { it.qty }} item", fontSize = 11.sp, color = TextMuted) }
                Text("${cart.size}", color = Blue, fontWeight = FontWeight.ExtraBold)
            }
            HorizontalDivider(Modifier.padding(vertical = 10.dp), color = Color(0xFFE9EEF4))
            if (cart.isEmpty()) EmptyState("Keranjang kosong", "Tambahkan produk dari katalog.") else {
                LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) { items(cart, key = { it.product.id }) { line -> CartLineRow(line, onMinus, onPlus) } }
                HorizontalDivider(Modifier.padding(vertical = 10.dp), color = Color(0xFFE9EEF4))
                Summary(total, 0, 0)
                Spacer(Modifier.height(10.dp))
                Button(onClick = onPay, Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Blue)) { Text("Bayar  •  ${idr(total)}", fontWeight = FontWeight.ExtraBold) }
            }
        }
    }
}

@Composable
private fun CartDetailSheet(cart: List<UiCartLine>, total: Long, onMinus: (UiCartLine) -> Unit, onPlus: (UiCartLine) -> Unit, onPay: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Detail Pesanan", fontWeight = FontWeight.ExtraBold, color = Navy) }, text = {
        Column(Modifier.heightIn(max = 520.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Text("${cart.sumOf { it.qty }} item", fontSize = 12.sp, color = TextMuted)
            LazyColumn(Modifier.heightIn(max = 280.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { items(cart) { line -> CartLineRow(line, onMinus, onPlus) } }
            HorizontalDivider()
            Summary(total, 0, 0)
            Spacer(Modifier.height(2.dp))
            DetailAmount("Dibayar", "Rp 0")
            DetailAmount("Kembalian", "Rp 0")
        }
    }, confirmButton = { Button(onClick = onPay, enabled = cart.isNotEmpty(), shape = RoundedCornerShape(10.dp), colors = ButtonDefaults.buttonColors(containerColor = Blue)) { Text("Lanjut Bayar") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Kembali") } })
}

@Composable
private fun CartLineRow(line: UiCartLine, onMinus: (UiCartLine) -> Unit, onPlus: (UiCartLine) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(43.dp).clip(RoundedCornerShape(10.dp)).background(BlueSoft), contentAlignment = Alignment.Center) { Text(line.product.name.take(1).uppercase(), color = Blue, fontWeight = FontWeight.Bold) }
        Column(Modifier.weight(1f).padding(start = 9.dp)) { Text(line.product.name, maxLines = 1, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Navy); Text(idr(line.price), fontSize = 10.sp, color = TextMuted) }
        Row(verticalAlignment = Alignment.CenterVertically) {
            SmallQty("−") { onMinus(line) }
            Text(line.qty.toString(), Modifier.width(27.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            SmallQty("+") { onPlus(line) }
        }
        Text(idr(line.price * line.qty), Modifier.width(74.dp), fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.End)
    }
}

@Composable private fun SmallQty(label: String, onClick: () -> Unit) { Surface(Modifier.size(28.dp).clickable { onClick() }, shape = RoundedCornerShape(8.dp), color = Color(0xFFF1F4F8)) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(label, fontWeight = FontWeight.Bold, color = Navy) } } }

@Composable
private fun Summary(total: Long, paid: Long, change: Long) {
    DetailAmount("Subtotal", idr(total))
    DetailAmount("Diskon", "Rp 0")
    DetailAmount("Pajak (0%)", "Rp 0")
    Spacer(Modifier.height(3.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text("TOTAL", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Navy); Text(idr(total), fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Navy) }
    if (paid > 0) { DetailAmount("Dibayar", idr(paid)); DetailAmount("Kembalian", idr(change)) }
}

@Composable private fun DetailAmount(label: String, value: String) { Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) { Text(label, fontSize = 11.sp, color = TextMuted); Text(value, fontSize = 11.sp, color = Navy, fontWeight = FontWeight.SemiBold) } }

@Composable
private fun PaymentSheet(total: Long, methods: List<UiPaymentMethod>, onDismiss: () -> Unit, onSubmit: (List<UiPaymentDraft>) -> Unit) {
    var selected by remember(methods) { mutableStateOf(methods.firstOrNull { it.code == "CASH" } ?: methods.firstOrNull()) }
    var amount by remember(total) { mutableStateOf(total.toString()) }
    var received by remember(total) { mutableStateOf(total.toString()) }
    var reference by remember { mutableStateOf("") }
    var drafts by remember { mutableStateOf<List<UiPaymentDraft>>(emptyList()) }
    var qrisMode by remember { mutableStateOf(false) }
    val paid = drafts.sumOf { it.amount }
    val remaining = (total - paid).coerceAtLeast(0L)
    val cashReceived = received.toLongOrNull() ?: 0L
    val change = if (selected?.code == "CASH") (cashReceived - (amount.toLongOrNull() ?: 0L)).coerceAtLeast(0L) else 0L

    AlertDialog(onDismissRequest = onDismiss, title = { Text("Pembayaran", fontWeight = FontWeight.ExtraBold, color = Navy) }, text = {
        Column(Modifier.heightIn(max = 570.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(BlueSoft)) { Column(Modifier.fillMaxWidth().padding(14.dp)) { Text("Total Pembayaran", fontSize = 11.sp, color = TextMuted); Text(idr(total), fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = Navy); Spacer(Modifier.height(4.dp)); Text("Sisa ${idr(remaining)}", fontSize = 12.sp, color = Blue, fontWeight = FontWeight.Bold) } }
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                methods.forEach { method ->
                    val active = selected?.id == method.id
                    Surface(Modifier.clickable { selected = method; qrisMode = method.code == "QRIS" }, shape = RoundedCornerShape(10.dp), color = if (active) Blue else Color.White, border = if (!active) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE1E8F0)) else null) {
                        Column(Modifier.padding(horizontal = 14.dp, vertical = 9.dp), horizontalAlignment = Alignment.CenterHorizontally) { Text(paymentIcon(method.code), fontSize = 17.sp, color = if (active) Color.White else Blue); Text(method.name, fontSize = 10.sp, color = if (active) Color.White else Navy, fontWeight = FontWeight.Bold) }
                    }
                }
            }
            if (qrisMode && selected?.code == "QRIS") {
                QrPreview(total)
                Text("Menunggu pembayaran. QR yang tampil belum berarti transaksi PAID.", fontSize = 11.sp, color = Warning, fontWeight = FontWeight.Medium)
            } else {
                OutlinedTextField(amount, { amount = it.filter(Char::isDigit) }, Modifier.fillMaxWidth(), label = { Text("Nominal pembayaran") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, shape = RoundedCornerShape(11.dp))
                if (selected?.code == "CASH") {
                    OutlinedTextField(received, { received = it.filter(Char::isDigit) }, Modifier.fillMaxWidth(), label = { Text("Uang diterima") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, shape = RoundedCornerShape(11.dp))
                    Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(Color(0xFFEAF8F0))) { Column(Modifier.fillMaxWidth().padding(12.dp)) { Text("KEMBALIAN", fontSize = 10.sp, color = Success, fontWeight = FontWeight.Bold); Text(idr(change), fontSize = 21.sp, color = Success, fontWeight = FontWeight.ExtraBold) } }
                }
                if (selected?.code == "TRANSFER") OutlinedTextField(reference, { reference = it }, Modifier.fillMaxWidth(), label = { Text("Referensi transfer") }, singleLine = true, shape = RoundedCornerShape(11.dp))
            }
            if (drafts.isNotEmpty()) {
                HorizontalDivider()
                Text("Pembayaran", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                drafts.forEach { d -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(methods.firstOrNull { it.id == d.methodId }?.name ?: "Payment", fontSize = 11.sp); Text(idr(d.amount), fontSize = 11.sp, fontWeight = FontWeight.Bold) } }
            }
        }
    }, confirmButton = {
        if (remaining == 0L) Button(onClick = { onSubmit(drafts) }, colors = ButtonDefaults.buttonColors(containerColor = Blue)) { Text("Selesaikan") }
        else Button(onClick = {
            val value = (amount.toLongOrNull() ?: 0L).coerceAtMost(remaining)
            val m = selected
            if (m != null && value > 0) { drafts = drafts + UiPaymentDraft(m.id, value, if (m.code == "CASH") cashReceived else value, reference.ifBlank { null }); amount = (remaining - value).toString(); received = (remaining - value).toString(); reference = "" }
        }, colors = ButtonDefaults.buttonColors(containerColor = Blue)) { Text(if (selected?.code == "QRIS") "Tampilkan QRIS" else "Tambah Pembayaran") }
    }, dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } })
}

private fun paymentIcon(code: String) = when (code) { "CASH" -> "▣"; "QRIS" -> "▦"; "TRANSFER" -> "⇄"; else -> "₱" }

@Composable private fun QrPreview(amount: Long) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(15.dp), colors = CardDefaults.cardColors(Color.White), elevation = CardDefaults.cardElevation(1.dp)) {
        Column(Modifier.fillMaxWidth().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Scan untuk membayar", fontSize = 11.sp, color = TextMuted)
            Spacer(Modifier.height(7.dp))
            Box(Modifier.size(185.dp).border(1.dp, Color(0xFFE1E8F0)).background(Color.White), contentAlignment = Alignment.Center) { FakeQr("DEMO-QRIS-${amount}") }
            Spacer(Modifier.height(7.dp))
            Text(idr(amount), fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Navy)
            Text("OPEN • Menunggu pembayaran", fontSize = 10.sp, color = Warning, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable private fun FakeQr(seed: String) {
    androidx.compose.foundation.Canvas(Modifier.size(160.dp)) {
        drawQrModules(this, seed)
    }
}

private fun drawQrModules(draw: DrawScope, seed: String) {
    val n = 25
    val cell = draw.size.minDimension / n
    val hash = seed.hashCode().toLong()
    fun filled(x: Int, y: Int): Boolean { var v = hash xor (x * 1103515245L) xor (y * 12345L); v = v xor (v ushr 17); return (v and 1L) == 0L }
    fun finder(x0: Int, y0: Int) { for (y in 0 until 7) for (x in 0 until 7) { val outer = x == 0 || y == 0 || x == 6 || y == 6; val inner = x in 2..4 && y in 2..4; if (outer || inner) draw.drawRect(Color.Black, androidx.compose.ui.geometry.Offset((x0+x)*cell, (y0+y)*cell), androidx.compose.ui.geometry.Size(cell, cell)) } }
    finder(0, 0); finder(18, 0); finder(0, 18)
    for (y in 0 until n) for (x in 0 until n) {
        val reserved = (x < 8 && y < 8) || (x >= 17 && y < 8) || (x < 8 && y >= 17)
        if (!reserved && filled(x, y)) draw.drawRect(Color.Black, androidx.compose.ui.geometry.Offset(x*cell, y*cell), androidx.compose.ui.geometry.Size(cell, cell))
    }
}

@Composable private fun CheckoutSuccess(result: UiCheckoutResult, onDone: () -> Unit) {
    AlertDialog(onDismissRequest = onDone, title = { Text("Pembayaran Berhasil", fontWeight = FontWeight.ExtraBold, color = Navy) }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(result.sale_no, fontWeight = FontWeight.Bold, color = Blue)
            Text(idr(result.total_amount), fontSize = 25.sp, fontWeight = FontWeight.ExtraBold, color = Navy)
            StatusBadge(result.sale_status)
            DetailAmount("Dibayar", idr(result.paid_amount))
            DetailAmount("Kembalian", idr(result.change_amount))
            Text("Struk siap dicetak melalui printer POS.", fontSize = 11.sp, color = TextMuted)
        }
    }, confirmButton = { Button(onClick = onDone, colors = ButtonDefaults.buttonColors(containerColor = Blue)) { Text("Transaksi Baru") } }, dismissButton = { TextButton(onClick = onDone) { Text("Tutup") } })
}

@Composable private fun StatusBadge(status: String) { val c = when (status.uppercase()) { "COMPLETED", "PAID" -> Success; "PENDING", "OPEN" -> Warning; else -> Danger }; Surface(shape = RoundedCornerShape(20.dp), color = c.copy(alpha = 0.12f)) { Text(status, Modifier.padding(horizontal = 10.dp, vertical = 5.dp), fontSize = 10.sp, color = c, fontWeight = FontWeight.Bold) } }

@Composable private fun EmptyState(title: String, subtitle: String) { Card(Modifier.fillMaxWidth().height(170.dp), shape = RoundedCornerShape(15.dp), colors = CardDefaults.cardColors(Color.White)) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("□", fontSize = 30.sp, color = Blue); Text(title, fontWeight = FontWeight.Bold, color = Navy); Text(subtitle, fontSize = 11.sp, color = TextMuted) } } } }

@Composable private fun PlaceholderPage(title: String) { Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { Text(title, fontSize = 25.sp, fontWeight = FontWeight.ExtraBold, color = Navy); Text("Modul ini tetap terhubung ke struktur POS yang sudah ada. UI transaksi menjadi fokus pada tahap ini.", color = TextMuted); Card(Modifier.fillMaxWidth().height(150.dp), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(Color.White)) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Modul $title", color = TextMuted) } } } }

private fun addLine(cart: List<UiCartLine>, product: UiProduct, price: Long): List<UiCartLine> = cart.map { if (it.product.id == product.id) it.copy(qty = it.qty + 1) else it }.let { updated -> if (updated.any { it.product.id == product.id }) updated else updated + UiCartLine(product, price, 1) }
private fun changeQty(cart: List<UiCartLine>, line: UiCartLine, delta: Int): List<UiCartLine> = cart.mapNotNull { if (it.product.id != line.product.id) it else { val q = it.qty + delta; if (q <= 0) null else it.copy(qty = q) } }

@Serializable private data class Business(val id: String, val code: String, val name: String, val is_active: Boolean = true)
@Serializable private data class Branch(val id: String, val business_id: String, val code: String, val name: String, val is_active: Boolean = true)
@Serializable private data class Location(val id: String, val branch_id: String, val code: String, val name: String, val is_active: Boolean = true)
@Serializable private data class PriceList(val id: String, val business_id: String, val name: String, val is_default: Boolean = false, val is_active: Boolean = true)
