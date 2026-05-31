package com.kasirinaja.api.store

import com.kasirinaja.api.common.BaseEntity
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "stores")
class Store : BaseEntity() {
    var ownerId: UUID? = null
    var storeCode: String = ""
    var storeName: String = ""
    var address: String? = null
    var phone: String? = null
}
