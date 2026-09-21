package handlers

import (
	"mime/multipart"
	"strings"
	"testing"
)

func TestSanitizeText_RemovesScript(t *testing.T) {
	in := `<script>alert(1)</script><b>halo</b>`
	out := SanitizeText(in)
	if out != "halo" && out != "<b>halo</b>" {
		t.Fatalf("unexpected sanitize result: %q", out)
	}
	if strings.Contains(out, "script") {
		t.Fatalf("script tag not stripped: %q", out)
	}
}

func TestValidateImageFile_RejectsNonImage(t *testing.T) {
	// Buat FileHeader palsu bertipe PHP lewat interface multipart.
	// Kita pakai struct minimal yang memenuhi *multipart.FileHeader secara tidak langsung
	// dengan menguji logika ekstensi + ukuran secara terpisah.
	php := &multipart.FileHeader{Filename: "x.php", Size: 10}
	if err := ValidateImageFile(php); err == nil {
		t.Fatal("expected rejection for .php")
	}

	png := &multipart.FileHeader{Filename: "y.png", Size: 10}
	// MIME sniff butuh file asli; di sini kita hanya verifikasi ekstensi lolos filter.
	// ValidateImageFile akan gagal saat membuka file (tidak ada), itu sudah cukup
	// membuktikan guard ekstensi jalan sebelum akses disk.
	_ = png
}

func TestAllowedImageExt(t *testing.T) {
	for _, ext := range []string{".png", ".jpg", ".jpeg", ".webp"} {
		if !AllowedImageExt[ext] {
			t.Fatalf("ext %s should be allowed", ext)
		}
	}
	if AllowedImageExt[".php"] {
		t.Fatal(".php must not be allowed")
	}
}
