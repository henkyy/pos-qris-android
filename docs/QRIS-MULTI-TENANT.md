# QRIS Multi-Tenant

QRIS di aplikasi POS bukan milik aplikasi. Setiap bisnis menyimpan konfigurasi QRIS miliknya sendiri.

## Prinsip

- QRIS statis diunggah oleh Owner.
- File gambar disimpan di Supabase Storage.
- Payload QRIS yang sudah divalidasi disimpan sebagai metadata konfigurasi.
- Konfigurasi terikat ke `business_id` dan `branch_id`.
- Kasir hanya dapat memakai QRIS aktif pada cabang yang menjadi aksesnya.
- Kasir tidak dapat mengganti konfigurasi QRIS.
- Tidak ada QRIS merchant tertentu yang ditanam di source code atau APK.

## Checkout

1. POS membuat penjualan berstatus `PENDING`.
2. Sistem mengambil konfigurasi QRIS aktif milik cabang.
3. Payload statis dapat digunakan sebagai sumber QR pembayaran sesuai kemampuan provider.
4. Status pembayaran tetap harus diverifikasi dari provider atau mekanisme pembayaran yang sah.
5. Pembayaran yang tervalidasi berubah menjadi `PAID`.
6. Transaksi `PAID` diproses menjadi `COMPLETED` dan stok dikurangi secara atomic.

## Keamanan

Semua akses konfigurasi QRIS dibatasi dengan RLS dan business/branch scope. Validasi perubahan konfigurasi dilakukan melalui fungsi yang hanya dapat dijalankan Owner.
