package com.expenso.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.AttachMoney
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.expenso.app.data.Customer
import com.expenso.app.data.CustomerInvoice
import com.expenso.app.data.ExpensoViewModel
import com.expenso.app.ui.components.ExpensoTopBar

@Composable
fun CustomerStatementScreen(
    viewModel: ExpensoViewModel,
    onDismiss: () -> Unit
) {
    BackHandler {
        onDismiss()
    }

    val customers by viewModel.allCustomers.collectAsStateWithLifecycle()
    val invoices by viewModel.allCustomerInvoices.collectAsStateWithLifecycle()
    val payments by viewModel.allCustomerPayments.collectAsStateWithLifecycle()

    var selectedCustomer by remember {
        mutableStateOf<Customer?>(null)
    }

    val customerInvoices = selectedCustomer?.let { customer ->
        invoices.filter { it.customerId == customer.id }
    } ?: emptyList()

    val customerPayments = selectedCustomer?.let { customer ->
        payments.filter { it.customerId == customer.id }
    } ?: emptyList()

    val totalInvoiced = customerInvoices.sumOf { it.amount }

    val totalPaid = customerPayments.sumOf { it.amount }

    val balanceDue = (totalInvoiced - totalPaid)
        .coerceAtLeast(0.0)

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {

        /*
         * HEADER + CUSTOMER SELECTION
         */
        item {

            Spacer(
                modifier = Modifier.height(32.dp)
            )

            ExpensoTopBar(
                title = "Customer Statement",
                navigationIcon = Icons.AutoMirrored.Rounded.ArrowBack,
                onNavigationClick = onDismiss,
                navigationContentDescription = "Back"
            )

            Spacer(
                modifier = Modifier.height(16.dp)
            )

            Text(
                text = "Customer Statement",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.height(4.dp)
            )

            Text(
                text = "View invoices, payments and balance due.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(
                modifier = Modifier.height(20.dp)
            )

            Text(
                text = "Select Customer",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            CustomerStatementCustomerSelector(
                customers = customers,
                selectedCustomer = selectedCustomer,
                onCustomerSelected = {
                    selectedCustomer = it
                }
            )

            Spacer(
                modifier = Modifier.height(16.dp)
            )
        }

        /*
         * NO CUSTOMER SELECTED
         */
        if (selectedCustomer == null) {

            item {

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {

                    Text(
                        text = "Select a customer to view their statement.",
                        modifier = Modifier.padding(20.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Spacer(
                    modifier = Modifier.height(20.dp)
                )
            }

        } else {

            /*
             * CUSTOMER SUMMARY
             */
            item {

                Text(
                    text = selectedCustomer!!.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(
                    modifier = Modifier.height(12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {

                    CustomerStatementSummaryCard(
                        modifier = Modifier.weight(1f),
                        title = "Invoiced",
                        amount = totalInvoiced,
                        icon = Icons.Rounded.ReceiptLong
                    )

                    CustomerStatementSummaryCard(
                        modifier = Modifier.weight(1f),
                        title = "Paid",
                        amount = totalPaid,
                        icon = Icons.Rounded.AttachMoney
                    )
                }

                Spacer(
                    modifier = Modifier.height(12.dp)
                )

                CustomerStatementSummaryCard(
                    modifier = Modifier.fillMaxWidth(),
                    title = "Balance Due",
                    amount = balanceDue,
                    icon = Icons.Rounded.AccountBalanceWallet
                )

                Spacer(
                    modifier = Modifier.height(20.dp)
                )

                Text(
                    text = "Invoices",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(
                    modifier = Modifier.height(8.dp)
                )
            }

            /*
             * CUSTOMER INVOICES
             */
            items(customerInvoices) { invoice ->

                CustomerStatementInvoiceCard(
                    invoice = invoice
                )
            }

            item {

                Spacer(
                    modifier = Modifier.height(20.dp)
                )
            }
        }
    }
}


/*
 * CUSTOMER SELECTOR
 */
@Composable
private fun CustomerStatementCustomerSelector(
    customers: List<Customer>,
    selectedCustomer: Customer?,
    onCustomerSelected: (Customer) -> Unit
) {
    var expanded by remember {
        mutableStateOf(false)
    }

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {

        OutlinedButton(
            onClick = {
                expanded = true
            },
            modifier = Modifier.fillMaxWidth()
        ) {

            Text(
                text = selectedCustomer?.let {
                    "${it.customerCode} - ${it.name}"
                } ?: "Select Customer"
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = {
                expanded = false
            }
        ) {

            customers.forEach { customer ->

                DropdownMenuItem(
                    text = {
                        Text(
                            text = "${customer.customerCode} - ${customer.name}"
                        )
                    },
                    onClick = {
                        onCustomerSelected(customer)
                        expanded = false
                    }
                )
            }
        }
    }
}


/*
 * SUMMARY CARD
 */
@Composable
private fun CustomerStatementSummaryCard(
    modifier: Modifier = Modifier,
    title: String,
    amount: Double,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {

        Column(
            modifier = Modifier.padding(16.dp)
        ) {

            Icon(
                imageVector = icon,
                contentDescription = title
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(
                modifier = Modifier.height(4.dp)
            )

            Text(
                text = "GH₵ ${"%,.2f".format(amount)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}


/*
 * INVOICE CARD
 */
@Composable
private fun CustomerStatementInvoiceCard(
    invoice: CustomerInvoice
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {

        Column(
            modifier = Modifier.padding(16.dp)
        ) {

            Text(
                text = invoice.invoiceNumber,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.height(4.dp)
            )

            Text(
                text = invoice.description.ifBlank {
                    "Customer Invoice"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {

                Text(
                    text = "Date: ${invoice.invoiceDate}"
                )

                Text(
                    text = "GH₵ ${"%,.2f".format(invoice.amount)}",
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(
                modifier = Modifier.height(6.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {

                Text(
                    text = "Paid: GH₵ ${"%,.2f".format(invoice.amountPaid)}"
                )

                Text(
                    text = invoice.status,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(
                modifier = Modifier.height(6.dp)
            )

            Text(
                text = "Balance: GH₵ ${
                    "%,.2f".format(
                        (invoice.amount - invoice.amountPaid)
                            .coerceAtLeast(0.0)
                    )
                }",
                fontWeight = FontWeight.Bold
            )
        }
    }
}