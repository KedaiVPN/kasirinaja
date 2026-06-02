# Panduan Deployment Kasir API (Go) di aaPanel (Build via VPS Ubuntu)

Dokumen ini berisi panduan langkah demi langkah untuk melakukan *deploy* dan *build* aplikasi backend **Kasir API** yang ditulis menggunakan Go langsung di *server* Ubuntu Anda yang menggunakan **aaPanel**.

## Prasyarat

Sebelum memulai, pastikan server aaPanel Anda sudah memiliki komponen berikut:

1.  **PostgreSQL** (Bisa diinstal melalui App Store aaPanel atau berjalan di Docker).
2.  **Nginx** (Sudah terinstal secara *default* di aaPanel).
3.  Akses Terminal/SSH ke server Ubuntu Anda (sebagai *root* atau *user* dengan akses *sudo*).
4.  Domain atau *subdomain* yang sudah diarahkan (A record) ke IP *server* Anda.

---

## Langkah 1: Instalasi Golang & Goose di VPS Ubuntu

Karena Anda akan melakukan *build* dan migrasi langsung di VPS, instal Go dan Goose terlebih dahulu melalui SSH.

**1. Install Golang:**
```bash
# Hapus versi Go lama (jika ada) dan ekstrak versi terbaru
wget https://go.dev/dl/go1.22.0.linux-amd64.tar.gz
sudo rm -rf /usr/local/go && sudo tar -C /usr/local -xzf go1.22.0.linux-amd64.tar.gz

# Tambahkan Go ke PATH (jalankan baris ini satu per satu)
echo "export PATH=\$PATH:/usr/local/go/bin" >> ~/.profile
echo "export PATH=\$PATH:\$(/usr/local/go/bin/go env GOPATH)/bin" >> ~/.profile
source ~/.profile

# Verifikasi instalasi Go
go version
```

**2. Install Goose (Alat Migrasi):**
```bash
go install github.com/pressly/goose/v3/cmd/goose@latest

# Verifikasi instalasi Goose
goose -version
```

---

## Langkah 2: Persiapan Database di aaPanel

1.  Buka **aaPanel**.
2.  Masuk ke menu **Databases** > **PostgreSQL** (atau Add Database jika menggunakan plugin eksternal/Docker).
3.  Buat *database* baru:
    *   **DB Name:** `kasir` (atau sesuai keinginan).
    *   **Username:** `kasir_user`
    *   **Password:** Buat *password* yang kuat.
4.  Catat kredensial ini karena akan digunakan di *file* `.env` dan migrasi.

---

## Langkah 3: Upload Source Code & Build Aplikasi

1. Upload *source code* (folder `kasir-api-go`) ke server aaPanel Anda. Letakkan di direktori *website*, misalnya di `/www/wwwroot/api.domainanda.com`.
2. Buka Terminal SSH, lalu navigasi ke direktori tersebut:
   ```bash
   cd /www/wwwroot/api.domainanda.com
   ```
3. Unduh dependensi dan *build* aplikasinya:
   ```bash
   go mod tidy
   go build -o kasir-api-app main.go
   ```
4. Pastikan file `kasir-api-app` terbentuk dan ubah hak aksesnya:
   ```bash
   chmod +x kasir-api-app
   ```
5. Buat file `.env` di folder yang sama:
   ```bash
   cat << 'ENVEOF' > .env
   PORT=8080
   DATABASE_URL=postgres://kasir_user:PASSWORD_ANDA@127.0.0.1:5432/kasir?sslmode=disable
   GIN_MODE=release
   ENVEOF
   ```
   *(Ganti `PASSWORD_ANDA` dan nama *database* sesuai yang dibuat di Langkah 2).*

---

## Langkah 4: Migrasi Database

Jalankan migrasi menggunakan *goose* dari terminal SSH di direktori aplikasi Anda:

```bash
cd /www/wwwroot/api.domainanda.com
goose -dir db/migrations postgres "postgres://kasir_user:PASSWORD_ANDA@127.0.0.1:5432/kasir?sslmode=disable" up
```
*(Ganti `PASSWORD_ANDA` sesuai password database Anda)*.

---

## Langkah 5: Menjalankan Aplikasi dengan Supervisor (Daemon)

Agar aplikasi terus menyala meskipun SSH ditutup, gunakan Supervisor.

1.  Buka menu **App Store** di aaPanel.
2.  Cari dan instal **Supervisor** (jika belum ada).
3.  Buka **Supervisor** dan klik **Add Daemon**.
4.  Isi konfigurasi berikut:
    *   **Name:** `kasir-api`
    *   **Run User:** `root` (atau *user* `www`)
    *   **Run Dir:** `/www/wwwroot/api.domainanda.com`
    *   **Start Command:** `/www/wwwroot/api.domainanda.com/kasir-api-app`
    *   **Processes:** 1
5.  Klik **Confirm**.
6.  Klik tombol **Log** untuk memastikan aplikasi berjalan (misalnya muncul tulisan `Server is running on port 8080`).

---

## Langkah 6: Konfigurasi Nginx (Reverse Proxy)

Ekspos aplikasi ke publik melalui Nginx di *port* 80/443.

1.  Di **aaPanel**, masuk ke menu **Websites**.
2.  Klik **Add site**.
3.  Masukkan nama domain/subdomain Anda (misal: `api.domainanda.com`).
4.  Pada bagian **PHP version**, pilih **Static**.
5.  Pada bagian **Site directory**, biarkan menunjuk ke `/www/wwwroot/api.domainanda.com`.
6.  Klik **Submit**.
7.  Klik nama situs Anda (atau klik `Conf`), masuk ke tab **Reverse proxy**.
8.  Klik **Add reverse proxy**.
9.  Isi konfigurasi:
    *   **Proxy name:** `go-api`
    *   **Target URL:** `http://127.0.0.1:8080`
    *   **Sent Domain:** `$host`
10. Klik **Submit**.

*(Opsional)* Aktifkan SSL di tab **SSL**, pilih **Let's Encrypt**, centang domain Anda, dan klik **Apply**.

---

## Langkah 7: Pengujian

Buka *browser* atau aplikasi Postman, akses:

```text
https://api.domainanda.com/api/health
```

Jika berhasil, responsnya adalah:
```json
{
    "status": "ok"
}
```
