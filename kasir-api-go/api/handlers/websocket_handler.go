package handlers

import (
	"encoding/json"
	"log"
	"net/http"
	"sync"

	"github.com/gin-gonic/gin"
	"github.com/gorilla/websocket"
	"kasir-api-go/db"
)

var upgrader = websocket.Upgrader{
	CheckOrigin: func(r *http.Request) bool {
		return true // Allow all origins for now (MVP)
	},
}

type WebSocketManager struct {
	clients map[*websocket.Conn]bool
	mu      sync.Mutex
}

func NewWebSocketManager() *WebSocketManager {
	return &WebSocketManager{
		clients: make(map[*websocket.Conn]bool),
	}
}

func (wm *WebSocketManager) HandleConnections(c *gin.Context) {
	ws, err := upgrader.Upgrade(c.Writer, c.Request, nil)
	if err != nil {
		log.Println("WebSocket upgrade error:", err)
		return
	}
	defer ws.Close()

	wm.mu.Lock()
	wm.clients[ws] = true
	wm.mu.Unlock()

	log.Println("Client connected to WebSocket")

	for {
		// Listen for messages (we don't strictly need to process incoming messages for this MVP,
		// but we need to read to detect disconnects)
		_, _, err := ws.ReadMessage()
		if err != nil {
			log.Println("Client disconnected:", err)
			wm.mu.Lock()
			delete(wm.clients, ws)
			wm.mu.Unlock()
			break
		}
	}
}

type ProductNotification struct {
	Type    string             `json:"type"`
	Product db.PendingProduct `json:"product"`
}

func (wm *WebSocketManager) BroadcastNewPendingProduct(product db.PendingProduct) {
	wm.mu.Lock()
	defer wm.mu.Unlock()

	notification := ProductNotification{
		Type:    "NEW_PENDING_PRODUCT",
		Product: product,
	}

	message, err := json.Marshal(notification)
	if err != nil {
		log.Println("Failed to marshal notification:", err)
		return
	}

	for client := range wm.clients {
		err := client.WriteMessage(websocket.TextMessage, message)
		if err != nil {
			log.Println("WebSocket write error:", err)
			client.Close()
			delete(wm.clients, client)
		}
	}
}
