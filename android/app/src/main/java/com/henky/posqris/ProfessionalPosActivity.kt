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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import java.util.UUID

private val posSupabase = createSupabaseClient(BuildConfig.SUPABASE_URL, BuildConfig.SUPABASE_PUBLISHABLE_KEY) {
    install(Postgrest)
}

private val Teal = Color(0xFF0B8F8A)
private val TealDark = Color(0xFF086E6A)
private val Ink = Color(0xFF182230)
private val Muted = Color(0xFF667085)
private val Canvas = Color(0xFFF7F9FA)
private val Border = Color(0xFFE4E7EC)
private val Success = Color(0xFF12B76A)
private val Danger = Color(0xFFD92D20)

@Serializable private data class UiBusiness(val id: String, val code: String, val name: String)
@Serializable private data class UiBranch(val id: String, val business_id: String, val name: String)
@Serializable private data class UiLocation(val id: String, val branch_id: String, val name: String)
@Serializable private data class UiUnit(val id: String, val business_id: String, val code: String, val name: String, val symbol: String? = null)
@Serializable private data class UiCategory(val id: String, val business_id: String, val code: String, val name: String)
@Serializable private data class UiPriceList(val id: String, val business_id: String, val name: String, val is_default: Boolean = false)
@Serializable private data class UiProduct(val id: String, val business_id: String, val sku: String, val name: String, val short_name: String? = null, val category_id: String? = null, val base_unit_id: String, val current_cost: Long = 0, val is_active: Boolean = true)
@Serializable private data class UiPrice(val id: String, val price_list_id: String, val product_id: String, val unit_id: String, val price: Long)
@Serializable private data class UiStock(val location_id: String, val product_id: String, val qty_base: Double = 0.0, val reserved_qty: Double = 0.0)
@Serializable private data class UiPaymentMethod(val id: String, val business_id: String, val code: String, val name: String, val method_type: String, val is_active: Boolean = true)
@Serializable private data class UiSale(val id: String, val business_id: String, val branch_id: String, val location_id: String, val customer_id: String? = null, val sale_no: String, val status: String = "COMPLETED", val subtotal: Long, val discount_amount: Long = 0, val tax_amount: Long = 0, val service_charge: Long = 0, val rounding_amount: Long = 0, val total_amount: Long, val paid_amount: Long, val change_amount: Long = 0, val hpp_amount: Long, val margin_amount: Long, val notes: String? = "POS QRIS")
@Serializable private data class UiSaleItem(val id: String, val sale_id: String, val product_id: String, val unit_id: String, val product_sku_snapshot: String, val product_name_snapshot: String, val qty: Double, val conversion_to_base: Double = 1.0, val unit_price: Long, val discount_amount: Long = 0, val tax_amount: Long = 0, val line_total: Long, val hpp_unit: Long, val hpp_total: Long)
@Serializable private data class UiPayment(val id: String, val business_id: String, val branch_id: String, val sale_id: String, val payment_method_id: String, val qris_configuration_id: String? = null, val payment_no: String, val amount: Long, val currency_code: String = "IDR", val status: String = "PAID", val provider: String? = "DEMO", val external_transaction_id: String? = null, val idempotency_key: String? = null, val qr_reference: String? = null, val reconciliation_status: String = "UNRECONCILED")

private data class UiCart(val product: UiProduct, val price: Long, val qty: Int)

class ProfessionalPosActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ProfessionalPosApp() }
    }
}

@Composable
private fun ProfessionalPosApp() {
    val width = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp
    val tablet = width >= 600
    var screen by remember { mutableStateOf("home") }
    var moreOpen by remember { mutableStateOf(false) }

    MaterialTheme(colorScheme = androidx.compose.material3.lightColorScheme(primary = Teal, onPrimary = Color.White, primaryContainer = Color(0xFFDDF4F2), onPrimaryContainer = TealDark, background = Canvas, surface = Color.White, surfaceVariant = Color(0xFFF2F4F7), outline = Border)) {
        Row(Modifier.fillMaxSize().background(Canvas)) {
            if (tablet) TabletSidebar(screen) { screen = it; moreOpen = false }
            Box(Modifier.weight(1f).fillMaxHeight()) {
                when (screen) {
                    "home" -> HomeScreen(tablet) { screen = "sales" }
                    "sales" -> SalesScreen(tablet)
                    "products" -> ProductsScreen(tablet)
                    "inventory" -> InventoryScreen(tablet)
                    "more" -> MoreScreen { screen = it; moreOpen = false }
                    else -> MoreScreen { screen = it; moreOpen = false }
                }
                if (!tablet) PhoneNav(screen) { screen = it; moreOpen = it == "more" }
            }
        }
    }
}

@Composable
private fun TabletSidebar(selected: String, onSelect: (String) -> Unit) {
    Surface(Modifier.width(210.dp).fillMaxHeight(), color = TealDark) {
        Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.padding(bottom = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)).background(Color.White), contentAlignment = Alignment.Center) { Text("P", color = TealDark, fontWeight = FontWeight.Black, fontSize = 20.sp) }
                Spacer(Modifier.width(10.dp))
                Column { Text("POS QRIS", color = Color.White, fontWeight = FontWeight.Bold); Text("Toko Demo", color = Color.White.copy(.7f), fontSize = 12.sp) }
            }
            SideItem("⌂", "Dashboard", "home", selected, onSelect)
            SideItem("＋", "Transaksi", "sales", selected, onSelect)
            SideItem("□", "Produk", "products", selected, onSelect)
            SideItem("▥", "Persediaan", "inventory", selected, onSelect)
            SideItem("⋯", "Lainnya", "more", selected, onSelect)
            Spacer(Modifier.weight(1f))
            Text("ONLINE", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text("Supabase • Demo", color = Color.White.copy(.65f), fontSize = 11.sp)
        }
    }
}

@Composable
private fun SideItem(icon: String, label: String, route: String, selected: String, onSelect: (String) -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(if (selected == route) Teal else Color.Transparent).clickable { onSelect(route) }.padding(horizontal = 12.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(icon, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.width(28.dp))
        Text(label, color = Color.White, fontWeight = if (selected == route) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
private fun PhoneNav(selected: String, onSelect: (String) -> Unit) {
    Surface(Modifier.fillMaxWidth().navigationBarsPadding(), shadowElevation = 10.dp) {
        Row(Modifier.fillMaxWidth().padding(7.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            PhoneNavItem("⌂", "Beranda", "home", selected, onSelect)
            PhoneNavItem("＋", "Transaksi", "sales", selected, onSelect)
            PhoneNavItem("□", "Produk", "products", selected, onSelect)
            PhoneNavItem("⋯", "Lainnya", "more", selected, onSelect)
        }
    }
}

@Composable
private fun RowScope.PhoneNavItem(icon: String, label: String, route: String, selected: String, onSelect: (String) -> Unit) {
    Column(Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(if (selected == route) Color(0xFFE4F5F3) else Color.White).clickable { onSelect(route) }.padding(vertical = 7.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(icon, color = if (selected == route) TealDark else Muted, fontWeight = FontWeight.Bold)
        Text(label, color = if (selected == route) TealDark else Muted, fontSize = 11.sp, fontWeight = if (selected == route) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
private fun Page(title: String, subtitle: String? = null, tablet: Boolean, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().padding(start = if (tablet) 28.dp else 16.dp, top = 20.dp, end = if (tablet) 28.dp else 16.dp, bottom = if (tablet) 20.dp else 78.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) { Text(title, color = Ink, fontSize = if (tablet) 26.sp else 22.sp, fontWeight = FontWeight.Bold); subtitle?.let { Text(it, color = Muted, fontSize = 13.sp) } }
            StoreStatus()
        }
        content()
    }
}

@Composable
private fun StoreStatus() {
    Row(verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(8.dp).clip(RoundedCornerShape(50)).background(Success)); Spacer(Modifier.width(6.dp)); Text("Toko Demo • Online", color = Muted, fontSize = 12.sp) }
}

@Composable
private fun HomeScreen(tablet: Boolean, onNewSale: () -> Unit) {
    Page("Dashboard", "Ringkasan operasional toko hari ini", tablet) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Metric("Total Penjualan", "Rp 1.250.000", "+12,5%", true, Modifier.weight(1.25f))
            Metric("Total Transaksi", "24", "+9,2%", false, Modifier.weight(1f))
            Metric("Pembayaran QRIS", "Rp 875.000", "+15,2%", false, Modifier.weight(1f))
            Metric("Stok Menipis", "3", "Lihat stok", false, Modifier.weight(1f))
        }
        Button(onClick = onNewSale, Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(10.dp)) { Text("＋  Transaksi Baru", fontWeight = FontWeight.Bold) }
        if (tablet) {
            Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                SalesOverview(Modifier.weight(1.55f))
                TopProducts(Modifier.weight(.85f))
            }
        } else {
            SalesOverview(Modifier.fillMaxWidth())
            TopProducts(Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun Metric(title: String, value: String, note: String, primary: Boolean, modifier: Modifier) {
    Card(modifier, shape = RoundedCornerShape(10.dp), colors = CardDefaults.cardColors(if (primary) Teal else Color.White), border = if (primary) null else androidx.compose.foundation.BorderStroke(1.dp, Border)) {
        Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(title, color = if (primary) Color.White.copy(.78f) else Muted, fontSize = 12.sp)
            Text(value, color = if (primary) Color.White else Ink, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(note, color = if (primary) Color.White.copy(.78f) else TealDark, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun SalesOverview(modifier: Modifier) {
    Card(modifier, shape = RoundedCornerShape(10.dp), colors = CardDefaults.cardColors(Color.White), border = androidx.compose.foundation.BorderStroke(1.dp, Border)) {
        Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text("Sales Overview", color = Ink, fontWeight = FontWeight.Bold); Spacer(Modifier.weight(1f)); Text("This Week", color = Muted, fontSize = 12.sp) }
            Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Bottom) {
                listOf(35, 52, 42, 68, 55, 76, 92).forEachIndexed { index, h ->
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                        Box(Modifier.width(22.dp).height((h * 1.35).dp).clip(RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp)).background(if (index == 6) Teal else Color(0xFFBFE8E5)))
                        Text(listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")[index], color = Muted, fontSize = 9.sp, modifier = Modifier.padding(top = 5.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun TopProducts(modifier: Modifier) {
    Card(modifier, shape = RoundedCornerShape(10.dp), colors = CardDefaults.cardColors(Color.White), border = androidx.compose.foundation.BorderStroke(1.dp, Border)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Produk Terlaris", color = Ink, fontWeight = FontWeight.Bold)
            listOf("Kopi Susu" to "Rp 2.450.000", "Nasi Goreng" to "Rp 1.820.000", "Es Teh Manis" to "Rp 1.180.000", "Roti Bakar" to "Rp 950.000").forEachIndexed { i, item ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(30.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFE9F7F5)), contentAlignment = Alignment.Center) { Text("${i + 1}", color = TealDark, fontWeight = FontWeight.Bold, fontSize = 12.sp) }; Spacer(Modifier.width(10.dp)); Text(item.first, Modifier.weight(1f), color = Ink, fontSize = 13.sp); Text(item.second, color = Ink, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
            }
        }
    }
}

@Composable
private fun SalesScreen(tablet: Boolean) {
    var business by remember { mutableStateOf<UiBusiness?>(null) }
    var branch by remember { mutableStateOf<UiBranch?>(null) }
    var location by remember { mutableStateOf<UiLocation?>(null) }
    var products by remember { mutableStateOf<List<UiProduct>>(emptyList()) }
    var prices by remember { mutableStateOf<List<UiPrice>>(emptyList()) }
    var categories by remember { mutableStateOf<List<UiCategory>>(emptyList()) }
    var units by remember { mutableStateOf<List<UiUnit>>(emptyList()) }
    var methods by remember { mutableStateOf<List<UiPaymentMethod>>(emptyList()) }
    var stock by remember { mutableStateOf<Map<String, Double>>(emptyMap()) }
    var cart by remember { mutableStateOf<List<UiCart>>(emptyList()) }
    var query by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Semua") }
    var message by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        try {
            val b = posSupabase.from("businesses").select { filter { eq("code", "DEMO") } }.decodeList<UiBusiness>().firstOrNull()
            business = b
            if (b != null) {
                branch = posSupabase.from("branches").select { filter { eq("business_id", b.id); eq("code", "MAIN") } }.decodeList<UiBranch>().firstOrNull()
                units = posSupabase.from("units").select { filter { eq("business_id", b.id) } }.decodeList()
                categories = posSupabase.from("categories").select { filter { eq("business_id", b.id) } }.decodeList()
                methods = posSupabase.from("payment_methods").select { filter { eq("business_id", b.id); eq("is_active", true) } }.decodeList()
                products = posSupabase.from("products").select { filter { eq("business_id", b.id); eq("is_active", true) } }.decodeList()
                val pl = posSupabase.from("price_lists").select { filter { eq("business_id", b.id); eq("is_default", true) } }.decodeList<UiPriceList>().firstOrNull()
                if (pl != null) prices = posSupabase.from("product_prices").select { filter { eq("price_list_id", pl.id) } }.decodeList()
                val br = branch
                if (br != null) location = posSupabase.from("locations").select { filter { eq("branch_id", br.id); eq("code", "STORE") } }.decodeList<UiLocation>().firstOrNull()
                val loc = location
                if (loc != null) stock = posSupabase.from("stock_balances").select { filter { eq("location_id", loc.id) } }.decodeList<UiStock>().associate { it.product_id to it.qty_base }
            }
        } catch (e: Exception) { message = "Gagal memuat data: ${e.message ?: "koneksi"}" }
    }

    val filtered = products.filter { p -> (selectedCategory == "Semua" || categories.firstOrNull { it.id == p.category_id }?.name == selectedCategory) && (query.isBlank() || p.name.contains(query, true) || p.sku.contains(query, true)) }
    val total = cart.sumOf { it.price * it.qty }
    Page("Transaksi", "Kasir cepat untuk penjualan harian", tablet) {
        message?.let { Notice(it) }
        if (tablet) {
            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Catalog(filtered, categories, selectedCategory, query, cart, prices, stock, Modifier.weight(1.55f), { query = it }, { selectedCategory = it }) { p -> cart = addToCart(cart, p, prices) }
                CartPanel(cart, total, Modifier.weight(.8f), saving) {
                    if (!saving) { saving = true; scope.launch { message = persistSale(business, branch, location, cart, units, methods, total); if (message?.startsWith("Tersimpan") == true) { cart = emptyList(); val loc = location; if (loc != null) stock = posSupabase.from("stock_balances").select { filter { eq("location_id", loc.id) } }.decodeList<UiStock>().associate { it.product_id to it.qty_base } }; saving = false } }
                }
            }
        } else {
            Catalog(filtered, categories, selectedCategory, query, cart, prices, stock, Modifier.fillMaxWidth().weight(1f), { query = it }, { selectedCategory = it }) { p -> cart = addToCart(cart, p, prices) }
            if (cart.isNotEmpty()) CartPanel(cart, total, Modifier.fillMaxWidth(), saving) {
                if (!saving) { saving = true; scope.launch { message = persistSale(business, branch, location, cart, units, methods, total); if (message?.startsWith("Tersimpan") == true) cart = emptyList(); saving = false } }
            }
        }
    }
}

private fun addToCart(cart: List<UiCart>, product: UiProduct, prices: List<UiPrice>): List<UiCart> {
    val price = prices.firstOrNull { it.product_id == product.id }?.price ?: product.current_cost
    return if (cart.any { it.product.id == product.id }) cart.map { if (it.product.id == product.id) it.copy(qty = it.qty + 1) else it } else cart + UiCart(product, price, 1)
}

@Composable
private fun Catalog(products: List<UiProduct>, categories: List<UiCategory>, selectedCategory: String, query: String, cart: List<UiCart>, prices: List<UiPrice>, stock: Map<String, Double>, modifier: Modifier, onQuery: (String) -> Unit, onCategory: (String) -> Unit, onAdd: (UiProduct) -> Unit) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(query, onQuery, Modifier.fillMaxWidth(), singleLine = true, label = { Text("Cari produk atau SKU") })
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            Chip("Semua", selectedCategory == "Semua") { onCategory("Semua") }
            categories.take(4).forEach { Chip(it.name, selectedCategory == it.name) { onCategory(it.name) } }
        }
        if (products.isEmpty()) Empty("Tidak ada produk yang cocok") else LazyVerticalGrid(GridCells.Adaptive(if (androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp >= 600) 150.dp else 145.dp), modifier = Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(products, key = { it.id }) { p -> ProductCard(p, prices.firstOrNull { it.product_id == p.id }?.price ?: p.current_cost, cart.firstOrNull { it.product.id == p.id }?.qty ?: 0, stock[p.id] ?: 0.0, onAdd) }
        }
    }
}

@Composable
private fun Chip(text: String, selected: Boolean, onClick: () -> Unit) {
    Surface(Modifier.clip(RoundedCornerShape(8.dp)).clickable { onClick() }, color = if (selected) Teal else Color.White, border = if (selected) null else androidx.compose.foundation.BorderStroke(1.dp, Border)) { Text(text, Modifier.padding(horizontal = 12.dp, vertical = 8.dp), color = if (selected) Color.White else Ink, fontSize = 12.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) }
}

@Composable
private fun ProductCard(product: UiProduct, price: Long, qty: Int, stock: Double, onAdd: (UiProduct) -> Unit) {
    Card(Modifier.fillMaxWidth().clickable { onAdd(product) }, shape = RoundedCornerShape(10.dp), colors = CardDefaults.cardColors(Color.White), border = androidx.compose.foundation.BorderStroke(1.dp, Border)) {
        Column(Modifier.padding(11.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Box(Modifier.fillMaxWidth().height(74.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFEAF7F5)), contentAlignment = Alignment.Center) { Text(product.name.take(1).uppercase(), color = TealDark, fontSize = 26.sp, fontWeight = FontWeight.Black) }
            Text(product.name, color = Ink, fontWeight = FontWeight.Bold, maxLines = 2, fontSize = 13.sp)
            Text(product.sku, color = Muted, fontSize = 10.sp)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text(rupiah(price), Modifier.weight(1f), color = TealDark, fontWeight = FontWeight.Bold, fontSize = 13.sp); Text("${stock.toInt()} stok", color = if (stock <= 5) Danger else Muted, fontSize = 10.sp) }
            Button(onClick = { onAdd(product) }, Modifier.fillMaxWidth().height(36.dp), contentPadding = ButtonDefaults.ContentPadding, shape = RoundedCornerShape(7.dp)) { Text(if (qty == 0) "Tambah" else "Tambah  •  $qty", fontSize = 12.sp) }
        }
    }
}

@Composable
private fun CartPanel(cart: List<UiCart>, total: Long, modifier: Modifier, saving: Boolean, onPay: () -> Unit) {
    Card(modifier, shape = RoundedCornerShape(10.dp), colors = CardDefaults.cardColors(Color.White), border = androidx.compose.foundation.BorderStroke(1.dp, Border)) {
        Column(Modifier.fillMaxWidth().padding(15.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text("Keranjang", color = Ink, fontWeight = FontWeight.Bold, fontSize = 17.sp); Spacer(Modifier.weight(1f)); Text("${cart.sumOf { it.qty }} item", color = Muted, fontSize = 12.sp) }
            HorizontalDivider(color = Border)
            if (cart.isEmpty()) Empty("Belum ada produk") else {
                LazyColumn(Modifier.weight(1f, fill = false).heightIn(max = 300.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    items(cart, key = { it.product.id }) { line ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) { Text(line.product.name, color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 13.sp); Text("${line.qty} × ${rupiah(line.price)}", color = Muted, fontSize = 11.sp) }
                            Text(rupiah(line.price * line.qty), color = Ink, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                HorizontalDivider(color = Border)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text("Total", color = Ink, fontWeight = FontWeight.Bold); Spacer(Modifier.weight(1f)); Text(rupiah(total), color = TealDark, fontSize = 20.sp, fontWeight = FontWeight.Black) }
                Button(onClick = onPay, enabled = !saving, Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(8.dp)) { Text(if (saving) "Menyimpan..." else "BAYAR  ${rupiah(total)}", fontWeight = FontWeight.Bold) }
                Text("Mode demo: pembayaran dicatat untuk pengujian. Scan QRIS belum menjadi bukti pembayaran nyata.", color = Muted, fontSize = 9.sp)
            }
        }
    }
}

private suspend fun persistSale(business: UiBusiness?, branch: UiBranch?, location: UiLocation?, cart: List<UiCart>, units: List<UiUnit>, methods: List<UiPaymentMethod>, total: Long): String {
    if (business == null || branch == null || location == null) return "Data toko belum siap"
    if (cart.isEmpty()) return "Keranjang kosong"
    val method = methods.firstOrNull { it.code == "QRIS" } ?: methods.firstOrNull() ?: return "Metode pembayaran belum tersedia"
    val saleId = UUID.randomUUID().toString()
    val saleNo = "TRX-${System.currentTimeMillis().toString().takeLast(8)}"
    val hpp = cart.sumOf { it.product.current_cost * it.qty }
    return try {
        posSupabase.from("sales").insert(UiSale(saleId, business.id, branch.id, location.id, saleNo = saleNo, subtotal = total, total_amount = total, paid_amount = total, hpp_amount = hpp, margin_amount = total - hpp))
        posSupabase.from("sale_items").insert(cart.map { line -> UiSaleItem(UUID.randomUUID().toString(), saleId, line.product.id, line.product.base_unit_id, line.product.sku, line.product.name, line.qty.toDouble(), unit_price = line.price, line_total = line.price * line.qty, hpp_unit = line.product.current_cost, hpp_total = line.product.current_cost * line.qty) })
        posSupabase.from("payments").insert(UiPayment(UUID.randomUUID().toString(), business.id, branch.id, saleId, method.id, payment_no = "PAY-${System.currentTimeMillis().toString().takeLast(8)}", amount = total, external_transaction_id = "DEMO-${UUID.randomUUID().toString().take(8)}", idempotency_key = "demo-${UUID.randomUUID()}"))
        cart.forEach { line ->
            val current = posSupabase.from("stock_balances").select { filter { eq("location_id", location.id); eq("product_id", line.product.id) } }.decodeList<UiStock>().firstOrNull()
            if (current != null) posSupabase.from("stock_balances").update(mapOf("qty_base" to (current.qty_base - line.qty).coerceAtLeast(0.0))) { filter { eq("location_id", location.id); eq("product_id", line.product.id) } }
        }
        "Tersimpan: $saleNo • ${rupiah(total)}"
    } catch (e: Exception) { "Gagal menyimpan: ${e.message ?: "database"}" }
}

@Composable
private fun ProductsScreen(tablet: Boolean) {
    var products by remember { mutableStateOf<List<UiProduct>>(emptyList()) }
    var message by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) { try { val b = posSupabase.from("businesses").select { filter { eq("code", "DEMO") } }.decodeList<UiBusiness>().first(); products = posSupabase.from("products").select { filter { eq("business_id", b.id); eq("is_active", true) } }.decodeList() } catch (e: Exception) { message = e.message } }
    Page("Produk", "Katalog yang langsung dipakai kasir", tablet) {
        message?.let { Notice(it) }
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) { items(products, key = { it.id }) { p -> Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(9.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Border)) { Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(42.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFEAF7F5)), contentAlignment = Alignment.Center) { Text(p.name.take(1).uppercase(), color = TealDark, fontWeight = FontWeight.Bold) }; Spacer(Modifier.width(10.dp)); Column(Modifier.weight(1f)) { Text(p.name, color = Ink, fontWeight = FontWeight.Bold); Text(p.sku, color = Muted, fontSize = 11.sp) }; Text("Aktif", color = Success, fontSize = 11.sp, fontWeight = FontWeight.Bold) } } } }
    }
}

@Composable
private fun InventoryScreen(tablet: Boolean) {
    var rows by remember { mutableStateOf<List<Pair<UiProduct, Double>>>(emptyList()) }
    var message by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) { try { val b = posSupabase.from("businesses").select { filter { eq("code", "DEMO") } }.decodeList<UiBusiness>().first(); val br = posSupabase.from("branches").select { filter { eq("business_id", b.id); eq("code", "MAIN") } }.decodeList<UiBranch>().first(); val loc = posSupabase.from("locations").select { filter { eq("branch_id", br.id); eq("code", "STORE") } }.decodeList<UiLocation>().first(); val products = posSupabase.from("products").select { filter { eq("business_id", b.id); eq("is_active", true) } }.decodeList<UiProduct>(); val stocks = posSupabase.from("stock_balances").select { filter { eq("location_id", loc.id) } }.decodeList<UiStock>().associate { it.product_id to it.qty_base }; rows = products.map { it to (stocks[it.id] ?: 0.0) } } catch (e: Exception) { message = e.message } }
    Page("Persediaan", "Pantau stok dan barang yang perlu diisi", tablet) {
        message?.let { Notice(it) }
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) { items(rows, key = { it.first.id }) { (p, qty) -> Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(9.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Border)) { Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(p.name, color = Ink, fontWeight = FontWeight.Bold); Text(p.sku, color = Muted, fontSize = 11.sp) }; Text("${qty.toInt()} pcs", color = if (qty <= 5) Danger else Ink, fontWeight = FontWeight.Bold); Spacer(Modifier.width(10.dp)); Text(if (qty <= 5) "MENIPIS" else "Aman", color = if (qty <= 5) Danger else Success, fontSize = 10.sp, fontWeight = FontWeight.Bold) } } } }
    }
}

@Composable
private fun MoreScreen(onSelect: (String) -> Unit) {
    Page("Lainnya", "Fitur operasional POS", true) {
        listOf("Pelanggan" to "Kelola data pelanggan", "Pembayaran" to "Riwayat pembayaran", "Laporan" to "Ringkasan penjualan", "Pengaturan" to "Konfigurasi toko dan kasir").forEach { item -> Card(Modifier.fillMaxWidth().clickable { }, shape = RoundedCornerShape(9.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Border)) { Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(item.first, color = Ink, fontWeight = FontWeight.Bold); Text(item.second, color = Muted, fontSize = 12.sp) }; Text("›", color = Teal, fontSize = 24.sp) } } }
        Text("Fitur lainnya tersedia setelah modul inti stabil.", color = Muted, fontSize = 11.sp)
    }
}

@Composable private fun Notice(text: String) { Surface(Modifier.fillMaxWidth(), color = Color(0xFFE4F5F3), shape = RoundedCornerShape(8.dp)) { Text(text, Modifier.padding(11.dp), color = TealDark, fontSize = 12.sp) } }
@Composable private fun Empty(text: String) { Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) { Text(text, color = Ink, fontWeight = FontWeight.SemiBold); Text("Belum ada data untuk ditampilkan", color = Muted, fontSize = 11.sp) } }
private fun rupiah(value: Long): String = "Rp " + value.toString().reversed().chunked(3).joinToString(".").reversed()
