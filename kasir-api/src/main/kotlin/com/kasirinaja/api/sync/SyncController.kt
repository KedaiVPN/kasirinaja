package com.kasirinaja.api.sync

import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/sync")
class SyncController(
    private val syncService: SyncService
) {

    @PostMapping("/push")
    fun pushSync(@Valid @RequestBody request: SyncPushRequest): ResponseEntity<SyncResponse> {
        val response = syncService.pushTransactions(request)
        return ResponseEntity.ok(response)
    }
}
