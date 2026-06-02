package handlers

import (
	"net/http"

	"github.com/gin-gonic/gin"
	"github.com/google/uuid"
	"github.com/jackc/pgx/v5/pgtype"
	"kasir-api-go/db"
)

type ProductHandler struct {
	queries *db.Queries
}

func NewProductHandler(queries *db.Queries) *ProductHandler {
	return &ProductHandler{queries: queries}
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
