package com.expenso.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Assessment
import androidx.compose.material.icons.rounded.AttachMoney
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.expenso.app.data.CustomerInvoice
import com.expenso.app.data.ExpensoViewModel
import com.expenso.app.ui.components.ExpensoTopBar

@Composable
fun AccountsReceivableScreen(
    viewModel: ExpensoViewModel,
    onDismiss: () -> Unit,
    onNavigateToCustomers: () -> Unit,
    onNavigateToInvoices: () -> Unit,
    onNavigateToPayments: () -> Unit,
    onNavigateToStatement: () -> Unit,
    onNavigateToAgingReport: () -> Unit
) {
    BackHandler {
        onDismiss()
    }

    val customers by viewModel.allCustomers
        .collectAsStateWithLifecycle()

    val invoices by viewModel.allCustomerInvoices
        .collectAsStateWithLifecycle()

    val outstandingInvoices by viewModel.outstandingCustomerInvoices
        .collectAsStateWithLifecycle()

    val payments by viewModel.allCustomerPayments
        .collectAsStateWithLifecycle()

    val totalInvoiced = invoices.sumOf { it.amount }

    val totalPaid = payments.sumOf { it.amount }

    val totalOutstanding = outstandingInvoices.sumOf {
        (it.amount - it.amountPaid).coerceAtLeast(0.0)
    }

    val totalOverdue = outstandingInvoices
        .filter { invoice ->
            invoice.status.equals("OVERDUE", ignoreCase = true)
        }
        .sumOf {
            (it.amount - it.amountPaid).coerceAtLeast(0.0)
        }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {

        ExpensoTopBar(
            title = "Accounts Receivable",
            navigationIcon = Icons.Rounded.ArrowBack,
            onNavigationClick = onDismiss,
            navigationContentDescription = "Back"
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            item {
                Spacer(modifier = Modifier.height(4.dp))
            }

            // ============================================================
            // AR SUMMARY
            // ============================================================

            item {
                Text(
                    text = "Receivables Overview",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {

                    ARSummaryCard(
                        modifier = Modifier.weight(1f),
                        title = "Invoiced",
                        amount = totalInvoiced,
                        icon = Icons.Rounded.Description
                    )

                    ARSummaryCard(
                        modifier = Modifier.weight(1f),
                        title = "Collected",
                        amount = totalPaid,
                        icon = Icons.Rounded.AttachMoney
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {

                    ARSummaryCard(
                        modifier = Modifier.weight(1f),
                        title = "Outstanding",
                        amount = totalOutstanding,
                        icon = Icons.Rounded.AccountBalanceWallet
                    )

                    ARSummaryCard(
                        modifier = Modifier.weight(1f),
                        title = "Overdue",
                        amount = totalOverdue,
                        icon = Icons.Rounded.Assessment
                    )
                }
            }

            // ============================================================
            // CUSTOMERS
            // ============================================================

            item {
                ARInfoCard(
                    title = "Customers",
                    description = "${customers.size} customer(s) registered",
                    icon = Icons.Rounded.People,
                    onClick = onNavigateToCustomers
                )
            }

            // ============================================================
            // CUSTOMER INVOICES
            // ============================================================

            item {
                ARInfoCard(
                    title = "Customer Invoices",
                    description = "${invoices.size} invoice(s) recorded",
                    icon = Icons.Rounded.Description,
                    onClick = onNavigateToInvoices
                )
            }

            // ============================================================
            // CUSTOMER PAYMENTS
            // ============================================================

            item {
                ARInfoCard(
                    title = "Customer Payments",
                    description = "${payments.size} payment(s) recorded",
                    icon = Icons.Rounded.AttachMoney,
                    onClick = onNavigateToPayments
                )
            }

            // ============================================================
            // CUSTOMER STATEMENTS
            // ============================================================

            item {
                ARInfoCard(
                    title = "Customer Statements",
                    description = "View customer transaction statements",
                    icon = Icons.Rounded.ReceiptLong,
                    onClick = onNavigateToStatement
                )
            }

            // ============================================================
            // AR AGING REPORT
            // ============================================================

            item {
                ARInfoCard(
                    title = "AR Aging Report",
                    description = "Analyze outstanding balances by age",
                    icon = Icons.Rounded.Assessment,
                    onClick = onNavigateToAgingReport
                )
            }

            // ============================================================
            // OUTSTANDING INVOICES
            // ============================================================

            item {
                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Outstanding Invoices",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            if (outstandingInvoices.isEmpty()) {

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {

                            Icon(
                                imageVector = Icons.Rounded.AccountBalanceWallet,
                                contentDescription = "No outstanding invoices"
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "No outstanding invoices",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "All customer invoices are currently paid.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

            } else {

                items(
                    items = outstandingInvoices,
                    key = { it.id }
                ) { invoice ->

                    OutstandingInvoiceCard(
                        invoice = invoice,
                        customerName = customers
                            .firstOrNull { it.id == invoice.customerId }
                            ?.name
                            ?: "Unknown Customer"
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

// ============================================================
// AR SUMMARY CARD
// ============================================================

@Composable
private fun ARSummaryCard(
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {

            Icon(
                imageVector = icon,
                contentDescription = title
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = formatARMoney(amount),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// ============================================================
// AR NAVIGATION CARD
// ============================================================

@Composable
private fun ARInfoCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Icon(
                imageVector = icon,
                contentDescription = title
            )

            Spacer(modifier = Modifier.padding(start = 14.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ============================================================
// OUTSTANDING INVOICE CARD
// ============================================================

@Composable
private fun OutstandingInvoiceCard(
    invoice: CustomerInvoice,
    customerName: String
) {
    val balance = (invoice.amount - invoice.amountPaid)
        .coerceAtLeast(0.0)

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
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {

                Column(
                    modifier = Modifier.weight(1f)
                ) {

                    Text(
                        text = invoice.invoiceNumber,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = customerName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = formatARMoney(balance),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            InvoiceDetailRow(
                label = "Invoice Date",
                value = invoice.invoiceDate
            )

            InvoiceDetailRow(
                label = "Due Date",
                value = invoice.dueDate
            )

            InvoiceDetailRow(
                label = "Status",
                value = invoice.status
            )

            InvoiceDetailRow(
                label = "Original Amount",
                value = formatARMoney(invoice.amount)
            )

            InvoiceDetailRow(
                label = "Paid",
                value = formatARMoney(invoice.amountPaid)
            )

            InvoiceDetailRow(
                label = "Balance",
                value = formatARMoney(balance)
            )
        }
    }
}

// ============================================================
// INVOICE DETAIL ROW
// ============================================================

@Composable
private fun InvoiceDetailRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {

        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium
        )
    }
}

// ============================================================
// MONEY FORMAT
// ============================================================

private fun formatARMoney(amount: Double): String {
    return "GH₵ ${"%,.2f".format(amount)}"
}