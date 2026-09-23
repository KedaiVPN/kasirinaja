package handlers

import (
	"log"
	"net/http"
	"os"
	"path/filepath"
	"strings"
	"time"

	"kasir-api-go/db"

	"github.com/gin-gonic/gin"
	"github.com/google/uuid"
	"github.com/jackc/pgx/v5/pgtype"
	"github.com/jackc/pgx/v5/pgxpool"
)

type AdminHandler struct {
	queries *db.Queries
	pool    *pgxpool.Pool
}

func NewAdminHandler(queries *db.Queries, pool *pgxpool.Pool) *AdminHandler {
	return &AdminHandler{queries: queries, pool: pool}
}

type UpdateProRequest struct {
	Days int `json:"days"`
}

func (h *AdminHandler) GetDashboardStats(c *gin.Context) {
	approvedCount, err := h.queries.CountMasterProducts(c.Request.Context())
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to count approved products"})
		return
	}

	pendingCount, err := h.queries.CountPendingProducts(c.Request.Context())
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to count pending products"})
		return
	}

	totalStores, err := h.queries.CountTotalStores(c.Request.Context())
	if err != nil {
		totalStores = 0
	}

	proStores, err := h.queries.CountProStores(c.Request.Context())
	if err != nil {
		proStores = 0
	}

	nonProStores, err := h.queries.CountNonProStores(c.Request.Context())
	if err != nil {
		nonProStores = 0
	}

	c.JSON(http.StatusOK, gin.H{
		"approved_count": approvedCount,
		"pending_count":  pendingCount,
		"total_stores":   totalStores,
		"pro_stores":     proStores,
		"non_pro_stores": nonProStores,
	})
}

func (h *AdminHandler) ListStores(c *gin.Context) {
	stores, err := h.queries.ListAdminStores(c.Request.Context())
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to fetch stores"})
		return
	}

	if stores == nil {
		stores = []db.AdminStoreListItem{}
	}

	c.JSON(http.StatusOK, stores)
}

func (h *AdminHandler) GetStoreDetail(c *gin.Context) {
	idParam := c.Param("id")
	id, err := uuid.Parse(idParam)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "Invalid store ID"})
		return
	}

	detail, err := h.queries.GetAdminStoreDetail(c.Request.Context(), pgtype.UUID{Bytes: id, Valid: true})
	if err != nil {
		c.JSON(http.StatusNotFound, gin.H{"error": "Store not found"})
		return
	}

	c.JSON(http.StatusOK, detail)
}

func (h *AdminHandler) UpdateStoreProStatus(c *gin.Context) {
	idParam := c.Param("id")
	id, err := uuid.Parse(idParam)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "Invalid store ID"})
		return
	}

	var req UpdateProRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "Invalid request body"})
		return
	}

	storeUUID := pgtype.UUID{Bytes: id, Valid: true}
	storeStatus, err := h.queries.GetStoreProStatus(c.Request.Context(), storeUUID)
	if err != nil {
		c.JSON(http.StatusNotFound, gin.H{"error": "Store not found"})
		return
	}

	now := time.Now()
	var newExpiry time.Time

	if req.Days > 0 {
		duration := time.Duration(req.Days) * 24 * time.Hour
		if storeStatus.ProExpiresAt.Valid && storeStatus.ProExpiresAt.Time.After(now) {
			newExpiry = storeStatus.ProExpiresAt.Time.Add(duration)
		} else {
			newExpiry = now.Add(duration)
		}
	} else {
		newExpiry = now
	}

	updatedStore, err := h.queries.UpdateStoreProExpiry(c.Request.Context(), db.UpdateStoreProExpiryParams{
		ProExpiresAt: pgtype.Timestamptz{Time: newExpiry, Valid: true},
		ID:           storeUUID,
	})
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to update store Pro status"})
		return
	}

	if req.Days > 0 {
		NotifyProUpgrade(c.Request.Context(), h.queries, storeUUID)
	}

	c.JSON(http.StatusOK, gin.H{
		"message":        "Pro status updated successfully",
		"store_id":       updatedStore.ID,
		"pro_expires_at": updatedStore.ProExpiresAt,
	})
}

func (h *AdminHandler) ApproveProduct(c *gin.Context) {
	idParam := c.Param("id")
	id, err := uuid.Parse(idParam)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "Invalid product ID"})
		return
	}

	pendingProduct, err := h.queries.GetPendingProduct(c.Request.Context(), pgtype.UUID{Bytes: id, Valid: true})
	if err != nil {
		c.JSON(http.StatusNotFound, gin.H{"error": "Pending product not found"})
		return
	}

	// Assuming user ID from token
	userIDStr, exists := c.Get("user_id")
	var userIDBytes [16]byte

	if exists {
		if idStr, ok := userIDStr.(string); ok {
			parsedUUID, err := uuid.Parse(idStr)
			if err == nil {
				userIDBytes = parsedUUID
			}
		} else if idBytes, ok := userIDStr.([]interface{}); ok {
			// JWT parses byte array as []interface{}
			if len(idBytes) == 16 {
				for i, v := range idBytes {
					if floatVal, ok := v.(float64); ok {
						userIDBytes[i] = byte(floatVal)
					}
				}
			}
		}
	}

	// Process category
	var categoryID pgtype.UUID
	categoryName := pendingProduct.Category
	if categoryName != "" {
		category, err := h.queries.GetCategoryByName(c.Request.Context(), categoryName)
		if err != nil {
			// Category doesn't exist, create it
			slug := strings.ToLower(strings.ReplaceAll(categoryName, " ", "-"))
			newCategory, err := h.queries.CreateCategory(c.Request.Context(), db.CreateCategoryParams{
				Name: categoryName,
				Slug: slug,
			})
			if err == nil {
				categoryID = newCategory.ID
			} else {
				categoryID = pgtype.UUID{Valid: false}
			}
		} else {
			categoryID = category.ID
		}
	} else {
		categoryID = pgtype.UUID{Valid: false}
	}

	// Create master product
	arg := db.CreateMasterProductParams{
		Barcode:            pendingProduct.Barcode.String,
		Name:               pendingProduct.Name,
		PhotoUrl:           pendingProduct.ImageUrl,
		PhotoPath:          pgtype.Text{Valid: false}, // We could derive this from URL, but keeping simple
		CategoryID:         categoryID,
		BrandID:            pgtype.UUID{Valid: false},
		Unit:               pgtype.Text{String: "pcs", Valid: true}, // Default unit
		Source:             pgtype.Text{String: "store_request", Valid: true},
		IsGeneratedBarcode: pgtype.Bool{Bool: false, Valid: true},
		CreatedBy:          pgtype.UUID{Bytes: userIDBytes, Valid: true},
	}

	masterProduct, err := h.queries.CreateMasterProduct(c.Request.Context(), arg)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to create master product"})
		return
	}

	// Add product to store_products for the store that requested it
	if pendingProduct.StoreID.Valid {
		storeProductArg := db.CreateStoreProductParams{
			StoreID:         pendingProduct.StoreID,
			MasterProductID: masterProduct.ID,
			BuyPrice:        pendingProduct.BuyPrice,
			SellPrice:       pendingProduct.SellPrice,
			Stock:           pendingProduct.Stock,
			MinStock:        0, // Default min stock
			LocalName:       pgtype.Text{String: pendingProduct.Name, Valid: true},
			LocalCategory:   pgtype.Text{String: pendingProduct.Category, Valid: true},
		}

		_, err = h.queries.CreateStoreProduct(c.Request.Context(), storeProductArg)
		if err != nil {
			log.Printf("Failed to add approved product %v to store %v: %v\n", masterProduct.ID, pendingProduct.StoreID, err)
			// Continue execution, as the master product is already created
		}
	}

	// Delete from pending
	err = h.queries.DeletePendingProduct(c.Request.Context(), pendingProduct.ID)
	if err != nil {
		// Log error, but we already created the master product
		log.Printf("Failed to delete pending product %v after approval: %v\n", pendingProduct.ID, err)
	}

	c.JSON(http.StatusOK, masterProduct)
}

func (h *AdminHandler) RejectProduct(c *gin.Context) {
	idParam := c.Param("id")
	id, err := uuid.Parse(idParam)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "Invalid product ID"})
		return
	}

	pendingProduct, err := h.queries.GetPendingProduct(c.Request.Context(), pgtype.UUID{Bytes: id, Valid: true})
	if err != nil {
		c.JSON(http.StatusNotFound, gin.H{"error": "Pending product not found"})
		return
	}

	// Delete image file if exists and is local
	if pendingProduct.ImageUrl.Valid {
		url := pendingProduct.ImageUrl.String
		if strings.HasPrefix(url, "/uploads/") {
			filePath := "." + url
			err := os.Remove(filePath)
			if err != nil {
				log.Printf("Failed to delete image file %s: %v\n", filePath, err)
			}
		}
	}

	err = h.queries.DeletePendingProduct(c.Request.Context(), pendingProduct.ID)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to delete pending product"})
		return
	}

	c.JSON(http.StatusOK, gin.H{"message": "Product rejected and deleted successfully"})
}

// DeleteStore handles the admin action to permanently delete a store and all its cascade dependencies
func (h *AdminHandler) DeleteStore(c *gin.Context) {
	idParam := c.Param("id")
	id, err := uuid.Parse(idParam)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "Invalid store ID"})
		return
	}
	storeUUID := pgtype.UUID{Bytes: id, Valid: true}

	ctx := c.Request.Context()

	// Begin transaction
	tx, err := h.pool.Begin(ctx)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to start transaction: " + err.Error()})
		return
	}
	defer tx.Rollback(ctx)

	q := h.queries.WithTx(tx)

	// 1) Fetch store + collect logo file
	store, err := q.GetStore(ctx, storeUUID)
	if err != nil {
		c.JSON(http.StatusNotFound, gin.H{"error": "Store not found"})
		return
	}

	var filesToDelete []string
	if store.LogoUrl.Valid && strings.HasPrefix(store.LogoUrl.String, "/uploads/") {
		filesToDelete = append(filesToDelete, "."+store.LogoUrl.String)
	}

	// 2) Collect user/owner files BEFORE deleting users
	var userIDsToDelete []pgtype.UUID
	if store.OwnerID.Valid {
		owner, err := q.GetUser(ctx, store.OwnerID)
		if err == nil && owner.PhotoUrl.Valid && strings.HasPrefix(owner.PhotoUrl.String, "/uploads/") {
			filesToDelete = append(filesToDelete, "."+owner.PhotoUrl.String)
		}
		userIDsToDelete = append(userIDsToDelete, store.OwnerID)
	}

	cashiers, _ := q.ListUsersByStore(ctx, storeUUID)
	for _, u := range cashiers {
		if u.PhotoUrl.Valid && strings.HasPrefix(u.PhotoUrl.String, "/uploads/") {
			filesToDelete = append(filesToDelete, "."+u.PhotoUrl.String)
		}
		userIDsToDelete = append(userIDsToDelete, u.ID)
	}

	// 3) Collect pending product images for this store
	pending, _ := q.ListPendingProductsByStore(ctx, storeUUID)
	for _, p := range pending {
		if p.ImageUrl.Valid && strings.HasPrefix(p.ImageUrl.String, "/uploads/") {
			filesToDelete = append(filesToDelete, "."+p.ImageUrl.String)
		}
	}

	// 4) CASCADE DELETE inside transaction — child rows first, store last
	//    because stores.owner_id REFERENCES users(id) blocks deleting owner before store.
	exec := func(query string, args ...interface{}) {
		_, err := tx.Exec(ctx, query, args...)
		if err != nil {
			log.Printf("DeleteStore tx.Exec error (%s): %v", query, err)
		}
	}

	// subscription_transactions has ON DELETE CASCADE on store_id but explicit is safer
	exec("DELETE FROM subscription_transactions WHERE store_id = $1", storeUUID)
	// cashier_reports also cascades, but explicit
	exec("DELETE FROM cashier_reports WHERE store_id = $1", storeUUID)
	// transaction_items -> transactions
	exec("DELETE FROM transaction_items WHERE transaction_id IN (SELECT id FROM transactions WHERE store_id = $1)", storeUUID)
	exec("DELETE FROM transactions WHERE store_id = $1", storeUUID)
	// stock_movements depend on store and store_products
	exec("DELETE FROM stock_movements WHERE store_id = $1", storeUUID)
	// pending_products: delete rows whose image belongs to this store folder as fallback
	exec("DELETE FROM pending_products WHERE store_id = $1", storeUUID)
	exec("DELETE FROM pending_products WHERE image_url LIKE '/uploads/' || $1 || '/%'", idParam)
	// store_products
	exec("DELETE FROM store_products WHERE store_id = $1", storeUUID)

	// 5) Delete the store itself (after all dependent rows are gone)
	if err := q.DeleteStore(ctx, storeUUID); err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to delete store row: " + err.Error()})
		return
	}

	// 6) NOW delete users (owner + cashiers) because stores no longer references them
	for _, uid := range userIDsToDelete {
		_, _ = tx.Exec(ctx, "DELETE FROM users WHERE id = $1", uid)
	}

	if err := tx.Commit(ctx); err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Transaction commit failed: " + err.Error()})
		return
	}

	// 7) Delete files/folder after successful commit
	for _, f := range filesToDelete {
		_ = os.Remove(f)
	}
	uploadsDir := filepath.Join(".", "uploads", idParam)
	_ = os.RemoveAll(uploadsDir)

	c.JSON(http.StatusOK, gin.H{"message": "Store and all related data deleted successfully"})
}
