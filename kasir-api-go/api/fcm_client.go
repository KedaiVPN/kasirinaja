package api

import (
	"context"
	"fmt"
	"log"

	firebase "firebase.google.com/go/v4"
	"firebase.google.com/go/v4/messaging"
	"google.golang.org/api/option"
)

var fcmClient *messaging.Client

func InitFirebase() {
	opt := option.WithCredentialsFile("firebase-service-account.json")
	app, err := firebase.NewApp(context.Background(), nil, opt)
	if err != nil {
		log.Printf("Firebase setup skipped: %v\n", err)
		return
	}

	client, err := app.Messaging(context.Background())
	if err != nil {
		log.Printf("Firebase messaging setup skipped: %v\n", err)
		return
	}
	fcmClient = client
	log.Println("Firebase Cloud Messaging initialized")
}

func SendPushNotification(token, title, body string) error {
	if fcmClient == nil || token == "" {
		return fmt.Errorf("FCM client not initialized or token empty")
	}

	message := &messaging.Message{
		Notification: &messaging.Notification{
			Title: title,
			Body:  body,
		},
		Token: token,
	}

	response, err := fcmClient.Send(context.Background(), message)
	if err != nil {
		return err
	}

	log.Println("Successfully sent message:", response)
	return nil
}
