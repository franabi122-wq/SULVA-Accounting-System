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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.expenso.app.data.ChartOfAccount
import com.expenso.app.data.ExpensoViewModel
import com.expenso.app.data.LedgerLine
import com.expenso.app.ui.components.ExpensoTopBar
import java.util.Locale

@Composable
fun GeneralLedgerScreen(
    viewModel: ExpensoViewModel,
    account: ChartOfAccount,
    onDismiss: () -> Unit
) {
    BackHandler {
        onDismiss()
    }

    val ledgerLines by remember(account.id) {
        viewModel.getGeneralLedger(account.id)
    }.collectAsState(initial = emptyList())

    val totalDebit = remember(ledgerLines) {
        ledgerLines.sumOf { it.debit }
    }

    val totalCredit = remember(ledgerLines) {
        ledgerLines.sumOf { it.credit }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 32.dp)
            .padding(horizontal = 16.dp)
    ) {

        ExpensoTopBar(
            title = "General Ledger"
        )

        Spacer(modifier = Modifier.height(16.dp))

        AccountHeader(
            account = account
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (ledgerLines.isEmpty()) {

            EmptyLedgerState()

        } else {

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                items(
                    items = ledgerLines,
                    key = { line ->
                        "${line.journalEntryId}-${line.debit}-${line.credit}"
                    }
                ) { line ->

                    LedgerLineCard(
                        line = line
                    )
                }

                item {

                    LedgerTotals(
                        totalDebit = totalDebit,
                        totalCredit = totalCredit
                    )

                    Spacer(
                        modifier = Modifier.height(80.dp)
                    )
                }
            }
        }
    }
}


@Composable
private fun AccountHeader(
    account: ChartOfAccount
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Icon(
                imageVector = Icons.Rounded.AccountBalance,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column {

                Text(
                    text = account.accountName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = "${account.accountCode} • ${account.accountType}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}


@Composable
private fun LedgerLineCard(
    line: LedgerLine
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {

                Text(
                    text = line.date,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (line.referenceNumber.isNotBlank()) {

                    Text(
                        text = "Ref: ${line.referenceNumber}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = line.description.ifBlank {
                    "Journal Entry #${line.journalEntryId}"
                },
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            HorizontalDivider()

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {

                LedgerAmount(
                    label = "Debit",
                    amount = line.debit
                )

                LedgerAmount(
                    label = "Credit",
                    amount = line.credit
                )

                LedgerAmount(
                    label = "Balance",
                    amount = line.balance
                )
            }
        }
    }
}


@Composable
private fun LedgerAmount(
    label: String,
    amount: Double
) {
    Column {

        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(3.dp))

        Text(
            text = formatLedgerAmount(amount),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
    }
}


@Composable
private fun LedgerTotals(
    totalDebit: Double,
    totalCredit: Double
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {

            LedgerAmount(
                label = "Total Debit",
                amount = totalDebit
            )

            LedgerAmount(
                label = "Total Credit",
                amount = totalCredit
            )
        }
    }
}


@Composable
private fun EmptyLedgerState() {

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {

            Icon(
                imageVector = Icons.Rounded.AccountBalance,
                contentDescription = null,
                modifier = Modifier.height(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "No Ledger Entries",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Journal transactions for this account will appear here.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}


private fun formatLedgerAmount(
    amount: Double
): String {
    return String.format(
        Locale.getDefault(),
        "%,.2f",
        amount
    )
}