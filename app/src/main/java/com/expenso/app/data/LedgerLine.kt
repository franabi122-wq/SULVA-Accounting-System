package com.expenso.app.data

data class LedgerLine(
    val journalEntryId: Int,
    val date: String,
    val referenceNumber: String,
    val description: String,
    val debit: Double,
    val credit: Double,
    val balance: Double
)