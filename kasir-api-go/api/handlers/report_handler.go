package handlers

import (
	"log"
	"net/http"
	"time"

	"github.com/gin-gonic/gin"
	"github.com/google/uuid"
	"github.com/jackc/pgx/v5/pgtype"
	"kasir-api-go/db"
)

type ReportHandler struct {
	q *db.Queries
}

func NewReportHandler(q *db.Queries) *ReportHandler {
	return &ReportHandler{q: q}
}

type SubmitReportRequest struct {
	StartTime         time.Time `json:"start_time" binding:"required"`
	EndTime           time.Time `json:"end_time" binding:"required"`
	TotalTransactions int32     `json:"total_transactions" binding:"required"`
	TotalRevenue      int64     `json:"total_revenue" binding:"required"`
	TotalProfit       int64     `json:"total_profit" binding:"required"`
}

func (h *ReportHandler) SubmitReport(c *gin.Context) {
	storeIDStr := c.GetString("store_id")
	userIDStr := c.GetString("user_id")

	storeID, err := uuid.Parse(storeIDStr)
	if err != nil {
		c.JSON(http.StatusUnauthorized, gin.H{"error": "Invalid store context"})
		return
	}
	userID, err := uuid.Parse(userIDStr)
	if err != nil {
		c.JSON(http.StatusUnauthorized, gin.H{"error": "Invalid user context"})
		return
	}

	pgStoreID := pgtype.UUID{Bytes: storeID, Valid: true}
	pgUserID := pgtype.UUID{Bytes: userID, Valid: true}

	var req SubmitReportRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}

	user, err := h.q.GetUser(c.Request.Context(), pgUserID)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to get user"})
		return
	}

	// Create the report
	report, err := h.q.CreateCashierReport(c.Request.Context(), db.CreateCashierReportParams{
		StoreID:           pgStoreID,
		CashierID:         pgUserID,
		CashierName:       user.FullName,
		StartTime:         pgtype.Timestamptz{Time: req.StartTime, Valid: true},
		EndTime:           pgtype.Timestamptz{Time: req.EndTime, Valid: true},
		TotalTransactions: req.TotalTransactions,
		TotalRevenue:      req.TotalRevenue,
		TotalProfit:       req.TotalProfit,
	})
	if err != nil {
		log.Printf("Error creating report: %v", err)
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to create report"})
		return
	}

	// Mark transactions as reported
	err = h.q.MarkTransactionsAsReported(c.Request.Context(), db.MarkTransactionsAsReportedParams{
		StoreID:   pgStoreID,
		CashierID: pgUserID,
	})
	if err != nil {
		log.Printf("Error marking transactions: %v", err)
		// We don't fail the request here, but log it
	}

	c.JSON(http.StatusOK, gin.H{
		"message": "Report submitted successfully",
		"report":  report,
	})
}

func (h *ReportHandler) GetStoreReports(c *gin.Context) {
	storeIDStr := c.GetString("store_id")
	storeID, err := uuid.Parse(storeIDStr)
	if err != nil {
		c.JSON(http.StatusUnauthorized, gin.H{"error": "Invalid store context"})
		return
	}

	pgStoreID := pgtype.UUID{Bytes: storeID, Valid: true}

	reports, err := h.q.GetCashierReportsByStore(c.Request.Context(), pgStoreID)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to fetch reports"})
		return
	}

    if reports == nil {
        reports = []db.CashierReport{}
    }

	c.JSON(http.StatusOK, gin.H{
		"reports": reports,
	})
}
