# Dokumentasi Build & Deploy (KASIRINAJA)

Dokumen ini adalah panduan lengkap untuk melakukan konfigurasi, *build*, dan proses *deploy* sistem KASIRINAJA baik untuk *backend* maupun aplikasi *Android*.

---

## 1. Persiapan Backend (Spring Boot & PostgreSQL)

### Syarat (Prerequisites)
- Java JDK 21 terpasang.
- PostgreSQL terpasang dan berjalan (Lokal atau di VPS).
- Akses ke Terminal / Command Line.

### Langkah-langkah Setup Database:
1. Buka PostgreSQL (menggunakan pgAdmin atau psql CLI):
   ```bash
   psql -U postgres
   ```
2. Buat database untuk proyek ini:
   ```sql
   CREATE DATABASE kasirinaja;
   ```

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

### Menjalankan Backend di Lokal (Development):
Masuk ke folder backend dan jalankan perintah Gradle:
```bash
cd kasir-api
./gradlew bootRun
```
*Catatan: Flyway akan secara otomatis membuat tabel-tabel database saat aplikasi pertama kali dijalankan.*

### Build & Deploy Backend ke Server (VPS):
1. Buat file JAR:
   ```bash
   cd kasir-api
   ./gradlew build -x test
   ```
2. File JAR yang siap di-deploy akan berada di `kasir-api/build/libs/kasir-api-0.0.1-SNAPSHOT.jar`.
3. Pindahkan file JAR ini ke VPS Anda (misalnya ke `/www/wwwroot/kasir-api/app.jar`).
4. Jalankan menggunakan *Systemd* atau sekadar *nohup* (Lihat bagian Nginx dan Systemd di bawah).

---

## 2. Persiapan Aplikasi Android (Tanpa PC/Laptop)

Membangun (build) aplikasi Android biasanya memerlukan PC/Laptop dengan Android Studio. Namun, karena keterbatasan perangkat, Anda bisa membangunnya langsung dari HP menggunakan layanan **Cloud CI/CD (GitHub Actions)**.

### Langkah-langkah Build APK via HP (GitHub Actions):
1. Buka repositori KASIRINAJA Anda di GitHub melalui browser HP.
2. (Opsional) Jika Anda perlu mengubah `BASE_URL` API agar mengarah ke VPS Anda:
   - Cari file `kasir-android/core/src/main/java/com/kasirinaja/core/network/RetrofitClient.kt`.
   - Klik ikon **Pensil** untuk mengedit file, lalu ubah `"http://10.0.2.2:8080/api/"` menjadi `"https://api.domainkamu.com/api/"`.
   - Simpan (*Commit changes*).
3. Pindah ke menu **Actions** di repositori GitHub Anda.
4. Buat atau jalankan alur kerja (workflow) untuk Build Android. GitHub akan menggunakan *Virtual Machine* mereka untuk meng-compile kode sumber Anda.
5. Setelah *Build* selesai dan sukses (centang hijau), klik *workflow run* tersebut.
6. Scroll ke bawah pada bagian **Artifacts**. Di sana akan ada file `app-release.apk` atau `app-debug.apk` yang bisa Anda unduh langsung ke HP Anda dan di-install.

*(Catatan: Mengembangkan kode/menambah fitur baru tanpa PC sangat sulit dan tidak direkomendasikan, tetapi sekadar meng-compile kode yang sudah ada menjadi APK bisa dilakukan sepenuhnya lewat HP).*

---

## 3. Deployment Server VPS (Menggunakan aaPanel & Systemd)

Agar API Spring Boot bisa terus berjalan di VPS walaupun terminal ditutup, gunakan `systemd`. Anda bisa mengakses terminal VPS ini melalui aplikasi SSH di HP (seperti Termius atau JuiceSSH).

1. Buat file service menggunakan perintah `cat`:
   ```bash
   sudo sh -c 'cat << "SYSTEMD" > /etc/systemd/system/kasir-api.service
   [Unit]
   Description=KasirinAja Spring Boot API
   After=syslog.target network.target

   [Service]
   User=root
   ExecStart=/usr/bin/java -jar /www/wwwroot/kasir-api/app.jar
   SuccessExitStatus=143
   Restart=always
   RestartSec=10

   [Install]
   WantedBy=multi-user.target
   SYSTEMD'
   ```
2. Aktifkan dan jalankan service:
   ```bash
   sudo systemctl daemon-reload
   sudo systemctl enable kasir-api.service
   sudo systemctl start kasir-api.service
   ```
3. Anda bisa melihat log-nya dengan perintah:
   ```bash
   sudo journalctl -u kasir-api.service -f
   ```

*Lihat file `nginx.conf.example` untuk cara menghubungkan Domain Anda dengan aplikasi API Spring Boot (Reverse Proxy).*
