package handlers

import (
	"encoding/json"
	"io"
	"net/http"
	"strconv"
	"time"

	"github.com/gin-gonic/gin"
	"github.com/google/uuid"
	"github.com/jackc/pgx/v5/pgtype"
	"kasir-api-go/db"
	"kasir-api-go/utils"
)

type SubscriptionHandler struct {
	queries      *db.Queries
	tripayClient *utils.TripayClient
}

func NewSubscriptionHandler(queries *db.Queries) *SubscriptionHandler {
	return &SubscriptionHandler{
		queries:      queries,
		tripayClient: utils.NewTripayClient(),
	}
}

// --- STORE ENDPOINTS ---

func (h *SubscriptionHandler) GetPlans(c *gin.Context) {
	plans, err := h.queries.ListActiveSubscriptionPlans(c.Request.Context())
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Gagal mengambil daftar paket"})
		return
	}

	storeIDRaw, exists := c.Get("store_id")
	var proExpiresAt *time.Time
	isPro := false

	if exists && storeIDRaw != nil {
		var storeUUID uuid.UUID
		switch v := storeIDRaw.(type) {
		case uuid.UUID:
			storeUUID = v
		case string:
			storeUUID, _ = uuid.Parse(v)
		}

		if storeUUID != uuid.Nil {
			storeStatus, err := h.queries.GetStoreProStatus(c.Request.Context(), pgtype.UUID{Bytes: storeUUID, Valid: true})
			if err == nil && storeStatus.ProExpiresAt.Valid {
				t := storeStatus.ProExpiresAt.Time
				proExpiresAt = &t
				if t.After(time.Now()) {
					isPro = true
				}
			}
		}
	}

	c.JSON(http.StatusOK, gin.H{"plans": plans, "is_pro": isPro, "pro_expires_at": proExpiresAt})
}

func (h *SubscriptionHandler) GetPaymentChannels(c *gin.Context) {
	channels, err := h.tripayClient.GetPaymentChannels()
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Gagal mengambil kanal pembayaran: " + err.Error()})
		return
	}
	c.JSON(http.StatusOK, gin.H{"channels": channels})
}

func (h *SubscriptionHandler) GetPaymentInstructions(c *gin.Context) {
	code := c.Query("code")
	if code == "" {
		c.JSON(http.StatusBadRequest, gin.H{"error": "Parameter code diperlukan"})
		return
	}

	payCode := c.Query("pay_code")
	amountStr := c.Query("amount")
	var amount int64
	if amountStr != "" {
		amount, _ = strconv.ParseInt(amountStr, 10, 64)
	}

	instructions, err := h.tripayClient.GetPaymentInstructions(code, payCode, amount)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Gagal mengambil instruksi pembayaran: " + err.Error()})
		return
	}

	c.JSON(http.StatusOK, gin.H{"instructions": instructions})
}

func (h *SubscriptionHandler) GetFeeCalculator(c *gin.Context) {
	code := c.Query("code")
	amountStr := c.Query("amount")
	if code == "" || amountStr == "" {
		c.JSON(http.StatusBadRequest, gin.H{"error": "Parameter code dan amount diperlukan"})
		return
	}

	amount, err := strconv.ParseInt(amountStr, 10, 64)
	if err != nil || amount <= 0 {
		c.JSON(http.StatusBadRequest, gin.H{"error": "Amount tidak valid"})
		return
	}

	fees, err := h.tripayClient.GetFeeCalculator(code, amount)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Gagal menghitung biaya: " + err.Error()})
		return
	}

	c.JSON(http.StatusOK, gin.H{"fees": fees})
}

type CheckoutRequest struct {
	PlanID        int32  `json:"plan_id" binding:"required"`
	PaymentMethod string `json:"payment_method" binding:"required"`
}

func (h *SubscriptionHandler) Checkout(c *gin.Context) {
	storeIDStr := c.GetString("store_id")
	if storeIDStr == "" {
		c.JSON(http.StatusUnauthorized, gin.H{"error": "Store ID tidak ditemukan"})
		return
	}
	storeUUID, err := uuid.Parse(storeIDStr)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "Invalid Store ID"})
		return
	}

	var req CheckoutRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "Request tidak valid"})
		return
	}

	plan, err := h.queries.GetSubscriptionPlanByID(c.Request.Context(), req.PlanID)
	if err != nil {
		c.JSON(http.StatusNotFound, gin.H{"error": "Paket langganan tidak ditemukan"})
		return
	}

	merchantRef := "SUB-" + strconv.FormatInt(time.Now().UnixNano(), 10)

	userName := "Owner Toko"
	userEmail := "owner@kasirinaja.com"
	userPhone := ""

	userIDVal, exists := c.Get("user_id")
	if exists && userIDVal != nil {
		var userUUID pgtype.UUID
		switch v := userIDVal.(type) {
		case uuid.UUID:
			userUUID = pgtype.UUID{Bytes: v, Valid: true}
		case string:
			parsed, err := uuid.Parse(v)
			if err == nil {
				userUUID = pgtype.UUID{Bytes: parsed, Valid: true}
			}
		}

		if userUUID.Valid {
			user, err := h.queries.GetUser(c.Request.Context(), userUUID)
			if err == nil {
				if user.FullName != "" {
					userName = user.FullName
				}
				if user.Email.Valid && user.Email.String != "" {
					userEmail = user.Email.String
				}
				if user.Phone.Valid && user.Phone.String != "" {
					userPhone = user.Phone.String
				}
			}
		}
	}

	if userPhone == "" {
		store, err := h.queries.GetStore(c.Request.Context(), pgtype.UUID{Bytes: storeUUID, Valid: true})
		if err == nil && store.Phone.Valid && store.Phone.String != "" {
			userPhone = store.Phone.String
		}
	}
	if userPhone == "" {
		userPhone = "08123456789"
	}

	tripayReq := utils.TripayCreateTransactionRequest{
		Method:        req.PaymentMethod,
		MerchantRef:   merchantRef,
		Amount:        plan.Price,
		CustomerName:  userName,
		CustomerEmail: userEmail,
		CustomerPhone: userPhone,
		OrderItems: []utils.TripayTransactionItem{
			{
				SKU:      "PLAN-" + strconv.Itoa(int(plan.ID)),
				Name:     "Langganan " + plan.Name + " (" + strconv.Itoa(int(plan.DurationDays)) + " Hari)",
				Price:    plan.Price,
				Quantity: 1,
			},
		},
	}

	tripayResp, err := h.tripayClient.CreateTransaction(tripayReq)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "Gagal membuat transaksi Tripay: " + err.Error()})
		return
	}

	instructionsJSON, _ := json.Marshal(tripayResp.Instructions)
	expiresAt := time.Unix(tripayResp.ExpiredTime, 0)

	trx, err := h.queries.CreateSubscriptionTransaction(c.Request.Context(), db.CreateSubscriptionTransactionParams{
		Reference:        tripayResp.Reference,
		MerchantRef:      tripayResp.MerchantRef,
		StoreID:          pgtype.UUID{Bytes: storeUUID, Valid: true},
		PlanID:           plan.ID,
		Amount:           tripayResp.Amount,
		PaymentMethod:    tripayResp.PaymentMethod,
		PaymentName:      tripayResp.PaymentName,
		Status:           tripayResp.Status,
		PayCode:          tripayResp.PayCode,
		QrUrl:            tripayResp.QrURL,
		CheckoutUrl:      tripayResp.CheckoutURL,
		InstructionsJson: string(instructionsJSON),
		ExpiresAt:        pgtype.Timestamptz{Time: expiresAt, Valid: true},
	})
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Gagal menyimpan transaksi: " + err.Error()})
		return
	}

	c.JSON(http.StatusOK, gin.H{
		"message":      "Berhasil membuat transaksi",
		"transaction":  trx,
		"instructions": tripayResp.Instructions,
	})
}

func (h *SubscriptionHandler) GetTransactionByRef(c *gin.Context) {
	ref := c.Param("reference")
	if ref == "" {
		c.JSON(http.StatusBadRequest, gin.H{"error": "Reference diperlukan"})
		return
	}

	trx, err := h.queries.GetSubscriptionTransactionByReference(c.Request.Context(), ref)
	if err != nil {
		c.JSON(http.StatusNotFound, gin.H{"error": "Transaksi tidak ditemukan"})
		return
	}

	c.JSON(http.StatusOK, gin.H{"transaction": trx})
}

func (h *SubscriptionHandler) TripayCallback(c *gin.Context) {
	bodyBytes, err := io.ReadAll(c.Request.Body)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"success": false, "message": "Invalid body"})
		return
	}

	callbackSig := c.GetHeader("X-Callback-Signature")
	if callbackSig == "" {
		callbackSig = c.GetHeader("X-Tripay-Signature")
	}
	if !h.tripayClient.VerifyCallbackSignature(bodyBytes, callbackSig) {
		c.JSON(http.StatusUnauthorized, gin.H{"success": false, "message": "Invalid signature"})
		return
	}

	var callbackData struct {
		Reference   string `json:"reference"`
		MerchantRef string `json:"merchant_ref"`
		Status      string `json:"status"`
		PaidAt      int64  `json:"paid_at"`
	}

	if err := json.Unmarshal(bodyBytes, &callbackData); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"success": false, "message": "Invalid JSON"})
		return
	}

	trx, err := h.queries.GetSubscriptionTransactionByReference(c.Request.Context(), callbackData.Reference)
	if err != nil {
		c.JSON(http.StatusNotFound, gin.H{"success": false, "message": "Transaction not found"})
		return
	}

	if trx.Status == "PAID" {
		c.JSON(http.StatusOK, gin.H{"success": true, "message": "Already processed"})
		return
	}

	paidTime := time.Unix(callbackData.PaidAt, 0)
	if callbackData.PaidAt == 0 {
		paidTime = time.Now()
	}

	var paidTimeTz pgtype.Timestamptz
	if callbackData.Status == "PAID" {
		paidTimeTz = pgtype.Timestamptz{Time: paidTime, Valid: true}
	}

	updatedTrx, err := h.queries.UpdateSubscriptionTransactionStatus(c.Request.Context(), db.UpdateSubscriptionTransactionStatusParams{
		Status:    callbackData.Status,
		PaidAt:    paidTimeTz,
		Reference: callbackData.Reference,
	})
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"success": false, "message": "Failed to update transaction"})
		return
	}

	if callbackData.Status == "PAID" {
		storeStatus, err := h.queries.GetStoreProStatus(c.Request.Context(), updatedTrx.StoreID)
		if err == nil {
			var newExpiry time.Time
			now := time.Now()
			duration := time.Duration(trx.DurationDays) * 24 * time.Hour

			if storeStatus.ProExpiresAt.Valid && storeStatus.ProExpiresAt.Time.After(now) {
				newExpiry = storeStatus.ProExpiresAt.Time.Add(duration)
			} else {
				newExpiry = now.Add(duration)
			}

			_, _ = h.queries.UpdateStoreProExpiry(c.Request.Context(), db.UpdateStoreProExpiryParams{
				ProExpiresAt: pgtype.Timestamptz{Time: newExpiry, Valid: true},
				ID:           updatedTrx.StoreID,
			})

			NotifyProUpgrade(c.Request.Context(), h.queries, updatedTrx.StoreID)
		}
	}

	c.JSON(http.StatusOK, gin.H{"success": true})
}

// --- ADMIN ENDPOINTS ---

func (h *SubscriptionHandler) AdminListPlans(c *gin.Context) {
	plans, err := h.queries.ListAllSubscriptionPlans(c.Request.Context())
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Gagal mengambil daftar paket: " + err.Error()})
		return
	}
	c.JSON(http.StatusOK, gin.H{"plans": plans})
}

type CreatePlanRequest struct {
	Name         string `json:"name" binding:"required"`
	DurationDays int32  `json:"duration_days" binding:"required"`
	Price        int64  `json:"price" binding:"required"`
	Description  string `json:"description"`
	IsActive     bool   `json:"is_active"`
}

func (h *SubscriptionHandler) AdminCreatePlan(c *gin.Context) {
	var req CreatePlanRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "Request tidak valid"})
		return
	}

	plan, err := h.queries.CreateSubscriptionPlan(c.Request.Context(), db.CreateSubscriptionPlanParams{
		Name:         req.Name,
		DurationDays: req.DurationDays,
		Price:        req.Price,
		Description:  req.Description,
		IsActive:     req.IsActive,
	})
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Gagal membuat paket: " + err.Error()})
		return
	}

	c.JSON(http.StatusCreated, gin.H{"message": "Paket berhasil dibuat", "plan": plan})
}

type UpdatePlanRequest struct {
	Name         string `json:"name" binding:"required"`
	DurationDays int32  `json:"duration_days" binding:"required"`
	Price        int64  `json:"price" binding:"required"`
	Description  string `json:"description"`
	IsActive     bool   `json:"is_active"`
}

func (h *SubscriptionHandler) AdminUpdatePlan(c *gin.Context) {
	idStr := c.Param("id")
	id, err := strconv.Atoi(idStr)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "ID tidak valid"})
		return
	}

	var req UpdatePlanRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "Request tidak valid"})
		return
	}

	plan, err := h.queries.UpdateSubscriptionPlan(c.Request.Context(), db.UpdateSubscriptionPlanParams{
		Name:         req.Name,
		DurationDays: req.DurationDays,
		Price:        req.Price,
		Description:  req.Description,
		IsActive:     req.IsActive,
		ID:           int32(id),
	})
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Gagal memperbarui paket: " + err.Error()})
		return
	}

	c.JSON(http.StatusOK, gin.H{"message": "Paket berhasil diperbarui", "plan": plan})
}

func (h *SubscriptionHandler) AdminDeletePlan(c *gin.Context) {
	idStr := c.Param("id")
	id, err := strconv.Atoi(idStr)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "ID tidak valid"})
		return
	}

	err = h.queries.DeleteSubscriptionPlan(c.Request.Context(), int32(id))
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Gagal menghapus paket: " + err.Error()})
		return
	}

	c.JSON(http.StatusOK, gin.H{"message": "Paket berhasil dihapus"})
}

func (h *SubscriptionHandler) AdminListTransactions(c *gin.Context) {
	trxs, err := h.queries.ListSubscriptionTransactions(c.Request.Context())
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Gagal mengambil daftar transaksi: " + err.Error()})
		return
	}
	c.JSON(http.StatusOK, gin.H{"transactions": trxs})
}
