package com.henky.posqris

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import java.util.UUID

private val supabase = createSupabaseClient(BuildConfig.SUPABASE_URL, BuildConfig.SUPABASE_PUBLISHABLE_KEY) { install(Postgrest) }
private const val DEMO = "DEMO"
private val Teal = Color(0xFF008F8C)
private val TealDark = Color(0xFF086E6A)
private val Canvas = Color(0xFFF6F8F9)
private val Border = Color(0xFFD9E0E2)
private val Muted = Color(0xFF667085)

@Serializable data class Business(val id: String, val code: String, val name: String)
@Serializable data class Branch(val id: String, val business_id: String, val code: String, val name: String)
@Serializable data class Location(val id: String, val branch_id: String, val code: String, val name: String)
@Serializable data class PosUnit(val id: String, val business_id: String, val code: String, val name: String, val symbol: String? = null)
@Serializable data class Category(val id: String, val business_id: String, val code: String, val name: String, val is_active: Boolean = true)
@Serializable data class PriceList(val id: String, val business_id: String, val name: String, val is_default: Boolean = true)
@Serializable data class Product(val id: String, val business_id: String, val sku: String, val name: String, val short_name: String? = null, val category_id: String? = null, val base_unit_id: String, val current_cost: Long = 0, val min_stock: Double = 0.0, val is_active: Boolean = true)
@Serializable data class ProductPrice(val id: String, val price_list_id: String, val product_id: String, val unit_id: String, val min_qty: Double = 1.0, val price: Long)
@Serializable data class Customer(val id: String, val business_id: String, val code: String, val name: String, val phone: String? = null, val is_active: Boolean = true)
@Serializable data class PaymentMethod(val id: String, val business_id: String, val code: String, val name: String, val method_type: String, val is_active: Boolean = true)
@Serializable data class StockBalance(val location_id: String, val product_id: String, val qty_base: Double, val reserved_qty: Double = 0.0)
@Serializable data class Sale(val id: String, val sale_no: String, val total_amount: Long, val paid_amount: Long, val status: String, val sale_date: String)
@Serializable data class Payment(val id: String, val payment_no: String, val sale_id: String? = null, val amount: Long, val status: String, val provider: String? = null, val created_at: String)
@Serializable data class ProductInsert(val id: String, val business_id: String, val sku: String, val name: String, val short_name: String? = null, val category_id: String? = null, val base_unit_id: String, val current_cost: Long = 0, val last_purchase_cost: Long = 0, val is_active: Boolean = true)
@Serializable data class ProductPriceInsert(val id: String, val price_list_id: String, val product_id: String, val unit_id: String, val min_qty: Double = 1.0, val price: Long)
@Serializable data class ActiveUpdate(val is_active: Boolean)
@Serializable data class StockUpdate(val qty_base: Double)
@Serializable data class SaleInsert(val id: String, val business_id: String, val branch_id: String, val location_id: String, val customer_id: String? = null, val sale_no: String, val status: String, val subtotal: Long, val discount_amount: Long = 0, val tax_amount: Long = 0, val service_charge: Long = 0, val rounding_amount: Long = 0, val total_amount: Long, val paid_amount: Long, val change_amount: Long = 0, val hpp_amount: Long, val margin_amount: Long, val notes: String? = null)
@Serializable data class SaleItemInsert(val id: String, val sale_id: String, val product_id: String, val unit_id: String, val product_sku_snapshot: String, val product_name_snapshot: String, val qty: Double, val conversion_to_base: Double = 1.0, val unit_price: Long, val discount_amount: Long = 0, val tax_amount: Long = 0, val line_total: Long, val hpp_unit: Long, val hpp_total: Long)
@Serializable data class PaymentInsert(val id: String, val business_id: String, val branch_id: String, val sale_id: String, val payment_method_id: String, val payment_no: String, val amount: Long, val currency_code: String = "IDR", val status: String = "PAID", val provider: String? = "DEMO", val external_transaction_id: String? = null, val idempotency_key: String? = null, val reconciliation_status: String = "UNRECONCILED")

data class CartLine(val product: Product, val price: Long, val qty: Int)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { PosTheme { PosApp() } }
    }
}

@Composable private fun PosTheme(content: @Composable () -> kotlin.Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(primary = Teal, onPrimary = Color.White, primaryContainer = Color(0xFFDDF5F2), onPrimaryContainer = TealDark, background = Canvas, surface = Color.White, surfaceVariant = Color(0xFFF0F3F4), outline = Border),
        content = content
    )
}

private fun money(value: Long): String = "Rp " + value.toString().reversed().chunked(3).joinToString(".").reversed()
private fun initial(value: String) = value.trim().take(1).uppercase().ifBlank { "P" }

@Composable private fun PosApp() {
    var route by remember { mutableStateOf("home") }
    var more by remember { mutableStateOf(false) }
    val tablet = LocalConfiguration.current.screenWidthDp >= 600
    Row(Modifier.fillMaxSize().background(Canvas)) {
        if (tablet) Sidebar(route) { route = it; more = false }
        Box(Modifier.weight(1f).fillMaxHeight()) {
            when (route) {
                "home" -> Home { route = "sales" }
                "sales" -> Sales()
                "products" -> Products()
                "stock" -> StockScreen()
                "customers" -> Customers()
                "payments" -> Payments()
                "reports" -> Reports()
                else -> Settings()
            }
            if (!tablet) BottomNav(route) { if (it == "more") more = true else route = it }
        }
    }
    if (more) MoreDialog({ more = false }) { route = it; more = false }
}

private fun title(route: String) = when (route) {
    "home" -> "Beranda"; "sales" -> "Transaksi"; "products" -> "Produk"; "stock" -> "Stok"
    "customers" -> "Pelanggan"; "payments" -> "Pembayaran"; "reports" -> "Laporan"; else -> "Pengaturan"
}

@Composable private fun Sidebar(selected: String, onSelect: (String) -> kotlin.Unit) {
    Surface(Modifier.width(224.dp).fillMaxHeight(), tonalElevation = 2.dp) {
        Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.padding(bottom = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(42.dp).background(Teal, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) { Text("Q", color = Color.White, fontWeight = FontWeight.Black, fontSize = 21.sp) }
                Spacer(Modifier.width(10.dp)); Column { Text("POS QRIS", fontWeight = FontWeight.Bold); Text("Toko Demo", color = Muted, fontSize = 12.sp) }
            }
            listOf("home", "sales", "products", "stock", "customers", "payments", "reports", "settings").forEach { route ->
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(if (selected == route) MaterialTheme.colorScheme.primaryContainer else Color.Transparent).clickable { onSelect(route) }.padding(horizontal = 12.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(when (route) { "home" -> "⌂"; "sales" -> "+"; "products" -> "□"; "stock" -> "▥"; "customers" -> "♙"; "payments" -> "Rp"; "reports" -> "▤"; else -> "⚙" }, fontWeight = FontWeight.Bold, color = if (selected == route) Teal else Muted, modifier = Modifier.width(28.dp))
                    Text(title(route), fontWeight = if (selected == route) FontWeight.SemiBold else FontWeight.Normal)
                }
            }
            Spacer(Modifier.weight(1f)); Text("ONLINE • SUPABASE", color = Color(0xFF039855), fontSize = 11.sp)
        }
    }
}

@Composable private fun BottomNav(selected: String, onSelect: (String) -> kotlin.Unit) {
    Surface(Modifier.fillMaxWidth().navigationBarsPadding(), shadowElevation = 10.dp) {
        Row(Modifier.fillMaxWidth().padding(6.dp)) {
            listOf("home", "sales", "products", "stock").forEach { route ->
                Column(Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(if (selected == route) MaterialTheme.colorScheme.primaryContainer else Color.Transparent).clickable { onSelect(route) }.padding(vertical = 7.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(when (route) { "home" -> "⌂"; "sales" -> "+"; "products" -> "□"; else -> "▥" }, fontWeight = FontWeight.Bold, color = if (selected == route) Teal else Muted)
                    Text(title(route), fontSize = 11.sp, color = if (selected == route) Teal else Muted)
                }
            }
            Column(Modifier.weight(1f).clickable { onSelect("more") }.padding(vertical = 7.dp), horizontalAlignment = Alignment.CenterHorizontally) { Text("•••", fontWeight = FontWeight.Bold, color = Muted); Text("Lainnya", fontSize = 11.sp, color = Muted) }
        }
    }
}

@Composable private fun MoreDialog(onDismiss: () -> kotlin.Unit, onRoute: (String) -> kotlin.Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Menu lainnya") }, text = { Column { listOf("customers", "payments", "reports", "settings").forEach { route -> TextButton(onClick = { onRoute(route) }, modifier = Modifier.fillMaxWidth()) { Text(title(route), Modifier.fillMaxWidth()) } } } }, confirmButton = { TextButton(onClick = onDismiss) { Text("Tutup") } })
}

@Composable private fun Page(title: String, subtitle: String, content: @Composable ColumnScope.() -> kotlin.Unit) {
    val phone = LocalConfiguration.current.screenWidthDp < 600
    Column(Modifier.fillMaxSize().padding(start = 20.dp, top = 18.dp, end = 20.dp, bottom = if (phone) 78.dp else 18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) { Column(Modifier.weight(1f)) { Text(title, fontSize = 26.sp, fontWeight = FontWeight.Bold); Text(subtitle, color = Muted, fontSize = 13.sp) }; Text("● Online", color = Color(0xFF039855), fontSize = 12.sp) }
        content()
    }
}

@Composable private fun Notice(text: String, error: Boolean = false) { Surface(Modifier.fillMaxWidth(), color = if (error) Color(0xFFFFE4E6) else MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(10.dp)) { Text(text, Modifier.padding(12.dp), color = if (error) Color(0xFF9F1239) else TealDark, fontSize = 13.sp) } }

@Composable private fun Home(onNewSale: () -> kotlin.Unit) {
    var sales by remember { mutableStateOf<List<Sale>>(emptyList()) }; var error by remember { mutableStateOf<String?>(null) }; var loading by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) { try { val business = businessId(); sales = supabase.from("sales").select { filter { eq("business_id", business) } }.decodeList() } catch (e: Exception) { error = e.message } finally { loading = false } }
    val total = sales.sumOf { it.total_amount }
    Page("Dashboard", "Ringkasan operasional toko hari ini") {
        error?.let { Notice(it, true) }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Kpi("Penjualan", money(total), "data live", true, Modifier.weight(1.3f)); Kpi("Transaksi", sales.size.toString(), "tercatat", false, Modifier.weight(1f)); Kpi("Rata-rata", if (sales.isEmpty()) money(0) else money(total / sales.size), "per transaksi", false, Modifier.weight(1f))
        }
        Button(onClick = onNewSale, Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(10.dp)) { Text("+  Transaksi Baru", fontWeight = FontWeight.Bold) }
        Text("Transaksi terbaru", fontWeight = FontWeight.Bold, fontSize = 17.sp)
        if (loading) LinearProgressIndicator(Modifier.fillMaxWidth()) else LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) { items(sales.take(10)) { sale -> SaleRow(sale) } }
    }
}

@Composable private fun Kpi(label: String, value: String, note: String, primary: Boolean, modifier: Modifier) {
    Card(modifier, shape = RoundedCornerShape(11.dp), colors = CardDefaults.cardColors(if (primary) Teal else Color.White), border = if (primary) null else BorderStroke(1.dp, Border)) { Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) { Text(label, color = if (primary) Color.White.copy(.75f) else Muted, fontSize = 12.sp); Text(value, color = if (primary) Color.White else Color(0xFF182230), fontSize = 21.sp, fontWeight = FontWeight.Bold); Text(note, color = if (primary) Color.White.copy(.75f) else TealDark, fontSize = 11.sp) } }
}

@Composable private fun SaleRow(sale: Sale) { Row(Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(10.dp)).padding(12.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(sale.sale_no, fontWeight = FontWeight.SemiBold); Text(sale.status, color = Color(0xFF039855), fontSize = 11.sp) }; Text(money(sale.total_amount), fontWeight = FontWeight.Bold) } }

@Composable private fun Sales() {
    var products by remember { mutableStateOf<List<Product>>(emptyList()) }; var prices by remember { mutableStateOf<List<ProductPrice>>(emptyList()) }; var categories by remember { mutableStateOf<List<Category>>(emptyList()) }; var cart by remember { mutableStateOf<List<CartLine>>(emptyList()) }; var query by remember { mutableStateOf("") }; var selectedCategory by remember { mutableStateOf("Semua") }; var message by remember { mutableStateOf<String?>(null) }; var busy by remember { mutableStateOf(false) }; val scope = rememberCoroutineScope(); val tablet = LocalConfiguration.current.screenWidthDp >= 600
    LaunchedEffect(Unit) { try { val business = businessId(); products = supabase.from("products").select { filter { eq("business_id", business); eq("is_active", true) } }.decodeList(); prices = supabase.from("product_prices").select().decodeList(); categories = supabase.from("categories").select { filter { eq("business_id", business); eq("is_active", true) } }.decodeList() } catch (e: Exception) { message = "Gagal memuat: ${e.message}" } }
    val filtered = products.filter { (selectedCategory == "Semua" || categories.firstOrNull { c -> c.id == it.category_id }?.name == selectedCategory) && (query.isBlank() || it.name.contains(query, true) || it.sku.contains(query, true)) }
    val total = cart.sumOf { it.price * it.qty }
    Page("Transaksi", "Kasir cepat • pilih produk lalu bayar") {
        message?.let { Notice(it, it.startsWith("Gagal")) }
        if (tablet) Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(14.dp)) { ProductCatalog(filtered, categories, selectedCategory, query, prices, Modifier.weight(1.45f), { query = it }, { selectedCategory = it }) { product -> cart = addToCart(cart, product, priceOf(product, prices)) }; CartPanel(cart, total, Modifier.weight(.85f), { line -> cart = cart.map { if (it.product.id == line.product.id) it.copy(qty = it.qty + 1) else it } }, { line -> cart = cart.mapNotNull { if (it.product.id != line.product.id) it else if (it.qty <= 1) null else it.copy(qty = it.qty - 1) } }, { if (!busy) { busy = true; scope.launch { message = checkout(cart); if (message?.startsWith("Tersimpan") == true) cart = emptyList(); busy = false } } }, busy) }
        else { ProductCatalog(filtered, categories, selectedCategory, query, prices, Modifier.fillMaxWidth().weight(1f), { query = it }, { selectedCategory = it }) { product -> cart = addToCart(cart, product, priceOf(product, prices)) }; if (cart.isNotEmpty()) CartPanel(cart, total, Modifier.fillMaxWidth(), { line -> cart = cart.map { if (it.product.id == line.product.id) it.copy(qty = it.qty + 1) else it } }, { line -> cart = cart.mapNotNull { if (it.product.id != line.product.id) it else if (it.qty <= 1) null else it.copy(qty = it.qty - 1) } }, { if (!busy) { busy = true; scope.launch { message = checkout(cart); if (message?.startsWith("Tersimpan") == true) cart = emptyList(); busy = false } } }, busy) } }
}

private fun priceOf(product: Product, prices: List<ProductPrice>) = prices.filter { it.product_id == product.id }.minByOrNull { it.min_qty }?.price ?: product.current_cost
private fun addToCart(cart: List<CartLine>, product: Product, price: Long): List<CartLine> { val old = cart.firstOrNull { it.product.id == product.id }; return if (old == null) cart + CartLine(product, price, 1) else cart.map { if (it.product.id == product.id) it.copy(qty = it.qty + 1) else it } }

@Composable private fun ProductCatalog(products: List<Product>, categories: List<Category>, selected: String, query: String, prices: List<ProductPrice>, modifier: Modifier, onQuery: (String) -> kotlin.Unit, onCategory: (String) -> kotlin.Unit, onAdd: (Product) -> kotlin.Unit) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(9.dp)) {
        OutlinedTextField(value = query, onValueChange = onQuery, modifier = Modifier.fillMaxWidth(), placeholder = { Text("Cari produk atau SKU") }, singleLine = true)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) { (listOf("Semua") + categories.map { it.name }).forEach { category -> FilterChip(selected = selected == category, onClick = { onCategory(category) }, label = { Text(category) }) } }
        LazyVerticalGrid(columns = GridCells.Adaptive(145.dp), modifier = Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { items(products) { product -> Card(Modifier.clickable { onAdd(product) }, shape = RoundedCornerShape(11.dp), border = BorderStroke(1.dp, Border)) { Column(Modifier.padding(12.dp)) { Box(Modifier.size(42.dp).background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(9.dp)), contentAlignment = Alignment.Center) { Text(initial(product.name), color = Teal, fontWeight = FontWeight.Bold) }; Spacer(Modifier.height(7.dp)); Text(product.name, fontWeight = FontWeight.SemiBold, maxLines = 2); Text(product.sku, color = Muted, fontSize = 11.sp); Spacer(Modifier.height(4.dp)); Text(money(priceOf(product, prices)), color = Teal, fontWeight = FontWeight.Bold) } } } }
    }
}

@Composable private fun CartPanel(cart: List<CartLine>, total: Long, modifier: Modifier, onPlus: (CartLine) -> kotlin.Unit, onMinus: (CartLine) -> kotlin.Unit, onPay: () -> kotlin.Unit, busy: Boolean) {
    Card(modifier, shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, Border)) { Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { Row(Modifier.fillMaxWidth()) { Text("Keranjang", fontWeight = FontWeight.Bold, fontSize = 17.sp); Spacer(Modifier.weight(1f)); Text("${cart.sumOf { it.qty }} item", color = Muted) }; HorizontalDivider(); if (cart.isEmpty()) { Spacer(Modifier.weight(1f)); Text("Belum ada produk", Modifier.align(Alignment.CenterHorizontally), color = Muted); Spacer(Modifier.weight(1f)) } else { LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) { items(cart) { line -> Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(line.product.name, fontWeight = FontWeight.SemiBold); Text(money(line.price), color = Muted, fontSize = 11.sp) }; TextButton(onClick = { onMinus(line) }) { Text("−") }; Text(line.qty.toString()); TextButton(onClick = { onPlus(line) }) { Text("+") }; Text(money(line.price * line.qty), fontWeight = FontWeight.SemiBold) } } }; HorizontalDivider(); Row(Modifier.fillMaxWidth()) { Text("Total", fontWeight = FontWeight.Bold); Spacer(Modifier.weight(1f)); Text(money(total), fontSize = 21.sp, fontWeight = FontWeight.Bold) }; Button(onClick = onPay, enabled = !busy, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(10.dp)) { Text(if (busy) "Menyimpan..." else "BAYAR  ${money(total)}") }; Text("Mode demo: pembayaran dicatat PAID, bukan verifikasi provider QRIS.", color = Muted, fontSize = 10.sp) } } }
}

private suspend fun businessId(): String = supabase.from("businesses").select { filter { eq("code", DEMO) } }.decodeList<Business>().first().id
private suspend fun contextData(): Triple<Business, Branch, Location> { val business = supabase.from("businesses").select { filter { eq("code", DEMO) } }.decodeList<Business>().first(); val branch = supabase.from("branches").select { filter { eq("business_id", business.id); eq("code", "MAIN") } }.decodeList<Branch>().first(); val location = supabase.from("locations").select { filter { eq("branch_id", branch.id); eq("code", "STORE") } }.decodeList<Location>().first(); return Triple(business, branch, location) }

private suspend fun checkout(cart: List<CartLine>): String {
    if (cart.isEmpty()) return "Keranjang kosong"
    return try {
        val (business, branch, location) = contextData()
        val methods = supabase.from("payment_methods").select { filter { eq("business_id", business.id); eq("is_active", true) } }.decodeList<PaymentMethod>()
        val method = methods.firstOrNull { it.code == "QRIS" } ?: methods.first()
        val saleId = UUID.randomUUID().toString(); val number = "TRX-" + System.currentTimeMillis().toString().takeLast(8); val total = cart.sumOf { it.price * it.qty }; val hpp = cart.sumOf { it.product.current_cost * it.qty }
        supabase.from("sales").insert(SaleInsert(saleId, business.id, branch.id, location.id, null, number, "COMPLETED", total, total_amount = total, paid_amount = total, hpp_amount = hpp, margin_amount = total - hpp, notes = "POS QRIS Demo"))
        supabase.from("sale_items").insert(cart.map { line -> SaleItemInsert(UUID.randomUUID().toString(), saleId, line.product.id, line.product.base_unit_id, line.product.sku, line.product.name, line.qty.toDouble(), unit_price = line.price, line_total = line.price * line.qty, hpp_unit = line.product.current_cost, hpp_total = line.product.current_cost * line.qty) })
        supabase.from("payments").insert(PaymentInsert(UUID.randomUUID().toString(), business.id, branch.id, saleId, method.id, "PAY-${number.removePrefix("TRX-")}", total))
        cart.forEach { line -> val stock = supabase.from("stock_balances").select { filter { eq("location_id", location.id); eq("product_id", line.product.id) } }.decodeList<StockBalance>().firstOrNull(); if (stock != null) supabase.from("stock_balances").update(StockUpdate((stock.qty_base - line.qty).coerceAtLeast(0.0))) { filter { eq("location_id", location.id); eq("product_id", line.product.id) } } }
        "Tersimpan: $number • ${money(total)}"
    } catch (e: Exception) { "Gagal: ${e.message ?: "database"}" }
}

@Composable private fun Products() {
    var products by remember { mutableStateOf<List<Product>>(emptyList()) }; var unit by remember { mutableStateOf<PosUnit?>(null) }; var priceList by remember { mutableStateOf<PriceList?>(null) }; var show by remember { mutableStateOf(false) }; var name by remember { mutableStateOf("") }; var sku by remember { mutableStateOf("") }; var price by remember { mutableStateOf("") }; var message by remember { mutableStateOf<String?>(null) }; val scope = rememberCoroutineScope()
    suspend fun reload() { val business = businessId(); products = supabase.from("products").select { filter { eq("business_id", business); eq("is_active", true) } }.decodeList(); unit = supabase.from("units").select { filter { eq("business_id", business); eq("code", "PCS") } }.decodeList<PosUnit>().firstOrNull(); priceList = supabase.from("price_lists").select { filter { eq("business_id", business); eq("is_default", true) } }.decodeList<PriceList>().firstOrNull() }
    LaunchedEffect(Unit) { try { reload() } catch (e: Exception) { message = "Gagal: ${e.message}" } }
    Page("Produk", "Katalog tersimpan langsung di Supabase") {
        message?.let { Notice(it, it.startsWith("Gagal")) }
        if (!show) Button(onClick = { show = true }) { Text("+ Produk") } else {
            Card(shape = RoundedCornerShape(11.dp), border = BorderStroke(1.dp, Border)) { Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text("Nama produk") }, singleLine = true); OutlinedTextField(sku, { sku = it }, Modifier.fillMaxWidth(), label = { Text("SKU") }, singleLine = true); OutlinedTextField(price, { price = it }, Modifier.fillMaxWidth(), label = { Text("Harga jual") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Button(onClick = { show = false }) { Text("Batal") }; Button(onClick = { scope.launch { try { val business = businessId(); val u = unit ?: error("Unit PCS tidak ditemukan"); val list = priceList ?: error("Price list tidak ditemukan"); val value = price.toLongOrNull() ?: error("Harga tidak valid"); require(name.isNotBlank() && sku.isNotBlank()) { "Nama dan SKU wajib" }; val id = UUID.randomUUID().toString(); supabase.from("products").insert(ProductInsert(id, business, sku, name, name, null, u.id, value, value)); supabase.from("product_prices").insert(ProductPriceInsert(UUID.randomUUID().toString(), list.id, id, u.id, 1.0, value)); reload(); name = ""; sku = ""; price = ""; show = false; message = "Produk tersimpan" } catch (e: Exception) { message = "Gagal: ${e.message}" } } }) { Text("Simpan") } } } }
        }
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) { items(products) { product -> Row(Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(10.dp)).padding(12.dp), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(40.dp).background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(9.dp)), contentAlignment = Alignment.Center) { Text(initial(product.name), color = Teal, fontWeight = FontWeight.Bold) }; Spacer(Modifier.width(10.dp)); Column(Modifier.weight(1f)) { Text(product.name, fontWeight = FontWeight.SemiBold); Text(product.sku, color = Muted, fontSize = 11.sp) }; TextButton(onClick = { scope.launch { try { supabase.from("products").update(ActiveUpdate(false)) { filter { eq("id", product.id) } }; reload(); message = "Produk dinonaktifkan" } catch (e: Exception) { message = "Gagal: ${e.message}" } } }) { Text("Nonaktif") } } } }
    }
}

@Composable private fun StockScreen() {
    var products by remember { mutableStateOf<List<Product>>(emptyList()) }; var stocks by remember { mutableStateOf<Map<String, Double>>(emptyMap()) }; var location by remember { mutableStateOf<Location?>(null) }; var message by remember { mutableStateOf<String?>(null) }; val scope = rememberCoroutineScope()
    suspend fun reload() { val business = businessId(); val branch = supabase.from("branches").select { filter { eq("business_id", business); eq("code", "MAIN") } }.decodeList<Branch>().first(); val loc = supabase.from("locations").select { filter { eq("branch_id", branch.id); eq("code", "STORE") } }.decodeList<Location>().first(); location = loc; products = supabase.from("products").select { filter { eq("business_id", business); eq("is_active", true) } }.decodeList(); stocks = supabase.from("stock_balances").select { filter { eq("location_id", loc.id) } }.decodeList<StockBalance>().associate { it.product_id to it.qty_base } }
    LaunchedEffect(Unit) { try { reload() } catch (e: Exception) { message = "Gagal: ${e.message}" } }
    Page("Persediaan", "Pantau stok dan lakukan penyesuaian cepat") { message?.let { Notice(it, true) }; LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) { items(products) { product -> val qty = stocks[product.id] ?: 0.0; Row(Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(10.dp)).padding(12.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(product.name, fontWeight = FontWeight.SemiBold); Text(product.sku, color = Muted, fontSize = 11.sp) }; Text("${qty.toInt()} pcs", fontWeight = FontWeight.Bold, color = if (qty <= product.min_stock) Color(0xFFD92D20) else Color(0xFF182230)); TextButton(onClick = { scope.launch { try { val loc = location ?: return@launch; supabase.from("stock_balances").update(StockUpdate(qty + 1)) { filter { eq("location_id", loc.id); eq("product_id", product.id) } }; reload() } catch (e: Exception) { message = "Gagal: ${e.message}" } } }) { Text("+1") } } } } }
}

@Composable private fun Customers() { var rows by remember { mutableStateOf<List<Customer>>(emptyList()) }; var message by remember { mutableStateOf<String?>(null) }; LaunchedEffect(Unit) { try { rows = supabase.from("customers").select { filter { eq("business_id", businessId()); eq("is_active", true) } }.decodeList() } catch (e: Exception) { message = "Gagal: ${e.message}" } }; Page("Pelanggan", "Daftar pelanggan dari Supabase") { message?.let { Notice(it, true) }; LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) { items(rows) { row -> DataRow(row.name, row.phone ?: "Tanpa nomor") } } } }
@Composable private fun Payments() { var rows by remember { mutableStateOf<List<Payment>>(emptyList()) }; var message by remember { mutableStateOf<String?>(null) }; LaunchedEffect(Unit) { try { rows = supabase.from("payments").select { filter { eq("business_id", businessId()) } }.decodeList() } catch (e: Exception) { message = "Gagal: ${e.message}" } }; Page("Pembayaran", "Monitoring pembayaran transaksi") { message?.let { Notice(it, true) }; if (rows.isEmpty()) Notice("Belum ada pembayaran") else LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) { items(rows) { row -> DataRow(row.payment_no, money(row.amount) + " • " + row.status) } } } }
@Composable private fun Reports() { var rows by remember { mutableStateOf<List<Sale>>(emptyList()) }; LaunchedEffect(Unit) { rows = try { supabase.from("sales").select { filter { eq("business_id", businessId()) } }.decodeList() } catch (_: Exception) { emptyList() } }; val total = rows.sumOf { it.total_amount }; Page("Laporan", "Ringkasan penjualan dari database") { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) { Kpi("Penjualan", money(total), "semua data", true, Modifier.weight(1.2f)); Kpi("Transaksi", rows.size.toString(), "tercatat", false, Modifier.weight(1f)) }; LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) { items(rows) { SaleRow(it) } } } }
@Composable private fun Settings() { Page("Pengaturan", "Status koneksi dan mode perangkat") { Notice("Supabase terhubung • Project pbcjiqwifiivibfrkbaf"); Notice("Mode Demo POS QRIS. Pembayaran PAID hanya simulasi, bukan verifikasi provider.") } }
@Composable private fun DataRow(name: String, detail: String) { Row(Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(10.dp)).padding(12.dp), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(40.dp).background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(9.dp)), contentAlignment = Alignment.Center) { Text(initial(name), color = Teal, fontWeight = FontWeight.Bold) }; Spacer(Modifier.width(10.dp)); Column { Text(name, fontWeight = FontWeight.SemiBold); Text(detail, color = Muted, fontSize = 11.sp) } } }
