package com.kasirinaja.api.user

import com.kasirinaja.api.common.BaseEntity
import com.kasirinaja.api.common.Role
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "users")
class User : BaseEntity() {
    var fullName: String = ""
    var email: String = ""
    var phone: String = ""
    var passwordHash: String = ""

    @Enumerated(EnumType.STRING)
    var role: Role = Role.CASHIER

    var storeId: UUID? = null
}
