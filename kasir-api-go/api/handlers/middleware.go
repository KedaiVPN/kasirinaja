package handlers

import (
	"fmt"
	"net/http"
	"os"
	"strings"
	"time"

	"kasir-api-go/db"

	"github.com/gin-gonic/gin"
	"github.com/golang-jwt/jwt/v5"
	"github.com/google/uuid"
	"github.com/jackc/pgx/v5/pgtype"
)

func AuthMiddleware(queries *db.Queries) gin.HandlerFunc {
	return func(c *gin.Context) {
		authHeader := c.GetHeader("Authorization")
		if authHeader == "" {
			c.JSON(http.StatusUnauthorized, gin.H{"error": "Authorization header is required"})
			c.Abort()
			return
		}

		parts := strings.Split(authHeader, " ")
		if len(parts) != 2 || parts[0] != "Bearer" {
			c.JSON(http.StatusUnauthorized, gin.H{"error": "Authorization header format must be Bearer {token}"})
			c.Abort()
			return
		}

		tokenString := parts[1]
		jwtSecret := os.Getenv("JWT_SECRET")
		if jwtSecret == "" {
			jwtSecret = "secret" // Default fallback
		}

		token, err := jwt.Parse(tokenString, func(token *jwt.Token) (interface{}, error) {
			if _, ok := token.Method.(*jwt.SigningMethodHMAC); !ok {
				return nil, fmt.Errorf("unexpected signing method: %v", token.Header["alg"])
			}
			return []byte(jwtSecret), nil
		})

		if err != nil || !token.Valid {
			c.JSON(http.StatusUnauthorized, gin.H{"error": "Invalid token"})
			c.Abort()
			return
		}

		claims, ok := token.Claims.(jwt.MapClaims)
		if !ok {
			c.JSON(http.StatusUnauthorized, gin.H{"error": "Invalid token claims"})
			c.Abort()
			return
		}

		c.Set("user_id", claims["user_id"])
		role, _ := claims["role"].(string)
		c.Set("role", role)

		if storeIDVal, exists := claims["store_id"]; exists {
			c.Set("store_id", storeIDVal)

			if role == "kasir" && queries != nil {
				if storeIDStr, ok := storeIDVal.(string); ok && storeIDStr != "" {
					parsedUUID, err := uuid.Parse(storeIDStr)
					if err == nil {
						storeStatus, err := queries.GetStoreProStatus(c.Request.Context(), pgtype.UUID{Bytes: parsedUUID, Valid: true})
						if err != nil || !storeStatus.ProExpiresAt.Valid || storeStatus.ProExpiresAt.Time.Before(time.Now()) {
							c.JSON(http.StatusUnauthorized, gin.H{"error": "Masa aktif Pro toko telah berakhir. Sesi login karyawan dikunci."})
							c.Abort()
							return
						}
					}
				}
			}
		}
		c.Next()
	}
}
