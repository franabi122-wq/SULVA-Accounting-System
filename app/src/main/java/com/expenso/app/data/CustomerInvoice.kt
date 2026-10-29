package com.expenso.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "customer_invoices")
data class CustomerInvoice(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val customerId: Int,

    val invoiceNumber: String,

    val invoiceDate: String,

    val dueDate: String,

    val description: String = "",

    val amount: Double,

    val amountPaid: Double = 0.0,

    val status: String = "OPEN",

    val journalEntryId: Int? = null,

    val createdAt: String
)