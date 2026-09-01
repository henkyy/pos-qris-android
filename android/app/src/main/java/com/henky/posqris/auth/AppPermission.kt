package com.henky.posqris.auth

/**
 * Satu sumber daftar permission yang dipakai Android.
 * Kode permission harus identik dengan data permission di Supabase.
 */
object AppPermission {
    const val USERS_MANAGE = "users.manage"
    const val BRANCHES_MANAGE = "branches.manage"
    const val SETTINGS_MANAGE = "settings.manage"
    const val QRIS_MANAGE = "qris.manage"
    const val PRINTER_MANAGE = "printer.manage"
}

fun AuthorizationState.can(code: String): Boolean = role == Role.OWNER || code in permissions
