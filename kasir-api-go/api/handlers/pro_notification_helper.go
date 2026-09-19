package handlers

import (
	"context"
	"log"

	"kasir-api-go/api"
	"kasir-api-go/db"

	"github.com/google/uuid"
	"github.com/jackc/pgx/v5/pgtype"
)

func NotifyProUpgrade(ctx context.Context, queries *db.Queries, storeID pgtype.UUID) {
	go func() {
		bgCtx := context.Background()
		store, err := queries.GetStore(bgCtx, storeID)
		if err != nil {
			log.Printf("NotifyProUpgrade: Failed to get store %v: %v\n", storeID, err)
			return
		}

		storeUUIDStr := ""
		if store.ID.Valid {
			parsedUUID, _ := uuid.FromBytes(store.ID.Bytes[:])
			storeUUIDStr = parsedUUID.String()
		}

		// 1. Send congratulations push notification to Store Owner
		owners, err := queries.ListStoreOwners(bgCtx, storeID)
		if err == nil {
			for _, owner := range owners {
				if owner.FcmToken.Valid && owner.FcmToken.String != "" {
					title := "Selamat! Toko Anda Berhasil Upgrade Pro 🎉"
					body := "Selamat, toko " + store.StoreName + " Anda telah aktif sebagai Toko Pro! Nikmati akses penuh ke seluruh fitur."
					err := api.SendPushNotificationWithData(owner.FcmToken.String, title, body, map[string]string{
						"type": "pro_upgrade",
					})
					if err != nil {
						log.Printf("Failed to send Pro upgrade notification to owner %s: %v\n", owner.FullName, err)
					}
				}
			}
		}

		// 2. Send notification to Super Admin
		adminTokens, err := queries.ListAdminFCMTokens(bgCtx)
		if err == nil {
			for _, token := range adminTokens {
				if token.Valid && token.String != "" {
					title := "Toko Upgrade ke versi Pro 🚀"
					body := "Toko " + store.StoreName + " baru saja upgrade ke versi Pro!"
					err := api.SendPushNotificationWithData(token.String, title, body, map[string]string{
						"type":     "admin_pro_upgrade",
						"store_id": storeUUIDStr,
					})
					if err != nil {
						log.Printf("Failed to send Pro upgrade notification to admin token %s: %v\n", token.String, err)
					}
				}
			}
		}
	}()
}
