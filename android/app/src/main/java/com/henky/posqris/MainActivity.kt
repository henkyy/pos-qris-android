package com.henky.posqris

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
private val Navy = Color(0xFF021024)
private val Blue = Color(0xFF052659)
private val MidBlue = Color(0xFF5483B3)
private val SoftBlue = Color(0xFF7DA0CA)
private val PaleBlue = Color(0xFFC1E8FF)
private val Canvas = Color(0xFFF5F8FC)
private val Surface = Color.White
private val Border = Color(0xFFD9E2EC)
private val Text = Color(0xFF172033)
private val Muted = Color(0xFF667085)
private val Success = Color(0xFF16855B)
private val Danger = Color(0xFFD64545)

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
        colorScheme = lightColorScheme(
            primary = Blue,
            onPrimary = Color.White,
            primaryContainer = PaleBlue,
            onPrimaryContainer = Navy,
            background = Canvas,
            surface = Surface,
            surfaceVariant = Color(0xFFEAF1F8),
            outline = Border,
            onBackground = Text,
            onSurface = Text
        ),
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
    "home" -> "Beranda"; "sales" -> "Penjualan"; "products" -> "Produk"; "stock" -> "Stok"
    "customers" -> "Pelanggan"; "payments" -> "Pembayaran"; "reports" -> "Laporan"; else -> "Pengaturan"
}

private fun icon(route: String) = when (route) {
    "home" -> "⌂"; "sales" -> "＋"; "products" -> "□"; "stock" -> "▥"
    "customers" -> "♙"; "payments" -> "Rp"; "reports" -> "▤"; else -> "⚙"
}

@Composable private fun Sidebar(selected: String, onSelect: (String) -> kotlin.Unit) {
    Surface(Modifier.width(236.dp).fillMaxHeight(), color = Navy) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Row(Modifier.padding(horizontal = 4.dp, vertical = 8.dp).padding(bottom = 22.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(42.dp).background(PaleBlue, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) { Text("Q", color = Navy, fontWeight = FontWeight.Black, fontSize = 21.sp) }
                Spacer(Modifier.width(11.dp))
                Column { Text("POS QRIS", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp); Text("TOKO DEMO • UTAMA", color = SoftBlue, fontSize = 10.sp, fontWeight = FontWeight.SemiBold) }
            }
            Text("OPERASIONAL", color = SoftBlue, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp))
            listOf("home", "sales", "products", "stock", "customers", "payments", "reports", "settings").forEach { route ->
                val active = selected == route
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(if (active) Blue else Color.Transparent).clickable { onSelect(route) }.padding(horizontal = 12.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(icon(route), fontWeight = FontWeight.Bold, color = if (active) PaleBlue else Color(0xFFB9C8D8), modifier = Modifier.width(30.dp))
                    Text(title(route), color = if (active) Color.White else Color(0xFFD5DEE8), fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal)
                }
            }
            Spacer(Modifier.weight(1f))
            Surface(color = Color.White.copy(alpha = .07f), shape = RoundedCornerShape(12.dp)) {
                Row(Modifier.fillMaxWidth().padding(11.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(34.dp).background(MidBlue, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) { Text("A", color = Color.White, fontWeight = FontWeight.Bold) }
                    Spacer(Modifier.width(9.dp)); Column { Text("Admin", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp); Text("Kasir / Owner", color = SoftBlue, fontSize = 10.sp) }
                }
            }
        }
    }
}

@Composable private fun BottomNav(selected: String, onSelect: (String) -> kotlin.Unit) {
    Surface(Modifier.fillMaxWidth().navigationBarsPadding(), color = Color.White, shadowElevation = 14.dp) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 5.dp)) {
            listOf("home", "sales", "products", "stock").forEach { route ->
                val active = selected == route
                Column(Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).clickable { onSelect(route) }.padding(vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(icon(route), fontWeight = FontWeight.Bold, color = if (active) Blue else Muted)
                    Text(title(route), fontSize = 10.sp, color = if (active) Blue else Muted, fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal)
                }
            }
            Column(Modifier.weight(1f).clickable { onSelect("more") }.padding(vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) { Text("•••", fontWeight = FontWeight.Bold, color = Muted); Text("Lainnya", fontSize = 10.sp, color = Muted) }
        }
    }
}

@Composable private fun MoreDialog(onDismiss: () -> kotlin.Unit, onRoute: (String) -> kotlin.Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Menu lainnya", fontWeight = FontWeight.Bold) }, text = { Column { listOf("customers", "payments", "reports", "settings").forEach { route -> TextButton(onClick = { onRoute(route) }, modifier = Modifier.fillMaxWidth()) { Text(title(route), Modifier.fillMaxWidth()) } } } }, confirmButton = { TextButton(onClick = onDismiss) { Text("Tutup") } })
}

@Composable private fun Page(pageTitle: String, subtitle: String, action: (@Composable () -> kotlin.Unit)? = null, content: @Composable ColumnScope.() -> kotlin.Unit) {
    val phone = LocalConfiguration.current.screenWidthDp < 600
    Column(Modifier.fillMaxSize().padding(start = if (phone) 16.dp else 24.dp, top = if (phone) 16.dp else 22.dp, end = if (phone) 16.dp else 24.dp, bottom = if (phone) 76.dp else 18.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { Text(pageTitle, fontSize = if (phone) 23.sp else 27.sp, fontWeight = FontWeight.Bold, color = Navy); Text(subtitle, color = Muted, fontSize = 12.sp) }
            action?.invoke()
        }
        Spacer(Modifier.height(16.dp))
        content()
    }
}

@Composable private fun Notice(text: String, error: Boolean = false) {
    Surface(Modifier.fillMaxWidth(), color = if (error) Color(0xFFFFE9EA) else Color(0xFFE9F5FF), shape = RoundedCornerShape(10.dp), border = BorderStroke(1.dp, if (error) Color(0xFFF4B6BB) else PaleBlue)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(if (error) "!" else "i", color = if (error) then Danger else Blue, fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 9.dp))
            Text(text, color = if (error) Color(0xFF9E2C38) else Blue, fontSize = 12.sp)
        }
    }
}

@Composable private fun Home(onNewSale: () -> kotlin.Unit) {
    var sales by remember { mutableStateOf<List<Sale>>(emptyList()) }
    var error by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) { try { sales = supabase.from("sales").select { filter { eq("business_id", businessId()) } }.decodeList() } catch (_: Exception) { error = true } finally { loading = false } }
    val total = sales.sumOf { it.total_amount }
    val avg = if (sales.isEmpty()) 0 else total / sales.size
    Page("Dashboard", "Ringkasan operasional • Toko Utama", Button(onClick = onNewSale, shape = RoundedCornerShape(9.dp), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)) { Text("＋ Penjualan Baru", fontWeight = FontWeight.Bold) }) {
        if (error) Notice("Data belum dapat dimuat. Periksa koneksi internet lalu coba lagi.", true)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Kpi("Penjualan hari ini", money(total), "LIVE", true, Modifier.weight(1.25f))
            Kpi("Transaksi", sales.size.toString(), "ORDER", false, Modifier.weight(1f))
            Kpi("Rata-rata", money(avg.toLong()), "PER ORDER", false, Modifier.weight(1f))
            Kpi("Stok menipis", "5", "PERLU RESTOCK", false, Modifier.weight(1f), warning = true)
        }
        Spacer(Modifier.height(14.dp))
        if (LocalConfiguration.current.screenWidthDp >= 600) {
            Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Surface(Modifier.weight(1.45f).fillMaxHeight(), color = Surface, shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, Border)) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("Performa penjualan", fontWeight = FontWeight.Bold, fontSize = 16.sp); Text("7 hari terakhir", color = Muted, fontSize = 11.sp) }; Text("Rp ${money(total).removePrefix("Rp ")}", color = Blue, fontWeight = FontWeight.Bold) }
                        Spacer(Modifier.height(22.dp))
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Grafik penjualan", color = SoftBlue, fontSize = 13.sp) }
                    }
                }
                Surface(Modifier.weight(.9f).fillMaxHeight(), color = Surface, shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, Border)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Produk terlaris", fontWeight = FontWeight.Bold, fontSize = 16.sp); Text("Stok & volume", color = Muted, fontSize = 11.sp); Spacer(Modifier.height(12.dp))
                        listOf("Nasi Goreng", "Kopi Susu", "Mie Goreng", "Roti Tawar", "Es Teh Manis").forEachIndexed { i, name ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) { Text("0${i + 1}", color = SoftBlue, fontSize = 11.sp, modifier = Modifier.width(28.dp)); Text(name, Modifier.weight(1f), fontWeight = FontWeight.Medium); Text("${312 - i * 34} pcs", color = Muted, fontSize = 11.sp) }
                        }
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
        }
        Text("Transaksi terbaru", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Navy)
        if (loading) LinearProgressIndicator(Modifier.fillMaxWidth(), color = Blue) else LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) { items(sales.take(8)) { SaleRow(it) } }
    }
}

@Composable private fun Kpi(label: String, value: String, note: String, primary: Boolean, modifier: Modifier, warning: Boolean = false) {
    Surface(modifier, color = if (primary) Blue else Surface, shape = RoundedCornerShape(13.dp), border = if (primary) null else BorderStroke(1.dp, if (warning) Color(0xFFF0C4C4) else Border)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) { Text(label, color = if (primary) Color.White.copy(.75f) else Muted, fontSize = 11.sp, modifier = Modifier.weight(1f)); if (warning) Text("!", color = Danger, fontWeight = FontWeight.Bold) }
            Text(value, color = if (primary) Color.White else Text, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(note, color = if (primary) PaleBlue else if (warning) Danger else MidBlue, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable private fun SaleRow(sale: Sale) {
    Surface(Modifier.fillMaxWidth(), color = Surface, shape = RoundedCornerShape(10.dp), border = BorderStroke(1.dp, Border)) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(34.dp).background(PaleBlue, RoundedCornerShape(9.dp)), contentAlignment = Alignment.Center) { Text(initial(sale.sale_no), color = Blue, fontWeight = FontWeight.Bold, fontSize = 12.sp) }
            Spacer(Modifier.width(10.dp)); Column(Modifier.weight(1f)) { Text(sale.sale_no, fontWeight = FontWeight.SemiBold, fontSize = 13.sp); Text("Penjualan • ${sale.status}", color = Muted, fontSize = 10.sp) }
            Text(money(sale.total_amount), fontWeight = FontWeight.Bold, color = Navy, fontSize = 13.sp)
        }
    }
}

@Composable private fun Sales() {
    var products by remember { mutableStateOf<List<Product>>(emptyList()) }; var prices by remember { mutableStateOf<List<ProductPrice>>(emptyList()) }; var categories by remember { mutableStateOf<List<Category>>(emptyList()) }; var cart by remember { mutableStateOf<List<CartLine>>(emptyList()) }; var query by remember { mutableStateOf("") }; var selectedCategory by remember { mutableStateOf("Semua") }; var message by remember { mutableStateOf<String?>(null) }; var busy by remember { mutableStateOf(false) }; val scope = rememberCoroutineScope(); val tablet = LocalConfiguration.current.screenWidthDp >= 600
    LaunchedEffect(Unit) { try { val business = businessId(); products = supabase.from("products").select { filter { eq("business_id", business); eq("is_active", true) } }.decodeList(); prices = supabase.from("product_prices").select().decodeList(); categories = supabase.from("categories").select { filter { eq("business_id", business); eq("is_active", true) } }.decodeList() } catch (_: Exception) { message = "Produk belum dapat dimuat. Periksa koneksi internet lalu coba lagi." } }
    val filtered = products.filter { (selectedCategory == "Semua" || categories.firstOrNull { c -> c.id == it.category_id }?.name == selectedCategory) && (query.isBlank() || it.name.contains(query, true) || it.sku.contains(query, true)) }
    val total = cart.sumOf { it.price * it.qty }
    Page("Penjualan Baru", "Kasir cepat • retail & distributor", if (tablet) Button(onClick = { cart = emptyList() }, enabled = cart.isNotEmpty(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEAF1F8), contentColor = Blue), shape = RoundedCornerShape(9.dp)) { Text("Kosongkan") } else null) {
        message?.let { Notice(it, true) }
        if (tablet) Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            ProductCatalog(filtered, categories, selectedCategory, query, prices, Modifier.weight(1.55f), { query = it }, { selectedCategory = it }) { product -> cart = addToCart(cart, product, priceOf(product, prices)) }
            CartPanel(cart, total, Modifier.width(360.dp), { line -> cart = cart.map { if (it.product.id == line.product.id) it.copy(qty = it.qty + 1) else it } }, { line -> cart = cart.mapNotNull { if (it.product.id != line.product.id) it else if (it.qty <= 1) null else it.copy(qty = it.qty - 1) } }, { if (!busy) { busy = true; scope.launch { message = checkout(cart); if (message?.startsWith("Tersimpan") == true) cart = emptyList(); busy = false } } }, busy)
        } else {
            ProductCatalog(filtered, categories, selectedCategory, query, prices, Modifier.fillMaxWidth().weight(1f), { query = it }, { selectedCategory = it }) { product -> cart = addToCart(cart, product, priceOf(product, prices)) }
            if (cart.isNotEmpty()) CartPanel(cart, total, Modifier.fillMaxWidth().heightIn(max = 300.dp), { line -> cart = cart.map { if (it.product.id == line.product.id) it.copy(qty = it.qty + 1) else it } }, { line -> cart = cart.mapNotNull { if (it.product.id != line.product.id) it else if (it.qty <= 1) null else it.copy(qty = it.qty - 1) } }, { if (!busy) { busy = true; scope.launch { message = checkout(cart); if (message?.startsWith("Tersimpan") == true) cart = emptyList(); busy = false } } }, busy)
        }
    }
}

private fun priceOf(product: Product, prices: List<ProductPrice>) = prices.filter { it.product_id == product.id }.minByOrNull { it.min_qty }?.price ?: product.current_cost
private fun addToCart(cart: List<CartLine>, product: Product, price: Long): List<CartLine> { val old = cart.firstOrNull { it.product.id == product.id }; return if (old == null) cart + CartLine(product, price, 1) else cart.map { if (it.product.id == product.id) it.copy(qty = it.qty + 1) else it } }

@Composable private fun ProductCatalog(products: List<Product>, categories: List<Category>, selected: String, query: String, prices: List<ProductPrice>, modifier: Modifier, onQuery: (String) -> kotlin.Unit, onCategory: (String) -> kotlin.Unit, onAdd: (Product) -> kotlin.Unit) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(9.dp)) {
        OutlinedTextField(value = query, onValueChange = onQuery, modifier = Modifier.fillMaxWidth(), placeholder = { Text("Cari nama, SKU, atau scan barcode") }, singleLine = true, shape = RoundedCornerShape(10.dp))
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) { (listOf("Semua") + categories.map { it.name }).forEach { category -> FilterChip(selected = selected == category, onClick = { onCategory(category) }, label = { Text(category, fontSize = 12.sp) }) } }
        LazyVerticalGrid(columns = GridCells.Adaptive(148.dp), modifier = Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(9.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            items(products) { product ->
                Surface(Modifier.fillMaxWidth().clickable { onAdd(product) }, color = Surface, shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, Border)) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(Modifier.fillMaxWidth().height(74.dp).background(Color(0xFFEAF1F8), RoundedCornerShape(9.dp)), contentAlignment = Alignment.Center) { Text(initial(product.name), color = Blue, fontWeight = FontWeight.Bold, fontSize = 22.sp) }
                        Text(product.name, fontWeight = FontWeight.SemiBold, maxLines = 2, fontSize = 13.sp)
                        Text(product.sku, color = Muted, fontSize = 10.sp)
                        Row(verticalAlignment = Alignment.CenterVertically) { Text(money(priceOf(product, prices)), color = Blue, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.weight(1f)); Text("＋", color = MidBlue, fontWeight = FontWeight.Bold) }
                    }
                }
            }
        }
    }
}

@Composable private fun CartPanel(cart: List<CartLine>, total: Long, modifier: Modifier, onPlus: (CartLine) -> kotlin.Unit, onMinus: (CartLine) -> kotlin.Unit, onPay: () -> kotlin.Unit, busy: Boolean) {
    Surface(modifier, color = Surface, shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, Border)) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("Keranjang", fontWeight = FontWeight.Bold, fontSize = 17.sp); Text("${cart.sumOf { it.qty }} item • Pelanggan Umum", color = Muted, fontSize = 10.sp) }; Text("⌫", color = Muted) }
            HorizontalDivider(Modifier.padding(vertical = 10.dp))
            if (cart.isEmpty()) { Spacer(Modifier.weight(1f)); Text("Belum ada produk", Modifier.align(Alignment.CenterHorizontally), color = Muted, fontSize = 13.sp); Text("Pilih produk untuk mulai transaksi", Modifier.align(Alignment.CenterHorizontally), color = SoftBlue, fontSize = 10.sp); Spacer(Modifier.weight(1f)) }
            else {
                LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(9.dp)) { items(cart) { line ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(42.dp).background(Color(0xFFEAF1F8), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) { Text(initial(line.product.name), color = Blue, fontWeight = FontWeight.Bold) }
                        Spacer(Modifier.width(9.dp)); Column(Modifier.weight(1f)) { Text(line.product.name, fontWeight = FontWeight.SemiBold, fontSize = 12.sp); Text(money(line.price), color = Muted, fontSize = 10.sp) }
                        Row(verticalAlignment = Alignment.CenterVertically) { TextButton(onClick = { onMinus(line) }, contentPadding = PaddingValues(0.dp)) { Text("−") }; Text(line.qty.toString(), fontWeight = FontWeight.SemiBold, modifier = Modifier.width(22.dp)); TextButton(onClick = { onPlus(line) }, contentPadding = PaddingValues(0.dp)) { Text("+") } }
                        Text(money(line.price * line.qty), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                } }
                HorizontalDivider(Modifier.padding(vertical = 10.dp))
                Row(Modifier.fillMaxWidth()) { Text("Subtotal", color = Muted, fontSize = 12.sp); Spacer(Modifier.weight(1f)); Text(money(total), fontWeight = FontWeight.SemiBold, fontSize = 12.sp) }
                Row(Modifier.fillMaxWidth().padding(top = 5.dp)) { Text("Diskon", color = Muted, fontSize = 12.sp); Spacer(Modifier.weight(1f)); Text(money(0), fontSize = 12.sp) }
                Row(Modifier.fillMaxWidth().padding(top = 9.dp), verticalAlignment = Alignment.CenterVertically) { Text("TOTAL", fontWeight = FontWeight.Bold, fontSize = 15.sp); Spacer(Modifier.weight(1f)); Text(money(total), color = Navy, fontWeight = FontWeight.Black, fontSize = 20.sp) }
                Spacer(Modifier.height(11.dp)); Button(onClick = onPay, enabled = !busy && total > 0, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(9.dp), colors = ButtonDefaults.buttonColors(containerColor = Blue)) { Text(if (busy) "Menyimpan..." else "BAYAR / QRIS  ${money(total)}", fontWeight = FontWeight.Bold) }
                Text("Mode demo • pembayaran belum diverifikasi provider QRIS", color = Muted, fontSize = 9.sp, modifier = Modifier.padding(top = 6.dp))
            }
        }
    }
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
    } catch (e: Exception) { "Gagal menyimpan transaksi. Periksa koneksi dan coba lagi." }
}

@Composable private fun Products() {
    var products by remember { mutableStateOf<List<Product>>(emptyList()) }; var unit by remember { mutableStateOf<PosUnit?>(null) }; var priceList by remember { mutableStateOf<PriceList?>(null) }; var show by remember { mutableStateOf(false) }; var name by remember { mutableStateOf("") }; var sku by remember { mutableStateOf("") }; var price by remember { mutableStateOf("") }; var message by remember { mutableStateOf<String?>(null) }; val scope = rememberCoroutineScope()
    suspend fun reload() { val business = businessId(); products = supabase.from("products").select { filter { eq("business_id", business); eq("is_active", true) } }.decodeList(); unit = supabase.from("units").select { filter { eq("business_id", business); eq("code", "PCS") } }.decodeList<PosUnit>().firstOrNull(); priceList = supabase.from("price_lists").select { filter { eq("business_id", business); eq("is_default", true) } }.decodeList<PriceList>().firstOrNull() }
    LaunchedEffect(Unit) { try { reload() } catch (_: Exception) { message = "Produk belum dapat dimuat. Periksa koneksi internet lalu coba lagi." } }
    Page("Produk", "Katalog, SKU, harga jual, dan status produk", Button(onClick = { show = true }, shape = RoundedCornerShape(9.dp)) { Text("＋ Produk", fontWeight = FontWeight.Bold) }) {
        message?.let { Notice(it, it.startsWith("Gagal") || it.contains("belum dapat"),) }
        if (show) {
            Surface(Modifier.fillMaxWidth(), color = Surface, shape = RoundedCornerShape(13.dp), border = BorderStroke(1.dp, Border)) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text("Tambah produk", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text("Nama produk") }, singleLine = true)
                OutlinedTextField(sku, { sku = it }, Modifier.fillMaxWidth(), label = { Text("SKU") }, singleLine = true)
                OutlinedTextField(price, { price = it }, Modifier.fillMaxWidth(), label = { Text("Harga jual") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedButton(onClick = { show = false }) { Text("Batal") }; Button(onClick = { scope.launch { try { val business = businessId(); val u = unit ?: error("Unit PCS tidak ditemukan"); val list = priceList ?: error("Price list tidak ditemukan"); val value = price.toLongOrNull() ?: error("Harga tidak valid"); require(name.isNotBlank() && sku.isNotBlank()) { "Nama dan SKU wajib" }; val id = UUID.randomUUID().toString(); supabase.from("products").insert(ProductInsert(id, business, sku, name, name, null, u.id, value, value)); supabase.from("product_prices").insert(ProductPriceInsert(UUID.randomUUID().toString(), list.id, id, u.id, 1.0, value)); reload(); name = ""; sku = ""; price = ""; show = false; message = "Produk tersimpan" } catch (e: Exception) { message = "Gagal menyimpan produk. Periksa data lalu coba lagi." } } }) { Text("Simpan") } }
            } }
        }
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) { items(products) { product -> Surface(Modifier.fillMaxWidth(), color = Surface, shape = RoundedCornerShape(10.dp), border = BorderStroke(1.dp, Border)) { Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(40.dp).background(PaleBlue, RoundedCornerShape(9.dp)), contentAlignment = Alignment.Center) { Text(initial(product.name), color = Blue, fontWeight = FontWeight.Bold) }; Spacer(Modifier.width(10.dp)); Column(Modifier.weight(1f)) { Text(product.name, fontWeight = FontWeight.SemiBold); Text(product.sku, color = Muted, fontSize = 10.sp) }; TextButton(onClick = { scope.launch { try { supabase.from("products").update(ActiveUpdate(false)) { filter { eq("id", product.id) } }; reload(); message = "Produk dinonaktifkan" } catch (_: Exception) { message = "Gagal mengubah produk" } } }) { Text("Nonaktif", color = Danger) } } } } }
    }
}

@Composable private fun StockScreen() {
    var products by remember { mutableStateOf<List<Product>>(emptyList()) }; var stocks by remember { mutableStateOf<Map<String, Double>>(emptyMap()) }; var location by remember { mutableStateOf<Location?>(null) }; var message by remember { mutableStateOf<String?>(null) }; val scope = rememberCoroutineScope()
    suspend fun reload() { val business = businessId(); val branch = supabase.from("branches").select { filter { eq("business_id", business); eq("code", "MAIN") } }.decodeList<Branch>().first(); val loc = supabase.from("locations").select { filter { eq("branch_id", branch.id); eq("code", "STORE") } }.decodeList<Location>().first(); location = loc; products = supabase.from("products").select { filter { eq("business_id", business); eq("is_active", true) } }.decodeList(); stocks = supabase.from("stock_balances").select { filter { eq("location_id", loc.id) } }.decodeList<StockBalance>().associate { it.product_id to it.qty_base } }
    LaunchedEffect(Unit) { try { reload() } catch (_: Exception) { message = "Stok belum dapat dimuat. Periksa koneksi internet lalu coba lagi." } }
    Page("Stok", "Pantau persediaan, reorder, dan penyesuaian cepat") { message?.let { Notice(it, true) }; LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) { items(products) { product -> val qty = stocks[product.id] ?: 0.0; Surface(Modifier.fillMaxWidth(), color = Surface, shape = RoundedCornerShape(10.dp), border = BorderStroke(1.dp, Border)) { Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(38.dp).background(PaleBlue, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) { Text(initial(product.name), color = Blue, fontWeight = FontWeight.Bold) }; Spacer(Modifier.width(10.dp)); Column(Modifier.weight(1f)) { Text(product.name, fontWeight = FontWeight.SemiBold); Text(product.sku, color = Muted, fontSize = 10.sp) }; Text("${qty.toInt()} pcs", fontWeight = FontWeight.Bold, color = if (qty <= product.min_stock) Danger else Text); TextButton(onClick = { scope.launch { try { val loc = location ?: return@launch; supabase.from("stock_balances").update(StockUpdate(qty + 1)) { filter { eq("location_id", loc.id); eq("product_id", product.id) } }; reload() } catch (_: Exception) { message = "Gagal mengubah stok" } } }) { Text("+1", color = Blue) } } } } } }
}

@Composable private fun Customers() { var rows by remember { mutableStateOf<List<Customer>>(emptyList()) }; var message by remember { mutableStateOf<String?>(null) }; LaunchedEffect(Unit) { try { rows = supabase.from("customers").select { filter { eq("business_id", businessId()); eq("is_active", true) } }.decodeList() } catch (_: Exception) { message = "Pelanggan belum dapat dimuat. Periksa koneksi internet lalu coba lagi." } }; Page("Pelanggan", "Customer retail, grosir, dan akun tempo") { message?.let { Notice(it, true) }; LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) { items(rows) { row -> DataRow(row.name, row.phone ?: "Tanpa nomor") } } } }
@Composable private fun Payments() { var rows by remember { mutableStateOf<List<Payment>>(emptyList()) }; var message by remember { mutableStateOf<String?>(null) }; LaunchedEffect(Unit) { try { rows = supabase.from("payments").select { filter { eq("business_id", businessId()) } }.decodeList() } catch (_: Exception) { message = "Pembayaran belum dapat dimuat. Periksa koneksi internet lalu coba lagi." } }; Page("Pembayaran", "Monitoring cash, transfer, dan QRIS") { message?.let { Notice(it, true) }; if (rows.isEmpty()) Notice("Belum ada pembayaran") else LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) { items(rows) { row -> DataRow(row.payment_no, money(row.amount) + " • " + row.status) } } } }
@Composable private fun Reports() { var rows by remember { mutableStateOf<List<Sale>>(emptyList()) }; LaunchedEffect(Unit) { rows = try { supabase.from("sales").select { filter { eq("business_id", businessId()) } }.decodeList() } catch (_: Exception) { emptyList() } }; val total = rows.sumOf { it.total_amount }; Page("Laporan", "Penjualan, transaksi, dan ringkasan performa") { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) { Kpi("Penjualan", money(total), "SEMUA DATA", true, Modifier.weight(1.2f)); Kpi("Transaksi", rows.size.toString(), "ORDER", false, Modifier.weight(1f)) }; LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) { items(rows) { SaleRow(it) } } } }
@Composable private fun Settings() { Page("Pengaturan", "Konfigurasi toko, perangkat, dan koneksi") { Notice("Mode demo aktif • Supabase project terkonfigurasi"); Notice("Pembayaran PAID masih simulasi dan belum memverifikasi provider QRIS.") } }
@Composable private fun DataRow(name: String, detail: String) { Surface(Modifier.fillMaxWidth(), color = Surface, shape = RoundedCornerShape(10.dp), border = BorderStroke(1.dp, Border)) { Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(38.dp).background(PaleBlue, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) { Text(initial(name), color = Blue, fontWeight = FontWeight.Bold) }; Spacer(Modifier.width(10.dp)); Column(Modifier.weight(1f)) { Text(name, fontWeight = FontWeight.SemiBold); Text(detail, color = Muted, fontSize = 10.sp) } } } }
