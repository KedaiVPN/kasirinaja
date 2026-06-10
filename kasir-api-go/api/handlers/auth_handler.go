package handlers

import (
	"context"
	"fmt"
	"log"
	"net/http"
	"os"
	"time"

	"github.com/gin-gonic/gin"
	"github.com/golang-jwt/jwt/v5"
	"github.com/jackc/pgx/v5/pgtype"
	"github.com/jackc/pgx/v5/pgxpool"
	"golang.org/x/crypto/bcrypt"
	"kasir-api-go/db"
	"kasir-api-go/utils"
)

type AuthHandler struct {
	queries *db.Queries
	pool    *pgxpool.Pool
}

func NewAuthHandler(queries *db.Queries, pool *pgxpool.Pool) *AuthHandler {
	return &AuthHandler{queries: queries, pool: pool}
}

type RegisterStoreRequest struct {
	FullName  string `json:"fullName" binding:"required"`
	Email     string `json:"email" binding:"required,email"`
	Phone     string `json:"phone" binding:"required"`
	Password  string `json:"password" binding:"required"`
	StoreName string `json:"storeName" binding:"required"`
	Address   string `json:"address" binding:"required"`
}

type VerifyOTPRequest struct {
	Email string `json:"email" binding:"required,email"`
	OTP   string `json:"otp" binding:"required"`
}

type ResendOTPRequest struct {
	Email string `json:"email" binding:"required,email"`
}

type LoginRequest struct {
	Email    string `json:"email" binding:"required,email"`
	Password string `json:"password" binding:"required"`
}

func (h *AuthHandler) RegisterStore(c *gin.Context) {
	var req RegisterStoreRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "Invalid request parameters"})
		return
	}

	// Check if email already exists
	_, err := h.queries.GetUserByEmail(c.Request.Context(), pgtype.Text{String: req.Email, Valid: true})
	if err == nil {
		c.JSON(http.StatusConflict, gin.H{"error": "Email already registered"})
		return
	}

	// Generate OTP
	otp := utils.GenerateOTP()

	// Hash Password before caching to avoid plain text in Redis
	hashedPassword, err := bcrypt.GenerateFromPassword([]byte(req.Password), bcrypt.DefaultCost)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to process password"})
		return
	}

	regData := utils.RegistrationData{
		FullName:  req.FullName,
		Email:     req.Email,
		Phone:     req.Phone,
		Password:  string(hashedPassword),
		StoreName: req.StoreName,
		Address:   req.Address,
		OTP:       otp,
	}

	if err := utils.SaveRegistrationData(c.Request.Context(), req.Email, regData); err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to save registration data"})
		return
	}

	// Send Email
	go func() {
		err := utils.SendOTPEmail(req.Email, otp)
		if err != nil {
			log.Printf("Failed to send OTP email to %s: %v", req.Email, err)
		}
	}()

	c.JSON(http.StatusOK, gin.H{"message": "OTP sent to email"})
}

func (h *AuthHandler) VerifyOTP(c *gin.Context) {
	var req VerifyOTPRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "Invalid request parameters"})
		return
	}

	regData, err := utils.GetRegistrationData(c.Request.Context(), req.Email)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "OTP expired or invalid email"})
		return
	}

	if regData.OTP != req.OTP {
		c.JSON(http.StatusUnauthorized, gin.H{"error": "Invalid OTP"})
		return
	}

	// Begin Transaction to save user and store
	tx, err := h.pool.Begin(c.Request.Context())
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to start transaction"})
		return
	}
	defer tx.Rollback(c.Request.Context())

	qtx := h.queries.WithTx(tx)

	// Create User
	user, err := qtx.CreateUser(c.Request.Context(), db.CreateUserParams{
		FullName:     regData.FullName,
		Email:        pgtype.Text{String: regData.Email, Valid: true},
		Phone:        pgtype.Text{String: regData.Phone, Valid: true},
		PasswordHash: regData.Password,
		Role:         "store",
		// StoreID will be updated after store creation
	})
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to create user"})
		return
	}

	// Create Store
	storeCode := "STORE-" + fmt.Sprintf("%x", user.ID.Bytes[0:4]) // Simple store code generation
	store, err := qtx.CreateStore(c.Request.Context(), db.CreateStoreParams{
		OwnerID:   user.ID,
		StoreCode: storeCode,
		StoreName: regData.StoreName,
		Address:   pgtype.Text{String: regData.Address, Valid: true},
		Phone:     pgtype.Text{String: regData.Phone, Valid: true},
	})
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to create store"})
		return
	}

	// Update User with Store ID
	err = qtx.UpdateUserStoreID(c.Request.Context(), db.UpdateUserStoreIDParams{
		ID:      user.ID,
		StoreID: store.ID,
	})
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to link store to user"})
		return
	}

	if err := tx.Commit(c.Request.Context()); err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to commit transaction"})
		return
	}

	// Delete OTP from Redis
	utils.DeleteRegistrationData(c.Request.Context(), req.Email)

	jwtSecret := os.Getenv("JWT_SECRET")
	if jwtSecret == "" {
		jwtSecret = "secret"
	}

	token := jwt.NewWithClaims(jwt.SigningMethodHS256, jwt.MapClaims{
		"user_id": user.ID.Bytes,
		"role":    user.Role,
		"exp":     time.Now().Add(time.Hour * 24).Unix(), // 24 hours
	})

	tokenString, err := token.SignedString([]byte(jwtSecret))
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to generate token"})
		return
	}

	c.JSON(http.StatusOK, gin.H{
		"message": "Registration successful",
		"token": tokenString,
		"user": gin.H{
			"id":        user.ID,
			"full_name": user.FullName,
			"email":     user.Email.String,
			"role":      user.Role,
			"store_id":  store.ID,
		},
	})
}

func (h *AuthHandler) ResendOTP(c *gin.Context) {
	var req ResendOTPRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "Invalid request parameters"})
		return
	}

	regData, err := utils.GetRegistrationData(c.Request.Context(), req.Email)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "Session expired, please register again"})
		return
	}

	// Generate new OTP
	newOtp := utils.GenerateOTP()
	regData.OTP = newOtp

	if err := utils.SaveRegistrationData(c.Request.Context(), req.Email, *regData); err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to update registration data"})
		return
	}

	// Send Email
	go func() {
		err := utils.SendOTPEmail(req.Email, newOtp)
		if err != nil {
			log.Printf("Failed to resend OTP email to %s: %v", req.Email, err)
		}
	}()

	c.JSON(http.StatusOK, gin.H{"message": "OTP resent to email"})
}

func (h *AuthHandler) Login(c *gin.Context) {
	var req LoginRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "Invalid request parameters"})
		return
	}

	user, err := h.queries.GetUserByEmail(c.Request.Context(), pgtype.Text{String: req.Email, Valid: true})
	if err != nil {
		c.JSON(http.StatusUnauthorized, gin.H{"error": "Invalid email or password"})
		return
	}

	if err := bcrypt.CompareHashAndPassword([]byte(user.PasswordHash), []byte(req.Password)); err != nil {
		c.JSON(http.StatusUnauthorized, gin.H{"error": "Invalid email or password"})
		return
	}

	if !user.IsActive.Bool {
		c.JSON(http.StatusUnauthorized, gin.H{"error": "Account is inactive"})
		return
	}

	jwtSecret := os.Getenv("JWT_SECRET")
	if jwtSecret == "" {
		jwtSecret = "secret"
	}

	token := jwt.NewWithClaims(jwt.SigningMethodHS256, jwt.MapClaims{
		"user_id": user.ID.Bytes,
		"role":    user.Role,
		"exp":     time.Now().Add(time.Hour * 24).Unix(), // 24 hours
	})

	tokenString, err := token.SignedString([]byte(jwtSecret))
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to generate token"})
		return
	}

	c.JSON(http.StatusOK, gin.H{
		"token": tokenString,
		"user": gin.H{
			"id":        user.ID,
			"full_name": user.FullName,
			"email":     user.Email.String,
			"role":      user.Role,
		},
	})
}

func InitAdminUser(queries *db.Queries) {
	adminEmail := os.Getenv("ADMIN_EMAIL")
	adminPassword := os.Getenv("ADMIN_PASSWORD")

	if adminEmail == "" || adminPassword == "" {
		log.Println("ADMIN_EMAIL or ADMIN_PASSWORD not set in .env. Skipping admin initialization.")
		return
	}

	_, err := queries.GetUserByEmail(context.Background(), pgtype.Text{String: adminEmail, Valid: true})
	if err == nil {
		log.Println("Admin user already exists. Skipping initialization.")
		return
	}

	hashedPassword, err := bcrypt.GenerateFromPassword([]byte(adminPassword), bcrypt.DefaultCost)
	if err != nil {
		log.Printf("Failed to hash admin password: %v\n", err)
		return
	}

	_, err = queries.CreateUser(context.Background(), db.CreateUserParams{
		FullName:     "Super Admin",
		Email:        pgtype.Text{String: adminEmail, Valid: true},
		PasswordHash: string(hashedPassword),
		Role:         "admin",
		Phone:        pgtype.Text{String: "-", Valid: true},
	})

	if err != nil {
		log.Printf("Failed to initialize admin user: %v\n", err)
	} else {
		log.Println("Admin user initialized successfully.")
	}
}
