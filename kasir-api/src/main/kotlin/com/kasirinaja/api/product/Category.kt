package com.kasirinaja.api.product

import com.kasirinaja.api.common.BaseEntity
import jakarta.persistence.Entity
import jakarta.persistence.Table

@Entity
@Table(name = "categories")
class Category : BaseEntity() {
    var name: String = ""
    var slug: String = ""
}
