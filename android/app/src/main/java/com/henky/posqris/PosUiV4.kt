package com.henky.posqris

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.NumberFormat
import java.util.Locale

private val UiBg = Color(0xFFF6F7FB)
private val UiSurface = Color.White
private val UiInk = Color(0xFF182033)
private val UiMuted = Color(0xFF6D778A)
private val UiLine = Color(0xFFE7EAF0)
private val UiBrand = Color(0xFF5B3CC4)
private val UiBrandSoft = Color(0xFFF0EAFF)
private val UiGreen = Color(0xFF148A63)
private val UiGreenSoft = Color(0xFFE8F7F1)
private val UiAmber = Color(0xFFB86B00)
private val UiAmberSoft = Color(0xFFFFF3D8)
private val UiRed = Color(0xFFC53B4B)
private val UiRedSoft = Color(0xFFFFE9EC)

private data class UiProduct(val sku: String, val name: String, val category: String, val price: Long, val stock: Int)
private data class UiSale(val no: String, val customer: String, val amount: Long, val method: String, val time: String, val status: String)
private data class UiCart(val product: UiProduct, val qty: Int)

private val uiProducts = listOf(
    UiProduct("SKU-001", "Kopi Susu Gula Aren", "Minuman", 18000, 42),
    UiProduct("SKU-002", "Americano", "Minuman", 15000, 31),
    UiProduct("SKU-003", "Matcha Latte", "Minuman", 22000, 18),
    UiProduct("SKU-004", "Roti Cokelat", "Makanan", 12000, 27),
    UiProduct("SKU-005", "Croissant Butter", "Makanan", 16000, 9),
    UiProduct("SKU-006", "Nasi Ayam Sambal", "Makanan", 28000, 14),
    UiProduct("SKU-007", "Air Mineral", "Minuman", 6000, 63),
    UiProduct("SKU-008", "Kentang Goreng", "Makanan", 19000, 7),
    UiProduct("SKU-009", "Teh Lemon", "Minuman", 14000, 22),
    UiProduct("SKU-010", "Donat Gula", "Makanan", 9000, 5)
)

private val uiSales = listOf(
    UiSale("TRX-260901-024", "Pelanggan Umum", 68000, "QRIS", "11:08", "LUNAS"),
    UiSale("TRX-260901-023", "Budi", 45000, "Tunai", "10:52", "LUNAS"),
    UiSale("TRX-260901-022", "Pelanggan Umum", 32000, "QRIS", "10:41", "LUNAS"),
    UiSale("TRX-260901-021", "Sari", 92000, "QRIS", "10:25", "LUNAS"),
    UiSale("TRX-260901-020", "Dewi", 56000, "Debit", "09:58", "LUNAS")
)

private fun money(value: Long): String = NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply { maximumFractionDigits = 0 }.format(value).replace("Rp", "Rp ")

private val menuItems = listOf("Dashboard", "Penjualan", "Pesanan", "Produk", "Persediaan", "Pelanggan", "Supplier", "Pembelian", "Piutang", "Pembayaran", "Laporan", "Pengaturan")

@Composable
fun ModernPosAppV4() {
    var page by remember { mutableStateOf("Dashboard") }
    var cart by remember { mutableStateOf(emptyList<UiCart>()) }
    val width = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp
    val tablet = width >= 600
    MaterialTheme(colorScheme = lightColorScheme(primary = UiBrand, background = UiBg, surface = UiSurface, onSurface = UiInk)) {
        Surface(Modifier.fillMaxSize(), color = UiBg) {
            if (tablet) {
                Row(Modifier.fillMaxSize()) {
                    V4Sidebar(page) { page = it }
                    Box(Modifier.weight(1f).fillMaxHeight()) { V4Page(page, cart, { cart = it }) }
                }
            } else {
                Column(Modifier.fillMaxSize()) {
                    Box(Modifier.weight(1f).fillMaxWidth()) { V4Page(page, cart, { cart = it }) }
                    V4BottomBar(page) { page = it }
                }
            }
        }
    }
}

@Composable
private fun V4Sidebar(selected: String, onSelect: (String) -> Unit) {
    Surface(Modifier.width(248.dp).fillMaxHeight(), color = Color(0xFF151A29)) {
        Column(Modifier.fillMaxSize().padding(14.dp)) {
            Row(Modifier.padding(8.dp, 8.dp, 8.dp, 18.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(42.dp).clip(RoundedCornerShape(13.dp)).background(UiBrand), contentAlignment = Alignment.Center) {
                    Text("QR", color = Color.White, fontWeight = FontWeight.ExtraBold)
                }
                Column(Modifier.padding(start = 10.dp)) {
                    Text("POS QRIS", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                    Text("Toko Demo", color = Color(0xFFAAB3C5), fontSize = 11.sp)
                }
            }
            menuItems.forEach { item ->
                val active = item == selected
                Surface(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(11.dp)).clickable { onSelect(item) },
                    shape = RoundedCornerShape(11.dp),
                    color = if (active) UiBrand else Color.Transparent
                ) {
                    Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(v4Glyph(item), color = if (active) Color.White else Color(0xFFABB5C8), fontSize = 16.sp, modifier = Modifier.width(27.dp))
                        Text(item, color = if (active) Color.White else Color(0xFFE7EBF3), fontSize = 13.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Medium)
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            Surface(Modifier.fillMaxWidth(), RoundedCornerShape(15.dp), Color(0xFF202638)) {
                Column(Modifier.padding(13.dp)) {
                    Text("STATUS TOKO", color = Color(0xFF8F9AAF), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Text("●  Online", color = Color(0xFF63D8AE), fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 5.dp))
                    Text("Supabase terhubung", color = Color(0xFFAAB3C5), fontSize = 10.sp)
                }
            }
        }
    }
}

private fun v4Glyph(item: String): String = when (item) {
    "Dashboard" -> "⌂"
    "Penjualan" -> "＋"
    "Pesanan" -> "▤"
    "Produk" -> "□"
    "Persediaan" -> "▥"
    "Pelanggan" -> "♙"
    "Supplier" -> "↔"
    "Pembelian" -> "▣"
    "Piutang" -> "◎"
    "Pembayaran" -> "QR"
    "Laporan" -> "▦"
    else -> "⚙"
}

@Composable
private fun V4BottomBar(selected: String, onSelect: (String) -> Unit) {
    val items = listOf("Dashboard", "Penjualan", "Produk", "Persediaan")
    Surface(Modifier.fillMaxWidth().navigationBarsPadding(), color = Color.White, shadowElevation = 12.dp) {
        Row(Modifier.fillMaxWidth().padding(6.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            items.forEach { item ->
                val active = item == selected
                Surface(Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).clickable { onSelect(item) }, RoundedCornerShape(12.dp), if (active) UiBrandSoft else Color.Transparent) {
                    Column(Modifier.padding(vertical = 7.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(v4Glyph(item), color = if (active) UiBrand else UiMuted, fontSize = 17.sp)
                        Text(item, color = if (active) UiBrand else UiMuted, fontSize = 9.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Medium)
                    }
                }
            }
        }
    }
}

@Composable
private fun V4Page(page: String, cart: List<UiCart>, setCart: (List<UiCart>) -> Unit) {
    when (page) {
        "Dashboard" -> V4Dashboard { }
        "Penjualan" -> V4Sales(cart, setCart)
        "Produk" -> V4Products()
        "Persediaan" -> V4Inventory()
        "Pesanan" -> V4DataList("Pesanan", "Daftar transaksi penjualan dan status pembayaran", listOf("TRX-260901-024", "TRX-260901-023", "TRX-260901-022", "TRX-260901-021"))
        "Pelanggan" -> V4DataList("Pelanggan", "Database pelanggan toko", listOf("CUST-001 • Budi Santoso", "CUST-002 • Sari", "CUST-003 • Dewi"))
        "Supplier" -> V4DataList("Supplier", "Rekanan dan pemasok barang", listOf("SUP-001 • PT Kopi Nusantara", "SUP-002 • CV Sumber Pangan"))
        "Pembelian" -> V4DataList("Pembelian", "Purchase order dan penerimaan barang", listOf("PO-260901-004 • Diproses", "PO-260831-003 • Diterima"))
        "Piutang" -> V4DataList("Piutang", "Tagihan pelanggan dan saldo outstanding", listOf("INV-0021 • Rp 350.000 • Jatuh tempo", "INV-0018 • Rp 120.000 • Sebagian"))
        "Pembayaran" -> V4Payments()
        "Laporan" -> V4Reports()
        else -> V4Settings()
    }
}

@Composable
private fun V4Frame(title: String, subtitle: String, action: (@Composable () -> Unit)? = null, body: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().padding(20.dp, 18.dp, 20.dp, 16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, color = UiInk, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                Text(subtitle, color = UiMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 3.dp))
            }
            if (action != null) action()
        }
        Spacer(Modifier.height(16.dp))
        Column(Modifier.weight(1f), content = body)
    }
}

@Composable
private fun V4Dashboard(onNewSale: () -> Unit) {
    V4Frame("Dashboard", "Toko Demo • Owner • 1 September 2026", {
        Button(onClick = onNewSale, shape = RoundedCornerShape(12.dp)) { Text("＋ Transaksi Baru", fontWeight = FontWeight.Bold) }
    }) {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item {
                Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(UiBrand)) {
                    Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Penjualan hari ini", color = Color(0xFFE8E0FF), fontSize = 12.sp)
                            Text(money(1250000), color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 3.dp))
                            Text("+12,5% dibanding kemarin", color = Color(0xFFD8CCFF), fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp))
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("24 transaksi", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("70% via QRIS", color = Color(0xFFD8CCFF), fontSize = 10.sp, modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    V4Metric("Transaksi", "24", "hari ini", "▣", Modifier.weight(1f))
                    V4Metric("Pembayaran QRIS", money(875000), "70% omzet", "QR", Modifier.weight(1.25f))
                    V4Metric("Produk", "128", "SKU aktif", "□", Modifier.weight(1f))
                    V4Metric("Stok menipis", "3", "perlu perhatian", "!", Modifier.weight(1f), true)
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Card(Modifier.weight(1.35f), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(Color.White)) {
                        Column(Modifier.padding(16.dp)) {
                            V4SectionTitle("Transaksi terbaru", "Aktivitas penjualan hari ini")
                            uiSales.forEach { V4SaleRow(it) }
                        }
                    }
                    Card(Modifier.weight(1f), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(Color.White)) {
                        Column(Modifier.padding(16.dp)) {
                            V4SectionTitle("Kesehatan toko", "Ringkasan operasional")
                            V4Progress("Target omzet", 0.72f)
                            V4Progress("Pembayaran QRIS", 0.70f)
                            V4Progress("Stok aman", 0.86f)
                            Surface(Modifier.fillMaxWidth().padding(top = 8.dp), RoundedCornerShape(11.dp), UiGreenSoft) {
                                Text("●  Semua sistem berjalan normal", color = UiGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(10.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun V4Metric(title: String, value: String, note: String, glyph: String, modifier: Modifier, warning: Boolean = false) {
    Card(modifier, shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(Color.White), elevation = CardDefaults.cardElevation(1.dp)) {
        Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(38.dp).clip(RoundedCornerShape(11.dp)).background(if (warning) UiAmberSoft else UiBrandSoft), contentAlignment = Alignment.Center) {
                Text(glyph, color = if (warning) UiAmber else UiBrand, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Column(Modifier.padding(start = 9.dp)) {
                Text(title, color = UiMuted, fontSize = 9.sp)
                Text(value, color = UiInk, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 1.dp))
                Text(note, color = if (warning) UiAmber else UiMuted, fontSize = 8.sp, modifier = Modifier.padding(top = 1.dp))
            }
        }
    }
}

@Composable
private fun V4SectionTitle(title: String, subtitle: String) {
    Text(title, color = UiInk, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
    Text(subtitle, color = UiMuted, fontSize = 9.sp, modifier = Modifier.padding(top = 2.dp, bottom = 5.dp))
}

@Composable
private fun V4SaleRow(sale: UiSale) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(33.dp).clip(RoundedCornerShape(9.dp)).background(if (sale.method == "QRIS") UiBrandSoft else Color(0xFFF0F2F6)), contentAlignment = Alignment.Center) {
            Text(if (sale.method == "QRIS") "QR" else "Rp", color = if (sale.method == "QRIS") UiBrand else UiMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold)
        }
        Column(Modifier.padding(start = 9.dp).weight(1f)) {
            Text(sale.no, color = UiInk, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            Text("${sale.customer} • ${sale.time}", color = UiMuted, fontSize = 8.sp)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(money(sale.amount), color = UiInk, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            Text(sale.status, color = UiGreen, fontSize = 8.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun V4Progress(label: String, value: Float) {
    Column(Modifier.padding(vertical = 6.dp)) {
        Row(Modifier.fillMaxWidth()) {
            Text(label, color = UiInk, fontSize = 9.sp)
            Spacer(Modifier.weight(1f))
            Text("${(value * 100).toInt()}%", color = UiBrand, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }
        Box(Modifier.fillMaxWidth().padding(top = 4.dp).height(6.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFFEEF0F5))) {
            Box(Modifier.fillMaxWidth(value).fillMaxHeight().background(UiBrand))
        }
    }
}

@Composable
private fun V4Sales(cart: List<UiCart>, setCart: (List<UiCart>) -> Unit) {
    var search by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Semua") }
    var paymentOpen by remember { mutableStateOf(false) }
    val width = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp
    val compact = width < 600
    val filtered = uiProducts.filter { (category == "Semua" || it.category == category) && (search.isBlank() || it.name.contains(search, true) || it.sku.contains(search, true)) }
    val total = cart.sumOf { it.product.price * it.qty }

    V4Frame("Penjualan", "Kasir • Toko Demo • Pilih produk untuk masuk ke keranjang") {
        if (compact) {
            Column(Modifier.fillMaxSize()) {
                V4Search(search) { search = it }
                V4Categories(category) { category = it }
                LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(9.dp), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    items(filtered) { product -> V4ProductCard(product) { addCart(product, cart, setCart) } }
                }
                V4CompactCheckout(cart, total) { if (total > 0) paymentOpen = true }
            }
        } else {
            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Column(Modifier.weight(1.55f)) {
                    V4Search(search) { search = it }
                    V4Categories(category) { category = it }
                    LazyVerticalGrid(columns = GridCells.Adaptive(150.dp), verticalArrangement = Arrangement.spacedBy(9.dp), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                        items(filtered) { product -> V4ProductCard(product) { addCart(product, cart, setCart) } }
                    }
                }
                V4Cart(cart, total, setCart) { paymentOpen = true }
            }
        }
    }
    if (paymentOpen) {
        V4PaymentDialog(total) { paymentOpen = false; setCart(emptyList()) }
    }
}

private fun addCart(product: UiProduct, cart: List<UiCart>, setCart: (List<UiCart>) -> Unit) {
    val current = cart.firstOrNull { it.product.sku == product.sku }
    setCart(if (current == null) cart + UiCart(product, 1) else cart.map { if (it.product.sku == product.sku) it.copy(qty = it.qty + 1) else it })
}

@Composable
private fun V4Search(value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(value, onValueChange, Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(13.dp), label = { Text("Cari produk atau SKU") })
}

@Composable
private fun V4Categories(selected: String, onSelect: (String) -> Unit) {
    Row(Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 9.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf("Semua", "Makanan", "Minuman").forEach { item ->
            FilterChip(selected = item == selected, onClick = { onSelect(item) }, label = { Text(item, fontSize = 11.sp) })
        }
    }
}

@Composable
private fun V4ProductCard(product: UiProduct, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable { onClick() }, shape = RoundedCornerShape(15.dp), colors = CardDefaults.cardColors(Color.White)) {
        Column(Modifier.padding(12.dp)) {
            Box(Modifier.fillMaxWidth().height(74.dp).clip(RoundedCornerShape(12.dp)).background(UiBrandSoft), contentAlignment = Alignment.Center) {
                Text(product.category.uppercase(), color = UiBrand, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            Text(product.name, color = UiInk, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 9.dp))
            Text(product.sku, color = UiMuted, fontSize = 8.sp)
            Row(Modifier.fillMaxWidth().padding(top = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(money(product.price), color = UiBrand, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
                Text("Stok ${product.stock}", color = if (product.stock <= 10) UiAmber else UiMuted, fontSize = 8.sp)
            }
        }
    }
}

@Composable
private fun V4Cart(cart: List<UiCart>, total: Long, setCart: (List<UiCart>) -> Unit, onPay: () -> Unit) {
    Card(Modifier.width(330.dp).fillMaxHeight(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(Color.White)) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Keranjang", color = UiInk, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
                    Text("${cart.sumOf { it.qty }} item", color = UiMuted, fontSize = 10.sp)
                }
                if (cart.isNotEmpty()) TextButton(onClick = { setCart(emptyList()) }) { Text("Kosongkan", fontSize = 10.sp) }
            }
            Divider(Modifier.padding(vertical = 8.dp), color = UiLine)
            if (cart.isEmpty()) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("＋", color = UiBrand, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)
                        Text("Keranjang masih kosong", color = UiInk, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text("Pilih produk untuk memulai transaksi", color = UiMuted, fontSize = 9.sp, modifier = Modifier.padding(top = 3.dp))
                    }
                }
            } else {
                LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(cart) { item ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(item.product.name, color = UiInk, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Text("${item.qty} × ${money(item.product.price)}", color = UiMuted, fontSize = 8.sp)
                            }
                            Text(money(item.product.price * item.qty), color = UiInk, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            Divider(Modifier.padding(vertical = 8.dp), color = UiLine)
            Row(Modifier.fillMaxWidth()) { Text("Subtotal", color = UiMuted, fontSize = 10.sp); Spacer(Modifier.weight(1f)); Text(money(total), color = UiInk, fontSize = 10.sp) }
            Row(Modifier.fillMaxWidth().padding(top = 4.dp)) { Text("Diskon", color = UiMuted, fontSize = 10.sp); Spacer(Modifier.weight(1f)); Text("Rp 0", color = UiInk, fontSize = 10.sp) }
            Row(Modifier.fillMaxWidth().padding(top = 10.dp)) { Text("TOTAL", color = UiInk, fontSize = 12.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.weight(1f)); Text(money(total), color = UiBrand, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold) }
            Button(onClick = onPay, enabled = total > 0, Modifier.fillMaxWidth().padding(top = 12.dp), shape = RoundedCornerShape(12.dp)) { Text("Bayar • ${money(total)}", fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun V4CompactCheckout(cart: List<UiCart>, total: Long, onPay: () -> Unit) {
    Card(Modifier.fillMaxWidth().padding(top = 8.dp), shape = RoundedCornerShape(15.dp), colors = CardDefaults.cardColors(Color.White)) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { Text("${cart.sumOf { it.qty }} item", color = UiMuted, fontSize = 9.sp); Text(money(total), color = UiInk, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold) }
            Button(onClick = onPay, enabled = total > 0, shape = RoundedCornerShape(11.dp)) { Text("Bayar") }
        }
    }
}

@Composable
private fun V4PaymentDialog(total: Long, onComplete: () -> Unit) {
    AlertDialog(onDismissRequest = {}, confirmButton = { Button(onClick = onComplete) { Text("Simulasikan Lunas") } }, dismissButton = { TextButton(onClick = onComplete) { Text("Batal") } }, title = { Text("Pembayaran QRIS", fontWeight = FontWeight.ExtraBold) }, text = { Column(horizontalAlignment = Alignment.CenterHorizontally) { Box(Modifier.size(190.dp).clip(RoundedCornerShape(16.dp)).background(Color(0xFFF0F1F5)), contentAlignment = Alignment.Center) { Text("QRIS", color = UiBrand, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold) }; Text(money(total), color = UiInk, fontSize = 23.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 14.dp)); Text("Scan QRIS toko untuk membayar", color = UiMuted, fontSize = 10.sp, modifier = Modifier.padding(top = 4.dp)) } })
}

@Composable
private fun V4Products() {
    var search by remember { mutableStateOf("") }
    V4Frame("Produk", "Katalog produk, SKU, harga, dan status stok", { Button(onClick = {}, shape = RoundedCornerShape(12.dp)) { Text("＋ Tambah Produk") } }) {
        V4Search(search) { search = it }
        Spacer(Modifier.height(10.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(uiProducts.filter { it.name.contains(search, true) || it.sku.contains(search, true) }) { p ->
                Card(shape = RoundedCornerShape(15.dp), colors = CardDefaults.cardColors(Color.White)) {
                    Row(Modifier.fillMaxWidth().padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(42.dp).clip(RoundedCornerShape(11.dp)).background(UiBrandSoft), contentAlignment = Alignment.Center) { Text(p.category.take(1), color = UiBrand, fontWeight = FontWeight.ExtraBold) }
                        Column(Modifier.padding(start = 10.dp).weight(1f)) { Text(p.name, color = UiInk, fontSize = 12.sp, fontWeight = FontWeight.Bold); Text("${p.sku} • ${p.category}", color = UiMuted, fontSize = 9.sp) }
                        Column(horizontalAlignment = Alignment.End) { Text(money(p.price), color = UiInk, fontSize = 11.sp, fontWeight = FontWeight.Bold); Text("Stok ${p.stock}", color = if (p.stock <= 10) UiAmber else UiGreen, fontSize = 9.sp) }
                    }
                }
            }
        }
    }
}

@Composable
private fun V4Inventory() {
    V4Frame("Persediaan", "Pantau stok, barang menipis, dan kebutuhan restock", { OutlinedButton(onClick = {}, shape = RoundedCornerShape(12.dp)) { Text("Penyesuaian Stok") } }) {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) { V4Metric("SKU aktif", "128", "semua lokasi", "□", Modifier.weight(1f)); V4Metric("Stok menipis", "3", "butuh restock", "!", Modifier.weight(1f), true); V4Metric("Nilai stok", money(18450000), "estimasi", "Rp", Modifier.weight(1.3f)) } }
            items(uiProducts) { p ->
                Card(shape = RoundedCornerShape(15.dp), colors = CardDefaults.cardColors(Color.White)) {
                    Row(Modifier.fillMaxWidth().padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) { Text(p.name, color = UiInk, fontSize = 11.sp, fontWeight = FontWeight.Bold); Text("${p.sku} • Gudang utama", color = UiMuted, fontSize = 8.sp) }
                        Text("${p.stock} unit", color = if (p.stock <= 10) UiAmber else UiInk, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(14.dp)); Text(if (p.stock <= 10) "RESTOCK" else "AMAN", color = if (p.stock <= 10) UiAmber else UiGreen, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun V4DataList(title: String, subtitle: String, rows: List<String>) {
    V4Frame(title, subtitle, { Button(onClick = {}, shape = RoundedCornerShape(12.dp)) { Text("＋ Tambah") } }) {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(9.dp)) {
            items(rows) { row ->
                Card(shape = RoundedCornerShape(15.dp), colors = CardDefaults.cardColors(Color.White)) {
                    Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(38.dp).clip(CircleShape).background(UiBrandSoft), contentAlignment = Alignment.Center) { Text("•", color = UiBrand, fontSize = 18.sp) }
                        Text(row, Modifier.padding(start = 11.dp).weight(1f), color = UiInk, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        Text("›", color = UiMuted, fontSize = 20.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun V4Payments() {
    V4Frame("Pembayaran", "Monitoring pembayaran QRIS dan metode pembayaran lain") {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) { V4Metric("QRIS hari ini", money(875000), "17 transaksi", "QR", Modifier.weight(1.2f)); V4Metric("Tunai", money(245000), "5 transaksi", "Rp", Modifier.weight(1f)); V4Metric("Lainnya", money(130000), "2 transaksi", "••", Modifier.weight(1f)) }
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) { items(uiSales) { sale -> Card(shape = RoundedCornerShape(15.dp), colors = CardDefaults.cardColors(Color.White)) { V4SaleRow(sale) } } }
        }
    }
}

@Composable
private fun V4Reports() {
    V4Frame("Laporan", "Ringkasan performa toko berdasarkan data transaksi") {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) { V4Metric("Omzet bulan ini", money(32850000), "+8,4%", "Rp", Modifier.weight(1.3f)); V4Metric("Transaksi", "486", "bulan ini", "▣", Modifier.weight(1f)); V4Metric("Rata-rata", money(67500), "/ transaksi", "≈", Modifier.weight(1f)) } }
            item { Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(Color.White)) { Column(Modifier.padding(16.dp)) { V4SectionTitle("Penjualan mingguan", "Performa 7 hari terakhir"); V4Progress("Senin", .62f); V4Progress("Selasa", .74f); V4Progress("Rabu", .55f); V4Progress("Kamis", .81f); V4Progress("Jumat", .92f) } } }
        }
    }
}

@Composable
private fun V4Settings() {
    var qrisEnabled by remember { mutableStateOf(true) }
    V4Frame("Pengaturan", "Kelola toko, QRIS, struk, printer, dan akses pengguna") {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { V4SettingCard("Profil Toko", "Toko Demo • Cabang Utama", "Informasi identitas toko") }
            item { V4SettingCard("QRIS Toko", "QRIS aktif • Merchant Toko Demo", "Upload dan kelola QRIS statis") { qrisEnabled = !qrisEnabled } }
            item { V4SettingCard("Struk & Printer", "80 mm • Printer kasir", "Logo, alamat, footer, dan printer") }
            item { V4SettingCard("Pengguna & Role", "1 Owner • 2 Kasir", "Atur hak akses aplikasi") }
            item { V4SettingCard("Sinkronisasi", if (qrisEnabled) "Supabase • Terhubung" else "Mode lokal", "Status koneksi data") }
        }
    }
}

@Composable
private fun V4SettingCard(title: String, value: String, description: String, onClick: (() -> Unit)? = null) {
    Card(Modifier.fillMaxWidth().clickable(enabled = onClick != null) { onClick?.invoke() }, shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(Color.White)) {
        Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)).background(UiBrandSoft), contentAlignment = Alignment.Center) { Text("•", color = UiBrand, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold) }
            Column(Modifier.padding(start = 11.dp).weight(1f)) { Text(title, color = UiInk, fontSize = 12.sp, fontWeight = FontWeight.Bold); Text(value, color = UiBrand, fontSize = 10.sp, fontWeight = FontWeight.SemiBold); Text(description, color = UiMuted, fontSize = 9.sp) }
            Text("›", color = UiMuted, fontSize = 21.sp)
        }
    }
}
