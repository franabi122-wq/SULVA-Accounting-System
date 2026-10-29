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
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import com.expenso.app.data.ChartOfAccount
import com.expenso.app.data.Customer
import com.expenso.app.data.CustomerInvoice
import com.expenso.app.data.CustomerPayment
import com.expenso.app.data.ExpensoViewModel
import com.expenso.app.ui.components.ExpensoTopBar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CustomerPaymentManagementScreen(
    viewModel: ExpensoViewModel,
    onDismiss: () -> Unit
) {
    BackHandler {
        onDismiss()
    }

    val payments by viewModel.allCustomerPayments.collectAsStateWithLifecycle()
    val customers by viewModel.allCustomers.collectAsStateWithLifecycle()
    val invoices by viewModel.allCustomerInvoices.collectAsStateWithLifecycle()
    val accounts by viewModel.allChartOfAccounts.collectAsStateWithLifecycle()

    var showFormDialog by remember {
        mutableStateOf(false)
    }

    var paymentToEdit by remember {
        mutableStateOf<CustomerPayment?>(null)
    }

    var paymentToDelete by remember {
        mutableStateOf<CustomerPayment?>(null)
    }



    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {

        Spacer(modifier = Modifier.height(32.dp))

        ExpensoTopBar(
            title = "Customer Payments",
            navigationIcon = Icons.AutoMirrored.Rounded.ArrowBack,
            onNavigationClick = onDismiss,
            navigationContentDescription = "Back",
            actionIcon = Icons.Rounded.Add,
            onActionClick = {
                paymentToEdit = null
                showFormDialog = true
            },
            actionContentDescription = "Add payment"
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Customer Payments",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Record payments received from customers.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(20.dp))

        if (payments.isEmpty()) {

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 60.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Icon(
                    imageVector = Icons.Rounded.Payments,
                    contentDescription = null,
                    modifier = Modifier.height(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "No customer payments yet",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Record your first customer payment.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        paymentToEdit = null
                        showFormDialog = true
                    }
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = null
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text("Add Payment")
                }
            }

        } else {

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                items(
                    items = payments
                ) { payment ->

                    val customer = customers.find {
                        it.id == payment.customerId
                    }

                    val invoice = invoices.find {
                        it.id == payment.invoiceId
                    }

                    val account = accounts.find {
                        it.id == payment.accountId
                    }

                    CustomerPaymentCard(
                        payment = payment,
                        customer = customer,
                        invoice = invoice,
                        account = account,
                        onEdit = {
                            paymentToEdit = payment
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

    //code starts here
    if (showFormDialog) {

        CustomerPaymentFormDialog(
            viewModel = viewModel,
            customers = customers,
            invoices = invoices,
            accounts = accounts,
            payment = paymentToEdit,
            onDismiss = {
                showFormDialog = false
                paymentToEdit = null
            },
            onSave = { payment ->

                if (paymentToEdit == null) {

                    viewModel.addCustomerPayment(
                        payment = payment,
                        onSuccess = {
                            showFormDialog = false
                            paymentToEdit = null
                        }
                    )

                } else {

                    viewModel.updateCustomerPayment(
                        payment = payment,
                        onSuccess = {
                            showFormDialog = false
                            paymentToEdit = null
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
                Text("Delete Payment")
            },
            text = {
                Text(
                    "Are you sure you want to delete this customer payment?"
                )
            },
            confirmButton = {

                TextButton(
                    onClick = {

                        viewModel.deleteCustomerPayment(
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

                TextButton(
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
private fun CustomerPaymentCard(
    payment: CustomerPayment,
    customer: Customer?,
    invoice: CustomerInvoice?,
    account: ChartOfAccount?,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {

    val balance = invoice?.let {
        (it.amount - it.amountPaid).coerceAtLeast(0.0)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {

        Column(
            modifier = Modifier.padding(16.dp)
        ) {

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {

                Icon(
                    imageVector = Icons.Rounded.Payments,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {

                    Text(
                        text = "GH₵ ${"%,.2f".format(payment.amount)}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = customer?.name ?: "Unknown Customer",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                IconButton(
                    onClick = onEdit
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = "Edit payment"
                    )
                }

                IconButton(
                    onClick = onDelete
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = "Delete payment"
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Invoice: ${invoice?.invoiceNumber ?: "Unknown"}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )

            Text(
                text = "Date: ${payment.paymentDate}",
                style = MaterialTheme.typography.bodySmall
            )

            Text(
                text = "Account: ${account?.accountName ?: "Unknown"}",
                style = MaterialTheme.typography.bodySmall
            )

            if (payment.referenceNumber.isNotBlank()) {

                Text(
                    text = "Reference: ${payment.referenceNumber}",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            if (payment.notes.isNotBlank()) {

                Text(
                    text = payment.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (invoice != null) {

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Invoice Balance: GH₵ ${"%,.2f".format(balance ?: 0.0)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Status: ${invoice.status}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}


//code for CustomerPaymentFormDialog() starts here
@Composable
private fun CustomerPaymentFormDialog(
    viewModel: ExpensoViewModel,
    customers: List<Customer>,
    invoices: List<CustomerInvoice>,
    accounts: List<ChartOfAccount>,
    payment: CustomerPayment?,
    onDismiss: () -> Unit,
    onSave: (CustomerPayment) -> Unit
) {

    var selectedCustomerId by remember(payment) {
        mutableStateOf(payment?.customerId ?: 0)
    }

    var selectedInvoiceId by remember(payment) {
        mutableStateOf(payment?.invoiceId ?: 0)
    }

    var selectedAccountId by remember(payment) {
        mutableStateOf(payment?.accountId ?: 0)
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
            if (payment != null) {
                payment.amount.toString()
            } else {
                ""
            }
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

    var errorMessage by remember {
        mutableStateOf("")
    }

    var customerDropdownExpanded by remember {
        mutableStateOf(false)
    }

    var invoiceDropdownExpanded by remember {
        mutableStateOf(false)
    }

    var accountDropdownExpanded by remember {
        mutableStateOf(false)
    }

    val activeCustomers = customers.filter {
        it.isActive
    }

    val outstandingInvoices = invoices.filter {
        it.status.uppercase() in
                setOf("OPEN", "PARTIALLY_PAID", "OVERDUE")
    }

    val assetAccounts = accounts.filter {
        it.isActive &&
                it.accountType.uppercase() == "ASSET"
    }

    val customerInvoices = if (selectedCustomerId == 0) {
        emptyList()
    } else {
        outstandingInvoices.filter {
            it.customerId == selectedCustomerId
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (payment == null) {
                    "Add Customer Payment"
                } else {
                    "Edit Customer Payment"
                }
            )
        },
        //code ends  here


        //LazyColumn code here
        text = {

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {

                // Customer code starts here
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
                //customer code ends here


                // Invoice
                Spacer(modifier = Modifier.height(4.dp))

                val selectedInvoice =
                    customerInvoices.find {
                        it.id == selectedInvoiceId
                    }

                Box(
                    modifier = Modifier.fillMaxWidth()
                ) {

                    OutlinedTextField(
                        value = selectedInvoice?.invoiceNumber ?: "",
                        onValueChange = {},
                        modifier = Modifier.fillMaxWidth(),
                        readOnly = true,
                        singleLine = true,
                        label = {
                            Text("Invoice")
                        },
                        placeholder = {
                            Text("Choose an invoice")
                        },
                        trailingIcon = {
                            Icon(
                                imageVector =
                                    if (invoiceDropdownExpanded) {
                                        Icons.Rounded.KeyboardArrowUp
                                    } else {
                                        Icons.Rounded.KeyboardArrowDown
                                    },
                                contentDescription = "Select invoice"
                            )
                        }
                    )

                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable {

                                if (customerInvoices.isNotEmpty()) {
                                    invoiceDropdownExpanded =
                                        !invoiceDropdownExpanded
                                }
                            }
                    )

                    DropdownMenu(
                        expanded = invoiceDropdownExpanded,
                        onDismissRequest = {
                            invoiceDropdownExpanded = false
                        }
                    ) {

                        customerInvoices.forEach { invoice ->

                            val balance =
                                (invoice.amount - invoice.amountPaid)
                                    .coerceAtLeast(0.0)

                            DropdownMenuItem(
                                text = {
                                    Column {

                                        Text(
                                            text = invoice.invoiceNumber,
                                            fontWeight = FontWeight.Medium
                                        )

                                        Text(
                                            text =
                                                "Balance: GH₵ ${"%,.2f".format(balance)}",
                                            style =
                                                MaterialTheme.typography.bodySmall,
                                            color =
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                onClick = {

                                    selectedInvoiceId =
                                        invoice.id

                                    invoiceDropdownExpanded =
                                        false
                                }
                            )
                        }
                    }
                }


                // Payment Date
                OutlinedTextField(
                    value = paymentDate,
                    onValueChange = {
                        paymentDate = it
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = {
                        Text("Payment Date")
                    },
                    placeholder = {
                        Text("2026-09-02")
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
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal
                    ),
                    label = {
                        Text("Amount (GH₵)")
                    },
                    placeholder = {
                        Text("200.00")
                    }
                )


                // Receiving Account
                val selectedAccount =
                    assetAccounts.find {
                        it.id == selectedAccountId
                    }

                Box(
                    modifier = Modifier.fillMaxWidth()
                ) {

                    OutlinedTextField(
                        value = selectedAccount?.accountName ?: "",
                        onValueChange = {},
                        modifier = Modifier.fillMaxWidth(),
                        readOnly = true,
                        singleLine = true,
                        label = {
                            Text("Receiving Account")
                        },
                        placeholder = {
                            Text("Choose a receiving account")
                        },
                        trailingIcon = {
                            Icon(
                                imageVector =
                                    if (accountDropdownExpanded) {
                                        Icons.Rounded.KeyboardArrowUp
                                    } else {
                                        Icons.Rounded.KeyboardArrowDown
                                    },
                                contentDescription =
                                    "Select receiving account"
                            )
                        }
                    )

                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable {

                                if (assetAccounts.isNotEmpty()) {
                                    accountDropdownExpanded =
                                        !accountDropdownExpanded
                                }
                            }
                    )

                    DropdownMenu(
                        expanded = accountDropdownExpanded,
                        onDismissRequest = {
                            accountDropdownExpanded = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {

                        assetAccounts.forEach { account ->

                            DropdownMenuItem(
                                text = {

                                    Column {

                                        Text(
                                            text = account.accountName,
                                            fontWeight = FontWeight.Medium
                                        )

                                        Text(
                                            text = account.accountCode,
                                            style =
                                                MaterialTheme.typography.bodySmall,
                                            color =
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                onClick = {

                                    selectedAccountId =
                                        account.id

                                    accountDropdownExpanded =
                                        false
                                }
                            )
                        }
                    }
                }


                // Reference Number
                OutlinedTextField(
                    value = referenceNumber,
                    onValueChange = {
                        referenceNumber = it
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = {
                        Text("Reference Number")
                    },
                    placeholder = {
                        Text("PAY-001")
                    }
                )


                // Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = {
                        notes = it
                    },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    label = {
                        Text("Notes")
                    },
                    placeholder = {
                        Text("Customer payment")
                    }
                )


                // Error
                if (errorMessage.isNotBlank()) {

                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        //end of text block code

        confirmButton = {

            Button(
                onClick = {

                    if (selectedCustomerId == 0) {

                        errorMessage =
                            "Please select a customer."

                        return@Button
                    }

                    if (selectedInvoiceId == 0) {

                        errorMessage =
                            "Please select an invoice."

                        return@Button
                    }

                    if (selectedAccountId == 0) {

                        errorMessage =
                            "Please select a receiving account."

                        return@Button
                    }

                    if (paymentDate.isBlank()) {

                        errorMessage =
                            "Payment date is required."

                        return@Button
                    }

                    val amountValue =
                        amount.toDoubleOrNull()

                    if (
                        amountValue == null ||
                        amountValue <= 0.0
                    ) {

                        errorMessage =
                            "Enter a valid payment amount."

                        return@Button
                    }

                    val selectedInvoice =
                        invoices.find {
                            it.id == selectedInvoiceId
                        }

                    if (selectedInvoice == null) {

                        errorMessage =
                            "Selected invoice was not found."

                        return@Button
                    }

                    val invoiceBalance =
                        (
                                selectedInvoice.amount -
                                        selectedInvoice.amountPaid
                                ).coerceAtLeast(0.0)

                    if (
                        payment == null &&
                        amountValue > invoiceBalance + 0.01
                    ) {

                        errorMessage =
                            "Payment exceeds the invoice balance."

                        return@Button
                    }

                    val now =
                        SimpleDateFormat(
                            "yyyy-MM-dd HH:mm:ss",
                            Locale.getDefault()
                        ).format(Date())

                    val paymentToSave =
                        CustomerPayment(
                            id = payment?.id ?: 0,

                            invoiceId =
                                selectedInvoiceId,

                            customerId =
                                selectedCustomerId,

                            paymentDate =
                                paymentDate.trim(),

                            amount =
                                amountValue,

                            accountId =
                                selectedAccountId,

                            referenceNumber =
                                referenceNumber.trim(),

                            notes =
                                notes.trim(),

                            journalEntryId =
                                payment?.journalEntryId,

                            createdAt =
                                payment?.createdAt ?: now
                        )

                    onSave(paymentToSave)
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
//code for CustomerPaymentFormDialog() ends here
