package middleware

import (
	"fmt"
	"net/http"
	"time"

	"github.com/gin-gonic/gin"
	"github.com/go-redis/redis/v8"
)

// RateLimit membatasi jumlah request per IP + path dalam jendela waktu tertentu.
// Implementasi fixed-window memakai Redis INCR + EXPIRE sehingga tetap benar
// walau aplikasi dijalankan di beberapa instance sekaligus.
//
// Jika Redis tidak tersedia, request TIDAK diblokir (fail-open) supaya API tetap
// hidup, tetapi error dicatat ke header supaya mudah dideteksi saat debugging.
func RateLimit(rdb *redis.Client, limit int, window time.Duration) gin.HandlerFunc {
	return func(c *gin.Context) {
		if rdb == nil || limit <= 0 {
			c.Next()
			return
		}

		// Key per IP + route, bukan per method, supaya tidak bisa dilewati
		// dengan mengganti method.
		key := fmt.Sprintf("rl:%s:%s", c.ClientIP(), c.FullPath())

		ctx := c.Request.Context()

		cnt, err := rdb.Incr(ctx, key).Result()
		if err != nil {
			// Fail-open: Redis mati jangan sampai mematikan seluruh API.
			c.Header("X-RateLimit-Error", "redis-unavailable")
			c.Next()
			return
		}

		// Set TTL hanya pada increment pertama supaya jendela tidak diperpanjang
		// terus-menerus oleh request berikutnya.
		if cnt == 1 {
			rdb.Expire(ctx, key, window)
		}

		remaining := int64(limit) - cnt
		if remaining < 0 {
			remaining = 0
		}
		c.Header("X-RateLimit-Limit", fmt.Sprintf("%d", limit))
		c.Header("X-RateLimit-Remaining", fmt.Sprintf("%d", remaining))

		if cnt > int64(limit) {
			ttl, _ := rdb.TTL(ctx, key).Result()
			if ttl > 0 {
				c.Header("Retry-After", fmt.Sprintf("%d", int(ttl.Seconds())))
			}
			c.AbortWithStatusJSON(http.StatusTooManyRequests, gin.H{
				"error": "Terlalu banyak permintaan. Silakan coba lagi nanti.",
			})
			return
		}

		c.Next()
	}
}
