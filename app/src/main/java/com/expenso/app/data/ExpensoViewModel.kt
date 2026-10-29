package com.expenso.app.data

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ExpensoViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = Repository(db)

    // --- SHARED PREFERENCES FOR PIN ---
    private val prefs = application.getSharedPreferences("expenso_prefs", Context.MODE_PRIVATE)

    // --- CUSTOM ACCOUNT TYPES STATE ---
    private val _accountTypes = MutableStateFlow<List<String>>(emptyList())
    val accountTypes = _accountTypes.asStateFlow()

    // --- SEARCH HISTORY STATE ---
    private val _searchHistory = MutableStateFlow<List<String>>(emptyList())
    val searchHistory = _searchHistory.asStateFlow()

    fun addSearchQuery(query: String) {
        if (query.isBlank()) return
        val current = _searchHistory.value.toMutableList()
        val trimmed = query.trim()
        current.remove(trimmed)
        current.add(0, trimmed)
        val limited = current.take(10)
        _searchHistory.value = limited
        prefs.edit().putString("search_history", limited.joinToString(",")).apply()
    }

    fun removeSearchQuery(query: String) {
        val current = _searchHistory.value.toMutableList()
        current.remove(query)
        _searchHistory.value = current
        prefs.edit().putString("search_history", current.joinToString(",")).apply()
    }

    fun clearSearchHistory() {
        _searchHistory.value = emptyList()
        prefs.edit().remove("search_history").apply()
    }

    fun addAccountType(newType: String) {
        val current = _accountTypes.value.toMutableSet()
        current.add(newType)
        prefs.edit().putStringSet("custom_account_types", current).apply()
        _accountTypes.value = current.toList().sorted()
    }

    fun editAccountType(oldType: String, newType: String) {
        val current = _accountTypes.value.toMutableSet()
        if (current.contains(oldType)) {
            current.remove(oldType)
            current.add(newType)
            prefs.edit().putStringSet("custom_account_types", current).apply()
            _accountTypes.value = current.toList().sorted()

            // Update existing accounts of oldType to newType in database
            viewModelScope.launch {
                val accounts = allAccounts.value
                accounts.forEach { acc ->
                    if (acc.type == oldType) {
                        repository.updateAccount(acc.copy(type = newType))
                    }
                }
            }
        }
    }

    // --- NAVIGATION STATE ---
    private val _currentTab = MutableStateFlow(0) // 0: Records, 1: Analysis, 2: Accounts, 3: More
    val currentTab = _currentTab.asStateFlow()

    fun selectTab(index: Int) {
        _currentTab.value = index
    }

    // --- SCROLL STATE ---
    private val _isScrolling = MutableStateFlow(false)
    val isScrolling = _isScrolling.asStateFlow()

    fun setScrolling(scrolling: Boolean) {
        _isScrolling.value = scrolling
    }

    // --- FILE PROCESSING STATE ---
    private val _isProcessingFile = MutableStateFlow(false)
    val isProcessingFile = _isProcessingFile.asStateFlow()

    private val _fileProcessingMessage = MutableStateFlow("")
    val fileProcessingMessage = _fileProcessingMessage.asStateFlow()

    fun setFileProcessing(processing: Boolean, message: String = "") {
        _isProcessingFile.value = processing
        _fileProcessingMessage.value = message
    }

    var isRecordsInitialLoadDone = false

    // --- PIN & LOCK STATE ---
    private val _isLocked = MutableStateFlow(false)
    val isLocked = _isLocked.asStateFlow()

    private val _isPinSet = MutableStateFlow(false)
    val isPinSet = _isPinSet.asStateFlow()

    private val saltKey = "expenso_salt_key_12345"

    init {
        val pinHash = prefs.getString("pin_hash", null)
        _isPinSet.value = !pinHash.isNullOrEmpty()
        _isLocked.value = !pinHash.isNullOrEmpty()

        // Load custom account types
        val storedTypes = prefs.getStringSet("custom_account_types", null)
        if (storedTypes == null) {
            val defaults =
                setOf("Cash", "Bank Account", "UPI", "Credit Card", "Debit Card", "Wallet", "Other")
            prefs.edit().putStringSet("custom_account_types", defaults).apply()
            _accountTypes.value = defaults.toList().sorted()
        } else {
            _accountTypes.value = storedTypes.toList().sorted()
        }

        // Load search history
        val historyStr = prefs.getString("search_history", "")
        if (!historyStr.isNullOrEmpty()) {
            _searchHistory.value = historyStr.split(",").filter { it.isNotBlank() }
        }

        // Process recurring transactions on view model initialization
        processRecurringTransactions()

        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                var cats = repository.allCategories.first()
                if (cats.any { it.name == "Food & Dining" } || cats.any { it.name == "Income" }) {
                    cats.forEach { repository.categoryDao.deleteCategory(it) }
                    cats = emptyList()
                }

                if (cats.isEmpty()) {
                    val defaultCats = listOf(
                        Category(
                            name = "Bills",
                            type = "expense",
                            icon = "receipt_long",
                            color = "#FFDFBA",
                            isDefault = false,
                            orderIndex = 1
                        ),
                        Category(
                            name = "Car",
                            type = "expense",
                            icon = "directions_car",
                            color = "#FFB3BA",
                            isDefault = false,
                            orderIndex = 2
                        ),
                        Category(
                            name = "Clothing",
                            type = "expense",
                            icon = "checkroom",
                            color = "#BDB2FF",
                            isDefault = false,
                            orderIndex = 3
                        ),
                        Category(
                            name = "Education",
                            type = "expense",
                            icon = "school",
                            color = "#D8B4FE",
                            isDefault = false,
                            orderIndex = 4
                        ),
                        Category(
                            name = "Electronics",
                            type = "expense",
                            icon = "devices",
                            color = "#FFCAD4",
                            isDefault = false,
                            orderIndex = 5
                        ),
                        Category(
                            name = "Entertainment",
                            type = "expense",
                            icon = "sports_esports",
                            color = "#A0C4FF",
                            isDefault = false,
                            orderIndex = 6
                        ),
                        Category(
                            name = "Food",
                            type = "expense",
                            icon = "restaurant",
                            color = "#D0F4DE",
                            isDefault = false,
                            orderIndex = 7
                        ),
                        Category(
                            name = "Health",
                            type = "expense",
                            icon = "medical_services",
                            color = "#FCE1E4",
                            isDefault = false,
                            orderIndex = 8
                        ),
                        Category(
                            name = "Home",
                            type = "expense",
                            icon = "home",
                            color = "#E2F0D9",
                            isDefault = false,
                            orderIndex = 9
                        ),
                        Category(
                            name = "Insurance",
                            type = "expense",
                            icon = "shield",
                            color = "#FCF6BD",
                            isDefault = false,
                            orderIndex = 10
                        ),
                        Category(
                            name = "Shopping",
                            type = "expense",
                            icon = "shopping_bag",
                            color = "#A9DEF9",
                            isDefault = false,
                            orderIndex = 11
                        ),
                        Category(
                            name = "Social",
                            type = "expense",
                            icon = "groups",
                            color = "#E4C1F9",
                            isDefault = false,
                            orderIndex = 12
                        ),
                        Category(
                            name = "Tax",
                            type = "expense",
                            icon = "account_balance",
                            color = "#D0F4EA",
                            isDefault = false,
                            orderIndex = 13
                        ),
                        Category(
                            name = "Mobile",
                            type = "expense",
                            icon = "phone_android",
                            color = "#FFD6A5",
                            isDefault = false,
                            orderIndex = 14
                        ),
                        Category(
                            name = "Transportation",
                            type = "expense",
                            icon = "directions_bus",
                            color = "#FFFACD",
                            isDefault = false,
                            orderIndex = 15
                        ),
                        Category(
                            name = "Others",
                            type = "expense",
                            icon = "more_horiz",
                            color = "#FAFAD2",
                            isDefault = false,
                            orderIndex = 16
                        ),
                        Category(
                            name = "Wifi & Internet",
                            type = "expense",
                            icon = "wifi",
                            color = "#FFEFD5",
                            isDefault = false,
                            orderIndex = 17
                        ),
                        Category(
                            name = "Gym",
                            type = "expense",
                            icon = "fitness_center",
                            color = "#FFE4B5",
                            isDefault = false,
                            orderIndex = 18
                        ),

                        Category(
                            name = "Others",
                            type = "income",
                            icon = "more_horiz",
                            color = "#BFFCC6",
                            isDefault = false,
                            orderIndex = 19
                        ),
                        Category(
                            name = "Awards",
                            type = "income",
                            icon = "emoji_events",
                            color = "#CAFFBF",
                            isDefault = false,
                            orderIndex = 20
                        ),
                        Category(
                            name = "Coupons",
                            type = "income",
                            icon = "local_offer",
                            color = "#9BF6FF",
                            isDefault = false,
                            orderIndex = 21
                        ),
                        Category(
                            name = "Grants",
                            type = "income",
                            icon = "school",
                            color = "#FFFFBA",
                            isDefault = false,
                            orderIndex = 22
                        ),
                        Category(
                            name = "Lottery",
                            type = "income",
                            icon = "casino",
                            color = "#FFCAD4",
                            isDefault = false,
                            orderIndex = 23
                        ),
                        Category(
                            name = "Refunds",
                            type = "income",
                            icon = "receipt_long",
                            color = "#A0C4FF",
                            isDefault = false,
                            orderIndex = 24
                        ),
                        Category(
                            name = "Rental",
                            type = "income",
                            icon = "home",
                            color = "#D8B4FE",
                            isDefault = false,
                            orderIndex = 25
                        ),
                        Category(
                            name = "Salary",
                            type = "income",
                            icon = "payments",
                            color = "#B9FBC0",
                            isDefault = false,
                            orderIndex = 26
                        ),
                        Category(
                            name = "Sold Items",
                            type = "income",
                            icon = "storefront",
                            color = "#FBF8CC",
                            isDefault = false,
                            orderIndex = 27
                        )
                    )
                    defaultCats.forEach { repository.categoryDao.insertCategory(it) }

                    repository.settingsDao.insertSettings(
                        Settings(
                            id = 1,
                            themeMode = "system",
                            currency = "₹",
                            pinEnabled = false,
                            biometricEnabled = false,
                            notificationsEnabled = true,
                            colorPalette = "Default"
                        )
                    )
                }


// Seed Default Chart of Accounts
                // Seed Default Accounts if there are no accounts
                val accounts = repository.allAccounts.first()

                if (accounts.isEmpty()) {
                    repository.accountDao.insertAccount(
                        Account(
                            name = "Cash",
                            type = "Cash",
                            openingBalance = 0.0,
                            currentBalance = 0.0,
                            icon = "wallet",
                            color = "#FFD6A5"
                        )
                    )

                    repository.accountDao.insertAccount(
                        Account(
                            name = "Bank Account",
                            type = "Bank Account",
                            openingBalance = 0.0,
                            currentBalance = 0.0,
                            icon = "account_balance",
                            color = "#A0C4FF"
                        )
                    )
                }

// Seed Default Chart of Accounts
                val chartAccounts = repository.allChartOfAccounts.first()

                if (chartAccounts.isEmpty()) {

                    val defaultChartAccounts = listOf(
                        ChartOfAccount(
                            accountCode = "1000",
                            accountName = "Cash",
                            accountType = "ASSET",
                            description = "Cash on hand"
                        ),

                        ChartOfAccount(
                            accountCode = "1100",
                            accountName = "Bank Account",
                            accountType = "ASSET",
                            description = "Money held in bank accounts"
                        ),

                        ChartOfAccount(
                            accountCode = "1200",
                            accountName = "Accounts Receivable",
                            accountType = "ASSET",
                            description = "Amounts owed by customers and other parties"
                        ),

                        ChartOfAccount(
                            accountCode = "2000",
                            accountName = "Accounts Payable",
                            accountType = "LIABILITY",
                            description = "Amounts owed to suppliers and other parties"
                        ),

                        ChartOfAccount(
                            accountCode = "3000",
                            accountName = "Owner's Equity",
                            accountType = "EQUITY",
                            description = "Owner's investment in the business"
                        ),

                        ChartOfAccount(
                            accountCode = "4000",
                            accountName = "Sales Revenue",
                            accountType = "REVENUE",
                            description = "Income generated from sales"
                        ),

                        ChartOfAccount(
                            accountCode = "5000",
                            accountName = "General Expenses",
                            accountType = "EXPENSE",
                            description = "General operating expenses"
                        )
                    )

                    defaultChartAccounts.forEach { account ->
                        repository.insertChartOfAccount(account)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

        }


    }//viewModelScope block code


    fun setPin(pin: String) {
        if (pin.length in 4..6) {
            val hash = sha256(pin, saltKey)
            prefs.edit().putString("pin_hash", hash).apply()
            _isPinSet.value = true
            _isLocked.value = false // unlocked upon setting
        }
    }

    fun disablePin() {
        prefs.edit().remove("pin_hash").apply()
        _isPinSet.value = false
        _isLocked.value = false
    }

    fun verifyPin(pin: String): Boolean {
        val storedHash = prefs.getString("pin_hash", null) ?: return true
        val checkHash = sha256(pin, saltKey)
        val success = (storedHash == checkHash)
        if (success) {
            _isLocked.value = false
        }
        return success
    }

    fun unlockApp() {
        _isLocked.value = false
    }

    fun lockApp() {
        if (_isPinSet.value || (settings.value?.biometricEnabled ?: false)) {
            _isLocked.value = true
        }
    }

    private fun sha256(input: String, salt: String): String {
        return try {
            val md = MessageDigest.getInstance("SHA-256")
            val bytes = md.digest((input + salt).toByteArray(Charsets.UTF_8))
            bytes.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            ""
        }
    }

    // --- DATA STREAM DECLARATIONS ---
    val allAccounts = repository.allAccounts
        .onEach { list -> saveCachedAccounts(list) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = loadCachedAccounts()
        )

    val allCategories = repository.allCategories
        .onEach { list -> saveCachedCategories(list) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = loadCachedCategories()
        )

    val allTransactions = repository.allTransactions
        .onEach { list -> saveCachedTransactions(list) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = loadCachedTransactions()
        )

    val allBudgets = repository.allBudgets.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allSavingsGoals = repository.allSavingsGoals.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allRecurringTransactions = repository.allRecurringTransactions.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

// --- ACCOUNTING SYSTEM ---

    val allChartOfAccounts = repository.allChartOfAccounts
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allJournalEntries = repository.allJournalEntries
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allJournalLines = repository.allJournalLines
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    //code starts here
// ============================================================
// ACCOUNTS RECEIVABLE
// ============================================================

    val allCustomers: StateFlow<List<Customer>> =
        repository.allCustomers.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val activeCustomers: StateFlow<List<Customer>> =
        repository.activeCustomers.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val allCustomerInvoices: StateFlow<List<CustomerInvoice>> =
        repository.allCustomerInvoices.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val outstandingCustomerInvoices: StateFlow<List<CustomerInvoice>> =
        repository.outstandingCustomerInvoices.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val allCustomerPayments: StateFlow<List<CustomerPayment>> =
        repository.allCustomerPayments.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )
    //ends here

    //code starts here

    //ends here
    // ============================================================
// ACCOUNTS PAYABLE
// ============================================================

    val allSuppliers: StateFlow<List<Supplier>> =
        repository.allSuppliers.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val activeSuppliers: StateFlow<List<Supplier>> =
        repository.activeSuppliers.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    // ============================================================
// SUPPLIER MANAGEMENT
// ============================================================

    //CODE STARTS HERE
    val allSupplierInvoices: StateFlow<List<SupplierInvoice>> =
        repository.allSupplierInvoices.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val allSupplierPayments: StateFlow<List<SupplierPayment>> =
        repository.allSupplierPayments.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val outstandingSupplierInvoices: StateFlow<List<SupplierInvoice>> =
        repository.outstandingSupplierInvoices.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )
    //ENDS HERE

    fun addSupplier(
        supplier: Supplier,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                repository.insertSupplier(supplier)
                onSuccess()
            } catch (e: Exception) {
                e.printStackTrace()
                onError(e.message ?: "Failed to add supplier.")
            }
        }
    }

    fun updateSupplier(
        supplier: Supplier,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                repository.updateSupplier(supplier)
                onSuccess()
            } catch (e: Exception) {
                e.printStackTrace()
                onError(e.message ?: "Failed to update supplier.")
            }
        }
    }

    fun deleteSupplier(
        supplier: Supplier,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                repository.deleteSupplier(supplier)
                onSuccess()
            } catch (e: Exception) {
                e.printStackTrace()
                onError(e.message ?: "Failed to delete supplier.")
            }
        }
    }

    //code starts here
// ============================================================
// SUPPLIER INVOICE MANAGEMENT
// ============================================================

    fun addSupplierInvoice(
        invoice: SupplierInvoice,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                repository.insertSupplierInvoice(invoice)
                onSuccess()
            } catch (e: Exception) {
                e.printStackTrace()
                onError(e.message ?: "Failed to add supplier invoice.")
            }
        }
    }

    fun updateSupplierInvoice(
        invoice: SupplierInvoice,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                repository.updateSupplierInvoice(invoice)
                onSuccess()
            } catch (e: Exception) {
                e.printStackTrace()
                onError(e.message ?: "Failed to update supplier invoice.")
            }
        }
    }

    fun deleteSupplierInvoice(
        invoice: SupplierInvoice,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                repository.deleteSupplierInvoice(invoice)
                onSuccess()
            } catch (e: Exception) {
                e.printStackTrace()
                onError(e.message ?: "Failed to delete supplier invoice.")
            }
        }
    }
    //ends here
    //code starts for updateCustomer()
// ============================================================
// CUSTOMER MANAGEMENT
// ============================================================

    //CODE STARTS HERE

    //ENDS HERE

    // ============================================================
// SUPPLIER PAYMENT MANAGEMENT
// ============================================================

    fun addSupplierPayment(
        payment: SupplierPayment,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {

                val validationError =
                    repository.validateSupplierPayment(payment)

                if (validationError != null) {
                    onError(validationError)
                    return@launch
                }

                repository.insertSupplierPayment(payment)

                onSuccess()

            } catch (e: Exception) {
                e.printStackTrace()
                onError(e.message ?: "Failed to add supplier payment.")
            }
        }
    }

    //code starts here
    fun updateSupplierPayment(
        payment: SupplierPayment,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {

                val validationError =
                    repository.validateSupplierPayment(payment)

                if (validationError != null) {
                    onError(validationError)
                    return@launch
                }

                val existingPayment =
                    repository.getSupplierPaymentById(payment.id)

                repository.updateSupplierPayment(payment)

                repository.recalculateSupplierInvoice(
                    invoiceId = payment.supplierInvoiceId
                )

                if (
                    existingPayment != null &&
                    existingPayment.supplierInvoiceId != payment.supplierInvoiceId
                ) {
                    repository.recalculateSupplierInvoice(
                        invoiceId = existingPayment.supplierInvoiceId
                    )
                }

                onSuccess()

            } catch (e: Exception) {
                e.printStackTrace()
                onError(e.message ?: "Failed to update supplier payment.")
            }
        }
    }
        //code ends here


    fun deleteSupplierPayment(
        payment: SupplierPayment,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {

                // Delete the payment
                repository.deleteSupplierPayment(payment)

                // Recalculate the related supplier invoice
                repository.recalculateSupplierInvoice(
                    invoiceId = payment.supplierInvoiceId
                )

                onSuccess()

            } catch (e: Exception) {
                e.printStackTrace()
                onError(e.message ?: "Failed to delete supplier payment.")
            }
        }
    }

    fun addCustomer(
        customer: Customer,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                repository.insertCustomer(customer)
                onSuccess()
            } catch (e: Exception) {
                e.printStackTrace()
                onError(e.message ?: "Failed to add customer.")
            }
        }
    }

    fun updateCustomer(
        customer: Customer,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                repository.updateCustomer(customer)
                onSuccess()
            } catch (e: Exception) {
                e.printStackTrace()
                onError(e.message ?: "Failed to update customer.")
            }
        }
    }

    fun deleteCustomer(
        customer: Customer,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                repository.deleteCustomer(customer)
                onSuccess()
            } catch (e: Exception) {
                e.printStackTrace()
                onError(e.message ?: "Failed to delete customer.")
            }
        }
    }
    //ends here

    //CUSTOMER INVOICE CODE STARTS HERE
    // ============================================================
// CUSTOMER INVOICE MANAGEMENT
// ============================================================

    fun addCustomerInvoice(
        invoice: CustomerInvoice,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                repository.insertCustomerInvoice(invoice)
                onSuccess()
            } catch (e: Exception) {
                e.printStackTrace()
                onError(e.message ?: "Failed to add customer invoice.")
            }
        }
    }

    fun updateCustomerInvoice(
        invoice: CustomerInvoice,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                repository.updateCustomerInvoice(invoice)
                onSuccess()
            } catch (e: Exception) {
                e.printStackTrace()
                onError(e.message ?: "Failed to update customer invoice.")
            }
        }
    }

    fun deleteCustomerInvoice(
        invoice: CustomerInvoice,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                repository.deleteCustomerInvoice(invoice)
                onSuccess()
            } catch (e: Exception) {
                e.printStackTrace()
                onError(e.message ?: "Failed to delete customer invoice.")
            }
        }
    }
    //CODE ENDS HERE

    //code starts for Customer Lookups here
// ============================================================
// CUSTOMER LOOKUPS
// ============================================================

    suspend fun getCustomerById(id: Int): Customer? =
        repository.getCustomerById(id)

    suspend fun getCustomerByCode(code: String): Customer? =
        repository.getCustomerByCode(code)

    fun getCustomerInvoices(customerId: Int): Flow<List<CustomerInvoice>> =
        repository.getInvoicesForCustomer(customerId)

    fun getCustomerPayments(customerId: Int): Flow<List<CustomerPayment>> =
        repository.getPaymentsForCustomer(customerId)

    fun getInvoicePayments(invoiceId: Int): Flow<List<CustomerPayment>> =
        repository.getPaymentsForInvoice(invoiceId)
    //ends here

    // ============================================================
// CUSTOMER PAYMENT MANAGEMENT
// ============================================================

    fun addCustomerPayment(
        payment: CustomerPayment,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                repository.insertCustomerPayment(payment)
                onSuccess()
            } catch (e: Exception) {
                e.printStackTrace()
                onError(e.message ?: "Failed to add customer payment.")
            }
        }
    }

    fun updateCustomerPayment(
        payment: CustomerPayment,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                repository.updateCustomerPayment(payment)
                onSuccess()
            } catch (e: Exception) {
                e.printStackTrace()
                onError(e.message ?: "Failed to update customer payment.")
            }
        }
    }

    fun deleteCustomerPayment(
        payment: CustomerPayment,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                repository.deleteCustomerPayment(payment)
                onSuccess()
            } catch (e: Exception) {
                e.printStackTrace()
                onError(e.message ?: "Failed to delete customer payment.")
            }
        }
    }


    //code starts here
    fun getGeneralLedger(accountId: Int): Flow<List<LedgerLine>> {
        return repository.getGeneralLedger(accountId)
    }
    //code ends here

    // NEW CODE
    fun createJournalEntry(
        entry: JournalEntry,
        lines: List<JournalLine>,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                repository.createJournalEntry(
                    entry = entry,
                    lines = lines
                )

                onSuccess()

            } catch (e: Exception) {
                e.printStackTrace()
                onError(e.message ?: "Failed to save journal entry.")
            }
        }
    }
    //New code ends here

    //new code for delete button starts here here
    fun deleteJournalEntry(
        entry: JournalEntry,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                repository.deleteJournalEntry(entry)
                onSuccess()
            } catch (e: Exception) {
                e.printStackTrace()
                onError(
                    e.message ?: "Failed to delete journal entry."
                )
            }
        }
    }
    //ends here

    private fun getInitialSettings(): Settings {
        val themeMode = prefs.getString("theme_mode", "system") ?: "system"
        val colorPalette = prefs.getString("color_palette", "Default") ?: "Default"
        return Settings(
            themeMode = themeMode,
            colorPalette = colorPalette
        )
    }

    val settings = repository.appSettings
        .onEach { settingsObj ->
            if (settingsObj != null) {
                prefs.edit()
                    .putString("theme_mode", settingsObj.themeMode)
                    .putString("color_palette", settingsObj.colorPalette)
                    .apply()
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = getInitialSettings()
        )

    // --- SEARCH AND FILTER MUTABLES ---
    val searchQuery = MutableStateFlow("")
    val filterType = MutableStateFlow("All") // All, Income, Expense
    val filterCategoryIds = MutableStateFlow<Set<Int>>(emptySet())
    val filterAccountIds = MutableStateFlow<Set<Int>>(emptySet())
    val filterDateRange = MutableStateFlow<Pair<Long?, Long?>?>(null) // Epoch milliseconds
    val filterTags = MutableStateFlow<Set<String>>(emptySet())

    val selectedHomeDateFilter =
        MutableStateFlow(prefs.getString("home_date_filter_option", "All Time") ?: "All Time")

    fun setHomeDateFilterOption(option: String) {
        selectedHomeDateFilter.value = option
        prefs.edit().putString("home_date_filter_option", option).apply()
    }

    val allTags: StateFlow<Set<String>> = allTransactions.map { txList ->
        txList.flatMap { tx ->
            tx.tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        }.toSet()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptySet()
    )

    fun toggleFilterTag(tag: String) {
        val current = filterTags.value
        filterTags.value = if (current.contains(tag)) current - tag else current + tag
    }

    fun clearFilterTags() {
        filterTags.value = emptySet()
    }

    // REACIVE FILTERED TRANSACTIONS
    val filteredTransactions: StateFlow<List<TransactionWithDetails>> = combine(
        allTransactions,
        searchQuery,
        filterType,
        filterCategoryIds,
        filterAccountIds,
        filterDateRange,
        filterTags
    ) { flows ->
        @Suppress("UNCHECKED_CAST")
        val txs = flows[0] as List<TransactionWithDetails>
        val query = flows[1] as String
        val type = flows[2] as String

        @Suppress("UNCHECKED_CAST")
        val catIds = flows[3] as Set<Int>

        @Suppress("UNCHECKED_CAST")
        val accIds = flows[4] as Set<Int>

        @Suppress("UNCHECKED_CAST")
        val dateRange = flows[5] as Pair<Long?, Long?>?

        @Suppress("UNCHECKED_CAST")
        val selectedTags = flows[6] as Set<String>

        var result = txs

        // 1. Filter by Search Query
        if (query.isNotBlank()) {
            val lowercaseQuery = query.lowercase().trim()
            result = result.filter {
                it.note.lowercase().contains(lowercaseQuery) ||
                        it.categoryName.lowercase().contains(lowercaseQuery) ||
                        it.accountName.lowercase().contains(lowercaseQuery) ||
                        it.tags.lowercase().contains(lowercaseQuery)
            }
        }

        // 2. Filter by Transaction Type
        if (type != "All") {
            val lowercaseType = type.lowercase()
            result = result.filter { it.type == lowercaseType }
        }

        // 3. Filter by Category IDs
        if (catIds.isNotEmpty()) {
            result = result.filter { catIds.contains(it.categoryId) }
        }

        // 4. Filter by Account IDs
        if (accIds.isNotEmpty()) {
            result = result.filter { accIds.contains(it.accountId) }
        }

        // 5. Filter by Date Range
        if (dateRange != null) {
            val startMs = dateRange.first
            val endMs = dateRange.second
            if (startMs != null && endMs != null) {
                result = result.filter {
                    try {
                        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                        val txDate = sdf.parse(it.transactionDate)
                        val txMs = txDate?.time ?: 0L
                        txMs in startMs..endMs
                    } catch (e: Exception) {
                        true
                    }
                }
            }
        }

        // 6. Filter by Custom Tags
        if (selectedTags.isNotEmpty()) {
            result = result.filter { tx ->
                val txTags =
                    tx.tags.split(",").map { it.trim().lowercase() }.filter { it.isNotEmpty() }
                        .toSet()
                selectedTags.any { txTags.contains(it.lowercase()) }
            }
        }

        android.util.Log.d(
            "ANALYTICS_DEBUG",
            "All transactions: ${txs.map { "${it.id}=${it.transactionDate}" }}"
        )

        android.util.Log.d(
            "ANALYTICS_DEBUG",
            "Filtered transactions: ${result.map { "${it.id}=${it.transactionDate}" }}"
        )


        result
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // --- LOCAL NOTIFICATION FEEDBACK ---
    private val _notificationMessage = MutableSharedFlow<String>()
    val notificationMessage = _notificationMessage.asSharedFlow()

    data class SnackbarEvent(
        val message: String,
        val actionLabel: String? = null,
        val onAction: (() -> Unit)? = null
    )

    private val _snackbarEvent = MutableSharedFlow<SnackbarEvent>()
    val snackbarEvent = _snackbarEvent.asSharedFlow()

    fun showSnackbar(message: String) {
        viewModelScope.launch {
            _snackbarEvent.emit(SnackbarEvent(message = message))
        }
    }

    fun showSnackbar(message: String, actionLabel: String, onAction: () -> Unit) {
        viewModelScope.launch {
            _snackbarEvent.emit(
                SnackbarEvent(
                    message = message,
                    actionLabel = actionLabel,
                    onAction = onAction
                )
            )
        }
    }

    fun triggerLocalNotification(message: String) {
        viewModelScope.launch {
            if (settings.value?.notificationsEnabled == true) {
                _notificationMessage.emit(message)
            }
        }
    }

    // --- RECURRING TRANSACTIONS AUTO SCHEDULER ---
    fun processRecurringTransactions() {
        viewModelScope.launch {
            repository.processRecurringTransactions()
        }
    }

    // --- MUTATION METHODS ---

    // Transactions
    fun insertTransaction(
        amount: Double,
        type: String,
        categoryId: Int,
        accountId: Int,
        note: String,
        dateStr: String, // format "yyyy-MM-dd"
        attachmentPath: String? = null,
        tags: String = ""
    ) {
        viewModelScope.launch {
            val nowStr =
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(Date())
            val txDateTime = if (dateStr.contains("T")) dateStr else dateStr + "T09:00:00"
            val tx = Transaction(
                amount = amount,
                type = type,
                categoryId = categoryId,
                accountId = accountId,
                note = note,
                transactionDate = txDateTime,
                createdAt = nowStr,
                updatedAt = nowStr,
                attachmentPath = attachmentPath,
                tags = tags
            )
            repository.insertTransaction(tx)

            // Trigger check for budget limits
            checkBudgetLimits(categoryId, type, amount, dateStr)
        }
    }

    fun updateTransaction(
        id: Int,
        amount: Double,
        type: String,
        categoryId: Int,
        accountId: Int,
        note: String,
        dateStr: String,
        attachmentPath: String? = null,
        tags: String = ""
    ) {
        viewModelScope.launch {
            val nowStr =
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(Date())
            val txDateTime = if (dateStr.contains("T")) dateStr else dateStr + "T09:00:00"
            val tx = Transaction(
                id = id,
                amount = amount,
                type = type,
                categoryId = categoryId,
                accountId = accountId,
                note = note,
                transactionDate = txDateTime,
                createdAt = nowStr, // Keep simple
                updatedAt = nowStr,
                attachmentPath = attachmentPath,
                tags = tags
            )
            repository.updateTransaction(tx)
        }
    }

    fun deleteTransaction(transactionId: Int) {
        viewModelScope.launch {
            repository.deleteTransaction(transactionId)
        }
    }

    fun restoreTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.insertTransaction(transaction)
        }
    }

    // Accounts
    fun insertAccount(
        name: String,
        type: String,
        openingBalance: Double,
        color: String,
        icon: String
    ) {
        viewModelScope.launch {
            val account = Account(
                name = name,
                type = type,
                openingBalance = openingBalance,
                currentBalance = openingBalance,
                color = color,
                icon = icon
            )
            repository.insertAccount(account)
        }
    }

    fun updateAccount(
        id: Int,
        name: String,
        type: String,
        openingBalance: Double,
        currentBalance: Double,
        color: String,
        icon: String
    ) {
        viewModelScope.launch {
            val account = Account(
                id = id,
                name = name,
                type = type,
                openingBalance = openingBalance,
                currentBalance = currentBalance,
                color = color,
                icon = icon
            )
            repository.updateAccount(account)
        }
    }

    fun deleteAccount(account: Account, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        viewModelScope.launch {
            val success = repository.deleteAccount(account)
            if (success) {
                onSuccess()
            } else {
                onFailure("Cannot delete account: transactions reference this account. Please reassign transactions first.")
            }
        }
    }

    // Categories
    fun insertCategory(name: String, type: String, color: String, icon: String) {
        viewModelScope.launch {
            val category = Category(
                name = name,
                type = type,
                color = color,
                icon = icon,
                isDefault = false
            )
            repository.insertCategory(category)
        }
    }

    fun updateCategory(category: Category) {
        viewModelScope.launch {
            repository.updateCategory(category)
        }
    }

    fun updateCategoryOrders(categories: List<Category>) {
        viewModelScope.launch {
            val updatedCategories = categories.mapIndexed { index, category ->
                category.copy(orderIndex = index)
            }
            repository.updateCategories(updatedCategories)
        }
    }

    fun deleteCategory(category: Category, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        viewModelScope.launch {
            val success = repository.deleteCategory(category)
            if (success) {
                onSuccess()
            } else {
                onFailure("Cannot delete category: transactions reference this category. Please reassign transactions first.")
            }
        }
    }

    // Budgets
    fun insertBudget(
        categoryId: Int?,
        amount: Double,
        month: Int,
        year: Int,
        budgetType: String = "MONTHLY",
        startDate: Long? = null,
        endDate: Long? = null,
        budgetName: String? = null,
        id: Int = 0
    ) {
        viewModelScope.launch {
            val budget = Budget(
                id = id,
                categoryId = categoryId,
                budgetAmount = amount,
                month = month,
                year = year,
                budgetType = budgetType,
                startDate = startDate,
                endDate = endDate,
                budgetName = budgetName
            )
            repository.insertBudget(budget)
            triggerLocalNotification("Budget configured successfully!")
        }
    }

    fun deleteBudget(budget: Budget) {
        viewModelScope.launch {
            repository.deleteBudget(budget)
        }
    }

    // Savings Goals
    fun insertSavingsGoal(
        title: String,
        targetAmount: Double,
        currentAmount: Double,
        targetDate: String,
        icon: String,
        color: String
    ) {
        viewModelScope.launch {
            val goal = SavingsGoal(
                title = title,
                targetAmount = targetAmount,
                currentAmount = currentAmount,
                targetDate = targetDate,
                status = if (currentAmount >= targetAmount) "completed" else "active",
                icon = icon,
                color = color
            )
            repository.insertSavingsGoal(goal)

            if (currentAmount >= targetAmount) {
                triggerLocalNotification("Savings Goal '${title}' completed! 🎉")
            }
        }
    }

    fun updateSavingsGoal(
        id: Int,
        title: String,
        targetAmount: Double,
        currentAmount: Double,
        targetDate: String,
        status: String,
        icon: String,
        color: String
    ) {
        viewModelScope.launch {
            val newStatus = if (currentAmount >= targetAmount) "completed" else "active"
            val goal = SavingsGoal(
                id = id,
                title = title,
                targetAmount = targetAmount,
                currentAmount = currentAmount,
                targetDate = targetDate,
                status = newStatus,
                icon = icon,
                color = color
            )
            repository.updateSavingsGoal(goal)
            if (newStatus == "completed" && status != "completed") {
                triggerLocalNotification("Savings Goal '${title}' completed! 🎉")
            }
        }
    }

    fun addSavingsGoalFunds(goal: SavingsGoal, amount: Double) {
        viewModelScope.launch {
            val newAmount = goal.currentAmount + amount
            val newStatus = if (newAmount >= goal.targetAmount) "completed" else "active"
            repository.updateSavingsGoal(
                goal.copy(
                    currentAmount = newAmount,
                    status = newStatus
                )
            )
            if (newStatus == "completed" && goal.status != "completed") {
                triggerLocalNotification("Savings Goal '${goal.title}' completed! 🎉")
            } else {
                triggerLocalNotification("Added funds to '${goal.title}'")
            }
        }
    }

    fun deleteSavingsGoal(goal: SavingsGoal) {
        viewModelScope.launch {
            repository.deleteSavingsGoal(goal)
        }
    }

    // Recurring Rules
    fun insertRecurringRule(
        amount: Double,
        type: String,
        categoryId: Int,
        accountId: Int,
        note: String,
        frequency: String,
        startDate: String
    ) {
        viewModelScope.launch {
            val rule = RecurringTransaction(
                amount = amount,
                type = type,
                categoryId = categoryId,
                accountId = accountId,
                note = note,
                frequency = frequency,
                nextExecutionDate = startDate,
                enabled = true
            )
            repository.insertRecurringTransaction(rule)
        }
    }

    fun updateRecurringRule(rule: RecurringTransaction) {
        viewModelScope.launch {
            repository.updateRecurringTransaction(rule)
        }
    }

    fun deleteRecurringRule(rule: RecurringTransaction) {
        viewModelScope.launch {
            repository.deleteRecurringTransaction(rule)
        }
    }

    // Settings
    fun updateSettings(themeMode: String, currency: String, notificationsEnabled: Boolean) {
        viewModelScope.launch {
            val current = repository.getSettingsDirect() ?: Settings()
            repository.updateSettings(
                current.copy(
                    themeMode = themeMode,
                    currency = currency,
                    notificationsEnabled = notificationsEnabled
                )
            )
        }
    }

    fun updateColorPalette(colorPalette: String) {
        viewModelScope.launch {
            val current = repository.getSettingsDirect() ?: Settings()
            repository.updateSettings(
                current.copy(
                    colorPalette = colorPalette
                )
            )
        }
    }

    fun updatePinSettings(enabled: Boolean, pin: String? = null) {
        viewModelScope.launch {
            val current = repository.getSettingsDirect() ?: Settings()
            if (enabled && !pin.isNullOrEmpty()) {
                setPin(pin)
                repository.updateSettings(current.copy(pinEnabled = true))
            } else {
                disablePin()
                repository.updateSettings(current.copy(pinEnabled = false))
            }
        }
    }

    fun updateBiometricSettings(enabled: Boolean) {
        viewModelScope.launch {
            val current = repository.getSettingsDirect() ?: Settings()
            repository.updateSettings(current.copy(biometricEnabled = enabled))
        }
    }

    // --- BUDGET TRIGGER EVALUATION ---
    private suspend fun checkBudgetLimits(
        categoryId: Int,
        type: String,
        amount: Double,
        dateStr: String
    ) {
        if (type != "expense") return

        try {
            // Get Month and Year from dateStr (format "yyyy-MM-dd")
            val parts = dateStr.split("-")
            val year = parts[0].toInt()
            val month = parts[1].toInt()

            // 1. Check Category Budget
            val catBudget = repository.getBudgetForCategory(categoryId, month, year)
            if (catBudget != null) {
                // SUM all expenses for this category in this month/year
                val txs = allTransactions.value
                val categorySum = txs.filter {
                    it.categoryId == categoryId &&
                            it.type == "expense" &&
                            it.transactionDate.startsWith("$year-${String.format("%02d", month)}")
                }.sumOf { it.amount } + amount

                if (categorySum > catBudget.budgetAmount) {
                    triggerLocalNotification("Alert: Monthly budget for category exceeded! Limit: ${settings.value?.currency}${catBudget.budgetAmount}")
                }
            }

            // 2. Check Overall Budget (categoryId is null)
            val overallBudget = repository.getBudgetForCategory(null, month, year)
            if (overallBudget != null) {
                val txs = allTransactions.value
                val totalExpenses = txs.filter {
                    it.type == "expense" &&
                            it.transactionDate.startsWith("$year-${String.format("%02d", month)}")
                }.sumOf { it.amount } + amount

                if (totalExpenses > overallBudget.budgetAmount) {
                    triggerLocalNotification("Warning: Total monthly budget exceeded! Limit: ${settings.value?.currency}${overallBudget.budgetAmount}")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // --- BACKUP & RESTORE IO ---
    fun exportTransactionsToCsv(outStream: java.io.OutputStream, onComplete: (Boolean) -> Unit) {
        setFileProcessing(true, "Exporting transactions to CSV...")
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            var success = false
            try {
                val txList = allTransactions.value
                val writer = java.io.BufferedWriter(java.io.OutputStreamWriter(outStream))
                writer.write("Transaction ID,Date,Type,Amount,Note,Category,Account\n")
                txList.forEach { tx ->
                    val cleanNote = tx.note.replace("\"", "\"\"")
                    val cleanCategory = tx.categoryName.replace("\"", "\"\"")
                    val cleanAccount = tx.accountName.replace("\"", "\"\"")
                    writer.write("${tx.id},${tx.transactionDate},${tx.type},${tx.amount},\"$cleanNote\",\"$cleanCategory\",\"$cleanAccount\"\n")
                }
                writer.flush()
                writer.close()
                success = true
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                try {
                    outStream.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                setFileProcessing(false)
                onComplete(success)
            }
        }
    }

    fun exportTransactionsToPdf(outStream: java.io.OutputStream, onComplete: (Boolean) -> Unit) {
        setFileProcessing(true, "Exporting statement to PDF...")
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            var success = false
            var logoBitmap: android.graphics.Bitmap? = null
            try {
                val txList = allTransactions.value.sortedBy { it.transactionDate }
                val pdfDocument = android.graphics.pdf.PdfDocument()

                // Define page parameters
                val pageWidth = 595 // A4 width in points (72 points/inch)
                val pageHeight = 842 // A4 height in points

                // Define layout constraints
                val margin = 40f
                val rowHeight = 24f
                val headerHeight = 35f
                val yStart = 140f
                val rowsPerPage = ((pageHeight - yStart - margin) / rowHeight).toInt()

                // Decode logo resource at a very small size to minimize PDF file size
                val context = getApplication<Application>()
                logoBitmap = try {
                    val options = android.graphics.BitmapFactory.Options().apply {
                        inJustDecodeBounds = true
                    }
                    android.graphics.BitmapFactory.decodeResource(
                        context.resources,
                        com.expenso.app.R.drawable.logo,
                        options
                    )

                    val reqWidth = 64
                    val reqHeight = 64
                    var inSampleSize = 1
                    if (options.outHeight > reqHeight || options.outWidth > reqWidth) {
                        val halfHeight = options.outHeight / 2
                        val halfWidth = options.outWidth / 2
                        while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                            inSampleSize *= 2
                        }
                    }

                    val decodeOptions = android.graphics.BitmapFactory.Options().apply {
                        this.inSampleSize = inSampleSize
                        inPreferredConfig = android.graphics.Bitmap.Config.ARGB_8888
                    }
                    val rawBitmap = android.graphics.BitmapFactory.decodeResource(
                        context.resources,
                        com.expenso.app.R.drawable.logo,
                        decodeOptions
                    )
                    if (rawBitmap != null) {
                        val scaled =
                            android.graphics.Bitmap.createScaledBitmap(rawBitmap, 64, 64, true)
                        if (rawBitmap != scaled) {
                            rawBitmap.recycle()
                        }
                        scaled
                    } else {
                        null
                    }
                } catch (e: Exception) {
                    null
                }

                // Split transactions into pages
                val chunks =
                    if (txList.isEmpty()) listOf(emptyList()) else txList.chunked(rowsPerPage)

                //currencySymbol code starts here
                // val currencySymbol = settings?.currency ?: "GH₵"

                //ends here
                chunks.forEachIndexed { pageIndex, pageTxList ->
                    val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(
                        pageWidth,
                        pageHeight,
                        pageIndex + 1
                    ).create()
                    val page = pdfDocument.startPage(pageInfo)
                    val canvas = page.canvas

                    // Set up paints
                    val paint = android.graphics.Paint().apply {
                        isAntiAlias = true
                        isFilterBitmap = true
                    }
                    val textPaint = android.graphics.Paint().apply {
                        color = android.graphics.Color.BLACK
                        textSize = 10f
                        isAntiAlias = true
                        typeface = android.graphics.Typeface.create(
                            "sans-serif",
                            android.graphics.Typeface.NORMAL
                        )
                    }
                    val boldTextPaint = android.graphics.Paint().apply {
                        color = android.graphics.Color.BLACK
                        textSize = 10f
                        isAntiAlias = true
                        typeface = android.graphics.Typeface.create(
                            "sans-serif",
                            android.graphics.Typeface.BOLD
                        )
                    }
                    val titlePaint = android.graphics.Paint().apply {
                        color = android.graphics.Color.parseColor("#121212")
                        textSize = 24f
                        isAntiAlias = true
                        typeface = android.graphics.Typeface.create(
                            "sans-serif",
                            android.graphics.Typeface.BOLD
                        )
                    }
                    val subtitlePaint = android.graphics.Paint().apply {
                        color = android.graphics.Color.GRAY
                        textSize = 10f
                        isAntiAlias = true
                        typeface = android.graphics.Typeface.create(
                            "sans-serif",
                            android.graphics.Typeface.NORMAL
                        )
                    }

                    // Header (Only on page 1)
                    if (pageIndex == 0) {
                        var titleStartX = margin
                        if (logoBitmap != null) {
                            try {
                                val dstRect =
                                    android.graphics.RectF(margin, 28f, margin + 32f, 28f + 32f)
                                canvas.drawBitmap(logoBitmap, null, dstRect, paint)
                                titleStartX = margin + 42f
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                        canvas.drawText("SULVA", titleStartX, 54f, titlePaint)
                        val currentDate = java.text.SimpleDateFormat(
                            "yyyy-MM-dd HH:mm:ss",
                            java.util.Locale.getDefault()
                        ).format(java.util.Date())
                        canvas.drawText("Generated on: $currentDate", margin, 78f, subtitlePaint)
                        canvas.drawText(
                            "Total Transactions: ${txList.size}",
                            margin,
                            92f,
                            subtitlePaint
                        )

                        // Draw decorative bar
                        paint.color = android.graphics.Color.parseColor("#006B5D") // Brand Teal
                        canvas.drawRect(margin, 116f, pageWidth - margin, 120f, paint)
                    } else {
                        var titlePageStartX = margin
                        if (logoBitmap != null) {
                            try {
                                val dstRect =
                                    android.graphics.RectF(margin, 30f, margin + 18f, 30f + 18f)
                                canvas.drawBitmap(logoBitmap, null, dstRect, paint)
                                titlePageStartX = margin + 24f
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                        canvas.drawText(
                            "SULVA - Transactions Statement (Page ${pageIndex + 1})",
                            titlePageStartX,
                            44f,
                            boldTextPaint
                        )
                        paint.color = android.graphics.Color.parseColor("#006B5D") // Brand Teal
                        canvas.drawRect(margin, 52f, pageWidth - margin, 54f, paint)
                    }

                    // Draw Table Header
                    val headerY = if (pageIndex == 0) yStart else 70f
                    paint.color = android.graphics.Color.parseColor("#EEEEEE")
                    canvas.drawRect(
                        margin,
                        headerY,
                        pageWidth - margin,
                        headerY + headerHeight,
                        paint
                    )

                    val colDateX = margin + 5f
                    val colTimeX = margin + 75f
                    val colAccountX = margin + 135f
                    val colCategoryX = margin + 235f
                    val colTypeX = margin + 345f
                    val colAmountX = margin + 425f

                    val headerTextY = headerY + 22f
                    canvas.drawText("Date", colDateX, headerTextY, boldTextPaint)
                    canvas.drawText("Time", colTimeX, headerTextY, boldTextPaint)
                    canvas.drawText("Account", colAccountX, headerTextY, boldTextPaint)
                    canvas.drawText("Category", colCategoryX, headerTextY, boldTextPaint)
                    canvas.drawText("Type", colTypeX, headerTextY, boldTextPaint)
                    canvas.drawText(
                        "Amount (GH₵)",
                        colAmountX,
                        headerTextY,
                        boldTextPaint
                    )
                    //canvas.drawText("Amount ($)", colAmountX, headerTextY, boldTextPaint)

                    // Draw rows
                    var currentY = headerY + headerHeight
                    pageTxList.forEachIndexed { index, tx ->
                        val isEven = index % 2 == 0
                        if (isEven) {
                            paint.color = android.graphics.Color.parseColor("#F9F9F9")
                            canvas.drawRect(
                                margin,
                                currentY,
                                pageWidth - margin,
                                currentY + rowHeight,
                                paint
                            )
                        }

                        val rowTextY = currentY + 16f

                        // Date & Time Split
                        val parts = tx.transactionDate.split("T")
                        val datePart = parts.getOrNull(0) ?: ""
                        val timePart = parts.getOrNull(1)?.take(5) ?: ""

                        // Date
                        canvas.drawText(datePart, colDateX, rowTextY, textPaint)

                        // Time
                        canvas.drawText(timePart, colTimeX, rowTextY, textPaint)

                        // Account
                        val accText =
                            if (tx.accountName.length > 12) tx.accountName.take(10) + ".." else tx.accountName
                        canvas.drawText(accText, colAccountX, rowTextY, textPaint)

                        // Category
                        val catText =
                            if (tx.categoryName.length > 15) tx.categoryName.take(13) + ".." else tx.categoryName
                        canvas.drawText(catText, colCategoryX, rowTextY, textPaint)

                        // Type
                        val typeUpper = tx.type.uppercase()
                        val typePaint = android.graphics.Paint(textPaint).apply {
                            color = if (tx.type.lowercase() == "income") {
                                android.graphics.Color.parseColor("#00796B") // Teal/Green
                            } else {
                                android.graphics.Color.parseColor("#D32F2F") // Red
                            }
                            typeface = android.graphics.Typeface.create(
                                "sans-serif",
                                android.graphics.Typeface.BOLD
                            )
                        }
                        canvas.drawText(typeUpper, colTypeX, rowTextY, typePaint)

                        // Amount
                        val formattedAmount = String.format("GH₵%.2f", tx.amount)
                        canvas.drawText(formattedAmount, colAmountX, rowTextY, textPaint)

                        currentY += rowHeight
                    }

                    // Draw Page number at bottom right
                    val footerY = pageHeight - 20f
                    canvas.drawText(
                        "Page ${pageIndex + 1} of ${chunks.size}",
                        pageWidth - margin - 60f,
                        footerY,
                        subtitlePaint
                    )

                    pdfDocument.finishPage(page)
                }

                pdfDocument.writeTo(outStream)
                pdfDocument.close()
                success = true
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                try {
                    logoBitmap?.recycle()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                try {
                    outStream.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                setFileProcessing(false)
                onComplete(success)
            }
        }
    }
    //ends here

    //ARAging page starts here

    private data class APAgingPdfRow(
        val supplierName: String,
        val supplierCode: String,
        val invoiceNumber: String,
        val invoiceDate: String,
        val dueDate: String,
        val balance: Double,
        val daysOverdue: Long
    )


// AR AGING PDF EXPORT
fun exportARAgingReportToPdf(
    outStream: java.io.OutputStream,
    asOfDate: String,
    onComplete: (Boolean) -> Unit
) {
    setFileProcessing(true, "Exporting AR Aging Report to PDF...")

    viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {

        var success = false
        var logoBitmap: android.graphics.Bitmap? = null

        try {
            // ============================================================
            // LOAD DATA
            // ============================================================

            val invoices = allCustomerInvoices.value
            val customers = allCustomers.value
            val payments = allCustomerPayments.value

            // ============================================================
            // BUILD HISTORICAL AGING DATA
            // ============================================================

            val agingRows = invoices.mapNotNull { invoice ->

                if (invoice.invoiceDate > asOfDate) {
                    return@mapNotNull null
                }

                val customer = customers.firstOrNull {
                    it.id == invoice.customerId
                } ?: return@mapNotNull null

                val historicalPaid: Double = payments
                    .filter { payment ->
                        payment.invoiceId == invoice.id &&
                                payment.paymentDate <= asOfDate
                    }
                    .sumOf { payment: CustomerPayment ->
                        payment.amount
                    }

                val balance: Double =
                    (invoice.amount - historicalPaid)
                        .coerceAtLeast(0.0)

                if (balance <= 0.009) {
                    return@mapNotNull null
                }
            //code ends here

                //ends here


                val daysOverdue =
                    calculateARAgingDaysOverdue(
                        dueDate = invoice.dueDate,
                        asOfDate = asOfDate
                    )

                ARAgingPdfRow(
                    customerName = customer.name,
                    customerCode = customer.customerCode,
                    invoiceNumber = invoice.invoiceNumber,
                    invoiceDate = invoice.invoiceDate,
                    dueDate = invoice.dueDate,
                    balance = balance,
                    daysOverdue = daysOverdue
                )
            }

            // ============================================================
            // TOTALS
            // ============================================================

            val totalOutstanding: Double =
                agingRows.sumOf { row: ARAgingPdfRow ->
                    row.balance
                }

            val currentTotal: Double =
                agingRows
                    .filter { row: ARAgingPdfRow ->
                        row.daysOverdue <= 0
                    }
                    .sumOf { row: ARAgingPdfRow ->
                        row.balance
                    }

            val days1To30Total: Double =
                agingRows
                    .filter { row: ARAgingPdfRow ->
                        row.daysOverdue in 1..30
                    }
                    .sumOf { row: ARAgingPdfRow ->
                        row.balance
                    }

            val days31To60Total: Double =
                agingRows
                    .filter { row: ARAgingPdfRow ->
                        row.daysOverdue in 31..60
                    }
                    .sumOf { row: ARAgingPdfRow ->
                        row.balance
                    }

            val days61To90Total: Double =
                agingRows
                    .filter { row: ARAgingPdfRow ->
                        row.daysOverdue in 61..90
                    }
                    .sumOf { row: ARAgingPdfRow ->
                        row.balance
                    }

            val over90Total: Double =
                agingRows
                    .filter { row: ARAgingPdfRow ->
                        row.daysOverdue > 90
                    }
                    .sumOf { row: ARAgingPdfRow ->
                        row.balance
                    }

            val overdueTotal: Double =
                days1To30Total +
                        days31To60Total +
                        days61To90Total +
                        over90Total

            val overduePercentage: Double =
                if (totalOutstanding > 0.0) {
                    (overdueTotal / totalOutstanding) * 100.0
                } else {
                    0.0
                }

            // ============================================================
            // PDF SETUP
            // ============================================================

            val pdfDocument =
                android.graphics.pdf.PdfDocument()

            val pageWidth = 595
            val pageHeight = 842

            val margin = 40f
            val rowHeight = 24f
            val headerHeight = 32f
            val tableStartY = 285f

            // ============================================================
            // LOGO
            // ============================================================

            val context =
                getApplication<android.app.Application>()

            logoBitmap = try {

                val options =
                    android.graphics.BitmapFactory.Options().apply {
                        inJustDecodeBounds = true
                    }

                android.graphics.BitmapFactory.decodeResource(
                    context.resources,
                    com.expenso.app.R.drawable.logo,
                    options
                )

                val decodeOptions =
                    android.graphics.BitmapFactory.Options().apply {
                        inSampleSize = 2
                        inPreferredConfig =
                            android.graphics.Bitmap.Config.ARGB_8888
                    }

                val rawBitmap =
                    android.graphics.BitmapFactory.decodeResource(
                        context.resources,
                        com.expenso.app.R.drawable.logo,
                        decodeOptions
                    )

                if (rawBitmap != null) {
                    android.graphics.Bitmap.createScaledBitmap(
                        rawBitmap,
                        56,
                        56,
                        true
                    )
                } else {
                    null
                }

            } catch (e: Exception) {
                null
            }

            // ============================================================
            // PAGE CALCULATION
            // ============================================================

            val rowsPerPage =
                ((pageHeight - 70f - margin) / rowHeight)
                    .toInt()
                    .coerceAtLeast(1)

            val chunks =
                if (agingRows.isEmpty()) {
                    listOf(emptyList<ARAgingPdfRow>())
                } else {
                    agingRows.chunked(rowsPerPage)
                }

            // ============================================================
            // DRAW PAGES
            // ============================================================

            chunks.forEachIndexed { pageIndex, pageRows ->

                val pageInfo =
                    android.graphics.pdf.PdfDocument.PageInfo
                        .Builder(
                            pageWidth,
                            pageHeight,
                            pageIndex + 1
                        )
                        .create()

                val page =
                    pdfDocument.startPage(pageInfo)

                val canvas = page.canvas

                val paint =
                    android.graphics.Paint().apply {
                        isAntiAlias = true
                        isFilterBitmap = true
                    }

                val textPaint =
                    android.graphics.Paint().apply {
                        color =
                            android.graphics.Color.BLACK
                        textSize = 9f
                        isAntiAlias = true
                        typeface =
                            android.graphics.Typeface.create(
                                "sans-serif",
                                android.graphics.Typeface.NORMAL
                            )
                    }

                val boldPaint =
                    android.graphics.Paint().apply {
                        color =
                            android.graphics.Color.BLACK
                        textSize = 9f
                        isAntiAlias = true
                        typeface =
                            android.graphics.Typeface.create(
                                "sans-serif",
                                android.graphics.Typeface.BOLD
                            )
                    }

                val titlePaint =
                    android.graphics.Paint().apply {
                        color =
                            android.graphics.Color.parseColor(
                                "#121212"
                            )
                        textSize = 22f
                        isAntiAlias = true
                        typeface =
                            android.graphics.Typeface.create(
                                "sans-serif",
                                android.graphics.Typeface.BOLD
                            )
                    }

                val subtitlePaint =
                    android.graphics.Paint().apply {
                        color =
                            android.graphics.Color.GRAY
                        textSize = 9f
                        isAntiAlias = true
                    }

                // ========================================================
                // HEADER
                // ========================================================

                if (pageIndex == 0) {

                    if (logoBitmap != null) {
                        val logoRect =
                            android.graphics.RectF(
                                margin,
                                25f,
                                margin + 42f,
                                67f
                            )

                        canvas.drawBitmap(
                            logoBitmap,
                            null,
                            logoRect,
                            paint
                        )
                    }

                    canvas.drawText(
                        "SULVA",
                        margin + 52f,
                        52f,
                        titlePaint
                    )

                    canvas.drawText(
                        "ACCOUNTS RECEIVABLE",
                        margin,
                        88f,
                        boldPaint
                    )

                    canvas.drawText(
                        "AR Aging Report",
                        margin,
                        104f,
                        subtitlePaint
                    )

                    canvas.drawText(
                        "As of: $asOfDate",
                        margin,
                        119f,
                        subtitlePaint
                    )

                    canvas.drawText(
                        "Generated: ${
                            java.text.SimpleDateFormat(
                                "yyyy-MM-dd HH:mm",
                                java.util.Locale.getDefault()
                            ).format(java.util.Date())
                        }",
                        margin,
                        134f,
                        subtitlePaint
                    )

                    paint.color =
                        android.graphics.Color.parseColor(
                            "#006B5D"
                        )

                    canvas.drawRect(
                        margin,
                        148f,
                        pageWidth - margin,
                        152f,
                        paint
                    )

                    // ====================================================
                    // SUMMARY
                    // ====================================================

                    canvas.drawText(
                        "Total Outstanding",
                        margin,
                        180f,
                        boldPaint
                    )

                    canvas.drawText(
                        formatARAgingMoney(totalOutstanding),
                        margin,
                        198f,
                        textPaint
                    )

                    canvas.drawText(
                        "Current",
                        180f,
                        180f,
                        boldPaint
                    )

                    canvas.drawText(
                        formatARAgingMoney(currentTotal),
                        180f,
                        198f,
                        textPaint
                    )

                    canvas.drawText(
                        "Overdue",
                        310f,
                        180f,
                        boldPaint
                    )

                    canvas.drawText(
                        formatARAgingMoney(overdueTotal),
                        310f,
                        198f,
                        textPaint
                    )

                    canvas.drawText(
                        "Overdue %",
                        440f,
                        180f,
                        boldPaint
                    )

                    canvas.drawText(
                        String.format(
                            java.util.Locale.getDefault(),
                            "%.1f%%",
                            overduePercentage
                        ),
                        440f,
                        198f,
                        textPaint
                    )

                    // ====================================================
                    // AGING BUCKET SUMMARY
                    // ====================================================

                    canvas.drawText(
                        "AGING BUCKETS",
                        margin,
                        225f,
                        boldPaint
                    )

                    canvas.drawText(
                        "Current: ${formatARAgingMoney(currentTotal)}",
                        margin,
                        242f,
                        textPaint
                    )

                    canvas.drawText(
                        "1-30: ${formatARAgingMoney(days1To30Total)}",
                        180f,
                        242f,
                        textPaint
                    )

                    canvas.drawText(
                        "31-60: ${formatARAgingMoney(days31To60Total)}",
                        300f,
                        242f,
                        textPaint
                    )

                    canvas.drawText(
                        "61-90: ${formatARAgingMoney(days61To90Total)}",
                        420f,
                        242f,
                        textPaint
                    )

                    canvas.drawText(
                        "90+: ${formatARAgingMoney(over90Total)}",
                        margin,
                        260f,
                        textPaint
                    )
                } else {

                    if (logoBitmap != null) {

                        val logoRect =
                            android.graphics.RectF(
                                margin,
                                25f,
                                margin + 28f,
                                53f
                            )

                        canvas.drawBitmap(
                            logoBitmap,
                            null,
                            logoRect,
                            paint
                        )
                    }

                    canvas.drawText(
                        "SULVA - AR Aging Report",
                        margin + 36f,
                        44f,
                        boldPaint
                    )

                    canvas.drawText(
                        "As of: $asOfDate",
                        margin + 36f,
                        58f,
                        subtitlePaint
                    )
                }

                // ========================================================
                // TABLE
                // ========================================================

                val headerY =
                    if (pageIndex == 0) {
                        tableStartY
                    } else {
                        85f
                    }

                paint.color =
                    android.graphics.Color.parseColor(
                        "#EEEEEE"
                    )

                canvas.drawRect(
                    margin,
                    headerY,
                    pageWidth - margin,
                    headerY + headerHeight,
                    paint
                )

                val colCustomer = margin + 5f
                val colInvoice = margin + 135f
                val colDate = margin + 220f
                val colDue = margin + 285f
                val colBalance = margin + 355f
                val colDays = margin + 445f

                val headerTextY =
                    headerY + 21f

                canvas.drawText(
                    "Customer",
                    colCustomer,
                    headerTextY,
                    boldPaint
                )

                canvas.drawText(
                    "Invoice",
                    colInvoice,
                    headerTextY,
                    boldPaint
                )

                canvas.drawText(
                    "Date",
                    colDate,
                    headerTextY,
                    boldPaint
                )

                canvas.drawText(
                    "Due Date",
                    colDue,
                    headerTextY,
                    boldPaint
                )

                canvas.drawText(
                    "Balance",
                    colBalance,
                    headerTextY,
                    boldPaint
                )

                canvas.drawText(
                    "Days",
                    colDays,
                    headerTextY,
                    boldPaint
                )

                // ========================================================
                // TABLE ROWS
                // ========================================================

                var currentY =
                    headerY + headerHeight

                pageRows.forEachIndexed { index, row ->

                    if (index % 2 == 0) {

                        paint.color =
                            android.graphics.Color.parseColor(
                                "#F9F9F9"
                            )

                        canvas.drawRect(
                            margin,
                            currentY,
                            pageWidth - margin,
                            currentY + rowHeight,
                            paint
                        )
                    }

                    val rowTextY =
                        currentY + 16f

                    val customerText =
                        if (row.customerName.length > 18) {
                            row.customerName.take(16) + ".."
                        } else {
                            row.customerName
                        }

                    val invoiceText =
                        if (row.invoiceNumber.length > 12) {
                            row.invoiceNumber.take(10) + ".."
                        } else {
                            row.invoiceNumber
                        }

                    canvas.drawText(
                        customerText,
                        colCustomer,
                        rowTextY,
                        textPaint
                    )

                    canvas.drawText(
                        invoiceText,
                        colInvoice,
                        rowTextY,
                        textPaint
                    )

                    canvas.drawText(
                        row.invoiceDate,
                        colDate,
                        rowTextY,
                        textPaint
                    )

                    canvas.drawText(
                        row.dueDate,
                        colDue,
                        rowTextY,
                        textPaint
                    )

                    canvas.drawText(
                        formatARAgingMoney(row.balance),
                        colBalance,
                        rowTextY,
                        textPaint
                    )

                    canvas.drawText(
                        row.daysOverdue.toString(),
                        colDays,
                        rowTextY,
                        textPaint
                    )

                    currentY += rowHeight
                }

                // ========================================================
                // FOOTER
                // ========================================================

                canvas.drawText(
                    "Page ${pageIndex + 1} of ${chunks.size}",
                    pageWidth - margin - 65f,
                    pageHeight - 20f,
                    subtitlePaint
                )

                pdfDocument.finishPage(page)
            }

            // ============================================================
            // WRITE PDF
            // ============================================================

            pdfDocument.writeTo(outStream)
            pdfDocument.close()

            success = true

        } catch (e: Exception) {

            e.printStackTrace()

        } finally {

            try {
                logoBitmap?.recycle()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            try {
                outStream.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            setFileProcessing(false)

            onComplete(success)
        }
    }
}

// ============================================================
// AR AGING PDF SUPPORTING CODE ENDS HERE
// ============================================================

private data class ARAgingPdfRow(
    val customerName: String,
    val customerCode: String,
    val invoiceNumber: String,
    val invoiceDate: String,
    val dueDate: String,
    val balance: Double,
    val daysOverdue: Long
)

//CODE STARTS HERE
fun exportAPAgingReportToPdf(
    outStream: java.io.OutputStream,
    asOfDate: String,
    onComplete: (Boolean) -> Unit
) {
    setFileProcessing(
        true,
        "Exporting AP Aging Report to PDF..."
    )

    viewModelScope.launch(
        kotlinx.coroutines.Dispatchers.IO
    ) {

        var success = false

        try {

            // ============================================================
            // LOAD DATA
            // ============================================================

            val invoices =
                allSupplierInvoices.value

            val suppliers =
                allSuppliers.value

            val payments =
                allSupplierPayments.value

            // ============================================================
            // BUILD HISTORICAL AGING DATA
            // ============================================================

            val agingRows =
                invoices.mapNotNull { invoice ->

                    // Ignore invoices created after the report date
                    if (invoice.invoiceDate > asOfDate) {
                        return@mapNotNull null
                    }

                    val supplier =
                        suppliers.firstOrNull {
                            it.id == invoice.supplierId
                        } ?: return@mapNotNull null

                    // Payments made on or before the report date
                    val historicalPaid =
                        payments
                            .filter { payment ->
                                payment.supplierInvoiceId == invoice.id &&
                                        payment.paymentDate <= asOfDate
                            }
                            .sumOf { payment ->
                                payment.amount
                            }

                    // Outstanding balance as of report date
                    val balance =
                        (
                                invoice.totalAmount -
                                        historicalPaid
                                ).coerceAtLeast(0.0)

                    // Ignore fully paid invoices
                    if (balance <= 0.009) {
                        return@mapNotNull null
                    }

                    val daysOverdue =
                        calculateAPAgingDaysOverdue(
                            dueDate = invoice.dueDate,
                            asOfDate = asOfDate
                        )

                    APAgingPdfRow(
                        supplierName = supplier.name,
                        supplierCode = supplier.supplierCode,
                        invoiceNumber = invoice.invoiceNumber,
                        invoiceDate = invoice.invoiceDate,
                        dueDate = invoice.dueDate,
                        balance = balance,
                        daysOverdue = daysOverdue
                    )
                }

            // ============================================================
            // TOTALS
            // ============================================================

            val totalOutstanding =
                agingRows.sumOf { row ->
                    row.balance
                }

            val currentTotal =
                agingRows
                    .filter { row ->
                        row.daysOverdue <= 0
                    }
                    .sumOf { row ->
                        row.balance
                    }

            val days1To30Total =
                agingRows
                    .filter { row ->
                        row.daysOverdue in 1..30
                    }
                    .sumOf { row ->
                        row.balance
                    }

            val days31To60Total =
                agingRows
                    .filter { row ->
                        row.daysOverdue in 31..60
                    }
                    .sumOf { row ->
                        row.balance
                    }

            val days61To90Total =
                agingRows
                    .filter { row ->
                        row.daysOverdue in 61..90
                    }
                    .sumOf { row ->
                        row.balance
                    }

            val over90Total =
                agingRows
                    .filter { row ->
                        row.daysOverdue > 90
                    }
                    .sumOf { row ->
                        row.balance
                    }

            val overdueTotal =
                days1To30Total +
                        days31To60Total +
                        days61To90Total +
                        over90Total

            val overduePercentage =
                if (totalOutstanding > 0.0) {
                    (
                            overdueTotal /
                                    totalOutstanding
                            ) * 100.0
                } else {
                    0.0
                }

            // ============================================================
            // PDF SETUP
            // ============================================================

            val pdfDocument =
                android.graphics.pdf.PdfDocument()

            val pageWidth = 595
            val pageHeight = 842

            val margin = 40f
            val rowHeight = 24f
            val headerHeight = 32f
            val tableStartY = 285f

            // ============================================================
            // LOGO
            // ============================================================

            val context =
                getApplication<android.app.Application>()

            val logoBitmap =
                try {

                    val decodeOptions =
                        android.graphics.BitmapFactory
                            .Options()
                            .apply {
                                inSampleSize = 2
                                inPreferredConfig =
                                    android.graphics.Bitmap.Config.ARGB_8888
                            }

                    val rawBitmap =
                        android.graphics.BitmapFactory
                            .decodeResource(
                                context.resources,
                                com.expenso.app.R.drawable.logo,
                                decodeOptions
                            )

                    if (rawBitmap != null) {

                        android.graphics.Bitmap
                            .createScaledBitmap(
                                rawBitmap,
                                56,
                                56,
                                true
                            )

                    } else {
                        null
                    }

                } catch (e: Exception) {
                    null
                }

            // ============================================================
            // PAGE CALCULATION
            // ============================================================

            val rowsPerPage =
                (
                        (
                                pageHeight -
                                        70f -
                                        margin
                                ) / rowHeight
                        )
                    .toInt()
                    .coerceAtLeast(1)

            val chunks =
                if (agingRows.isEmpty()) {
                    listOf(
                        emptyList<APAgingPdfRow>()
                    )
                } else {
                    agingRows.chunked(rowsPerPage)
                }

            // ============================================================
            // DRAW PAGES
            // ============================================================

            chunks.forEachIndexed { pageIndex, pageRows ->

                val pageInfo =
                    android.graphics.pdf.PdfDocument
                        .PageInfo
                        .Builder(
                            pageWidth,
                            pageHeight,
                            pageIndex + 1
                        )
                        .create()

                val page =
                    pdfDocument.startPage(pageInfo)

                val canvas =
                    page.canvas

                val paint =
                    android.graphics.Paint().apply {
                        isAntiAlias = true
                        isFilterBitmap = true
                    }

                val textPaint =
                    android.graphics.Paint().apply {
                        color =
                            android.graphics.Color.BLACK
                        textSize = 9f
                        isAntiAlias = true
                        typeface =
                            android.graphics.Typeface.create(
                                "sans-serif",
                                android.graphics.Typeface.NORMAL
                            )
                    }

                val boldPaint =
                    android.graphics.Paint().apply {
                        color =
                            android.graphics.Color.BLACK
                        textSize = 9f
                        isAntiAlias = true
                        typeface =
                            android.graphics.Typeface.create(
                                "sans-serif",
                                android.graphics.Typeface.BOLD
                            )
                    }

                val titlePaint =
                    android.graphics.Paint().apply {
                        color =
                            android.graphics.Color.parseColor(
                                "#121212"
                            )
                        textSize = 22f
                        isAntiAlias = true
                        typeface =
                            android.graphics.Typeface.create(
                                "sans-serif",
                                android.graphics.Typeface.BOLD
                            )
                    }

                val subtitlePaint =
                    android.graphics.Paint().apply {
                        color =
                            android.graphics.Color.GRAY
                        textSize = 9f
                        isAntiAlias = true
                    }

                // ========================================================
                // HEADER
                // ========================================================

                if (pageIndex == 0) {

                    if (logoBitmap != null) {

                        val logoRect =
                            android.graphics.RectF(
                                margin,
                                25f,
                                margin + 42f,
                                67f
                            )

                        canvas.drawBitmap(
                            logoBitmap,
                            null,
                            logoRect,
                            paint
                        )
                    }

                    canvas.drawText(
                        "SULVA",
                        margin + 52f,
                        52f,
                        titlePaint
                    )

                    canvas.drawText(
                        "ACCOUNTS PAYABLE",
                        margin,
                        88f,
                        boldPaint
                    )

                    canvas.drawText(
                        "AP Aging Report",
                        margin,
                        104f,
                        subtitlePaint
                    )

                    canvas.drawText(
                        "As of: $asOfDate",
                        margin,
                        119f,
                        subtitlePaint
                    )

                    canvas.drawText(
                        "Generated: ${
                            java.text.SimpleDateFormat(
                                "yyyy-MM-dd HH:mm",
                                java.util.Locale.getDefault()
                            ).format(
                                java.util.Date()
                            )
                        }",
                        margin,
                        134f,
                        subtitlePaint
                    )

                    paint.color =
                        android.graphics.Color.parseColor(
                            "#006B5D"
                        )

                    canvas.drawRect(
                        margin,
                        148f,
                        pageWidth - margin,
                        152f,
                        paint
                    )

                    // ====================================================
                    // SUMMARY
                    // ====================================================

                    canvas.drawText(
                        "Total Outstanding",
                        margin,
                        180f,
                        boldPaint
                    )

                    canvas.drawText(
                        formatAPAgingMoney(
                            totalOutstanding
                        ),
                        margin,
                        198f,
                        textPaint
                    )

                    canvas.drawText(
                        "Current",
                        180f,
                        180f,
                        boldPaint
                    )

                    canvas.drawText(
                        formatAPAgingMoney(
                            currentTotal
                        ),
                        180f,
                        198f,
                        textPaint
                    )

                    canvas.drawText(
                        "Overdue",
                        310f,
                        180f,
                        boldPaint
                    )

                    canvas.drawText(
                        formatAPAgingMoney(
                            overdueTotal
                        ),
                        310f,
                        198f,
                        textPaint
                    )

                    canvas.drawText(
                        "Overdue %",
                        440f,
                        180f,
                        boldPaint
                    )

                    canvas.drawText(
                        String.format(
                            java.util.Locale.getDefault(),
                            "%.1f%%",
                            overduePercentage
                        ),
                        440f,
                        198f,
                        textPaint
                    )

                    // ====================================================
                    // AGING BUCKET SUMMARY
                    // ====================================================

                    canvas.drawText(
                        "AGING BUCKETS",
                        margin,
                        225f,
                        boldPaint
                    )

                    canvas.drawText(
                        "Current: ${
                            formatAPAgingMoney(
                                currentTotal
                            )
                        }",
                        margin,
                        242f,
                        textPaint
                    )

                    canvas.drawText(
                        "1-30: ${
                            formatAPAgingMoney(
                                days1To30Total
                            )
                        }",
                        180f,
                        242f,
                        textPaint
                    )

                    canvas.drawText(
                        "31-60: ${
                            formatAPAgingMoney(
                                days31To60Total
                            )
                        }",
                        300f,
                        242f,
                        textPaint
                    )

                    canvas.drawText(
                        "61-90: ${
                            formatAPAgingMoney(
                                days61To90Total
                            )
                        }",
                        420f,
                        242f,
                        textPaint
                    )

                    canvas.drawText(
                        "90+: ${
                            formatAPAgingMoney(
                                over90Total
                            )
                        }",
                        margin,
                        260f,
                        textPaint
                    )

                } else {

                    if (logoBitmap != null) {

                        val logoRect =
                            android.graphics.RectF(
                                margin,
                                25f,
                                margin + 28f,
                                53f
                            )

                        canvas.drawBitmap(
                            logoBitmap,
                            null,
                            logoRect,
                            paint
                        )
                    }

                    canvas.drawText(
                        "SULVA - AP Aging Report",
                        margin + 36f,
                        44f,
                        boldPaint
                    )

                    canvas.drawText(
                        "As of: $asOfDate",
                        margin + 36f,
                        58f,
                        subtitlePaint
                    )
                }

                // ========================================================
                // TABLE
                // ========================================================

                val headerY =
                    if (pageIndex == 0) {
                        tableStartY
                    } else {
                        85f
                    }

                paint.color =
                    android.graphics.Color.parseColor(
                        "#EEEEEE"
                    )

                canvas.drawRect(
                    margin,
                    headerY,
                    pageWidth - margin,
                    headerY + headerHeight,
                    paint
                )

                val colSupplier =
                    margin + 5f

                val colInvoice =
                    margin + 135f

                val colDate =
                    margin + 220f

                val colDue =
                    margin + 285f

                val colBalance =
                    margin + 355f

                val colDays =
                    margin + 445f

                val colStatus =
                    margin + 480f

                val headerTextY =
                    headerY + 21f

                canvas.drawText(
                    "Supplier",
                    colSupplier,
                    headerTextY,
                    boldPaint
                )

                canvas.drawText(
                    "Invoice",
                    colInvoice,
                    headerTextY,
                    boldPaint
                )

                canvas.
                drawText(
                    "Due Date",
                    colDue,
                    headerTextY,
                    boldPaint
                )

                canvas.drawText(
                    "Balance",
                    colBalance,
                    headerTextY,
                    boldPaint
                )

                canvas.drawText(
                    "Days",
                    colDays,
                    headerTextY,
                    boldPaint
                )

                canvas.drawText(
                    "Status",
                    colStatus,
                    headerTextY,
                    boldPaint
                )

// ========================================================
// TABLE ROWS
// ========================================================

                var currentY =
                    headerY + headerHeight

                pageRows.forEachIndexed { index, row ->

                    if (index % 2 == 0) {

                        paint.color =
                            android.graphics.Color.parseColor(
                                "#F9F9F9"
                            )

                        canvas.drawRect(
                            margin,
                            currentY,
                            pageWidth - margin,
                            currentY + rowHeight,
                            paint
                        )
                    }

                    val supplierText =
                        if (row.supplierCode.isNotBlank()) {
                            "${row.supplierCode} ${row.supplierName}"
                        } else {
                            row.supplierName
                        }

                    canvas.drawText(
                        supplierText.take(20),
                        colSupplier,
                        currentY + 16f,
                        textPaint
                    )

                    canvas.drawText(
                        row.invoiceNumber.take(12),
                        colInvoice,
                        currentY + 16f,
                        textPaint
                    )

                    canvas.drawText(
                        row.invoiceDate,
                        colDate,
                        currentY + 16f,
                        textPaint
                    )

                    canvas.drawText(
                        row.dueDate,
                        colDue,
                        currentY + 16f,
                        textPaint
                    )

                    canvas.drawText(
                        formatAPAgingMoney(row.balance),
                        colBalance,
                        currentY + 16f,
                        textPaint
                    )

                    canvas.drawText(
                        row.daysOverdue.toString(),
                        colDays,
                        currentY + 16f,
                        textPaint
                    )

                    val status =
                        when {
                            row.daysOverdue <= 0 -> "CURRENT"
                            row.daysOverdue <= 30 -> "1-30"
                            row.daysOverdue <= 60 -> "31-60"
                            row.daysOverdue <= 90 -> "61-90"
                            else -> "90+"
                        }

                    canvas.drawText(
                        status,
                        colStatus,
                        currentY + 16f,
                        textPaint
                    )

                    currentY += rowHeight
                }

// ========================================================
// FOOTER
// ========================================================

                val footerPaint =
                    android.graphics.Paint().apply {
                        color =
                            android.graphics.Color.GRAY
                        textSize = 8f
                        isAntiAlias = true
                    }

                canvas.drawText(
                    "Generated by SULVA Accounting System",
                    margin,
                    pageHeight - 20f,
                    footerPaint
                )

                canvas.drawText(
                    "Page ${pageIndex + 1} of ${chunks.size}",
                    pageWidth - 110f,
                    pageHeight - 20f,
                    footerPaint
                )

                pdfDocument.finishPage(page)
            }

// ============================================================
// WRITE PDF
// ============================================================

            pdfDocument.writeTo(outStream)

            outStream.flush()

            pdfDocument.close()

            success = true

        } catch (e: Exception) {

            e.printStackTrace()

            success = false

        } finally {

            try {
                outStream.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            setFileProcessing(false)

            onComplete(success)
        }
    }
}
//ENDS HERE

    //FINANCIAL REPORT SCREEN START HERE
    fun exportFinancialStatementsToPdf(
        outStream: java.io.OutputStream,
        onComplete: (Boolean) -> Unit
    ) {
        setFileProcessing(
            true,
            "Exporting Financial Statements to PDF..."
        )

        viewModelScope.launch(
            kotlinx.coroutines.Dispatchers.IO
        ) {

            var success = false

            try {

                // ============================================================
                // LOAD DATA
                // ============================================================

                val accounts =
                    allChartOfAccounts.value

                val journalLines =
                    allJournalLines.value

                // ============================================================
                // ACCOUNT BALANCE
                // ============================================================

                fun accountBalance(
                    account: com.expenso.app.data.ChartOfAccount
                ): Double {

                    val accountLines =
                        journalLines.filter {
                            it.accountId == account.id
                        }

                    val totalDebit =
                        accountLines.sumOf {
                            it.debit
                        }

                    val totalCredit =
                        accountLines.sumOf {
                            it.credit
                        }

                    return when (
                        account.accountType.uppercase()
                    ) {

                        "ASSET",
                        "EXPENSE" ->
                            totalDebit - totalCredit

                        "LIABILITY",
                        "EQUITY",
                        "REVENUE" ->
                            totalCredit - totalDebit

                        else ->
                            totalDebit - totalCredit
                    }
                }

                // ============================================================
                // FINANCIAL ACCOUNTS
                // ============================================================

                val revenueAccounts =
                    accounts
                        .filter {
                            it.accountType.equals(
                                "REVENUE",
                                ignoreCase = true
                            )
                        }
                        .map {
                            it to accountBalance(it)
                        }
                        .filter {
                            it.second != 0.0
                        }
                        .sortedBy {
                            it.first.accountCode
                        }

                val expenseAccounts =
                    accounts
                        .filter {
                            it.accountType.equals(
                                "EXPENSE",
                                ignoreCase = true
                            )
                        }
                        .map {
                            it to accountBalance(it)
                        }
                        .filter {
                            it.second != 0.0
                        }
                        .sortedBy {
                            it.first.accountCode
                        }

                val assetAccounts =
                    accounts
                        .filter {
                            it.accountType.equals(
                                "ASSET",
                                ignoreCase = true
                            )
                        }
                        .map {
                            it to accountBalance(it)
                        }
                        .filter {
                            it.second != 0.0
                        }
                        .sortedBy {
                            it.first.accountCode
                        }

                val liabilityAccounts =
                    accounts
                        .filter {
                            it.accountType.equals(
                                "LIABILITY",
                                ignoreCase = true
                            )
                        }
                        .map {
                            it to accountBalance(it)
                        }
                        .filter {
                            it.second != 0.0
                        }
                        .sortedBy {
                            it.first.accountCode
                        }

                val equityAccounts =
                    accounts
                        .filter {
                            it.accountType.equals(
                                "EQUITY",
                                ignoreCase = true
                            )
                        }
                        .map {
                            it to accountBalance(it)
                        }
                        .filter {
                            it.second != 0.0
                        }
                        .sortedBy {
                            it.first.accountCode
                        }

                // ============================================================
                // TOTALS
                // ============================================================

                val totalRevenue =
                    revenueAccounts.sumOf {
                        it.second
                    }

                val totalExpenses =
                    expenseAccounts.sumOf {
                        it.second
                    }

                val netIncome =
                    totalRevenue - totalExpenses

                val totalAssets =
                    assetAccounts.sumOf {
                        it.second
                    }

                val totalLiabilities =
                    liabilityAccounts.sumOf {
                        it.second
                    }

                val totalEquityAccounts =
                    equityAccounts.sumOf {
                        it.second
                    }

                val totalEquity =
                    totalEquityAccounts + netIncome

                val totalLiabilitiesAndEquity =
                    totalLiabilities + totalEquity

                val isBalanced =
                    kotlin.math.abs(
                        totalAssets -
                                totalLiabilitiesAndEquity
                    ) < 0.01

                // ============================================================
                // PDF SETUP
                // ============================================================

                val pdfDocument =
                    android.graphics.pdf.PdfDocument()

                val pageWidth = 595
                val pageHeight = 842

                val margin = 40f

                // Each financial row
                val rowHeight = 22f

                // ============================================================
                // LOGO
                // ============================================================

                val context =
                    getApplication<android.app.Application>()

                val logoBitmap =
                    try {

                        val decodeOptions =
                            android.graphics.BitmapFactory
                                .Options()
                                .apply {
                                    inSampleSize = 2
                                    inPreferredConfig =
                                        android.graphics.Bitmap.Config.ARGB_8888
                                }

                        val rawBitmap =
                            android.graphics.BitmapFactory
                                .decodeResource(
                                    context.resources,
                                    com.expenso.app.R.drawable.logo,
                                    decodeOptions
                                )

                        if (rawBitmap != null) {

                            android.graphics.Bitmap
                                .createScaledBitmap(
                                    rawBitmap,
                                    56,
                                    56,
                                    true
                                )

                        } else {
                            null
                        }

                    } catch (e: Exception) {
                        null
                    }

                // ============================================================
                // PAINTS
                // ============================================================

                val paint =
                    android.graphics.Paint().apply {
                        isAntiAlias = true
                        isFilterBitmap = true
                    }

                val textPaint =
                    android.graphics.Paint().apply {
                        color =
                            android.graphics.Color.BLACK
                        textSize = 10f
                        isAntiAlias = true
                        typeface =
                            android.graphics.Typeface.create(
                                "sans-serif",
                                android.graphics.Typeface.NORMAL
                            )
                    }

                val boldPaint =
                    android.graphics.Paint().apply {
                        color =
                            android.graphics.Color.BLACK
                        textSize = 10f
                        isAntiAlias = true
                        typeface =
                            android.graphics.Typeface.create(
                                "sans-serif",
                                android.graphics.Typeface.BOLD
                            )
                    }

                val titlePaint =
                    android.graphics.Paint().apply {
                        color =
                            android.graphics.Color.parseColor(
                                "#121212"
                            )
                        textSize = 22f
                        isAntiAlias = true
                        typeface =
                            android.graphics.Typeface.create(
                                "sans-serif",
                                android.graphics.Typeface.BOLD
                            )
                    }

                val subtitlePaint =
                    android.graphics.Paint().apply {
                        color =
                            android.graphics.Color.GRAY
                        textSize = 9f
                        isAntiAlias = true
                    }

                val sectionPaint =
                    android.graphics.Paint().apply {
                        color =
                            android.graphics.Color.parseColor(
                                "#006B5D"
                            )
                        textSize = 12f
                        isAntiAlias = true
                        typeface =
                            android.graphics.Typeface.create(
                                "sans-serif",
                                android.graphics.Typeface.BOLD
                            )
                    }

                // ============================================================
                // BUILD REPORT ROWS
                // ============================================================

                data class PdfRow(
                    val label: String,
                    val amount: Double,
                    val bold: Boolean = false
                )

                val rows =
                    mutableListOf<PdfRow>()

                rows.add(
                    PdfRow(
                        "INCOME STATEMENT",
                        0.0,
                        true
                    )
                )

                rows.add(
                    PdfRow(
                        "Revenue",
                        totalRevenue
                    )
                )

                revenueAccounts.forEach { (account, amount) ->
                    rows.add(
                        PdfRow(
                            "  ${account.accountCode} - ${account.accountName}",
                            amount
                        )
                    )
                }

                rows.add(
                    PdfRow(
                        "Total Revenue",
                        totalRevenue,
                        true
                    )
                )

                rows.add(
                    PdfRow(
                        "Expenses",
                        totalExpenses
                    )
                )

                expenseAccounts.forEach { (account, amount) ->
                    rows.add(
                        PdfRow(
                            "  ${account.accountCode} - ${account.accountName}",
                            amount
                        )
                    )
                }

                rows.add(
                    PdfRow(
                        "Total Expenses",
                        totalExpenses,
                        true
                    )
                )

                rows.add(
                    PdfRow(
                        "Net Income",
                        netIncome,
                        true
                    )
                )

                rows.add(
                    PdfRow(
                        "BALANCE SHEET",
                        0.0,
                        true
                    )
                )

                rows.add(
                    PdfRow(
                        "ASSETS",
                        0.0,
                        true
                    )
                )

                assetAccounts.forEach { (account, amount) ->
                    rows.add(
                        PdfRow(
                            "${account.accountCode} - ${account.accountName}",
                            amount
                        )
                    )
                }

                rows.add(
                    PdfRow(
                        "Total Assets",
                        totalAssets,
                        true
                    )
                )

                rows.add(
                    PdfRow(
                        "LIABILITIES",
                        0.0,
                        true
                    )
                )

                liabilityAccounts.forEach { (account, amount) ->
                    rows.add(
                        PdfRow(
                            "${account.accountCode} - ${account.accountName}",
                            amount
                        )
                    )
                }

                rows.add(
                    PdfRow(
                        "Total Liabilities",
                        totalLiabilities,
                        true
                    )
                )

                rows.add(
                    PdfRow(
                        "EQUITY",
                        0.0,
                        true
                    )
                )

                equityAccounts.forEach { (account, amount) ->
                    rows.add(
                        PdfRow(
                            "${account.accountCode} - ${account.accountName}",
                            amount
                        )
                    )
                }

                rows.add(
                    PdfRow(
                        "Current Period Net Income",
                        netIncome
                    )
                )

                rows.add(
                    PdfRow(
                        "Total Equity",
                        totalEquity,
                        true
                    )
                )

                rows.add(
                    PdfRow(
                        "Liabilities + Equity",
                        totalLiabilitiesAndEquity,
                        true
                    )
                )

                // ============================================================
                // PAGE CALCULATION
                // ============================================================

                val firstPageStartY = 190f
                val otherPageStartY = 95f
                val footerY = pageHeight - 20f

                val firstPageRows =
                    (
                            (
                                    pageHeight -
                                            firstPageStartY -
                                            50f
                                    ) / rowHeight
                            )
                        .toInt()
                        .coerceAtLeast(1)

                val otherPageRows =
                    (
                            (
                                    pageHeight -
                                            otherPageStartY -
                                            50f
                                    ) / rowHeight
                            )
                        .toInt()
                        .coerceAtLeast(1)

                val chunks =
                    mutableListOf<List<PdfRow>>()

                if (rows.isNotEmpty()) {
                    var startIndex = 0
                    var first = true

                    while (startIndex < rows.size) {

                        val pageSize =
                            if (first) firstPageRows else otherPageRows

                        val endIndex =
                            (startIndex + pageSize).coerceAtMost(rows.size)

                        chunks.add(
                            rows.subList(startIndex, endIndex)
                        )

                        startIndex = endIndex
                        first = false
                    }
                } else {
                    chunks.add(emptyList())
                }

                // ============================================================
                // DRAW PAGES
                // ============================================================

                chunks.forEachIndexed { pageIndex, pageRows ->

                    val pageInfo =
                        android.graphics.pdf.PdfDocument
                            .PageInfo
                            .Builder(
                                pageWidth,
                                pageHeight,
                                pageIndex + 1
                            )
                            .create()

                    val page =
                        pdfDocument.startPage(pageInfo)

                    val canvas =
                        page.canvas

                    // ========================================================
                    // HEADER
                    // ========================================================

                    if (pageIndex == 0) {

                        if (logoBitmap != null) {

                            val logoRect =
                                android.graphics.RectF(
                                    margin,
                                    25f,
                                    margin + 42f,
                                    67f
                                )

                            canvas.drawBitmap(
                                logoBitmap,
                                null,
                                logoRect,
                                paint
                            )
                        }

                        canvas.drawText(
                            "SULVA",
                            margin + 52f,
                            52f,
                            titlePaint
                        )

                        canvas.drawText(
                            "FINANCIAL STATEMENTS",
                            margin,
                            88f,
                            boldPaint
                        )

                        canvas.drawText(
                            "Income Statement & Balance Sheet",
                            margin,
                            104f,
                            subtitlePaint
                        )

                        canvas.drawText(
                            "Generated: ${
                                java.text.SimpleDateFormat(
                                    "yyyy-MM-dd HH:mm",
                                    java.util.Locale.getDefault()
                                ).format(
                                    java.util.Date()
                                )
                            }",
                            margin,
                            119f,
                            subtitlePaint
                        )

                        paint.color =
                            android.graphics.Color.parseColor(
                                "#006B5D"
                            )

                        canvas.drawRect(
                            margin,
                            135f,
                            pageWidth - margin,
                            139f,
                            paint
                        )

                    } else {

                        if (logoBitmap != null) {

                            val logoRect =
                                android.graphics.RectF(
                                    margin,
                                    25f,
                                    margin + 28f,
                                    53f
                                )

                            canvas.drawBitmap(
                                logoBitmap,
                                null,
                                logoRect,
                                paint
                            )
                        }

                        canvas.drawText(
                            "SULVA - Financial Statements",
                            margin + 36f,
                            44f,
                            boldPaint
                        )
                    }

                    // ========================================================
                    // REPORT ROWS
                    // ========================================================

                    var currentY =
                        if (pageIndex == 0) {
                            firstPageStartY
                        } else {
                            otherPageStartY
                        }

                    pageRows.forEach { row ->

                        when (row.label) {

                            "INCOME STATEMENT",
                            "BALANCE SHEET",
                            "ASSETS",
                            "LIABILITIES",
                            "EQUITY" -> {

                                canvas.drawText(
                                    row.label,
                                    margin,
                                    currentY,
                                    sectionPaint
                                )

                                currentY += 24f
                            }

                            else -> {

                                val rowPaint =
                                    if (row.bold) {
                                        boldPaint
                                    } else {
                                        textPaint
                                    }

                                canvas.drawText(
                                    row.label,
                                    margin,
                                    currentY,
                                    rowPaint
                                )

                                canvas.drawText(
                                    formatFinancialPdfMoney(
                                        row.amount
                                    ),
                                    pageWidth - margin - 100f,
                                    currentY,
                                    rowPaint
                                )

                                currentY += rowHeight
                            }
                        }
                    }

                    // ========================================================
                    // FOOTER
                    // ========================================================

                    val footerPaint =
                        android.graphics.Paint().apply {
                            color =
                                android.graphics.Color.GRAY
                            textSize = 8f
                            isAntiAlias = true
                        }

                    canvas.drawText(
                        "Generated by SULVA Accounting System",
                        margin,
                        footerY,
                        footerPaint
                    )

                    canvas.drawText(
                        "Page ${pageIndex + 1} of ${chunks.size}",
                        pageWidth - 110f,
                        footerY,
                        footerPaint
                    )

                    pdfDocument.finishPage(page)
                }

                // ============================================================
                // WRITE PDF
                // ============================================================

                pdfDocument.writeTo(outStream)

                outStream.flush()

                pdfDocument.close()

                success = true

            } catch (e: Exception) {

                e.printStackTrace()

                success = false

            } finally {

                try {
                    outStream.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                setFileProcessing(false)

                onComplete(success)
            }
        }
    }
    //ENDS HERE

    private fun formatFinancialPdfMoney(
        amount: Double
    ): String {
        return "GH₵ %.2f".format(
            java.util.Locale.getDefault(),
            amount
        )
    }

    //CODE STARTS HERE
    private fun calculateAPAgingDaysOverdue(
        dueDate: String,
        asOfDate: String
    ): Long {
        return try {

            val formatter =
                java.text.SimpleDateFormat(
                    "yyyy-MM-dd",
                    java.util.Locale.getDefault()
                )

            formatter.isLenient = false

            val due =
                formatter.parse(dueDate)
                    ?: return 0L

            val asOf =
                formatter.parse(asOfDate)
                    ?: return 0L

            val difference =
                asOf.time - due.time

            difference /
                    (1000L * 60L * 60L * 24L)

        } catch (e: Exception) {
            0L
        }
    }

    private fun formatAPAgingMoney(
        amount: Double
    ): String {
        return String.format(
            java.util.Locale.getDefault(),
            "GH₵%.2f",
            amount
        )
    }
    //ENDS HERE


private fun calculateARAgingDaysOverdue(
    dueDate: String,
    asOfDate: String
): Long {
    return try {

        val formatter =
            java.text.SimpleDateFormat(
                "yyyy-MM-dd",
                java.util.Locale.getDefault()
            )

        formatter.isLenient = false

        val due =
            formatter.parse(dueDate)
                ?: return 0L

        val asOf =
            formatter.parse(asOfDate)
                ?: return 0L

        val difference =
            asOf.time - due.time

        difference /
                (1000L * 60L * 60L * 24L)

    } catch (e: Exception) {
        0L
    }
}

private fun formatARAgingMoney(
    amount: Double
): String {
    return String.format(
        java.util.Locale.getDefault(),
        "GH₵%.2f",
        amount
    )
}

// ============================================================
// EXISTING CSV IMPORT FUNCTION
// ============================================================


fun importTransactionsFromCsv(inStream: java.io.InputStream, onComplete: (Boolean, String) -> Unit) {
        setFileProcessing(true, "Importing transactions from CSV...")
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            var success = false
            var message = ""
            try {
                val reader = java.io.BufferedReader(java.io.InputStreamReader(inStream))
                val lines = reader.readLines()
                if (lines.isEmpty()) {
                    onComplete(false, "CSV file is empty")
                    return@launch
                }

                // Check header to identify columns or map them
                val header = lines.first().lowercase()
                val hasHeader = header.contains("amount") || header.contains("type") || header.contains("date")
                val dataLines = if (hasHeader) lines.drop(1) else lines

                var successCount = 0
                var errorCount = 0

                // Parse each line
                dataLines.forEach { line ->
                    if (line.trim().isEmpty()) return@forEach
                    try {
                        val tokens = parseCsvLine(line)
                        if (tokens.size >= 5) {
                            val dateStrRaw: String
                            val typeRaw: String
                            val amountRaw: String
                            val noteRaw: String
                            val categoryRaw: String
                            val accountRaw: String

                            if (tokens.size >= 7 && hasHeader && (header.startsWith("transaction id") || header.contains("id"))) {
                                dateStrRaw = tokens[1].trim()
                                typeRaw = tokens[2].trim()
                                amountRaw = tokens[3].trim()
                                noteRaw = tokens[4].trim()
                                categoryRaw = tokens[5].trim()
                                accountRaw = tokens[6].trim()
                            } else {
                                dateStrRaw = tokens.getOrNull(0)?.trim() ?: ""
                                typeRaw = tokens.getOrNull(1)?.trim() ?: "expense"
                                amountRaw = tokens.getOrNull(2)?.trim() ?: "0.0"
                                noteRaw = tokens.getOrNull(3)?.trim() ?: ""
                                categoryRaw = tokens.getOrNull(4)?.trim() ?: "Other"
                                accountRaw = tokens.getOrNull(5)?.trim() ?: "Cash"
                            }

                            val amount = amountRaw.toDoubleOrNull() ?: 0.0
                            val type = if (typeRaw.lowercase().contains("income")) "income" else "expense"
                            val note = noteRaw.removeSurrounding("\"")
                            val catName = categoryRaw.removeSurrounding("\"").ifBlank { "Other" }
                            val accName = accountRaw.removeSurrounding("\"").ifBlank { "Cash" }

                            // Format Date
                            var finalDate = dateStrRaw.removeSurrounding("\"")
                            if (finalDate.isBlank()) {
                                finalDate = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(Date())
                            } else if (!finalDate.contains("T")) {
                                finalDate = "${finalDate}T09:00:00"
                            }

                            // 1. Find or create Account
                            var account = repository.getAccountByNameDirect(accName)
                            if (account == null) {
                                val newAcc = Account(
                                    name = accName,
                                    type = "Other",
                                    openingBalance = 0.0,
                                    currentBalance = 0.0,
                                    color = "#4CAF50",
                                    icon = "wallet"
                                )
                                val accId = repository.insertAccountDirect(newAcc)
                                account = newAcc.copy(id = accId.toInt())
                            }

                            // 2. Find or create Category
                            var category = repository.getCategoryByNameDirect(catName, type)
                            if (category == null) {
                                val newCat = Category(
                                    name = catName,
                                    type = type,
                                    color = if (type == "income") "#4CAF50" else "#F44336",
                                    icon = if (type == "income") "trending_up" else "trending_down"
                                )
                                val catId = repository.insertCategoryDirect(newCat)
                                category = newCat.copy(id = catId.toInt())
                            }

                            // 3. Insert transaction
                            val tx = Transaction(
                                amount = amount,
                                type = type,
                                categoryId = category.id,
                                accountId = account.id,
                                note = note,
                                transactionDate = finalDate,
                                createdAt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(Date()),
                                updatedAt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(Date())
                            )
                            repository.insertTransaction(tx)
                            successCount++
                        } else {
                            errorCount++
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        errorCount++
                    }
                }
                success = true
                message = "Successfully imported $successCount transactions. Failed: $errorCount"
            } catch (e: Exception) {
                e.printStackTrace()
                success = false
                message = "Import failed: ${e.localizedMessage}"
            } finally {
                try {
                    inStream.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                setFileProcessing(false)
                onComplete(success, message)
            }
        }
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        var curVal = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val ch = line[i]
            if (inQuotes) {
                if (ch == '\"') {
                    if (i + 1 < line.length && line[i + 1] == '\"') {
                        curVal.append('\"')
                        i++
                    } else {
                        inQuotes = false
                    }
                } else {
                    curVal.append(ch)
                }
            } else {
                if (ch == '\"') {
                    inQuotes = true
                } else if (ch == ',') {
                    result.add(curVal.toString())
                    curVal = StringBuilder()
                } else {
                    curVal.append(ch)
                }
            }
            i++
        }
        result.add(curVal.toString())
        return result
    }

    private fun saveCachedAccounts(accounts: List<Account>) {
        val array = org.json.JSONArray()
        accounts.forEach { array.put(it.toJson()) }
        prefs.edit().putString("cached_accounts", array.toString()).apply()
    }

    private fun loadCachedAccounts(): List<Account> {
        val jsonStr = prefs.getString("cached_accounts", null) ?: return emptyList()
        val list = mutableListOf<Account>()
        try {
            val array = org.json.JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                list.add(jsonToAccount(array.getJSONObject(i)))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    private fun saveCachedCategories(categories: List<Category>) {
        val array = org.json.JSONArray()
        categories.forEach { array.put(it.toJson()) }
        prefs.edit().putString("cached_categories", array.toString()).apply()
    }

    private fun loadCachedCategories(): List<Category> {
        val jsonStr = prefs.getString("cached_categories", null) ?: return emptyList()
        val list = mutableListOf<Category>()
        try {
            val array = org.json.JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                list.add(jsonToCategory(array.getJSONObject(i)))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    private fun saveCachedTransactions(transactions: List<TransactionWithDetails>) {
        val array = org.json.JSONArray()
        transactions.forEach { array.put(it.toJson()) }
        prefs.edit().putString("cached_transactions", array.toString()).apply()
    }

    private fun loadCachedTransactions(): List<TransactionWithDetails> {
        val jsonStr = prefs.getString("cached_transactions", null) ?: return emptyList()
        val list = mutableListOf<TransactionWithDetails>()
        try {
            val array = org.json.JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                list.add(jsonToTransactionWithDetails(array.getJSONObject(i)))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }
}

private fun Account.toJson(): org.json.JSONObject {
    return org.json.JSONObject().apply {
        put("id", id)
        put("name", name)
        put("type", type)
        put("openingBalance", openingBalance)
        put("currentBalance", currentBalance)
        put("icon", icon)
        put("color", color)
    }
}

private fun jsonToAccount(json: org.json.JSONObject): Account {
    return Account(
        id = json.getInt("id"),
        name = json.getString("name"),
        type = json.getString("type"),
        openingBalance = json.getDouble("openingBalance"),
        currentBalance = json.getDouble("currentBalance"),
        icon = json.getString("icon"),
        color = json.getString("color")
    )
}

private fun Category.toJson(): org.json.JSONObject {
    return org.json.JSONObject().apply {
        put("id", id)
        put("name", name)
        put("type", type)
        put("icon", icon)
        put("color", color)
        put("isDefault", isDefault)
        put("orderIndex", orderIndex)
    }
}

private fun jsonToCategory(json: org.json.JSONObject): Category {
    return Category(
        id = json.getInt("id"),
        name = json.getString("name"),
        type = json.getString("type"),
        icon = json.getString("icon"),
        color = json.getString("color"),
        isDefault = json.optBoolean("isDefault", false),
        orderIndex = json.optInt("orderIndex", 0)
    )
}

private fun TransactionWithDetails.toJson(): org.json.JSONObject {
    return org.json.JSONObject().apply {
        put("id", id)
        put("amount", amount)
        put("type", type)
        put("categoryId", categoryId)
        put("accountId", accountId)
        put("note", note)
        put("transactionDate", transactionDate)
        put("createdAt", createdAt)
        put("updatedAt", updatedAt)
        put("attachmentPath", attachmentPath ?: "")
        put("tags", tags)
        put("categoryName", categoryName)
        put("categoryIcon", categoryIcon)
        put("categoryColor", categoryColor)
        put("accountName", accountName)
        put("accountIcon", accountIcon)
        put("accountColor", accountColor)
    }
}

private fun jsonToTransactionWithDetails(json: org.json.JSONObject): TransactionWithDetails {
    return TransactionWithDetails(
        id = json.getInt("id"),
        amount = json.getDouble("amount"),
        type = json.getString("type"),
        categoryId = json.getInt("categoryId"),
        accountId = json.getInt("accountId"),
        note = json.getString("note"),
        transactionDate = json.getString("transactionDate"),
        createdAt = json.getString("createdAt"),
        updatedAt = json.getString("updatedAt"),
        attachmentPath = json.optString("attachmentPath").let { if (it.isEmpty()) null else it },
        tags = json.optString("tags", ""),
        categoryName = json.getString("categoryName"),
        categoryIcon = json.getString("categoryIcon"),
        categoryColor = json.getString("categoryColor"),
        accountName = json.getString("accountName"),
        accountIcon = json.getString("accountIcon"),
        accountColor = json.getString("accountColor")
    )
}
