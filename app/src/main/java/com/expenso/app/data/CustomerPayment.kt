package com.expenso.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "customer_payments")
data class CustomerPayment(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val invoiceId: Int,

    val customerId: Int,

    val paymentDate: String,

    val amount: Double,

    val accountId: Int,

    val referenceNumber: String = "",

    val notes: String = "",

    val journalEntryId: Int? = null,

    val createdAt: String
)