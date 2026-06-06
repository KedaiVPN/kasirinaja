<<<<<<< SEARCH
	// Assuming user ID from token
	userIDStr, _ := c.Get("user_id")
	var userIDBytes [16]byte
	if b, ok := userIDStr.([16]byte); ok {
		userIDBytes = b
	}
=======
	// Assuming user ID from token
	userIDStr, exists := c.Get("user_id")
	var userIDBytes [16]byte

	if exists {
		if idStr, ok := userIDStr.(string); ok {
			parsedUUID, err := uuid.Parse(idStr)
			if err == nil {
				userIDBytes = parsedUUID
			}
		} else if idBytes, ok := userIDStr.([]interface{}); ok {
			// JWT parses byte array as []interface{}
			if len(idBytes) == 16 {
				for i, v := range idBytes {
					if floatVal, ok := v.(float64); ok {
						userIDBytes[i] = byte(floatVal)
					}
				}
			}
		}
	}
>>>>>>> REPLACE
