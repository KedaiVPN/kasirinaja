package handlers

import (
	"context"
	"net/http"
	"time"

	"github.com/gin-gonic/gin"
	"github.com/google/uuid"
	"github.com/jackc/pgx/v5/pgtype"
	"github.com/jackc/pgx/v5/pgxpool"
	"kasir-api-go/db"
)

type TransactionHandler struct {
	queries *db.Queries
	pool    *pgxpool.Pool
}

func NewTransactionHandler(queries *db.Queries, pool *pgxpool.Pool) *TransactionHandler {
	return &TransactionHandler{queries: queries, pool: pool}
}

type CreateTransactionItemRequest struct {
	StoreProductID  string `json:"store_product_id" binding:"required"`
	MasterProductID string `json:"master_product_id" binding:"required"`
	ProductName     string `json:"product_name" binding:"required"`
	Barcode         string `json:"barcode" binding:"required"`
	Quantity        int32  `json:"quantity" binding:"required"`
	BuyPrice        string `json:"buy_price" binding:"required"`
	SellPrice       string `json:"sell_price" binding:"required"`
	Subtotal        string `json:"subtotal" binding:"required"`
}

type CreateTransactionRequest struct {
	StoreID         string                         `json:"store_id" binding:"required"`
	CashierID       string                         `json:"cashier_id" binding:"required"`
	InvoiceNumber   string                         `json:"invoice_number" binding:"required"`
	TotalAmount     string                         `json:"total_amount" binding:"required"`
	PaidAmount      string                         `json:"paid_amount" binding:"required"`
	ChangeAmount    string                         `json:"change_amount" binding:"required"`
	PaymentMethod   string                         `json:"payment_method" binding:"required"`
	SyncStatus      string                         `json:"sync_status" binding:"required"`
	DeviceID        string                         `json:"device_id"`
	TransactionTime string                         `json:"transaction_time" binding:"required"`
	Items           []CreateTransactionItemRequest `json:"items" binding:"required,min=1"`
}

func (h *TransactionHandler) CreateTransaction(c *gin.Context) {
	var req CreateTransactionRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}

	storeID, err := uuid.Parse(req.StoreID)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "invalid store id"})
		return
	}

	cashierID, err := uuid.Parse(req.CashierID)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "invalid cashier id"})
		return
	}

	transactionTime, err := time.Parse(time.RFC3339, req.TransactionTime)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "invalid transaction time format, use RFC3339"})
		return
	}

	var totalAmount, paidAmount, changeAmount pgtype.Numeric
	totalAmount.Scan(req.TotalAmount)
	paidAmount.Scan(req.PaidAmount)
	changeAmount.Scan(req.ChangeAmount)

	// Use database transaction
	ctx := context.Background()
	tx, err := h.pool.Begin(ctx)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "failed to begin transaction"})
		return
	}
	defer tx.Rollback(ctx)

	qtx := h.queries.WithTx(tx)

	// Create transaction
	arg := db.CreateTransactionParams{
		StoreID:         pgtype.UUID{Bytes: storeID, Valid: true},
		CashierID:       pgtype.UUID{Bytes: cashierID, Valid: true},
		InvoiceNumber:   req.InvoiceNumber,
		TotalAmount:     totalAmount,
		PaidAmount:      paidAmount,
		ChangeAmount:    changeAmount,
		PaymentMethod:   req.PaymentMethod,
		TransactionTime: pgtype.Timestamp{Time: transactionTime, Valid: true},
		SyncStatus:      req.SyncStatus,
		DeviceID:        pgtype.Text{String: req.DeviceID, Valid: req.DeviceID != ""},
	}

	createdTx, err := qtx.CreateTransaction(ctx, arg)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
		return
	}

	// Create transaction items
	for _, item := range req.Items {
		spID, err := uuid.Parse(item.StoreProductID)
		if err != nil {
			c.JSON(http.StatusBadRequest, gin.H{"error": "invalid store product id in items"})
			return
		}
		mpID, err := uuid.Parse(item.MasterProductID)
		if err != nil {
			c.JSON(http.StatusBadRequest, gin.H{"error": "invalid master product id in items"})
			return
		}

		var bPrice, sPrice, stotal pgtype.Numeric
		bPrice.Scan(item.BuyPrice)
		sPrice.Scan(item.SellPrice)
		stotal.Scan(item.Subtotal)

		itemArg := db.CreateTransactionItemParams{
			TransactionID:   createdTx.ID,
			StoreProductID:  pgtype.UUID{Bytes: spID, Valid: true},
			MasterProductID: pgtype.UUID{Bytes: mpID, Valid: true},
			ProductName:     item.ProductName,
			Barcode:         item.Barcode,
			Quantity:        item.Quantity,
			BuyPrice:        bPrice,
			SellPrice:       sPrice,
			Subtotal:        stotal,
		}

		_, err = qtx.CreateTransactionItem(ctx, itemArg)
		if err != nil {
			c.JSON(http.StatusInternalServerError, gin.H{"error": "failed to create transaction item: " + err.Error()})
			return
		}
	}

	err = tx.Commit(ctx)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "failed to commit transaction"})
		return
	}

	c.JSON(http.StatusCreated, createdTx)
}
