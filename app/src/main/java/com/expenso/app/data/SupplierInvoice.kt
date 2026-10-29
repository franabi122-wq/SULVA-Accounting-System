package com.expenso.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "supplier_invoices")
data class SupplierInvoice(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val supplierId: Int,
    val invoiceNumber: String,

    val invoiceDate: String,
    val dueDate: String,

    val description: String = "",

    val subtotal: Double,
    val taxAmount: Double = 0.0,
    val totalAmount: Double,

    val amountPaid: Double = 0.0,

    val status: String = "OPEN",

    val createdAt: String
)