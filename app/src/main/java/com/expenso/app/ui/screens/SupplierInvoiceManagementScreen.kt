package com.expenso.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import com.expenso.app.ui.components.ExpensoTopBar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SupplierInvoiceManagementScreen(
    viewModel: ExpensoViewModel,
    onDismiss: () -> Unit
) {
    val invoices by viewModel.allSupplierInvoices.collectAsStateWithLifecycle()
    val suppliers by viewModel.activeSuppliers.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var showFormDialog by remember { mutableStateOf(false) }
    var selectedInvoice by remember { mutableStateOf<SupplierInvoice?>(null) }
    var invoiceToDelete by remember { mutableStateOf<SupplierInvoice?>(null) }

    val filteredInvoices = invoices.filter { invoice ->
        invoice.invoiceNumber.contains(searchQuery, ignoreCase = true) ||
                invoice.description.contains(searchQuery, ignoreCase = true) ||
                suppliers.any {
                    it.id == invoice.supplierId &&
                            it.name.contains(searchQuery, ignoreCase = true)
                }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {

        ExpensoTopBar(
            title = "Supplier Invoices",
            actionIcon = Icons.Default.Add,
            onActionClick = {
                selectedInvoice = null
                showFormDialog = true
            },
            actionContentDescription = "Add Supplier Invoice",
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
                text = "Manage supplier invoices and bills.",
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
                    Text("Search invoices...")
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

            if (filteredInvoices.isEmpty()) {

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Text(
                        text = if (searchQuery.isBlank()) {
                            "No supplier invoices yet."
                        } else {
                            "No supplier invoices found."
                        },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    if (searchQuery.isBlank()) {
                        Text(
                            text = "Tap + to add your first supplier invoice.",
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
                        items = filteredInvoices,
                        key = { it.id }
                    ) { invoice ->

                        val supplierName =
                            suppliers.firstOrNull {
                                it.id == invoice.supplierId
                            }?.name ?: "Unknown Supplier"

                        SupplierInvoiceCard(
                            invoice = invoice,
                            supplierName = supplierName,
                            onEdit = {
                                selectedInvoice = invoice
                                showFormDialog = true
                            },
                            onDelete = {
                                invoiceToDelete = invoice
                            }
                        )
                    }
                }
            }
        }
    }

    if (showFormDialog) {

        SupplierInvoiceFormDialog(
            invoice = selectedInvoice,
            suppliers = suppliers,
            onDismiss = {
                showFormDialog = false
                selectedInvoice = null
            },
            onSave = { invoice ->

                if (selectedInvoice == null) {

                    viewModel.addSupplierInvoice(
                        invoice = invoice,
                        onSuccess = {
                            showFormDialog = false
                            selectedInvoice = null
                        }
                    )

                } else {

                    viewModel.updateSupplierInvoice(
                        invoice = invoice,
                        onSuccess = {
                            showFormDialog = false
                            selectedInvoice = null
                        }
                    )
                }
            }
        )
    }

    invoiceToDelete?.let { invoice ->

        AlertDialog(
            onDismissRequest = {
                invoiceToDelete = null
            },
            title = {
                Text("Delete Supplier Invoice")
            },
            text = {
                Text(
                    "Are you sure you want to delete invoice ${invoice.invoiceNumber}?"
                )
            },
            confirmButton = {

                Button(
                    onClick = {

                        viewModel.deleteSupplierInvoice(
                            invoice = invoice,
                            onSuccess = {
                                invoiceToDelete = null
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
                        invoiceToDelete = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SupplierInvoiceCard(
    invoice: SupplierInvoice,
    supplierName: String,
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
                        text = invoice.invoiceNumber,
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
                        contentDescription = "Edit Invoice"
                    )
                }

                IconButton(
                    onClick = onDelete
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Invoice"
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Invoice Date: ${invoice.invoiceDate}",
                fontSize = 14.sp
            )

            Text(
                text = "Due Date: ${invoice.dueDate}",
                fontSize = 14.sp
            )

            if (invoice.description.isNotBlank()) {
                Text(
                    text = "Description: ${invoice.description}",
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Subtotal: ${formatAmount(invoice.subtotal)}",
                fontSize = 14.sp
            )

            Text(
                text = "Tax: ${formatAmount(invoice.taxAmount)}",
                fontSize = 14.sp
            )

            Text(
                text = "Total: ${formatAmount(invoice.totalAmount)}",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Paid: ${formatAmount(invoice.amountPaid)}",
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Status: ${invoice.status}",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
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

@Composable
private fun SupplierInvoiceFormDialog(
    invoice: SupplierInvoice?,
    suppliers: List<com.expenso.app.data.Supplier>,
    onDismiss: () -> Unit,
    onSave: (SupplierInvoice) -> Unit
) {
    var supplierId by remember(invoice) {
        mutableStateOf(invoice?.supplierId ?: 0)
    }

    var invoiceNumber by remember(invoice) {
        mutableStateOf(invoice?.invoiceNumber ?: "")
    }

    var invoiceDate by remember(invoice) {
        mutableStateOf(invoice?.invoiceDate ?: "")
    }

    var dueDate by remember(invoice) {
        mutableStateOf(invoice?.dueDate ?: "")
    }

    var description by remember(invoice) {
        mutableStateOf(invoice?.description ?: "")
    }

    var subtotal by remember(invoice) {
        mutableStateOf(
            if (invoice == null) "" else invoice.subtotal.toString()
        )
    }

    var taxAmount by remember(invoice) {
        mutableStateOf(
            if (invoice == null) "" else invoice.taxAmount.toString()
        )
    }

    var amountPaid by remember(invoice) {
        mutableStateOf(
            if (invoice == null) "" else invoice.amountPaid.toString()
        )
    }

    var status by remember(invoice) {
        mutableStateOf(invoice?.status ?: "OPEN")
    }

    var errorMessage by remember {
        mutableStateOf("")
    }

    val calculatedSubtotal =
        subtotal.toDoubleOrNull() ?: 0.0

    val calculatedTax =
        taxAmount.toDoubleOrNull() ?: 0.0

    val calculatedTotal =
        calculatedSubtotal + calculatedTax

    //Alert dialog code starts here
    AlertDialog(
        onDismissRequest = onDismiss,

        title = {
            Text(
                if (invoice == null) {
                    "Add Supplier Invoice"
                } else {
                    "Edit Supplier Invoice"
                }
            )
        },

        text = {

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp)
                    .verticalScroll(rememberScrollState()),

                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {

                // ---------------------------------------------------------
                // SUPPLIER SELECTION
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

                        var supplierMenuExpanded by remember {
                            mutableStateOf(false)
                        }

                        val selectedSupplierName =
                            suppliers
                                .firstOrNull { it.id == supplierId }
                                ?.name
                                ?: "Select supplier"

                        OutlinedButton(
                            onClick = {
                                supplierMenuExpanded = true
                            },

                            modifier = Modifier.fillMaxWidth()
                        ) {

                            Text(
                                text = selectedSupplierName,
                                modifier = Modifier.weight(1f)
                            )

                            Text("▼")
                        }

                        DropdownMenu(
                            expanded = supplierMenuExpanded,

                            onDismissRequest = {
                                supplierMenuExpanded = false
                            },

                            modifier = Modifier.fillMaxWidth()
                        ) {

                            suppliers.forEach { supplier ->

                                DropdownMenuItem(
                                    text = {
                                        Text(supplier.name)
                                    },

                                    onClick = {

                                        supplierId = supplier.id

                                        supplierMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }


                // ---------------------------------------------------------
                // INVOICE NUMBER
                // ---------------------------------------------------------

                OutlinedTextField(
                    value = invoiceNumber,

                    onValueChange = {
                        invoiceNumber = it
                    },

                    label = {
                        Text("Invoice Number")
                    },

                    modifier = Modifier.fillMaxWidth(),

                    singleLine = true
                )


                // ---------------------------------------------------------
                // INVOICE DATE
                // ---------------------------------------------------------

                OutlinedTextField(
                    value = invoiceDate,

                    onValueChange = {
                        invoiceDate = it
                    },

                    label = {
                        Text("Invoice Date")
                    },

                    placeholder = {
                        Text("YYYY-MM-DD")
                    },

                    modifier = Modifier.fillMaxWidth(),

                    singleLine = true
                )


                // ---------------------------------------------------------
                // DUE DATE
                // ---------------------------------------------------------

                OutlinedTextField(
                    value = dueDate,

                    onValueChange = {
                        dueDate = it
                    },

                    label = {
                        Text("Due Date")
                    },

                    placeholder = {
                        Text("YYYY-MM-DD")
                    },

                    modifier = Modifier.fillMaxWidth(),

                    singleLine = true
                )


                // ---------------------------------------------------------
                // DESCRIPTION
                // ---------------------------------------------------------

                OutlinedTextField(
                    value = description,

                    onValueChange = {
                        description = it
                    },

                    label = {
                        Text("Description")
                    },

                    modifier = Modifier.fillMaxWidth(),

                    minLines = 2,

                    maxLines = 3
                )


                // ---------------------------------------------------------
                // SUBTOTAL
                // ---------------------------------------------------------

                OutlinedTextField(
                    value = subtotal,

                    onValueChange = {
                        subtotal = it
                    },

                    label = {
                        Text("Subtotal")
                    },

                    modifier = Modifier.fillMaxWidth(),

                    singleLine = true
                )


                // ---------------------------------------------------------
                // TAX AMOUNT
                // ---------------------------------------------------------

                OutlinedTextField(
                    value = taxAmount,

                    onValueChange = {
                        taxAmount = it
                    },

                    label = {
                        Text("Tax Amount")
                    },

                    modifier = Modifier.fillMaxWidth(),

                    singleLine = true
                )


                // ---------------------------------------------------------
                // TOTAL
                // ---------------------------------------------------------

                OutlinedTextField(
                    value = formatAmount(calculatedTotal),

                    onValueChange = {},

                    readOnly = true,

                    label = {
                        Text("Total")
                    },

                    supportingText = {
                        Text("Subtotal + Tax Amount")
                    },

                    modifier = Modifier.fillMaxWidth(),

                    singleLine = true
                )


                // ---------------------------------------------------------
                // AMOUNT PAID
                // ---------------------------------------------------------

                OutlinedTextField(
                    value = amountPaid,

                    onValueChange = {
                        amountPaid = it
                    },

                    label = {
                        Text("Amount Paid")
                    },

                    modifier = Modifier.fillMaxWidth(),

                    singleLine = true
                )


                // ---------------------------------------------------------
                // STATUS
                // ---------------------------------------------------------

                OutlinedTextField(
                    value = status,

                    onValueChange = {},

                    readOnly = true,

                    label = {
                        Text("Status")
                    },

                    modifier = Modifier.fillMaxWidth(),

                    singleLine = true
                )


                // ---------------------------------------------------------
                // ERROR MESSAGE
                // ---------------------------------------------------------

                if (errorMessage.isNotBlank()) {

                    Text(
                        text = errorMessage,
                        fontSize = 13.sp
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

                        errorMessage = "Please select a supplier."

                        return@Button
                    }

                    if (invoiceNumber.isBlank()) {

                        errorMessage = "Invoice number is required."

                        return@Button
                    }

                    if (invoiceDate.isBlank()) {

                        errorMessage = "Invoice date is required."

                        return@Button
                    }

                    if (dueDate.isBlank()) {

                        errorMessage = "Due date is required."

                        return@Button
                    }

                    if (subtotal.toDoubleOrNull() == null) {

                        errorMessage = "Enter a valid subtotal."

                        return@Button
                    }

                    if (taxAmount.toDoubleOrNull() == null) {

                        errorMessage = "Enter a valid tax amount."

                        return@Button
                    }

                    val initialAmountPaid =
                        amountPaid.toDoubleOrNull() ?: 0.0

                    val now =
                        SimpleDateFormat(
                            "yyyy-MM-dd HH:mm:ss",
                            Locale.getDefault()
                        ).format(Date())

                    val invoiceToSave = SupplierInvoice(

                        id = invoice?.id ?: 0,

                        supplierId = supplierId,

                        invoiceNumber = invoiceNumber.trim(),

                        invoiceDate = invoiceDate.trim(),

                        dueDate = dueDate.trim(),

                        description = description.trim(),

                        subtotal = subtotal.toDouble(),

                        taxAmount = taxAmount.toDouble(),

                        totalAmount = calculatedTotal,

                        amountPaid = initialAmountPaid,

                        status = status.trim().ifBlank {
                            "OPEN"
                        },

                        createdAt =
                            invoice?.createdAt ?: now
                    )

                    onSave(invoiceToSave)
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
    //code ends here
}