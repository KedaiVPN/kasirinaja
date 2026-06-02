package routes

import (
	"github.com/gin-gonic/gin"
	"github.com/jackc/pgx/v5/pgxpool"
	"kasir-api-go/api/handlers"
	"kasir-api-go/db"
)

func SetupRoutes(router *gin.Engine, queries *db.Queries, pool *pgxpool.Pool) {
	// Initialize handlers
	userHandler := handlers.NewUserHandler(queries)
	productHandler := handlers.NewProductHandler(queries)
	transactionHandler := handlers.NewTransactionHandler(queries, pool)

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
		}

		// Transaction routes
		transactions := api.Group("/transactions")
		{
			transactions.POST("/", transactionHandler.CreateTransaction)
		}
	}
}
