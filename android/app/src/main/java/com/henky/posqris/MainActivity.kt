package com.henky.posqris

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
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
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

private val supabase = createSupabaseClient(BuildConfig.SUPABASE_URL, BuildConfig.SUPABASE_PUBLISHABLE_KEY) { install(Postgrest) }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { PosApp() }
    }
}

@Serializable data class Business(val id: String, val code: String, val name: String)
@Serializable data class Branch(val id: String, val business_id: String, val code: String, val name: String)
@Serializable data class Location(val id: String, val branch_id: String, val code: String, val name: String)
@Serializable data class Category(val id: String, val business_id: String, val code: String, val name: String, val is_active: Boolean = true)
@Serializable data class PriceList(val id: String, val business_id: String, val name: String, val is_default: Boolean = false)
@Serializable data class Product(val id: String, val business_id: String, val sku: String, val name: String, val short_name: String? = null, val category_id: String? = null, val base_unit_id: String, val current_cost: Long = 0, val min_stock: Double = 0.0, val is_active: Boolean = true)
@Serializable data class ProductPrice(val id: String, val price_list_id: String, val product_id: String, val unit_id: String, val min_qty: Double = 1.0, val price: Long)
@Serializable data class PaymentMethod(val id: String, val business_id: String, val code: String, val name: String, val method_type: String, val is_active: Boolean = true)
@Serializable data class Stock(val location_id: String, val product_id: String, val qty_base: Double = 0.0, val reserved_qty: Double = 0.0)
@Serializable data class SaleRow(val id: String, val sale_no: String, val total_amount: Long, val paid_amount: Long, val status: String, val sale_date: String)
@Serializable data class CartLine(val product: Product, val price: Long, val qty: Int)
@Serializable data class CheckoutResult(val sale_id: String, val sale_no: String, val total_amount: Long, val paid_amount: Long, val change_amount: Long, val sale_status: String)

private fun rupiah(value: Long): String = "Rp " + value.toString().reversed().chunked(3).joinToString(".").reversed()
private fun ProductPrice.matches(product: Product): Boolean = product_id == product.id

@Composable private fun PosApp() {
    var page by remember { mutableStateOf("POS") }
    val pages = listOf("Dashboard", "POS", "Produk", "Stok", "Pembayaran", "Laporan")
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        val tablet = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp >= 600
        if (tablet) {
            Row(Modifier.fillMaxSize()) {
                NavigationRailLike(pages, page) { page = it }
                Box(Modifier.weight(1f).fillMaxSize()) { PageContent(page) }
            }
        } else {
            Column(Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f).fillMaxWidth()) { PageContent(page) }
                BottomNav(pages.take(4), page) { page = it }
            }
        }
    }
}

@Composable private fun NavigationRailLike(pages: List<String>, selected: String, onSelect: (String) -> kotlin.Unit) {
    Surface(Modifier.width(210.dp).fillMaxSize(), tonalElevation = 2.dp) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text("POS", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
            Text("Toko Demo", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(14.dp))
            pages.forEach { p -> Text(p, Modifier.fillMaxWidth().clickable { onSelect(p) }.padding(13.dp), fontWeight = if (p == selected) FontWeight.Bold else FontWeight.Normal, color = if (p == selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface) }
        }
    }
}

@Composable private fun BottomNav(pages: List<String>, selected: String, onSelect: (String) -> kotlin.Unit) {
    Surface(Modifier.fillMaxWidth().navigationBarsPadding(), shadowElevation = 8.dp) {
        Row(Modifier.fillMaxWidth().padding(6.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            pages.forEach { p -> Text(p, Modifier.weight(1f).clickable { onSelect(p) }.padding(11.dp), fontWeight = if (p == selected) FontWeight.Bold else FontWeight.Normal, color = if (p == selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}

@Composable private fun PageContent(page: String) {
    when (page) {
        "Dashboard" -> DashboardScreen()
        "Produk" -> ProductsScreen()
        "Stok" -> InventoryScreen()
        "Pembayaran" -> PaymentsScreen()
        "Laporan" -> ReportsScreen()
        else -> PosScreen()
    }
}

@Composable private fun Page(title: String, subtitle: String, content: @Composable () -> kotlin.Unit) {
    Column(Modifier.fillMaxSize().padding(20.dp, 18.dp, 20.dp, 24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        content()
    }
}

@Composable private fun DashboardScreen() {
    var business by remember { mutableStateOf<Business?>(null) }
    var sales by remember { mutableStateOf<List<SaleRow>>(emptyList()) }
    var stock by remember { mutableStateOf<List<Stock>>(emptyList()) }
    var products by remember { mutableStateOf<List<Product>>(emptyList()) }
    LaunchedEffect(Unit) { runCatching {
        business = supabase.from("businesses").select { filter { eq("is_active", true) } }.decodeList<Business>().firstOrNull()
        sales = supabase.from("sales").select().decodeList<SaleRow>()
        stock = supabase.from("stock_balances").select().decodeList<Stock>()
        products = supabase.from("products").select { filter { eq("is_active", true) } }.decodeList<Product>()
    } }
    val today = LocalDate.now(ZoneId.of("Asia/Jakarta")).toString()
    val todaySales = sales.filter { it.sale_date.startsWith(today) && it.status == "COMPLETED" }
    val low = stock.count { s -> products.firstOrNull { it.id == s.product_id }?.let { s.qty_base <= it.min_stock } == true }
    Page("Dashboard", business?.name ?: "Memuat toko...") {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard("Penjualan", rupiah(todaySales.sumOf { it.total_amount }), Modifier.weight(1.4f))
            StatCard("Transaksi", todaySales.size.toString(), Modifier.weight(1f))
            StatCard("Stok menipis", low.toString(), Modifier.weight(1f))
        }
        Text("Transaksi terbaru", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Card(shape = RoundedCornerShape(16.dp)) {
            LazyColumn(Modifier.height(300.dp)) {
                items(sales.takeLast(12).reversed()) { sale ->
                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) { Text(sale.sale_no, fontWeight = FontWeight.SemiBold); Text(sale.status, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        Text(rupiah(sale.total_amount), fontWeight = FontWeight.Bold)
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable private fun StatCard(title: String, value: String, modifier: Modifier) {
    Card(modifier, shape = RoundedCornerShape(16.dp)) { Column(Modifier.padding(15.dp)) { Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant); Text(value, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold) } }
}

@Composable private fun PosScreen() {
    var branch by remember { mutableStateOf<Branch?>(null) }
    var location by remember { mutableStateOf<Location?>(null) }
    var products by remember { mutableStateOf<List<Product>>(emptyList()) }
    var prices by remember { mutableStateOf<List<ProductPrice>>(emptyList()) }
    var categories by remember { mutableStateOf<List<Category>>(emptyList()) }
    var methods by remember { mutableStateOf<List<PaymentMethod>>(emptyList()) }
    var cart by remember { mutableStateOf<List<CartLine>>(emptyList()) }
    var search by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Semua") }
    var showPayment by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) { runCatching {
        val business = supabase.from("businesses").select { filter { eq("is_active", true) } }.decodeList<Business>().first()
        branch = supabase.from("branches").select { filter { eq("business_id", business.id); eq("is_active", true) } }.decodeList<Branch>().first()
        val br = branch ?: error("Cabang tidak ditemukan")
        location = supabase.from("locations").select { filter { eq("branch_id", br.id); eq("is_active", true) } }.decodeList<Location>().first()
        products = supabase.from("products").select { filter { eq("business_id", business.id); eq("is_active", true) } }.decodeList<Product>()
        categories = supabase.from("categories").select { filter { eq("business_id", business.id); eq("is_active", true) } }.decodeList<Category>()
        val priceList = supabase.from("price_lists").select { filter { eq("business_id", business.id); eq("is_default", true); eq("is_active", true) } }.decodeList<PriceList>().firstOrNull()
        if (priceList != null) prices = supabase.from("product_prices").select { filter { eq("price_list_id", priceList.id) } }.decodeList<ProductPrice>()
        methods = supabase.from("payment_methods").select { filter { eq("business_id", business.id); eq("is_active", true) } }.decodeList<PaymentMethod>()
    } }
    fun priceOf(product: Product): Long = prices.filter { it.matches(product) }.minByOrNull { it.min_qty }?.price ?: 0L
    val filtered = products.filter { p -> (search.isBlank() || p.name.contains(search, true) || p.sku.contains(search, true)) && (category == "Semua" || categories.firstOrNull { it.id == p.category_id }?.name == category) }
    val total = cart.sumOf { it.price * it.qty }
    val scope = rememberCoroutineScope()
    Page("Kasir", branch?.name ?: "Memuat data...") {
        OutlinedTextField(search, { search = it }, Modifier.fillMaxWidth(), label = { Text("Cari produk / SKU") }, singleLine = true)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            listOf("Semua").plus(categories.map { it.name }).forEach { name -> OutlinedButton({ category = name }, shape = RoundedCornerShape(10.dp)) { Text(name) } }
        }
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            items(filtered) { p ->
                val price = priceOf(p)
                Card(Modifier.fillMaxWidth().clickable {
                    val existing = cart.firstOrNull { it.product.id == p.id }
                    cart = if (existing == null) cart + CartLine(p, price, 1) else cart.map { if (it.product.id == p.id) it.copy(qty = it.qty + 1) else it }
                }, shape = RoundedCornerShape(14.dp)) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) { Text(p.name, fontWeight = FontWeight.Bold); Text(p.sku, style = MaterialTheme.typography.labelSmall) }
                        Text(rupiah(price), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        if (cart.isNotEmpty()) Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(MaterialTheme.colorScheme.primaryContainer)) {
            Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { Text("Keranjang", fontWeight = FontWeight.Bold); Text("${cart.sumOf { it.qty }} item • ${rupiah(total)}") }
                Button({ showPayment = true }) { Text("Bayar") }
            }
        }
    }
    if (showPayment) PaymentDialog(total, methods, { showPayment = false }) { payments ->
        val br = branch; val loc = location
        if (br == null || loc == null) notice = "Data cabang/lokasi belum siap" else scope.launch {
            runCatching {
                val result = supabase.postgrest.rpc("checkout_sale_multi_payment", buildJsonObject {
                    put("p_branch_id", br.id); put("p_location_id", loc.id); put("p_customer_id", null as String?)
                    put("p_items", buildJsonArray { cart.forEach { line -> add(buildJsonObject { put("product_id", line.product.id); put("unit_id", line.product.base_unit_id); put("sku", line.product.sku); put("name", line.product.name); put("qty", line.qty); put("conversion_to_base", 1); put("unit_price", line.price); put("hpp_unit", line.product.current_cost) }) } })
                    put("p_payments", buildJsonArray { payments.forEach { payment -> add(buildJsonObject { put("payment_method_id", payment.methodId); put("amount", payment.amount); put("cash_received", payment.cashReceived); put("reference", payment.reference); put("qris_confirmed", false) }) } })
                    put("p_idempotency_key", UUID.randomUUID().toString())
                }).decodeSingle<CheckoutResult>()
                notice = "${result.sale_no} • ${result.sale_status} • ${rupiah(result.paid_amount)}"
                cart = emptyList(); showPayment = false
            }.onFailure { notice = it.message ?: "Checkout gagal" }
        }
    }
    notice?.let { text -> AlertDialog(onDismissRequest = { notice = null }, confirmButton = { TextButton({ notice = null }) { Text("OK") } }, text = { Text(text) }) }
}

private data class PaymentDraft(val methodId: String, val amount: Long, val cashReceived: Long, val reference: String?)

@Composable private fun PaymentDialog(total: Long, methods: List<PaymentMethod>, onDismiss: () -> kotlin.Unit, onSubmit: (List<PaymentDraft>) -> kotlin.Unit) {
    var selected by remember { mutableStateOf(methods.firstOrNull()) }
    var amount by remember { mutableStateOf(total.toString()) }
    var cashReceived by remember { mutableStateOf(total.toString()) }
    var reference by remember { mutableStateOf("") }
    val method = selected
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Pembayaran") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Text("Total ${rupiah(total)}", fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { methods.forEach { m -> OutlinedButton({ selected = m }) { Text(m.name) } } }
            OutlinedTextField(amount, { amount = it.filter(Char::isDigit) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, label = { Text("Nominal") })
            if (method?.code == "CASH") OutlinedTextField(cashReceived, { cashReceived = it.filter(Char::isDigit) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, label = { Text("Uang diterima") })
            if (method?.code == "TRANSFER") OutlinedTextField(reference, { reference = it }, singleLine = true, label = { Text("Referensi transfer") })
            if (method?.code == "QRIS") Text("QRIS akan tetap PENDING sampai dikonfirmasi provider/kasir.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }, confirmButton = { Button({ val a = amount.toLongOrNull() ?: 0L; if (method != null && a > 0L) onSubmit(listOf(PaymentDraft(method.id, a, cashReceived.toLongOrNull() ?: a, reference.ifBlank { null }))) }) { Text("Proses") } }, dismissButton = { TextButton(onDismiss) { Text("Batal") } })
}

@Composable private fun ProductsScreen() {
    var products by remember { mutableStateOf<List<Product>>(emptyList()) }
    LaunchedEffect(Unit) { products = runCatching { supabase.from("products").select { filter { eq("is_active", true) } }.decodeList<Product>() }.getOrDefault(emptyList()) }
    Page("Produk", "Data produk dari Supabase") { LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) { items(products) { p -> Card(shape = RoundedCornerShape(14.dp)) { Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(p.name, fontWeight = FontWeight.Bold); Text(p.sku) }; Text("Min ${p.min_stock}") } } } } }
}

@Composable private fun InventoryScreen() {
    var rows by remember { mutableStateOf<List<Stock>>(emptyList()) }
    LaunchedEffect(Unit) { rows = runCatching { supabase.from("stock_balances").select().decodeList<Stock>() }.getOrDefault(emptyList()) }
    Page("Stok", "Saldo stok aktual") { LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) { items(rows) { s -> Card(shape = RoundedCornerShape(14.dp)) { Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) { Text(s.product_id, Modifier.weight(1f)); Text(s.qty_base.toString()) } } } } }
}

@Composable private fun PaymentsScreen() {
    var sales by remember { mutableStateOf<List<SaleRow>>(emptyList()) }
    LaunchedEffect(Unit) { sales = runCatching { supabase.from("sales").select().decodeList<SaleRow>() }.getOrDefault(emptyList()) }
    Page("Pembayaran", "Status transaksi dan pembayaran") { LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) { items(sales.takeLast(30).reversed()) { s -> Card(shape = RoundedCornerShape(14.dp)) { Column(Modifier.fillMaxWidth().padding(14.dp)) { Text(s.sale_no, fontWeight = FontWeight.Bold); Text("${s.status} • ${rupiah(s.paid_amount)} / ${rupiah(s.total_amount)}") } } } } }
}

@Composable private fun ReportsScreen() {
    var sales by remember { mutableStateOf<List<SaleRow>>(emptyList()) }
    LaunchedEffect(Unit) { sales = runCatching { supabase.from("sales").select().decodeList<SaleRow>() }.getOrDefault(emptyList()) }
    val today = LocalDate.now(ZoneId.of("Asia/Jakarta")).toString()
    val todaySales = sales.filter { it.sale_date.startsWith(today) && it.status == "COMPLETED" }
    Page("Laporan", "Ringkasan penjualan hari ini") { StatCard("Omzet", rupiah(todaySales.sumOf { it.total_amount }), Modifier.fillMaxWidth()); StatCard("Transaksi selesai", todaySales.size.toString(), Modifier.fillMaxWidth()) }
}
