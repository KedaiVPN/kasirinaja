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

## 2. Persiapan Aplikasi Android

### Syarat (Prerequisites)
- Android Studio versi terbaru (Iguana / Jellyfish).
- Emulator atau HP Android fisik dengan OS minimal Android 7.0 (API 24).

### Langkah-langkah Build Android:
1. Buka **Android Studio**.
2. Pilih **File -> Open**, lalu arahkan ke folder `kasir-android/`.
3. Tunggu hingga proses *Gradle Sync* selesai.
4. (Opsional) Buka `kasir-android/core/src/main/java/com/kasirinaja/core/network/RetrofitClient.kt`. Ubah `BASE_URL` dari `http://10.0.2.2:8080/api/` menjadi URL domain VPS Anda jika ingin mengetes versi production (misal: `https://api.domainkamu.com/api/`).
5. Pada *toolbar* atas di Android Studio, Anda akan melihat pilihan modul.
   - Pilih modul **app-store** untuk meng-compile Aplikasi Toko.
   - Pilih modul **app-admin** untuk meng-compile Aplikasi Admin.
6. Klik tombol **Run (Segitiga Hijau)**.

### Build APK (Untuk di-instal di HP):
Jika Anda ingin membuat file APK:
1. Di Android Studio, klik menu **Build -> Build Bundle(s) / APK(s) -> Build APK(s)**.
2. File APK akan muncul di:
   - `kasir-android/app-store/build/outputs/apk/debug/` (Aplikasi Toko)
   - `kasir-android/app-admin/build/outputs/apk/debug/` (Aplikasi Admin)

---

## 3. Deployment Server VPS (Menggunakan aaPanel & Systemd)

Agar API Spring Boot bisa terus berjalan di VPS walaupun terminal ditutup, gunakan `systemd`.

1. Buat file service menggunakan command line editor:
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
