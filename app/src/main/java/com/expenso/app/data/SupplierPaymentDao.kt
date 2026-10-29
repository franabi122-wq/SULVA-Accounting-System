package com.expenso.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SupplierPaymentDao {

    @Insert
    suspend fun insertSupplierPayment(payment: SupplierPayment): Long

    @Update
    suspend fun updateSupplierPayment(payment: SupplierPayment)

    @Delete
    suspend fun deleteSupplierPayment(payment: SupplierPayment)

    @Query("""
        SELECT * FROM supplier_payments
        ORDER BY paymentDate DESC
    """)
    fun getAllSupplierPayments(): Flow<List<SupplierPayment>>

    @Query("""
        SELECT * FROM supplier_payments
        WHERE supplierId = :supplierId
        ORDER BY paymentDate DESC
    """)
    fun getSupplierPayments(
        supplierId: Int
    ): Flow<List<SupplierPayment>>

    @Query("""
        SELECT * FROM supplier_payments
        WHERE supplierInvoiceId = :invoiceId
        ORDER BY paymentDate DESC
    """)
    fun getPaymentsForInvoice(
        invoiceId: Int
    ): Flow<List<SupplierPayment>>

    @Query("""
    SELECT * FROM supplier_payments
    WHERE supplierInvoiceId = :invoiceId
""")
    suspend fun getPaymentsForInvoiceOnce(
        invoiceId: Int
    ): List<SupplierPayment>

    @Query("""
        SELECT * FROM supplier_payments
        WHERE id = :paymentId
        LIMIT 1
    """)
    suspend fun getSupplierPaymentById(
        paymentId: Int
    ): SupplierPayment?
}