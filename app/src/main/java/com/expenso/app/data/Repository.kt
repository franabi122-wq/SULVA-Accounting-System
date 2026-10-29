package com.expenso.app.data

import android.content.Context
import android.util.Log.d
import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class Repository(private val db: AppDatabase) {

    val accountDao = db.accountDao()
    val categoryDao = db.categoryDao()
    val transactionDao = db.transactionDao()
    val budgetDao = db.budgetDao()
    val savingsGoalDao = db.savingsGoalDao()
    val recurringTransactionDao = db.recurringTransactionDao()
    val settingsDao = db.settingsDao()
    // --- ACCOUNTING ---
    val chartOfAccountDao = db.chartOfAccountDao()
    val journalEntryDao = db.journalEntryDao()
    val journalLineDao = db.journalLineDao()
    val customerDao = db.customerDao()
    val customerInvoiceDao = db.customerInvoiceDao()
    val customerPaymentDao = db.customerPaymentDao()

    val supplierDao = db.supplierDao()
    val supplierInvoiceDao = db.supplierInvoiceDao()
    val supplierPaymentDao = db.supplierPaymentDao()

    // --- ACCOUNTS ---
    val allAccounts: Flow<List<Account>> = accountDao.getAllAccounts()

    suspend fun getAccountById(id: Int): Account? = accountDao.getAccountById(id)

    suspend fun getAccountByNameDirect(name: String): Account? = accountDao.getAccountByName(name)

    suspend fun insertAccount(account: Account): Long = accountDao.insertAccount(account)

    suspend fun insertAccountDirect(account: Account): Long = accountDao.insertAccount(account)

    suspend fun updateAccount(account: Account) = accountDao.updateAccount(account)

    suspend fun deleteAccount(account: Account): Boolean {
        // Guarded deletion: check if any transaction uses this account
        val count = transactionDao.getTransactionCountForAccount(account.id)
        if (count > 0) {
            return false // Deletion blocked
        }
        accountDao.deleteAccount(account)
        return true
    }

    // --- CATEGORIES ---
    val allCategories: Flow<List<Category>> = categoryDao.getAllCategories()

    fun getCategoriesByType(type: String): Flow<List<Category>> = categoryDao.getCategoriesByType(type)

    suspend fun getCategoryById(id: Int): Category? = categoryDao.getCategoryById(id)

    suspend fun getCategoryByNameDirect(name: String, type: String): Category? = categoryDao.getCategoryByName(name, type)

    suspend fun insertCategory(category: Category): Long = categoryDao.insertCategory(category)

    suspend fun insertCategoryDirect(category: Category): Long = categoryDao.insertCategory(category)

    suspend fun updateCategory(category: Category) = categoryDao.updateCategory(category)

    suspend fun updateCategories(categories: List<Category>) = categoryDao.updateCategories(categories)

    suspend fun deleteCategory(category: Category): Boolean {
        // Guarded deletion
        val count = transactionDao.getTransactionCountForCategory(category.id)
        if (count > 0) {
            return false // Deletion blocked
        }
        categoryDao.deleteCategory(category)
        return true
    }

    // --- TRANSACTIONS ---
    val allTransactions: Flow<List<TransactionWithDetails>> = transactionDao.getAllTransactions()

    fun getTransactionsForAccount(accountId: Int): Flow<List<TransactionWithDetails>> =
        transactionDao.getTransactionsForAccount(accountId)

    suspend fun getTransactionById(id: Int): TransactionWithDetails? = transactionDao.getTransactionById(id)

    // TRANSACTION SAFE INSERT
    suspend fun insertTransaction(transaction: Transaction): Long {
        return db.withTransaction {
            val account = accountDao.getAccountById(transaction.accountId)
            if (account != null) {
                val newBalance = if (transaction.type == "income") {
                    account.currentBalance + transaction.amount
                } else {
                    account.currentBalance - transaction.amount
                }
                accountDao.updateAccount(account.copy(currentBalance = newBalance))
            }
            transactionDao.insertTransaction(transaction)
        }
    }

    // TRANSACTION SAFE UPDATE
    suspend fun updateTransaction(newTransaction: Transaction) {
        db.withTransaction {
            val oldTransaction = transactionDao.getRawTransactionById(newTransaction.id) ?: return@withTransaction
            
            // 1. Revert old transaction effect
            val oldAccount = accountDao.getAccountById(oldTransaction.accountId)
            if (oldAccount != null) {
                val revertedBalance = if (oldTransaction.type == "income") {
                    oldAccount.currentBalance - oldTransaction.amount
                } else {
                    oldAccount.currentBalance + oldTransaction.amount
                }
                accountDao.updateAccount(oldAccount.copy(currentBalance = revertedBalance))
            }

            // 2. Apply new transaction effect
            val newAccount = accountDao.getAccountById(newTransaction.accountId)
            if (newAccount != null) {
                val appliedBalance = if (newTransaction.type == "income") {
                    newAccount.currentBalance + newTransaction.amount
                } else {
                    newAccount.currentBalance - newTransaction.amount
                }
                accountDao.updateAccount(newAccount.copy(currentBalance = appliedBalance))
            }

            // 3. Update the transaction
            transactionDao.updateTransaction(newTransaction)
        }
    }

    // TRANSACTION SAFE DELETE
    suspend fun deleteTransaction(transactionId: Int) {
        db.withTransaction {
            val transaction = transactionDao.getRawTransactionById(transactionId) ?: return@withTransaction
            
            // Revert balance effect
            val account = accountDao.getAccountById(transaction.accountId)
            if (account != null) {
                val revertedBalance = if (transaction.type == "income") {
                    account.currentBalance - transaction.amount
                } else {
                    account.currentBalance + transaction.amount
                }
                accountDao.updateAccount(account.copy(currentBalance = revertedBalance))
            }

            // Delete transaction
            transactionDao.deleteTransaction(transaction)
        }
    }

    // --- BUDGETS ---
    val allBudgets: Flow<List<Budget>> = budgetDao.getAllBudgets()

    fun getBudgetsForPeriod(month: Int, year: Int): Flow<List<Budget>> = budgetDao.getBudgetsForPeriod(month, year)

    suspend fun getBudgetForCategory(categoryId: Int?, month: Int, year: Int): Budget? =
        budgetDao.getBudgetForCategory(categoryId, month, year)

    suspend fun insertBudget(budget: Budget): Long = budgetDao.insertBudget(budget)

    suspend fun deleteBudget(budget: Budget) = budgetDao.deleteBudget(budget)

    // --- SAVINGS GOALS ---
    val allSavingsGoals: Flow<List<SavingsGoal>> = savingsGoalDao.getAllSavingsGoals()

    suspend fun getSavingsGoalById(id: Int): SavingsGoal? = savingsGoalDao.getSavingsGoalById(id)

    suspend fun insertSavingsGoal(savingsGoal: SavingsGoal): Long = savingsGoalDao.insertSavingsGoal(savingsGoal)

    suspend fun updateSavingsGoal(savingsGoal: SavingsGoal) = savingsGoalDao.updateSavingsGoal(savingsGoal)

    suspend fun deleteSavingsGoal(savingsGoal: SavingsGoal) = savingsGoalDao.deleteSavingsGoal(savingsGoal)

    // --- RECURRING TRANSACTIONS ---
    val allRecurringTransactions: Flow<List<RecurringTransaction>> = recurringTransactionDao.getAllRecurringTransactions()

    suspend fun insertRecurringTransaction(rule: RecurringTransaction): Long =
        recurringTransactionDao.insertRecurringTransaction(rule)

    suspend fun updateRecurringTransaction(rule: RecurringTransaction) =
        recurringTransactionDao.updateRecurringTransaction(rule)

    suspend fun deleteRecurringTransaction(rule: RecurringTransaction) =
        recurringTransactionDao.deleteRecurringTransaction(rule)

    // RUN RECURRING TRANSACTIONS SCHEDULER
    suspend fun processRecurringTransactions() {
        db.withTransaction {
            val nowStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val dueRules = recurringTransactionDao.getEnabledRecurringTransactions()
            for (rule in dueRules) {
                if (rule.nextExecutionDate <= nowStr) {
                    var execDateStr = rule.nextExecutionDate
                    
                    // While the execution date is in the past/today, generate transactions and advance
                    while (execDateStr <= nowStr) {
                        val transaction = Transaction(
                            amount = rule.amount,
                            type = rule.type,
                            categoryId = rule.categoryId,
                            accountId = rule.accountId,
                            note = "[Recurring] " + rule.note,
                            transactionDate = execDateStr + "T09:00:00",
                            createdAt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(Date()),
                            updatedAt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(Date())
                        )
                        
                        // Insert and update account balance
                        val account = accountDao.getAccountById(rule.accountId)
                        if (account != null) {
                            val newBalance = if (rule.type == "income") {
                                account.currentBalance + rule.amount
                            } else {
                                account.currentBalance - rule.amount
                            }
                            accountDao.updateAccount(account.copy(currentBalance = newBalance))
                        }
                        transactionDao.insertTransaction(transaction)

                        // Advance execution date
                        execDateStr = advanceDate(execDateStr, rule.frequency)
                    }

                    // Update rule in database with new execution date
                    recurringTransactionDao.updateRecurringTransaction(
                        rule.copy(nextExecutionDate = execDateStr)
                    )
                }
            }
        }
    }

    private fun advanceDate(dateStr: String, frequency: String): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val date = sdf.parse(dateStr) ?: return dateStr
        val calendar = java.util.Calendar.getInstance()
        calendar.time = date
        when (frequency.lowercase()) {
            "daily" -> calendar.add(java.util.Calendar.DAY_OF_YEAR, 1)
            "weekly" -> calendar.add(java.util.Calendar.WEEK_OF_YEAR, 1)
            "monthly" -> calendar.add(java.util.Calendar.MONTH, 1)
            "yearly" -> calendar.add(java.util.Calendar.YEAR, 1)
            else -> calendar.add(java.util.Calendar.MONTH, 1)
        }
        return sdf.format(calendar.time)
    }

    // --- SETTINGS ---
    val appSettings: Flow<Settings?> = settingsDao.getSettings()

    suspend fun getSettingsDirect(): Settings? = settingsDao.getSettingsDirect()

    suspend fun updateSettings(settings: Settings) = settingsDao.updateSettings(settings)

    // ============================================================
    // ACCOUNTING
    // ============================================================

    val allChartOfAccounts: Flow<List<ChartOfAccount>> =
        chartOfAccountDao.getAllAccounts()

    val allJournalEntries: Flow<List<JournalEntry>> =
        journalEntryDao.getAllJournalEntries()

    val allJournalLines: Flow<List<JournalLine>> =
        journalLineDao.getAllJournalLines()

    //Accounts Receivable code starts here
// ============================================================
// ACCOUNTS RECEIVABLE
// ============================================================

    val allCustomers: Flow<List<Customer>> =
        customerDao.getAllCustomers()

    val activeCustomers: Flow<List<Customer>> =
        customerDao.getActiveCustomers()

    val allCustomerInvoices: Flow<List<CustomerInvoice>> =
        customerInvoiceDao.getAllInvoices()

    val outstandingCustomerInvoices: Flow<List<CustomerInvoice>> =
        customerInvoiceDao.getOutstandingInvoices()

    val allCustomerPayments: Flow<List<CustomerPayment>> =
        customerPaymentDao.getAllPayments()

    suspend fun getCustomerById(id: Int): Customer? =
        customerDao.getCustomerById(id)

    suspend fun getCustomerByCode(code: String): Customer? =
        customerDao.getCustomerByCode(code)

    suspend fun insertCustomer(customer: Customer): Long =
        customerDao.insertCustomer(customer)

    suspend fun updateCustomer(customer: Customer) =
        customerDao.updateCustomer(customer)

    suspend fun deleteCustomer(customer: Customer) =
        customerDao.deleteCustomer(customer)

    suspend fun getCustomerInvoiceById(id: Int): CustomerInvoice? =
        customerInvoiceDao.getInvoiceById(id)

    fun getInvoicesForCustomer(customerId: Int): Flow<List<CustomerInvoice>> =
        customerInvoiceDao.getInvoicesForCustomer(customerId)

    //code starts here
    suspend fun insertCustomerInvoice(
        invoice: CustomerInvoice
    ): Long {

        return db.withTransaction {

            // ----------------------------------------------------
            // 1. Validate the invoice amount
            // ----------------------------------------------------
            require(invoice.amount > 0.0) {
                "Customer invoice amount must be greater than zero."
            }

            // ----------------------------------------------------
            // 2. Save the customer invoice
            // ----------------------------------------------------
            val invoiceId =
                customerInvoiceDao.insertInvoice(invoice)

            // ----------------------------------------------------
            // 3. Find accounting accounts
            //
            // 1200 = Accounts Receivable
            // 4000 = Sales Revenue
            // ----------------------------------------------------
            val accountsReceivableAccount =
                chartOfAccountDao.getAccountByCode("1200")
                    ?: throw IllegalStateException(
                        "Accounts Receivable account (1200) was not found."
                    )

            val salesRevenueAccount =
                chartOfAccountDao.getAccountByCode("4000")
                    ?: throw IllegalStateException(
                        "Sales Revenue account (4000) was not found."
                    )

            // ----------------------------------------------------
            // 4. Create the journal entry
            // ----------------------------------------------------
            val journalEntry = JournalEntry(
                entryDate = invoice.invoiceDate,
                referenceNumber = "CUSTOMER-INVOICE-$invoiceId",
                description = if (invoice.description.isNotBlank()) {
                    "Customer Invoice ${invoice.invoiceNumber}: ${invoice.description}"
                } else {
                    "Customer Invoice ${invoice.invoiceNumber}"
                },
                createdAt = invoice.createdAt
            )

            // ----------------------------------------------------
            // 5. Save the journal entry
            // ----------------------------------------------------
            val journalEntryId =
                journalEntryDao.insertJournalEntry(journalEntry)

            // ----------------------------------------------------
            // 6. Create the double-entry journal lines
            //
            // Debit  = Accounts Receivable
            // Credit = Sales Revenue
            // ----------------------------------------------------
            val journalLines = listOf(

                JournalLine(
                    journalEntryId = journalEntryId.toInt(),
                    accountId = accountsReceivableAccount.id,
                    debit = invoice.amount,
                    credit = 0.0,
                    description =
                        "Accounts Receivable - ${invoice.invoiceNumber}"
                ),

                JournalLine(
                    journalEntryId = journalEntryId.toInt(),
                    accountId = salesRevenueAccount.id,
                    debit = 0.0,
                    credit = invoice.amount,
                    description =
                        "Sales Revenue - ${invoice.invoiceNumber}"
                )
            )

            // ----------------------------------------------------
            // 7. Save the journal lines
            // ----------------------------------------------------
            journalLineDao.insertJournalLines(journalLines)

            // ----------------------------------------------------
            // 8. Return the customer invoice ID
            // ----------------------------------------------------
            invoiceId
        }
    }
    //ends here

    suspend fun updateCustomerInvoice(invoice: CustomerInvoice) =
        customerInvoiceDao.updateInvoice(invoice)


//code for delete invoice starts here
    suspend fun deleteCustomerInvoice(
        invoice: CustomerInvoice
    ) {
        db.withTransaction {

            // 1. Check whether this invoice has payments
            val payments =
                customerPaymentDao.getPaymentsForInvoiceOnce(invoice.id)

            require(payments.isEmpty()) {
                "This customer invoice has payments and cannot be deleted. " +
                        "Delete the related customer payments first."
            }

            // 2. Find the journal entry created for this invoice
            val journalEntry =
                journalEntryDao.getJournalEntryByReferenceNumber(
                    "CUSTOMER-INVOICE-${invoice.id}"
                )

            // 3. Delete the related journal lines and journal entry
            if (journalEntry != null) {

                journalLineDao.deleteLinesForJournalEntry(
                    journalEntry.id
                )

                journalEntryDao.deleteJournalEntry(
                    journalEntry
                )
            }

            // 4. Delete the customer invoice
            customerInvoiceDao.deleteInvoice(invoice)
        }
    }
    //code starts here

    fun getPaymentsForInvoice(invoiceId: Int): Flow<List<CustomerPayment>> =
        customerPaymentDao.getPaymentsForInvoice(invoiceId)

    fun getPaymentsForCustomer(customerId: Int): Flow<List<CustomerPayment>> =
        customerPaymentDao.getPaymentsForCustomer(customerId)

    suspend fun getTotalPaidForInvoice(invoiceId: Int): Double =
        customerPaymentDao.getTotalPaidForInvoice(invoiceId)



    //code starts here
    suspend fun insertCustomerPayment(payment: CustomerPayment): Long {
        return db.withTransaction {

            // 1. Find the invoice being paid
            val invoice =
                customerInvoiceDao.getInvoiceById(payment.invoiceId)
                    ?: throw IllegalArgumentException(
                        "Invoice ${payment.invoiceId} was not found."
                    )

            // 2. Validate payment amount
            require(payment.amount > 0.0) {
                "Customer payment amount must be greater than zero."
            }

            // 3. Calculate the current total paid
            val currentTotalPaid =
                customerPaymentDao.getTotalPaidForInvoice(
                    payment.invoiceId
                )

            // 4. Calculate the new total after this payment
            val newTotalPaid =
                currentTotalPaid + payment.amount

            // 5. Prevent overpayment
            if (newTotalPaid > invoice.amount + 0.01) {
                throw IllegalArgumentException(
                    "Payment exceeds the invoice balance."
                )
            }

            // 6. Save the customer payment first
            //    so Room generates the payment ID
            val paymentId =
                customerPaymentDao.insertPayment(payment)

            // 7. Create the Journal Entry
            val journalEntry = JournalEntry(
                entryDate = payment.paymentDate,
                referenceNumber = if (
                    payment.referenceNumber.isNotBlank()
                ) {
                    payment.referenceNumber
                } else {
                    "CUSTOMER-PAYMENT-$paymentId"
                },
                description = if (payment.notes.isNotBlank()) {
                    "Customer Payment - ${invoice.invoiceNumber}: ${payment.notes}"
                } else {
                    "Customer Payment - ${invoice.invoiceNumber}"
                },
                createdAt = payment.createdAt
            )

            // 8. Save the Journal Entry
            val journalEntryId =
                journalEntryDao.insertJournalEntry(journalEntry)

            // 9. Find Accounts Receivable
            val accountsReceivableAccount =
                chartOfAccountDao.getAccountByCode("1200")
                    ?: throw IllegalStateException(
                        "Accounts Receivable account (1200) was not found."
                    )

            // 10. Create the double-entry journal lines
            //
            // Debit  = Cash/Bank account selected by the user
            // Credit = Accounts Receivable (1200)
            val journalLines = listOf(

                JournalLine(
                    journalEntryId = journalEntryId.toInt(),
                    accountId = payment.accountId,
                    debit = payment.amount,
                    credit = 0.0,
                    description =
                        "Customer Payment - ${invoice.invoiceNumber}"
                ),

                JournalLine(
                    journalEntryId = journalEntryId.toInt(),
                    accountId = accountsReceivableAccount.id,
                    debit = 0.0,
                    credit = payment.amount,
                    description =
                        "Accounts Receivable - ${invoice.invoiceNumber}"
                )
            )

            // 11. Save the journal lines
            journalLineDao.insertJournalLines(journalLines)

            // 12. Link the payment to its Journal Entry
            customerPaymentDao.updatePayment(
                payment.copy(
                    id = paymentId.toInt(),
                    journalEntryId = journalEntryId.toInt()
                )
            )

            // 13. Determine the new invoice status
            val newStatus = when {
                newTotalPaid >= invoice.amount - 0.01 ->
                    "PAID"

                newTotalPaid > 0.0 ->
                    "PARTIALLY_PAID"

                else ->
                    "OPEN"
            }

            // 14. Update the invoice
            customerInvoiceDao.updateInvoice(
                invoice.copy(
                    amountPaid = newTotalPaid,
                    status = newStatus
                )
            )

            // 15. Return the payment ID
            paymentId
        }
    }
    //ends here

  //starts here
  suspend fun updateCustomerPayment(
      payment: CustomerPayment
  ) {
      db.withTransaction {

          // 1. Find the existing payment
          val existingPayment =
              customerPaymentDao.getPaymentById(payment.id)
                  ?: throw IllegalArgumentException(
                      "Customer payment ${payment.id} was not found."
                  )

          // 2. Update the payment
          customerPaymentDao.updatePayment(payment)

          // 3. Recalculate the OLD invoice
          recalculateCustomerInvoice(
              existingPayment.invoiceId
          )

          // 4. If the payment was moved to a different invoice,
          //    recalculate the NEW invoice as well.
          if (existingPayment.invoiceId != payment.invoiceId) {
              recalculateCustomerInvoice(
                  payment.invoiceId
              )
          }
      }
  }

    //code for deleteCustomerPayment starts here
      suspend fun deleteCustomerPayment(
        payment: CustomerPayment
    ) {
        db.withTransaction {

            // 1. Remember the invoice before deleting the payment
            val invoiceId = payment.invoiceId

            // 2. Delete the payment
            customerPaymentDao.deletePayment(payment)

            // 3. Recalculate the invoice
            recalculateCustomerInvoice(invoiceId)
        }
    }
    //ends here

    //recalculate code function starts here
    suspend fun recalculateCustomerInvoice(
        invoiceId: Int
    ) {
        db.withTransaction {

            val invoice =
                customerInvoiceDao.getInvoiceById(invoiceId)
                    ?: return@withTransaction

            val totalPaid =
                customerPaymentDao.getTotalPaidForInvoice(invoiceId)

            val newStatus = when {
                totalPaid <= 0.0 -> "OPEN"

                totalPaid >= invoice.amount -> "PAID"

                else -> "PARTIALLY_PAID"
            }

            val updatedInvoice = invoice.copy(
                amountPaid = totalPaid,
                status = newStatus
            )

            customerInvoiceDao.updateInvoice(updatedInvoice)
        }
    }
    //ends here

    // ============================================================
// ACCOUNTS PAYABLE
// ============================================================

    val allSuppliers: Flow<List<Supplier>> =
        supplierDao.getAllSuppliers()

    val activeSuppliers: Flow<List<Supplier>> =
        supplierDao.getActiveSuppliers()

    suspend fun getSupplierById(id: Int): Supplier? =
        supplierDao.getSupplierById(id)

    suspend fun getSupplierByCode(code: String): Supplier? =
        supplierDao.getSupplierByCode(code)

    suspend fun insertSupplier(supplier: Supplier): Long =
        supplierDao.insertSupplier(supplier)

    suspend fun updateSupplier(supplier: Supplier) =
        supplierDao.updateSupplier(supplier)

    suspend fun deleteSupplier(supplier: Supplier) =
        supplierDao.deleteSupplier(supplier)

    //code start here
// ============================================================
// SUPPLIER INVOICE MANAGEMENT
// ============================================================

    val allSupplierInvoices: Flow<List<SupplierInvoice>> =
        supplierInvoiceDao.getAllSupplierInvoices()

    val outstandingSupplierInvoices: Flow<List<SupplierInvoice>> =
        supplierInvoiceDao.getOutstandingSupplierInvoices()

    suspend fun getSupplierInvoices(supplierId: Int): Flow<List<SupplierInvoice>> =
        supplierInvoiceDao.getSupplierInvoices(supplierId)

    suspend fun getSupplierInvoiceById(invoiceId: Int): SupplierInvoice? =
        supplierInvoiceDao.getSupplierInvoiceById(invoiceId)

    //code start here
    suspend fun insertSupplierInvoice(
        invoice: SupplierInvoice
    ): Long {

        return db.withTransaction {

            // ----------------------------------------------------
            // 1. Validate the invoice amount
            // ----------------------------------------------------
            require(invoice.totalAmount > 0.0) {
                "Supplier invoice total must be greater than zero."
            }

            // ----------------------------------------------------
            // 2. Save the supplier invoice
            // ----------------------------------------------------
            val invoiceId =
                supplierInvoiceDao.insertSupplierInvoice(invoice)

            // ----------------------------------------------------
            // 3. Find accounting accounts
            //
            // 5000 = General Expenses
            // 2000 = Accounts Payable
            // ----------------------------------------------------
            val expenseAccount =
                chartOfAccountDao.getAccountByCode("5000")
                    ?: throw IllegalStateException(
                        "General Expenses account (5000) was not found."
                    )

            val accountsPayableAccount =
                chartOfAccountDao.getAccountByCode("2000")
                    ?: throw IllegalStateException(
                        "Accounts Payable account (2000) was not found."
                    )

            // ----------------------------------------------------
            // 4. Create the journal entry
            // ----------------------------------------------------
            val journalEntry = JournalEntry(
                entryDate = invoice.invoiceDate,
                referenceNumber = "SUPPLIER-INVOICE-$invoiceId",
                description = if (invoice.description.isNotBlank()) {
                    "Supplier Invoice ${invoice.invoiceNumber}: ${invoice.description}"
                } else {
                    "Supplier Invoice ${invoice.invoiceNumber}"
                },
                createdAt = invoice.createdAt
            )

            // ----------------------------------------------------
            // 5. Save the journal entry
            // ----------------------------------------------------
            val journalEntryId =
                journalEntryDao.insertJournalEntry(journalEntry)

            // ----------------------------------------------------
            // 6. Create the double-entry journal lines
            //
            // Debit  = General Expenses
            // Credit = Accounts Payable
            // ----------------------------------------------------
            val journalLines = listOf(

                JournalLine(
                    journalEntryId = journalEntryId.toInt(),
                    accountId = expenseAccount.id,
                    debit = invoice.totalAmount,
                    credit = 0.0,
                    description = "Supplier Invoice ${invoice.invoiceNumber}"
                ),

                JournalLine(
                    journalEntryId = journalEntryId.toInt(),
                    accountId = accountsPayableAccount.id,
                    debit = 0.0,
                    credit = invoice.totalAmount,
                    description = "Accounts Payable - ${invoice.invoiceNumber}"
                )
            )

            // ----------------------------------------------------
            // 7. Save the journal lines
            // ----------------------------------------------------
            journalLineDao.insertJournalLines(journalLines)

            // ----------------------------------------------------
            // 8. Return the supplier invoice ID
            // ----------------------------------------------------
            invoiceId
        }
    }
    //ends here

    suspend fun updateSupplierInvoice(invoice: SupplierInvoice) =
        supplierInvoiceDao.updateSupplierInvoice(invoice)

   //start here
   suspend fun deleteSupplierInvoice(
       invoice: SupplierInvoice
   ) {
       db.withTransaction {

           // ----------------------------------------------------
           // 1. Check whether the invoice has payments
           // ----------------------------------------------------
           val payments =
               supplierPaymentDao.getPaymentsForInvoiceOnce(
                   invoice.id
               )

           require(payments.isEmpty()) {
               "This supplier invoice has payments and cannot be deleted. " +
                       "Delete the related supplier payments first."
           }

           // ----------------------------------------------------
           // 2. Find the journal entry created for this invoice
           // ----------------------------------------------------
           val journalEntry =
               journalEntryDao.getJournalEntryByReferenceNumber(
                   "SUPPLIER-INVOICE-${invoice.id}"
               )

           // ----------------------------------------------------
           // 3. Delete the journal lines and journal entry
           // ----------------------------------------------------
           if (journalEntry != null) {

               journalLineDao.deleteLinesForJournalEntry(
                   journalEntry.id
               )

               journalEntryDao.deleteJournalEntry(
                   journalEntry
               )
           }

           // ----------------------------------------------------
           // 4. Delete the supplier invoice
           // ----------------------------------------------------
           supplierInvoiceDao.deleteSupplierInvoice(invoice)
       }
   }
    //ends here

    suspend fun getChartOfAccountById(id: Int): ChartOfAccount? =
        chartOfAccountDao.getAccountById(id)

    suspend fun getChartOfAccountByCode(code: String): ChartOfAccount? =
        chartOfAccountDao.getAccountByCode(code)

    suspend fun insertChartOfAccount(account: ChartOfAccount): Long =
        chartOfAccountDao.insertAccount(account)

    suspend fun updateChartOfAccount(account: ChartOfAccount) =
        chartOfAccountDao.updateAccount(account)

    suspend fun deleteChartOfAccount(account: ChartOfAccount) =
        chartOfAccountDao.deleteAccount(account)

    suspend fun getJournalEntryById(id: Int): JournalEntry? =
        journalEntryDao.getJournalEntryById(id)

    suspend fun getJournalLines(journalEntryId: Int): List<JournalLine> =
        journalLineDao.getLinesForJournalEntry(journalEntryId)

    fun getLedgerLines(accountId: Int): Flow<List<JournalLine>> =
        journalLineDao.getLinesForAccount(accountId)

    //code starts here

    //code starts here
// ============================================================
// SUPPLIER PAYMENT MANAGEMENT
// ============================================================

    val allSupplierPayments: Flow<List<SupplierPayment>> =
        supplierPaymentDao.getAllSupplierPayments()

    suspend fun getSupplierPayments(
        supplierId: Int
    ): Flow<List<SupplierPayment>> =
        supplierPaymentDao.getSupplierPayments(supplierId)

    suspend fun getPaymentsForSupplierInvoice(
        invoiceId: Int
    ): Flow<List<SupplierPayment>> =
        supplierPaymentDao.getPaymentsForInvoice(invoiceId)

    suspend fun getSupplierInvoicePaymentsOnce(
        invoiceId: Int
    ): List<SupplierPayment> =
        supplierPaymentDao.getPaymentsForInvoiceOnce(invoiceId)

    suspend fun getSupplierPaymentById(
        paymentId: Int
    ): SupplierPayment? =
        supplierPaymentDao.getSupplierPaymentById(paymentId)

    suspend fun validateSupplierPayment(
        payment: SupplierPayment
    ): String? {
        val invoice =
            supplierInvoiceDao.getSupplierInvoiceById(
                payment.supplierInvoiceId
            ) ?: return "Supplier invoice not found."

        val existingPayment =
            if (payment.id != 0) {
                supplierPaymentDao.getSupplierPaymentById(payment.id)
            } else {
                null
            }

        val existingPaymentAmount =
            if (
                existingPayment != null &&
                existingPayment.supplierInvoiceId == payment.supplierInvoiceId
            ) {
                existingPayment.amount
            } else {
                0.0
            }

        val currentPaid =
            invoice.amountPaid - existingPaymentAmount

        val newTotalPaid =
            currentPaid + payment.amount

        return if (newTotalPaid > invoice.totalAmount) {
            "Payment exceeds the outstanding invoice balance."
        } else {
            null
        }
    }

  //code starts here
  suspend fun insertSupplierPayment(
      payment: SupplierPayment
  ): Long {

      return db.withTransaction {

          // ----------------------------------------------------
          // 1. Save the supplier payment
          // ----------------------------------------------------
          val paymentId =
              supplierPaymentDao.insertSupplierPayment(payment)

          // ----------------------------------------------------
          // 2. Recalculate the supplier invoice
          // ----------------------------------------------------
          val invoice =
              supplierInvoiceDao.getSupplierInvoiceById(
                  payment.supplierInvoiceId
              ) ?: throw IllegalStateException(
                  "Supplier invoice not found."
              )

          val payments =
              supplierPaymentDao.getPaymentsForInvoiceOnce(
                  payment.supplierInvoiceId
              )

          val totalPaid =
              payments.sumOf { it.amount }

          val newStatus = when {
              totalPaid <= 0.0 -> "OPEN"

              totalPaid >= invoice.totalAmount -> "PAID"

              else -> "PARTIALLY_PAID"
          }

          val updatedInvoice = invoice.copy(
              amountPaid = totalPaid,
              status = newStatus
          )

          supplierInvoiceDao.updateSupplierInvoice(
              updatedInvoice
          )

          // ----------------------------------------------------
          // 3. Find Accounts Payable account
          // ----------------------------------------------------
          val accountsPayableAccount =
              chartOfAccountDao.getAccountByCode("2000")
                  ?: throw IllegalStateException(
                      "Accounts Payable account (2000) was not found."
                  )

          // ----------------------------------------------------
          // 4. Determine payment account
          //
          // CASH = 1000
          // BANK = 1100
          // ----------------------------------------------------
          val paymentAccountCode =
              when (payment.paymentMethod.trim().uppercase()) {

                  "BANK",
                  "BANK ACCOUNT" -> "1100"

                  else -> "1000"
              }

          val paymentAccount =
              chartOfAccountDao.getAccountByCode(
                  paymentAccountCode
              ) ?: throw IllegalStateException(
                  "Payment account ($paymentAccountCode) was not found."
              )

          // ----------------------------------------------------
          // 5. Create the journal entry
          // ----------------------------------------------------
          val journalEntry = JournalEntry(
              entryDate = payment.paymentDate,
              referenceNumber = "SUPPLIER-PAYMENT-$paymentId",
              description =
                  if (payment.referenceNumber.isNotBlank()) {
                      "Supplier Payment ${payment.referenceNumber}"
                  } else {
                      "Supplier Payment"
                  },
              createdAt = payment.createdAt
          )

          // ----------------------------------------------------
          // 6. Create the double-entry journal lines
          //
          // Debit  = Accounts Payable
          // Credit = Cash or Bank
          // ----------------------------------------------------
          val journalLines = listOf(

              JournalLine(
                  journalEntryId = 0,
                  accountId = accountsPayableAccount.id,
                  debit = payment.amount,
                  credit = 0.0,
                  description =
                      "Accounts Payable - Supplier Payment $paymentId"
              ),

              JournalLine(
                  journalEntryId = 0,
                  accountId = paymentAccount.id,
                  debit = 0.0,
                  credit = payment.amount,
                  description =
                      if (paymentAccountCode == "1100") {
                          "Bank Payment - Supplier Payment $paymentId"
                      } else {
                          "Cash Payment - Supplier Payment $paymentId"
                      }
              )
          )

          // ----------------------------------------------------
          // 7. Save the journal entry and journal lines
          // ----------------------------------------------------
          createJournalEntry(
              entry = journalEntry,
              lines = journalLines
          )

          // ----------------------------------------------------
          // 8. Return the Supplier Payment ID
          // ----------------------------------------------------
          paymentId
      }
  }
    //ends here


    //new delete() code starts here
    suspend fun updateSupplierPayment(
        payment: SupplierPayment
    ) {
        db.withTransaction {

            // ----------------------------------------------------
            // 1. Get the existing payment
            // ----------------------------------------------------
            val existingPayment =
                supplierPaymentDao.getSupplierPaymentById(payment.id)
                    ?: throw IllegalStateException(
                        "Supplier payment not found."
                    )

            // ----------------------------------------------------
            // 2. Find the journal entry created for this payment
            // ----------------------------------------------------
            val referenceNumber =
                "SUPPLIER-PAYMENT-${payment.id}"

            val journalEntry =
                journalEntryDao.getJournalEntryByReferenceNumber(
                    referenceNumber
                )

            // ----------------------------------------------------
            // 3. Update the Supplier Payment
            // ----------------------------------------------------
            supplierPaymentDao.updateSupplierPayment(payment)

            // ----------------------------------------------------
            // 4. Update the accounting journal
            // ----------------------------------------------------
            if (journalEntry != null) {

                // Delete the old journal lines first.
                journalLineDao.deleteLinesForJournalEntry(
                    journalEntry.id
                )

                // Find Accounts Payable
                val accountsPayableAccount =
                    chartOfAccountDao.getAccountByCode("2000")
                        ?: throw IllegalStateException(
                            "Accounts Payable account (2000) was not found."
                        )

                // Determine CASH or BANK account
                val paymentAccountCode =
                    when (payment.paymentMethod.trim().uppercase()) {
                        "BANK",
                        "BANK ACCOUNT" -> "1100"

                        else -> "1000"
                    }

                val paymentAccount =
                    chartOfAccountDao.getAccountByCode(
                        paymentAccountCode
                    ) ?: throw IllegalStateException(
                        "Payment account ($paymentAccountCode) was not found."
                    )

                // ------------------------------------------------
                // Update Journal Entry information
                // ------------------------------------------------
                val updatedJournalEntry =
                    journalEntry.copy(
                        entryDate = payment.paymentDate,
                        description =
                            if (payment.referenceNumber.isNotBlank()) {
                                "Supplier Payment ${payment.referenceNumber}"
                            } else {
                                "Supplier Payment"
                            }
                    )

                journalEntryDao.updateJournalEntry(
                    updatedJournalEntry
                )

                // ------------------------------------------------
                // Create the new journal lines
                // ------------------------------------------------
                val updatedJournalLines =
                    listOf(

                        // Debit Accounts Payable
                        JournalLine(
                            journalEntryId = journalEntry.id,
                            accountId =
                                accountsPayableAccount.id,
                            debit = payment.amount,
                            credit = 0.0,
                            description =
                                "Accounts Payable - Supplier Payment ${payment.id}"
                        ),

                        // Credit Cash or Bank
                        JournalLine(
                            journalEntryId = journalEntry.id,
                            accountId =
                                paymentAccount.id,
                            debit = 0.0,
                            credit = payment.amount,
                            description =
                                if (paymentAccountCode == "1100") {
                                    "Bank Payment - Supplier Payment ${payment.id}"
                                } else {
                                    "Cash Payment - Supplier Payment ${payment.id}"
                                }
                        )
                    )

                journalLineDao.insertJournalLines(
                    updatedJournalLines
                )
            }

            // ----------------------------------------------------
            // 5. Recalculate the new supplier invoice
            // ----------------------------------------------------
            val newInvoice =
                supplierInvoiceDao.getSupplierInvoiceById(
                    payment.supplierInvoiceId
                )

            if (newInvoice != null) {

                val payments =
                    supplierPaymentDao.getPaymentsForInvoiceOnce(
                        payment.supplierInvoiceId
                    )

                val totalPaid =
                    payments.sumOf { it.amount }

                val newStatus = when {
                    totalPaid <= 0.0 -> "OPEN"

                    totalPaid >= newInvoice.totalAmount -> "PAID"

                    else -> "PARTIALLY_PAID"
                }

                supplierInvoiceDao.updateSupplierInvoice(
                    newInvoice.copy(
                        amountPaid = totalPaid,
                        status = newStatus
                    )
                )
            }

            // ----------------------------------------------------
            // 6. If payment was moved to another invoice,
            //    recalculate the old invoice too.
            // ----------------------------------------------------
            if (
                existingPayment.supplierInvoiceId !=
                payment.supplierInvoiceId
            ) {

                val oldInvoice =
                    supplierInvoiceDao.getSupplierInvoiceById(
                        existingPayment.supplierInvoiceId
                    )

                if (oldInvoice != null) {

                    val oldPayments =
                        supplierPaymentDao.getPaymentsForInvoiceOnce(
                            existingPayment.supplierInvoiceId
                        )

                    val oldTotalPaid =
                        oldPayments.sumOf { it.amount }

                    val oldStatus = when {
                        oldTotalPaid <= 0.0 -> "OPEN"

                        oldTotalPaid >= oldInvoice.totalAmount ->
                            "PAID"

                        else ->
                            "PARTIALLY_PAID"
                    }

                    supplierInvoiceDao.updateSupplierInvoice(
                        oldInvoice.copy(
                            amountPaid = oldTotalPaid,
                            status = oldStatus
                        )
                    )
                }
            }
        }
    }
    //ends here

    //delete method function starts here
     suspend fun deleteSupplierPayment(
        payment: SupplierPayment
    ) {
        db.withTransaction {

            // ----------------------------------------------------
            // 1. Find the journal entry created for this payment
            // ----------------------------------------------------
            val referenceNumber =
                "SUPPLIER-PAYMENT-${payment.id}"

            val journalEntry =
                journalEntryDao.getJournalEntryByReferenceNumber(
                    referenceNumber
                )

            // ----------------------------------------------------
            // 2. Delete the related journal entry and its lines
            // ----------------------------------------------------
            if (journalEntry != null) {

                journalLineDao.deleteLinesForJournalEntry(
                    journalEntry.id
                )

                journalEntryDao.deleteJournalEntry(
                    journalEntry
                )
            }

            // ----------------------------------------------------
            // 3. Delete the Supplier Payment
            // ----------------------------------------------------
            supplierPaymentDao.deleteSupplierPayment(
                payment
            )

            // ----------------------------------------------------
            // 4. Recalculate the Supplier Invoice
            // ----------------------------------------------------
            val invoice =
                supplierInvoiceDao.getSupplierInvoiceById(
                    payment.supplierInvoiceId
                )

            if (invoice != null) {

                val payments =
                    supplierPaymentDao.getPaymentsForInvoiceOnce(
                        payment.supplierInvoiceId
                    )

                val totalPaid =
                    payments.sumOf { it.amount }

                val newStatus = when {
                    totalPaid <= 0.0 ->
                        "OPEN"

                    totalPaid >= invoice.totalAmount ->
                        "PAID"

                    else ->
                        "PARTIALLY_PAID"
                }

                supplierInvoiceDao.updateSupplierInvoice(
                    invoice.copy(
                        amountPaid = totalPaid,
                        status = newStatus
                    )
                )
            }
        }
    }
    //ends here

    //code starts here
    suspend fun recalculateSupplierInvoice(
        invoiceId: Int
    ) {
        db.withTransaction {

            val invoice =
                supplierInvoiceDao.getSupplierInvoiceById(invoiceId)
                    ?: return@withTransaction

            val payments =
                supplierPaymentDao.getPaymentsForInvoiceOnce(invoiceId)

            val totalPaid =
                payments.sumOf { it.amount }

            val newStatus = when {
                totalPaid <= 0.0 -> "OPEN"

                totalPaid >= invoice.totalAmount -> "PAID"

                else -> "PARTIALLY_PAID"
            }

            val updatedInvoice = invoice.copy(
                amountPaid = totalPaid,
                status = newStatus
            )

            supplierInvoiceDao.updateSupplierInvoice(updatedInvoice)
        }
    }
    //ends here

    fun getGeneralLedger(accountId: Int): Flow<List<LedgerLine>> {
        return combine(
            journalLineDao.getLinesForAccount(accountId),
            allJournalEntries,
            chartOfAccountDao.getAllAccounts()
        ) { lines, entries, accounts ->

            val entryMap = entries.associateBy { it.id }
            val account = accounts.find { it.id == accountId }

            if (account == null) {
                return@combine emptyList()
            }

            // Debit-normal accounts:
            // Assets and Expenses
            //
            // Credit-normal accounts:
            // Liabilities, Equity and Revenue
            val isDebitNormal = when (account.accountType.uppercase()) {
                "ASSET",
                "EXPENSE" -> true

                "LIABILITY",
                "EQUITY",
                "REVENUE" -> false

                else -> true
            }

            var runningBalance = 0.0

            lines
                .sortedWith(
                    compareBy<JournalLine> {
                        entryMap[it.journalEntryId]?.entryDate ?: ""
                    }.thenBy {
                        it.journalEntryId
                    }
                )
                .mapNotNull { line ->

                    val entry = entryMap[line.journalEntryId]
                        ?: return@mapNotNull null

                    val balanceChange = if (isDebitNormal) {
                        line.debit - line.credit
                    } else {
                        line.credit - line.debit
                    }

                    runningBalance += balanceChange

                    LedgerLine(
                        journalEntryId = entry.id,
                        date = entry.entryDate,
                        referenceNumber = entry.referenceNumber,
                        description = if (line.description.isNotBlank()) {
                            line.description
                        } else {
                            entry.description
                        },
                        debit = line.debit,
                        credit = line.credit,
                        balance = runningBalance
                    )
                }
        }
    }
    //General Ledger code block ends here
    //ends here

    /**
     * Creates a complete double-entry journal transaction.
     *
     * Every journal entry must have:
     *
     * Total Debits == Total Credits
     */
    suspend fun createJournalEntry(
        entry: JournalEntry,
        lines: List<JournalLine>
    ): Long {

        require(lines.isNotEmpty()) {
            "A journal entry must contain at least one journal line."
        }

        val totalDebit = lines.sumOf { it.debit }
        val totalCredit = lines.sumOf { it.credit }

        require(totalDebit > 0.0 || totalCredit > 0.0) {
            "A journal entry must contain a debit or credit."
        }

        require(kotlin.math.abs(totalDebit - totalCredit) < 0.01) {
            "Journal entry is not balanced. Debit: $totalDebit, Credit: $totalCredit"
        }

        require(lines.all { it.debit >= 0.0 && it.credit >= 0.0 }) {
            "Debit and credit amounts cannot be negative."
        }

        require(lines.all { !(it.debit > 0.0 && it.credit > 0.0) }) {
            "A journal line cannot contain both debit and credit."
        }

        return db.withTransaction {

            val journalEntryId =
                journalEntryDao.insertJournalEntry(entry)

            val preparedLines = lines.map { line ->
                line.copy(
                    journalEntryId = journalEntryId.toInt()
                )
            }

            journalLineDao.insertJournalLines(preparedLines)

            journalEntryId
        }
    }

    suspend fun deleteJournalEntry(entry: JournalEntry) {
        db.withTransaction {

            journalLineDao.deleteLinesForJournalEntry(entry.id)

            journalEntryDao.deleteJournalEntry(entry)
        }
    }
}
