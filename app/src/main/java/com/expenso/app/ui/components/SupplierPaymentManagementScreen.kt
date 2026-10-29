package com.expenso.app.ui.screens

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.AlertDialog

import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import com.expenso.app.data.SupplierInvoice
import com.expenso.app.data.SupplierPayment
import com.expenso.app.ui.components.ExpensoTopBar
import com.expenso.app.ui.screens.SupplierPaymentFormDialog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SupplierPaymentManagementScreen(
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

        payment.referenceNumber.contains(
            searchQuery,
            ignoreCase = true
        ) ||
                supplierName.contains(
                    searchQuery,
                    ignoreCase = true
                ) ||
                invoiceNumber.contains(
                    searchQuery,
                    ignoreCase = true
                )
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {

        ExpensoTopBar(
            title = "Supplier Payments",
            actionIcon = Icons.Default.Add,
            onActionClick = {
                selectedPayment = null
                paymentErrorMessage = ""
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
                            text = "Tap + to record your first payment.",
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
                                paymentErrorMessage = ""
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
            errorMessage = paymentErrorMessage,
            onDismiss = {
                showFormDialog = false
                selectedPayment = null
            },
            //code ends here
            onSave = { payment ->

                if (selectedPayment == null) {

                    viewModel.addSupplierPayment(
                        payment = payment,
                        onSuccess = {
                            paymentErrorMessage = ""
                            showFormDialog = false
                            selectedPayment = null
                        },
                        onError = { error ->
                            paymentErrorMessage = error
                        }
                    )

                } else {

                    viewModel.updateSupplierPayment(
                        payment = payment,
                        onSuccess = {
                            paymentErrorMessage = ""
                            showFormDialog = false
                            selectedPayment = null
                        },
                        onError = { error ->
                            paymentErrorMessage = error
                        }
                    )
                }
            }
            //code starts here
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
                    "Are you sure you want to delete this payment?"
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
                        text = supplierName,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Invoice: $invoiceNumber",
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
                text = "Payment Date: ${payment.paymentDate}",
                fontSize = 14.sp
            )

            Text(
                text = "Amount: ${formatPaymentAmount(payment.amount)}",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Method: ${payment.paymentMethod}",
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
                    fontSize = 13.sp
                )
            }
        }
    }
}

//SupplierPaymentFormDialog starts here
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SupplierPaymentFormDialog(
    payment: SupplierPayment?,
    suppliers: List<com.expenso.app.data.Supplier>,
    invoices: List<SupplierInvoice>,
    errorMessage: String,
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

    var formErrorMessage by remember {
        mutableStateOf("")
    }

    var supplierDropdownExpanded by remember {
        mutableStateOf(false)
    }

    var invoiceDropdownExpanded by remember {
        mutableStateOf(false)
    }

    var paymentMethodDropdownExpanded by remember {
        mutableStateOf(false)
    }

    val supplierInvoices =
        invoices.filter {
            it.supplierId == supplierId
        }

    val selectedSupplier =
        suppliers.firstOrNull {
            it.id == supplierId
        }

    val selectedInvoice =
        supplierInvoices.firstOrNull {
            it.id == supplierInvoiceId
        }

    val outstandingBalance =
        selectedInvoice?.let {
            (it.totalAmount - it.amountPaid)
                .coerceAtLeast(0.0)
        } ?: 0.0

    AlertDialog(
        onDismissRequest = onDismiss,

        title = {
            Text(
                if (payment == null) {
                    "Add Supplier Payment"
                } else {
                    "Edit Supplier Payment"
                }
            )
        },

        text = {
            val scrollState = rememberScrollState()

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                // ---------------------------------------------------------
                // SUPPLIER DROPDOWN
                // ---------------------------------------------------------

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
                                selectedSupplier?.name
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

                                        // Reset invoice whenever
                                        // supplier changes.
                                        supplierInvoiceId = 0

                                        supplierDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // ---------------------------------------------------------
                // SUPPLIER INVOICE DROPDOWN
                // ---------------------------------------------------------

                if (supplierId != 0) {

                    Text(
                        text = "Supplier Invoice",
                        fontWeight = FontWeight.Medium
                    )

                    if (supplierInvoices.isEmpty()) {

                        Text(
                            text = "No invoices found for this supplier.",
                            fontSize = 14.sp
                        )

                    } else {

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
                                    selectedInvoice?.invoiceNumber
                                        ?: "Select Supplier Invoice"
                                )
                            }

                            DropdownMenu(
                                expanded = invoiceDropdownExpanded,
                                onDismissRequest = {
                                    invoiceDropdownExpanded = false
                                }
                            ) {

                                supplierInvoices.forEach { invoice ->

                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                invoice.invoiceNumber
                                            )
                                        },
                                        onClick = {

                                            supplierInvoiceId =
                                                invoice.id

                                            invoiceDropdownExpanded =
                                                false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // ---------------------------------------------------------
                // SELECTED INVOICE INFORMATION
                // ---------------------------------------------------------

                if (selectedInvoice != null) {

                    Spacer(
                        modifier = Modifier.height(4.dp)
                    )

                    Text(
                        text =
                            "Invoice Total: GH₵ ${
                                String.format(
                                    Locale.getDefault(),
                                    "%.2f",
                                    selectedInvoice.totalAmount
                                )
                            }",
                        fontSize = 14.sp
                    )

                    Text(
                        text =
                            "Amount Paid: GH₵ ${
                                String.format(
                                    Locale.getDefault(),
                                    "%.2f",
                                    selectedInvoice.amountPaid
                                )
                            }",
                        fontSize = 14.sp
                    )

                    Text(
                        text =
                            "Outstanding Balance: GH₵ ${
                                String.format(
                                    Locale.getDefault(),
                                    "%.2f",
                                    outstandingBalance
                                )
                            }",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // ---------------------------------------------------------
                // PAYMENT DATE
                // ---------------------------------------------------------

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

                // ---------------------------------------------------------
                // AMOUNT
                // ---------------------------------------------------------

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

                // ---------------------------------------------------------
                // PAYMENT METHOD DROPDOWN
                // ---------------------------------------------------------

                //Payment method code starts here
// ---------------------------------------------------------
// PAYMENT METHOD DROPDOWN
// ---------------------------------------------------------

                Box(
                    modifier = Modifier.fillMaxWidth()
                ) {

                    OutlinedTextField(
                        value = paymentMethod,
                        onValueChange = {},
                        modifier = Modifier.fillMaxWidth(),
                        readOnly = true,
                        singleLine = true,
                        label = {
                            Text("Payment Method")
                        },
                        placeholder = {
                            Text("Choose a payment method")
                        },
                        trailingIcon = {
                            Icon(
                                imageVector =
                                    if (paymentMethodDropdownExpanded) {
                                        Icons.Rounded.KeyboardArrowUp
                                    } else {
                                        Icons.Rounded.KeyboardArrowDown
                                    },
                                contentDescription =
                                    "Select payment method"
                            )
                        }
                    )

                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable {
                                paymentMethodDropdownExpanded =
                                    !paymentMethodDropdownExpanded
                            }
                    )

                    DropdownMenu(
                        expanded = paymentMethodDropdownExpanded,
                        onDismissRequest = {
                            paymentMethodDropdownExpanded = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {

                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "CASH",
                                    fontWeight = FontWeight.Medium
                                )
                            },
                            onClick = {

                                paymentMethod = "CASH"

                                paymentMethodDropdownExpanded =
                                    false
                            }
                        )

                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "BANK",
                                    fontWeight = FontWeight.Medium
                                )
                            },
                            onClick = {

                                paymentMethod = "BANK"

                                paymentMethodDropdownExpanded =
                                    false
                            }
                        )
                    }
                }
       //payment method code ends here


                // ---------------------------------------------------------
                // REFERENCE NUMBER
                // ---------------------------------------------------------

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

                // ---------------------------------------------------------
                // NOTES
                // ---------------------------------------------------------

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

                // ---------------------------------------------------------
                // LOCAL FORM ERROR
                // ---------------------------------------------------------

                if (formErrorMessage.isNotBlank()) {

                    Text(
                        text = formErrorMessage,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // ---------------------------------------------------------
                // VIEWMODEL ERROR
                // ---------------------------------------------------------

                if (errorMessage.isNotBlank()) {

                    Text(
                        text = errorMessage,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },

        // -------------------------------------------------------------
        // SAVE BUTTON
        // -------------------------------------------------------------

        confirmButton = {

            Button(
                onClick = {

                    if (supplierId == 0) {

                        formErrorMessage =
                            "Please select a supplier."

                        return@Button
                    }

                    if (supplierInvoiceId == 0) {

                        formErrorMessage =
                            "Please select a supplier invoice."

                        return@Button
                    }

                    val paymentAmount =
                        amount.toDoubleOrNull()

                    if (
                        paymentAmount == null ||
                        paymentAmount <= 0.0
                    ) {

                        formErrorMessage =
                            "Enter a valid payment amount."

                        return@Button
                    }

                    if (paymentAmount > outstandingBalance) {

                        formErrorMessage =
                            "Payment cannot exceed the outstanding balance of GH₵ ${
                                String.format(
                                    Locale.getDefault(),
                                    "%.2f",
                                    outstandingBalance
                                )
                            }."

                        return@Button
                    }

                    if (paymentDate.isBlank()) {

                        formErrorMessage =
                            "Payment date is required."

                        return@Button
                    }

                    val now =
                        SimpleDateFormat(
                            "yyyy-MM-dd HH:mm:ss",
                            Locale.getDefault()
                        ).format(Date())

                    val paymentToSave =
                        SupplierPayment(
                            id = payment?.id ?: 0,
                            supplierId = supplierId,
                            supplierInvoiceId =
                                supplierInvoiceId,
                            paymentDate =
                                paymentDate.trim(),
                            amount = paymentAmount,
                            paymentMethod =
                                paymentMethod
                                    .trim()
                                    .ifBlank {
                                        "CASH"
                                    },
                            referenceNumber =
                                referenceNumber.trim(),
                            notes =
                                notes.trim(),
                            createdAt =
                                payment?.createdAt ?: now
                        )

                    onSave(paymentToSave)
                }
            ) {
                Text("Save")
            }
        },

        // -------------------------------------------------------------
        // CANCEL BUTTON
        // -------------------------------------------------------------

        dismissButton = {

            OutlinedButton(
                onClick = onDismiss
            ) {
                Text("Cancel")
            }
        }
    )
}
//code ends here

private fun formatPaymentAmount(
    amount: Double
): String {
    return String.format(
        Locale.getDefault(),
        "GH₵ %.2f",
        amount
    )
}