package com.henky.posqris.auth

/** Server-authoritative authorization contract.
 * Implementations must load role, permissions and branch access from Supabase.
 * Never derive privileged access from mutable client metadata.
 */
interface AuthorizationRepository {
    suspend fun load(userId: String): AuthorizationState
}

class AuthorizationManager(private val repository: AuthorizationRepository) {
    private var state: AuthorizationState? = null

    suspend fun refresh(userId: String): AuthorizationState {
        return repository.load(userId).also { state = it }
    }

    fun can(permission: Permission): Boolean = state?.can(permission) == true

    fun canAccessBranch(branchId: String): Boolean = state?.branchIds?.contains(branchId) == true

    fun current(): AuthorizationState? = state
}
