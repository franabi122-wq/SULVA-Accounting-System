package com.expenso.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.expenso.app.data.ExpensoViewModel
import com.expenso.app.data.Supplier
import com.expenso.app.data.SupplierInvoice
import com.expenso.app.data.SupplierPayment
import com.expenso.app.ui.screens.SupplierPaymentsScreen
import com.expenso.app.ui.components.ExpensoTopBar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SupplierPaymentsScreen(
    viewModel: ExpensoViewModel,
    onDismiss: () -> Unit
) {
    val payments by viewModel.allSupplierPayments.collectAsStateWithLifecycle()
    val suppliers by viewModel.activeSuppliers.collectAsStateWithLifecycle()
    val invoices by viewModel.allSupplierInvoices.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var showFormDialog by remember { mutableStateOf(false) }
    var selectedPayment by remember { mutableStateOf<SupplierPayment?>(null) }
    var paymentToDelete by remember { mutableStateOf<SupplierPayment?>(null) }
    var paymentErrorMessage by remember { mutableStateOf("") }
    val filteredPayments = payments.filter { payment ->

        val supplierName =
            suppliers.firstOrNull {
                it.id == payment.supplierId
            }?.name ?: ""

        val invoiceNumber =
            invoices.firstOrNull {
                it.id == payment.supplierInvoiceId
            }?.invoiceNumber ?: ""

        supplierName.contains(searchQuery, ignoreCase = true) ||
                invoiceNumber.contains(searchQuery, ignoreCase = true) ||
                payment.referenceNumber.contains(searchQuery, ignoreCase = true) ||
                payment.paymentMethod.contains(searchQuery, ignoreCase = true) ||
                payment.notes.contains(searchQuery, ignoreCase = true)
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {

        ExpensoTopBar(
            title = "Supplier Payments",
            actionIcon = Icons.Default.Add,
            onActionClick = {
                selectedPayment = null
                showFormDialog = true
            },
            actionContentDescription = "Add Supplier Payment",
            navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
            onNavigationClick = onDismiss,
            navigationContentDescription = "Back"
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {

            Text(
                text = "Record payments made to suppliers.",
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = {
                    searchQuery = it
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text("Search payments...")
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search"
                    )
                },
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (filteredPayments.isEmpty()) {

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Icon(
                        imageVector = Icons.Default.Payments,
                        contentDescription = null
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = if (searchQuery.isBlank()) {
                            "No supplier payments yet."
                        } else {
                            "No supplier payments found."
                        },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    if (searchQuery.isBlank()) {
                        Text(
                            text = "Tap + to record your first supplier payment.",
                            fontSize = 14.sp
                        )
                    }
                }

            } else {

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {

                    items(
                        items = filteredPayments,
                        key = { it.id }
                    ) { payment ->

                        val supplierName =
                            suppliers.firstOrNull {
                                it.id == payment.supplierId
                            }?.name ?: "Unknown Supplier"

                        val invoiceNumber =
                            invoices.firstOrNull {
                                it.id == payment.supplierInvoiceId
                            }?.invoiceNumber ?: "Unknown Invoice"

                        SupplierPaymentCard(
                            payment = payment,
                            supplierName = supplierName,
                            invoiceNumber = invoiceNumber,
                            onEdit = {
                                selectedPayment = payment
                                showFormDialog = true
                            },
                            onDelete = {
                                paymentToDelete = payment
                            }
                        )
                    }
                }
            }
        }
    }

    if (showFormDialog) {

        SupplierPaymentFormDialog(
            payment = selectedPayment,
            suppliers = suppliers,
            invoices = invoices,
            onDismiss = {
                showFormDialog = false
                selectedPayment = null
            },
            onSave = { payment ->

                if (selectedPayment == null) {

                    viewModel.addSupplierPayment(
                        payment = payment,
                        onSuccess = {
                            showFormDialog = false
                            selectedPayment = null
                        }
                    )

                } else {

                    viewModel.updateSupplierPayment(
                        payment = payment,
                        onSuccess = {
                            showFormDialog = false
                            selectedPayment = null
                        }
                    )
                }
            }
        )
    }

    paymentToDelete?.let { payment ->

        AlertDialog(
            onDismissRequest = {
                paymentToDelete = null
            },
            title = {
                Text("Delete Supplier Payment")
            },
            text = {
                Text(
                    "Are you sure you want to delete this supplier payment?"
                )
            },
            confirmButton = {

                Button(
                    onClick = {

                        viewModel.deleteSupplierPayment(
                            payment = payment,
                            onSuccess = {
                                paymentToDelete = null
                            }
                        )
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {

                OutlinedButton(
                    onClick = {
                        paymentToDelete = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SupplierPaymentCard(
    payment: SupplierPayment,
    supplierName: String,
    invoiceNumber: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {

        Column(
            modifier = Modifier.padding(16.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Column(
                    modifier = Modifier.weight(1f)
                ) {

                    Text(
                        text = formatAmount(payment.amount),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = supplierName,
                        fontSize = 14.sp
                    )
                }

                IconButton(
                    onClick = onEdit
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Payment"
                    )
                }

                IconButton(
                    onClick = onDelete
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Payment"
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Invoice: $invoiceNumber",
                fontSize = 14.sp
            )

            Text(
                text = "Payment Date: ${payment.paymentDate}",
                fontSize = 14.sp
            )

            Text(
                text = "Payment Method: ${payment.paymentMethod}",
                fontSize = 14.sp
            )

            if (payment.referenceNumber.isNotBlank()) {
                Text(
                    text = "Reference: ${payment.referenceNumber}",
                    fontSize = 14.sp
                )
            }

            if (payment.notes.isNotBlank()) {
                Text(
                    text = "Notes: ${payment.notes}",
                    fontSize = 14.sp
                )
            }
        }
    }
}

private fun formatAmount(amount: Double): String {
    return String.format(
        Locale.getDefault(),
        "GH₵ %.2f",
        amount
    )
}

//form dialog starts here
@Composable
private fun SupplierPaymentFormDialog(
    payment: SupplierPayment?,
    suppliers: List<Supplier>,
    invoices: List<SupplierInvoice>,
    onDismiss: () -> Unit,
    onSave: (SupplierPayment) -> Unit
) {
    var supplierId by remember(payment) {
        mutableStateOf(payment?.supplierId ?: 0)
    }

    var supplierInvoiceId by remember(payment) {
        mutableStateOf(payment?.supplierInvoiceId ?: 0)
    }

    var paymentDate by remember(payment) {
        mutableStateOf(
            payment?.paymentDate
                ?: SimpleDateFormat(
                    "yyyy-MM-dd",
                    Locale.getDefault()
                ).format(Date())
        )
    }

    var amount by remember(payment) {
        mutableStateOf(
            if (payment == null) {
                ""
            } else {
                payment.amount.toString()
            }
        )
    }

    var paymentMethod by remember(payment) {
        mutableStateOf(
            payment?.paymentMethod ?: "CASH"
        )
    }

    var referenceNumber by remember(payment) {
        mutableStateOf(
            payment?.referenceNumber ?: ""
        )
    }

    var notes by remember(payment) {
        mutableStateOf(
            payment?.notes ?: ""
        )
    }

    // Controls the validation dialog
    var validationMessage by remember {
        mutableStateOf<String?>(null)
    }

    val selectedSupplierInvoices =
        invoices.filter {
            it.supplierId == supplierId
        }

    /*
     * Show the main Supplier Payment form.
     */
    AlertDialog(
        onDismissRequest = onDismiss,

        title = {
            Text(
                text = if (payment == null) {
                    "Add Supplier Payment"
                } else {
                    "Edit Supplier Payment"
                }
            )
        },

        text = {

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                // -----------------------------
                // SUPPLIER
                // -----------------------------

                //CODE START HERE
                // -----------------------------
// SUPPLIER DROPDOWN
// -----------------------------
                if (suppliers.isEmpty()) {

                    Text(
                        text = "No active suppliers available. Add a supplier first.",
                        fontSize = 14.sp
                    )

                } else {

                    Text(
                        text = "Supplier",
                        fontWeight = FontWeight.Medium
                    )

                    var supplierDropdownExpanded by remember {
                        mutableStateOf(false)
                    }

                    val selectedSupplier =
                        suppliers.firstOrNull {
                            it.id == supplierId
                        }

                    Box(
                        modifier = Modifier.fillMaxWidth()
                    ) {

                        OutlinedButton(
                            onClick = {
                                supplierDropdownExpanded =
                                    !supplierDropdownExpanded
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = selectedSupplier?.name
                                    ?: "Select Supplier"
                            )
                        }

                        DropdownMenu(
                            expanded = supplierDropdownExpanded,
                            onDismissRequest = {
                                supplierDropdownExpanded = false
                            }
                        ) {

                            suppliers.forEach { supplier ->

                                DropdownMenuItem(
                                    text = {
                                        Text(supplier.name)
                                    },
                                    onClick = {

                                        supplierId = supplier.id

                                        // Changing supplier must reset invoice
                                        supplierInvoiceId = 0

                                        supplierDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
                //CODE ENDS HERE

                Spacer(modifier = Modifier.height(4.dp))

                // -----------------------------
                // SUPPLIER INVOICE
                // -----------------------------

                //CODE START HERE
                // -----------------------------
// SUPPLIER INVOICE DROPDOWN
// -----------------------------
                if (supplierId != 0) {

                    Text(
                        text = "Supplier Invoice",
                        fontWeight = FontWeight.Medium
                    )

                    if (selectedSupplierInvoices.isEmpty()) {

                        Text(
                            text = "No invoices found for this supplier.",
                            fontSize = 14.sp
                        )

                    } else {

                        var invoiceDropdownExpanded by remember {
                            mutableStateOf(false)
                        }

                        val selectedInvoice =
                            selectedSupplierInvoices.firstOrNull {
                                it.id == supplierInvoiceId
                            }

                        Box(
                            modifier = Modifier.fillMaxWidth()
                        ) {

                            OutlinedButton(
                                onClick = {
                                    invoiceDropdownExpanded =
                                        !invoiceDropdownExpanded
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = selectedInvoice?.invoiceNumber
                                        ?: "Select Supplier Invoice"
                                )
                            }

                            DropdownMenu(
                                expanded = invoiceDropdownExpanded,
                                onDismissRequest = {
                                    invoiceDropdownExpanded = false
                                }
                            ) {

                                selectedSupplierInvoices.forEach { invoice ->

                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                "${invoice.invoiceNumber} - ${
                                                    formatAmount(
                                                        invoice.totalAmount
                                                    )
                                                }"
                                            )
                                        },
                                        onClick = {

                                            supplierInvoiceId =
                                                invoice.id

                                            invoiceDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
      //CODE ENDS HERE


                // -----------------------------
                // PAYMENT DATE
                // -----------------------------
                OutlinedTextField(
                    value = paymentDate,
                    onValueChange = {
                        paymentDate = it
                    },
                    label = {
                        Text("Payment Date")
                    },
                    placeholder = {
                        Text("YYYY-MM-DD")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // -----------------------------
                // AMOUNT
                // -----------------------------
                OutlinedTextField(
                    value = amount,
                    onValueChange = {
                        amount = it
                    },
                    label = {
                        Text("Amount")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // -----------------------------
                // PAYMENT METHOD
                // -----------------------------
                OutlinedTextField(
                    value = paymentMethod,
                    onValueChange = {
                        paymentMethod = it
                    },
                    label = {
                        Text("Payment Method")
                    },
                    placeholder = {
                        Text("CASH, BANK, MOBILE MONEY...")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // -----------------------------
                // REFERENCE NUMBER
                // -----------------------------
                OutlinedTextField(
                    value = referenceNumber,
                    onValueChange = {
                        referenceNumber = it
                    },
                    label = {
                        Text("Reference Number")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // -----------------------------
                // NOTES
                // -----------------------------
                OutlinedTextField(
                    value = notes,
                    onValueChange = {
                        notes = it
                    },
                    label = {
                        Text("Notes")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
            }
        },

        // -----------------------------
        // SAVE BUTTON
        // -----------------------------
        confirmButton = {

            Button(
                onClick = {

                    // --------------------------------
                    // 1. SUPPLIER VALIDATION
                    // --------------------------------
                    if (supplierId == 0) {
                        validationMessage =
                            "Please select a supplier."
                        return@Button
                    }

                    // --------------------------------
                    // 2. INVOICE VALIDATION
                    // --------------------------------
                    if (supplierInvoiceId == 0) {
                        validationMessage =
                            "Please select a supplier invoice."
                        return@Button
                    }

                    // --------------------------------
                    // 3. PAYMENT DATE VALIDATION
                    // --------------------------------
                    if (paymentDate.isBlank()) {
                        validationMessage =
                            "Payment date is required."
                        return@Button
                    }

                    // --------------------------------
                    // 4. AMOUNT VALIDATION
                    // --------------------------------
                    val parsedAmount =
                        amount.toDoubleOrNull()

                    if (
                        parsedAmount == null ||
                        parsedAmount <= 0.0
                    ) {
                        validationMessage =
                            "Enter a valid payment amount."
                        return@Button
                    }

                    // --------------------------------
                    // 5. FIND SELECTED INVOICE
                    // --------------------------------
                    val selectedInvoice =
                        invoices.firstOrNull {
                            it.id == supplierInvoiceId &&
                                    it.supplierId == supplierId
                        }

                    if (selectedInvoice == null) {
                        validationMessage =
                            "The selected supplier invoice could not be found."
                        return@Button
                    }

                    // --------------------------------
                    // 6. CALCULATE ALREADY-PAID AMOUNT
                    // --------------------------------
                    /*
                     * SupplierInvoice.amountPaid contains the amount
                     * already recorded against the invoice.
                     *
                     * When EDITING an existing payment, we subtract
                     * that payment from the already-paid amount because
                     * we are about to replace it with the new amount.
                     */
                    val alreadyPaid =
                        selectedInvoice.amountPaid

                    val currentPaymentAmount =
                        if (payment != null) {
                            payment.amount
                        } else {
                            0.0
                        }

                    val outstandingAmount =
                        (
                                selectedInvoice.totalAmount -
                                        alreadyPaid +
                                        currentPaymentAmount
                                ).coerceAtLeast(0.0)

                    // --------------------------------
                    // 7. EXCESS PAYMENT VALIDATION
                    // --------------------------------
                    if (parsedAmount > outstandingAmount) {

                        validationMessage =
                            "Payment amount cannot exceed the outstanding invoice amount.\n\n" +
                                    "Invoice Total: ${
                                        formatAmount(
                                            selectedInvoice.totalAmount
                                        )
                                    }\n" +
                                    "Already Paid: ${
                                        formatAmount(
                                            (alreadyPaid - currentPaymentAmount)
                                                .coerceAtLeast(0.0)
                                        )
                                    }\n" +
                                    "Outstanding: ${
                                        formatAmount(
                                            outstandingAmount
                                        )
                                    }\n" +
                                    "Payment Entered: ${
                                        formatAmount(
                                            parsedAmount
                                        )
                                    }"

                        return@Button
                    }

                    // --------------------------------
                    // 8. PAYMENT METHOD VALIDATION
                    // --------------------------------
                    if (paymentMethod.isBlank()) {
                        validationMessage =
                            "Payment method is required."
                        return@Button
                    }

                    // --------------------------------
                    // 9. CREATE PAYMENT
                    // --------------------------------
                    val now =
                        SimpleDateFormat(
                            "yyyy-MM-dd HH:mm:ss",
                            Locale.getDefault()
                        ).format(Date())

                    val newPayment =
                        SupplierPayment(
                            id = payment?.id ?: 0,
                            supplierId = supplierId,
                            supplierInvoiceId = supplierInvoiceId,
                            paymentDate = paymentDate,
                            amount = parsedAmount,
                            paymentMethod = paymentMethod.trim(),
                            referenceNumber = referenceNumber.trim(),
                            notes = notes.trim(),
                            createdAt = payment?.createdAt ?: now
                        )

                    // --------------------------------
                    // 10. SAVE
                    // --------------------------------
                    onSave(newPayment)
                }
            ) {
                Text("Save")
            }
        },

        // -----------------------------
        // CANCEL BUTTON
        // -----------------------------
        dismissButton = {

            OutlinedButton(
                onClick = onDismiss
            ) {
                Text("Cancel")
            }
        }
    )

    // =====================================================
    // VALIDATION DIALOG
    // =====================================================
    if (validationMessage != null) {
        AlertDialog(
            onDismissRequest = {
                validationMessage = null
            },

            title = {
                Text(
                    text = "Validation"
                )
            },

            text = {
                Text(
                    text = validationMessage ?: ""
                )
            },

            confirmButton = {
                Button(
                    onClick = {
                        validationMessage = null
                    }
                ) {
                    Text("OK")
                }
            }
        )
    }
}
//end of SupplierPaymentFormDialog code
