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

## 2. Deployment Server VPS (Murni via Terminal / Tanpa aaPanel)

Mengingat aaPanel terkadang memiliki bug integrasi Java dan PostgreSQL (seperti *shared memory error*), deploy secara manual via Terminal OS Ubuntu/Debian justru lebih stabil, hemat memori, dan mudah diprediksi.

Anda hanya perlu menggunakan aplikasi SSH di HP (seperti Termius atau JuiceSSH).

### Langkah 1: Install Java 21 (JDK)
Masuk ke terminal VPS Anda dan jalankan perintah:
```bash
sudo apt update
sudo apt install openjdk-21-jdk -y
```
Pastikan instalasi berhasil dengan mengecek versinya:
```bash
java -version
```

### Langkah 2: Install & Setup PostgreSQL
Mari kita install database PostgreSQL langsung di OS.
1. Install PostgreSQL:
   ```bash
   sudo apt install postgresql postgresql-contrib -y
   ```
2. Pastikan service berjalan otomatis:
   ```bash
   sudo systemctl enable postgresql
   sudo systemctl start postgresql
   ```
3. Masuk ke console PostgreSQL (`psql`):
   ```bash
   sudo -i -u postgres psql
   ```
4. Di dalam console PostgreSQL (ada tanda `postgres=#`), jalankan perintah SQL ini satu per satu (ganti passwordnya sesuai keinginan Anda):
   ```sql
   CREATE DATABASE kasirinaja;
   CREATE USER kasirinaja WITH ENCRYPTED PASSWORD 'bendakerep123';
   GRANT ALL PRIVILEGES ON DATABASE kasirinaja TO kasirinaja;
   \c kasirinaja
   GRANT ALL ON SCHEMA public TO kasirinaja;
   \q
   ```
*(Perintah `\q` digunakan untuk keluar dari console).*

### Langkah 3: Upload `.jar` dan Buat `application.yml`
1. Buat folder untuk aplikasi Anda:
   ```bash
   sudo mkdir -p /var/www/kasir-api
   sudo chown -R $USER:$USER /var/www/kasir-api
   ```
2. Upload file `.jar` (hasil unduhan dari GitHub Actions di Langkah 1.B) ke folder `/var/www/kasir-api/` menggunakan aplikasi SFTP/Termius.
3. Buat konfigurasi `application.yml` di terminal:
   ```bash
   cat << 'EOF' > /var/www/kasir-api/application.yml
   server:
     port: 8080

   spring:
     datasource:
       url: jdbc:postgresql://localhost:5432/kasirinaja
       username: kasirinaja
       password: bendakerep123
       driver-class-name: org.postgresql.Driver
     jpa:
       hibernate:
         ddl-auto: validate
   EOF
   ```

### Langkah 4: Membuat Systemd Service
Agar aplikasi Spring Boot terus menyala meskipun terminal Anda tutup, buat sebuah daemon service:

```bash
sudo sh -c 'cat << "SYSTEMD" > /etc/systemd/system/kasir-api.service
[Unit]
Description=KasirinAja Spring Boot API
After=syslog.target network.target postgresql.service

[Service]
User=root
WorkingDirectory=/var/www/kasir-api
ExecStart=/usr/bin/java -jar /var/www/kasir-api/kasir-api-0.0.1-SNAPSHOT.jar
SuccessExitStatus=143
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
SYSTEMD'
```
*(Catatan: Sesuaikan nama `kasir-api-0.0.1-SNAPSHOT.jar` dengan nama asli file Anda jika berbeda).*

Jalankan aplikasinya:
```bash
sudo systemctl daemon-reload
sudo systemctl enable kasir-api.service
sudo systemctl start kasir-api.service
```

Cek log-nya untuk memastikan Spring Boot menyala dan berhasil membuat tabel di PostgreSQL:
```bash
sudo journalctl -u kasir-api.service -f
```

### Langkah 5: Install Nginx & Reverse Proxy
Agar API bisa diakses publik (tanpa harus mengetik port 8080 di URL) dan mendukung HTTPS.

1. Install Nginx:
   ```bash
   sudo apt install nginx -y
   ```
2. Buat konfigurasi routing Nginx:
   ```bash
   sudo sh -c 'cat << "NGINX" > /etc/nginx/sites-available/kasirinaja
   server {
       listen 80;
       server_name api.domainkamu.com;

       location /uploads/ {
           alias /var/www/kasir-api/uploads/;
           autoindex off;
       }

       location / {
           proxy_pass http://127.0.0.1:8080;
           proxy_set_header Host $host;
           proxy_set_header X-Real-IP $remote_addr;
           proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
           proxy_set_header X-Forwarded-Proto $scheme;
       }
   }
   NGINX'
   ```
3. Aktifkan konfigurasi dan restart Nginx:
   ```bash
   sudo ln -s /etc/nginx/sites-available/kasirinaja /etc/nginx/sites-enabled/
   sudo nginx -t
   sudo systemctl restart nginx
   ```

*(Opsional: Gunakan `certbot` untuk menginstall SSL gratis dari Let's Encrypt dengan perintah: `sudo apt install certbot python3-certbot-nginx -y` lalu jalankan `sudo certbot --nginx -d api.domainkamu.com`)*.

Selesai! Seluruh arsitektur backend Anda kini berjalan di lingkungan yang bersih, *native*, dan stabil.
