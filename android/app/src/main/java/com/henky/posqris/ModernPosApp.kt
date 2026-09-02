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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val MInk = Color(0xFF171B2B)
private val MMuted = Color(0xFF748096)
private val MBrand = Color(0xFF5D3DC6)
private val MSoft = Color(0xFFF0EAFF)
private val MBg = Color(0xFFF7F8FC)
private val MLine = Color(0xFFE7E9F0)
private val MGreen = Color(0xFF159A6C)
private val MGreenSoft = Color(0xFFE8F7F1)
private val MAmber = Color(0xFFE79A18)
private val MAmberSoft = Color(0xFFFFF4DC)

private data class MP(val sku:String,val name:String,val category:String,val price:Long,val stock:Int)
private data class MS(val no:String,val customer:String,val total:Long,val method:String,val time:String)
private data class MC(val product:MP,val qty:Int)

private val products = listOf(
    MP("SKU-001","Kopi Susu Gula Aren","Minuman",18000,42), MP("SKU-002","Americano","Minuman",15000,31),
    MP("SKU-003","Matcha Latte","Minuman",22000,18), MP("SKU-004","Roti Cokelat","Makanan",12000,27),
    MP("SKU-005","Croissant Butter","Makanan",16000,9), MP("SKU-006","Nasi Ayam Sambal","Makanan",28000,14),
    MP("SKU-007","Air Mineral","Minuman",6000,63), MP("SKU-008","Kentang Goreng","Makanan",19000,7),
    MP("SKU-009","Teh Lemon","Minuman",14000,22), MP("SKU-010","Donat Gula","Makanan",9000,5)
)
private val sales = listOf(
    MS("TRX-260901-024","Pelanggan Umum",68000,"QRIS","11:08"), MS("TRX-260901-023","Budi",45000,"Tunai","10:52"),
    MS("TRX-260901-022","Pelanggan Umum",32000,"QRIS","10:41"), MS("TRX-260901-021","Sari",92000,"QRIS","10:25"),
    MS("TRX-260901-020","Pelanggan Umum",28000,"Tunai","10:12")
)
private fun rm(v:Long)="Rp "+v.toString().reversed().chunked(3).joinToString(".").reversed()
private fun dateLabel()=LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy",Locale("id","ID")))

@Composable fun ModernPosApp(){
    var page by remember{mutableStateOf("Dashboard")}; var cart by remember{mutableStateOf(listOf<MC>())}
    val tablet=androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp>=600
    MaterialTheme(colorScheme=lightColorScheme(primary=MBrand,background=MBg,surface=Color.White,onSurface=MInk)){
        Surface(Modifier.fillMaxSize(),color=MBg){if(tablet) Row(Modifier.fillMaxSize()){MSidebar(page){page=it};Box(Modifier.weight(1f).fillMaxHeight()){MPage(page,cart,{cart=it})}}else Column(Modifier.fillMaxSize()){Box(Modifier.weight(1f)){MPage(page,cart,{cart=it})};MBottom(page){page=it}}}
    }
}

@Composable private fun MPage(page:String,cart:List<MC>,setCart:(List<MC>)->Unit){when(page){"Dashboard"->MDashboard{page="Penjualan"};"Penjualan"->MPOS(cart,setCart);"Produk"->MProducts();"Persediaan"->MInventory();"Pembayaran"->MPayments();"Laporan"->MReports();else->MSettings()}}

@Composable private fun MSidebar(selected:String,onSelect:(String)->Unit){val pages=listOf("Dashboard","Penjualan","Produk","Persediaan","Pembayaran","Laporan","Pengaturan");Surface(Modifier.width(244.dp).fillMaxHeight(),Color(0xFF171B27)){Column(Modifier.fillMaxHeight().padding(14.dp),verticalArrangement=Arrangement.spacedBy(5.dp)){Row(Modifier.padding(8.dp,8.dp,8.dp,18.dp),verticalAlignment=Alignment.CenterVertically){Box(Modifier.size(42.dp).clip(RoundedCornerShape(13.dp)).background(MBrand),contentAlignment=Alignment.Center){Text("QR",color=Color.White,fontWeight=FontWeight.ExtraBold)};Column(Modifier.padding(start=10.dp)){Text("POS QRIS",color=Color.White,fontSize=18.sp,fontWeight=FontWeight.ExtraBold);Text("Toko Demo • Owner",color=Color(0xFF9CA7BB),fontSize=11.sp)}};pages.forEach{p->val a=p==selected;Surface(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable{onSelect(p)},if(a)MBrand else Color.Transparent){Row(Modifier.padding(12.dp,11.dp),verticalAlignment=Alignment.CenterVertically){Text(MGlyph(p),color=if(a)Color.White else Color(0xFFAAB4C7),fontSize=18.sp,modifier=Modifier.width(30.dp));Text(p,color=if(a)Color.White else Color(0xFFE6EAF2),fontWeight=if(a)FontWeight.Bold else FontWeight.Medium,fontSize=13.sp)}}};Spacer(Modifier.weight(1f));Surface(Modifier.fillMaxWidth(),RoundedCornerShape(14.dp),Color(0xFF222838)){Column(Modifier.padding(13.dp)){Text("STATUS TOKO",color=Color(0xFF929DB1),fontSize=10.sp,fontWeight=FontWeight.Bold);Row(Modifier.padding(top=7.dp),verticalAlignment=Alignment.CenterVertically){Text("●  Online",color=Color.White,fontSize=12.sp,fontWeight=FontWeight.SemiBold);Spacer(Modifier.weight(1f));Text("Supabase",color=Color(0xFF8E99AE),fontSize=10.sp)}}}}}
private fun MGlyph(p:String)=when(p){"Dashboard"->"⌂";"Penjualan"->"＋";"Produk"->"□";"Persediaan"->"▤";"Pembayaran"->"QR";"Laporan"->"▥";else->"⚙"}

@Composable private fun MBottom(selected:String,onSelect:(String)->Unit){listOf("Dashboard","Penjualan","Produk","Persediaan").let{items->Surface(Modifier.fillMaxWidth().navigationBarsPadding(),Color.White,shadowElevation=12.dp){Row(Modifier.padding(6.dp),horizontalArrangement=Arrangement.spacedBy(4.dp)){items.forEach{p->val a=p==selected;Surface(Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).clickable{onSelect(p)},if(a)MSoft else Color.Transparent){Column(Modifier.padding(vertical=7.dp),horizontalAlignment=Alignment.CenterHorizontally){Text(MGlyph(p),color=if(a)MBrand else MMuted,fontSize=17.sp);Text(p,color=if(a)MBrand else MMuted,fontSize=10.sp,fontWeight=if(a)FontWeight.Bold else FontWeight.Medium)}}}}}}}

@Composable private fun MScreen(title:String,sub:String,content:@Composable()->Unit){Column(Modifier.fillMaxSize().padding(20.dp,18.dp,20.dp,16.dp)){Text(title,color=MInk,fontSize=28.sp,fontWeight=FontWeight.ExtraBold);Text(sub,color=MMuted,fontSize=12.sp,modifier=Modifier.padding(top=3.dp,bottom=16.dp));Box(Modifier.weight(1f).fillMaxWidth()){content()}}}

@Composable private fun MDashboard(open:()->Unit){MScreen("Dashboard","Toko Demo • Owner • $dateLabel()"){LazyColumn(verticalArrangement=Arrangement.spacedBy(14.dp)){item{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp),verticalAlignment=Alignment.CenterVertically){Card(Modifier.weight(1f),RoundedCornerShape(20.dp),colors=CardDefaults.cardColors(MBrand)){Column(Modifier.padding(18.dp)){Text("Penjualan hari ini",color=Color(0xFFE9E2FF),fontSize=12.sp);Text(rm(1250000),color=Color.White,fontSize=30.sp,fontWeight=FontWeight.ExtraBold);Text("+12,5% dibanding kemarin",color=Color(0xFFD8CCFF),fontSize=10.sp)}};Button(open,Modifier.height(74.dp),shape=RoundedCornerShape(14.dp)){Text("＋ Transaksi Baru",fontWeight=FontWeight.Bold)}}};item{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)){MMetric("Transaksi","24","hari ini","▣",Modifier.weight(1f));MMetric("QRIS","Rp 875.000","70% omzet","QR",Modifier.weight(1.1f));MMetric("Stok menipis","3","perlu perhatian","!",Modifier.weight(1f),true)}};item{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(14.dp)){Card(Modifier.weight(1.3f),RoundedCornerShape(18.dp),colors=CardDefaults.cardColors(Color.White)){Column(Modifier.padding(17.dp)){MTitle("Transaksi terbaru","5 transaksi terakhir");sales.forEach{MSale(it)}}};Card(Modifier.weight(1f),RoundedCornerShape(18.dp),colors=CardDefaults.cardColors(Color.White)){Column(Modifier.padding(17.dp)){MTitle("Kinerja hari ini","Status operasional");MProgress("Target omzet",.72f);MProgress("QRIS",.70f);MProgress("Stok aman",.86f);Surface(Modifier.fillMaxWidth(),RoundedCornerShape(12.dp),MGreenSoft){Text("●  Semua sistem berjalan normal",color=MGreen,fontSize=10.sp,fontWeight=FontWeight.Bold,modifier=Modifier.padding(11.dp))}}}}}}}

@Composable private fun MMetric(t:String,v:String,n:String,i:String,m:Modifier,w:Boolean=false){Card(m,RoundedCornerShape(16.dp),colors=CardDefaults.cardColors(Color.White),elevation=CardDefaults.cardElevation(1.dp)){Row(Modifier.padding(13.dp),verticalAlignment=Alignment.CenterVertically){Box(Modifier.size(37.dp).clip(RoundedCornerShape(10.dp)).background(if(w)MAmberSoft else MSoft),contentAlignment=Alignment.Center){Text(i,color=if(w)MAmber else MBrand,fontWeight=FontWeight.Bold,fontSize=12.sp)};Column(Modifier.padding(start=9.dp)){Text(t,color=MMuted,fontSize=9.sp);Text(v,color=MInk,fontSize=16.sp,fontWeight=FontWeight.ExtraBold);Text(n,color=if(w)MAmber else MMuted,fontSize=8.sp)}}}}
@Composable private fun MTitle(t:String,s:String){Row(Modifier.fillMaxWidth()){Column(Modifier.weight(1f)){Text(t,color=MInk,fontSize=15.sp,fontWeight=FontWeight.ExtraBold);Text(s,color=MMuted,fontSize=9.sp)}}}
@Composable private fun MSale(s:MS){Row(Modifier.fillMaxWidth().padding(vertical=8.dp),verticalAlignment=Alignment.CenterVertically){Box(Modifier.size(33.dp).clip(RoundedCornerShape(9.dp)).background(if(s.method=="QRIS")MSoft else Color(0xFFF1F3F7)),contentAlignment=Alignment.Center){Text(if(s.method=="QRIS")"QR" else "Rp",color=if(s.method=="QRIS")MBrand else MMuted,fontSize=9.sp,fontWeight=FontWeight.Bold)};Column(Modifier.padding(start=9.dp).weight(1f)){Text(s.no,color=MInk,fontSize=10.sp,fontWeight=FontWeight.Bold);Text("${s.customer} • ${s.time}",color=MMuted,fontSize=8.sp)};Column(horizontalAlignment=Alignment.End){Text(rm(s.total),fontSize=10.sp,fontWeight=FontWeight.Bold);Text("LUNAS",color=MGreen,fontSize=8.sp,fontWeight=FontWeight.Bold)}}}
@Composable private fun MProgress(label:String,v:Float){Column(Modifier.padding(vertical=7.dp)){Row(Modifier.fillMaxWidth()){Text(label,color=MInk,fontSize=9.sp);Spacer(Modifier.weight(1f));Text("${(v*100).toInt()}%",color=MBrand,fontSize=9.sp,fontWeight=FontWeight.Bold)};Box(Modifier.fillMaxWidth().padding(top=5.dp).height(7.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFFEEF0F5))){Box(Modifier.fillMaxWidth(v).fillMaxHeight().background(MBrand))}}}

@Composable private fun MPOS(cart:List<MC>,setCart:(List<MC>)->Unit){var search by remember{mutableStateOf("")};var cat by remember{mutableStateOf("Semua")};var pay by remember{mutableStateOf(false)};val cats=listOf("Semua","Makanan","Minuman");val filtered=products.filter{(cat=="Semua"||it.category==cat)&&(search.isBlank()||it.name.contains(search,true)||it.sku.contains(search,true))};val total=cart.sumOf{it.product.price*it.qty};MScreen("Penjualan","Kasir • Toko Demo • $dateLabel()"){Row(Modifier.fillMaxSize(),horizontalArrangement=Arrangement.spacedBy(14.dp)){Column(Modifier.weight(1.5f)){OutlinedTextField(search,{search=it},Modifier.fillMaxWidth(),singleLine=true,shape=RoundedCornerShape(13.dp),label={Text("Cari produk atau SKU")});Row(Modifier.horizontalScroll(rememberScrollState()).padding(vertical=9.dp),horizontalArrangement=Arrangement.spacedBy(7.dp)){cats.forEach{Filter(it,it==cat){cat=it}}};LazyVerticalGrid(columns=GridCells.Adaptive(145.dp),verticalArrangement=Arrangement.spacedBy(9.dp),horizontalArrangement=Arrangement.spacedBy(9.dp)){items(filtered){p->MProduct(p){add(p,cart,setCart)}}}};Card(Modifier.width(325.dp).fillMaxHeight(),RoundedCornerShape(20.dp),colors=CardDefaults.cardColors(Color.White)){Column(Modifier.fillMaxHeight().padding(16.dp)){Text("Keranjang",fontSize=18.sp,fontWeight=FontWeight.ExtraBold);Text("${cart.sumOf{it.qty}} item",color=MMuted,fontSize=9.sp);Spacer(Modifier.height(8.dp));if(cart.isEmpty())Box(Modifier.weight(1f).fillMaxWidth(),contentAlignment=Alignment.Center){Text("Belum ada produk",color=MMuted,fontSize=11.sp)}else LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(8.dp)){items(cart){line->MCart(line,cart,setCart)}};Spacer(Modifier.height(8.dp));MRow("Subtotal",rm(total));MRow("Diskon","Rp 0");Row(Modifier.padding(top=7.dp)){Text("TOTAL",fontWeight=FontWeight.ExtraBold);Spacer(Modifier.weight(1f));Text(rm(total),color=MBrand,fontSize=20.sp,fontWeight=FontWeight.ExtraBold)};Button({if(total>0)pay=true},Modifier.fillMaxWidth().padding(top=11.dp).height(50.dp),enabled=total>0,shape=RoundedCornerShape(13.dp)){Text("Lanjut Pembayaran",fontWeight=FontWeight.Bold)}}}};if(pay)MPayment(total){pay=false;setCart(emptyList())}}

@Composable private fun Filter(t:String,a:Boolean,on:()->Unit){Surface(Modifier.clip(RoundedCornerShape(10.dp)).clickable{on()},if(a)MBrand else Color.White,shadowElevation=if(a)0.dp else 1.dp){Text(t,color=if(a)Color.White else MInk,fontSize=10.sp,fontWeight=if(a)FontWeight.Bold else FontWeight.Medium,modifier=Modifier.padding(12.dp,8.dp))}}
@Composable private fun MProduct(p:MP,on:()->Unit){Card(Modifier.fillMaxWidth().height(145.dp).clickable{on()},RoundedCornerShape(15.dp),colors=CardDefaults.cardColors(Color.White)){Column(Modifier.padding(12.dp)){Box(Modifier.fillMaxWidth().height(50.dp).clip(RoundedCornerShape(11.dp)).background(MSoft),contentAlignment=Alignment.Center){Text(p.category.take(1),color=MBrand,fontSize=20.sp,fontWeight=FontWeight.ExtraBold)};Text(p.name,color=MInk,fontSize=10.sp,fontWeight=FontWeight.Bold,maxLines=1,modifier=Modifier.padding(top=8.dp));Row(Modifier.padding(top=4.dp)){Text(rm(p.price),color=MBrand,fontSize=10.sp,fontWeight=FontWeight.ExtraBold);Spacer(Modifier.weight(1f));Text("Stok ${p.stock}",color=if(p.stock<=7)MAmber else MMuted,fontSize=8.sp)}}}}
private fun add(p:MP,c:List<MC,set:(List<MC>)->Unit){val x=c.firstOrNull{it.product.sku==p.sku};set(if(x==null)c+MC(p,1)else c.map{if(it.product.sku==p.sku)it.copy(qty=it.qty+1)else it})}
@Composable private fun MCart(l:MC,c:List<MC,set:(List<MC>)->Unit){Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(l.product.name,fontSize=9.sp,fontWeight=FontWeight.SemiBold);Text(rm(l.product.price),color=MMuted,fontSize=8.sp)};Text("−",Modifier.clickable{qty(l,-1,c,set)},color=MBrand,fontWeight=FontWeight.Bold);Text(" ${l.qty} ",fontSize=10.sp,fontWeight=FontWeight.Bold);Text("+",Modifier.clickable{qty(l,1,c,set)},color=MBrand,fontWeight=FontWeight.Bold)}}
private fun qty(l:MC,d:Int,c:List<MC,set:(List<MC>)->Unit){val n=l.qty+d;set(if(n<=0)c.filterNot{it.product.sku==l.product.sku}else c.map{if(it.product.sku==l.product.sku)it.copy(qty=n)else it})}
@Composable private fun MRow(a:String,b:String){Row(Modifier.fillMaxWidth().padding(vertical=2.dp)){Text(a,color=MMuted,fontSize=9.sp);Spacer(Modifier.weight(1f));Text(b,color=MInk,fontSize=9.sp,fontWeight=FontWeight.SemiBold)}}

@Composable private fun MPayment(total:Long,done:()->Unit){var method by remember{mutableStateOf("QRIS")};AlertDialog(onDismissRequest=done,title={Text("Pembayaran",fontWeight=FontWeight.ExtraBold)},text={Column(horizontalAlignment=Alignment.CenterHorizontally){Text("Total",color=MMuted,fontSize=10.sp);Text(rm(total),color=MBrand,fontSize=26.sp,fontWeight=FontWeight.ExtraBold);Row(horizontalArrangement=Arrangement.spacedBy(7.dp),modifier=Modifier.padding(vertical=10.dp)){Filter("QRIS",method=="QRIS"){method="QRIS"};Filter("Tunai",method=="Tunai"){method="Tunai"}};if(method=="QRIS")MQr()else Text("Pembayaran tunai siap diproses.",color=MMuted,fontSize=10.sp)}},confirmButton={Button(done,shape=RoundedCornerShape(10.dp)){Text("Simulasikan Dibayar")}},dismissButton={TextButton(done){Text("Batal")}})}
@Composable private fun MQr(){Column(horizontalAlignment=Alignment.CenterHorizontally){Box(Modifier.size(150.dp).background(Color.White),contentAlignment=Alignment.Center){Column(verticalArrangement=Arrangement.spacedBy(2.dp)){repeat(10){r->Row(horizontalArrangement=Arrangement.spacedBy(2.dp)){repeat(10){c->Box(Modifier.size(11.dp).background(if((r*5+c*7+r)%3==0)Color.White else Color.Black))}}}}};Text("QRIS • Toko Demo",fontSize=10.sp,fontWeight=FontWeight.Bold,modifier=Modifier.padding(top=6.dp));Text("Demo UI • QR asli akan berasal dari konfigurasi toko",color=MMuted,fontSize=8.sp)}}

@Composable private fun MProducts(){MScreen("Produk","Katalog, harga, kategori, dan status produk"){LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp)){items(products){p->Card(Modifier.fillMaxWidth(),RoundedCornerShape(14.dp),colors=CardDefaults.cardColors(Color.White)){Row(Modifier.padding(13.dp),verticalAlignment=Alignment.CenterVertically){Box(Modifier.size(36.dp).background(MSoft,RoundedCornerShape(10.dp)),contentAlignment=Alignment.Center){Text(p.category.take(1),color=MBrand,fontWeight=FontWeight.Bold)};Column(Modifier.padding(start=9.dp).weight(1f)){Text(p.name,fontSize=11.sp,fontWeight=FontWeight.Bold);Text("${p.sku} • ${p.category}",color=MMuted,fontSize=8.sp)};Column(horizontalAlignment=Alignment.End){Text(rm(p.price),color=MBrand,fontSize=10.sp,fontWeight=FontWeight.Bold);Text("Stok ${p.stock}",color=if(p.stock<=7)MAmber else MGreen,fontSize=8.sp)}}}}}}
@Composable private fun MInventory(){MScreen("Persediaan","Pantau stok minimum dan kebutuhan restock"){LazyColumn(verticalArrangement=Arrangement.spacedBy(9.dp)){item{Row(horizontalArrangement=Arrangement.spacedBy(9.dp)){MMetric("Total SKU","10","aktif","□",Modifier.weight(1f));MMetric("Stok aman","7","SKU","✓",Modifier.weight(1f));MMetric("Restock","3","SKU","!",Modifier.weight(1f),true)}};item{Card(Modifier.fillMaxWidth(),RoundedCornerShape(16.dp),colors=CardDefaults.cardColors(Color.White)){Column(Modifier.padding(15.dp)){MTitle("Monitoring stok","Paling kritis di atas");products.sortedBy{it.stock}.forEach{p->Row(Modifier.fillMaxWidth().padding(vertical=7.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(p.name,fontSize=10.sp,fontWeight=FontWeight.SemiBold);Text("Minimum 10 unit",color=MMuted,fontSize=8.sp)};Text("${p.stock} unit",color=if(p.stock<=7)MAmber else MGreen,fontSize=10.sp,fontWeight=FontWeight.Bold)}}}}}}}
@Composable private fun MPayments(){MScreen("Pembayaran","Pantau pembayaran QRIS dan transaksi kasir"){LazyColumn(verticalArrangement=Arrangement.spacedBy(10.dp)){item{Row(horizontalArrangement=Arrangement.spacedBy(9.dp)){MMetric("QRIS","Rp 875.000","21 transaksi","QR",Modifier.weight(1f));MMetric("Berhasil","24","100%","✓",Modifier.weight(1f));MMetric("Menunggu","0","transaksi","…",Modifier.weight(1f))}};item{Card(Modifier.fillMaxWidth(),RoundedCornerShape(16.dp),colors=CardDefaults.cardColors(Color.White)){Column(Modifier.padding(15.dp)){MTitle("Riwayat pembayaran","Terbaru");sales.forEach{MSale(it)}}}};item{Surface(Modifier.fillMaxWidth(),RoundedCornerShape(16.dp),MSoft){Column(Modifier.padding(15.dp)){Text("QRIS toko",color=MBrand,fontSize=14.sp,fontWeight=FontWeight.ExtraBold);Text("Toko Demo • merchant aktif",fontSize=10.sp);OutlinedButton({},modifier=Modifier.padding(top=7.dp),shape=RoundedCornerShape(10.dp)){Text("Kelola QRIS")}}}}}}
@Composable private fun MReports(){MScreen("Laporan","Ringkasan performa penjualan dan pembayaran"){LazyColumn(verticalArrangement=Arrangement.spacedBy(11.dp)){item{Card(Modifier.fillMaxWidth(),RoundedCornerShape(17.dp),colors=CardDefaults.cardColors(Color.White)){Column(Modifier.padding(16.dp)){MTitle("Penjualan 7 hari","Data demo untuk validasi UI");listOf(.45f,.63f,.51f,.78f,.68f,.91f,.72f).forEachIndexed{i,v->Row(Modifier.padding(vertical=5.dp),verticalAlignment=Alignment.CenterVertically){Text("H-${6-i}",Modifier.width(30.dp),fontSize=8.sp,color=MMuted);Box(Modifier.weight(1f).height(12.dp).background(Color(0xFFEEEAF8))){Box(Modifier.fillMaxWidth(v).fillMaxHeight().background(MBrand))};Text(if(i==6)"Rp 1,25 jt" else "Rp ${450+i*90} rb",Modifier.width(65.dp),fontSize=8.sp,fontWeight=FontWeight.Bold)}}}}};item{Row(horizontalArrangement=Arrangement.spacedBy(9.dp)){MMetric("Omzet","Rp 8,42 jt","bulan ini","Rp",Modifier.weight(1f));MMetric("Rata-rata","Rp 52.900","per transaksi","↗",Modifier.weight(1f));MMetric("Terlaris","Kopi Susu","42 terjual","★",Modifier.weight(1f))}}}}}
@Composable private fun MSettings(){MScreen("Pengaturan","Konfigurasi toko, QRIS, struk, printer, dan hak akses"){LazyColumn(verticalArrangement=Arrangement.spacedBy(9.dp)){item{MSetting("Toko & Cabang","Toko Demo","Toko Utama • aktif")};item{MSetting("QRIS","QRIS Statis","Merchant aktif • siap digunakan kasir")};item{MSetting("Struk","58 mm","Logo, alamat, footer, format kertas")};item{MSetting("Printer","Belum dipasangkan","Bluetooth / LAN / USB")};item{MSetting("Role & Akses","Owner","Hak akses penuh • demo mode")};item{MSetting("Koneksi","Supabase","Connected • publishable key")}}}}
@Composable private fun MSetting(t:String,v:String,d:String){Card(Modifier.fillMaxWidth(),RoundedCornerShape(14.dp),colors=CardDefaults.cardColors(Color.White)){Row(Modifier.padding(14.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(t,fontSize=11.sp,fontWeight=FontWeight.ExtraBold);Text(v,color=MBrand,fontSize=11.sp,fontWeight=FontWeight.Bold,modifier=Modifier.padding(top=3.dp));Text(d,color=MMuted,fontSize=8.sp)}};Text("›",color=MMuted,fontSize=20.sp)}}
