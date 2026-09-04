'use client'

import { useCallback, useEffect, useMemo, useState } from 'react'
import Button from '../../components/ui/Button'
import Modal from '../../components/ui/Modal'
import { getActiveWorkspace } from '../../lib/business-context'
import { requireSupabase } from '../../lib/supabase'
import styles from './ProductWorkspace.module.css'

type Option = { id: string; name: string }
type UnitRow = { id?: string; unit_id: string; conversion_to_base: number; is_purchase_unit: boolean; is_sales_unit: boolean; name?: string; symbol?: string }
type PriceRow = { id?: string; unit_id: string; price: number; min_qty: number; discount_percent: number; valid_from?: string | null; valid_until?: string | null; unit_name?: string }
type Product = { id:string; sku:string; name:string; barcode:string|null; category_id:string|null; base_unit_id:string; current_cost:number; last_purchase_cost:number; min_stock:number; reorder_point:number; is_active:boolean; category_name?:string; base_unit_name?:string; units?:UnitRow[]; prices?:PriceRow[] }

const money = (n:number) => `Rp ${new Intl.NumberFormat('id-ID').format(Number(n)||0)}`
const num = (n:number) => new Intl.NumberFormat('id-ID').format(Number(n)||0)

export default function ProductWorkspace(){
 const db=useMemo(()=>requireSupabase(),[])
 const [products,setProducts]=useState<Product[]>([]),[categories,setCategories]=useState<Option[]>([]),[units,setUnits]=useState<Option[]>([])
 const [loading,setLoading]=useState(true),[error,setError]=useState(''),[search,setSearch]=useState(''),[open,setOpen]=useState(false),[editing,setEditing]=useState<Product|null>(null),[saving,setSaving]=useState(false)
 const [form,setForm]=useState<any>({name:'',sku:'',barcode:'',category_id:'',base_unit_id:'',current_cost:'0',min_stock:'0',reorder_point:'0'})
 const [unitRows,setUnitRows]=useState<UnitRow[]>([]),[priceRows,setPriceRows]=useState<PriceRow[]>([])
 const [defaultPriceList,setDefaultPriceList]=useState<string>('')

 const load=useCallback(async()=>{setLoading(true);setError('');try{
  const ws=await getActiveWorkspace();
  const [{data:p,error:pe},{data:c,error:ce},{data:u,error:ue},{data:pl,error:ple}]=await Promise.all([
   db.from('products').select('*').eq('business_id',ws.business.id).order('name').limit(500),
   db.from('categories').select('id,name').eq('business_id',ws.business.id).eq('is_active',true).order('name'),
   db.from('units').select('id,name,symbol').eq('business_id',ws.business.id).order('name'),
   db.from('price_lists').select('id').eq('business_id',ws.business.id).eq('is_default',true).eq('is_active',true).limit(1)
  ]); if(pe)throw pe;if(ce)throw ce;if(ue)throw ue;if(ple)throw ple;
  const listId=pl?.[0]?.id||'';setDefaultPriceList(listId);const ids=(p||[]).map((x:any)=>x.id)
  const [{data:pu,error:pue},{data:pp,error:ppe}]=await Promise.all([
   ids.length?db.from('product_units').select('id,product_id,unit_id,conversion_to_base,is_purchase_unit,is_sales_unit').in('product_id',ids):Promise.resolve({data:[],error:null}),
   ids.length&&listId?db.from('product_prices').select('id,product_id,unit_id,price,min_qty,discount_percent,valid_from,valid_until').eq('price_list_id',listId).in('product_id',ids):Promise.resolve({data:[],error:null})
  ]);if(pue)throw pue;if(ppe)throw ppe;
  const um=new Map((u||[]).map((x:any)=>[x.id,x]));const cm=new Map((c||[]).map((x:any)=>[x.id,x.name]));
  const unitsBy=new Map<string,UnitRow[]>(),pricesBy=new Map<string,PriceRow[]>();
  for(const x of pu||[]){const a=unitsBy.get(x.product_id)||[];a.push({...x,name:um.get(x.unit_id)?.name||'?',symbol:um.get(x.unit_id)?.symbol});unitsBy.set(x.product_id,a)}
  for(const x of pp||[]){const a=pricesBy.get(x.product_id)||[];a.push({...x,unit_name:um.get(x.unit_id)?.name||'?'});pricesBy.set(x.product_id,a)}
  setProducts((p||[]).map((x:any)=>({...x,category_name:cm.get(x.category_id)||'-',base_unit_name:um.get(x.base_unit_id)?.name||'-',units:unitsBy.get(x.id)||[],prices:pricesBy.get(x.id)||[]})))
  setCategories(c||[]);setUnits((u||[]).map((x:any)=>({id:x.id,name:x.symbol?`${x.name} (${x.symbol})`:x.name})))
 }catch(e:any){setError(e?.message||'Gagal memuat katalog produk.')}finally{setLoading(false)}},[db])
 useEffect(()=>{load()},[load])
 const filtered=useMemo(()=>products.filter(p=>`${p.name} ${p.sku} ${p.barcode||''} ${p.category_name||''}`.toLowerCase().includes(search.toLowerCase())),[products,search])
 function startCreate(){setEditing(null);setForm({name:'',sku:'',barcode:'',category_id:'',base_unit_id:'',current_cost:'0',min_stock:'0',reorder_point:'0'});setUnitRows([]);setPriceRows([]);setError('');setOpen(true)}
 function startEdit(p:Product){setEditing(p);setForm({name:p.name,sku:p.sku,barcode:p.barcode||'',category_id:p.category_id||'',base_unit_id:p.base_unit_id,current_cost:String(p.current_cost||0),min_stock:String(p.min_stock||0),reorder_point:String(p.reorder_point||0)});setUnitRows((p.units||[]).map(x=>({...x})));setPriceRows((p.prices||[]).map(x=>({...x})));setError('');setOpen(true)}
 function setUnit(i:number,key:string,value:any){setUnitRows(a=>a.map((x,j)=>j===i?{...x,[key]:value}:x))}
 function addUnit(){if(unitRows.length>=3)return;setUnitRows(a=>[...a,{unit_id:'',conversion_to_base:1,is_purchase_unit:a.length===0,is_sales_unit:a.length===0}])}
 function addPrice(){setPriceRows(a=>[...a,{unit_id:form.base_unit_id||'',price:0,min_qty:1,discount_percent:0,valid_from:null,valid_until:null}])}
 async function save(){
  if(!form.name?.trim()||!form.sku?.trim()||!form.base_unit_id){setError('Nama, SKU dan satuan dasar wajib diisi.');return}
  const validUnits=unitRows.filter(x=>x.unit_id);if(!validUnits.some(x=>x.unit_id===form.base_unit_id)){validUnits.unshift({unit_id:form.base_unit_id,conversion_to_base:1,is_purchase_unit:true,is_sales_unit:true})}
  if(validUnits.some(x=>Number(x.conversion_to_base)<=0)){setError('Konversi satuan harus lebih besar dari 0.');return}
  const duplicate=new Set(validUnits.map(x=>x.unit_id));if(duplicate.size!==validUnits.length){setError('Satuan yang sama tidak boleh dipasang dua kali.');return}
  if(!defaultPriceList){setError('Daftar harga default belum tersedia. Buat price list default terlebih dahulu.');return}
  setSaving(true);setError('');try{const ws=await getActiveWorkspace();let productId=editing?.id;
   const payload={name:form.name.trim(),sku:form.sku.trim(),barcode:form.barcode?.trim()||null,category_id:form.category_id||null,base_unit_id:form.base_unit_id,product_type:'GOODS',track_batch:false,track_expiry:false,min_stock:Number(form.min_stock||0),reorder_point:Number(form.reorder_point||0),cost_method:'MOVING_AVERAGE',last_purchase_cost:editing?.last_purchase_cost||0,current_cost:Number(form.current_cost||0),is_active:editing?.is_active??true}
   if(productId){const r=await db.from('products').update(payload).eq('id',productId).eq('business_id',ws.business.id);if(r.error)throw r.error}else{const r=await db.from('products').insert({...payload,business_id:ws.business.id}).select('id').single();if(r.error)throw r.error;productId=r.data.id}
   if(!productId)throw new Error('Produk tidak memiliki ID.');
   await db.from('product_units').delete().eq('product_id',productId);const ur=await db.from('product_units').insert(validUnits.map(x=>({product_id:productId,unit_id:x.unit_id,conversion_to_base:Number(x.conversion_to_base),is_purchase_unit:!!x.is_purchase_unit,is_sales_unit:!!x.is_sales_unit})));if(ur.error)throw ur.error
   const existing=await db.from('product_prices').select('id').eq('price_list_id',defaultPriceList).eq('product_id',productId);if(existing.error)throw existing.error; if((existing.data||[]).length) {const dr=await db.from('product_prices').delete().eq('price_list_id',defaultPriceList).eq('product_id',productId);if(dr.error)throw dr.error}
   const prices=priceRows.filter(x=>x.unit_id&&Number(x.price)>=0).map(x=>({price_list_id:defaultPriceList,product_id:productId,unit_id:x.unit_id,min_qty:Number(x.min_qty||1),price:Math.round(Number(x.price||0)),discount_percent:Number(x.discount_percent||0),valid_from:x.valid_from||new Date().toISOString(),valid_until:x.valid_until||null}));
   if(prices.length){const pr=await db.from('product_prices').insert(prices);if(pr.error)throw pr.error}
   setOpen(false);await load()
  }catch(e:any){setError(e?.message||'Gagal menyimpan produk.')}finally{setSaving(false)}
 }
 return <div className="module-page"><div className={styles.hero}><div><span className="eyebrow">MASTER · PRODUK</span><h1>Produk</h1><p>Katalog lengkap: identitas, HPP, harga jual, satuan, konversi dan batas stok.</p></div><Button onClick={startCreate}>+ Tambah Produk</Button></div>
  <div className={styles.summary}><div><span>Total produk</span><strong>{products.length}</strong></div><div><span>Harga terkonfigurasi</span><strong>{products.filter(x=>(x.prices||[]).length).length}</strong></div><div><span>Satuan multi-unit</span><strong>{products.filter(x=>(x.units||[]).length>1).length}</strong></div><div><span>HPP saat ini</span><strong>{money(products.reduce((a,x)=>a+Number(x.current_cost||0),0))}</strong><small>Akumulasi per-unit, bukan nilai persediaan</small></div></div>
  {error&&<div className="module-alert">{error}</div>}<section className="module-card"><div className={styles.toolbar}><div><strong>Daftar Produk</strong><span>{loading?'Memuat…':`${filtered.length} dari ${products.length}`}</span></div><input value={search} onChange={e=>setSearch(e.target.value)} placeholder="Cari nama, SKU, barcode…"/><Button variant="secondary" onClick={load} disabled={loading}>Muat ulang</Button></div>
   {loading?<div className={styles.empty}>Memuat katalog…</div>:<div className="ui-table-wrap"><table className="ui-table"><thead><tr><th>Produk</th><th>Kategori</th><th>Satuan dasar</th><th>HPP / unit</th><th>Harga jual</th><th>Multi-satuan</th><th>Status</th><th>Aksi</th></tr></thead><tbody>{filtered.map(p=><tr key={p.id}><td><strong>{p.name}</strong><small>{p.sku}{p.barcode?` · ${p.barcode}`:''}</small></td><td>{p.category_name}</td><td>{p.base_unit_name}</td><td>{money(p.current_cost)}</td><td>{p.prices?.length?p.prices.map(x=>`${x.unit_name}: ${money(x.price)}`).join(' · '):<span className={styles.missing}>Belum diatur</span>}</td><td>{p.units?.length||0} satuan</td><td><span className={p.is_active?styles.active:styles.inactive}>{p.is_active?'Aktif':'Nonaktif'}</span></td><td><button className="table-edit" onClick={()=>startEdit(p)}>Edit</button></td></tr>)}</tbody></table></div>}
  </section>
  <Modal open={open} title={editing?`Edit Produk · ${editing.name}`:'Tambah Produk'} onClose={()=>setOpen(false)}><div className={styles.form}><div className={styles.section}><h3>Identitas produk</h3><div className={styles.grid}><label>Nama *<input value={form.name} onChange={e=>setForm({...form,name:e.target.value})}/></label><label>SKU *<input value={form.sku} onChange={e=>setForm({...form,sku:e.target.value})}/></label><label>Barcode<input value={form.barcode} onChange={e=>setForm({...form,barcode:e.target.value})}/></label><label>Kategori<select value={form.category_id} onChange={e=>setForm({...form,category_id:e.target.value})}><option value="">Tanpa kategori</option>{categories.map(x=><option key={x.id} value={x.id}>{x.name}</option>)}</select></label></div></div>
   <div className={styles.section}><h3>HPP & stok</h3><p>HPP saat ini adalah biaya per unit dari metode Moving Average. Penerimaan pembelian akan memperbaruinya secara otomatis.</p><div className={styles.grid}><label>HPP / unit<input type="number" min="0" value={form.current_cost} onChange={e=>setForm({...form,current_cost:e.target.value})}/></label><label>Minimum stok<input type="number" min="0" value={form.min_stock} onChange={e=>setForm({...form,min_stock:e.target.value})}/></label><label>Titik pesan ulang<input type="number" min="0" value={form.reorder_point} onChange={e=>setForm({...form,reorder_point:e.target.value})}/></label><div className={styles.readonly}><span>Metode HPP</span><strong>Moving Average</strong></div></div></div>
   <div className={styles.section}><div className={styles.sectionHead}><div><h3>Satuan & konversi</h3><p>Satuan dasar = 1. Satuan lain harus menyatakan berapa unit dasar yang terkandung di dalamnya.</p></div><Button variant="secondary" onClick={addUnit} disabled={unitRows.length>=3}>+ Satuan</Button></div>{unitRows.length===0?<div className={styles.hint}>Tambahkan sampai 3 satuan. Contoh: PCS = 1, BOX = 12, DUS = 144.</div>:unitRows.map((u,i)=><div className={styles.unitRow} key={i}><select value={u.unit_id} onChange={e=>setUnit(i,'unit_id',e.target.value)}><option value="">Pilih satuan</option>{units.map(x=><option key={x.id} value={x.id}>{x.name}</option>)}</select><input type="number" min="0.000001" step="any" value={u.conversion_to_base} disabled={u.unit_id===form.base_unit_id} onChange={e=>setUnit(i,'conversion_to_base',e.target.value)}/><label className={styles.check}><input type="checkbox" checked={u.is_purchase_unit} onChange={e=>setUnit(i,'is_purchase_unit',e.target.checked)}/> Pembelian</label><label className={styles.check}><input type="checkbox" checked={u.is_sales_unit} onChange={e=>setUnit(i,'is_sales_unit',e.target.checked)}/> Penjualan</label></div>)}</div>
   <div className={styles.section}><div className={styles.sectionHead}><div><h3>Harga jual</h3><p>Harga disimpan per satuan dan berlaku mulai tanggal yang ditentukan agar histori harga tidak berubah.</p></div><Button variant="secondary" onClick={addPrice} disabled={!form.base_unit_id}>+ Harga</Button></div>{priceRows.length===0?<div className={styles.hint}>Belum ada harga. Tambahkan harga jual minimal untuk satuan yang dijual.</div>:priceRows.map((x,i)=><div className={styles.priceRow} key={i}><select value={x.unit_id} onChange={e=>setPriceRows(a=>a.map((r,j)=>j===i?{...r,unit_id:e.target.value}:r))}><option value="">Pilih satuan</option>{unitRows.filter(u=>u.unit_id).map(u=><option key={u.unit_id} value={u.unit_id}>{units.find(z=>z.id===u.unit_id)?.name||'Satuan'}</option>)}</select><input type="number" min="0" value={x.price} onChange={e=>setPriceRows(a=>a.map((r,j)=>j===i?{...r,price:e.target.value}:r))} placeholder="Harga jual"/><input type="number" min="1" value={x.min_qty} onChange={e=>setPriceRows(a=>a.map((r,j)=>j===i?{...r,min_qty:e.target.value}:r))} placeholder="Min qty"/><button onClick={()=>setPriceRows(a=>a.filter((_,j)=>j!==i))}>Hapus</button></div>)}</div>
   <div className={styles.actions}><Button variant="secondary" onClick={()=>setOpen(false)} disabled={saving}>Batal</Button><Button onClick={save} disabled={saving}>{saving?'Menyimpan…':'Simpan produk'}</Button></div></div></Modal>
 </div>
}
