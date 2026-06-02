# Panduan Deployment Kasir API (Go) di aaPanel

Dokumen ini berisi panduan langkah demi langkah untuk melakukan *deploy* aplikasi backend **Kasir API** yang ditulis menggunakan Go ke *server* yang menggunakan **aaPanel**.

## Prasyarat

Sebelum memulai, pastikan server aaPanel Anda sudah memiliki komponen berikut:

1.  **PostgreSQL** (Bisa diinstal melalui App Store aaPanel atau berjalan di Docker).
2.  **Nginx** (Sudah terinstal secara *default* di aaPanel).
3.  Akses Terminal/SSH ke server (sebagai *root* atau *user* dengan akses *sudo*).
4.  (Opsional tapi disarankan) Domain atau *subdomain* yang sudah diarahkan (A record) ke IP *server* Anda.

---

## Langkah 1: Persiapan Database di aaPanel

1.  Buka **aaPanel**.
2.  Masuk ke menu **Databases** > **PostgreSQL** (atau Add Database jika menggunakan plugin eksternal/Docker).
3.  Buat *database* baru:
    *   **DB Name:** `kasir` (atau sesuai keinginan).
    *   **Username:** `kasir_user`
    *   **Password:** Buat *password* yang kuat.
4.  Catat kredensial ini karena akan digunakan di *file* `.env` nanti.

---

## Langkah 2: Build Aplikasi Go

Lebih disarankan melakukan *build* aplikasi di komputer lokal Anda (atau di CI/CD) untuk menghindari perlunya menginstal Go di server *production*.

Di terminal komputer lokal Anda, jalankan perintah berikut:

```bash
# Set target OS ke Linux dan arsitektur ke amd64 (sesuaikan jika server menggunakan ARM)
GOOS=linux GOARCH=amd64 go build -o kasir-api-app main.go
```

Perintah di atas akan menghasilkan *file* *binary* bernama `kasir-api-app`.

---

## Langkah 3: Upload File ke Server

1.  Di **aaPanel**, masuk ke menu **Files**.
2.  Buat direktori baru untuk aplikasi Anda, misalnya di `/www/wwwroot/api.domainanda.com`.
3.  Unggah (*upload*) *file* berikut ke dalam folder tersebut:
    *   `kasir-api-app` (File *binary* yang baru saja di-*build*).
    *   Folder `db/migrations` (Berisi *file* skema SQL).
4.  Buat *file* baru bernama `.env` di dalam folder tersebut dan isi dengan konfigurasi berikut:

```env
PORT=8080
DATABASE_URL=postgres://kasir_user:PASSWORD_ANDA@127.0.0.1:5432/kasir?sslmode=disable
GIN_MODE=release
```
*(Ganti `PASSWORD_ANDA` dan nama *database* sesuai dengan yang Anda buat di Langkah 1)*.

---

## Langkah 4: Berikan Izin Eksekusi (Execute Permission)

Agar *file binary* bisa dijalankan, berikan izin eksekusi.
1.  Di menu **Files** aaPanel, klik kanan pada *file* `kasir-api-app`.
2.  Pilih **Permission**.
3.  Ubah *permission* menjadi `755` atau centang opsi *Execute* (x) untuk *Owner*.

*(Atau via SSH: `chmod +x /www/wwwroot/api.domainanda.com/kasir-api-app`)*

---

## Langkah 5: Migrasi Database (Goose)

Untuk menjalankan migrasi, Anda membutuhkan *tool* `goose`. Jika belum ada di server, Anda bisa menginstalnya atau menjalankan migrasi dari komputer lokal dengan mengarahkan `DATABASE_URL` ke server Anda.

Jika menjalankan dari SSH Server:
```bash
cd /www/wwwroot/api.domainanda.com
# Pastikan goose sudah terinstal di server, lalu jalankan:
goose -dir db/migrations postgres "postgres://kasir_user:PASSWORD_ANDA@127.0.0.1:5432/kasir?sslmode=disable" up
```

---

## Langkah 6: Menjalankan Aplikasi dengan Supervisor (Daemon)

Aplikasi Go harus berjalan di latar belakang secara terus-menerus. Di aaPanel, cara termudah adalah menggunakan **Supervisor**.

1.  Buka menu **App Store** di aaPanel.
2.  Cari dan instal **Supervisor** (jika belum ada).
3.  Setelah terinstal, buka **Supervisor** dan klik **Add Daemon**.
4.  Isi konfigurasi berikut:
    *   **Name:** `kasir-api`
    *   **Run User:** `root` (atau *user* `www`)
    *   **Run Dir:** `/www/wwwroot/api.domainanda.com` (Pilih folder aplikasi Anda)
    *   **Start Command:** `/www/wwwroot/api.domainanda.com/kasir-api-app`
    *   **Processes:** 1
5.  Klik **Confirm**.
6.  Pastikan status *daemon* berubah menjadi **Running**. Anda bisa mengecek log di tombol **Log** untuk memastikan aplikasi berjalan (misalnya muncul tulisan `Server is running on port 8080`).

---

## Langkah 7: Konfigurasi Nginx (Reverse Proxy)

Sekarang aplikasi berjalan di *port* `8080`. Kita perlu mengeksposnya ke publik melalui Nginx di *port* `80` (atau `443` untuk HTTPS).

1.  Di **aaPanel**, masuk ke menu **Websites**.
2.  Klik **Add site**.
3.  Masukkan nama domain/subdomain Anda (misal: `api.domainanda.com`).
4.  Pada bagian **PHP version**, pilih **Static** (karena ini bukan aplikasi PHP).
5.  Pada bagian **Site directory**, arahkan ke `/www/wwwroot/api.domainanda.com`.
6.  Klik **Submit**.

Setelah situs ditambahkan:
1.  Klik nama situs Anda (atau klik tulisan `Conf`).
2.  Masuk ke tab **Reverse proxy**.
3.  Klik **Add reverse proxy**.
4.  Isi konfigurasi:
    *   **Proxy name:** `go-api`
    *   **Target URL:** `http://127.0.0.1:8080`
    *   **Sent Domain:** `$host`
5.  Klik **Submit**.

*(Opsional)* Anda bisa langsung mengaktifkan SSL dengan masuk ke tab **SSL**, pilih **Let's Encrypt**, centang domain Anda, dan klik **Apply**.

---

## Langkah 8: Pengujian

Buka *browser* atau aplikasi seperti Postman/cURL, lalu akses:

```text
http(s)://api.domainanda.com/api/health
```

Jika berhasil, Anda akan menerima respons:
```json
{
    "status": "ok"
}
```

**Selesai!** Aplikasi Backend Kasir API Go Anda telah berhasil di-*deploy* dan siap digunakan di production.
