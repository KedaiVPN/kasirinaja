# Dokumentasi Build & Deploy (KASIRINAJA)

Dokumen ini adalah panduan lengkap untuk melakukan konfigurasi, *build*, dan proses *deploy* sistem KASIRINAJA baik untuk *backend* maupun aplikasi *Android*.

---

## 1. Build Proyek Melalui HP (GitHub Actions)

Membangun aplikasi Android (`.apk`) maupun backend Java (`.jar`) biasanya membutuhkan spesifikasi komputer atau laptop yang tinggi. Namun, Anda tidak perlu khawatir karena Anda bisa melakukan kompilasi proyek ini sepenuhnya melalui *Cloud* gratis dari GitHub Actions menggunakan HP.

### A. Build Aplikasi Android (`.apk`):
1. Buka repositori KASIRINAJA Anda di GitHub melalui browser HP.
2. (Opsional) Jika Anda perlu mengubah `BASE_URL` API agar mengarah ke domain VPS Anda:
   - Cari file `kasir-android/core/src/main/java/com/kasirinaja/core/network/RetrofitClient.kt`.
   - Klik ikon **Pensil** untuk mengedit file, ubah `"http://10.0.2.2:8080/api/"` menjadi `"https://api.domainkamu.com/api/"`.
   - Simpan (*Commit changes*).
3. Pindah ke menu/tab **Actions** di atas repositori GitHub Anda.
4. Di bilah kiri, pilih workflow **"Build Android APKs"** lalu klik tombol **"Run workflow"**.
5. Tunggu sekitar 5-10 menit. Setelah proses *Build* selesai dan centang hijau, klik hasilnya.
6. Scroll ke bawah pada bagian **Artifacts**. Anda akan melihat file `app-store-debug.apk` dan `app-admin-debug.apk`.
7. Klik untuk mengunduh, lalu instal di HP Anda.

### B. Build Backend API (`.jar`):
File `.jar` adalah aplikasi *backend* yang nantinya akan kita masukkan ke VPS agar toko bisa saling sinkron data. File `.jar` **TIDAK** otomatis terbuat saat Anda membuild `.apk`.
1. Di menu/tab **Actions**, pilih workflow **"Build Backend JAR"**.
2. Klik tombol **"Run workflow"**.
3. Setelah selesai (centang hijau), klik hasil workflow-nya.
4. Scroll ke bagian **Artifacts**, klik **kasir-api-jar** untuk mengunduhnya ke HP. (File ini nantinya akan Anda *upload* ke VPS / aaPanel).

---

## 2. Persiapan Deployment Server VPS Menggunakan aaPanel

Setelah mendapatkan file `.jar` dari langkah 1.B, saatnya menyebarkannya (*deploy*) di VPS Anda. Berikut daftar Plugin yang **WAJIB** Anda install dari menu **App Store** di dalam aaPanel:

### Plugin yang harus di-install di aaPanel:
1. **Nginx** (Versi berapa saja yang terbaru, rekomendasi Nginx 1.22+).
   - *Fungsi:* Sebagai web server untuk meneruskan internet (Reverse Proxy) ke API dan mengurus HTTPS.
2. **PostgreSQL** (Versi 14, 15, atau yang terbaru).
   - *Fungsi:* Menyimpan data transaksi dan katalog produk. Setelah install, buat database baru dengan nama `kasirinaja` dan buat *username/password* melalui menu "Databases" -> "PgSQL".
3. **Java / Tomcat** (Pilih Java 17 atau Java 21).
   - *Fungsi:* Menjalankan aplikasi backend (file `.jar`) yang Anda unduh dari GitHub. aaPanel menyediakan menu khusus "Java Project" untuk mempermudah.
4. **FTP Server** / **Pure-Ftpd** (Opsional).
   - *Fungsi:* Memudahkan Anda mengirim file `.jar` dan foto produk dari memori HP ke server VPS menggunakan aplikasi seperti AndFTP di HP.

### Langkah Deploy via aaPanel (Tanpa Terminal):
1. **Upload File `.jar`**: Buka menu **Files** di aaPanel. Buat folder baru, misalnya `/www/wwwroot/kasir-api`. Upload file JAR yang Anda dapatkan dari GitHub Actions (Langkah 1.B) ke dalam folder tersebut.
2. **Setup Database**: Pergi ke menu **Databases -> PgSQL**. Klik *Add Database*, masukkan nama `kasirinaja`, buat user dan passwordnya.
3. **Konfigurasi Backend**:
   - Karena Anda tidak menggunakan editor IDE, Anda harus mengatur rahasia *database* dari aaPanel.
   - Pergi ke menu **Files**, masuk ke folder tempat `.jar` Anda berada, lalu buat file baru bernama `application.yml` persis di sebelah file `.jar` tersebut.
   - Edit `application.yml` dan isi dengan konfigurasi database PostgreSQL yang baru saja Anda buat:
     ```yaml
     spring:
       datasource:
         url: jdbc:postgresql://localhost:5432/kasirinaja
         username: username_postgres_anda
         password: password_postgres_anda
     ```
4. **Jalankan Aplikasi Spring Boot**:
   - Pergi ke menu **Website**, klik tab **Java Project**.
   - Klik **Add Java project**.
   - **Project jar/war path**: Pilih file `.jar` yang baru Anda upload.
   - **Project port**: Isi dengan `8080`.
   - Centang opsi agar dia menggunakan `application.yml` eksternal jika dibutuhkan.
   - Klik submit dan pastikan statusnya "Running".
5. **Setup Reverse Proxy Nginx & SSL**:
   - Masih di pengaturan domain Java Project Anda (Atau di menu Website -> PHP Project), klik nama domain Anda (`api.domainkamu.com`).
   - Pergi ke menu **Reverse proxy**, tambahkan proksi ke `http://127.0.0.1:8080`.
   - Pergi ke menu **SSL**, dan klik "Let's Encrypt" untuk mendapatkan sertifikat HTTPS gratis.

*Anda tidak perlu masuk ke terminal/SSH dari HP untuk mengetikkan perintah apa pun secara manual, semua sudah dikelola oleh antarmuka visual (GUI) aaPanel.*
