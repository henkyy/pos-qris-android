# Roadmap Implementasi POS QRIS

Dokumen ini menjadi checklist implementasi agar aplikasi siap dipakai toko retail dan distributor.

## Tahap 1: Fondasi
- [x] Android Gradle Wrapper
- [x] Kotlin + Jetpack Compose
- [x] Build dan unit test
- [x] Layout compact/tablet dasar
- [x] Navigation shell

## Tahap 2: Identitas tenant
- [ ] Supabase Auth
- [ ] Merchant/toko
- [ ] Cabang
- [ ] User profile
- [ ] Role dan permission server-side
- [ ] RLS per merchant/cabang

## Tahap 3: Operasional
- [ ] Produk/SKU
- [ ] Kategori
- [ ] Satuan
- [ ] Harga beli/jual
- [ ] Stok dan mutasi
- [ ] Supplier
- [ ] Pelanggan
- [ ] Penjualan
- [ ] Retur
- [ ] Pembelian

## Tahap 4: Pembayaran QRIS
- [ ] Konfigurasi QRIS per merchant
- [ ] Upload QRIS statis
- [ ] Validasi file dan penyimpanan aman
- [ ] Tampilkan QRIS aktif saat checkout
- [ ] Payment transaction
- [ ] Idempotency
- [ ] Rekonsiliasi/status PAID, FAILED, EXPIRED
- [ ] Audit pembayaran

## Tahap 5: Struk dan printer
- [ ] Pengaturan logo toko
- [ ] Header/footer
- [ ] Kertas 58/80 mm
- [ ] Bluetooth printer
- [ ] USB printer
- [ ] Test print
- [ ] Auto print setelah transaksi sukses

## Tahap 6: Produksi
- [ ] Error handling
- [ ] Offline-safe transaction strategy
- [ ] Audit log
- [ ] Permission enforcement di UI dan server
- [ ] Tablet QA
- [ ] Unit/instrumentation tests
- [ ] Release build

> QRIS statis selalu milik merchant yang sedang aktif. Jangan menanam QRIS merchant tertentu di APK atau source code.
