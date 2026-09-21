package middleware

import (
	"github.com/gin-gonic/gin"
)

// SecurityHeaders menyisipkan header keamanan standar pada tiap response.
// Selain defence-in-depth, ini juga membersihkan header server bawaan Gin.
func SecurityHeaders() gin.HandlerFunc {
	return func(c *gin.Context) {
		c.Header("X-Content-Type-Options", "nosniff")
		c.Header("X-Frame-Options", "DENY")
		c.Header("X-XSS-Protection", "1; mode=block")
		c.Header("Referrer-Policy", "no-referrer")
		c.Header("Content-Security-Policy", "default-src 'none'; frame-ancestors 'none'")
		if c.Request.TLS != nil {
			c.Header("Strict-Transport-Security", "max-age=31536000; includeSubDomains")
		}
		// Sembunyikan teknologi server.
		c.Header("Server", "pos-kedai")

		c.Next()
	}
}
