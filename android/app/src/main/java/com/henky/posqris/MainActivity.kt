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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.henky.posqris.navigation.PosDestination
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
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
        setContent { PosTheme { PosApp() } }
    }
}

@Composable private fun PosTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = androidx.compose.material3.lightColorScheme(
        primary = androidx.compose.ui.graphics.Color(0xFF052659),
        onPrimary = androidx.compose.ui.graphics.Color.White,
        primaryContainer = androidx.compose.ui.graphics.Color(0xFFC1E8FF),
        secondary = androidx.compose.ui.graphics.Color(0xFF5483B3),
        background = androidx.compose.ui.graphics.Color(0xFFF5F8FC),
        surface = androidx.compose.ui.graphics.Color.White,
        surfaceVariant = androidx.compose.ui.graphics.Color(0xFFEAF1F8),
        outline = androidx.compose.ui.graphics.Color(0xFFD9E2EC)
    ), content = content)
}

@Serializable data class Business(val id: String, val code: String, val name: String)
@Serializable data class Branch(val id: String, val business_id: String, val code: String, val name: String)
@Serializable data class Location(val id: String, val branch_id: String, val code: String, val name: String)
@Serializable data class Category(val id: String, val business_id: String, val code: String, val name: String, val is_active: Boolean = true)
@Serializable data class Unit(val id: String, val business_id: String, val code: String, val name: String, val symbol: String? = null)
@Serializable data class PriceList(val id: String, val business_id: String, val name: String, val is_default: Boolean = false)
@Serializable data class Product(val id: String, val business_id: String, val sku: String, val name: String, val short_name: String? = null, val category_id: String? = null, val base_unit_id: String, val current_cost: Long = 0, val min_stock: Double = 0.0, val is_active: Boolean = true)
@Serializable data class ProductPrice(val id: String, val price_list_id: String, val product_id: String, val unit_id: String, val min_qty: Double = 1.0, val price: Long)
@Serializable data class Customer(val id: String, val business_id: String, val code: String, val name: String, val phone: String? = null, val is_active: Boolean = true)
@Serializable data class PaymentMethod(val id: String, val business_id: String, val code: String, val name: String, val method_type: String, val is_active: Boolean = true)
@Serializable data class Stock(val location_id: String, val product_id: String, val qty_base: Double = 0.0, val reserved_qty: Double = 0.0)
@Serializable data class SaleRow(val id: String, val sale_no: String, val total_amount: Long, val paid_amount: Long, val status: String, val sale_date: String)
@Serializable data class PaymentRow(val id: String, val sale_id: String? = null, val payment_method_id: String, val amount: Long, val status: String, val created_at: String)
@Serializable data class CartLine(val product: Product, val price: Long, val qty: Int)
@Serializable data class CheckoutResult(val sale_id: String, val sale_no: String, val total_amount: Long, val paid_amount: Long, val change_amount: Long, val sale_status: String)

private fun rupiah(v: Long) = "Rp " + v.toString().reversed().chunked(3).joinToString(".").reversed()
private fun icon(d: PosDestination) = when (d) {
    PosDestination.Dashboard -> "⌂"; PosDestination.Pos -> "▣"; PosDestination.Products -> "□"; PosDestination.Inventory -> "▥"
    PosDestination.Customers -> "♙"; PosDestination.Payments -> "Rp"; PosDestination.Reports -> "▤"; PosDestination.Settings -> "⚙"; else -> "•"
}

@Composable private fun PosApp() {
    var route by remember { mutableStateOf(PosDestination.Pos.route) }
    val items = remember { listOf(PosDestination.Dashboard, PosDestination.Pos, PosDestination.Products, PosDestination.Inventory, PosDestination.Customers, PosDestination.Payments, PosDestination.Reports, PosDestination.Settings) }
    val tablet = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp >= 600
    Row(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        if (tablet) SideNav(items, route) { route = it }
        Box(Modifier.weight(1f).fillMaxHeight()) {
            when (route) {
                PosDestination.Dashboard.route -> DashboardScreen { route = it }
                PosDestination.Pos.route -> PosScreen()
                PosDestination.Products.route -> ProductsScreen()
                PosDestination.Inventory.route -> InventoryScreen()
                PosDestination.Customers.route -> CustomersScreen()
                PosDestination.Payments.route -> PaymentsScreen()
                PosDestination.Reports.route -> ReportsScreen()
                PosDestination.Settings.route -> SettingsScreen()
                else -> PosScreen()
            }
            if (!tablet) BottomNav(items.take(4), route) { route = it }
        }
    }
}

@Composable private fun SideNav(items: List<PosDestination>, selected: String, onSelect: (String) -> Unit) {
    Surface(Modifier.width(210.dp).fillMaxHeight(), tonalElevation = 2.dp) {
        Column(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("POS", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
            Text("Point of Sale", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(18.dp))
            items.forEach { d -> NavButton(d, selected == d.route, Modifier.fillMaxWidth()) { onSelect(d.route) } }
        }
    }
}

@Composable private fun BottomNav(items: List<PosDestination>, selected: String, onSelect: (String) -> Unit) {
    Surface(Modifier.fillMaxWidth().navigationBarsPadding(), shadowElevation = 10.dp) {
        Row(Modifier.fillMaxWidth().padding(6.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            items.forEach { d -> NavButton(d, selected == d.route, Modifier.weight(1f)) { onSelect(d.route) } }
        }
    }
}

@Composable private fun NavButton(d: PosDestination, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Column(modifier.clip(RoundedCornerShape(12.dp)).background(if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface).clickable { onClick() }.padding(9.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(icon(d), fontWeight = FontWeight.Bold, color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
        Text(d.title, style = MaterialTheme.typography.labelMedium, maxLines = 1)
    }
}

@Composable private fun Page(title: String, subtitle: String, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxSize().padding(22.dp, 20.dp, 22.dp, 96.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        content()
    }
}

@Composable private fun DashboardScreen(onNavigate: (String) -> Unit) {
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
    val total = todaySales.sumOf { it.total_amount }
    val low = stock.count { s -> products.firstOrNull { it.id == s.product_id }?.let { s.qty_base <= it.min_stock } == true }
    Page("Beranda", business?.name ?: "Memuat data toko...") {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Metric("Penjualan hari ini", rupiah(total), "${todaySales.size} transaksi", Modifier.weight(1.4f), true)
            Metric("Transaksi", todaySales.size.toString(), "selesai", Modifier.weight(1f))
            Metric("Stok menipis", low.toString(), "perlu perhatian", Modifier.weight(1f))
        }
        Button({ onNavigate(PosDestination.Pos.route) }, Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(14.dp)) { Text("▣  Transaksi baru", fontWeight = FontWeight.Bold) }
        Text("Aktivitas terakhir", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Card(shape = RoundedCornerShape(16.dp)) {
            LazyColumn(Modifier.height(280.dp)) {
                items(sales.takeLast(10).reversed()) { s ->
                    Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) { Text(s.sale_no, fontWeight = FontWeight.SemiBold); Text(s.status, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        Text(rupiah(s.total_amount), fontWeight = FontWeight.Bold)
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable private fun Metric(title: String, value: String, note: String, modifier: Modifier, accent: Boolean = false) {
    Card(modifier, shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(if (accent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(17.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium, color = if (accent) MaterialTheme.colorScheme.onPrimary.copy(.75f) else MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = if (accent) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
            Text(note, style = MaterialTheme.typography.labelSmall, color = if (accent) MaterialTheme.colorScheme.onPrimary.copy(.75f) else MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable private fun PosScreen() {
    var business by remember { mutableStateOf<Business?>(null) }
    var branch by remember { mutableStateOf<Branch?>(null) }
    var location by remember { mutableStateOf<Location?>(null) }
    var products by remember { mutableStateOf<List<Product>>(emptyList()) }
    var prices by remember { mutableStateOf<List<ProductPrice>>(emptyList()) }
    var categories by remember { mutableStateOf<List<Category>>(emptyList()) }
    var customers by remember { mutableStateOf<List<Customer>>(emptyList()) }
    var methods by remember { mutableStateOf<List<PaymentMethod>>(emptyList()) }
    var cart by remember { mutableStateOf<List<CartLine>>(emptyList()) }
    var customer by remember { mutableStateOf<Customer?>(null) }
    var query by remember { mutableStateOf("") }
    var cat by remember { mutableStateOf("Semua") }
    var showPay by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) { runCatching {
        val b = supabase.from("businesses").select { filter { eq("is_active", true) } }.decodeList<Business>().first()
        business = b
        val br = supabase.from("branches").select { filter { eq("business_id", b.id); eq("is_active", true) } }.decodeList<Branch>().first()
        branch = br
        location = supabase.from("locations").select { filter { eq("branch_id", br.id); eq("is_active", true) } }.decodeList<Location>().first()
        products = supabase.from("products").select { filter { eq("business_id", b.id); eq("is_active", true) } }.decodeList<Product>()
        categories = supabase.from("categories").select { filter { eq("business_id", b.id); eq("is_active", true) } }.decodeList<Category>()
        val pl = supabase.from("price_lists").select { filter { eq("business_id", b.id); eq("is_default", true); eq("is_active", true) } }.decodeList<PriceList>().firstOrNull()
        if (pl != null) prices = supabase.from("product_prices").select { filter { eq("price_list_id", pl.id) } }.decodeList<ProductPrice>()
        customers = supabase.from("customers").select { filter { eq("business_id", b.id); eq("is_active", true) } }.decodeList<Customer>()
        methods = supabase.from("payment_methods").select { filter { eq("business_id", b.id); eq("is_active", true) } }.decodeList<PaymentMethod>()
    } }
    val priceOf: (Product) -> Long = { p -> prices.filter { it.product_id == p.id }.minByOrNull { it.min_qty }?.price ?: 0L }
    val filtered = products.filter { p ->
        (query.isBlank() || p.name.contains(query, true) || p.sku.contains(query, true)) &&
            (cat == "Semua" || categories.firstOrNull { it.id == p.category_id }?.name == cat)
    }
    val total = cart.sumOf { it.price * it.qty }
    val tablet = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp >= 600
    if (tablet) {
        Row(Modifier.fillMaxSize().padding(20.dp, 18.dp), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) { Text("Penjualan", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold); Text(branch?.name ?: "Memuat cabang...", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    Text("${cart.sumOf { it.qty }} item", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
                OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), placeholder = { Text("Cari produk atau SKU") }, singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) { (listOf("Semua") + categories.map { it.name }).distinct().forEach { x -> OutlinedButton({ cat = x }) { Text(x) } } }
                LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) { items(filtered) { p -> ProductCard(p, priceOf(p)) { cart = addCart(cart, p, priceOf(p)) } } }
            }
            CartPanel(cart, total, customer, { customer = it }, { id -> cart = changeQty(cart, id, -1) }, { id -> cart = changeQty(cart, id, 1) }) { if (cart.isNotEmpty()) showPay = true }
        }
    } else {
        Column(Modifier.fillMaxSize().padding(16.dp, 12.dp, 16.dp, 88.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Penjualan", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
            Text(branch?.name ?: "Memuat cabang...", color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), placeholder = { Text("Cari produk atau SKU") }, singleLine = true)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { (listOf("Semua") + categories.map { it.name }).distinct().forEach { x -> OutlinedButton({ cat = x }) { Text(x) } } }
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) { items(filtered) { p -> ProductCard(p, priceOf(p)) { cart = addCart(cart, p, priceOf(p)) } } }
            Card(shape = RoundedCornerShape(15.dp)) { Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("${cart.sumOf { it.qty }} item", fontWeight = FontWeight.Bold); Text(rupiah(total)) }; Button({ showPay = true }, enabled = cart.isNotEmpty()) { Text("Bayar") } } }
        }
    }
    if (showPay) PaymentDialog(total, cart, methods, customers, customer, { customer = it }, { showPay = false }, { showPay = false; message = it })
    message?.let { AlertDialog(onDismissRequest = { message = null }, confirmButton = { TextButton({ message = null }) { Text("Tutup") } }, title = { Text("Transaksi") }, text = { Text(it) }) }
}

private fun addCart(cart: List<CartLine>, product: Product, price: Long): List<CartLine> = cart.firstOrNull { it.product.id == product.id }?.let { old -> cart.map { if (it.product.id == product.id) it.copy(qty = it.qty + 1) else it } } ?: (cart + CartLine(product, price, 1))
private fun changeQty(cart: List<CartLine>, id: String, delta: Int): List<CartLine> = cart.mapNotNull { line -> if (line.product.id != id) line else { val q = line.qty + delta; if (q <= 0) null else line.copy(qty = q) } }

@Composable private fun ProductCard(p: Product, price: Long, onAdd: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable { onAdd() }, shape = RoundedCornerShape(14.dp)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Box(Modifier.fillMaxWidth().height(62.dp).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) { Text(p.sku, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary) }
            Text(p.name, fontWeight = FontWeight.Bold)
            Text(rupiah(price), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable private fun CartPanel(cart: List<CartLine>, total: Long, customer: Customer?, onCustomer: (Customer?) -> Unit, minus: (String) -> Unit, plus: (String) -> Unit, pay: () -> Unit) {
    Card(Modifier.width(370.dp).fillMaxHeight(), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.fillMaxSize().padding(18.dp)) {
            Text("Keranjang", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
            Text(customer?.name ?: "Pelanggan Umum", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(10.dp))
            LazyColumn(Modifier.weight(1f)) { items(cart) { l -> Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(l.product.name, fontWeight = FontWeight.SemiBold); Text(rupiah(l.price * l.qty), style = MaterialTheme.typography.bodySmall) }; TextButton({ minus(l.product.id) }) { Text("−") }; Text(l.qty.toString()); TextButton({ plus(l.product.id) }) { Text("+") } }; HorizontalDivider() } }
            Row(Modifier.fillMaxWidth().padding(top = 12.dp)) { Text("Total", fontWeight = FontWeight.Bold); Spacer(Modifier.weight(1f)); Text(rupiah(total), fontWeight = FontWeight.ExtraBold) }
            Spacer(Modifier.height(10.dp)); Button(pay, Modifier.fillMaxWidth().height(52.dp), enabled = cart.isNotEmpty(), shape = RoundedCornerShape(13.dp)) { Text("Bayar", fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable private fun PaymentDialog(total: Long, cart: List<CartLine>, methods: List<PaymentMethod>, customers: List<Customer>, customer: Customer?, onCustomer: (Customer?) -> Unit, close: () -> Unit, done: (String) -> Unit) {
    var selected by remember { mutableStateOf(methods.firstOrNull { it.code == "CASH" } ?: methods.firstOrNull()) }
    var amount by remember { mutableStateOf(total.toString()) }
    var cash by remember { mutableStateOf(total.toString()) }
    var ref by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    AlertDialog(onDismissRequest = close, title = { Text("Pembayaran", fontWeight = FontWeight.ExtraBold) }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Total ${rupiah(total)}", fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { methods.forEach { m -> OutlinedButton({ selected = m }) { Text(m.name) } } }
            OutlinedTextField(amount, { amount = it.filter(Char::isDigit) }, Modifier.fillMaxWidth(), label = { Text("Nominal") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
            if (selected?.code == "CASH") OutlinedTextField(cash, { cash = it.filter(Char::isDigit) }, Modifier.fillMaxWidth(), label = { Text("Uang diterima") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
            if (selected?.code == "TRANSFER") OutlinedTextField(ref, { ref = it }, Modifier.fillMaxWidth(), label = { Text("Referensi transfer") }, singleLine = true)
            if (selected?.code == "QRIS") Card(colors = CardDefaults.cardColors(MaterialTheme.colorScheme.primaryContainer)) { Column(Modifier.padding(12.dp)) { Text("QRIS", fontWeight = FontWeight.Bold); Text("Menunggu konfirmasi pembayaran dari provider/kasir.", style = MaterialTheme.typography.bodySmall) } }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }, confirmButton = {
        Button({
            val m = selected ?: return@Button
            val a = amount.toLongOrNull() ?: 0L
            val c = cash.toLongOrNull() ?: a
            if (a <= 0) { error = "Nominal pembayaran tidak valid"; return@Button }
            if (m.code == "CASH" && c < a) { error = "Uang tunai kurang"; return@Button }
            if (m.code == "TRANSFER" && ref.isBlank()) { error = "Referensi transfer wajib"; return@Button }
            scope.launch {
                saving = true
                runCatching {
                    val b = supabase.from("businesses").select { filter { eq("is_active", true) } }.decodeList<Business>().first()
                    val br = supabase.from("branches").select { filter { eq("business_id", b.id); eq("is_active", true) } }.decodeList<Branch>().first()
                    val loc = supabase.from("locations").select { filter { eq("branch_id", br.id); eq("is_active", true) } }.decodeList<Location>().first()
                    val items = buildJsonArray { cart.forEach { l -> add(buildJsonObject { put("product_id", l.product.id); put("unit_id", l.product.base_unit_id); put("sku", l.product.sku); put("name", l.product.name); put("qty", l.qty); put("unit_price", l.price); put("hpp_unit", l.product.current_cost) }) } }
                    val payments = buildJsonArray { add(buildJsonObject { put("payment_method_id", m.id); put("amount", a); put("cash_received", c); put("reference", ref); put("qris_confirmed", false) }) }
                    supabase.postgrest.rpc("checkout_sale_multi_payment", buildJsonObject { put("p_branch_id", br.id); put("p_location_id", loc.id); customer?.id?.let { put("p_customer_id", it) }; put("p_items", items); put("p_payments", payments); put("p_idempotency_key", "android-${UUID.randomUUID()}") }).decodeList<CheckoutResult>().first()
                }.onSuccess { r -> done(if (r.sale_status == "COMPLETED") "Transaksi ${r.sale_no} tersimpan • ${rupiah(r.total_amount)}" else "${r.sale_no} tersimpan belum lunas • ${rupiah(r.paid_amount)} / ${rupiah(r.total_amount)}") }.onFailure { e -> error = e.message ?: "Gagal menyimpan transaksi" }
                saving = false
            }
        }, enabled = selected != null && !saving) { Text(if (saving) "Menyimpan..." else "Proses") }
    }, dismissButton = { TextButton(close) { Text("Batal") } })
}

@Composable private fun ProductsScreen() { var data by remember { mutableStateOf<List<Product>>(emptyList()) }; LaunchedEffect(Unit) { data = runCatching { supabase.from("products").select { filter { eq("is_active", true) } }.decodeList<Product>() }.getOrDefault(emptyList()) }; Page("Produk", "Katalog aktif dari Supabase") { Card { LazyColumn(Modifier.height(520.dp)) { items(data) { p -> Row(Modifier.fillMaxWidth().padding(15.dp)) { Column(Modifier.weight(1f)) { Text(p.name, fontWeight = FontWeight.Bold); Text(p.sku, style = MaterialTheme.typography.bodySmall) }; Text(rupiah(p.current_cost)) }; HorizontalDivider() } } } } }
@Composable private fun InventoryScreen() { var products by remember { mutableStateOf<List<Product>>(emptyList()) }; var stock by remember { mutableStateOf<List<Stock>>(emptyList()) }; LaunchedEffect(Unit) { products = runCatching { supabase.from("products").select { filter { eq("is_active", true) } }.decodeList<Product>() }.getOrDefault(emptyList()); stock = runCatching { supabase.from("stock_balances").select().decodeList<Stock>() }.getOrDefault(emptyList()) }; Page("Persediaan", "Stok aktual per lokasi") { Card { LazyColumn(Modifier.height(520.dp)) { items(products) { p -> val s = stock.firstOrNull { it.product_id == p.id }; Row(Modifier.fillMaxWidth().padding(15.dp)) { Column(Modifier.weight(1f)) { Text(p.name, fontWeight = FontWeight.SemiBold); Text("Min. ${p.min_stock}", style = MaterialTheme.typography.labelSmall) }; Text("${s?.qty_base ?: 0}") }; HorizontalDivider() } } } } }
@Composable private fun CustomersScreen() { var data by remember { mutableStateOf<List<Customer>>(emptyList()) }; LaunchedEffect(Unit) { data = runCatching { supabase.from("customers").select { filter { eq("is_active", true) } }.decodeList<Customer>() }.getOrDefault(emptyList()) }; Page("Pelanggan", "Data pelanggan aktif") { Card { LazyColumn(Modifier.height(520.dp)) { items(data) { c -> Row(Modifier.fillMaxWidth().padding(15.dp)) { Column(Modifier.weight(1f)) { Text(c.name, fontWeight = FontWeight.Bold); Text(c.code) }; Text(c.phone ?: "-") }; HorizontalDivider() } } } } }
@Composable private fun PaymentsScreen() { var data by remember { mutableStateOf<List<PaymentRow>>(emptyList()) }; LaunchedEffect(Unit) { data = runCatching { supabase.from("payments").select().decodeList<PaymentRow>() }.getOrDefault(emptyList()) }; Page("Pembayaran", "Riwayat pembayaran dari Supabase") { Card { LazyColumn(Modifier.height(520.dp)) { items(data.takeLast(30).reversed()) { p -> Row(Modifier.fillMaxWidth().padding(15.dp)) { Text(p.status, Modifier.weight(1f), fontWeight = FontWeight.SemiBold); Text(rupiah(p.amount)) }; HorizontalDivider() } } } } }
@Composable private fun ReportsScreen() { var data by remember { mutableStateOf<List<SaleRow>>(emptyList()) }; LaunchedEffect(Unit) { data = runCatching { supabase.from("sales").select().decodeList<SaleRow>() }.getOrDefault(emptyList()) }; val completed = data.filter { it.status == "COMPLETED" }; Page("Laporan", "Penjualan aktual dari database") { Metric("Total penjualan", rupiah(completed.sumOf { it.total_amount }), "${completed.size} transaksi", Modifier.fillMaxWidth(), true); Card { LazyColumn(Modifier.height(420.dp)) { items(completed.takeLast(20).reversed()) { s -> Row(Modifier.fillMaxWidth().padding(15.dp)) { Text(s.sale_no, Modifier.weight(1f)); Text(rupiah(s.total_amount), fontWeight = FontWeight.Bold) }; HorizontalDivider() } } } } }
@Composable private fun SettingsScreen() { var b by remember { mutableStateOf<Business?>(null) }; var br by remember { mutableStateOf<Branch?>(null) }; var loc by remember { mutableStateOf<Location?>(null) }; LaunchedEffect(Unit) { runCatching { b = supabase.from("businesses").select { filter { eq("is_active", true) } }.decodeList<Business>().first(); br = supabase.from("branches").select { filter { eq("business_id", b!!.id); eq("is_active", true) } }.decodeList<Branch>().first(); loc = supabase.from("locations").select { filter { eq("branch_id", br!!.id); eq("is_active", true) } }.decodeList<Location>().first() } }; Page("Pengaturan", "Konfigurasi operasional") { Card { Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { Text(b?.name ?: "-", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); Text("Cabang: ${br?.name ?: "-"}"); Text("Lokasi stok: ${loc?.name ?: "-"}"); Text("Mata uang: IDR"); Text("Zona waktu: Asia/Jakarta") } } } }
