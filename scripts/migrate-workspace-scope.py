from pathlib import Path
import re

sales = Path('app/sales-terminal.tsx')
s = sales.read_text()
if "../lib/business-context" not in s:
    s = s.replace("import { requireSupabase } from '../lib/supabase'", "import { requireSupabase } from '../lib/supabase'\nimport { getActiveWorkspace } from '../lib/business-context'")

pattern = r"  const load = useCallback\(async \(silent = false\) => \{.*?\n  \}, \[toast\]\)\n\n  const syncPending"
replacement = """  const load = useCallback(async (silent = false) => {
    if (!silent) setLoading(true)
    try {
      if (!navigator.onLine) throw new Error('OFFLINE_MODE')
      const db = requireSupabase()
      const { business, branch } = await getActiveWorkspace()
      const { data: locations, error: le } = await db.from('locations').select('*').eq('branch_id', branch.id).eq('is_active', true).order('name')
      if (le) throw le
      const locationIdNext = locations?.[0]?.id || ''
      if (!locationIdNext) throw new Error(`Lokasi stok aktif untuk cabang ${text(branch.name || branch.code || branch.id)} tidak ditemukan.`)
      const [{ data: ps, error: pe }, { data: cs, error: ce }, { data: us, error: ue }, { data: bs, error: se }, { data: cus, error: cue }, { data: methods, error: me }] = await Promise.all([
        db.from('products').select('*').eq('business_id', business.id).eq('is_active', true).order('name'),
        db.from('categories').select('*').eq('business_id', business.id).eq('is_active', true).order('name'),
        db.from('units').select('*').eq('business_id', business.id).order('name'),
        db.from('stock_balances').select('*').eq('location_id', locationIdNext),
        db.from('customers').select('*').eq('business_id', business.id).eq('is_active', true).order('name'),
        db.from('payment_methods').select('*').eq('business_id', business.id).eq('is_active', true).order('name'),
      ])
      if (pe || ce || ue || se || cue || me) throw pe || ce || ue || se || cue || me
      const { data: pls, error: ple } = await db.from('price_lists').select('*').eq('business_id', business.id).eq('is_default', true).eq('is_active', true).limit(1)
      if (ple) throw ple
      const pl = pls?.[0]
      const { data: prs, error: pre } = pl ? await db.from('product_prices').select('*').eq('price_list_id', pl.id) : { data: [], error: null }
      if (pre) throw pre
      const usable = (methods || []).filter(m => ['CASH','RECEIVABLE','PIUTANG','AR','QRIS','TRANSFER','BANK_TRANSFER'].includes(text(m.code).toUpperCase()) || ['CASH','RECEIVABLE','PIUTANG','QRIS','TRANSFER','BANK_TRANSFER'].includes(text(m.method_type).toUpperCase()))
      const cashMethod = usable.find(m => normalizePaymentCode(text(m.code || m.method_type)) === 'CASH')
      const defaultMethod = cashMethod || usable[0]
      setProducts(ps || []); setCategories(cs || []); setUnits(us || []); setStock(bs || []); setCustomers(cus || []); setPaymentMethods(usable); setBranchId(branch.id); setLocationId(locationIdNext); setPrices(prs || []); setPaymentMethodId(defaultMethod?.id || ''); setPaymentCode(normalizePaymentCode(text(defaultMethod?.code || defaultMethod?.method_type || 'CASH'))); setOnline(true)
      await saveCatalogCache({ products: ps || [], categories: cs || [], units: us || [], prices: prs || [], stock: bs || [], branchId: branch.id, locationId: locationIdNext, cashMethodId: cashMethod?.id || '', paymentMethods: usable, customers: cus || [] } as any)
    } catch (e: any) {
      const cached = await getCatalogCache().catch(() => null)
      if (cached) {
        setProducts(cached.products || []); setCategories(cached.categories || []); setUnits(cached.units || []); setPrices(cached.prices || []); setStock(cached.stock || []); setCustomers((cached as any).customers || []); setPaymentMethods((cached as any).paymentMethods || []); setBranchId(cached.branchId || ''); setLocationId(cached.locationId || '')
        const cashMethod = ((cached as any).paymentMethods || []).find((m: Row) => normalizePaymentCode(text(m.code || m.method_type)) === 'CASH')
        setPaymentMethodId(cashMethod?.id || cached.cashMethodId || ''); setPaymentCode('CASH'); setOnline(false)
        if (e?.message !== 'OFFLINE_MODE') toast('info', 'Mode offline', 'Server tidak dapat dihubungi. Katalog, stok, pelanggan, dan metode tersimpan lokal tetap digunakan.')
      } else toast('error', 'Gagal memuat data', e.message || 'Periksa koneksi Supabase.')
    } finally { if (!silent) setLoading(false) }
  }, [toast])

  const syncPending"""
ns, count = re.subn(pattern, replacement, s, flags=re.S)
if count != 1:
    raise SystemExit(f'Sales load replacement count={count}')
sales.write_text(ns)

purchase = Path('features/purchases/PurchaseWorkspace.tsx')
s = purchase.read_text()
if "../../lib/business-context" not in s:
    s = s.replace("import { requireSupabase } from '../../lib/supabase'", "import { requireSupabase } from '../../lib/supabase'\nimport { getActiveWorkspace } from '../../lib/business-context'")
pattern = r" const load=useCallback\(async\(\)=>\{.*?\n useEffect\(\(\)=>\{load\(\)\},\[load\]\)"
replacement = """ const load=useCallback(async()=>{setLoading(true);setError('');try{const db=requireSupabase();const {business,branch}=await getActiveWorkspace();const bid=business.id;const brid=branch.id
 const {data:locs,error:le}=await db.from('locations').select('id').eq('branch_id',brid).eq('is_active',true).order('name').limit(1);if(le)throw le;const lid=locs?.[0]?.id;if(!lid)throw new Error(`Lokasi stok aktif untuk cabang ${branch.name||branch.code||brid} tidak ditemukan.`)
 const [{data:sups,error:se},{data:prods,error:pe},{data:purchases,error:qe},{data:aps,error:ae}]=await Promise.all([
  db.from('suppliers').select('id,name,code,payment_term_days').eq('business_id',bid).eq('is_active',true).order('name'),db.from('products').select('id,name,sku,base_unit_id,current_cost,last_purchase_cost').eq('business_id',bid).eq('is_active',true).order('name').limit(500),db.from('purchase_orders').select('id,order_no,supplier_id,order_date,expected_date,status,subtotal,discount_amount,tax_amount,other_cost,total_amount,notes,suppliers(name)').eq('business_id',bid).eq('branch_id',brid).order('order_date',{ascending:false}).limit(100),db.from('payables').select('id,supplier_id,invoice_no,due_date,original_amount,paid_amount,outstanding_amount,status,suppliers(name)').eq('business_id',bid).eq('branch_id',brid).order('due_date',{ascending:true}).limit(100)
 ]);if(se)throw se;if(pe)throw pe;if(qe)throw qe;if(ae)throw ae;setBusinessId(bid);setBranchId(brid);setLocationId(lid);setSuppliers((sups||[]) as Supplier[]);setProducts((prods||[]) as Product[]);setRows((purchases||[]) as PurchaseRow[]);setPayables((aps||[]) as PayableRow[])
 }catch(e:any){setError(e?.message||'Gagal memuat modul pembelian.')}finally{setLoading(false)}},[])
 useEffect(()=>{load()},[load])"""
ns, count = re.subn(pattern, replacement, s, flags=re.S)
if count != 1:
    raise SystemExit(f'Purchase load replacement count={count}')
purchase.write_text(ns)
print('workspace scope migration applied')
