package com.expenso.app.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Assessment
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.expenso.app.data.Customer
import com.expenso.app.data.CustomerInvoice
import com.expenso.app.data.CustomerPayment
import com.expenso.app.data.ExpensoViewModel
import com.expenso.app.ui.components.ExpensoTopBar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ARAgingReportScreen(
    viewModel: ExpensoViewModel,
    onDismiss: () -> Unit,
    onExportPdf: (String) -> Unit
) {
    BackHandler {
        onDismiss()
    }

    val invoices by viewModel.allCustomerInvoices
        .collectAsStateWithLifecycle()

    val customers by viewModel.allCustomers
        .collectAsStateWithLifecycle()

    val payments by viewModel.allCustomerPayments
        .collectAsStateWithLifecycle()

    val today = remember {
        SimpleDateFormat(
            "yyyy-MM-dd",
            Locale.getDefault()
        ).format(Date())
    }

    var asOfDate by remember {
        mutableStateOf(today)
    }

    var searchQuery by remember {
        mutableStateOf("")
    }

    /*
     * --------------------------------------------------------
     * BUILD AGING ROWS
     * --------------------------------------------------------
     */

    val agingRows = remember(
        invoices,
        customers,
        payments,
        asOfDate,
        searchQuery
    ) {
        invoices.mapNotNull { invoice ->

            // Ignore invoices created after the selected date.
            if (invoice.invoiceDate > asOfDate) {
                return@mapNotNull null
            }

            val customer = customers.firstOrNull {
                it.id == invoice.customerId
            } ?: return@mapNotNull null

            /*
             * Only payments made on or before the
             * selected as-of date are counted.
             */
            val historicalPaid = payments
                .filter { payment ->
                    payment.invoiceId == invoice.id &&
                            payment.paymentDate <= asOfDate
                }
                .sumOf { payment ->
                    payment.amount
                }

            val balance = (
                    invoice.amount - historicalPaid
                    ).coerceAtLeast(0.0)

            if (balance <= 0.009) {
                return@mapNotNull null
            }

            val matchesSearch =
                searchQuery.isBlank() ||
                        customer.name.contains(
                            searchQuery,
                            ignoreCase = true
                        ) ||
                        customer.customerCode.contains(
                            searchQuery,
                            ignoreCase = true
                        ) ||
                        invoice.invoiceNumber.contains(
                            searchQuery,
                            ignoreCase = true
                        )

            if (!matchesSearch) {
                return@mapNotNull null
            }

            val daysOverdue = calculateDaysOverdue(
                dueDate = invoice.dueDate,
                today = asOfDate
            )

            ARAgingRow(
                customer = customer,
                invoice = invoice,
                balance = balance,
                amountPaid = historicalPaid,
                daysOverdue = daysOverdue
            )
        }
    }

    /*
     * --------------------------------------------------------
     * AGING TOTALS
     * --------------------------------------------------------
     */

    val totalOutstanding = agingRows.sumOf {
        it.balance
    }

    val currentTotal = agingRows
        .filter { it.daysOverdue <= 0 }
        .sumOf { it.balance }

    val days1To30Total = agingRows
        .filter { it.daysOverdue in 1..30 }
        .sumOf { it.balance }

    val days31To60Total = agingRows
        .filter { it.daysOverdue in 31..60 }
        .sumOf { it.balance }

    val days61To90Total = agingRows
        .filter { it.daysOverdue in 61..90 }
        .sumOf { it.balance }

    val over90Total = agingRows
        .filter { it.daysOverdue > 90 }
        .sumOf { it.balance }

    val overdueTotal =
        days1To30Total +
                days31To60Total +
                days61To90Total +
                over90Total

    val overduePercentage =
        if (totalOutstanding > 0.0) {
            (overdueTotal / totalOutstanding) * 100.0
        } else {
            0.0
        }

    /*
     * --------------------------------------------------------
     * CUSTOMER TOTALS
     * --------------------------------------------------------
     */

    val customerTotals = agingRows
        .groupBy { it.customer.id }
        .mapNotNull { (_, rows) ->

            val customer = rows.firstOrNull()?.customer
                ?: return@mapNotNull null

            val current = rows
                .filter { it.daysOverdue <= 0 }
                .sumOf { it.balance }

            val overdue = rows
                .filter { it.daysOverdue > 0 }
                .sumOf { it.balance }

            CustomerAgingTotal(
                customer = customer,
                invoiceCount = rows.size,
                current = current,
                overdue = overdue
            )
        }
        .sortedByDescending {
            it.total
        }

    /*
     * --------------------------------------------------------
     * UI
     * --------------------------------------------------------
     */

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {

        ExpensoTopBar(
            title = "AR Aging Report",
            navigationIcon = Icons.Rounded.ArrowBack,
            onNavigationClick = onDismiss,
            navigationContentDescription = "Back",
            actionIcon = Icons.Rounded.PictureAsPdf,
            onActionClick = {
                onExportPdf(asOfDate)
            },
            actionContentDescription = "Export AR Aging Report to PDF"
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            /*
             * HEADER
             */

            //code starts here

            //code ends here

            /*
             * SEARCH
             */

            item {

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
                        Text(
                            text = "Search customer or invoice"
                        )
                    },
                    label = {
                        Text(
                            text = "Search"
                        )
                    }
                )
            }

            /*
             * AS-OF DATE
             */

            item {

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor =
                            MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment =
                            Alignment.CenterVertically,
                        horizontalArrangement =
                            Arrangement.spacedBy(12.dp)
                    ) {

                        Icon(
                            imageVector =
                                Icons.Rounded.CalendarMonth,
                            contentDescription = null
                        )

                        Column(
                            modifier = Modifier.weight(1f)
                        ) {

                            Text(
                                text = "As-of Date",
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                text = asOfDate,
                                style =
                                    MaterialTheme.typography.bodyMedium
                            )
                        }

                        Button(
                            onClick = {
                                asOfDate = today
                            }
                        ) {
                            Text("Today")
                        }
                    }
                }
            }

            /*
             * TOTAL OUTSTANDING
             */

            item {

                AgingTotalCard(
                    total = totalOutstanding,
                    invoiceCount = agingRows.size
                )
            }

            /*
             * OVERDUE EXPOSURE
             */

            item {

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor =
                            MaterialTheme.colorScheme.errorContainer
                    )
                ) {

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        Icon(
                            imageVector =
                                Icons.Rounded.Warning,
                            contentDescription = null
                        )

                        Spacer(
                            modifier = Modifier
                                .padding(horizontal = 8.dp)
                        )

                        Column {

                            Text(
                                text = "Overdue Exposure",
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                text =
                                    "${formatMoney(overdueTotal)} " +
                                            "(${String.format(
                                                Locale.getDefault(),
                                                "%.1f",
                                                overduePercentage
                                            )}%)",
                                style =
                                    MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            /*
             * AGING DASHBOARD
             */

            item {

                Text(
                    text = "Aging Summary",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            item {

                AgingBucketCard(
                    title = "Current",
                    description = "Not yet overdue",
                    amount = currentTotal,
                    invoiceCount = agingRows.count {
                        it.daysOverdue <= 0
                    }
                )
            }

            item {

                AgingBucketCard(
                    title = "1–30 Days",
                    description = "Recently overdue",
                    amount = days1To30Total,
                    invoiceCount = agingRows.count {
                        it.daysOverdue in 1..30
                    }
                )
            }

            item {

                AgingBucketCard(
                    title = "31–60 Days",
                    description = "Moderately overdue",
                    amount = days31To60Total,
                    invoiceCount = agingRows.count {
                        it.daysOverdue in 31..60
                    }
                )
            }

            item {

                AgingBucketCard(
                    title = "61–90 Days",
                    description = "Seriously overdue",
                    amount = days61To90Total,
                    invoiceCount = agingRows.count {
                        it.daysOverdue in 61..90
                    }
                )
            }

            item {

                AgingBucketCard(
                    title = "90+ Days",
                    description = "Severely overdue",
                    amount = over90Total,
                    invoiceCount = agingRows.count {
                        it.daysOverdue > 90
                    }
                )
            }

            /*
             * CUSTOMER TOTALS
             */

            item {

                Text(
                    text = "Customer Balances",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            if (customerTotals.isEmpty()) {

                item {

                    EmptyAgingCard(
                        message =
                            "No outstanding customer balances found."
                    )
                }

            } else {

                items(
                    items = customerTotals,
                    key = {
                        it.customer.id
                    }
                ) { customerTotal ->

                    CustomerAgingTotalCard(
                        customerTotal = customerTotal
                    )
                }
            }

            /*
             * OUTSTANDING INVOICES
             */

            item {

                Text(
                    text = "Outstanding Invoices",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            if (agingRows.isEmpty()) {

                item {

                    EmptyAgingCard(
                        message =
                            "No outstanding invoices match your search."
                    )
                }

            } else {

                items(agingRows) { row ->

                    AgingInvoiceCard(
                        row = row
                    )
                }
            }

            item {

                Spacer(
                    modifier = Modifier.height(24.dp)
                )
            }
        }
    }
}

/*
 * ============================================================
 * DATA CLASSES
 * ============================================================
 */

private data class ARAgingRow(
    val customer: Customer,
    val invoice: CustomerInvoice,
    val balance: Double,
    val amountPaid: Double,
    val daysOverdue: Long
)

private data class CustomerAgingTotal(
    val customer: Customer,
    val invoiceCount: Int,
    val current: Double,
    val overdue: Double
) {
    val total: Double
        get() = current + overdue
}

/*
 * ============================================================
 * AGING CALCULATION
 * ============================================================
 */

private fun calculateDaysOverdue(
    dueDate: String,
    today: String
): Long {

    return try {

        val formatter = SimpleDateFormat(
            "yyyy-MM-dd",
            Locale.getDefault()
        )

        formatter.isLenient = false

        val due = formatter.parse(dueDate)
            ?: return 0L

        val current = formatter.parse(today)
            ?: return 0L

        val difference =
            current.time - due.time

        difference /
                (1000L * 60L * 60L * 24L)

    } catch (exception: Exception) {

        0L
    }
}

/*
 * ============================================================
 * TOTAL CARD
 * ============================================================
 */

//code starts here
@Composable
private fun AgingTotalCard(
    total: Double,
    invoiceCount: Int
) {

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor =
                MaterialTheme.colorScheme.primaryContainer
        )
    ) {

        Column(
            modifier = Modifier.padding(20.dp)
        ) {

            Row(
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Icon(
                    imageVector =
                        Icons.Rounded.AccountBalanceWallet,
                    contentDescription = null
                )

                Spacer(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                )

                Text(
                    text = "Total Outstanding",
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Text(
                text = formatMoney(total),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold
            )

            Text(text = "$invoiceCount outstanding invoice(s)",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/*
 * ============================================================
 * AGING BUCKET CARD
 * ============================================================
 */
//code ends here

/*
 * ============================================================
 * AGING BUCKET CARD
 * ============================================================
 */

@Composable
private fun AgingBucketCard(
    title: String,
    description: String,
    amount: Double,
    invoiceCount: Int
) {

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Icon(
                imageVector = Icons.Rounded.Assessment,
                contentDescription = null
            )

            Spacer(
                modifier = Modifier
                    .padding(horizontal = 10.dp)
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text = title,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(
                horizontalAlignment = Alignment.End
            ) {

                Text(
                    text = formatMoney(amount),
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "$invoiceCount invoice(s)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
//code ends here

/*
 * ============================================================
 * CUSTOMER TOTAL CARD
 * ============================================================
 */

@Composable
private fun CustomerAgingTotalCard(
    customerTotal: CustomerAgingTotal
) {

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {

        Column(
            modifier = Modifier.padding(16.dp)
        ) {

            Text(
                text = customerTotal.customer.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold
            )

            if (customerTotal.customer.customerCode.isNotBlank()) {

                Text(
                    text = customerTotal.customer.customerCode,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(
                modifier = Modifier.height(10.dp)
            )

            InvoiceInfoRow(
                label = "Invoices",
                value = customerTotal.invoiceCount.toString()
            )

            InvoiceInfoRow(
                label = "Current",
                value = formatMoney(customerTotal.current)
            )

            InvoiceInfoRow(
                label = "Overdue",
                value = formatMoney(customerTotal.overdue)
            )

            InvoiceInfoRow(
                label = "Total",
                value = formatMoney(customerTotal.total),
                bold = true
            )
        }
    }
}

/*
 * ============================================================
 * OUTSTANDING INVOICE CARD
 * ============================================================
 */

@Composable
private fun AgingInvoiceCard(
    row: ARAgingRow
) {

    val status = getAgingStatus(
        daysOverdue = row.daysOverdue
    )

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {

        Column(
            modifier = Modifier.padding(16.dp)
        ) {

            Text(
                text = row.customer.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold
            )

            Text(
                text = "Invoice ${row.invoice.invoiceNumber}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(
                modifier = Modifier.height(10.dp)
            )

            InvoiceInfoRow(
                label = "Invoice Date",
                value = row.invoice.invoiceDate
            )

            InvoiceInfoRow(
                label = "Due Date",
                value = row.invoice.dueDate
            )

            InvoiceInfoRow(
                label = "Invoice Amount",
                value = formatMoney(row.invoice.amount)
            )

            InvoiceInfoRow(
                label = "Paid as of date",
                value = formatMoney(row.amountPaid)
            )

            InvoiceInfoRow(
                label = "Balance",
                value = formatMoney(row.balance),
                bold = true
            )

            InvoiceInfoRow(
                label = "Aging Status",
                value = status,
                bold = true
            )

            if (row.daysOverdue > 0) {

                InvoiceInfoRow(
                    label = "Days Overdue",
                    value = "${row.daysOverdue} day(s)"
                )
            }
        }
    }
}

/*
 * ============================================================
 * INFO ROW
 * ============================================================
 */

@Composable
private fun InvoiceInfoRow(
    label: String,
    value: String,
    bold: Boolean = false
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {

        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = value,
            fontWeight = if (bold) {
                FontWeight.Bold
            } else {
                FontWeight.Normal
            }
        )
    }
}

/*
 * ============================================================
 * EMPTY CARD
 * ============================================================
 */

@Composable
private fun EmptyAgingCard(
    message: String
) {

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {

        Text(
            text = message,
            modifier = Modifier.padding(20.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/*
 * ============================================================
 * AGING STATUS
 * ============================================================
 */

private fun getAgingStatus(
    daysOverdue: Long
): String {

    return when {

        daysOverdue <= 0 ->
            "CURRENT"

        daysOverdue <= 30 ->
            "1–30 DAYS OVERDUE"

        daysOverdue <= 60 ->
            "31–60 DAYS OVERDUE"

        daysOverdue <= 90 ->
            "61–90 DAYS OVERDUE"

        else ->
            "90+ DAYS OVERDUE"
    }
}

/*
 * ============================================================
 * MONEY FORMATTER
 * ============================================================
 */

private fun formatMoney(
    amount: Double
): String {

    return String.format(
        Locale.getDefault(),
        "GH₵ %.2f",
        amount
    )
}