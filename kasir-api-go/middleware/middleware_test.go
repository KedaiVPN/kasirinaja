package middleware

import (
	"net/http"
	"net/http/httptest"
	"testing"

	"github.com/gin-gonic/gin"
)

func TestRoleRequired_AllowsAndRejects(t *testing.T) {
	gin.SetMode(gin.TestMode)

	cases := []struct {
		name     string
		role     interface{}
		allowed  []string
		wantCode int
	}{
		{"admin allowed for admin-only", "admin", []string{"admin"}, http.StatusOK},
		{"owner rejected for admin-only", "owner", []string{"admin"}, http.StatusForbidden},
		{"no role -> unauthorized", nil, []string{"admin"}, http.StatusUnauthorized},
		{"owner allowed for owner+admin", "owner", []string{"owner", "admin"}, http.StatusOK},
	}

	for _, tc := range cases {
		t.Run(tc.name, func(t *testing.T) {
			r := gin.New()
			r.Use(func(c *gin.Context) {
				if tc.role != nil {
					c.Set("role", tc.role)
				}
				c.Next()
			})
			r.Use(RoleRequired(tc.allowed...))
			r.GET("/x", func(c *gin.Context) { c.Status(http.StatusOK) })

			w := httptest.NewRecorder()
			req := httptest.NewRequest(http.MethodGet, "/x", nil)
			r.ServeHTTP(w, req)

			if w.Code != tc.wantCode {
				t.Errorf("got %d, want %d", w.Code, tc.wantCode)
			}
		})
	}
}

func TestSecurityHeadersPresent(t *testing.T) {
	gin.SetMode(gin.TestMode)
	r := gin.New()
	r.Use(SecurityHeaders())
	r.GET("/x", func(c *gin.Context) { c.Status(http.StatusOK) })

	w := httptest.NewRecorder()
	req := httptest.NewRequest(http.MethodGet, "/x", nil)
	r.ServeHTTP(w, req)

	for _, h := range []string{"X-Content-Type-Options", "X-Frame-Options", "Content-Security-Policy", "Referrer-Policy"} {
		if w.Header().Get(h) == "" {
			t.Errorf("missing header %s", h)
		}
	}
}
