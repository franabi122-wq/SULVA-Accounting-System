package com.expenso.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chart_of_accounts")
data class ChartOfAccount(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val accountCode: String,
    val accountName: String,

    // ASSET, LIABILITY, EQUITY, REVENUE, EXPENSE
    val accountType: String,

    val description: String = "",

    val isActive: Boolean = true
)

@Entity(
    tableName = "journal_entries"
)
data class JournalEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val entryDate: String,

    val referenceNumber: String = "",

    val description: String,

    val createdAt: String
)

@Entity(
    tableName = "journal_lines"
)
data class JournalLine(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val journalEntryId: Int,

    val accountId: Int,

    val debit: Double = 0.0,

    val credit: Double = 0.0,

    val description: String = ""
)