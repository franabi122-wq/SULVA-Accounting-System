package com.expenso.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Assessment
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.expenso.app.data.ChartOfAccount
import com.expenso.app.data.ExpensoViewModel
import com.expenso.app.data.JournalLine
import com.expenso.app.ui.components.ExpensoTopBar
import kotlin.math.abs

@Composable
fun FinancialScreen(
    viewModel: ExpensoViewModel,
    onDismiss: () -> Unit,
    onExportPdf: () -> Unit
) {
    BackHandler {
        onDismiss()
    }

    val accounts by viewModel.allChartOfAccounts.collectAsState()
    val journalLines by viewModel.allJournalLines.collectAsState()

    val financialData = remember(accounts, journalLines) {
        buildFinancialData(
            accounts = accounts,
            journalLines = journalLines
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 32.dp,
            bottom = 32.dp
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        item {
            ExpensoTopBar(
                title = "Financial",
                navigationIcon = Icons.AutoMirrored.Rounded.ArrowBack,
                onNavigationClick = onDismiss,
                navigationContentDescription = "Back"
            )
        }


        // ============================================================
        // INCOME STATEMENT
        // ============================================================

        item {
            FinancialSectionHeader(
                icon = Icons.Rounded.Assessment,
                title = "Income Statement",
                subtitle = "Revenue, expenses and net income"
            )
        }

        item {
            FinancialReportCard {

                if (financialData.revenueAccounts.isEmpty() &&
                    financialData.expenseAccounts.isEmpty()
                ) {
                    EmptyFinancialMessage(
                        message = "No revenue or expense activity has been recorded yet."
                    )
                } else {

                    financialData.revenueAccounts.forEach { row ->
                        FinancialAccountRow(
                            account = row.account,
                            amount = row.amount
                        )
                    }

                    if (financialData.revenueAccounts.isNotEmpty() &&
                        financialData.expenseAccounts.isNotEmpty()
                    ) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }

                    financialData.expenseAccounts.forEach { row ->
                        FinancialAccountRow(
                            account = row.account,
                            amount = row.amount
                        )
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp)
                    )

                    FinancialTotalRow(
                        label = "Total Revenue",
                        amount = financialData.totalRevenue
                    )

                    FinancialTotalRow(
                        label = "Total Expenses",
                        amount = financialData.totalExpenses
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    FinancialTotalRow(
                        label = "Net Income",
                        amount = financialData.netIncome,
                        emphasized = true
                    )
                }
            }
        }

        // ============================================================
        // BALANCE SHEET
        // ============================================================

        item {
            FinancialSectionHeader(
                icon = Icons.Rounded.AccountBalance,
                title = "Balance Sheet",
                subtitle = "Assets, liabilities and equity"
            )
        }

        item {
            FinancialReportCard {

                FinancialCategoryTitle("Assets")

                if (financialData.assetAccounts.isEmpty()) {
                    EmptyFinancialMessage(
                        message = "No asset activity has been recorded yet."
                    )
                } else {
                    financialData.assetAccounts.forEach { row ->
                        FinancialAccountRow(
                            account = row.account,
                            amount = row.amount
                        )
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp)
                )

                FinancialTotalRow(
                    label = "Total Assets",
                    amount = financialData.totalAssets,
                    emphasized = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                FinancialCategoryTitle("Liabilities")

                if (financialData.liabilityAccounts.isEmpty()) {
                    EmptyFinancialMessage(
                        message = "No liability activity has been recorded yet."
                    )
                } else {
                    financialData.liabilityAccounts.forEach { row ->
                        FinancialAccountRow(
                            account = row.account,
                            amount = row.amount
                        )
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp)
                )

                FinancialTotalRow(
                    label = "Total Liabilities",
                    amount = financialData.totalLiabilities,
                    emphasized = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                FinancialCategoryTitle("Equity")

                if (financialData.equityAccounts.isEmpty()) {
                    EmptyFinancialMessage(
                        message = "No equity activity has been recorded yet."
                    )
                } else {
                    financialData.equityAccounts.forEach { row ->
                        FinancialAccountRow(
                            account = row.account,
                            amount = row.amount
                        )
                    }
                }

                FinancialAccountRow(
                    accountName = "Current Period Net Income",
                    amount = financialData.netIncome
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp)
                )

                FinancialTotalRow(
                    label = "Total Equity",
                    amount = financialData.totalEquity,
                    emphasized = true
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp)
                )

                FinancialTotalRow(
                    label = "Liabilities + Equity",
                    amount = financialData.totalLiabilitiesAndEquity,
                    emphasized = true
                )
            }
        }

        // ============================================================
        // BALANCE CHECK
        // ============================================================

        item {
            BalanceCheckCard(
                isBalanced = financialData.isBalanced,
                totalAssets = financialData.totalAssets,
                totalLiabilitiesAndEquity =
                    financialData.totalLiabilitiesAndEquity
            )
        }

        //code starts herw
        item {
            Column {
                Text(
                    text = "Financial Statements",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "View the business income statement and balance sheet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onExportPdf,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Export PDF")
                }
            }
        }
        //ends here
    }
}

// ============================================================
// FINANCIAL DATA
// ============================================================

private data class FinancialAccountRowData(
    val account: ChartOfAccount,
    val amount: Double
)

private data class FinancialData(
    val revenueAccounts: List<FinancialAccountRowData>,
    val expenseAccounts: List<FinancialAccountRowData>,
    val assetAccounts: List<FinancialAccountRowData>,
    val liabilityAccounts: List<FinancialAccountRowData>,
    val equityAccounts: List<FinancialAccountRowData>,

    val totalRevenue: Double,
    val totalExpenses: Double,
    val netIncome: Double,

    val totalAssets: Double,
    val totalLiabilities: Double,
    val totalEquity: Double,
    val totalLiabilitiesAndEquity: Double,

    val isBalanced: Boolean
)

private fun buildFinancialData(
    accounts: List<ChartOfAccount>,
    journalLines: List<JournalLine>
): FinancialData {

    fun accountBalance(account: ChartOfAccount): Double {

        val accountLines = journalLines.filter {
            it.accountId == account.id
        }

        val totalDebit = accountLines.sumOf {
            it.debit
        }

        val totalCredit = accountLines.sumOf {
            it.credit
        }

        return when (account.accountType.uppercase()) {

            "ASSET",
            "EXPENSE" -> totalDebit - totalCredit

            "LIABILITY",
            "EQUITY",
            "REVENUE" -> totalCredit - totalDebit

            else -> totalDebit - totalCredit
        }
    }

    val revenueAccounts = accounts
        .filter {
            it.accountType.equals("REVENUE", ignoreCase = true)
        }
        .map {
            FinancialAccountRowData(
                account = it,
                amount = accountBalance(it)
            )
        }
        .filter { it.amount != 0.0 }
        .sortedBy { it.account.accountCode }

    val expenseAccounts = accounts
        .filter {
            it.accountType.equals("EXPENSE", ignoreCase = true)
        }
        .map {
            FinancialAccountRowData(
                account = it,
                amount = accountBalance(it)
            )
        }
        .filter { it.amount != 0.0 }
        .sortedBy { it.account.accountCode }

    val assetAccounts = accounts
        .filter {
            it.accountType.equals("ASSET", ignoreCase = true)
        }
        .map {
            FinancialAccountRowData(
                account = it,
                amount = accountBalance(it)
            )
        }
        .filter { it.amount != 0.0 }
        .sortedBy { it.account.accountCode }

    val liabilityAccounts = accounts
        .filter {
            it.accountType.equals("LIABILITY", ignoreCase = true)
        }
        .map {
            FinancialAccountRowData(
                account = it,
                amount = accountBalance(it)
            )
        }
        .filter { it.amount != 0.0 }
        .sortedBy { it.account.accountCode }

    val equityAccounts = accounts
        .filter {
            it.accountType.equals("EQUITY", ignoreCase = true)
        }
        .map {
            FinancialAccountRowData(
                account = it,
                amount = accountBalance(it)
            )
        }
        .filter { it.amount != 0.0 }
        .sortedBy { it.account.accountCode }

    val totalRevenue = revenueAccounts.sumOf {
        it.amount
    }

    val totalExpenses = expenseAccounts.sumOf {
        it.amount
    }

    val netIncome = totalRevenue - totalExpenses

    val totalAssets = assetAccounts.sumOf {
        it.amount
    }

    val totalLiabilities = liabilityAccounts.sumOf {
        it.amount
    }

    val totalEquityAccounts = equityAccounts.sumOf {
        it.amount
    }

    val totalEquity = totalEquityAccounts + netIncome

    val totalLiabilitiesAndEquity =
        totalLiabilities + totalEquity

    val isBalanced =
        abs(totalAssets - totalLiabilitiesAndEquity) < 0.01

    return FinancialData(
        revenueAccounts = revenueAccounts,
        expenseAccounts = expenseAccounts,
        assetAccounts = assetAccounts,
        liabilityAccounts = liabilityAccounts,
        equityAccounts = equityAccounts,

        totalRevenue = totalRevenue,
        totalExpenses = totalExpenses,
        netIncome = netIncome,

        totalAssets = totalAssets,
        totalLiabilities = totalLiabilities,
        totalEquity = totalEquity,
        totalLiabilitiesAndEquity = totalLiabilitiesAndEquity,

        isBalanced = isBalanced
    )
}

// ============================================================
// UI COMPONENTS
// ============================================================

@Composable
private fun FinancialSectionHeader(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun FinancialReportCard(
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            content = content
        )
    }
}

@Composable
private fun FinancialCategoryTitle(
    title: String
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
    )

    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
private fun FinancialAccountRow(
    account: ChartOfAccount,
    amount: Double
) {
    FinancialAccountRow(
        accountName = "${account.accountCode} - ${account.accountName}",
        amount = amount
    )
}

@Composable
private fun FinancialAccountRow(
    accountName: String,
    amount: Double
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {

        Text(
            text = accountName,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = formatAmount(amount),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun FinancialTotalRow(
    label: String,
    amount: Double,
    emphasized: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {

        Text(
            text = label,
            style = if (emphasized) {
                MaterialTheme.typography.titleMedium
            } else {
                MaterialTheme.typography.bodyMedium
            },
            fontWeight = if (emphasized) {
                FontWeight.Bold
            } else {
                FontWeight.Medium
            }
        )

        Text(
            text = formatAmount(amount),
            style = if (emphasized) {
                MaterialTheme.typography.titleMedium
            } else {
                MaterialTheme.typography.bodyMedium
            },
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun EmptyFinancialMessage(
    message: String
) {
    Text(
        text = message,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
private fun BalanceCheckCard(
    isBalanced: Boolean,
    totalAssets: Double,
    totalLiabilitiesAndEquity: Double
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isBalanced) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.errorContainer
            }
        )
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Icon(
                imageVector = if (isBalanced) {
                    Icons.Rounded.CheckCircle
                } else {
                    Icons.Rounded.Error
                },
                contentDescription = null
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text = if (isBalanced) {
                        "Balance Sheet Balanced"
                    } else {
                        "Balance Sheet Difference"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = if (isBalanced) {
                        "Assets equal liabilities plus equity."
                    } else {
                        "Assets and liabilities plus equity do not currently agree."
                    },
                    style = MaterialTheme.typography.bodySmall
                )

                if (!isBalanced) {
                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Assets: ${formatAmount(totalAssets)}  •  L + E: ${
                            formatAmount(totalLiabilitiesAndEquity)
                        }",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

private fun formatAmount(
    amount: Double
): String {
    return "GH₵ %.2f".format(amount)
}