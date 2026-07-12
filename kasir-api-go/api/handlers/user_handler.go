package handlers

import (
	"net/http"
	"os"
	"fmt"
	"time"

	"github.com/gin-gonic/gin"
	"github.com/google/uuid"
	"github.com/jackc/pgx/v5/pgtype"
	"golang.org/x/crypto/bcrypt"
	"kasir-api-go/db"
)

type UserHandler struct {
	queries *db.Queries
}

func NewUserHandler(queries *db.Queries) *UserHandler {
	return &UserHandler{queries: queries}
}

type CreateUserRequest struct {
	FullName string `json:"full_name" binding:"required"`
	Email    string `json:"email"`
	Phone    string `json:"phone" binding:"required"`
	Password string `json:"password" binding:"required"`
	Role     string `json:"role" binding:"required"`
	StoreID  string `json:"store_id"` // Optional
}

func (h *UserHandler) CreateUser(c *gin.Context) {
	var req CreateUserRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}

	hashedPassword, err := bcrypt.GenerateFromPassword([]byte(req.Password), bcrypt.DefaultCost)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to hash password"})
		return
	}

	arg := db.CreateUserParams{
		FullName:     req.FullName,
		Email:        pgtype.Text{String: req.Email, Valid: true},
		Phone:        pgtype.Text{String: req.Phone, Valid: true},
		PasswordHash: string(hashedPassword),
		Role:         req.Role,
	}

	if req.StoreID != "" {
		storeUUID, err := uuid.Parse(req.StoreID)
		if err == nil {
			arg.StoreID = pgtype.UUID{Bytes: storeUUID, Valid: true}
		}
	}

	user, err := h.queries.CreateUser(c.Request.Context(), arg)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
		return
	}

	c.JSON(http.StatusCreated, user)
}

func (h *UserHandler) GetUser(c *gin.Context) {
	idParam := c.Param("id")
	id, err := uuid.Parse(idParam)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "invalid user id"})
		return
	}

	user, err := h.queries.GetUser(c.Request.Context(), pgtype.UUID{Bytes: id, Valid: true})
	if err != nil {
		c.JSON(http.StatusNotFound, gin.H{"error": "user not found"})
		return
	}

	c.JSON(http.StatusOK, user)
}

func (h *UserHandler) ListUsers(c *gin.Context) {
	users, err := h.queries.ListUsers(c.Request.Context())
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
		return
	}

	c.JSON(http.StatusOK, users)
}

func (h *UserHandler) ListStoreUsers(c *gin.Context) {
	storeIDStr, exists := c.Get("store_id")
	if !exists || storeIDStr == "" {
		c.JSON(http.StatusUnauthorized, gin.H{"error": "Store ID missing from token"})
		return
	}

	storeIDUUID, err := uuid.Parse(storeIDStr.(string))
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "Invalid store ID format"})
		return
	}

	users, err := h.queries.ListUsersByStore(c.Request.Context(), pgtype.UUID{Bytes: storeIDUUID, Valid: true})
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
		return
	}

	c.JSON(http.StatusOK, users)
}

func (h *UserHandler) AddStoreEmployee(c *gin.Context) {
	role, exists := c.Get("role")
	if !exists || role != "owner" {
		c.JSON(http.StatusForbidden, gin.H{"error": "Only owners can add employees"})
		return
	}

	storeIDStr, exists := c.Get("store_id")
	if !exists || storeIDStr == "" {
		c.JSON(http.StatusUnauthorized, gin.H{"error": "Store ID missing from token"})
		return
	}

	var req CreateUserRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}

	// We don't need email for cashier, but the database schema requires a unique email.
	// Since we are not using email for login, we can generate a dummy email based on phone or name.
	// Wait, the schema says: email VARCHAR(255) UNIQUE.
	email := req.Email
	if email == "" {
		email = req.Phone + "@dummy.kasirinaja.com"
	}

	hashedPassword, err := bcrypt.GenerateFromPassword([]byte(req.Password), bcrypt.DefaultCost)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to hash password"})
		return
	}

	storeUUID, _ := uuid.Parse(storeIDStr.(string))

	arg := db.CreateUserParams{
		FullName:     req.FullName,
		Email:        pgtype.Text{String: email, Valid: true},
		Phone:        pgtype.Text{String: req.Phone, Valid: true},
		PasswordHash: string(hashedPassword),
		Role:         req.Role, // Should be "kasir"
		StoreID:      pgtype.UUID{Bytes: storeUUID, Valid: true},
	}

	user, err := h.queries.CreateUser(c.Request.Context(), arg)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
		return
	}

	c.JSON(http.StatusCreated, user)
}

type UpdateProfileRequest struct {
	FullName string `form:"full_name"`
	// photo is handled as a file upload
}

func (h *UserHandler) UpdateProfile(c *gin.Context) {
	userIDStr, exists := c.Get("user_id")
	if !exists {
		c.JSON(http.StatusUnauthorized, gin.H{"error": "User ID missing from token"})
		return
	}

	storeIDStr, exists := c.Get("store_id")
	if !exists {
		c.JSON(http.StatusUnauthorized, gin.H{"error": "Store ID missing from token"})
		return
	}

	userIDUUID, err := uuid.Parse(userIDStr.(string))
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "Invalid user ID"})
		return
	}

	var req UpdateProfileRequest
	if err := c.ShouldBind(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}

	arg := db.UpdateUserProfileParams{
		ID: pgtype.UUID{Bytes: userIDUUID, Valid: true},
	}

	if req.FullName != "" {
		arg.FullName = pgtype.Text{String: req.FullName, Valid: true}
	}

	file, err := c.FormFile("photo")
	if err == nil {
		// user uploaded a file
		// Sanitize file path just like in upload_handler
		uploadDir := "./uploads/" + storeIDStr.(string)

		if err := os.MkdirAll(uploadDir, os.ModePerm); err != nil {
			c.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to create directory"})
			return
		}

		filename := userIDStr.(string) + ".png"
		filePath := uploadDir + "/" + filename

		if err := c.SaveUploadedFile(file, filePath); err != nil {
			c.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to save file"})
			return
		}

		photoURL := fmt.Sprintf("/uploads/%s/%s?t=%d", storeIDStr.(string), filename, time.Now().Unix())
		arg.PhotoUrl = pgtype.Text{String: photoURL, Valid: true}
	}

	updatedUser, err := h.queries.UpdateUserProfile(c.Request.Context(), arg)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
		return
	}

	c.JSON(http.StatusOK, updatedUser)
}
