package routes

import (
	"github.com/gin-gonic/gin"
	"github.com/jackc/pgx/v5/pgxpool"
	"kasir-api-go/api/handlers"
	"kasir-api-go/db"
)

func SetupRoutes(router *gin.Engine, queries *db.Queries, pool *pgxpool.Pool) {
	// Initialize handlers
	wsManager := handlers.NewWebSocketManager()
	userHandler := handlers.NewUserHandler(queries)
	productHandler := handlers.NewProductHandler(queries, wsManager)
	transactionHandler := handlers.NewTransactionHandler(queries, pool)
	authHandler := handlers.NewAuthHandler(queries, pool)
	adminHandler := handlers.NewAdminHandler(queries)

	// Root route to prevent 404 on base domain
	router.GET("/", func(c *gin.Context) {
		c.JSON(200, gin.H{
			"message": "Kasir API is running. Access endpoints under /api",
		})
	})

	api := router.Group("/api")
	{
		// Health check
		api.GET("/health", func(c *gin.Context) {
			c.JSON(200, gin.H{"status": "ok"})
		})

		// User routes
		users := api.Group("/users")
		{
			users.POST("/", userHandler.CreateUser)
			users.GET("/:id", userHandler.GetUser)
			users.GET("/", userHandler.ListUsers)
		}

		// Product routes
		products := api.Group("/products")
		{
			products.POST("/master", productHandler.CreateMasterProduct)
			products.GET("/master/:id", productHandler.GetMasterProduct)
			products.GET("/master", productHandler.ListMasterProducts)

			products.POST("/store", productHandler.CreateStoreProduct)
			products.GET("/store/:id", productHandler.GetStoreProduct)
			products.GET("/store", productHandler.ListStoreProducts)

			products.POST("/pending", productHandler.SubmitPendingProduct)
			products.GET("/pending", productHandler.ListPendingProducts)

			products.DELETE("/store/:id", handlers.AuthMiddleware(), productHandler.DeleteStoreProductSpecific)
			products.DELETE("/:id", productHandler.DeleteProduct)
			products.PUT("/:id", productHandler.UpdateProduct)
		}

		// Transaction routes
		transactions := api.Group("/transactions")
		transactions.Use(handlers.AuthMiddleware())
		{
			transactions.POST("/", transactionHandler.CreateTransaction)
			transactions.GET("/dashboard", transactionHandler.GetDashboardStats)
		}

		// WebSocket routes
		api.GET("/ws", wsManager.HandleConnections)

		// Auth
		api.POST("/login", authHandler.Login) // Used by Admin
		api.POST("/auth/login", authHandler.Login) // Used by Store
		api.POST("/auth/register-store", authHandler.RegisterStore)
		api.POST("/auth/verify-otp", authHandler.VerifyOTP)
		api.POST("/auth/resend-otp", authHandler.ResendOTP)

		// Admin routes
		adminRoutes := api.Group("/admin")
		adminRoutes.Use(handlers.AuthMiddleware())
		{
			adminRoutes.GET("/dashboard", adminHandler.GetDashboardStats)
			adminRoutes.POST("/products/:id/approve", adminHandler.ApproveProduct)
			adminRoutes.POST("/products/:id/reject", adminHandler.RejectProduct)
		}

		// Upload route
		api.POST("/upload", handlers.UploadImage)
	}

	// Serve static files for uploads
	router.Static("/uploads", "./uploads")
}
