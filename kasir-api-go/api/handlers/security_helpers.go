package handlers

import (
	"fmt"
	"os"
	"strings"

	"github.com/microcosm-cc/bluemonday"
)

// GetJWTSecret mengembalikan secret yang dipakai untuk signing JWT.
// Jika env JWT_SECRET tidak diset atau kosong, fungsi ini akan panic supaya
// tidak pernah jatuh ke default yang mudah ditebak ("secret") di produksi.
func GetJWTSecret() string {
	s := os.Getenv("JWT_SECRET")
	if s == "" {
		panic("JWT_SECRET environment variable wajib di-set di produksi. Tidak boleh kosong.")
	}
	if s == "secret" || s == "change-me" || len(s) < 16 {
		panic("JWT_SECRET terlalu lemah atau memakai default. Gunakan minimal 32 karakter random.")
	}
	return s
}

// ValidateJWTSecret memeriksa kekuatan JWT_SECRET tanpa memicu panic.
// Dipakai di main() (fail-fast) agar server tidak jalan dengan secret lemah.
func ValidateJWTSecret() error {
	s := os.Getenv("JWT_SECRET")
	if s == "" {
		return fmt.Errorf("JWT_SECRET kosong")
	}
	if s == "secret" || s == "change-me" || len(s) < 16 {
		return fmt.Errorf("JWT_SECRET terlalu lemah (pakai default/minimal 16 karakter)")
	}
	return nil
}

// htmlCleaner adalah UGC policy bluemonday yang mengizinkan tag HTML aman
// sambil membuang script / event handler berbahaya.
var htmlCleaner = bluemonday.UGCPolicy()

// SanitizeText menghapus tag HTML/JS berbahaya dari input teks user
// sebelum disimpan ke database. Cukup untuk mencegah XSS tersimpan.
func SanitizeText(input string) string {
	return strings.TrimSpace(htmlCleaner.Sanitize(input))
}

// AllowedImageExt daftar ekstensi gambar yang boleh di-upload.
var AllowedImageExt = map[string]bool{
	".png":  true,
	".jpg":  true,
	".jpeg": true,
	".webp": true,
}

// MaxUploadSize default 3 MB – sama dengan nilai di store_handler.go.
const MaxUploadSize int64 = 3 * 1024 * 1024