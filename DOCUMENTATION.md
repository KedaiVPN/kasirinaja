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
3. **Java Manager** (Atau instal JDK Manual via Terminal, lihat catatan di bawah).
4. **FTP Server** / **Pure-Ftpd** (Opsional, untuk upload dari HP).

### ⚠️ PERHATIAN: Masalah Java Manager Versi Lama (v2.4.1)
Beberapa versi aaPanel (terutama yang gratis/versi lama) memiliki **Java Manager (v2.4.1) yang usang dan HANYA mentok mendukung JDK 1.8 (Java 8)**.
Sedangkan aplikasi kita membutuhkan minimal **Java 21**. Jika Anda memaksakan file `.jar` dijalankan di Java 8 melalui Java Manager ini, aplikasi akan diam-diam *crash* ("Stopped").

**Solusi Jitu:** Anda TIDAK BISA menggunakan Java Manager untuk mengeksekusi (run) file `.jar`. Anda harus meng-install Java 21 secara manual via Terminal/SSH, dan mendeploy file `.jar` menggunakan Systemd. Berikut panduan lengkapnya.

---

## 3. Deployment Backend Menggunakan Terminal & Systemd (Alternatif Java Manager)

Langkah ini dilakukan jika Java Manager di aaPanel Anda mentok di Java 8. Anda dapat menggunakan aplikasi SSH di HP (seperti Termius, JuiceSSH, atau menggunakan menu "Terminal" bawaan di dalam aaPanel).

### Langkah 1: Install Java 21 (JDK 21) via Terminal
Buka Terminal aaPanel atau aplikasi SSH Anda, jalankan perintah ini untuk mengunduh dan menginstal Java 21:
```bash
sudo apt update
sudo apt install openjdk-21-jdk -y
```
*(Catatan: Jika OS Anda CentOS/AlmaLinux, gunakan perintah `sudo yum install java-21-openjdk-devel -y`)*

Cek apakah Java 21 sudah terpasang dengan benar:
```bash
java -version
```
(Pesan yang keluar harus menunjukkan "openjdk version 21...").

### Langkah 2: Upload File `.jar` & Konfigurasi (Via GUI aaPanel)
1. **Upload File `.jar`**:
   - Buka menu **Files** di aaPanel.
   - Buat folder `/www/wwwroot/kasir-api`.
   - Upload file **`.jar`** ke dalam folder `/www/wwwroot/kasir-api`.
2. **Setup Database**:
   - Pergi ke menu **Databases -> PgSQL**.
   - Klik *Add Database*, buat database bernama `kasirinaja`, buat user dan passwordnya.
3. **Konfigurasi Database (application.yml)**:
   - Di menu **Files**, buat file baru bernama `application.yml` persis di sebelah file `.jar` Anda.
   - Isi dengan:
     ```yaml
     spring:
       datasource:
         url: jdbc:postgresql://localhost:5432/kasirinaja
         username: username_postgres_anda
         password: password_postgres_anda
     ```

### Langkah 3: Membuat Systemd Service (Via Terminal)
Kembali ke Terminal. Kita akan membuat *service* agar Spring Boot otomatis berjalan di background dan otomatis *restart* jika server mati.

Jalankan perintah ini:
```bash
sudo sh -c 'cat << "SYSTEMD" > /etc/systemd/system/kasir-api.service
[Unit]
Description=KasirinAja Spring Boot API
After=syslog.target network.target

[Service]
User=root
WorkingDirectory=/www/wwwroot/kasir-api
ExecStart=/usr/bin/java -jar /www/wwwroot/kasir-api/kasir-api-0.0.1-SNAPSHOT.jar
SuccessExitStatus=143
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
SYSTEMD'
```
*(Penting: Ganti tulisan `kasir-api-0.0.1-SNAPSHOT.jar` dengan nama asli file `.jar` yang Anda upload).*

### Langkah 4: Jalankan Service
Di terminal, jalankan urutan perintah ini:
```bash
sudo systemctl daemon-reload
sudo systemctl enable kasir-api.service
sudo systemctl start kasir-api.service
```

Untuk melihat apakah aplikasi berhasil berjalan (atau mengecek error), gunakan perintah ini:
```bash
sudo journalctl -u kasir-api.service -f
```

### Langkah 5: Setup Reverse Proxy Nginx & SSL (Via GUI aaPanel)
Karena kita tidak memakai Java Manager, kita buat websitenya manual.
1. Pergi ke menu **Website** -> tab **PHP Project** di aaPanel.
2. Klik **Add site**.
   - Isi **Domain** dengan domain API Anda (Misal: `api.domainkamu.com`).
   - Biarkan PHP version di "Pure Static" (karena kita akan mem-bypassnya).
3. Setelah website terbuat, klik nama domainnya.
4. Pergi ke menu **Reverse proxy** -> **Add reverse proxy**.
   - Target URL: `http://127.0.0.1:8080`.
   - Simpan.
5. Pergi ke menu **SSL**, dan klik "Let's Encrypt" untuk mendapatkan sertifikat HTTPS gratis.

Selesai! API Anda sudah berjalan menggunakan Java 21 murni tanpa keterbatasan Java Manager aaPanel.
