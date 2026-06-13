package handlers

import (
	"log"
	"net/http"
	"os"
	"strings"

	"kasir-api-go/db"

	"github.com/gin-gonic/gin"
	"github.com/google/uuid"
	"github.com/jackc/pgx/v5/pgtype"
)

type AdminHandler struct {
	queries *db.Queries
}

func NewAdminHandler(queries *db.Queries) *AdminHandler {
	return &AdminHandler{queries: queries}
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

	c.JSON(http.StatusOK, gin.H{
		"approved_count": approvedCount,
		"pending_count":  pendingCount,
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
			LocalName:       pgtype.Text{String: pendingProduct.Name, Valid: pendingProduct.Name != ""},
			LocalCategory:   pgtype.Text{String: pendingProduct.Category, Valid: pendingProduct.Category != ""},
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
