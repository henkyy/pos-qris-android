package com.henky.posqris.auth

enum class Permission(val code: String) {
    PRODUCTS_VIEW("products.view"), PRODUCTS_CREATE("products.create"), PRODUCTS_UPDATE("products.update"), PRODUCTS_DELETE("products.delete"),
    SALES_CREATE("sales.create"), SALES_VOID("sales.void"), SALES_RETURN("sales.return"),
    INVENTORY_VIEW("inventory.view"), INVENTORY_ADJUST("inventory.adjust"), INVENTORY_TRANSFER("inventory.transfer"),
    PAYMENTS_CREATE("payments.create"), PAYMENTS_REFUND("payments.refund"), PAYMENTS_RECONCILE("payments.reconcile"),
    USERS_VIEW("users.view"), USERS_CREATE("users.create"), USERS_UPDATE_ROLE("users.update_role"), USERS_DISABLE("users.disable"),
    SETTINGS_VIEW("settings.view"), SETTINGS_UPDATE("settings.update")
}

enum class Role { OWNER, MANAGER, CASHIER, WAREHOUSE, SALES }

data class AuthorizationState(
    val role: Role,
    val permissions: Set<String>,
    val branchIds: Set<String> = emptySet()
) {
    fun can(permission: Permission): Boolean = role == Role.OWNER || permission.code in permissions
}
