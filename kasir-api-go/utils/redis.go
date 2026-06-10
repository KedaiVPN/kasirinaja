package utils

import (
	"context"
	"encoding/json"
	"log"
	"os"
	"time"

	"github.com/go-redis/redis/v8"
)

var RedisClient *redis.Client

func InitRedis() {
	redisUrl := os.Getenv("REDIS_URL")
	if redisUrl == "" {
		redisUrl = "localhost:6379"
	}

	redisPassword := os.Getenv("REDIS_PASSWORD")

	RedisClient = redis.NewClient(&redis.Options{
		Addr:     redisUrl,
		Password: redisPassword,
		DB:       0,
	})

	_, err := RedisClient.Ping(context.Background()).Result()
	if err != nil {
		log.Printf("Warning: Failed to connect to Redis at %s: %v", redisUrl, err)
	} else {
		log.Printf("Successfully connected to Redis at %s", redisUrl)
	}
}

type RegistrationData struct {
	FullName  string `json:"full_name"`
	Email     string `json:"email"`
	Phone     string `json:"phone"`
	Password  string `json:"password"`
	StoreName string `json:"store_name"`
	Address   string `json:"address"`
	OTP       string `json:"otp"`
}

func SaveRegistrationData(ctx context.Context, email string, data RegistrationData) error {
	jsonData, err := json.Marshal(data)
	if err != nil {
		return err
	}
	// Save for 5 minutes
	return RedisClient.Set(ctx, "registration:"+email, jsonData, 5*time.Minute).Err()
}

func GetRegistrationData(ctx context.Context, email string) (*RegistrationData, error) {
	val, err := RedisClient.Get(ctx, "registration:"+email).Result()
	if err != nil {
		return nil, err
	}

	var data RegistrationData
	err = json.Unmarshal([]byte(val), &data)
	if err != nil {
		return nil, err
	}

	return &data, nil
}

func DeleteRegistrationData(ctx context.Context, email string) error {
	return RedisClient.Del(ctx, "registration:"+email).Err()
}
