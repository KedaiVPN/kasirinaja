package handlers

import (
	"net/http"
	"os"
	"path/filepath"

	"github.com/gin-gonic/gin"
	"github.com/google/uuid"
	"github.com/jackc/pgx/v5/pgtype"
	"kasir-api-go/db"
)

type ProductHandler struct {
	queries *db.Queries
	wsManager *WebSocketManager
}

func NewProductHandler(queries *db.Queries, wsManager *WebSocketManager) *ProductHandler {
	return &ProductHandler{
		queries: queries,
		wsManager: wsManager,
	}
}

type CreateMasterProductRequest struct {
	Barcode            string `json:"barcode" binding:"required"`
	Name               string `json:"name" binding:"required"`
	PhotoURL           string `json:"photo_url"`
	PhotoPath          string `json:"photo_path"`
	CategoryID         string `json:"category_id"`
	BrandID            string `json:"brand_id"`
	Unit               string `json:"unit"`
	Source             string `json:"source"`
	IsGeneratedBarcode bool   `json:"is_generated_barcode"`
	CreatedBy          string `json:"created_by"`
}

func (h *ProductHandler) CreateMasterProduct(c *gin.Context) {
	var req CreateMasterProductRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}

	arg := db.CreateMasterProductParams{
		Barcode:            req.Barcode,
		Name:               req.Name,
		PhotoUrl:           pgtype.Text{String: req.PhotoURL, Valid: req.PhotoURL != ""},
		PhotoPath:          pgtype.Text{String: req.PhotoPath, Valid: req.PhotoPath != ""},
		Unit:               pgtype.Text{String: req.Unit, Valid: req.Unit != ""},
		Source:             pgtype.Text{String: req.Source, Valid: req.Source != ""},
		IsGeneratedBarcode: pgtype.Bool{Bool: req.IsGeneratedBarcode, Valid: true},
	}

	if req.CategoryID != "" {
		uid, err := uuid.Parse(req.CategoryID)
		if err == nil {
			arg.CategoryID = pgtype.UUID{Bytes: uid, Valid: true}
		}
	}
	if req.BrandID != "" {
		uid, err := uuid.Parse(req.BrandID)
		if err == nil {
			arg.BrandID = pgtype.UUID{Bytes: uid, Valid: true}
		}
	}
	if req.CreatedBy != "" {
		uid, err := uuid.Parse(req.CreatedBy)
		if err == nil {
			arg.CreatedBy = pgtype.UUID{Bytes: uid, Valid: true}
		}
	}

	product, err := h.queries.CreateMasterProduct(c.Request.Context(), arg)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
		return
	}

	c.JSON(http.StatusCreated, product)
}

func (h *ProductHandler) GetMasterProduct(c *gin.Context) {
	idParam := c.Param("id")
	id, err := uuid.Parse(idParam)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "invalid master product id"})
		return
	}

	product, err := h.queries.GetMasterProduct(c.Request.Context(), pgtype.UUID{Bytes: id, Valid: true})
	if err != nil {
		c.JSON(http.StatusNotFound, gin.H{"error": "master product not found"})
		return
	}

	c.JSON(http.StatusOK, product)
}

func (h *ProductHandler) ListMasterProducts(c *gin.Context) {
	products, err := h.queries.ListMasterProducts(c.Request.Context())
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
		return
	}

	c.JSON(http.StatusOK, products)
}

type CreateStoreProductRequest struct {
	StoreID         string `json:"store_id" binding:"required"`
	MasterProductID string `json:"master_product_id" binding:"required"`
	BuyPrice        string `json:"buy_price" binding:"required"`
	SellPrice       string `json:"sell_price" binding:"required"`
	Stock           int32  `json:"stock"`
	MinStock        int32  `json:"min_stock"`
	LocalName       string `json:"local_name"`
}

func (h *ProductHandler) CreateStoreProduct(c *gin.Context) {
	var req CreateStoreProductRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}

	storeID, err := uuid.Parse(req.StoreID)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "invalid store id"})
		return
	}
	masterProductID, err := uuid.Parse(req.MasterProductID)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "invalid master product id"})
		return
	}

	var buyPrice pgtype.Numeric
	buyPrice.Scan(req.BuyPrice)

	var sellPrice pgtype.Numeric
	sellPrice.Scan(req.SellPrice)

	arg := db.CreateStoreProductParams{
		StoreID:         pgtype.UUID{Bytes: storeID, Valid: true},
		MasterProductID: pgtype.UUID{Bytes: masterProductID, Valid: true},
		BuyPrice:        buyPrice,
		SellPrice:       sellPrice,
		Stock:           req.Stock,
		MinStock:        req.MinStock,
		LocalName:       pgtype.Text{String: req.LocalName, Valid: req.LocalName != ""},
	}

	product, err := h.queries.CreateStoreProduct(c.Request.Context(), arg)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
		return
	}

	c.JSON(http.StatusCreated, product)
}

func (h *ProductHandler) GetStoreProduct(c *gin.Context) {
	idParam := c.Param("id")
	id, err := uuid.Parse(idParam)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "invalid store product id"})
		return
	}

	product, err := h.queries.GetStoreProduct(c.Request.Context(), pgtype.UUID{Bytes: id, Valid: true})
	if err != nil {
		c.JSON(http.StatusNotFound, gin.H{"error": "store product not found"})
		return
	}

	c.JSON(http.StatusOK, product)
}

func (h *ProductHandler) ListStoreProducts(c *gin.Context) {
	storeIDParam := c.Query("store_id")
	if storeIDParam == "" {
		c.JSON(http.StatusBadRequest, gin.H{"error": "store_id query param is required"})
		return
	}

	storeID, err := uuid.Parse(storeIDParam)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "invalid store id"})
		return
	}

	products, err := h.queries.ListStoreProductsByStore(c.Request.Context(), pgtype.UUID{Bytes: storeID, Valid: true})
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
		return
	}

	c.JSON(http.StatusOK, products)
}

type SubmitPendingProductRequest struct {
	Name        string `json:"name" binding:"required"`
	BuyPrice    string `json:"buy_price" binding:"required"`
	SellPrice   string `json:"sell_price" binding:"required"`
	Stock       int32  `json:"stock"`
	Category    string `json:"category" binding:"required"`
	Description string `json:"description"`
	Barcode     string `json:"barcode"`
	ImageURL    string `json:"image_url"`
	StoreID     string `json:"store_id"`
}

func (h *ProductHandler) SubmitPendingProduct(c *gin.Context) {
	var req SubmitPendingProductRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}

	var buyPrice pgtype.Numeric
	buyPrice.Scan(req.BuyPrice)

	var sellPrice pgtype.Numeric
	sellPrice.Scan(req.SellPrice)

	arg := db.CreatePendingProductParams{
		Name:        req.Name,
		BuyPrice:    buyPrice,
		SellPrice:   sellPrice,
		Stock:       req.Stock,
		Category:    req.Category,
		Description: pgtype.Text{String: req.Description, Valid: req.Description != ""},
		Barcode:     pgtype.Text{String: req.Barcode, Valid: req.Barcode != ""},
		ImageUrl:    pgtype.Text{String: req.ImageURL, Valid: req.ImageURL != ""},
	}

	if req.StoreID != "" {
		uid, err := uuid.Parse(req.StoreID)
		if err == nil {
			arg.StoreID = pgtype.UUID{Bytes: uid, Valid: true}
		}
	}

	pendingProduct, err := h.queries.CreatePendingProduct(c.Request.Context(), arg)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
		return
	}

	if h.wsManager != nil {
		h.wsManager.BroadcastNewPendingProduct(pendingProduct)
	}

	c.JSON(http.StatusCreated, pendingProduct)
}

func (h *ProductHandler) ListPendingProducts(c *gin.Context) {
	products, err := h.queries.ListPendingProducts(c.Request.Context())
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
		return
	}

	c.JSON(http.StatusOK, products)
}

func (h *ProductHandler) DeleteProduct(c *gin.Context) {
	idParam := c.Param("id")
	status := c.Query("status") // "pending" or "approved"

	id, err := uuid.Parse(idParam)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "invalid product id"})
		return
	}

	if status == "pending" {
		// Get pending product first to delete the image from disk
		product, err := h.queries.GetPendingProduct(c.Request.Context(), pgtype.UUID{Bytes: id, Valid: true})
		if err == nil && product.ImageUrl.Valid && product.ImageUrl.String != "" {
			// Basic security check: ensure we don't delete system files by accident
			filename := filepath.Base(product.ImageUrl.String)
			fullPath := filepath.Join("uploads", filename)
			_ = os.Remove(fullPath) // Ignore err if file doesn't exist
		}

		err = h.queries.DeletePendingProduct(c.Request.Context(), pgtype.UUID{Bytes: id, Valid: true})
		if err != nil {
			c.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
			return
		}
	} else if status == "approved" {
		err = h.queries.DeleteStoreProduct(c.Request.Context(), pgtype.UUID{Bytes: id, Valid: true})
		if err != nil {
			c.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
			return
		}
	} else {
		c.JSON(http.StatusBadRequest, gin.H{"error": "status query param must be 'pending' or 'approved'"})
		return
	}

	c.JSON(http.StatusOK, gin.H{"message": "product deleted successfully"})
}

type UpdateProductRequest struct {
	Name        string `json:"name"`
	BuyPrice    string `json:"buy_price"`
	SellPrice   string `json:"sell_price"`
	Stock       int32  `json:"stock"`
	Category    string `json:"category"`
	Description string `json:"description"`
	Barcode     string `json:"barcode"`
	ImageURL    string `json:"image_url"`
}

func (h *ProductHandler) UpdateProduct(c *gin.Context) {
	idParam := c.Param("id")
	status := c.Query("status") // "pending" or "approved"

	id, err := uuid.Parse(idParam)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "invalid product id"})
		return
	}

	var req UpdateProductRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}

	var buyPrice pgtype.Numeric
	buyPrice.Scan(req.BuyPrice)

	var sellPrice pgtype.Numeric
	sellPrice.Scan(req.SellPrice)

	if status == "pending" {
		arg := db.UpdatePendingProductParams{
			ID:          pgtype.UUID{Bytes: id, Valid: true},
			Name:        req.Name,
			BuyPrice:    buyPrice,
			SellPrice:   sellPrice,
			Stock:       req.Stock,
			Category:    req.Category,
			Description: pgtype.Text{String: req.Description, Valid: req.Description != ""},
			Barcode:     pgtype.Text{String: req.Barcode, Valid: req.Barcode != ""},
			ImageUrl:    pgtype.Text{String: req.ImageURL, Valid: req.ImageURL != ""},
		}
		err = h.queries.UpdatePendingProduct(c.Request.Context(), arg)
		if err != nil {
			c.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
			return
		}
	} else if status == "approved" {
		arg := db.UpdateStoreProductParams{
			ID:        pgtype.UUID{Bytes: id, Valid: true},
			BuyPrice:  buyPrice,
			SellPrice: sellPrice,
			Stock:     req.Stock,
			LocalName: pgtype.Text{String: req.Name, Valid: req.Name != ""}, // Name mapped to local_name
		}
		err = h.queries.UpdateStoreProduct(c.Request.Context(), arg)
		if err != nil {
			c.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
			return
		}
	} else {
		c.JSON(http.StatusBadRequest, gin.H{"error": "status query param must be 'pending' or 'approved'"})
		return
	}

	c.JSON(http.StatusOK, gin.H{"message": "product updated successfully"})
}
