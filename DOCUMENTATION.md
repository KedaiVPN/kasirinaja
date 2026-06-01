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
Untuk mendeploy *backend* Java (Spring Boot), **Anda TIDAK PERLU mengunggah seluruh *source code* (folder `kasir-api`) ke VPS Anda**. Sama seperti React/Vue yang di-*build* menjadi folder `dist`, Spring Boot di-*build* menjadi satu buah file berekstensi `.jar`. **Hanya file `.jar` ini saja yang perlu diunggah ke VPS.**

1. Di menu/tab **Actions** di GitHub, pilih workflow **"Build Backend JAR"**.
2. Klik tombol **"Run workflow"**.
3. Setelah selesai (centang hijau), klik hasil workflow-nya.
4. Scroll ke bagian **Artifacts**, klik **kasir-api-jar** untuk mengunduhnya ke HP.
5. Ekstrak file .zip yang Anda unduh, di dalamnya akan ada file berakhiran `.jar` (misalnya: `kasir-api-0.0.1-SNAPSHOT.jar`).

---

## 2. Persiapan Deployment Server VPS Menggunakan aaPanel

Setelah mendapatkan file `.jar` dari langkah 1.B, saatnya menyebarkannya (*deploy*) di VPS Anda.

### Plugin yang harus di-install di aaPanel:
1. **Nginx** (Web Server & Reverse Proxy).
2. **PostgreSQL** (Database).
3. **Java / Tomcat** (Untuk menjalankan file `.jar`).
4. **FTP Server** / **Pure-Ftpd** (Opsional, untuk upload dari HP).

### Langkah Deploy via aaPanel (Tanpa Terminal):
Berbeda dengan web React/Vue yang mengarahkan path ke folder `dist`, pada Java Spring Boot, kita akan menjalankan aplikasinya sebagai **Java Project**, bukan PHP/Static HTML project.

1. **Upload File `.jar`**:
   - Buka menu **Files** di aaPanel.
   - Buat folder baru, misalnya `/www/wwwroot/kasir-api`.
   - Upload file **`.jar`** (hasil ekstrak dari GitHub Actions) ke dalam folder `/www/wwwroot/kasir-api` tersebut.
   - *(Ingat: Folder `kasir-api` dari GitHub JANGAN di-upload ke sini).*
2. **Setup Database**:
   - Pergi ke menu **Databases -> PgSQL**.
   - Klik *Add Database*, masukkan nama `kasirinaja`, lalu buat user dan passwordnya.
3. **Konfigurasi Database (application.yml)**:
   - Pergi ke menu **Files** aaPanel, masuk ke folder `/www/wwwroot/kasir-api`.
   - Buat file baru bernama `application.yml` persis di sebelah/satu folder dengan file `.jar` Anda.
   - Edit `application.yml` dan isi dengan konfigurasi berikut:
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
   - **Project jar/war path**: Klik ikon folder, lalu arahkan **langsung ke file `.jar`** yang Anda upload tadi (Contoh: `/www/wwwroot/kasir-api/kasir-api-0.0.1-SNAPSHOT.jar`).
   - **Project port**: Isi dengan `8080`.
   - Klik submit dan pastikan statusnya berubah menjadi "Running".
5. **Setup Reverse Proxy Nginx & SSL**:
   - Masih di pengaturan domain Java Project Anda (Atau di menu Website -> PHP Project), klik nama domain Anda (misal: `api.domainkamu.com`).
   - Pergi ke menu **Reverse proxy**, tambahkan proksi ke `http://127.0.0.1:8080`.
   - Pergi ke menu **SSL**, dan klik "Let's Encrypt" untuk mendapatkan sertifikat HTTPS gratis.

Selamat! API Anda sekarang sudah berjalan dan dapat diakses dari aplikasi Android Toko Anda.
