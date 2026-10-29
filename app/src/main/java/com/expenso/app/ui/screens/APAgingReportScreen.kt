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
import com.expenso.app.data.ExpensoViewModel
import com.expenso.app.data.Supplier
import com.expenso.app.data.SupplierInvoice
import com.expenso.app.data.SupplierPayment
import com.expenso.app.ui.components.ExpensoTopBar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun APAgingReportScreen(
    viewModel: ExpensoViewModel,
    onDismiss: () -> Unit,
    onExportPdf: (String) -> Unit
) {
    BackHandler {
        onDismiss()
    }

    val invoices by viewModel.allSupplierInvoices
        .collectAsStateWithLifecycle()

    val suppliers by viewModel.allSuppliers
        .collectAsStateWithLifecycle()

    val payments by viewModel.allSupplierPayments
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
        suppliers,
        payments,
        asOfDate,
        searchQuery
    ) {
        invoices.mapNotNull { invoice ->

            // Ignore invoices created after the selected date.
            if (invoice.invoiceDate > asOfDate) {
                return@mapNotNull null
            }

            val supplier = suppliers.firstOrNull {
                it.id == invoice.supplierId
            } ?: return@mapNotNull null

            /*
             * Only payments made on or before the
             * selected as-of date are counted.
             */
            val historicalPaid = payments
                .filter { payment ->
                    payment.supplierInvoiceId == invoice.id &&
                            payment.paymentDate <= asOfDate
                }
                .sumOf { payment ->
                    payment.amount
                }

            val balance = (
                    invoice.totalAmount - historicalPaid
                    ).coerceAtLeast(0.0)

            if (balance <= 0.009) {
                return@mapNotNull null
            }

            val matchesSearch =
                searchQuery.isBlank() ||
                        supplier.name.contains(
                            searchQuery,
                            ignoreCase = true
                        ) ||
                        supplier.supplierCode.contains(
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

            val daysOverdue = calculateAPAgingDaysOverdue(
                dueDate = invoice.dueDate,
                asOfDate = asOfDate
            )

            APAgingRow(
                supplier = supplier,
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
     * SUPPLIER TOTALS
     * --------------------------------------------------------
     */

    val supplierTotals = agingRows
        .groupBy { it.supplier.id }
        .mapNotNull { (_, rows) ->

            val supplier = rows.firstOrNull()?.supplier
                ?: return@mapNotNull null

            val current = rows
                .filter { it.daysOverdue <= 0 }
                .sumOf { it.balance }

            val overdue = rows
                .filter { it.daysOverdue > 0 }
                .sumOf { it.balance }

            SupplierAgingTotal(
                supplier = supplier,
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
            title = "AP Aging Report",
            navigationIcon = Icons.Rounded.ArrowBack,
            onNavigationClick = onDismiss,
            navigationContentDescription = "Back",
            actionIcon = Icons.Rounded.PictureAsPdf,
            onActionClick = {
                onExportPdf(asOfDate)
            },
            actionContentDescription = "Export AP Aging Report to PDF"
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

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
                            text = "Search supplier or invoice"
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

                APAgingTotalCard(
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
                                    "${formatAPMoney(overdueTotal)} " +
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
                APAgingBucketCard(
                    title = "Current",
                    description = "Not yet overdue",
                    amount = currentTotal,
                    invoiceCount = agingRows.count {
                        it.daysOverdue <= 0
                    }
                )
            }

            item {
                APAgingBucketCard(
                    title = "1–30 Days",
                    description = "Recently overdue",
                    amount = days1To30Total,
                    invoiceCount = agingRows.count {
                        it.daysOverdue in 1..30
                    }
                )
            }

            item {
                APAgingBucketCard(
                    title = "31–60 Days",
                    description = "Moderately overdue",
                    amount = days31To60Total,
                    invoiceCount = agingRows.count {
                        it.daysOverdue in 31..60
                    }
                )
            }

            item {
                APAgingBucketCard(
                    title = "61–90 Days",
                    description = "Seriously overdue",
                    amount = days61To90Total,
                    invoiceCount = agingRows.count {
                        it.daysOverdue in 61..90
                    }
                )
            }

            item {
                APAgingBucketCard(
                    title = "90+ Days",
                    description = "Severely overdue",
                    amount = over90Total,
                    invoiceCount = agingRows.count {
                        it.daysOverdue > 90
                    }
                )
            }

            /*
             * SUPPLIER TOTALS
             */

            item {

                Text(
                    text = "Supplier Balances",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            if (supplierTotals.isEmpty()) {

                item {

                    APAgingEmptyCard(
                        message =
                            "No outstanding supplier balances found."
                    )
                }

            } else {

                items(
                    items = supplierTotals,
                    key = {
                        it.supplier.id
                    }
                ) { supplierTotal ->

                    SupplierAgingTotalCard(
                        supplierTotal = supplierTotal
                    )
                }
            }

            /*
             * OUTSTANDING INVOICES
             */

            item {

                Text(
                    text = "Outstanding Supplier Invoices",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            if (agingRows.isEmpty()) {

                item {

                    APAgingEmptyCard(
                        message =
                            "No outstanding supplier invoices match your search."
                    )
                }

            } else {

                items(
                    items = agingRows
                ) { row ->

                    APAgingInvoiceCard(
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

private data class APAgingRow(
    val supplier: Supplier,
    val invoice: SupplierInvoice,
    val balance: Double,
    val amountPaid: Double,
    val daysOverdue: Long
)

private data class SupplierAgingTotal(
    val supplier: Supplier,
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

private fun calculateAPAgingDaysOverdue(
    dueDate: String,
    asOfDate: String
): Long {

    return try {

        val formatter = SimpleDateFormat(
            "yyyy-MM-dd",
            Locale.getDefault()
        )

        formatter.isLenient = false

        val due = formatter.parse(dueDate)
            ?: return 0L

        val current = formatter.parse(asOfDate)
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

@Composable
private fun APAgingTotalCard(
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
                text = formatAPMoney(total),
                style =
                    MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold
            )

            Text(
                text =
                    "$invoiceCount outstanding supplier invoice(s)",
                color =
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/*
 * ============================================================
 * AGING BUCKET CARD
 * ============================================================
 */

@Composable
private fun APAgingBucketCard(
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

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            ) {

                Text(
                    text = title,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall
                )

                Text(
                    text = "$invoiceCount invoice(s)",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Text(
                text = formatAPMoney(amount),
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/*
 * ============================================================
 * SUPPLIER TOTAL CARD
 * ============================================================
 */

@Composable
private fun SupplierAgingTotalCard(
    supplierTotal: SupplierAgingTotal
) {

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {

        Column(
            modifier = Modifier.padding(16.dp)
        ) {

            Text(
                text = supplierTotal.supplier.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            if (supplierTotal.supplier.supplierCode.isNotBlank()) {

                Text(
                    text = supplierTotal.supplier.supplierCode,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Text(
                text = "${supplierTotal.invoiceCount} invoice(s)"
            )

            Spacer(
                modifier = Modifier.height(4.dp)
            )

            Text(
                text = "Current: ${
                    formatAPMoney(
                        supplierTotal.current
                    )
                }"
            )

            Text(
                text = "Overdue: ${
                    formatAPMoney(
                        supplierTotal.overdue
                    )
                }"
            )

            Spacer(
                modifier = Modifier.height(4.dp)
            )

            Text(
                text = "Total: ${
                    formatAPMoney(
                        supplierTotal.total
                    )
                }",
                fontWeight = FontWeight.Bold
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
private fun APAgingInvoiceCard(
    row: APAgingRow
) {

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {

        Column(
            modifier = Modifier.padding(16.dp)
        ) {

            Text(
                text = row.supplier.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Invoice: ${row.invoice.invoiceNumber}",
                fontWeight = FontWeight.Medium
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            APInvoiceInfoRow(
                label = "Invoice Date",
                value = row.invoice.invoiceDate
            )

            APInvoiceInfoRow(
                label = "Due Date",
                value = row.invoice.dueDate
            )

            APInvoiceInfoRow(
                label = "Invoice Amount",
                value = formatAPMoney(
                    row.invoice.totalAmount
                )
            )

            APInvoiceInfoRow(
                label = "Paid as of Date",
                value = formatAPMoney(
                    row.amountPaid
                )
            )

            APInvoiceInfoRow(
                label = "Balance",
                value = formatAPMoney(
                    row.balance
                )
            )

            APInvoiceInfoRow(
                label = "Aging Status",
                value = getAPAgingStatus(
                    row.daysOverdue
                )
            )

            APInvoiceInfoRow(
                label = "Days Overdue",
                value = row.daysOverdue.toString()
            )
        }
    }
}

/*
 * ============================================================
 * INVOICE INFO ROW
 * ============================================================
 */

@Composable
private fun APInvoiceInfoRow(
    label: String,
    value: String
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {

        Text(
            text = "$label:",
            modifier = Modifier.weight(1f),
            fontWeight = FontWeight.Medium
        )

        Text(
            text = value,
            modifier = Modifier.weight(1f)
        )
    }
}

/*
 * ============================================================
 * EMPTY CARD
 * ============================================================
 */

@Composable
private fun APAgingEmptyCard(
    message: String
) {

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

/*
 * ============================================================
 * AGING STATUS
 * ============================================================
 */

private fun getAPAgingStatus(
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
 * MONEY FORMAT
 * ============================================================
 */

private fun formatAPMoney(
    amount: Double
): String {

    return String.format(
        Locale.getDefault(),
        "GH₵ %.2f",
        amount
    )
}