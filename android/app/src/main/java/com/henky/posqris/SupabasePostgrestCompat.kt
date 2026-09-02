package com.henky.posqris

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.Postgrest

/**
 * Local bridge for supabase-kt 3.x so MainActivity can access the installed Postgrest plugin
 * without depending on the package-level extension import.
 */
private val SupabaseClient.posgrestPlugin: Postgrest
    get() = pluginManager.getPlugin(Postgrest)

val SupabaseClient.postgrest: Postgrest
    get() = posgrestPlugin
