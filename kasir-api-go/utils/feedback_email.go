package utils

import (
	"fmt"
	"os"
	"strconv"
	"strings"

	"gopkg.in/gomail.v2"
)

// SendFeedbackEmail sends user feedback/critique/suggestion to EMAIL_KRITIK_SARAN
func SendFeedbackEmail(senderName, senderEmail, senderRole, storeName, feedbackText string) error {
	host := os.Getenv("SMTP_FB_HOST")
	portStr := os.Getenv("SMTP_FB_PORT")
	user := os.Getenv("SMTP_FB_USER")
	pass := strings.Trim(os.Getenv("SMTP_FB_PASS"), `"`)
	from := strings.Trim(os.Getenv("SMTP_FB_FROM"), `"`)

	recipient := os.Getenv("EMAIL_KRITIK_SARAN")
	if recipient == "" {
		recipient = "kritiksaranposkedai@gmail.com"
	}

	port, err := strconv.Atoi(portStr)
	if err != nil {
		port = 587
	}

	m := gomail.NewMessage()

	// Set From address
	if strings.Contains(from, "<") && strings.Contains(from, ">") {
		start := strings.Index(from, "<")
		end := strings.Index(from, ">")
		name := strings.TrimSpace(from[:start])
		emailAddr := from[start+1 : end]
		m.SetAddressHeader("From", emailAddr, name)
	} else if from != "" {
		m.SetHeader("From", from)
	} else {
		m.SetHeader("From", user)
	}

	// Set Reply-To header so replying in email client goes directly to the sender's email
	if senderEmail != "" {
		if senderName != "" {
			m.SetAddressHeader("Reply-To", senderEmail, senderName)
		} else {
			m.SetHeader("Reply-To", senderEmail)
		}
	}

	m.SetHeader("To", recipient)
	m.SetHeader("Subject", fmt.Sprintf("Kritik & Saran dari %s - %s", senderName, storeName))

	htmlBody := fmt.Sprintf(`
		<h3>Kritik & Saran Baru</h3>
		<p><b>Dari:</b> %s</p>
		<p><b>Role:</b> %s</p>
		<p><b>Toko:</b> %s</p>
		<hr />
		<p><b>Pesan:</b></p>
		<p style="white-space: pre-wrap; background-color: #f4f4f4; padding: 12px; border-radius: 8px;">%s</p>
	`, senderName, senderRole, storeName, feedbackText)

	m.SetBody("text/html", htmlBody)

	d := gomail.NewDialer(host, port, user, pass)

	return d.DialAndSend(m)
}
