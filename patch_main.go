<<<<<<< SEARCH
	// Initialize sqlc queries
	queries := db.New(pool)

	// Setup Gin router
	router := gin.Default()

	// Setup Routes
	routes.SetupRoutes(router, queries, pool)
=======
	// Initialize sqlc queries
	queries := db.New(pool)

	// Initialize Admin User from .env
	handlers.InitAdminUser(queries)

	// Setup Gin router
	router := gin.Default()

	// Setup Routes
	routes.SetupRoutes(router, queries, pool)
>>>>>>> REPLACE
