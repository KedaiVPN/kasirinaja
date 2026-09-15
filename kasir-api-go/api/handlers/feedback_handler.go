package handlers

import (
	"net/http"
	"strings"

	"github.com/gin-gonic/gin"
	"github.com/google/uuid"
	"github.com/jackc/pgx/v5/pgtype"
	"kasir-api-go/db"
	"kasir-api-go/utils"
)

type FeedbackHandler struct {
	queries *db.Queries
}

func NewFeedbackHandler(queries *db.Queries) *FeedbackHandler {
	return &FeedbackHandler{
		queries: queries,
	}
}

type SendFeedbackRequest struct {
	SenderEmail string `json:"sender_email"`
	Message     string `json:"message" binding:"required"`
}

func (h *FeedbackHandler) SendFeedback(c *gin.Context) {
	var req SendFeedbackRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "Pesan kritik & saran tidak boleh kosong"})
		return
	}

	if strings.TrimSpace(req.Message) == "" {
		c.JSON(http.StatusBadRequest, gin.H{"error": "Pesan kritik & saran tidak boleh kosong"})
		return
	}

	userIDVal, exists := c.Get("user_id")
	if !exists {
		c.JSON(http.StatusUnauthorized, gin.H{"error": "Pengguna tidak terautentikasi"})
		return
	}

	var userUUID pgtype.UUID
	if str, ok := userIDVal.(string); ok {
		parsed, err := uuid.Parse(str)
		if err == nil {
			userUUID = pgtype.UUID{Bytes: parsed, Valid: true}
		}
	}

	if !userUUID.Valid {
		c.JSON(http.StatusBadRequest, gin.H{"error": "ID pengguna tidak valid"})
		return
	}

	user, err := h.queries.GetUser(c.Request.Context(), userUUID)
	if err != nil {
		c.JSON(http.StatusNotFound, gin.H{"error": "Pengguna tidak ditemukan"})
		return
	}

	senderName := user.FullName
	senderEmail := strings.TrimSpace(req.SenderEmail)
	senderRole := user.Role

	// If owner or senderEmail not provided, fallback to user's registered email
	if senderEmail == "" || user.Role == "owner" {
		senderEmail = user.Email.String
	}

	storeName := "Kedai SSH"
	if user.StoreID.Valid {
		store, err := h.queries.GetStore(c.Request.Context(), user.StoreID)
		if err == nil && store.StoreName != "" {
			storeName = store.StoreName
		}
	}

	err = utils.SendFeedbackEmail(senderName, senderEmail, senderRole, storeName, req.Message)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Gagal mengirim email kritik & saran: " + err.Error()})
		return
	}

	c.JSON(http.StatusOK, gin.H{
		"message": "Kritik & saran berhasil dikirim",
	})
}
