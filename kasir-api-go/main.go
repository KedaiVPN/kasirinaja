package main

import (
	"context"
	"fmt"
	"log"
	"os"

	"kasir-api-go/api"
	"kasir-api-go/api/handlers"
	"kasir-api-go/api/routes"
	"kasir-api-go/db"
	"kasir-api-go/utils"

	"github.com/gin-gonic/gin"
	"github.com/jackc/pgx/v5/pgxpool"
	"github.com/joho/godotenv"
)

func main() {
	// Load .env file if it exists
	if err := godotenv.Load(); err != nil {
		log.Println("No .env file found, relying on environment variables")
	}

	// Fail-fast: pastikan JWT_SECRET kuat sebelum server melayani request.
	if err := handlers.ValidateJWTSecret(); err != nil {
		log.Fatalf("Konfigurasi JWT tidak aman: %v", err)
	}

	// Initialize Firebase Cloud Messaging
	api.InitFirebase()

	dbUrl := os.Getenv("DATABASE_URL")
	if dbUrl == "" {
		dbUrl = "postgres://postgres:postgres@localhost:5432/kasir?sslmode=disable"
	}

	// Initialize pgxpool
	pool, err := pgxpool.New(context.Background(), dbUrl)
	if err != nil {
		log.Fatalf("Unable to connect to database: %v\n", err)
	}
	defer pool.Close()

	// Initialize sqlc queries
	queries := db.New(pool)

	// Initialize Admin User from .env
	handlers.InitAdminUser(queries)

	// Setup Gin router
	router := gin.Default()

	// Initialize Redis (dipakai untuk OTP, rate limit, dan lockout login)
	rdb := utils.InitRedis()

	// Mode release: jangan bocorkan detail route/log di produksi.
	if os.Getenv("APP_ENV") == "production" {
		gin.SetMode(gin.ReleaseMode)
	}

	// Setup Routes
	routes.SetupRoutes(router, queries, pool, rdb)

	port := os.Getenv("PORT")
	if port == "" {
		port = "8080"
	}

	fmt.Printf("Server is running on port %s\n", port)
	if err := router.Run(":" + port); err != nil {
		log.Fatalf("Failed to start server: %v", err)
	}
}
