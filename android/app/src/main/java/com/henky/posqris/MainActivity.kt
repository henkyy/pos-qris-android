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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.henky.posqris.navigation.PosDestination
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import java.util.UUID

private val supabase = createSupabaseClient(BuildConfig.SUPABASE_URL, BuildConfig.SUPABASE_PUBLISHABLE_KEY) {
    install(Postgrest)
}

private const val DEMO_CODE = "DEMO"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { PosTheme { PosApp() } }
    }
}

@Composable
private fun PosTheme(content: @Composable () -> Unit) {
    val scheme = androidx.compose.material3.lightColorScheme(
        primary = androidx.compose.ui.graphics.Color(0xFF155EEF),
        onPrimary = androidx.compose.ui.graphics.Color.White,
        primaryContainer = androidx.compose.ui.graphics.Color(0xFFE8F0FF),
        secondary = androidx.compose.ui.graphics.Color(0xFF475467),
        surface = androidx.compose.ui.graphics.Color.White,
        background = androidx.compose.ui.graphics.Color(0xFFF7F8FA),
        surfaceVariant = androidx.compose.ui.graphics.Color(0xFFF0F2F5),
        outline = androidx.compose.ui.graphics.Color(0xFFD0D5DD)
    )
    MaterialTheme(colorScheme = scheme, typography = androidx.compose.material3.Typography(), content = content)
}

@Serializable
data class BusinessRow(val id: String, val code: String, val name: String)

@Serializable
data class BranchRow(val id: String, val business_id: String, val name: String)

@Serializable
data class LocationRow(val id: String, val branch_id: String, val name: String)

@Serializable
data class UnitRow(val id: String, val business_id: String, val name: String, val symbol: String? = null)

@Serializable
data class CategoryRow(val id: String, val business_id: String, val name: String)

@Serializable
data class PriceListRow(val id: String, val business_id: String, val name: String, val is_default: Boolean = false)

@Serializable
data class ProductRow(
    val id: String,
    val business_id: String,
    val sku: String,
    val name: String,
    val short_name: String? = null,
    val category_id: String? = null,
    val base_unit_id: String,
    val current_cost: Long = 0,
    val is_active: Boolean = true
)

@Serializable
data class ProductPriceRow(val id: String, val price_list_id: String, val product_id: String, val unit_id: String, val price: Long)

@Serializable
data class CustomerRow(val id: String, val business_id: String, val code: String, val name: String, val phone: String? = null, val is_active: Boolean = true)

@Serializable
data class PaymentMethodRow(val id: String, val business_id: String, val code: String, val name: String, val method_type: String, val is_active: Boolean = true)

@Serializable
data class StockRow(val location_id: String, val product_id: String, val qty_base: Double = 0.0, val reserved_qty: Double = 0.0)

@Serializable
data class CartLine(val product: ProductRow, val price: Long, val qty: Int)

@Serializable
data class SaleInsert(
    val id: String,
    val business_id: String,
    val branch_id: String,
    val location_id: String,
    val customer_id: String? = null,
    val sale_no: String,
    val status: String = "COMPLETED",
    val subtotal: Long,
    val discount_amount: Long = 0,
    val tax_amount: Long = 0,
    val service_charge: Long = 0,
    val rounding_amount: Long = 0,
    val total_amount: Long,
    val paid_amount: Long,
    val change_amount: Long = 0,
    val hpp_amount: Long,
    val margin_amount: Long,
    val notes: String? = "Demo POS QRIS"
)

@Serializable
data class SaleItemInsert(
    val id: String,
    val sale_id: String,
    val product_id: String,
    val unit_id: String,
    val product_sku_snapshot: String,
    val product_name_snapshot: String,
    val qty: Double,
    val conversion_to_base: Double = 1.0,
    val unit_price: Long,
    val discount_amount: Long = 0,
    val tax_amount: Long = 0,
    val line_total: Long,
    val hpp_unit: Long,
    val hpp_total: Long
)

@Serializable
data class PaymentInsert(
    val id: String,
    val business_id: String,
    val branch_id: String,
    val sale_id: String,
    val payment_method_id: String,
    val qris_configuration_id: String? = null,
    val payment_no: String,
    val amount: Long,
    val currency_code: String = "IDR",
    val status: String = "PAID",
    val provider: String? = "DEMO",
    val external_transaction_id: String? = null,
    val idempotency_key: String? = null,
    val qr_reference: String? = null,
    val reconciliation_status: String = "UNRECONCILED"
)

private fun rupiah(value: Long): String = "Rp " + value.toString().reversed().chunked(3).joinToString(".").reversed()

private fun navIcon(destination: PosDestination): String = when (destination) {
    PosDestination.Dashboard -> "⌂"
    PosDestination.Pos -> "＋"
    PosDestination.Products -> "□"
    PosDestination.Inventory -> "▥"
    PosDestination.Customers -> "♙"
    PosDestination.Payments -> "Rp"
    PosDestination.Reports -> "▤"
    PosDestination.Settings -> "⚙"
    else -> "•"
}

@Composable
private fun PosApp() {
    var selected by remember { mutableStateOf(PosDestination.Pos.route) }
    val destinations = remember {
        listOf(PosDestination.Dashboard, PosDestination.Pos, PosDestination.Products, PosDestination.Inventory, PosDestination.Customers, PosDestination.Payments, PosDestination.Reports, PosDestination.Settings)
    }
    val width = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp
    val tablet = width >= 600

    Row(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        if (tablet) {
            TabletRail(destinations, selected) { selected = it }
        }
        Box(Modifier.weight(1f).fillMaxHeight()) {
            when (selected) {
                PosDestination.Dashboard.route -> DashboardScreen { selected = it }
                PosDestination.Pos.route -> SalesScreen()
                PosDestination.Products.route -> ProductsScreen()
                PosDestination.Inventory.route -> InventoryScreen()
                PosDestination.Customers.route -> CustomersScreen()
                PosDestination.Payments.route -> PaymentsScreen()
                PosDestination.Reports.route -> ReportsScreen()
                PosDestination.Settings.route -> SettingsScreen()
                else -> SalesScreen()
            }
            if (!tablet) PhoneBottomNav(destinations.take(4), selected) { selected = it }
        }
    }
}

@Composable
private fun TabletRail(destinations: List<PosDestination>, selected: String, onSelect: (String) -> Unit) {
    Surface(modifier = Modifier.width(88.dp).fillMaxHeight(), tonalElevation = 1.dp) {
        Column(Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(46.dp).clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
                Text("P", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }
            Spacer(Modifier.height(18.dp))
            destinations.forEach { item ->
                NavItem(item, selected == item.route, compact = true) { onSelect(item.route) }
            }
        }
    }
}

@Composable
private fun PhoneBottomNav(destinations: List<PosDestination>, selected: String, onSelect: (String) -> Unit) {
    Surface(Modifier.fillMaxWidth().navigationBarsPadding(), shadowElevation = 8.dp) {
        Row(Modifier.fillMaxWidth().padding(6.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            destinations.forEach { item -> NavItem(item, selected == item.route, compact = false, Modifier.weight(1f)) { onSelect(item.route) } }
        }
    }
}

@Composable
private fun NavItem(item: PosDestination, selected: Boolean, compact: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface).clickable { onClick() }.padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(navIcon(item), color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
        Text(if (compact) item.title else when (item) { PosDestination.Dashboard -> "Beranda"; PosDestination.Pos -> "Transaksi"; PosDestination.Products -> "Produk"; else -> "Stok" }, style = MaterialTheme.typography.labelSmall, maxLines = 1)
    }
}

@Composable
private fun Page(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(modifier.fillMaxSize().padding(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 92.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) { content() }
}

@Composable
private fun Header(title: String, subtitle: String, action: (@Composable () -> Unit)? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        action?.invoke()
    }
}

@Composable
private fun DashboardScreen(onNavigate: (String) -> Unit) {
    Page {
        Header("Beranda", "Ringkasan toko dan akses cepat")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatBlock("Penjualan hari ini", rupiah(1_250_000), "+12,5%", Modifier.weight(1.5f), true)
            StatBlock("Transaksi", "24", "hari ini", Modifier.weight(1f))
            StatBlock("QRIS", rupiah(875_000), "70% penjualan", Modifier.weight(1f))
        }
        Button(onClick = { onNavigate(PosDestination.Pos.route) }, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp)) { Text("＋  Transaksi baru") }
        Text("Operasional", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            QuickAction("Produk", "Kelola katalog") { onNavigate(PosDestination.Products.route) }
            QuickAction("Stok", "Pantau persediaan") { onNavigate(PosDestination.Inventory.route) }
            QuickAction("Laporan", "Lihat performa") { onNavigate(PosDestination.Reports.route) }
        }
        Card(shape = RoundedCornerShape(16.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Status toko", fontWeight = FontWeight.Bold)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(10.dp).clip(CircleShape).background(androidx.compose.ui.graphics.Color(0xFF12B76A)))
                    Spacer(Modifier.width(8.dp))
                    Text("Toko Demo • Online")
                    Spacer(Modifier.weight(1f))
                    Text("Kasir aktif", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
private fun StatBlock(title: String, value: String, note: String, modifier: Modifier, emphasis: Boolean = false) {
    Card(modifier, shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(if (emphasis) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(16.dp)) {
            Text(title, color = if (emphasis) MaterialTheme.colorScheme.onPrimary.copy(alpha = .78f) else MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
            Text(value, fontSize = if (emphasis) 25.sp else 20.sp, fontWeight = FontWeight.Bold, color = if (emphasis) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
            Text(note, style = MaterialTheme.typography.labelSmall, color = if (emphasis) MaterialTheme.colorScheme.onPrimary.copy(alpha = .78f) else MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun QuickAction(title: String, subtitle: String, onClick: () -> Unit) {
    Card(Modifier.weight(1f).clickable { onClick() }, shape = RoundedCornerShape(14.dp)) {
        Column(Modifier.padding(14.dp)) { Text(title, fontWeight = FontWeight.SemiBold); Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable
private fun SalesScreen() {
    var business by remember { mutableStateOf<BusinessRow?>(null) }
    var branch by remember { mutableStateOf<BranchRow?>(null) }
    var location by remember { mutableStateOf<LocationRow?>(null) }
    var products by remember { mutableStateOf<List<ProductRow>>(emptyList()) }
    var prices by remember { mutableStateOf<List<ProductPriceRow>>(emptyList()) }
    var units by remember { mutableStateOf<List<UnitRow>>(emptyList()) }
    var categories by remember { mutableStateOf<List<CategoryRow>>(emptyList()) }
    var customers by remember { mutableStateOf<List<CustomerRow>>(emptyList()) }
    var paymentMethods by remember { mutableStateOf<List<PaymentMethodRow>>(emptyList()) }
    var cart by remember { mutableStateOf<List<CartLine>>(emptyList()) }
    var query by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Semua") }
    var message by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val tablet = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp >= 600

    LaunchedEffect(Unit) {
        try {
            val b = supabase.from("businesses").select { filter { eq("code", DEMO_CODE) } }.decodeList<BusinessRow>().firstOrNull()
            business = b
            if (b != null) {
                branch = supabase.from("branches").select { filter { eq("business_id", b.id); eq("code", "MAIN") } }.decodeList<BranchRow>().firstOrNull()
                units = supabase.from("units").select { filter { eq("business_id", b.id) } }.decodeList()
                categories = supabase.from("categories").select { filter { eq("business_id", b.id) } }.decodeList()
                customers = supabase.from("customers").select { filter { eq("business_id", b.id); eq("is_active", true) } }.decodeList()
                paymentMethods = supabase.from("payment_methods").select { filter { eq("business_id", b.id); eq("is_active", true) } }.decodeList()
                val bl = branch
                if (bl != null) {
                    location = supabase.from("locations").select { filter { eq("branch_id", bl.id); eq("code", "STORE") } }.decodeList<LocationRow>().firstOrNull()
                }
                products = supabase.from("products").select { filter { eq("business_id", b.id); eq("is_active", true) } }.decodeList()
                val pl = supabase.from("price_lists").select { filter { eq("business_id", b.id); eq("is_default", true) } }.decodeList<PriceListRow>().firstOrNull()
                if (pl != null) prices = supabase.from("product_prices").select { filter { eq("price_list_id", pl.id) } }.decodeList()
            }
        } catch (e: Exception) { message = "Gagal memuat data: ${e.message ?: "koneksi"}" }
    }

    val categoryNames = listOf("Semua") + categories.map { it.name }
    val filtered = products.filter { p -> (category == "Semua" || categories.firstOrNull { it.id == p.category_id }?.name == category) && (query.isBlank() || p.name.contains(query, true) || p.sku.contains(query, true)) }
    val total = cart.sumOf { it.price * it.qty }

    Page {
        Header("Transaksi", "Kasir • Toko Demo • Online")
        if (message != null) Notice(message!!)
        if (tablet) {
            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                SalesCatalog(filtered, categoryNames, category, query, cart, prices, Modifier.weight(1.45f), { query = it }, { category = it }) { product ->
                    val price = prices.firstOrNull { it.product_id == product.id }?.price ?: product.current_cost
                    val existing = cart.firstOrNull { it.product.id == product.id }
                    cart = if (existing == null) cart + CartLine(product, price, 1) else cart.map { if (it.product.id == product.id) it.copy(qty = it.qty + 1) else it }
                }
                CartPanel(cart, total, Modifier.weight(.85f), { line -> cart = cart.map { if (it.product.id == line.product.id) it.copy(qty = it.qty + 1) else it } }, { line -> cart = cart.mapNotNull { if (it.product.id != line.product.id) it else if (it.qty <= 1) null else it.copy(qty = it.qty - 1) } }, {
                    if (!saving) {
                        saving = true
                        scope.launch { message = saveSale(business, branch, location, cart, customers.firstOrNull(), paymentMethods, units, prices); if (message?.startsWith("Tersimpan") == true) cart = emptyList(); saving = false }
                    }
                }, saving)
            }
        } else {
            SalesCatalog(filtered, categoryNames, category, query, cart, prices, Modifier.fillMaxWidth(), { query = it }, { category = it }) { product ->
                val price = prices.firstOrNull { it.product_id == product.id }?.price ?: product.current_cost
                val existing = cart.firstOrNull { it.product.id == product.id }
                cart = if (existing == null) cart + CartLine(product, price, 1) else cart.map { if (it.product.id == product.id) it.copy(qty = it.qty + 1) else it }
            }
            if (cart.isNotEmpty()) CartPanel(cart, total, Modifier.fillMaxWidth(), { line -> cart = cart.map { if (it.product.id == line.product.id) it.copy(qty = it.qty + 1) else it } }, { line -> cart = cart.mapNotNull { if (it.product.id != line.product.id) it else if (it.qty <= 1) null else it.copy(qty = it.qty - 1) } }, {
                if (!saving) { saving = true; scope.launch { message = saveSale(business, branch, location, cart, customers.firstOrNull(), paymentMethods, units, prices); if (message?.startsWith("Tersimpan") == true) cart = emptyList(); saving = false } }
            }, saving)
        }
    }
}

@Composable
private fun SalesCatalog(products: List<ProductRow>, categories: List<String>, selectedCategory: String, query: String, cart: List<CartLine>, prices: List<ProductPriceRow>, modifier: Modifier, onQuery: (String) -> Unit, onCategory: (String) -> Unit, onAdd: (ProductRow) -> Unit) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(query, onQuery, Modifier.fillMaxWidth(), label = { Text("Cari produk atau SKU") }, singleLine = true)
        LazyColumn(Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { categories.forEach { c -> FilterChip(c, selectedCategory == c) { onCategory(c) } } } }
            items(products, key = { it.id }) { product ->
                val price = prices.firstOrNull { it.product_id == product.id }?.price ?: product.current_cost
                ProductTile(product, price, cart.firstOrNull { it.product.id == product.id }?.qty ?: 0) { onAdd(product) }
            }
        }
    }
}

@Composable
private fun FilterChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Surface(shape = RoundedCornerShape(20.dp), color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface, modifier = Modifier.clickable { onClick() }) { Text(text, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp), color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.labelMedium) }
}

@Composable
private fun ProductTile(product: ProductRow, price: Long, qty: Int, onAdd: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable { onAdd() }, shape = RoundedCornerShape(14.dp)) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) { Text(product.name.take(1).uppercase(), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) { Text(product.name, fontWeight = FontWeight.SemiBold); Text(product.sku, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant); Text(rupiah(price), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) }
            if (qty > 0) Text("$qty", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(8.dp))
            Button(onClick = onAdd, shape = RoundedCornerShape(10.dp), contentPadding = ButtonDefaults.ContentPadding) { Text("Tambah") }
        }
    }
}

@Composable
private fun CartPanel(cart: List<CartLine>, total: Long, modifier: Modifier, onPlus: (CartLine) -> Unit, onMinus: (CartLine) -> Unit, onPay: () -> Unit, saving: Boolean) {
    Card(modifier, shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth()) { Text("Keranjang", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold); Spacer(Modifier.weight(1f)); Text("${cart.sumOf { it.qty }} item", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            HorizontalDivider()
            if (cart.isEmpty()) {
                Column(Modifier.fillMaxWidth().padding(vertical = 30.dp), horizontalAlignment = Alignment.CenterHorizontally) { Text("Keranjang kosong", fontWeight = FontWeight.SemiBold); Text("Pilih produk untuk mulai transaksi", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            } else {
                LazyColumn(Modifier.height(180.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(cart, key = { it.product.id }) { line ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) { Text(line.product.name, fontWeight = FontWeight.SemiBold); Text("${line.qty} × ${rupiah(line.price)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                            TextButton(onClick = { onMinus(line) }) { Text("−") }
                            Text("${line.qty}")
                            TextButton(onClick = { onPlus(line) }) { Text("+") }
                            Text(rupiah(line.price * line.qty), fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                HorizontalDivider()
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text("Total", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold); Spacer(Modifier.weight(1f)); Text(rupiah(total), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
                Button(onClick = onPay, enabled = !saving, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(12.dp)) { Text(if (saving) "Menyimpan..." else "Bayar ${rupiah(total)}") }
                Text("Mode demo: pembayaran dicatat PAID untuk pengujian. Bukan konfirmasi QRIS nyata.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private suspend fun saveSale(business: BusinessRow?, branch: BranchRow?, location: LocationRow?, cart: List<CartLine>, customer: CustomerRow?, methods: List<PaymentMethodRow>, units: List<UnitRow>, prices: List<ProductPriceRow>): String {
    if (business == null || branch == null || location == null) return "Data toko belum siap"
    if (cart.isEmpty()) return "Keranjang kosong"
    val method = methods.firstOrNull { it.code == "QRIS" } ?: methods.firstOrNull() ?: return "Metode pembayaran belum tersedia"
    val unitByProduct = units.associateBy { it.id }
    val saleId = UUID.randomUUID().toString()
    val saleNo = "TRX-${System.currentTimeMillis().toString().takeLast(8)}"
    val subtotal = cart.sumOf { it.price * it.qty }
    val hpp = cart.sumOf { it.product.current_cost * it.qty }
    val total = subtotal
    try {
        supabase.from("sales").insert(SaleInsert(saleId, business.id, branch.id, location.id, customer?.id, saleNo, subtotal = subtotal, total_amount = total, paid_amount = total, hpp_amount = hpp, margin_amount = total - hpp))
        val items = cart.map { line ->
            val unit = unitByProduct[line.product.base_unit_id]
            SaleItemInsert(UUID.randomUUID().toString(), saleId, line.product.id, line.product.base_unit_id, line.product.sku, line.product.name, line.qty.toDouble(), unit_price = line.price, line_total = line.price * line.qty, hpp_unit = line.product.current_cost, hpp_total = line.product.current_cost * line.qty)
        }
        supabase.from("sale_items").insert(items)
        supabase.from("payments").insert(PaymentInsert(UUID.randomUUID().toString(), business.id, branch.id, saleId, method.id, payment_no = "PAY-${System.currentTimeMillis().toString().takeLast(8)}", amount = total, external_transaction_id = "DEMO-${UUID.randomUUID().toString().take(8)}", idempotency_key = "demo-${UUID.randomUUID()}"))
        cart.forEach { line ->
            val current = supabase.from("stock_balances").select { filter { eq("location_id", location.id); eq("product_id", line.product.id) } }.decodeList<StockRow>().firstOrNull()
            val next = (current?.qty_base ?: 0.0) - line.qty
            if (current != null) supabase.from("stock_balances").update(mapOf("qty_base" to next)) { filter { eq("location_id", location.id); eq("product_id", line.product.id) } }
        }
        return "Tersimpan: $saleNo • ${rupiah(total)}"
    } catch (e: Exception) { return "Gagal menyimpan: ${e.message ?: "error database"}" }
}

@Composable
private fun ProductsScreen() {
    var products by remember { mutableStateOf<List<ProductRow>>(emptyList()) }
    var business by remember { mutableStateOf<BusinessRow?>(null) }
    var name by remember { mutableStateOf("") }
    var sku by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var showForm by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) { try { business = supabase.from("businesses").select { filter { eq("code", DEMO_CODE) } }.decodeList<BusinessRow>().firstOrNull(); if (business != null) products = supabase.from("products").select { filter { eq("business_id", business!!.id); eq("is_active", true) } }.decodeList() } catch (e: Exception) { message = e.message } }
    Page {
        Header("Produk", "Katalog yang dipakai langsung di kasir") { Button(onClick = { showForm = !showForm }) { Text(if (showForm) "Tutup" else "+ Produk") } }
        message?.let { Notice(it) }
        if (showForm) Card(shape = RoundedCornerShape(16.dp)) { Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text("Nama produk") }, singleLine = true)
            OutlinedTextField(sku, { sku = it }, Modifier.fillMaxWidth(), label = { Text("SKU") }, singleLine = true)
            OutlinedTextField(price, { price = it }, Modifier.fillMaxWidth(), label = { Text("Harga jual") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
            Button(onClick = { scope.launch { val b = business; val p = price.toLongOrNull(); if (b == null || name.isBlank() || sku.isBlank() || p == null) { message = "Lengkapi nama, SKU, dan harga" } else { try { val unit = supabase.from("units").select { filter { eq("business_id", b.id); eq("code", "PCS") } }.decodeList<UnitRow>().first(); val id = UUID.randomUUID().toString(); supabase.from("products").insert(mapOf("id" to id, "business_id" to b.id, "sku" to sku, "name" to name, "short_name" to name, "base_unit_id" to unit.id, "current_cost" to p, "last_purchase_cost" to p, "is_active" to true)); val pl = supabase.from("price_lists").select { filter { eq("business_id", b.id); eq("is_default", true) } }.decodeList<PriceListRow>().first(); supabase.from("product_prices").insert(mapOf("id" to UUID.randomUUID().toString(), "price_list_id" to pl.id, "product_id" to id, "unit_id" to unit.id, "min_qty" to 1, "price" to p)); products = supabase.from("products").select { filter { eq("business_id", b.id); eq("is_active", true) } }.decodeList(); name = ""; sku = ""; price = ""; showForm = false; message = "Produk tersimpan" } catch (e: Exception) { message = "Gagal: ${e.message}" } } } } ) { Text("Simpan produk") }
        } }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) { items(products, key = { it.id }) { p -> Card(shape = RoundedCornerShape(14.dp)) { Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(p.name, fontWeight = FontWeight.SemiBold); Text(p.sku, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }; TextButton(onClick = { scope.launch { try { supabase.from("products").update(mapOf("is_active" to false)) { filter { eq("id", p.id) } }; products = products.filterNot { it.id == p.id } } catch (e: Exception) { message = e.message } } }) { Text("Nonaktifkan") } } } } }
    }
}

@Composable
private fun InventoryScreen() {
    var products by remember { mutableStateOf<List<ProductRow>>(emptyList()) }
    var stocks by remember { mutableStateOf<Map<String, Double>>(emptyMap()) }
    var location by remember { mutableStateOf<LocationRow?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) { try { val b = supabase.from("businesses").select { filter { eq("code", DEMO_CODE) } }.decodeList<BusinessRow>().first(); val br = supabase.from("branches").select { filter { eq("business_id", b.id); eq("code", "MAIN") } }.decodeList<BranchRow>().first(); location = supabase.from("locations").select { filter { eq("branch_id", br.id); eq("code", "STORE") } }.decodeList<LocationRow>().first(); products = supabase.from("products").select { filter { eq("business_id", b.id); eq("is_active", true) } }.decodeList(); stocks = supabase.from("stock_balances").select { filter { eq("location_id", location!!.id) } }.decodeList<StockRow>().associate { it.product_id to it.qty_base } } catch (e: Exception) { message = e.message } }
    Page { Header("Stok", "Pantau persediaan dari database") ; message?.let { Notice(it) }; LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) { items(products, key = { it.id }) { p -> val stock = stocks[p.id] ?: 0.0; Card(shape = RoundedCornerShape(14.dp)) { Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(p.name, fontWeight = FontWeight.SemiBold); Text("Stok ${stock.toInt()} pcs", color = if (stock <= 5) androidx.compose.ui.graphics.Color(0xFFD92D20) else MaterialTheme.colorScheme.onSurfaceVariant) }; Text("${stock.toInt()}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) } } } } }
}

@Composable
private fun CustomersScreen() {
    var customers by remember { mutableStateOf<List<CustomerRow>>(emptyList()) }
    var business by remember { mutableStateOf<BusinessRow?>(null) }
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) { try { business = supabase.from("businesses").select { filter { eq("code", DEMO_CODE) } }.decodeList<BusinessRow>().firstOrNull(); if (business != null) customers = supabase.from("customers").select { filter { eq("business_id", business!!.id); eq("is_active", true) } }.decodeList() } catch (e: Exception) { message = e.message } }
    Page { Header("Pelanggan", "Data pelanggan untuk transaksi dan riwayat") ; message?.let { Notice(it) }; Card(shape = RoundedCornerShape(16.dp)) { Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text("Nama pelanggan") }, singleLine = true); OutlinedTextField(phone, { phone = it }, Modifier.fillMaxWidth(), label = { Text("Nomor telepon") }, singleLine = true); Button(onClick = { scope.launch { val b = business; if (b == null || name.isBlank()) { message = "Nama wajib diisi" } else { try { supabase.from("customers").insert(mapOf("id" to UUID.randomUUID().toString(), "business_id" to b.id, "code" to "CUS-${System.currentTimeMillis().toString().takeLast(6)}", "name" to name, "customer_type" to "RETAIL", "phone" to phone.ifBlank { null }, "is_active" to true)); customers = supabase.from("customers").select { filter { eq("business_id", b.id); eq("is_active", true) } }.decodeList(); name = ""; phone = ""; message = "Pelanggan tersimpan" } catch (e: Exception) { message = "Gagal: ${e.message}" } } } }) { Text("Tambah pelanggan") } } }; LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) { items(customers, key = { it.id }) { c -> Card(shape = RoundedCornerShape(14.dp)) { Row(Modifier.fillMaxWidth().padding(14.dp)) { Column(Modifier.weight(1f)) { Text(c.name, fontWeight = FontWeight.SemiBold); Text(c.phone ?: "Tanpa nomor", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } } } } } }
}

@Composable
private fun PaymentsScreen() {
    var payments by remember { mutableStateOf<List<Map<String, Any?>>>(emptyList()) }
    var message by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) { try { val b = supabase.from("businesses").select { filter { eq("code", DEMO_CODE) } }.decodeList<BusinessRow>().first(); val rows = supabase.from("payments").select { filter { eq("business_id", b.id) } }.decodeList<Map<String, Any?>>(); payments = rows } catch (e: Exception) { message = e.message } }
    Page { Header("Pembayaran", "Riwayat pembayaran yang tercatat") ; message?.let { Notice(it) }; if (payments.isEmpty()) EmptyState("Belum ada pembayaran"); LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) { items(payments) { p -> Card(shape = RoundedCornerShape(14.dp)) { Column(Modifier.padding(14.dp)) { Text((p["payment_no"] ?: "Pembayaran").toString(), fontWeight = FontWeight.SemiBold); Text("${p["status"] ?: "-"} • ${rupiah((p["amount"] as? Number)?.toLong() ?: 0)}", color = MaterialTheme.colorScheme.primary) } } } } }
}

@Composable
private fun ReportsScreen() {
    Page { Header("Laporan", "Ringkasan transaksi untuk pengecekan cepat"); Card(shape = RoundedCornerShape(16.dp)) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { Text("Hari ini", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold); Text(rupiah(1_250_000), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); Text("24 transaksi • QRIS ${rupiah(875_000)}", color = MaterialTheme.colorScheme.onSurfaceVariant) } }; Text("Transaksi terbaru", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold); listOf("TRX-1024" to "Rp 43.000", "TRX-1023" to "Rp 25.000", "TRX-1022" to "Rp 15.000").forEach { (no, amount) -> Card(shape = RoundedCornerShape(12.dp)) { Row(Modifier.fillMaxWidth().padding(14.dp)) { Text(no, Modifier.weight(1f), fontWeight = FontWeight.SemiBold); Text(amount) } } } }
}

@Composable
private fun SettingsScreen() {
    var storeOpen by remember { mutableStateOf(true) }
    var autoPrint by remember { mutableStateOf(true) }
    Page { Header("Pengaturan", "Preferensi perangkat kasir dan toko"); SettingRow("Status toko", if (storeOpen) "Buka" else "Tutup") { storeOpen = !storeOpen }; SettingRow("Cetak otomatis", if (autoPrint) "Aktif" else "Nonaktif") { autoPrint = !autoPrint }; Card(shape = RoundedCornerShape(16.dp)) { Column(Modifier.padding(16.dp)) { Text("Mode pengujian", fontWeight = FontWeight.Bold); Text("Aplikasi saat ini menggunakan Toko Demo dan publishable key Supabase. Tidak ada service-role key di aplikasi.", color = MaterialTheme.colorScheme.onSurfaceVariant) } } }
}

@Composable
private fun SettingRow(title: String, value: String, onClick: () -> Unit) { Card(Modifier.fillMaxWidth().clickable { onClick() }, shape = RoundedCornerShape(14.dp)) { Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Text(title, Modifier.weight(1f), fontWeight = FontWeight.SemiBold); Text(value, color = MaterialTheme.colorScheme.primary) } } }

@Composable
private fun Notice(text: String) { Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.primaryContainer) { Text(text, Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onPrimaryContainer) } }

@Composable
private fun EmptyState(text: String) { Column(Modifier.fillMaxWidth().padding(30.dp), horizontalAlignment = Alignment.CenterHorizontally) { Text(text, fontWeight = FontWeight.SemiBold); Text("Data akan muncul setelah ada input", color = MaterialTheme.colorScheme.onSurfaceVariant) } }
