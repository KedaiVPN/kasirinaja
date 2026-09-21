package middleware

import (
	"net/http"

	"github.com/gin-gonic/gin"
)

// RoleRequired memastikan role pengguna (dari klaim JWT) termasuk dalam daftar
// role yang diizinkan. Dipakai sebagai gerbang otorisasi terpusat supaya tidak
// ada handler yang "lupa" memeriksa role.
//
// Wajib dipasang SETELAH AuthMiddleware (butuh context "role").
func RoleRequired(roles ...string) gin.HandlerFunc {
	allowed := make(map[string]struct{}, len(roles))
	for _, r := range roles {
		allowed[r] = struct{}{}
	}

	return func(c *gin.Context) {
		roleVal, exists := c.Get("role")
		if !exists {
			c.AbortWithStatusJSON(http.StatusUnauthorized, gin.H{"error": "Konteks role tidak ditemukan"})
			return
		}

		role, ok := roleVal.(string)
		if !ok || role == "" {
			c.AbortWithStatusJSON(http.StatusUnauthorized, gin.H{"error": "Role tidak valid"})
			return
		}

		if _, ok := allowed[role]; !ok {
			c.AbortWithStatusJSON(http.StatusForbidden, gin.H{"error": "Anda tidak memiliki akses ke endpoint ini"})
			return
		}

		c.Next()
	}
}

// AdminOnly adalah alias eksplisit untuk route /admin/*.
func AdminOnly() gin.HandlerFunc {
	return RoleRequired("admin")
}
