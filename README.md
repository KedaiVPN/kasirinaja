# KASIRINAJA POS System

KASIRINAJA adalah sistem aplikasi kasir/POS (Point of Sales) untuk toko kecil dan menengah yang dirancang dengan konsep **Offline-first** untuk aplikasi kasir, serta menggunakan arsitektur tersentralisasi untuk katalog produk.

Proyek ini terdiri dari beberapa komponen utama:
1. **Aplikasi Toko (Android)**: Digunakan oleh pemilik toko dan kasir. Memiliki fitur transaksi offline menggunakan Room Database.
2. **Aplikasi Admin (Android)**: Digunakan oleh admin untuk mengelola katalog pusat.
3. **Backend API (Spring Boot)**: Menyediakan layanan sentralisasi data dan sinkronisasi antara perangkat lokal dan server.

---

## 🛠 Tech Stack

### Backend
- **Framework:** Spring Boot (Kotlin)
- **Database:** PostgreSQL
- **Migration:** Flyway
- **Authentication:** JWT (JSON Web Tokens) & Spring Security
- **Email Service:** Spring Mail (SMTP)
- **Deployment:** VPS Linux (dengan aaPanel / Nginx / Systemd)

### Android Frontend
- **Language:** Kotlin
- **UI Toolkit:** Jetpack Compose
- **Architecture:** Clean Architecture + MVVM (Multi-module)
- **Local Database:** Room SQLite
- **Networking:** Retrofit2, OkHttp3
- **Background Tasks:** WorkManager (untuk Sinkronisasi Offline-to-Online)
- **Barcode Scanner:** CameraX + ML Kit Barcode Scanning

---

## 📂 Struktur Proyek

Proyek ini menggunakan satu *monorepo* yang menampung API dan Aplikasi Android.

```text
.
├── kasir-api/                  # Backend Spring Boot Kotlin
│   ├── src/main/kotlin/...     # Source code (Auth, Catalog, Store, Sync, dll.)
│   └── src/main/resources/     # application.yml dan file migrasi db (Flyway)
│
├── kasir-android/              # Android Multi-module Project
│   ├── core/                   # Modul yang dipakai bersama (Retrofit, TokenManager)
│   ├── app-store/              # Aplikasi Toko (Fitur Kasir Offline & Sync)
│   └── app-admin/              # Aplikasi Admin (Manajemen Katalog Master)
│
├── README.md                   # Penjelasan Proyek Utama
├── DOCUMENTATION.md            # Dokumentasi Build & Deploy (Beginner Friendly)
└── nginx.conf.example          # Contoh konfigurasi Reverse Proxy Nginx
```

---

## 🚀 Fitur Utama

- **Katalog Terpusat:** Admin mengatur katalog master (Barcode, Nama, Foto). Pemilik toko tinggal mengunduh produk ke katalog lokal mereka dan menentukan harga jual/beli sendiri.
- **Offline-First Cashier:** Kasir dapat melakukan pemindaian barcode dan menyelesaikan transaksi meskipun tidak ada koneksi internet. Data disimpan di Room SQLite.
- **Auto-Sync:** Ketika internet kembali tersedia, WorkManager akan menjalankan *SyncService* untuk mendorong (push) data transaksi tertunda ke server PostgreSQL.
- **Keamanan Lanjutan:** Endpoint API dilindungi menggunakan token JWT berbasis role (`SUPER_ADMIN`, `ADMIN`, `STORE_OWNER`, `CASHIER`).
