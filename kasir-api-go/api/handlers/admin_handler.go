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
// DeleteStore handles the admin action to permanently delete a store and all its cascade dependencies
func (h *AdminHandler) DeleteStore(c *gin.Context) {
	idParam := c.Param("id")
	log.Printf("[DELETE_STORE] === Starting DeleteStore for store_id: %s ===", idParam)

	id, err := uuid.Parse(idParam)
	if err != nil {
		log.Printf("[DELETE_STORE] ERROR: Invalid store UUID parameter '%s': %v", idParam, err)
		c.JSON(http.StatusBadRequest, gin.H{"error": "Invalid store ID: " + err.Error()})
		return
	}
	storeUUID := pgtype.UUID{Bytes: id, Valid: true}

	ctx := c.Request.Context()

	if h.pool == nil {
		log.Printf("[DELETE_STORE] CRITICAL ERROR: h.pool is NIL! Check routes initialization.")
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Database connection pool is nil"})
		return
	}

	// Begin transaction
	log.Printf("[DELETE_STORE] Step 0: Starting database transaction...")
	tx, err := h.pool.Begin(ctx)
	if err != nil {
		log.Printf("[DELETE_STORE] ERROR starting transaction: %v", err)
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to start transaction: " + err.Error()})
		return
	}
	defer tx.Rollback(ctx)

	q := h.queries.WithTx(tx)

	// 1) Fetch store + collect logo file
	log.Printf("[DELETE_STORE] Step 1: Fetching store metadata for UUID: %s", idParam)
	store, err := q.GetStore(ctx, storeUUID)
	if err != nil {
		log.Printf("[DELETE_STORE] ERROR fetching store (ID: %s): %v", idParam, err)
		c.JSON(http.StatusNotFound, gin.H{"error": "Store not found: " + err.Error()})
		return
	}
	log.Printf("[DELETE_STORE] Found store: name='%s', owner_id_valid=%v", store.StoreName, store.OwnerID.Valid)

	var filesToDelete []string
	if store.LogoUrl.Valid && strings.HasPrefix(store.LogoUrl.String, "/uploads/") {
		filesToDelete = append(filesToDelete, "."+store.LogoUrl.String)
		log.Printf("[DELETE_STORE] Will delete logo file: %s", store.LogoUrl.String)
	}

	// 2) Collect user/owner files BEFORE deleting users
	log.Printf("[DELETE_STORE] Step 2: Collecting user & owner IDs for store...")
	var userIDsToDelete []pgtype.UUID
	if store.OwnerID.Valid {
		owner, err := q.GetUser(ctx, store.OwnerID)
		if err == nil {
			log.Printf("[DELETE_STORE] Found owner user ID: %v", store.OwnerID)
			if owner.PhotoUrl.Valid && strings.HasPrefix(owner.PhotoUrl.String, "/uploads/") {
				filesToDelete = append(filesToDelete, "."+owner.PhotoUrl.String)
			}
			userIDsToDelete = append(userIDsToDelete, store.OwnerID)
		} else {
			log.Printf("[DELETE_STORE] WARNING: GetUser for owner_id failed: %v", err)
		}
	}

	cashiers, err := q.ListUsersByStore(ctx, storeUUID)
	if err != nil {
		log.Printf("[DELETE_STORE] WARNING: ListUsersByStore failed: %v", err)
	} else {
		log.Printf("[DELETE_STORE] Found %d cashiers/employees linked to store", len(cashiers))
		for _, u := range cashiers {
			if u.PhotoUrl.Valid && strings.HasPrefix(u.PhotoUrl.String, "/uploads/") {
				filesToDelete = append(filesToDelete, "."+u.PhotoUrl.String)
			}
			userIDsToDelete = append(userIDsToDelete, u.ID)
		}
	}

	// 3) Collect pending product images for this store
	log.Printf("[DELETE_STORE] Step 3: Checking pending products...")
	pending, err := q.ListPendingProductsByStore(ctx, storeUUID)
	if err != nil {
		log.Printf("[DELETE_STORE] WARNING: ListPendingProductsByStore failed: %v", err)
	} else {
		log.Printf("[DELETE_STORE] Found %d pending products", len(pending))
		for _, p := range pending {
			if p.ImageUrl.Valid && strings.HasPrefix(p.ImageUrl.String, "/uploads/") {
				filesToDelete = append(filesToDelete, "."+p.ImageUrl.String)
			}
		}
	}

	// 4) CASCADE DELETE inside transaction — child rows first, store last
	log.Printf("[DELETE_STORE] Step 4: Running CASCADE SQL deletions for child tables...")
	execStep := func(stepName string, query string, args ...interface{}) error {
		tag, execErr := tx.Exec(ctx, query, args...)
		if execErr != nil {
			log.Printf("[DELETE_STORE] ERROR at cascade step '%s' (Query: %s): %v", stepName, query, execErr)
			return execErr
		}
		log.Printf("[DELETE_STORE] CASCADE OK [%s]: deleted %d rows", stepName, tag.RowsAffected())
		return nil
	}

	cascadeQueries := []struct {
		name  string
		query string
		args  []interface{}
	}{
		{"subscription_transactions", "DELETE FROM subscription_transactions WHERE store_id = $1", []interface{}{storeUUID}},
		{"cashier_reports", "DELETE FROM cashier_reports WHERE store_id = $1", []interface{}{storeUUID}},
		{"transaction_items", "DELETE FROM transaction_items WHERE transaction_id IN (SELECT id FROM transactions WHERE store_id = $1)", []interface{}{storeUUID}},
		{"transactions", "DELETE FROM transactions WHERE store_id = $1", []interface{}{storeUUID}},
		{"stock_movements", "DELETE FROM stock_movements WHERE store_id = $1", []interface{}{storeUUID}},
		{"pending_products_by_store_id", "DELETE FROM pending_products WHERE store_id = $1", []interface{}{storeUUID}},
		{"pending_products_by_image_path", "DELETE FROM pending_products WHERE image_url LIKE '/uploads/' || $1 || '/%'", []interface{}{idParam}},
		{"store_products", "DELETE FROM store_products WHERE store_id = $1", []interface{}{storeUUID}},
	}

	for _, cq := range cascadeQueries {
		if err := execStep(cq.name, cq.query, cq.args...); err != nil {
			log.Printf("[DELETE_STORE] ABORTING: Cascade query '%s' failed: %v", cq.name, err)
			c.JSON(http.StatusInternalServerError, gin.H{
				"error": "Failed during cascade delete step '" + cq.name + "': " + err.Error(),
			})
			return
		}
	}

	// 5) Delete the store itself
	log.Printf("[DELETE_STORE] Step 5: Deleting store row from database...")
	if err := q.DeleteStore(ctx, storeUUID); err != nil {
		log.Printf("[DELETE_STORE] ERROR deleting store row: %v", err)
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to delete store row: " + err.Error()})
		return
	}
	log.Printf("[DELETE_STORE] Store row deleted successfully.")

	// 6) NOW delete users (owner + cashiers)
	log.Printf("[DELETE_STORE] Step 6: Deleting %d associated user(s) (owner & cashiers)...", len(userIDsToDelete))
	for i, uid := range userIDsToDelete {
		tag, uErr := tx.Exec(ctx, "DELETE FROM users WHERE id = $1", uid)
		if uErr != nil {
			log.Printf("[DELETE_STORE] WARNING: Failed to delete user #%d (ID: %v): %v", i+1, uid, uErr)
		} else {
			log.Printf("[DELETE_STORE] Deleted user #%d (ID: %v, rows: %d)", i+1, uid, tag.RowsAffected())
		}
	}

	// 7) Commit transaction
	log.Printf("[DELETE_STORE] Step 7: Committing database transaction...")
	if err := tx.Commit(ctx); err != nil {
		log.Printf("[DELETE_STORE] ERROR committing transaction: %v", err)
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Transaction commit failed: " + err.Error()})
		return
	}
	log.Printf("[DELETE_STORE] Transaction committed successfully!")

	// 8) Delete physical files/folders
	log.Printf("[DELETE_STORE] Step 8: Cleaning up physical upload files...")
	for _, f := range filesToDelete {
		if err := os.Remove(f); err != nil {
			log.Printf("[DELETE_STORE] File remove note (%s): %v", f, err)
		} else {
			log.Printf("[DELETE_STORE] Removed file: %s", f)
		}
	}
	uploadsDir := filepath.Join(".", "uploads", idParam)
	if err := os.RemoveAll(uploadsDir); err != nil {
		log.Printf("[DELETE_STORE] Folder remove note (%s): %v", uploadsDir, err)
	} else {
		log.Printf("[DELETE_STORE] Removed uploads directory: %s", uploadsDir)
	}

	log.Printf("[DELETE_STORE] === SUCCESS: Store %s and all associated data deleted ===", idParam)
	c.JSON(http.StatusOK, gin.H{"message": "Store and all related data deleted successfully"})
}