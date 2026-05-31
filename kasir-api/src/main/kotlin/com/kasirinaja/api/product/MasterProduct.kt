package com.kasirinaja.api.product

import com.kasirinaja.api.common.BaseEntity
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "master_products")
class MasterProduct : BaseEntity() {
    var barcode: String = ""
    var name: String = ""
    var photoUrl: String? = null
    var photoPath: String? = null
    var categoryId: UUID? = null
    var brandId: UUID? = null
    var unit: String? = null
    var source: String? = null
    var isGeneratedBarcode: Boolean = false
    var createdBy: UUID? = null
}
