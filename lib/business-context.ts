'use client'

import { requireSupabase } from './supabase'

export type ActiveWorkspace = {
  business: Record<string, any>
  branch: Record<string, any>
}

export async function getActiveWorkspace(): Promise<ActiveWorkspace> {
  const db = requireSupabase()

  const { data: memberships, error: membershipError } = await db
    .from('business_users')
    .select('business_id')
    .eq('is_active', true)
    .limit(1)

  if (membershipError) throw membershipError

  const businessId = memberships?.[0]?.business_id
  if (!businessId) {
    throw new Error('Business aktif tidak ditemukan. Akun belum memiliki akses bisnis.')
  }

  const { data: business, error: businessError } = await db
    .from('businesses')
    .select('*')
    .eq('id', businessId)
    .eq('is_active', true)
    .single()

  if (businessError) throw businessError
  if (!business) throw new Error('Business aktif tidak ditemukan atau sudah tidak aktif.')

  const { data: accesses, error: accessError } = await db
    .from('user_branch_access')
    .select('branch_id')
    .limit(1)

  if (accessError) throw accessError

  const branchId = accesses?.[0]?.branch_id
  if (!branchId) throw new Error('Cabang aktif tidak ditemukan. Akun belum memiliki akses cabang.')

  const { data: branch, error: branchError } = await db
    .from('branches')
    .select('*')
    .eq('id', branchId)
    .eq('business_id', business.id)
    .eq('is_active', true)
    .single()

  if (branchError) throw branchError
  if (!branch) throw new Error('Cabang aktif tidak ditemukan atau sudah tidak aktif.')

  return { business, branch }
}
