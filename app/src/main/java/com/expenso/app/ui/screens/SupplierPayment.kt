package com.expenso.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "supplier_payments")
data class SupplierPayment(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val supplierId: Int,
    val supplierInvoiceId: Int,

    val paymentDate: String,

    val amount: Double,

    val paymentMethod: String = "CASH",

    val referenceNumber: String = "",

    val notes: String = "",

    val createdAt: String
)