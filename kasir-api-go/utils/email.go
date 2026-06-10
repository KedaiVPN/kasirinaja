package utils

import (
	"crypto/rand"
	"fmt"
	"io"
	"os"
	"strconv"

	"gopkg.in/gomail.v2"
)

// GenerateOTP generates a 6-digit random OTP
func GenerateOTP() string {
	b := make([]byte, 4)
	_, err := io.ReadFull(rand.Reader, b)
	if err != nil {
		return "123456" // fallback
	}

	// Generate number between 100000 and 999999
	val := int(b[0])<<24 | int(b[1])<<16 | int(b[2])<<8 | int(b[3])
	if val < 0 {
		val = -val
	}
	otp := 100000 + (val % 900000)
	return strconv.Itoa(otp)
}

// SendOTPEmail sends the OTP to the specified email using SMTP configuration from environment variables
func SendOTPEmail(toEmail string, otp string) error {
	host := os.Getenv("SMTP_HOST")
	portStr := os.Getenv("SMTP_PORT")
	user := os.Getenv("SMTP_USER")
	pass := os.Getenv("SMTP_PASS")
	from := os.Getenv("SMTP_FROM")

	port, err := strconv.Atoi(portStr)
	if err != nil {
		port = 587 // default SMTP port
	}

	m := gomail.NewMessage()
	m.SetHeader("From", from)
	m.SetHeader("To", toEmail)
	m.SetHeader("Subject", "Kode Verifikasi OTP Anda")

	htmlBody := fmt.Sprintf(`
		<h2>Verifikasi Pendaftaran</h2>
		<p>Terima kasih telah mendaftar. Berikut adalah kode OTP Anda untuk menyelesaikan pendaftaran:</p>
		<h1 style="color: #4A90E2; letter-spacing: 5px;">%s</h1>
		<p>Kode ini berlaku selama 5 menit.</p>
		<p>Jika Anda tidak meminta kode ini, mohon abaikan email ini.</p>
	`, otp)

	m.SetBody("text/html", htmlBody)

	d := gomail.NewDialer(host, port, user, pass)

	return d.DialAndSend(m)
}
