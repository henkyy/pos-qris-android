'use client'

import { requireSupabase } from './supabase'

export type ActiveWorkspace = {
  business: Record<string, any>
  branch: Record<string, any>
}

const BRANCH_STORAGE_KEY = 'qris-active-branch-id'

export function getStoredBranchId(): string | null {
  try {
    return localStorage.getItem(BRANCH_STORAGE_KEY)
  } catch {
    return null
  }
}

export function setStoredBranchId(branchId: string) {
  try {
    localStorage.setItem(BRANCH_STORAGE_KEY, branchId)
  } catch {}
}

export function clearStoredBranchId() {
  try {
    localStorage.removeItem(BRANCH_STORAGE_KEY)
  } catch {}
}

export async function getAccessibleBranches(businessId?: string): Promise<Record<string, any>[]> {
  const db = requireSupabase()
  let business = businessId

  if (!business) {
    const { data: memberships, error } = await db
      .from('business_users')
      .select('business_id')
      .eq('is_active', true)
      .limit(1)
    if (error) throw error
    business = memberships?.[0]?.business_id
  }

  if (!business) return []

  const { data: accesses, error: accessError } = await db
    .from('user_branch_access')
    .select('branch_id')
  if (accessError) throw accessError

  const branchIds = (accesses || []).map((x: any) => x.branch_id).filter(Boolean)
  if (!branchIds.length) return []

  const { data: branches, error: branchError } = await db
    .from('branches')
    .select('*')
    .eq('business_id', business)
    .eq('is_active', true)
    .in('id', branchIds)
    .order('name')

  if (branchError) throw branchError
  return branches || []
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

  const branches = await getAccessibleBranches(business.id)
  if (!branches.length) {
    throw new Error('Cabang aktif tidak ditemukan. Akun belum memiliki akses cabang.')
  }

  const storedBranchId = getStoredBranchId()
  const branch = branches.find((x: any) => x.id === storedBranchId) || branches[0]

  if (branch.id !== storedBranchId) setStoredBranchId(branch.id)

  return { business, branch }
}
