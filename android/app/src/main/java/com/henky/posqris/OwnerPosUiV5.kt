@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.henky.posqris

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.*
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

private val v5Client = createSupabaseClient(BuildConfig.SUPABASE_URL, BuildConfig.SUPABASE_PUBLISHABLE_KEY) { install(Postgrest) }
private val V5Bg = Color(0xFFF4F7FB)
private val V5Navy = Color(0xFF071A33)
private val V5Blue = Color(0xFF2563EB)
private val V5Blue2 = Color(0xFF5483E3)
private val V5Soft = Color(0xFFEAF2FF)
private val V5Text = Color(0xFF617089)
private val V5Green = Color(0xFF14966B)
private val V5Amber = Color(0xFFF2A900)
private val V5Red = Color(0xFFD94A4A)
private val V5Menus = listOf("Dashboard","Penjualan","Pesanan","Produk","Stok","Pelanggan","Supplier","Pembelian","Piutang","Laporan","Pembayaran","Pengaturan")

private fun jStr(o: JsonObject, k: String) = o[k]?.jsonPrimitive?.contentOrNull ?: ""
private fun jLong(o: JsonObject, k: String) = o[k]?.jsonPrimitive?.longOrNull ?: 0L
private fun jDouble(o: JsonObject, k: String) = o[k]?.jsonPrimitive?.doubleOrNull ?: 0.0
private fun jBool(o: JsonObject, k: String) = o[k]?.jsonPrimitive?.booleanOrNull ?: false
private fun v5Money(v: Long) = "Rp " + v.toString().reversed().chunked(3).joinToString(".").reversed()
private fun statusColor(s: String) = when (s.uppercase()) {
    "PAID", "COMPLETED", "ACTIVE", "RECEIVED" -> V5Green
    "OPEN", "PENDING", "PARTIAL", "LOW", "DRAFT" -> V5Amber
    "FAILED", "EXPIRED", "CANCELLED", "OVERDUE", "VOID" -> V5Red
    else -> V5Text
}
private fun menuIcon(name: String) = when (name) {
    "Dashboard" -> Icons.Default.Home
    "Penjualan" -> Icons.Default.PointOfSale
    "Pesanan" -> Icons.Default.ReceiptLong
    "Produk" -> Icons.Default.Inventory2
    "Stok" -> Icons.Default.Warehouse
    "Pelanggan" -> Icons.Default.People
    "Supplier" -> Icons.Default.LocalShipping
    "Pembelian" -> Icons.Default.ShoppingCart
    "Piutang" -> Icons.Default.AccountBalanceWallet
    "Laporan" -> Icons.Default.BarChart
    "Pembayaran" -> Icons.Default.Payments
    else -> Icons.Default.Settings
}

@Composable
fun OwnerPosAppV5() {
    var page by remember { mutableStateOf("Dashboard") }
    var more by remember { mutableStateOf(false) }
    val tablet = LocalConfiguration.current.screenWidthDp >= 600
    MaterialTheme(colorScheme = lightColorScheme(primary = V5Blue, background = V5Bg, surface = Color.White)) {
        Surface(Modifier.fillMaxSize(), color = V5Bg) {
            if (tablet) Row(Modifier.fillMaxSize()) {
                SidebarV5(page) { page = it }
                Box(Modifier.weight(1f).fillMaxSize()) { V5Page(page) }
            } else {
                Box(Modifier.fillMaxSize()) {
                    Column(Modifier.fillMaxSize()) {
                        Box(Modifier.weight(1f).fillMaxWidth()) { V5Page(page) }
                        BottomV5(page, { page = it }) { more = true }
                    }
                    if (more) MoreV5(page, { page = it; more = false }) { more = false }
                }
            }
        }
    }
}

@Composable private fun SidebarV5(selected: String, onSelect: (String) -> Unit) {
    Surface(Modifier.width(246.dp).fillMaxHeight(), color = V5Navy) {
        Column(Modifier.fillMaxHeight().padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(Modifier.padding(7.dp, 9.dp, 7.dp, 18.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(42.dp).background(V5Blue, RoundedCornerShape(13.dp)), contentAlignment = Alignment.Center) { Icon(Icons.Default.QrCode2, null, tint = Color.White) }
                Column(Modifier.padding(start = 10.dp)) { Text("POS QRIS", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold); Text("Owner workspace", color = Color(0xFFA9BAD0), fontSize = 11.sp) }
            }
            V5Menus.forEach { m ->
                val active = m == selected
                Surface(Modifier.fillMaxWidth().clickable { onSelect(m) }, RoundedCornerShape(12.dp), if (active) V5Blue else Color.Transparent) {
                    Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(menuIcon(m), null, Modifier.size(19.dp), tint = if (active) Color.White else Color(0xFFB7C6D9))
                        Text(m, Modifier.padding(start = 11.dp), color = if (active) Color.White else Color(0xFFE7EEF7), fontWeight = if (active) FontWeight.Bold else FontWeight.Medium)
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            Surface(RoundedCornerShape(15.dp), Color(0xFF0D2542)) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(34.dp).background(V5Soft, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) { Icon(Icons.Default.AdminPanelSettings, null, tint = V5Blue) }
                    Column(Modifier.padding(start = 9.dp)) { Text("OWNER", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp); Text("Live Supabase", color = Color(0xFFA9BAD0), fontSize = 10.sp) }
                }
            }
        }
    }
}

@Composable private fun BottomV5(selected: String, onSelect: (String) -> Unit, onMore: () -> Unit) {
    val main = listOf("Dashboard", "Penjualan", "Produk", "Stok")
    Surface(Modifier.fillMaxWidth().navigationBarsPadding(), color = Color.White, shadowElevation = 14.dp) {
        Row(Modifier.fillMaxWidth().padding(7.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            main.forEach { m -> NavItemV5(m, selected == m, Modifier.weight(1f)) { onSelect(m) } }
            NavItemV5("Lainnya", selected !in main, Modifier.weight(1f)) { onMore() }
        }
    }
}

@Composable private fun NavItemV5(label: String, active: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Surface(modifier.clickable { onClick() }, RoundedCornerShape(13.dp), if (active) V5Soft else Color.Transparent) {
        Column(Modifier.padding(vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(if (label == "Lainnya") Icons.Default.MoreHoriz else menuIcon(label), null, Modifier.size(19.dp), tint = if (active) V5Blue else V5Text)
            Text(label, fontSize = 10.sp, color = if (active) V5Blue else V5Text, fontWeight = if (active) FontWeight.Bold else FontWeight.Medium)
        }
    }
}

@Composable private fun MoreV5(selected: String, onSelect: (String) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color.White) {
        Column(Modifier.fillMaxWidth().padding(18.dp)) {
            Text("Menu Owner", color = V5Navy, fontSize = 23.sp, fontWeight = FontWeight.ExtraBold)
            Text("Semua modul menggunakan data Supabase", color = V5Text, fontSize = 12.sp, modifier = Modifier.padding(top = 3.dp, bottom = 13.dp))
            V5Menus.drop(4).forEach { m ->
                Surface(Modifier.fillMaxWidth().clickable { onSelect(m) }, RoundedCornerShape(12.dp), if (m == selected) V5Soft else Color.Transparent) {
                    Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) { Icon(menuIcon(m), null, Modifier.size(20.dp), tint = V5Blue); Text(m, Modifier.padding(start = 12.dp), color = V5Navy, fontWeight = if (m == selected) FontWeight.Bold else FontWeight.Medium) }
                }
            }
            Spacer(Modifier.height(14.dp))
        }
    }
}

@Composable private fun V5Page(page: String) {
    when (page) {
        "Dashboard" -> DashboardV5()
        "Penjualan" -> SalesV5()
        "Pesanan" -> DataModuleV5("Pesanan", "sales", listOf("sale_no","customer_id","total_amount","status","sale_date"))
        "Produk" -> ProductsV5()
        "Stok" -> StockV5()
        "Pelanggan" -> DataModuleV5("Pelanggan", "customers", listOf("code","name","phone","customer_type","is_active"))
        "Supplier" -> DataModuleV5("Supplier", "suppliers", listOf("code","name","phone","bank_name","is_active"))
        "Pembelian" -> DataModuleV5("Pembelian", "purchase_orders", listOf("order_no","supplier_id","total_amount","status","order_date"))
        "Piutang" -> DataModuleV5("Piutang", "receivables", listOf("invoice_no","customer_id","original_amount","outstanding_amount","status"))
        "Laporan" -> ReportsV5()
        "Pembayaran" -> DataModuleV5("Pembayaran", "payments", listOf("payment_no","sale_id","amount","status","provider"))
        "Pengaturan" -> SettingsV5()
    }
}

@Composable private fun PageHeaderV5(title: String, subtitle: String, onRefresh: () -> Unit, refreshing: Boolean = false) {
    Row(Modifier.fillMaxWidth().padding(bottom = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) { Text(title, color = V5Navy, fontSize = 27.sp, fontWeight = FontWeight.ExtraBold); Text(subtitle, color = V5Text, fontSize = 12.sp, modifier = Modifier.padding(top = 3.dp)) }
        OutlinedButton(onClick = onRefresh, enabled = !refreshing, shape = RoundedCornerShape(12.dp)) { Icon(Icons.Default.Refresh, null, Modifier.size(17.dp)); Spacer(Modifier.width(6.dp)); Text(if (refreshing) "Memuat" else "Refresh") }
    }
}

@Composable private fun ModernCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) { Card(modifier, RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(Color.White), elevation = CardDefaults.cardElevation(1.dp)) { Column(Modifier.padding(15.dp), content = content) } }
@Composable private fun StatusBadgeV5(status: String) { val c = statusColor(status); Surface(RoundedCornerShape(8.dp), c.copy(alpha = .11f)) { Text(status.uppercase(), color = c, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) } }

@Composable private fun DashboardV5() {
    var business by remember { mutableStateOf<JsonObject?>(null) }; var branch by remember { mutableStateOf<JsonObject?>(null) }; var products by remember { mutableStateOf<List<JsonObject>>(emptyList()) }; var customers by remember { mutableStateOf<List<JsonObject>>(emptyList()) }; var sales by remember { mutableStateOf<List<JsonObject>>(emptyList()) }; var payments by remember { mutableStateOf<List<JsonObject>>(emptyList()) }; var stock by remember { mutableStateOf<List<JsonObject>>(emptyList()) }; var refreshing by remember { mutableStateOf(false) }; val scope = rememberCoroutineScope()
    suspend fun load(){refreshing=true;try{business=v5Client.from("businesses").select{filter{eq("is_active",true)}}.decodeList<JsonObject>().firstOrNull();val bid=business?.let{jStr(it,"id")};if(bid!=null)branch=v5Client.from("branches").select{filter{eq("business_id",bid);eq("is_active",true)}}.decodeList<JsonObject>().firstOrNull();products=v5Client.from("products").select{filter{eq("is_active",true)}}.decodeList<JsonObject>();customers=v5Client.from("customers").select{filter{eq("is_active",true)}}.decodeList<JsonObject>();sales=v5Client.from("sales").select().decodeList<JsonObject>();payments=v5Client.from("payments").select().decodeList<JsonObject>();stock=v5Client.from("stock_balances").select().decodeList<JsonObject>()}finally{refreshing=false}}
    LaunchedEffect(Unit){load();while(true){delay(10000);runCatching{load()}}};val today=LocalDate.now(ZoneId.of("Asia/Jakarta")).toString();val todaySales=sales.filter{jStr(it,"sale_date").startsWith(today)&&jStr(it,"status").uppercase()=="COMPLETED"};val low=stock.count{r->products.firstOrNull{jStr(it,"id")==jStr(r,"product_id")}?.let{jDouble(r,"qty_base")<=jDouble(it,"min_stock")}==true}
    Column(Modifier.fillMaxSize().padding(20.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){PageHeaderV5("Beranda","${jStr(business?:buildJsonObject{},"name")} • ${jStr(branch?:buildJsonObject{},"name")} • Owner",{scope.launch{runCatching{load()}}},refreshing);LazyColumn(verticalArrangement=Arrangement.spacedBy(14.dp)){item{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)){MetricV5("Penjualan hari ini",v5Money(todaySales.sumOf{jLong(it,"total_amount")}),"${todaySales.size} transaksi",Modifier.weight(1.3f));MetricV5("Produk",products.size.toString(),"SKU aktif",Modifier.weight(1f));MetricV5("Pelanggan",customers.size.toString(),"aktif",Modifier.weight(1f));MetricV5("Stok menipis",low.toString(),"perlu perhatian",Modifier.weight(1f))}};item{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(14.dp)){ModernCard(Modifier.weight(1.4f)){Text("Transaksi terbaru",color=V5Navy,fontWeight=FontWeight.ExtraBold);if(sales.isEmpty())EmptyStateV5("Belum ada transaksi")else sales.sortedByDescending{jStr(it,"sale_date")}.take(8).forEach{s->Row(Modifier.fillMaxWidth().padding(vertical=9.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(jStr(s,"sale_no"),color=V5Navy,fontWeight=FontWeight.SemiBold);Text(jStr(s,"sale_date").replace("T"," ").take(16),color=V5Text,fontSize=10.sp)};Text(v5Money(jLong(s,"total_amount")),color=V5Navy,fontWeight=FontWeight.Bold);Spacer(Modifier.width(8.dp));StatusBadgeV5(jStr(s,"status"))}}};ModernCard(Modifier.weight(1f)){Text("Live database",color=V5Navy,fontWeight=FontWeight.ExtraBold);Text("Data aplikasi dibaca langsung dari Supabase.",color=V5Text,fontSize=12.sp,modifier=Modifier.padding(top=5.dp));Spacer(Modifier.height(12.dp));LiveLineV5("Sales",sales.size.toString());LiveLineV5("Payments",payments.size.toString());LiveLineV5("Products",products.size.toString());LiveLineV5("Stock balances",stock.size.toString())}}}}}
}
@Composable private fun MetricV5(label:String,value:String,caption:String,modifier:Modifier){ModernCard(modifier){Text(label,color=V5Text,fontSize=11.sp);Text(value,color=V5Navy,fontSize=21.sp,fontWeight=FontWeight.ExtraBold,modifier=Modifier.padding(top=4.dp));Text(caption,color=V5Text,fontSize=10.sp,modifier=Modifier.padding(top=2.dp))}}
@Composable private fun LiveLineV5(label:String,value:String){Row(Modifier.fillMaxWidth().padding(vertical=7.dp),verticalAlignment=Alignment.CenterVertically){Box(Modifier.size(8.dp).background(V5Blue2,RoundedCornerShape(50)));Text(label,Modifier.weight(1f).padding(start=8.dp),color=V5Text,fontSize=12.sp);Text(value,color=V5Navy,fontWeight=FontWeight.Bold)}}
@Composable private fun EmptyStateV5(text:String){Box(Modifier.fillMaxWidth().padding(vertical=25.dp),contentAlignment=Alignment.Center){Text(text,color=V5Text,fontSize=12.sp)}}

@Composable private fun SalesV5(){
    var products by remember{mutableStateOf<List<JsonObject>>(emptyList())};var prices by remember{mutableStateOf<List<JsonObject>>(emptyList())};var cats by remember{mutableStateOf<List<JsonObject>>(emptyList())};var stock by remember{mutableStateOf<List<JsonObject>>(emptyList())};var branch by remember{mutableStateOf<JsonObject?>(null)};var location by remember{mutableStateOf<JsonObject?>(null)};var methods by remember{mutableStateOf<List<JsonObject>>(emptyList())};var cart by remember{mutableStateOf<List<V5CartLine>>(emptyList())};var search by remember{mutableStateOf("")};var cat by remember{mutableStateOf("Semua")};var cartOpen by remember{mutableStateOf(false)};var paymentOpen by remember{mutableStateOf(false)};var refreshing by remember{mutableStateOf(false)};val scope=rememberCoroutineScope()
    suspend fun load(){refreshing=true;try{val b=v5Client.from("businesses").select{filter{eq("is_active",true)}}.decodeList<JsonObject>().first();branch=v5Client.from("branches").select{filter{eq("business_id",jStr(b,"id"));eq("is_active",true)}}.decodeList<JsonObject>().first();location=v5Client.from("locations").select{filter{eq("branch_id",jStr(branch!!,"id"));eq("is_active",true)}}.decodeList<JsonObject>().first();products=v5Client.from("products").select{filter{eq("business_id",jStr(b,"id"));eq("is_active",true)}}.decodeList<JsonObject>();cats=v5Client.from("categories").select{filter{eq("business_id",jStr(b,"id"));eq("is_active",true)}}.decodeList<JsonObject>();val pl=v5Client.from("price_lists").select{filter{eq("business_id",jStr(b,"id"));eq("is_default",true);eq("is_active",true)}}.decodeList<JsonObject>().firstOrNull();prices=if(pl==null)emptyList()else v5Client.from("product_prices").select{filter{eq("price_list_id",jStr(pl,"id"))}}.decodeList<JsonObject>();stock=v5Client.from("stock_balances").select().decodeList<JsonObject>();methods=v5Client.from("payment_methods").select{filter{eq("business_id",jStr(b,"id"));eq("is_active",true)}}.decodeList<JsonObject>()}finally{refreshing=false}}
    LaunchedEffect(Unit){load();while(true){delay(10000);runCatching{load()}}};fun price(p:JsonObject)=prices.filter{jStr(it,"product_id")==jStr(p,"id")}.minByOrNull{jDouble(it,"min_qty")}?.let{jLong(it,"price")}?:0L;fun qty(p:JsonObject)=stock.firstOrNull{jStr(it,"product_id")==jStr(p,"id")&&jStr(it,"location_id")==jStr(location?:buildJsonObject{},"id")}?.let{jDouble(it,"qty_base")}?:0.0;val shown=products.filter{(search.isBlank()||jStr(it,"name").contains(search,true)||jStr(it,"sku").contains(search,true))&&(cat=="Semua"||cats.firstOrNull{x->jStr(x,"id")==jStr(it,"category_id")}?.let{x->jStr(x,"name")}==cat)};val total=cart.sumOf{it.unitPrice*it.qty}
    Column(Modifier.fillMaxSize().padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){PageHeaderV5("Penjualan Baru","${jStr(branch?:buildJsonObject{},"name")} • kasir Owner",{scope.launch{runCatching{load()}}},refreshing);Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp),verticalAlignment=Alignment.CenterVertically){OutlinedTextField(search,{search=it},Modifier.weight(1f),singleLine=true,shape=RoundedCornerShape(14.dp),placeholder={Text("Cari produk atau scan barcode")},leadingIcon={Icon(Icons.Default.Search,null)});AssistChip(onClick={},label={Text("Pelanggan Umum")})};Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(7.dp)){(listOf("Semua")+cats.map{jStr(it,"name")}).forEach{m->FilterChip(selected=cat==m,onClick={cat=m},label={Text(m,fontSize=11.sp)})}};Box(Modifier.weight(1f).fillMaxWidth()){if(shown.isEmpty())EmptyStateV5("Tidak ada produk dari database")else LazyVerticalGrid(GridCells.Adaptive(155.dp),contentPadding=PaddingValues(bottom=90.dp),horizontalArrangement=Arrangement.spacedBy(10.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){items(shown){p->ModernCard(Modifier.fillMaxWidth().clickable{val old=cart.firstOrNull{it.productId==jStr(p,"id")};cart=if(old==null)cart+V5CartLine(jStr(p,"id"),jStr(p,"name"),jStr(p,"sku"),jStr(p,"base_unit_id"),jLong(p,"current_cost"),price(p),1)else cart.map{if(it.productId==old.productId)it.copy(qty=it.qty+1)else it}}){Box(Modifier.fillMaxWidth().height(76.dp).background(V5Soft,RoundedCornerShape(14.dp)),contentAlignment=Alignment.Center){Icon(Icons.Default.Inventory2,null,Modifier.size(34.dp),tint=V5Blue2)};Text(jStr(p,"name"),color=V5Navy,fontWeight=FontWeight.Bold,modifier=Modifier.padding(top=10.dp));Text(v5Money(price(p)),color=V5Blue,fontWeight=FontWeight.ExtraBold,fontSize=15.sp);Text("Stok ${qty(p).toInt()}",color=V5Text,fontSize=10.sp,modifier=Modifier.padding(top=2.dp))}}}}};Surface(Modifier.fillMaxWidth().clickable{cartOpen=true},RoundedCornerShape(17.dp),V5Navy){Row(Modifier.padding(14.dp),verticalAlignment=Alignment.CenterVertically){Box(Modifier.size(40.dp).background(V5Blue,RoundedCornerShape(12.dp)),contentAlignment=Alignment.Center){Icon(Icons.Default.ShoppingCart,null,tint=Color.White)};Column(Modifier.weight(1f).padding(start=10.dp)){Text("Keranjang • ${cart.sumOf{it.qty}} item",color=Color.White,fontWeight=FontWeight.Bold);Text("Total ${v5Money(total)}",color=Color(0xFFBFD0E5),fontSize=11.sp)};Text("Bayar",color=Color.White,fontWeight=FontWeight.ExtraBold,fontSize=16.sp)}}};if(cartOpen)CartSheetV5(total,cart,{cart=it},{cartOpen=false},{paymentOpen=true;cartOpen=false});if(paymentOpen)PaymentSheetV5(total,methods,branch,location,cart,{paymentOpen=false},{paymentOpen=false;cart=emptyList();scope.launch{runCatching{load()}}})}
}
private data class V5CartLine(val productId:String,val name:String,val sku:String,val unitId:String,val hpp:Long,val unitPrice:Long,val qty:Long)

@Composable private fun CartSheetV5(total:Long,cart:List<V5CartLine>,setCart:(List<V5CartLine>)->Unit,onDismiss:()->Unit,onPay:()->Unit){ModalBottomSheet(onDismissRequest=onDismiss,containerColor=Color.White){Column(Modifier.fillMaxWidth().padding(18.dp)){Text("Detail Pesanan",fontSize=23.sp,fontWeight=FontWeight.ExtraBold,color=V5Navy);Text("${cart.sumOf{it.qty}} item",color=V5Text,fontSize=12.sp,modifier=Modifier.padding(top=3.dp,bottom=12.dp));if(cart.isEmpty())EmptyStateV5("Keranjang kosong")else LazyColumn(Modifier.heightIn(max=390.dp),verticalArrangement=Arrangement.spacedBy(9.dp)){items(cart){line->ModernCard{Row(verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(line.name,color=V5Navy,fontWeight=FontWeight.Bold);Text("${v5Money(line.unitPrice)} • ${line.sku}",color=V5Text,fontSize=10.sp)};Row(verticalAlignment=Alignment.CenterVertically){IconButton(onClick={val q=line.qty-1;setCart(if(q<=0)cart.filterNot{it.productId==line.productId}else cart.map{if(it.productId==line.productId)it.copy(qty=q)else it})}){Icon(Icons.Default.Remove,null)};Text(line.qty.toString(),fontWeight=FontWeight.Bold);IconButton(onClick={setCart(cart.map{if(it.productId==line.productId)it.copy(qty=it.qty+1)else it})}){Icon(Icons.Default.Add,null)}}}}}};Spacer(Modifier.height(12.dp));SummaryV5(total,0,0,0);Button(onClick=onPay,enabled=cart.isNotEmpty(),Modifier.fillMaxWidth().height(52.dp),shape=RoundedCornerShape(14.dp)){Text("Lanjut Pembayaran",fontWeight=FontWeight.ExtraBold)}}}}
@Composable private fun SummaryV5(total:Long,paid:Long,remaining:Long,change:Long){ModernCard{SummaryLineV5("Subtotal",v5Money(total));SummaryLineV5("Diskon",v5Money(0));SummaryLineV5("Pajak",v5Money(0));HorizontalDivider(Modifier.padding(vertical=8.dp));SummaryLineV5("TOTAL",v5Money(total),true);SummaryLineV5("Dibayar",v5Money(paid));SummaryLineV5("Sisa",v5Money(remaining));SummaryLineV5("Kembalian",v5Money(change),true)}}
@Composable private fun SummaryLineV5(label:String,value:String,bold:Boolean=false){Row(Modifier.fillMaxWidth().padding(vertical=4.dp)){Text(label,Modifier.weight(1f),color=if(bold)V5Navy else V5Text,fontWeight=if(bold)FontWeight.Bold else FontWeight.Normal);Text(value,color=V5Navy,fontWeight=if(bold)FontWeight.ExtraBold else FontWeight.SemiBold)}}

@Composable private fun PaymentSheetV5(total:Long,methods:List<JsonObject>,branch:JsonObject?,location:JsonObject?,cart:List<V5CartLine>,onDismiss:()->Unit,onDone:()->Unit){var selected by remember{mutableStateOf(methods.firstOrNull()?.let{jStr(it,"id")}?:"")};var cashReceived by remember{mutableStateOf(total.toString())};var amount by remember{mutableStateOf(total.toString())};var reference by remember{mutableStateOf("")};var loading by remember{mutableStateOf(false)};var error by remember{mutableStateOf("")};var success by remember{mutableStateOf("")};val scope=rememberCoroutineScope();val selectedMethod=methods.firstOrNull{jStr(it,"id")==selected};val code=jStr(selectedMethod?:buildJsonObject{},"code").uppercase();val payAmount=amount.toLongOrNull()?:0L;val received=cashReceived.toLongOrNull()?:0L;val change=(received-total).coerceAtLeast(0);ModalBottomSheet(onDismissRequest={if(!loading)onDismiss()},containerColor=Color.White){Column(Modifier.fillMaxWidth().padding(18.dp)){Text("Pembayaran",fontSize=23.sp,fontWeight=FontWeight.ExtraBold,color=V5Navy);Text("Total pembayaran",color=V5Text,fontSize=12.sp);Text(v5Money(total),fontSize=28.sp,fontWeight=FontWeight.ExtraBold,color=V5Navy,modifier=Modifier.padding(bottom=12.dp));Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)){methods.forEach{m->FilterChip(selected=selected==jStr(m,"id"),onClick={selected=jStr(m,"id")},label={Text(jStr(m,"name"))})}};Spacer(Modifier.height(10.dp));OutlinedTextField(amount,{amount=it.filter(Char::isDigit)},Modifier.fillMaxWidth(),singleLine=true,label={Text("Nominal pembayaran")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),shape=RoundedCornerShape(13.dp));if(code=="CASH"){OutlinedTextField(cashReceived,{cashReceived=it.filter(Char::isDigit)},Modifier.fillMaxWidth().padding(top=9.dp),singleLine=true,label={Text("Uang diterima")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),shape=RoundedCornerShape(13.dp));Spacer(Modifier.height(10.dp));Surface(Modifier.fillMaxWidth(),RoundedCornerShape(15.dp),V5Soft){Column(Modifier.padding(14.dp)){Text("KEMBALIAN",color=V5Text,fontSize=10.sp,fontWeight=FontWeight.Bold);Text(v5Money(change),color=V5Blue,fontSize=24.sp,fontWeight=FontWeight.ExtraBold)}}}else if(code=="QRIS"){Surface(Modifier.fillMaxWidth().height(190.dp),RoundedCornerShape(16.dp),Color(0xFFF7FAFF)){Column(Modifier.fillMaxSize(),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center){Icon(Icons.Default.QrCode2,null,Modifier.size(82.dp),tint=V5Navy);Text("QRIS",color=V5Navy,fontWeight=FontWeight.Bold);Text("Menunggu konfirmasi simulasi",color=V5Text,fontSize=11.sp)}}}else{OutlinedTextField(reference,{reference=it},Modifier.fillMaxWidth().padding(top=9.dp),singleLine=true,label={Text("Referensi transfer")},shape=RoundedCornerShape(13.dp))};Spacer(Modifier.height(10.dp));SummaryV5(total,payAmount,(total-payAmount).coerceAtLeast(0),if(code=="CASH")change else 0);if(error.isNotBlank())Text(error,color=V5Red,fontSize=12.sp,modifier=Modifier.padding(top=7.dp));if(success.isNotBlank())Text(success,color=V5Green,fontWeight=FontWeight.Bold,modifier=Modifier.padding(top=7.dp));Button(onClick={scope.launch{loading=true;error="";success="";runCatching{v5Client.postgrest.rpc("checkout_sale_multi_payment",buildJsonObject{put("p_branch_id",jStr(branch!!,"id"));put("p_location_id",jStr(location!!,"id"));put("p_customer_id",null as String?);put("p_items",buildJsonArray{cart.forEach{line->add(buildJsonObject{put("product_id",line.productId);put("unit_id",line.unitId);put("sku",line.sku);put("name",line.name);put("qty",line.qty);put("conversion_to_base",1);put("unit_price",line.unitPrice);put("hpp_unit",line.hpp)})}});put("p_payments",buildJsonArray{add(buildJsonObject{put("payment_method_id",selected);put("amount",payAmount);put("cash_received",if(code=="CASH")received else payAmount);put("reference",reference);put("qris_confirmed",code=="QRIS")})});put("p_idempotency_key",UUID.randomUUID().toString())});success="Transaksi berhasil disimpan ke Supabase";onDone()}.onFailure{error=it.message?:"Checkout gagal"};loading=false}},enabled=!loading&&cart.isNotEmpty()&&payAmount>0&&selected.isNotBlank(),Modifier.fillMaxWidth().height(52.dp),shape=RoundedCornerShape(14.dp)){Text(if(loading)"Memproses..." else "Proses Pembayaran",fontWeight=FontWeight.ExtraBold)}}}}

@Composable private fun ProductsV5(){var rows by remember{mutableStateOf<List<JsonObject>>(emptyList())};var cats by remember{mutableStateOf<List<JsonObject>>(emptyList())};var units by remember{mutableStateOf<List<JsonObject>>(emptyList())};var prices by remember{mutableStateOf<List<JsonObject>>(emptyList())};var priceLists by remember{mutableStateOf<List<JsonObject>>(emptyList())};var query by remember{mutableStateOf("")};var add by remember{mutableStateOf(false)};var refreshing by remember{mutableStateOf(false)};val scope=rememberCoroutineScope();suspend fun load(){refreshing=true;try{val b=v5Client.from("businesses").select{filter{eq("is_active",true)}}.decodeList<JsonObject>().first();rows=v5Client.from("products").select{filter{eq("business_id",jStr(b,"id"))}}.decodeList<JsonObject>();cats=v5Client.from("categories").select{filter{eq("business_id",jStr(b,"id"));eq("is_active",true)}}.decodeList<JsonObject>();units=v5Client.from("units").select{filter{eq("business_id",jStr(b,"id"));eq("is_active",true)}}.decodeList<JsonObject>();priceLists=v5Client.from("price_lists").select{filter{eq("business_id",jStr(b,"id"));eq("is_active",true)}}.decodeList<JsonObject>();prices=v5Client.from("product_prices").select().decodeList<JsonObject>()}finally{refreshing=false}};LaunchedEffect(Unit){load();while(true){delay(10000);runCatching{load()}}};val filtered=rows.filter{query.isBlank()||jStr(it,"name").contains(query,true)||jStr(it,"sku").contains(query,true)};Column(Modifier.fillMaxSize().padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){PageHeaderV5("Produk","Master produk • live Supabase",{scope.launch{runCatching{load()}}},refreshing);Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)){OutlinedTextField(query,{query=it},Modifier.weight(1f),singleLine=true,placeholder={Text("Cari nama atau SKU")},leadingIcon={Icon(Icons.Default.Search,null)},shape=RoundedCornerShape(13.dp));Button(onClick={add=true},shape=RoundedCornerShape(12.dp)){Icon(Icons.Default.Add,null);Spacer(Modifier.width(5.dp));Text("Produk")}};LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp)){items(filtered){p->ModernCard{Row(verticalAlignment=Alignment.CenterVertically){Box(Modifier.size(45.dp).background(V5Soft,RoundedCornerShape(12.dp)),contentAlignment=Alignment.Center){Icon(Icons.Default.Inventory2,null,tint=V5Blue)};Column(Modifier.weight(1f).padding(start=11.dp)){Text(jStr(p,"name"),color=V5Navy,fontWeight=FontWeight.Bold);Text(jStr(p,"sku"),color=V5Text,fontSize=10.sp)};Column(horizontalAlignment=Alignment.End){Text(v5Money(prices.filter{jStr(it,"product_id")==jStr(p,"id")}.minByOrNull{jDouble(it,"min_qty")}?.let{jLong(it,"price")}?:0),color=V5Blue,fontWeight=FontWeight.ExtraBold);Text(jStr(cats.firstOrNull{x->jStr(x,"id")==jStr(p,"category_id")}?:buildJsonObject{},"name"),color=V5Text,fontSize=10.sp)}}}}}}};if(add)AddProductV5(cats,units,priceLists,{add=false},{scope.launch{runCatching{load()}}})}
}
@Composable private fun AddProductV5(cats:List<JsonObject>,units:List<JsonObject>,priceLists:List<JsonObject>,onDismiss:()->Unit,onSaved:()->Unit){var name by remember{mutableStateOf("")};var sku by remember{mutableStateOf("")};var price by remember{mutableStateOf("")};var category by remember{mutableStateOf(cats.firstOrNull()?.let{jStr(it,"id")}?:"")};var saving by remember{mutableStateOf(false)};var error by remember{mutableStateOf("")};val scope=rememberCoroutineScope();ModalBottomSheet(onDismissRequest={if(!saving)onDismiss()},containerColor=Color.White){Column(Modifier.fillMaxWidth().padding(18.dp)){Text("Produk Baru",fontSize=23.sp,fontWeight=FontWeight.ExtraBold,color=V5Navy);OutlinedTextField(name,{name=it},Modifier.fillMaxWidth().padding(top=10.dp),label={Text("Nama produk")},singleLine=true);OutlinedTextField(sku,{sku=it},Modifier.fillMaxWidth().padding(top=9.dp),label={Text("SKU")},singleLine=true);OutlinedTextField(price,{price=it.filter(Char::isDigit)},Modifier.fillMaxWidth().padding(top=9.dp),label={Text("Harga")},singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number));if(cats.isNotEmpty()){Text("Kategori",color=V5Text,fontSize=11.sp,modifier=Modifier.padding(top=10.dp,bottom=5.dp));Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(7.dp)){cats.forEach{c->FilterChip(selected=category==jStr(c,"id"),onClick={category=jStr(c,"id")},label={Text(jStr(c,"name"))})}}};if(error.isNotBlank())Text(error,color=V5Red,fontSize=12.sp,modifier=Modifier.padding(top=8.dp));Button(onClick={scope.launch{saving=true;error="";runCatching{val b=v5Client.from("businesses").select{filter{eq("is_active",true)}}.decodeList<JsonObject>().first();val unit=units.firstOrNull()?:error("Belum ada unit aktif di Supabase");val pl=priceLists.firstOrNull{jBool(it,"is_default")}?:priceLists.firstOrNull()?:error("Belum ada price list aktif di Supabase");val pid=UUID.randomUUID().toString();v5Client.from("products").insert(buildJsonObject{put("id",pid);put("business_id",jStr(b,"id"));put("sku",sku.trim());put("name",name.trim());put("category_id",category.ifBlank{null as String?});put("base_unit_id",jStr(unit,"id"));put("product_type","GOODS");put("track_batch",false);put("track_expiry",false);put("min_stock",0);put("reorder_point",0);put("cost_method","AVERAGE");put("last_purchase_cost",0);put("current_cost",0);put("is_active",true)});v5Client.from("product_prices").insert(buildJsonObject{put("id",UUID.randomUUID().toString());put("price_list_id",jStr(pl,"id"));put("product_id",pid);put("unit_id",jStr(unit,"id"));put("min_qty",1);put("price",price.toLongOrNull()?:0);put("discount_percent",0)});onSaved();onDismiss()}.onFailure{error=it.message?:"Gagal menyimpan"};saving=false}},enabled=!saving&&name.isNotBlank()&&sku.isNotBlank()&&price.isNotBlank(),Modifier.fillMaxWidth().height(52.dp),shape=RoundedCornerShape(14.dp)){Text(if(saving)"Menyimpan..." else "Simpan ke Supabase",fontWeight=FontWeight.ExtraBold)}}}

@Composable private fun StockV5(){var products by remember{mutableStateOf<List<JsonObject>>(emptyList())};var stock by remember{mutableStateOf<List<JsonObject>>(emptyList())};var refreshing by remember{mutableStateOf(false)};val scope=rememberCoroutineScope();suspend fun load(){refreshing=true;try{products=v5Client.from("products").select{filter{eq("is_active",true)}}.decodeList<JsonObject>();stock=v5Client.from("stock_balances").select().decodeList<JsonObject>()}finally{refreshing=false}};LaunchedEffect(Unit){load();while(true){delay(10000);runCatching{load()}}};Column(Modifier.fillMaxSize().padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){PageHeaderV5("Stok","Saldo stok per lokasi • live Supabase",{scope.launch{runCatching{load()}}},refreshing);LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp)){items(products){p->val s=stock.firstOrNull{jStr(it,"product_id")==jStr(p,"id")};val q=jDouble(s?:buildJsonObject{},"qty_base");val low=q<=jDouble(p,"min_stock");ModernCard{Row(verticalAlignment=Alignment.CenterVertically){Box(Modifier.size(42.dp).background(if(low)Color(0xFFFFF2F2)else V5Soft,RoundedCornerShape(11.dp)),contentAlignment=Alignment.Center){Icon(if(low)Icons.Default.WarningAmber else Icons.Default.Warehouse,null,tint=if(low)V5Red else V5Blue)};Column(Modifier.weight(1f).padding(start=10.dp)){Text(jStr(p,"name"),color=V5Navy,fontWeight=FontWeight.Bold);Text(jStr(p,"sku"),color=V5Text,fontSize=10.sp)};Column(horizontalAlignment=Alignment.End){Text(q.toInt().toString(),color=V5Navy,fontSize=19.sp,fontWeight=FontWeight.ExtraBold);Text(if(low)"Stok menipis"else"tersedia",color=if(low)V5Red else V5Text,fontSize=10.sp)}}}}}}}

@Composable private fun DataModuleV5(title:String,table:String,columns:List<String>){var rows by remember{mutableStateOf<List<JsonObject>>(emptyList())};var refreshing by remember{mutableStateOf(false)};var query by remember{mutableStateOf("")};val scope=rememberCoroutineScope();suspend fun load(){refreshing=true;try{rows=v5Client.from(table).select().decodeList<JsonObject>()}finally{refreshing=false}};LaunchedEffect(Unit){load();while(true){delay(10000);runCatching{load()}}};val filtered=rows.filter{query.isBlank()||columns.any{c->jStr(it,c).contains(query,true)}};Column(Modifier.fillMaxSize().padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){PageHeaderV5(title,"$table • live Supabase",{scope.launch{runCatching{load()}}},refreshing);OutlinedTextField(query,{query=it},Modifier.fillMaxWidth(),singleLine=true,placeholder={Text("Cari data")},leadingIcon={Icon(Icons.Default.Search,null)},shape=RoundedCornerShape(13.dp));LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp)){items(filtered){row->ModernCard{columns.forEachIndexed{idx,c->if(idx<5){Row(Modifier.fillMaxWidth().padding(vertical=3.dp)){Text(c,Modifier.weight(1f),color=V5Text,fontSize=10.sp);val v=jStr(row,c);if(c=="status")StatusBadgeV5(v)else Text(if(c.contains("amount"))v5Money(jLong(row,c))else v.ifBlank{"-"},color=V5Navy,fontSize=11.sp,fontWeight=if(idx==1)FontWeight.SemiBold else FontWeight.Normal)}}}}}}}}

@Composable private fun ReportsV5(){var sales by remember{mutableStateOf<List<JsonObject>>(emptyList())};var payments by remember{mutableStateOf<List<JsonObject>>(emptyList())};var refreshing by remember{mutableStateOf(false)};val scope=rememberCoroutineScope();suspend fun load(){refreshing=true;try{sales=v5Client.from("sales").select().decodeList<JsonObject>();payments=v5Client.from("payments").select().decodeList<JsonObject>()}finally{refreshing=false}};LaunchedEffect(Unit){load();while(true){delay(10000);runCatching{load()}}};val completed=sales.filter{jStr(it,"status").uppercase()=="COMPLETED"};Column(Modifier.fillMaxSize().padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){PageHeaderV5("Laporan","Ringkasan dihitung dari data sales dan payments",{scope.launch{runCatching{load()}}},refreshing);LazyColumn(verticalArrangement=Arrangement.spacedBy(12.dp)){item{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)){MetricV5("Omzet",v5Money(completed.sumOf{jLong(it,"total_amount")}),"sales completed",Modifier.weight(1.3f));MetricV5("Transaksi",completed.size.toString(),"completed",Modifier.weight(1f));MetricV5("Payments",payments.size.toString(),"record",Modifier.weight(1f))}};item{ModernCard{Text("Payment mix",color=V5Navy,fontWeight=FontWeight.ExtraBold);payments.groupBy{jStr(it,"provider").ifBlank{"OTHER"}}.forEach{(k,v)->LiveLineV5(k,v.size.toString())}}};item{ModernCard{Text("Penjualan terbaru",color=V5Navy,fontWeight=FontWeight.ExtraBold);completed.sortedByDescending{jStr(it,"sale_date")}.take(10).forEach{s->Row(Modifier.fillMaxWidth().padding(vertical=8.dp)){Text(jStr(s,"sale_no"),Modifier.weight(1f),color=V5Navy,fontWeight=FontWeight.SemiBold);Text(v5Money(jLong(s,"total_amount")),color=V5Navy,fontWeight=FontWeight.Bold)}}}}}}}

@Composable private fun SettingsV5(){var business by remember{mutableStateOf<List<JsonObject>>(emptyList())};var branches by remember{mutableStateOf<List<JsonObject>>(emptyList())};var locations by remember{mutableStateOf<List<JsonObject>>(emptyList())};var methods by remember{mutableStateOf<List<JsonObject>>(emptyList())};var qris by remember{mutableStateOf<List<JsonObject>>(emptyList())};var priceLists by remember{mutableStateOf<List<JsonObject>>(emptyList())};var refreshing by remember{mutableStateOf(false)};val scope=rememberCoroutineScope();suspend fun load(){refreshing=true;try{business=v5Client.from("businesses").select().decodeList<JsonObject>();branches=v5Client.from("branches").select().decodeList<JsonObject>();locations=v5Client.from("locations").select().decodeList<JsonObject>();methods=v5Client.from("payment_methods").select().decodeList<JsonObject>();qris=v5Client.from("qris_configurations").select().decodeList<JsonObject>();priceLists=v5Client.from("price_lists").select().decodeList<JsonObject>()}finally{refreshing=false}};LaunchedEffect(Unit){load();while(true){delay(10000);runCatching{load()}}};Column(Modifier.fillMaxSize().padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){PageHeaderV5("Pengaturan","Konfigurasi bisnis dan pembayaran • live Supabase",{scope.launch{runCatching{load()}}},refreshing);LazyColumn(verticalArrangement=Arrangement.spacedBy(12.dp)){item{SettingsCardV5("Bisnis",business,listOf("code","name","phone","email"))};item{SettingsCardV5("Cabang",branches,listOf("code","name","phone","city"))};item{SettingsCardV5("Lokasi",locations,listOf("code","name","location_type","is_active"))};item{SettingsCardV5("Metode pembayaran",methods,listOf("code","name","method_type","is_active"))};item{SettingsCardV5("QRIS",qris,listOf("provider","mode","display_name","merchant_name","nmid","is_active"))};item{SettingsCardV5("Price list",priceLists,listOf("code","name","price_type","is_default","is_active"))}}}}
@Composable private fun SettingsCardV5(title:String,rows:List<JsonObject>,columns:List<String>){ModernCard{Text(title,color=V5Navy,fontWeight=FontWeight.ExtraBold);if(rows.isEmpty())EmptyStateV5("Belum ada data")else rows.forEach{r->Column(Modifier.padding(top=8.dp)){columns.forEach{c->val v=jStr(r,c);if(v.isNotBlank())Row{Text(c,Modifier.weight(1f),color=V5Text,fontSize=10.sp);Text(v,color=V5Navy,fontSize=11.sp,fontWeight=FontWeight.SemiBold)}}}}}}
