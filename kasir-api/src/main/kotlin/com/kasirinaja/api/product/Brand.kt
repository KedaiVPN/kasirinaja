package com.kasirinaja.api.product

import com.kasirinaja.api.common.BaseEntity
import jakarta.persistence.Entity
import jakarta.persistence.Table

@Entity
@Table(name = "brands")
class Brand : BaseEntity() {
    var name: String = ""
}
