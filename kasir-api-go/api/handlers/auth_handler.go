package handlers

import (
	"context"
	"fmt"
	"log"
	"net/http"
	"os"
	"strings"
	"time"

	"kasir-api-go/db"
	"kasir-api-go/utils"

	"github.com/gin-gonic/gin"
	"github.com/golang-jwt/jwt/v5"
	"github.com/google/uuid"
	"github.com/jackc/pgx/v5/pgtype"
	"github.com/jackc/pgx/v5/pgxpool"
	"golang.org/x/crypto/bcrypt"
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
	Email    string `json:"email" binding:"required"`
	Password string `json:"password" binding:"required"`
}

type ForgotPasswordRequest struct {
	Role       string `json:"role" binding:"required"`       // "owner" atau "kasir"
	Identifier string `json:"identifier" binding:"required"` // email (owner) / username (kasir)
}

type VerifyForgotOTPRequest struct {
	Role       string `json:"role" binding:"required"`
	Identifier string `json:"identifier" binding:"required"`
	OTP        string `json:"otp" binding:"required"`
}

type ResetPasswordRequest struct {
	Role            string `json:"role" binding:"required"`       // "owner" atau "kasir"
	Identifier      string `json:"identifier" binding:"required"` // email / username
	NewPassword     string `json:"newPassword" binding:"required"`
	ConfirmPassword string `json:"confirmPassword" binding:"required"`
}

func (h *AuthHandler) ForgotPassword(c *gin.Context) {
	var req ForgotPasswordRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "Parameter tidak valid"})
		return
	}

	req.Role = strings.ToLower(strings.TrimSpace(req.Role))
	req.Identifier = strings.TrimSpace(req.Identifier)

	var targetUser db.User
	var ownerEmail string
	var ownerName string
	var err error
	var emailCategory utils.OTPEmailCategory

	if req.Role == "owner" {
		targetUser, err = h.queries.GetUserByEmail(c.Request.Context(), pgtype.Text{String: req.Identifier, Valid: true})
		if err != nil {
			c.JSON(http.StatusNotFound, gin.H{"error": "Email owner tidak ditemukan"})
			return
		}
		if targetUser.Role != "owner" {
			c.JSON(http.StatusBadRequest, gin.H{"error": "Akun bukan merupakan akun owner"})
			return
		}
		ownerEmail = targetUser.Email.String
		ownerName = targetUser.FullName
		emailCategory = utils.OTPCategoryForgotPasswordOwner
	} else if req.Role == "kasir" || req.Role == "karyawan" {
		targetUser, err = h.queries.GetUserByIdentifier(c.Request.Context(), req.Identifier)
		if err != nil {
			c.JSON(http.StatusNotFound, gin.H{"error": "Username kasir/karyawan tidak ditemukan"})
			return
		}
		if targetUser.Role != "kasir" && targetUser.Role != "karyawan" {
			c.JSON(http.StatusBadRequest, gin.H{"error": "Akun bukan merupakan akun kasir/karyawan"})
			return
		}

		if !targetUser.StoreID.Valid {
			c.JSON(http.StatusBadRequest, gin.H{"error": "Kasir/karyawan tidak terhubung ke toko manapun"})
			return
		}

		owners, err := h.queries.ListStoreOwners(c.Request.Context(), targetUser.StoreID)
		if err != nil || len(owners) == 0 {
			c.JSON(http.StatusNotFound, gin.H{"error": "Email owner toko tidak ditemukan"})
			return
		}
		ownerEmail = owners[0].Email.String
		ownerName = owners[0].FullName
		emailCategory = utils.OTPCategoryForgotPasswordKasir
	} else {
		c.JSON(http.StatusBadRequest, gin.H{"error": "Role tidak valid"})
		return
	}

	if ownerEmail == "" {
		c.JSON(http.StatusBadRequest, gin.H{"error": "Email tujuan pengiriman OTP tidak valid"})
		return
	}

	otp := utils.GenerateOTP()

	targetUserIDUUID, _ := uuid.FromBytes(targetUser.ID.Bytes[:])

	resetData := utils.PasswordResetData{
		UserID:     targetUserIDUUID.String(),
		TargetUser: req.Identifier,
		Role:       req.Role,
		OwnerEmail: ownerEmail,
		OTP:        otp,
	}

	redisKey := fmt.Sprintf("%s:%s", req.Role, req.Identifier)
	if err := utils.SavePasswordResetData(c.Request.Context(), redisKey, resetData); err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Gagal menyimpan data reset password"})
		return
	}

	go func() {
		emailOpts := utils.OTPEmailOptions{
			ToEmail:          ownerEmail,
			OTP:              otp,
			Category:         emailCategory,
			RecipientName:    ownerName,
			TargetName:       targetUser.FullName,
			TargetIdentifier: req.Identifier,
		}
		err := utils.SendOTPEmail(emailOpts)
		if err != nil {
			log.Printf("Gagal mengirim email OTP lupa password ke %s: %v", ownerEmail, err)
		}
	}()

	c.JSON(http.StatusOK, gin.H{
		"message":     "OTP berhasil dikirim",
		"owner_email": ownerEmail,
	})
}

func (h *AuthHandler) VerifyForgotOTP(c *gin.Context) {
	var req VerifyForgotOTPRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "Parameter tidak valid"})
		return
	}

	req.Role = strings.ToLower(strings.TrimSpace(req.Role))
	req.Identifier = strings.TrimSpace(req.Identifier)

	// Brute-force protection OTP lupa password: 5 gagal dalam 15 menit.
	lockKey := "otp-reset:" + req.Role + ":" + req.Identifier
	if utils.IsLocked(c.Request.Context(), lockKey, 5) {
		c.JSON(http.StatusTooManyRequests, gin.H{"error": "Terlalu banyak percobaan OTP salah. Coba lagi nanti."})
		return
	}

	redisKey := fmt.Sprintf("%s:%s", req.Role, req.Identifier)
	resetData, err := utils.GetPasswordResetData(c.Request.Context(), redisKey)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "Sesi OTP telah kedaluwarsa atau tidak ditemukan"})
		return
	}

	if resetData.OTP != req.OTP {
		if _, locked := utils.RecordFailedAttempt(c.Request.Context(), lockKey, 5, 15*time.Minute); locked {
			c.JSON(http.StatusTooManyRequests, gin.H{"error": "Terlalu banyak percobaan OTP salah. Coba lagi nanti."})
			return
		}
		c.JSON(http.StatusUnauthorized, gin.H{"error": "Kode OTP tidak valid"})
		return
	}

	utils.ClearFailedAttempts(c.Request.Context(), lockKey)

	resetData.IsVerified = true
	if err := utils.SavePasswordResetData(c.Request.Context(), redisKey, *resetData); err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Gagal memperbarui status verifikasi OTP"})
		return
	}

	c.JSON(http.StatusOK, gin.H{"message": "OTP berhasil diverifikasi"})
}

func (h *AuthHandler) ResetPassword(c *gin.Context) {
	var req ResetPasswordRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "Parameter tidak valid"})
		return
	}

	if req.NewPassword != req.ConfirmPassword {
		c.JSON(http.StatusBadRequest, gin.H{"error": "Password baru dan konfirmasi password tidak cocok"})
		return
	}

	req.Role = strings.ToLower(strings.TrimSpace(req.Role))
	req.Identifier = strings.TrimSpace(req.Identifier)

	redisKey := fmt.Sprintf("%s:%s", req.Role, req.Identifier)
	resetData, err := utils.GetPasswordResetData(c.Request.Context(), redisKey)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "Sesi OTP telah kedaluwarsa atau tidak ditemukan"})
		return
	}

	if !resetData.IsVerified {
		c.JSON(http.StatusUnauthorized, gin.H{"error": "OTP belum diverifikasi"})
		return
	}

	hashedPassword, err := bcrypt.GenerateFromPassword([]byte(req.NewPassword), bcrypt.DefaultCost)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Gagal memproses password baru"})
		return
	}

	targetUUID, err := uuid.Parse(resetData.UserID)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "ID pengguna tidak valid"})
		return
	}

	err = h.queries.UpdateUserPassword(c.Request.Context(), db.UpdateUserPasswordParams{
		ID:           pgtype.UUID{Bytes: targetUUID, Valid: true},
		PasswordHash: string(hashedPassword),
	})
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Gagal memperbarui password"})
		return
	}

	utils.DeletePasswordResetData(c.Request.Context(), redisKey)

	c.JSON(http.StatusOK, gin.H{"message": "Password berhasil diperbarui"})
}

func (h *AuthHandler) RegisterStore(c *gin.Context) {
	var req RegisterStoreRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "Invalid request parameters"})
		return
	}

	// Sanitasi input teks user untuk mencegah XSS tersimpan.
	req.FullName = SanitizeText(req.FullName)
	req.StoreName = SanitizeText(req.StoreName)
	req.Address = SanitizeText(req.Address)
	req.Phone = SanitizeText(req.Phone)

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
		emailOpts := utils.OTPEmailOptions{
			ToEmail:       req.Email,
			OTP:           otp,
			Category:      utils.OTPCategoryRegistration,
			RecipientName: req.FullName,
		}
		err := utils.SendOTPEmail(emailOpts)
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

	email := strings.ToLower(strings.TrimSpace(req.Email))
	// Brute-force protection OTP: 5 gagal dalam 15 menit.
	lockKey := "otp-reg:" + email
	if utils.IsLocked(c.Request.Context(), lockKey, 5) {
		c.JSON(http.StatusTooManyRequests, gin.H{"error": "Terlalu banyak percobaan OTP salah. Coba lagi nanti."})
		return
	}

	regData, err := utils.GetRegistrationData(c.Request.Context(), email)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "OTP expired or invalid email"})
		return
	}

	if regData.OTP != req.OTP {
		if _, locked := utils.RecordFailedAttempt(c.Request.Context(), lockKey, 5, 15*time.Minute); locked {
			c.JSON(http.StatusTooManyRequests, gin.H{"error": "Terlalu banyak percobaan OTP salah. Coba lagi nanti."})
			return
		}
		c.JSON(http.StatusUnauthorized, gin.H{"error": "Invalid OTP"})
		return
	}

	utils.ClearFailedAttempts(c.Request.Context(), lockKey)

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
		Role:         "owner",
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

	jwtSecret := GetJWTSecret()

	parsedID, _ := uuid.FromBytes(user.ID.Bytes[:])
	storeIDStr := ""
	// Use store.ID directly since user.StoreID locally hasn't been updated
	parsedStoreID, _ := uuid.FromBytes(store.ID.Bytes[:])
	storeIDStr = parsedStoreID.String()

	token := jwt.NewWithClaims(jwt.SigningMethodHS256, jwt.MapClaims{
		"user_id":  parsedID.String(),
		"role":     user.Role,
		"store_id": storeIDStr,
	})

	tokenString, err := token.SignedString([]byte(jwtSecret))
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to generate token"})
		return
	}

	c.JSON(http.StatusOK, gin.H{
		"message": "Registration successful",
		"token":   tokenString,
		"user": gin.H{
			"id":            user.ID,
			"full_name":     user.FullName,
			"email":         user.Email.String,
			"role":          user.Role,
			"store_id":      store.ID,
			"store_name":    store.StoreName,
			"store_address": store.Address.String,
			"logo_url":      store.LogoUrl.String,
			"photo_url":     user.PhotoUrl.String,
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
		emailOpts := utils.OTPEmailOptions{
			ToEmail:       req.Email,
			OTP:           newOtp,
			Category:      utils.OTPCategoryRegistration,
			RecipientName: regData.FullName,
		}
		err := utils.SendOTPEmail(emailOpts)
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

	// Brute-force protection: kunci per email setelah 5 gagal dalam 15 menit.
	lockKey := "login:" + strings.ToLower(strings.TrimSpace(req.Email))
	if utils.IsLocked(c.Request.Context(), lockKey, 5) {
		c.JSON(http.StatusTooManyRequests, gin.H{"error": "Terlalu banyak percobaan login gagal. Coba lagi nanti."})
		return
	}

	user, err := h.queries.GetUserByEmail(c.Request.Context(), pgtype.Text{String: req.Email, Valid: true})
	if err != nil {
		// Tetap catat percobaan gagal agar email tidak diketahui ada/tidak.
		if _, locked := utils.RecordFailedAttempt(c.Request.Context(), lockKey, 5, 15*time.Minute); locked {
			c.JSON(http.StatusTooManyRequests, gin.H{"error": "Terlalu banyak percobaan login gagal. Coba lagi nanti."})
			return
		}
		c.JSON(http.StatusUnauthorized, gin.H{"error": "Invalid email or password"})
		return
	}

	if err := bcrypt.CompareHashAndPassword([]byte(user.PasswordHash), []byte(req.Password)); err != nil {
		if _, locked := utils.RecordFailedAttempt(c.Request.Context(), lockKey, 5, 15*time.Minute); locked {
			c.JSON(http.StatusTooManyRequests, gin.H{"error": "Terlalu banyak percobaan login gagal. Coba lagi nanti."})
			return
		}
		c.JSON(http.StatusUnauthorized, gin.H{"error": "Invalid email or password"})
		return
	}

	// Sukses: reset counter gagal.
	utils.ClearFailedAttempts(c.Request.Context(), lockKey)

	if !user.IsActive.Bool {
		c.JSON(http.StatusUnauthorized, gin.H{"error": "Account is inactive"})
		return
	}

	// Check Pro status if user is a cashier (kasir)
	if user.Role == "kasir" && user.StoreID.Valid {
		storeStatus, err := h.queries.GetStoreProStatus(c.Request.Context(), user.StoreID)
		if err != nil || !storeStatus.ProExpiresAt.Valid || storeStatus.ProExpiresAt.Time.Before(time.Now()) {
			c.JSON(http.StatusForbidden, gin.H{"error": "Masa aktif Pro toko telah berakhir. Sesi login karyawan dikunci. Silakan minta owner untuk melakukan perpanjangan Pro."})
			return
		}
	}

	jwtSecret := GetJWTSecret()

	parsedID, _ := uuid.FromBytes(user.ID.Bytes[:])
	storeIDStr := ""
	if user.StoreID.Valid {
		parsedStoreID, _ := uuid.FromBytes(user.StoreID.Bytes[:])
		storeIDStr = parsedStoreID.String()
	}

	token := jwt.NewWithClaims(jwt.SigningMethodHS256, jwt.MapClaims{
		"user_id":  parsedID.String(),
		"role":     user.Role,
		"store_id": storeIDStr,
	})

	tokenString, err := token.SignedString([]byte(jwtSecret))
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to generate token"})
		return
	}

	var storeName, storeAddress, storeLogoUrl string
	if user.StoreID.Valid {
		store, err := h.queries.GetStore(c.Request.Context(), user.StoreID)
		if err == nil {
			storeName = store.StoreName
			storeAddress = store.Address.String
			storeLogoUrl = store.LogoUrl.String
		}
	}

	c.JSON(http.StatusOK, gin.H{
		"token": tokenString,
		"user": gin.H{
			"id":            user.ID,
			"full_name":     user.FullName,
			"email":         user.Email.String,
			"role":          user.Role,
			"store_id":      user.StoreID,
			"store_name":    storeName,
			"store_address": storeAddress,
			"logo_url":      storeLogoUrl,
			"photo_url":     user.PhotoUrl.String,
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

type SwitchUserRequest struct {
	TargetUserID string `json:"target_user_id" binding:"required"`
	Password     string `json:"password"` // Required only when switching from cashier back to owner
}

func (h *AuthHandler) SwitchUser(c *gin.Context) {
	var req SwitchUserRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "Invalid request parameters"})
		return
	}

	targetUserIDUUID, err := uuid.Parse(req.TargetUserID)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "Invalid user ID"})
		return
	}

	// currentUserIDStr, _ := c.Get("user_id")
	currentUserRole, _ := c.Get("role")
	currentStoreID, _ := c.Get("store_id")

	// Get target user
	targetUser, err := h.queries.GetUser(c.Request.Context(), pgtype.UUID{Bytes: targetUserIDUUID, Valid: true})
	if err != nil {
		c.JSON(http.StatusNotFound, gin.H{"error": "Target user not found"})
		return
	}

	// Verify both users belong to the same store
	var targetStoreIDStr string
	if targetUser.StoreID.Valid {
		parsedStoreID, _ := uuid.FromBytes(targetUser.StoreID.Bytes[:])
		targetStoreIDStr = parsedStoreID.String()
	}

	if currentStoreID != targetStoreIDStr {
		c.JSON(http.StatusForbidden, gin.H{"error": "Users do not belong to the same store"})
		return
	}

	// Authorization logic
	if currentUserRole == "owner" {
		// Owner can switch to any user in the same store without password
	} else if currentUserRole == "kasir" && targetUser.Role == "owner" {
		// Cashier switching to owner requires owner's password
		if req.Password == "" {
			c.JSON(http.StatusBadRequest, gin.H{"error": "Password required to switch to owner"})
			return
		}

		if err := bcrypt.CompareHashAndPassword([]byte(targetUser.PasswordHash), []byte(req.Password)); err != nil {
			c.JSON(http.StatusUnauthorized, gin.H{"error": "Invalid password"})
			return
		}
	} else {
		// Cashier switching to another cashier? Or other cases. Disallow for now to keep it simple, or allow without password?
		// Usually cashier shouldn't switch to another cashier without logging out.
		c.JSON(http.StatusForbidden, gin.H{"error": "Not allowed to switch user"})
		return
	}

	// Generate new token for target user
	jwtSecret := GetJWTSecret()

	parsedID, _ := uuid.FromBytes(targetUser.ID.Bytes[:])

	token := jwt.NewWithClaims(jwt.SigningMethodHS256, jwt.MapClaims{
		"user_id":  parsedID.String(),
		"role":     targetUser.Role,
		"store_id": targetStoreIDStr,
	})

	tokenString, err := token.SignedString([]byte(jwtSecret))
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to generate token"})
		return
	}

	var storeName, storeAddress, storeLogoUrl string
	if targetUser.StoreID.Valid {
		store, err := h.queries.GetStore(c.Request.Context(), targetUser.StoreID)
		if err == nil {
			storeName = store.StoreName
			storeAddress = store.Address.String
			storeLogoUrl = store.LogoUrl.String
		}
	}

	c.JSON(http.StatusOK, gin.H{
		"message": "User switched successfully",
		"token":   tokenString,
		"user": gin.H{
			"id":            targetUser.ID,
			"full_name":     targetUser.FullName,
			"email":         targetUser.Email.String,
			"role":          targetUser.Role,
			"store_id":      targetUser.StoreID,
			"store_name":    storeName,
			"store_address": storeAddress,
			"logo_url":      storeLogoUrl,
			"photo_url":     targetUser.PhotoUrl.String,
		},
	})
}
