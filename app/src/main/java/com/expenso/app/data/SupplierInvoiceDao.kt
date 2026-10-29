package com.expenso.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SupplierInvoiceDao {

    @Insert
    suspend fun insertSupplierInvoice(invoice: SupplierInvoice): Long

    @Update
    suspend fun updateSupplierInvoice(invoice: SupplierInvoice)

    @Delete
    suspend fun deleteSupplierInvoice(invoice: SupplierInvoice)

    @Query("SELECT * FROM supplier_invoices ORDER BY invoiceDate DESC")
    fun getAllSupplierInvoices(): Flow<List<SupplierInvoice>>

    @Query("""
        SELECT * FROM supplier_invoices
        WHERE status != 'PAID'
        ORDER BY dueDate ASC
    """)
    fun getOutstandingSupplierInvoices(): Flow<List<SupplierInvoice>>

    @Query("""
        SELECT * FROM supplier_invoices
        WHERE supplierId = :supplierId
        ORDER BY invoiceDate DESC
    """)
    fun getSupplierInvoices(supplierId: Int): Flow<List<SupplierInvoice>>

    @Query("""
        SELECT * FROM supplier_invoices
        WHERE id = :invoiceId
        LIMIT 1
    """)
    suspend fun getSupplierInvoiceById(invoiceId: Int): SupplierInvoice?
}