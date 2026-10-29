package com.expenso.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerDao {

    @Query("""
        SELECT * FROM customers
        WHERE isActive = 1
        ORDER BY name ASC
    """)
    fun getActiveCustomers(): Flow<List<Customer>>

    @Query("""
        SELECT * FROM customers
        ORDER BY name ASC
    """)
    fun getAllCustomers(): Flow<List<Customer>>

    @Query("""
        SELECT * FROM customers
        WHERE id = :id
    """)
    suspend fun getCustomerById(id: Int): Customer?

    @Query("""
        SELECT * FROM customers
        WHERE customerCode = :code
        LIMIT 1
    """)
    suspend fun getCustomerByCode(code: String): Customer?

    @Insert
    suspend fun insertCustomer(customer: Customer): Long

    @Update
    suspend fun updateCustomer(customer: Customer)

    @Delete
    suspend fun deleteCustomer(customer: Customer)
}


@Dao
interface CustomerInvoiceDao {

    @Query("""
        SELECT * FROM customer_invoices
        ORDER BY invoiceDate DESC, id DESC
    """)
    fun getAllInvoices(): Flow<List<CustomerInvoice>>

    @Query("""
        SELECT * FROM customer_invoices
        WHERE customerId = :customerId
        ORDER BY invoiceDate DESC, id DESC
    """)
    fun getInvoicesForCustomer(
        customerId: Int
    ): Flow<List<CustomerInvoice>>

    @Query("""
        SELECT * FROM customer_invoices
        WHERE id = :id
    """)
    suspend fun getInvoiceById(id: Int): CustomerInvoice?

    @Query("""
        SELECT * FROM customer_invoices
        WHERE status IN ('OPEN', 'PARTIALLY_PAID', 'OVERDUE')
        ORDER BY dueDate ASC
    """)
    fun getOutstandingInvoices(): Flow<List<CustomerInvoice>>

    @Insert
    suspend fun insertInvoice(invoice: CustomerInvoice): Long

    @Update
    suspend fun updateInvoice(invoice: CustomerInvoice)

    @Delete
    suspend fun deleteInvoice(invoice: CustomerInvoice)
}


@Dao
interface CustomerPaymentDao {

    @Query("""
        SELECT * FROM customer_payments
        ORDER BY paymentDate DESC, id DESC
    """)
    fun getAllPayments(): Flow<List<CustomerPayment>>

    @Query("""
        SELECT * FROM customer_payments
        WHERE invoiceId = :invoiceId
        ORDER BY paymentDate DESC, id DESC
    """)
    fun getPaymentsForInvoice(
        invoiceId: Int
    ): Flow<List<CustomerPayment>>

    @Query("""
    SELECT * FROM customer_payments
    WHERE invoiceId = :invoiceId
    ORDER BY paymentDate DESC, id DESC
""")
    suspend fun getPaymentsForInvoiceOnce(
        invoiceId: Int
    ): List<CustomerPayment>

    @Query("""
        SELECT * FROM customer_payments
        WHERE customerId = :customerId
        ORDER BY paymentDate DESC, id DESC
    """)
    fun getPaymentsForCustomer(
        customerId: Int
    ): Flow<List<CustomerPayment>>

    // Get one customer payment by ID
    @Query("""
        SELECT * FROM customer_payments
        WHERE id = :id
    """)
    suspend fun getPaymentById(
        id: Int
    ): CustomerPayment?

    // Calculate total amount paid for an invoice
    @Query("""
        SELECT COALESCE(SUM(amount), 0.0)
        FROM customer_payments
        WHERE invoiceId = :invoiceId
    """)
    suspend fun getTotalPaidForInvoice(
        invoiceId: Int
    ): Double

    @Insert
    suspend fun insertPayment(
        payment: CustomerPayment
    ): Long

    @Update
    suspend fun updatePayment(
        payment: CustomerPayment
    )

    @Delete
    suspend fun deletePayment(
        payment: CustomerPayment
    )
}