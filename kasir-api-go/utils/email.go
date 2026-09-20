package utils

import (
	"crypto/rand"
	"fmt"
	"io"
	"os"
	"strconv"
	"strings"

	"gopkg.in/gomail.v2"
)

type OTPEmailCategory string

const (
	OTPCategoryRegistration        OTPEmailCategory = "registrasi"
	OTPCategoryForgotPasswordOwner OTPEmailCategory = "lupa_password_owner"
	OTPCategoryForgotPasswordKasir OTPEmailCategory = "lupa_password_kasir"
)

type OTPEmailOptions struct {
	ToEmail          string
	OTP              string
	Category         OTPEmailCategory
	RecipientName    string // e.g. Owner name or registrant name
	TargetName       string // e.g. Cashier full name
	TargetIdentifier string // e.g. Cashier username
}

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

// SendOTPEmail sends a customized OTP email based on OTPEmailOptions
func SendOTPEmail(opts OTPEmailOptions) error {
	host := os.Getenv("SMTP_HOST")
	portStr := os.Getenv("SMTP_PORT")
	user := os.Getenv("SMTP_USER")
	pass := strings.Trim(os.Getenv("SMTP_PASS"), `"`)
	from := strings.Trim(os.Getenv("SMTP_FROM"), `"`)

	port, err := strconv.Atoi(portStr)
	if err != nil {
		port = 587 // default SMTP port
	}

	m := gomail.NewMessage()

	// Safely parse "Name <email>" if provided, or fallback to simple email
	if strings.Contains(from, "<") && strings.Contains(from, ">") {
		start := strings.Index(from, "<")
		end := strings.Index(from, ">")
		name := strings.TrimSpace(from[:start])
		emailAddr := from[start+1 : end]
		m.SetAddressHeader("From", emailAddr, name)
	} else {
		m.SetHeader("From", from)
	}

	m.SetHeader("To", opts.ToEmail)

	var subject string
	var headingTitle string
	var messageContent string

	recipientName := strings.TrimSpace(opts.RecipientName)
	if recipientName == "" {
		recipientName = "Pengguna POS Kedai"
	}

	switch opts.Category {
	case OTPCategoryRegistration:
		subject = "Kode OTP Registrasi Toko - POS Kedai"
		headingTitle = "Verifikasi Registrasi Toko"
		messageContent = fmt.Sprintf(
			"Halo <strong>%s</strong>, terima kasih telah mendaftar di <strong>POS Kedai</strong>. Gunakan kode One-Time Password (OTP) berikut untuk memverifikasi pendaftaran toko Anda:",
			recipientName,
		)

	case OTPCategoryForgotPasswordOwner:
		subject = "Kode OTP Reset Password Owner - POS Kedai"
		headingTitle = "Reset Password Owner"
		messageContent = fmt.Sprintf(
			"Halo <strong>%s</strong>, kami menerima permintaan untuk meriset password akun Owner Anda di <strong>POS Kedai</strong>. Gunakan kode One-Time Password (OTP) berikut untuk melanjutkan:",
			recipientName,
		)

	case OTPCategoryForgotPasswordKasir:
		subject = "Kode OTP Reset Password Kasir - POS Kedai"
		headingTitle = "Permintaan Reset Password Kasir"
		targetName := opts.TargetName
		if targetName == "" {
			targetName = "Kasir"
		}
		targetIdent := opts.TargetIdentifier
		if targetIdent == "" {
			targetIdent = "-"
		}
		messageContent = fmt.Sprintf(
			"Halo <strong>%s</strong>, kasir Anda atas nama <strong>%s</strong> (Username: <code>%s</code>) mengajukan permintaan reset password. Silakan berikan kode One-Time Password (OTP) berikut kepada kasir Anda untuk meriset password:",
			recipientName,
			targetName,
			targetIdent,
		)

	default:
		subject = "Kode Verifikasi OTP Anda - POS Kedai"
		headingTitle = "Kode Verifikasi Keamanan"
		messageContent = fmt.Sprintf(
			"Halo <strong>%s</strong>, gunakan kode One-Time Password (OTP) berikut untuk memverifikasi permintaan Anda di aplikasi <strong>POS Kedai</strong>:",
			recipientName,
		)
	}

	m.SetHeader("Subject", subject)

	// Embed Header Logo as CID attachment checking multiple possible paths
	logoPaths := []string{
		"assets/poskedai_logo.png",
		"kasir-api-go/assets/poskedai_logo.png",
		"../assets/poskedai_logo.png",
	}
	hasEmbeddedLogo := false
	for _, lPath := range logoPaths {
		if _, err := os.Stat(lPath); err == nil {
			m.Embed(lPath)
			hasEmbeddedLogo = true
			break
		}
	}

	var logoImgHtml string
	if hasEmbeddedLogo {
		logoImgHtml = `<img src="cid:poskedai_logo.png" alt="POS Kedai" style="max-width: 220px; height: auto; display: block;" />`
	} else {
		logoImgHtml = `<h1 style="color: #1D5040; margin: 0; font-size: 26px; font-weight: bold;">POS Kedai</h1>`
	}

	htmlBody := fmt.Sprintf(`
	<!DOCTYPE html>
	<html>
	<head>
		<meta charset="utf-8">
		<meta name="viewport" content="width=device-width, initial-scale=1.0">
		<title>%s</title>
	</head>
	<body style="margin: 0; padding: 0; background-color: #f4f7f6; font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif; color: #333333;">
		<table role="presentation" width="100%%" border="0" cellspacing="0" cellpadding="0" style="background-color: #f4f7f6; padding: 20px 0;">
			<tr>
				<td align="center">
					<table role="presentation" width="100%%" max-width="600" border="0" cellspacing="0" cellpadding="0" style="background-color: #ffffff; max-width: 600px; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 12px rgba(0,0,0,0.08);">
						<!-- Header with Logo -->
						<tr>
							<td align="center" style="background-color: #ffffff; padding: 30px 20px 20px 20px; border-bottom: 2px solid #eef2f1;">
								%s
							</td>
						</tr>

						<!-- Main Body -->
						<tr>
							<td style="padding: 35px 30px; text-align: center;">
								<h2 style="color: #1D5040; margin: 0 0 12px 0; font-size: 22px; font-weight: 700;">%s</h2>
								<p style="margin: 0 0 24px 0; font-size: 15px; line-height: 1.6; color: #555555;">
									%s
								</p>

								<!-- OTP Box -->
								<div style="background-color: #F0F7F4; border: 2px dashed #1D5040; border-radius: 10px; padding: 20px; margin: 0 auto 28px auto; max-width: 320px;">
									<span style="font-size: 34px; font-weight: bold; letter-spacing: 8px; color: #1D5040; font-family: 'Courier New', Courier, monospace;">%s</span>
								</div>

								<p style="margin: 0 0 20px 0; font-size: 14px; color: #666666; line-height: 1.5;">
									Kode ini berlaku selama <strong>5 menit</strong>. Demi keamanan akun Anda, jangan bagikan kode ini kepada siapa pun.
								</p>

								<hr style="border: none; border-top: 1px solid #eef2f1; margin: 24px 0;" />

								<p style="margin: 0; font-size: 13px; color: #888888; line-height: 1.5;">
									Jika Anda tidak merasa melakukan permintaan ini, silakan abaikan email ini atau hubungi Layanan Bantuan POS Kedai.
								</p>
							</td>
						</tr>

						<!-- Footer -->
						<tr>
							<td align="center" style="background-color: #1D5040; padding: 20px; color: #ffffff; font-size: 12px; line-height: 1.5;">
								<p style="margin: 0 0 4px 0; font-weight: bold;">POS Kedai — Kasir nya UMKM</p>
								<p style="margin: 0; color: #c8e0d8;">&copy; %d POS Kedai. All rights reserved.</p>
							</td>
						</tr>
					</table>
				</td>
			</tr>
		</table>
	</body>
	</html>
	`, headingTitle, logoImgHtml, headingTitle, messageContent, opts.OTP, 2025)

	m.SetBody("text/html", htmlBody)

	d := gomail.NewDialer(host, port, user, pass)

	return d.DialAndSend(m)
}
