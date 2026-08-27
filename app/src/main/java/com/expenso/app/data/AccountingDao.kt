package com.expenso.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ChartOfAccountDao {

    @Query("SELECT * FROM chart_of_accounts ORDER BY accountCode ASC")
    fun getAllAccounts(): Flow<List<ChartOfAccount>>

    @Query("SELECT * FROM chart_of_accounts WHERE id = :id")
    suspend fun getAccountById(id: Int): ChartOfAccount?

    @Query("SELECT * FROM chart_of_accounts WHERE accountCode = :code LIMIT 1")
    suspend fun getAccountByCode(code: String): ChartOfAccount?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: ChartOfAccount): Long

    @Update
    suspend fun updateAccount(account: ChartOfAccount)

    @Delete
    suspend fun deleteAccount(account: ChartOfAccount)

    @Query("DELETE FROM chart_of_accounts")
    suspend fun deleteAllAccounts()
}


@Dao
interface JournalEntryDao {

    @Query("""
        SELECT * FROM journal_entries
        ORDER BY entryDate DESC, id DESC
    """)
    fun getAllJournalEntries(): Flow<List<JournalEntry>>

    @Query("""
        SELECT * FROM journal_entries
        WHERE id = :id
    """)
    suspend fun getJournalEntryById(id: Int): JournalEntry?

    @Insert
    suspend fun insertJournalEntry(entry: JournalEntry): Long

    @Update
    suspend fun updateJournalEntry(entry: JournalEntry)

    @Delete
    suspend fun deleteJournalEntry(entry: JournalEntry)

    @Query("DELETE FROM journal_entries")
    suspend fun deleteAllJournalEntries()
}


@Dao
interface JournalLineDao {

    @Query("""
        SELECT * FROM journal_lines
        WHERE journalEntryId = :journalEntryId
        ORDER BY id ASC
    """)
    suspend fun getLinesForJournalEntry(
        journalEntryId: Int
    ): List<JournalLine>

    @Query("""
        SELECT * FROM journal_lines
        WHERE accountId = :accountId
        ORDER BY journalEntryId DESC
    """)
    fun getLinesForAccount(
        accountId: Int
    ): Flow<List<JournalLine>>

    @Insert
    suspend fun insertJournalLine(line: JournalLine): Long

    @Insert
    suspend fun insertJournalLines(lines: List<JournalLine>)

    @Update
    suspend fun updateJournalLine(line: JournalLine)

    @Delete
    suspend fun deleteJournalLine(line: JournalLine)

    @Query("""
        DELETE FROM journal_lines
        WHERE journalEntryId = :journalEntryId
    """)
    suspend fun deleteLinesForJournalEntry(journalEntryId: Int)

    @Query("DELETE FROM journal_lines")
    suspend fun deleteAllJournalLines()

    @Query("SELECT * FROM journal_lines ORDER BY id ASC")
    fun getAllJournalLines(): Flow<List<JournalLine>>
}