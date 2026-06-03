# Panduan Deployment Kasir API (Go) di aaPanel (Build via VPS Ubuntu)

Dokumen ini berisi panduan langkah demi langkah untuk melakukan *deploy* dan *build* aplikasi backend **Kasir API** yang ditulis menggunakan Go langsung di *server* Ubuntu Anda yang menggunakan **aaPanel**.

## Prasyarat

Sebelum memulai, pastikan server aaPanel Anda sudah memiliki komponen berikut:

1.  **PostgreSQL** (Bisa diinstal melalui App Store aaPanel atau berjalan di Docker). Disarankan menggunakan PostgreSQL versi 13 atau lebih baru yang sudah mendukung fungsi `gen_random_uuid()` secara bawaan.
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

Jalankan migrasi menggunakan *goose* dari terminal SSH di direktori aplikasi Anda.
*(Catatan: Migrasi ini menggunakan fungsi `gen_random_uuid()` bawaan PostgreSQL modern, sehingga tidak memerlukan ekstensi khusus yang butuh akses Superuser)*.

```bash
cd /www/wwwroot/api.domainanda.com
goose -dir db/migrations postgres "postgres://kasir_user:PASSWORD_ANDA@127.0.0.1:5432/kasir?sslmode=disable" up
```
*(Ganti `PASSWORD_ANDA` sesuai password database Anda)*.

---

## Langkah 5: Deployment Menggunakan Fitur Go Project di aaPanel

aaPanel memiliki fitur bawaan untuk menjalankan aplikasi Go dengan lebih mudah. Fitur ini akan otomatis menangani daemon (agar aplikasi terus berjalan) dan juga konfigurasi reverse proxy Nginx.

1.  Buka menu **Website** di aaPanel.
2.  Klik pada tab **Go project** (terletak di bagian atas, sejajar dengan PHP project, Node project, dll).
3.  Klik **Add Go project**.
4.  Isi konfigurasi berikut:
    *   **Project name:** `kasir-api` (atau nama lain yang Anda inginkan)
    *   **Project path:** Pilih folder tempat Anda menaruh *source code* (misalnya `/www/wwwroot/api.domainanda.com`)
    *   **Executable file:** Pilih file *binary* yang sudah Anda *build* di langkah sebelumnya (misalnya `/www/wwwroot/api.domainanda.com/kasir-api-app`)
    *   **Port:** `8080` (Pastikan port ini sama dengan yang ada di file `.env`)
    *   **Run user:** `www`
    *   **Domain:** Masukkan nama domain atau subdomain Anda (misal: `api.domainanda.com`)
5.  Klik **Submit**. aaPanel akan secara otomatis membuatkan konfigurasi Nginx dan menjalankan aplikasi Go Anda di latar belakang.

*(Opsional)* Setelah project ditambahkan, Anda bisa klik tombol **SSL** di baris project tersebut pada daftar Go project untuk mengaktifkan HTTPS menggunakan Let's Encrypt.

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
