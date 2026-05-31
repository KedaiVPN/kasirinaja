package com.kasirinaja.api.common

import jakarta.persistence.Id
import jakarta.persistence.MappedSuperclass
import java.time.LocalDateTime
import java.util.UUID

@MappedSuperclass
abstract class BaseEntity {
    @Id
    var id: UUID = UUID.randomUUID()

    var isActive: Boolean = true

    var createdAt: LocalDateTime = LocalDateTime.now()

    var updatedAt: LocalDateTime = LocalDateTime.now()
}
