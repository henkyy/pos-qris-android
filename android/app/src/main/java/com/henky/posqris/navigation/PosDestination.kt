package com.henky.posqris.navigation

import com.henky.posqris.auth.AppPermission

sealed class PosDestination(
    val route: String,
    val title: String,
    val permission: String? = null
) {
    data object Dashboard : PosDestination("dashboard", "Dashboard")
    data object Pos : PosDestination("pos", "Penjualan")
    data object Products : PosDestination("products", "Produk", "products.view")
    data object Inventory : PosDestination("inventory", "Persediaan", "inventory.view")
    data object Customers : PosDestination("customers", "Pelanggan")
    data object Suppliers : PosDestination("suppliers", "Pemasok")
    data object Payments : PosDestination("payments", "Pembayaran", "payments.create")
    data object Reports : PosDestination("reports", "Laporan")
    data object Users : PosDestination("users", "Pengguna", AppPermission.USERS_MANAGE)
    data object Settings : PosDestination("settings", "Pengaturan", AppPermission.SETTINGS_MANAGE)
}
