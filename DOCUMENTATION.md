# Dokumentasi Build & Deploy (KASIRINAJA)

Dokumen ini adalah panduan lengkap untuk melakukan konfigurasi, *build*, dan proses *deploy* sistem KASIRINAJA baik untuk *backend* maupun aplikasi *Android*.

---

## 1. Persiapan Backend (Spring Boot & PostgreSQL)

### Konfigurasi Backend:
Buka file konfigurasi di `kasir-api/src/main/resources/application.yml` (atau gunakan *Environment Variables* / `.env` untuk keamanan).
Pastikan Anda mengubah bagian berikut sesuai dengan *environment* Anda:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/kasirinaja
    username: postgres
    password: password_database_anda

  mail:
    username: email.anda@gmail.com
    password: app_password_email_anda

jwt:
  secret: GANTI_DENGAN_STRING_BASE64_YANG_PANJANG_DAN_AMAN
```

### Build APK (Untuk di-instal di HP):
1. Buka repositori KASIRINAJA Anda di GitHub melalui browser HP.
2. (Opsional) Jika Anda perlu mengubah `BASE_URL` API agar mengarah ke VPS Anda:
   - Cari file `kasir-android/core/src/main/java/com/kasirinaja/core/network/RetrofitClient.kt`.
   - Klik ikon **Pensil** untuk mengedit file, lalu ubah `"http://10.0.2.2:8080/api/"` menjadi `"https://api.domainkamu.com/api/"`.
   - Simpan (*Commit changes*).
3. Pindah ke menu **Actions** di repositori GitHub Anda.
4. Buat atau jalankan alur kerja (workflow) untuk Build Android. GitHub akan menggunakan *Virtual Machine* mereka untuk meng-compile kode sumber Anda.
5. Setelah *Build* selesai dan sukses (centang hijau), klik *workflow run* tersebut.
6. Scroll ke bawah pada bagian **Artifacts**. Di sana akan ada file `app-release.apk` atau `app-debug.apk` yang bisa Anda unduh langsung ke HP Anda dan di-install.

---

## 2. Persiapan Deployment Server VPS Menggunakan aaPanel

Karena backend proyek ini dibangun menggunakan Java Spring Boot (menjadi file `.jar`) dan database PostgreSQL, berikut adalah daftar Plugin yang **WAJIB** Anda install dari menu **App Store** di dalam aaPanel:

### Plugin yang harus di-install di aaPanel:
1. **Nginx** (Versi berapa saja yang terbaru, rekomendasi Nginx 1.22+).
   - *Fungsi:* Digunakan sebagai web server untuk menjalankan *Reverse Proxy* dan mengurus sertifikat SSL (HTTPS) agar aplikasi aman.
2. **PostgreSQL** (Versi 14, 15, atau yang terbaru).
   - *Fungsi:* Digunakan sebagai *Database* utama penyimpan data transaksi dan katalog produk. Setelah install, buat database baru dengan nama `kasirinaja` dan buat *username/password* melalui menu "Databases" -> "PgSQL".
3. **Java / Tomcat** (Pilih Java 17 atau Java 21).
   - *Fungsi:* Digunakan sebagai environment *Runtime* untuk menjalankan file `app.jar` dari Spring Boot API kita.
   - *Catatan:* aaPanel juga memiliki fitur **"Java Project"** di bawah menu **Website**. Anda bisa langsung men-deploy file JAR dari menu ini tanpa perlu mengatur `systemd` via terminal manual.
4. **FTP Server** / **Pure-Ftpd** (Opsional tapi direkomendasikan).
   - *Fungsi:* Membantu Anda untuk upload file `.jar` dan foto produk (dari lokal ke VPS) melalui aplikasi File Manager FTP dari HP (seperti AndFTP). Anda juga bisa langsung menggunakan menu "Files" bawaan aaPanel jika file-nya tidak terlalu besar.

### Langkah Singkat Deploy via aaPanel (Tanpa Terminal):
1. **Upload File `.jar`**: Buka menu **Files** di aaPanel. Buat folder baru misalnya `/www/wwwroot/kasir-api`. Upload file JAR hasil build ke dalam folder tersebut.
2. **Setup Database**: Pergi ke menu **Databases -> PgSQL**. Klik *Add Database*, masukkan nama `kasirinaja`, buat user dan passwordnya. Masukkan user/password ini ke `application.yml` Anda sebelum melakukan build `.jar`.
3. **Jalankan Aplikasi Spring Boot**:
   - Pergi ke menu **Website**, klik tab **Java Project**.
   - Klik **Add Java project**.
   - **Project jar/war path**: Pilih file `.jar` yang baru Anda upload di langkah 1.
   - **Project port**: Isi dengan `8080`.
   - Klik submit dan pastikan statusnya "Running".
4. **Setup Reverse Proxy Nginx & SSL**:
   - Masih di menu **Website**, masuk ke tab **PHP Project** (Atau tetap di setelan domain Java Project Anda jika sudah mengikat domain).
   - Klik nama domain Anda, pergi ke menu **Reverse proxy**, tambahkan proksi ke `http://127.0.0.1:8080`.
   - Pergi ke menu **SSL**, dan klik "Let's Encrypt" untuk mendapatkan sertifikat HTTPS gratis.

*Anda tidak perlu lagi masuk ke terminal/SSH HP untuk setup systemd secara manual, semua sudah dikelola oleh fitur "Java Project" milik aaPanel.*
