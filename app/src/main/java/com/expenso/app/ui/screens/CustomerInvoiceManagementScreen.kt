package com.expenso.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.expenso.app.data.Customer
import com.expenso.app.data.CustomerInvoice
import com.expenso.app.data.ExpensoViewModel
import com.expenso.app.ui.components.ExpensoTopBar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CustomerInvoiceManagementScreen(
    viewModel: ExpensoViewModel,
    onDismiss: () -> Unit
) {
    BackHandler {
        onDismiss()
    }

    val customers by viewModel.allCustomers.collectAsStateWithLifecycle()
    val activeCustomers by viewModel.activeCustomers.collectAsStateWithLifecycle()
    val invoices by viewModel.allCustomerInvoices.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var showInvoiceForm by remember { mutableStateOf(false) }
    var invoiceToEdit by remember { mutableStateOf<CustomerInvoice?>(null) }
    var invoiceToDelete by remember { mutableStateOf<CustomerInvoice?>(null) }

    val filteredInvoices = invoices.filter { invoice ->
        val query = searchQuery.trim()

        if (query.isEmpty()) {
            true
        } else {
            invoice.invoiceNumber.contains(query, ignoreCase = true) ||
                    invoice.description.contains(query, ignoreCase = true) ||
                    customers.firstOrNull {
                        it.id == invoice.customerId
                    }?.name?.contains(query, ignoreCase = true) == true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {

        Spacer(modifier = Modifier.height(32.dp))

        ExpensoTopBar(
            title = "Customer Invoices",
            navigationIcon = Icons.AutoMirrored.Rounded.ArrowBack,
            onNavigationClick = onDismiss,
            navigationContentDescription = "Back"
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text = "Customer Invoices",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Manage invoices owed by customers.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(
                onClick = {
                    invoiceToEdit = null
                    showInvoiceForm = true
                }
            ) {

                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = "Add invoice"
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = {
                searchQuery = it
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            leadingIcon = {

                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = "Search"
                )
            },
            placeholder = {
                Text("Search invoices...")
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (filteredInvoices.isEmpty()) {

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Icon(
                    imageVector = Icons.Rounded.ReceiptLong,
                    contentDescription = null,
                    modifier = Modifier
                        .height(64.dp)
                        .width(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = if (searchQuery.isBlank()) {
                        "No invoices yet"
                    } else {
                        "No invoices found"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (searchQuery.isBlank()) {
                        "Tap + to create your first invoice."
                    } else {
                        "Try a different search."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (searchQuery.isBlank()) {

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            invoiceToEdit = null
                            showInvoiceForm = true
                        }
                    ) {

                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = null
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text("Create Invoice")
                    }
                }
            }

        } else {

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                items(
                    items = filteredInvoices
                ) { invoice ->

                    val customer = customers.firstOrNull {
                        it.id == invoice.customerId
                    }

                    CustomerInvoiceCard(
                        invoice = invoice,
                        customer = customer,
                        onEdit = {
                            invoiceToEdit = invoice
                            showInvoiceForm = true
                        },
                        onDelete = {
                            invoiceToDelete = invoice
                        }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }

    // ============================================================
    // ADD / EDIT INVOICE DIALOG
    // ============================================================

    if (showInvoiceForm) {

        CustomerInvoiceFormDialog(
            invoice = invoiceToEdit,
            customers = activeCustomers,

            onDismiss = {
                showInvoiceForm = false
                invoiceToEdit = null
            },

            onSave = { invoiceToSave ->

                if (invoiceToSave.id == 0) {

                    viewModel.addCustomerInvoice(
                        invoice = invoiceToSave
                    )

                } else {

                    viewModel.updateCustomerInvoice(
                        invoice = invoiceToSave
                    )
                }

                showInvoiceForm = false
                invoiceToEdit = null
            }
        )
    }

    // ============================================================
    // DELETE INVOICE DIALOG
    // ============================================================

    invoiceToDelete?.let { invoice ->

        AlertDialog(
            onDismissRequest = {
                invoiceToDelete = null
            },

            title = {
                Text("Delete Invoice")
            },

            text = {
                Text(
                    "Are you sure you want to delete invoice " +
                            "${invoice.invoiceNumber}?"
                )
            },

            confirmButton = {

                TextButton(
                    onClick = {

                        viewModel.deleteCustomerInvoice(
                            invoice = invoice
                        )

                        invoiceToDelete = null
                    }
                ) {
                    Text("Delete")
                }
            },

            dismissButton = {

                TextButton(
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


// ============================================================
// CUSTOMER INVOICE CARD
// ============================================================

@Composable
private fun CustomerInvoiceCard(
    invoice: CustomerInvoice,
    customer: Customer?,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {

    val balance = (
            invoice.amount - invoice.amountPaid
            ).coerceAtLeast(0.0)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
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
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = customer?.name ?: "Unknown Customer",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(
                    onClick = onEdit
                ) {

                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = "Edit invoice"
                    )
                }

                IconButton(
                    onClick = onDelete
                ) {

                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = "Delete invoice"
                    )
                }
            }

            if (invoice.description.isNotBlank()) {

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = invoice.description,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Invoice Date: ${invoice.invoiceDate}",
                style = MaterialTheme.typography.bodySmall
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Due Date: ${invoice.dueDate}",
                style = MaterialTheme.typography.bodySmall
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Amount: GH₵ ${"%,.2f".format(invoice.amount)}",
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Paid: GH₵ ${"%,.2f".format(invoice.amountPaid)}"
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Balance: GH₵ ${"%,.2f".format(balance)}",
                fontWeight = FontWeight.Bold,
                color = if (balance > 0.0) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.primary
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Status: ${invoice.status}",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}


// ============================================================
// CUSTOMER INVOICE FORM DIALOG
// ============================================================

@Composable
private fun CustomerInvoiceFormDialog(
    invoice: CustomerInvoice?,
    customers: List<Customer>,
    onDismiss: () -> Unit,
    onSave: (CustomerInvoice) -> Unit
) {

    var invoiceNumber by remember(invoice) {
        mutableStateOf(
            invoice?.invoiceNumber ?: ""
        )
    }

    var selectedCustomerId by remember(invoice) {
        mutableStateOf(
            invoice?.customerId ?: 0
        )
    }

    var invoiceDate by remember(invoice) {
        mutableStateOf(
            invoice?.invoiceDate
                ?: SimpleDateFormat(
                    "yyyy-MM-dd",
                    Locale.getDefault()
                ).format(Date())
        )
    }

    var dueDate by remember(invoice) {
        mutableStateOf(
            invoice?.dueDate ?: ""
        )
    }

    var description by remember(invoice) {
        mutableStateOf(
            invoice?.description ?: ""
        )
    }

    var amount by remember(invoice) {
        mutableStateOf(
            invoice?.amount?.toString() ?: ""
        )
    }

    var errorMessage by remember(invoice) {
        mutableStateOf("")
    }

    var customerDropdownExpanded by remember {
        mutableStateOf(false)
    }

    AlertDialog(
        onDismissRequest = onDismiss,

        title = {

            Text(
                text = if (invoice == null) {
                    "Create Customer Invoice"
                } else {
                    "Edit Customer Invoice"
                }
            )
        },

        //code starts here
        text = {

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {

                OutlinedTextField(
                    value = invoiceNumber,
                    onValueChange = {
                        invoiceNumber = it
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = {
                        Text("Invoice Number")
                    },
                    placeholder = {
                        Text("INV-001")
                    }
                )

                // Customer section starts here
                val selectedCustomer =
                    customers.find {
                        it.id == selectedCustomerId
                    }

                Box(
                    modifier = Modifier.fillMaxWidth()
                ) {

                    OutlinedTextField(
                        value = selectedCustomer?.name ?: "",
                        onValueChange = {},
                        modifier = Modifier.fillMaxWidth(),
                        readOnly = true,
                        singleLine = true,
                        label = {
                            Text("Customer")
                        },
                        placeholder = {
                            Text("Choose a customer")
                        },
                        trailingIcon = {
                            Icon(
                                imageVector =
                                    if (customerDropdownExpanded) {
                                        Icons.Rounded.KeyboardArrowUp
                                    } else {
                                        Icons.Rounded.KeyboardArrowDown
                                    },
                                contentDescription =
                                    "Select customer"
                            )
                        }
                    )

                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable {

                                if (customers.isNotEmpty()) {
                                    customerDropdownExpanded =
                                        !customerDropdownExpanded
                                }
                            }
                    )

                    DropdownMenu(
                        expanded = customerDropdownExpanded,
                        onDismissRequest = {
                            customerDropdownExpanded = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {

                        customers.forEach { customer ->

                            DropdownMenuItem(
                                text = {

                                    Column {

                                        Text(
                                            text = customer.name,
                                            fontWeight = FontWeight.Medium
                                        )

                                        Text(
                                            text = customer.customerCode,
                                            style =
                                                MaterialTheme.typography.bodySmall,
                                            color =
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                onClick = {

                                    selectedCustomerId =
                                        customer.id

                                    customerDropdownExpanded =
                                        false
                                }
                            )
                        }
                    }
                }
                //code ends here

                // Invoice Date
                OutlinedTextField(
                    value = invoiceDate,
                    onValueChange = {
                        invoiceDate = it
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = {
                        Text("Invoice Date")
                    }
                )

                // Due Date
                OutlinedTextField(
                    value = dueDate,
                    onValueChange = {
                        dueDate = it
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = {
                        Text("Due Date")
                    }
                )

                // Description
                OutlinedTextField(
                    value = description,
                    onValueChange = {
                        description = it
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text("Description")
                    }
                )

                // Amount
                OutlinedTextField(
                    value = amount,
                    onValueChange = {
                        amount = it
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = {
                        Text("Amount")
                    }
                )

                if (errorMessage.isNotBlank()) {

                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            //outlined textmode starts here
        },

        confirmButton = {

            Button(
                onClick = {

                    if (invoiceNumber.isBlank()) {

                        errorMessage =
                            "Invoice number is required."

                        return@Button
                    }

                    if (selectedCustomerId == 0) {

                        errorMessage =
                            "Please select a customer."

                        return@Button
                    }

                    if (invoiceDate.isBlank()) {

                        errorMessage =
                            "Invoice date is required."

                        return@Button
                    }

                    if (dueDate.isBlank()) {

                        errorMessage =
                            "Due date is required."

                        return@Button
                    }

                    val amountValue =
                        amount.toDoubleOrNull()

                    if (
                        amountValue == null ||
                        amountValue <= 0.0
                    ) {

                        errorMessage =
                            "Enter a valid invoice amount."

                        return@Button
                    }

                    val now =
                        SimpleDateFormat(
                            "yyyy-MM-dd HH:mm:ss",
                            Locale.getDefault()
                        ).format(Date())

                    val invoiceToSave =
                        CustomerInvoice(

                            id = invoice?.id ?: 0,

                            customerId =
                                selectedCustomerId,

                            invoiceNumber =
                                invoiceNumber.trim(),

                            invoiceDate =
                                invoiceDate.trim(),

                            dueDate =
                                dueDate.trim(),

                            description =
                                description.trim(),

                            amount =
                                amountValue,

                            amountPaid =
                                invoice?.amountPaid ?: 0.0,

                            status =
                                invoice?.status ?: "OPEN",

                            journalEntryId =
                                invoice?.journalEntryId,

                            createdAt =
                                invoice?.createdAt ?: now
                        )

                    onSave(invoiceToSave)
                }
            ) {

                Text("Save")
            }
        },

        dismissButton = {

            OutlinedButton(
                onClick = onDismiss
            ) {

                Text("Cancel")
            }
        }
    )
}