package com.expenso.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.expenso.app.data.ChartOfAccount
import com.expenso.app.data.ExpensoViewModel
import com.expenso.app.data.JournalLine
import com.expenso.app.ui.components.ExpensoTopBar
import java.util.Locale
import kotlin.math.abs


/**
 * Represents one account on the Trial Balance.
 *
 * The debitBalance and creditBalance values are the
 * NET ending balances for the account.
 */
private data class TrialBalanceRow(
    val account: ChartOfAccount,
    val totalDebit: Double,
    val totalCredit: Double,
    val debitBalance: Double,
    val creditBalance: Double
)


/**
 * Trial Balance screen.
 *
 * Calculates account balances from:
 *
 * - Chart of Accounts
 * - Journal Lines
 *
 * Fundamental accounting rule:
 *
 * Total Debit Balance == Total Credit Balance
 */
@Composable
fun TrialBalanceScreen(
    viewModel: ExpensoViewModel,
    onDismiss: () -> Unit
) {

    BackHandler {
        onDismiss()
    }

    val accounts by viewModel.allChartOfAccounts.collectAsState()

    val journalLines by viewModel.allJournalLines.collectAsState()


    /*
     * Build Trial Balance rows.
     */
    val trialBalanceRows = remember(
        accounts,
        journalLines
    ) {
        buildTrialBalanceRows(
            accounts = accounts,
            journalLines = journalLines
        )
    }


    /*
     * Total ending debit balances.
     */
    val totalDebitBalance = remember(
        trialBalanceRows
    ) {
        trialBalanceRows.sumOf {
            it.debitBalance
        }
    }


    /*
     * Total ending credit balances.
     */
    val totalCreditBalance = remember(
        trialBalanceRows
    ) {
        trialBalanceRows.sumOf {
            it.creditBalance
        }
    }


    /*
     * Verify the Trial Balance.
     */
    val isBalanced = abs(
        totalDebitBalance - totalCreditBalance
    ) < 0.01


    /*
     * Raw journal totals.
     */
    val totalJournalDebits = remember(
        journalLines
    ) {
        journalLines.sumOf {
            it.debit
        }
    }


    val totalJournalCredits = remember(
        journalLines
    ) {
        journalLines.sumOf {
            it.credit
        }
    }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 32.dp)
            .padding(horizontal = 16.dp)
    ) {

        ExpensoTopBar(
            title = "Trial Balance",
            navigationIcon = Icons.AutoMirrored.Rounded.ArrowBack,
            onNavigationClick = onDismiss,
            navigationContentDescription = "Back"
        )

        Spacer(
            modifier = Modifier.height(16.dp)
        )


        if (accounts.isEmpty()) {

            EmptyTrialBalanceState()

        } else {

            LazyColumn(
                modifier = Modifier.fillMaxSize(),

                verticalArrangement = Arrangement.spacedBy(
                    12.dp
                ),

                contentPadding = PaddingValues(
                    bottom = 100.dp
                )
            ) {

                /*
                 * STATUS
                 */
                item {

                    TrialBalanceStatusCard(
                        isBalanced = isBalanced,
                        accountCount = trialBalanceRows.size
                    )
                }


                /*
                 * HEADER
                 */
                item {

                    TrialBalanceHeader()
                }


                /*
                 * ACCOUNT ROWS
                 */
                items(
                    items = trialBalanceRows,
                    key = {
                        it.account.id
                    }
                ) { row ->

                    TrialBalanceAccountRow(
                        row = row
                    )
                }


                /*
                 * TOTALS
                 */
                item {

                    TrialBalanceTotals(
                        totalDebit = totalDebitBalance,
                        totalCredit = totalCreditBalance
                    )
                }


                /*
                 * VERIFICATION
                 */
                item {

                    TrialBalanceVerificationCard(
                        isBalanced = isBalanced,
                        totalJournalDebits = totalJournalDebits,
                        totalJournalCredits = totalJournalCredits,
                        totalDebitBalance = totalDebitBalance,
                        totalCreditBalance = totalCreditBalance
                    )
                }


                item {

                    Spacer(
                        modifier = Modifier.height(40.dp)
                    )
                }
            }
        }
    }
}


/**
 * Builds Trial Balance rows from existing accounting data.
 */
private fun buildTrialBalanceRows(
    accounts: List<ChartOfAccount>,
    journalLines: List<JournalLine>
): List<TrialBalanceRow> {

    return accounts
        .map { account ->

            val accountLines = journalLines.filter { line ->
                line.accountId == account.id
            }


            val totalDebit = accountLines.sumOf { line ->
                line.debit
            }


            val totalCredit = accountLines.sumOf { line ->
                line.credit
            }


            /*
             * Net account balance.
             */
            val netBalance =
                totalDebit - totalCredit


            val debitBalance =
                if (netBalance > 0.0) {
                    netBalance
                } else {
                    0.0
                }


            val creditBalance =
                if (netBalance < 0.0) {
                    abs(netBalance)
                } else {
                    0.0
                }


            TrialBalanceRow(
                account = account,
                totalDebit = totalDebit,
                totalCredit = totalCredit,
                debitBalance = debitBalance,
                creditBalance = creditBalance
            )
        }

        .sortedBy {
            it.account.accountCode
        }
}


/**
 * Displays the overall Trial Balance status.
 */
@Composable
private fun TrialBalanceStatusCard(
    isBalanced: Boolean,
    accountCount: Int
) {

    Card(
        modifier = Modifier.fillMaxWidth(),

        shape = RoundedCornerShape(20.dp),

        colors = CardDefaults.cardColors(
            containerColor =
                if (isBalanced) {
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
                imageVector =
                    if (isBalanced) {
                        Icons.Rounded.CheckCircle
                    } else {
                        Icons.Rounded.Error
                    },

                contentDescription = null,

                tint =
                    if (isBalanced) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    },

                modifier = Modifier.size(32.dp)
            )


            Spacer(
                modifier = Modifier.width(12.dp)
            )


            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text =
                        if (isBalanced) {
                            "Trial Balance is Balanced"
                        } else {
                            "Trial Balance is Not Balanced"
                        },

                    style = MaterialTheme.typography.titleMedium,

                    fontWeight = FontWeight.Bold
                )


                Spacer(
                    modifier = Modifier.height(4.dp)
                )


                Text(
                    text =
                        "$accountCount account(s) included",

                    style = MaterialTheme.typography.bodyMedium,

                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}


/**
 * Trial Balance column header.
 */
@Composable
private fun TrialBalanceHeader() {

    Card(
        modifier = Modifier.fillMaxWidth(),

        shape = RoundedCornerShape(16.dp),

        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 14.dp,
                    vertical = 12.dp
                ),

            verticalAlignment = Alignment.CenterVertically
        ) {

            Text(
                text = "Account",

                modifier = Modifier.weight(1.5f),

                style = MaterialTheme.typography.labelLarge,

                fontWeight = FontWeight.Bold
            )


            Text(
                text = "Debit",

                modifier = Modifier.weight(1f),

                style = MaterialTheme.typography.labelLarge,

                fontWeight = FontWeight.Bold,

                textAlign = TextAlign.End
            )


            Spacer(
                modifier = Modifier.width(8.dp)
            )


            Text(
                text = "Credit",

                modifier = Modifier.weight(1f),

                style = MaterialTheme.typography.labelLarge,

                fontWeight = FontWeight.Bold,

                textAlign = TextAlign.End
            )
        }
    }
}


/**
 * Displays one Trial Balance account.
 */
@Composable
private fun TrialBalanceAccountRow(
    row: TrialBalanceRow
) {

    Card(
        modifier = Modifier.fillMaxWidth(),

        shape = RoundedCornerShape(16.dp),

        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),

        elevation = CardDefaults.cardElevation(
            defaultElevation = 1.dp
        )
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),

                verticalAlignment = Alignment.CenterVertically
            ) {

                Column(
                    modifier = Modifier.weight(1.5f)
                ) {

                    Text(
                        text = row.account.accountCode,

                        style = MaterialTheme.typography.labelMedium,

                        color = MaterialTheme.colorScheme.primary,

                        fontWeight = FontWeight.Bold
                    )


                    Spacer(
                        modifier = Modifier.height(3.dp)
                    )


                    Text(
                        text = row.account.accountName,

                        style = MaterialTheme.typography.bodyMedium,

                        fontWeight = FontWeight.SemiBold
                    )
                }


                Text(
                    text = formatAmount(
                        row.debitBalance
                    ),

                    modifier = Modifier.weight(1f),

                    style = MaterialTheme.typography.bodySmall,

                    fontWeight = FontWeight.Bold,

                    textAlign = TextAlign.End
                )


                Spacer(
                    modifier = Modifier.width(8.dp)
                )


                Text(
                    text = formatAmount(
                        row.creditBalance
                    ),

                    modifier = Modifier.weight(1f),

                    style = MaterialTheme.typography.bodySmall,

                    fontWeight = FontWeight.Bold,

                    textAlign = TextAlign.End
                )
            }
        }
    }
}


/**
 * Displays Trial Balance totals.
 */
@Composable
private fun TrialBalanceTotals(
    totalDebit: Double,
    totalCredit: Double
) {

    Card(
        modifier = Modifier.fillMaxWidth(),

        shape = RoundedCornerShape(18.dp),

        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {

            Text(
                text = "Trial Balance Totals",

                style = MaterialTheme.typography.titleMedium,

                fontWeight = FontWeight.Bold
            )


            Spacer(
                modifier = Modifier.height(12.dp)
            )


            Row(
                modifier = Modifier.fillMaxWidth()
            ) {

                Column(
                    modifier = Modifier.weight(1f)
                ) {

                    Text(
                        text = "Total Debit",

                        style = MaterialTheme.typography.bodyMedium
                    )


                    Spacer(
                        modifier = Modifier.height(4.dp)
                    )


                    Text(
                        text = formatAmount(totalDebit),

                        style = MaterialTheme.typography.titleMedium,

                        fontWeight = FontWeight.Bold
                    )
                }


                Column(
                    modifier = Modifier.weight(1f)
                ) {

                    Text(
                        text = "Total Credit",

                        style = MaterialTheme.typography.bodyMedium,

                        textAlign = TextAlign.End,

                        modifier = Modifier.fillMaxWidth()
                    )


                    Spacer(
                        modifier = Modifier.height(4.dp)
                    )


                    Text(
                        text = formatAmount(totalCredit),

                        style = MaterialTheme.typography.titleMedium,

                        fontWeight = FontWeight.Bold,

                        textAlign = TextAlign.End,

                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}


/**
 * Displays the final accounting verification.
 */
@Composable
private fun TrialBalanceVerificationCard(
    isBalanced: Boolean,
    totalJournalDebits: Double,
    totalJournalCredits: Double,
    totalDebitBalance: Double,
    totalCreditBalance: Double
) {

    Card(
        modifier = Modifier.fillMaxWidth(),

        shape = RoundedCornerShape(20.dp),

        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),

        elevation = CardDefaults.cardElevation(
            defaultElevation = 1.dp
        )
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {

                Icon(
                    imageVector = Icons.Rounded.Calculate,

                    contentDescription = null,

                    tint = MaterialTheme.colorScheme.primary,

                    modifier = Modifier.size(24.dp)
                )


                Spacer(
                    modifier = Modifier.width(10.dp)
                )


                Text(
                    text = "Accounting Verification",

                    style = MaterialTheme.typography.titleMedium,

                    fontWeight = FontWeight.Bold
                )
            }


            Spacer(
                modifier = Modifier.height(16.dp)
            )


            VerificationRow(
                label = "Journal Debits",
                amount = totalJournalDebits
            )


            VerificationRow(
                label = "Journal Credits",
                amount = totalJournalCredits
            )


            HorizontalDivider(
                modifier = Modifier.padding(
                    vertical = 10.dp
                ),

                color = MaterialTheme.colorScheme.outlineVariant
            )


            VerificationRow(
                label = "Trial Balance Debits",
                amount = totalDebitBalance
            )


            VerificationRow(
                label = "Trial Balance Credits",
                amount = totalCreditBalance
            )


            Spacer(
                modifier = Modifier.height(14.dp)
            )


            Row(
                modifier = Modifier.fillMaxWidth(),

                verticalAlignment = Alignment.CenterVertically
            ) {

                Icon(
                    imageVector =
                        if (isBalanced) {
                            Icons.Rounded.CheckCircle
                        } else {
                            Icons.Rounded.Error
                        },

                    contentDescription = null,

                    tint =
                        if (isBalanced) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.error
                        }
                )


                Spacer(
                    modifier = Modifier.width(8.dp)
                )


                Text(
                    text =
                        if (isBalanced) {
                            "Verification Passed — Total Debits = Total Credits"
                        } else {
                            "Verification Failed — Debits and Credits do not agree"
                        },

                    style = MaterialTheme.typography.bodyMedium,

                    fontWeight = FontWeight.Bold,

                    color =
                        if (isBalanced) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.error
                        }
                )
            }
        }
    }
}


/**
 * One verification row.
 */
@Composable
private fun VerificationRow(
    label: String,
    amount: Double
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                vertical = 5.dp
            ),

        verticalAlignment = Alignment.CenterVertically
    ) {

        Text(
            text = label,

            modifier = Modifier.weight(1f),

            style = MaterialTheme.typography.bodyMedium,

            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = formatAmount(amount),

            style = MaterialTheme.typography.bodyMedium,

            fontWeight = FontWeight.Bold
        )
    }
}


/**
 * Empty Trial Balance state.
 */
@Composable
private fun EmptyTrialBalanceState() {

    Box(
        modifier = Modifier.fillMaxSize(),

        contentAlignment = Alignment.Center
    ) {

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,

            modifier = Modifier.padding(
                horizontal = 24.dp
            )
        ) {

            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(
                        RoundedCornerShape(20.dp)
                    )
                    .background(
                        MaterialTheme.colorScheme.primaryContainer
                    ),

                contentAlignment = Alignment.Center
            ) {

                Icon(
                    imageVector = Icons.Rounded.AccountBalance,

                    contentDescription = null,

                    tint = MaterialTheme.colorScheme.primary,

                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(
                modifier = Modifier.height(18.dp)
            )

            Text(
                text = "No Trial Balance Data",

                style = MaterialTheme.typography.titleLarge,

                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Text(
                text = "Create accounting accounts and journal entries to generate your Trial Balance.",

                style = MaterialTheme.typography.bodyMedium,

                color = MaterialTheme.colorScheme.onSurfaceVariant,

                textAlign = TextAlign.Center
            )
        }
    }
}


/**
 * Formats accounting amounts using Ghana Cedi.
 */
private fun formatAmount(
    amount: Double
): String {

    return String.format(
        Locale.getDefault(),
        "GH₵ %,.2f",
        amount
    )
}
  //End of Trial balance screen code


