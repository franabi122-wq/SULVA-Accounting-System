package com.expenso.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.SwapHoriz
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
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import com.expenso.app.data.ChartOfAccount
import com.expenso.app.data.ExpensoViewModel
import com.expenso.app.data.JournalEntry
import com.expenso.app.data.JournalLine
import com.expenso.app.ui.components.ExpensoTopBar
import java.util.Locale

@Composable
fun JournalLinesScreen(
    viewModel: ExpensoViewModel,
    onDismiss: ()-> Unit
) {
    BackHandler{
        onDismiss()
    }
    /*
     * These three properties already exist in your ViewModel.
     */
    val journalLines by viewModel.allJournalLines.collectAsState()
    val journalEntries by viewModel.allJournalEntries.collectAsState()
    val accounts by viewModel.allChartOfAccounts.collectAsState()

    /*
     * Convert accounts and journal entries into lookup maps.
     */
    val accountMap: Map<Int, ChartOfAccount> = remember(accounts) {
        accounts.associateBy { account ->
            account.id
        }
    }

    val journalEntryMap: Map<Int, JournalEntry> = remember(journalEntries) {
        journalEntries.associateBy { entry ->
            entry.id
        }
    }

    /*
     * Group JournalLines by JournalEntry.
     *
     * Example:
     *
     * Journal Entry 1
     *   - Cash       Debit 500
     *   - Sales      Credit 500
     *
     * Journal Entry 2
     *   - Bank       Debit 1000
     *   - Revenue    Credit 1000
     */
    val groupedLines: List<Pair<Int, List<JournalLine>>> =
        remember(journalLines) {
            journalLines
                .groupBy { line ->
                    line.journalEntryId
                }
                .toList()
                .sortedByDescending { pair ->
                    pair.second.maxOfOrNull { line ->
                        line.id
                    } ?: 0
                }
        }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 32.dp)
            .padding(horizontal = 16.dp)
    ) {

        ExpensoTopBar(
            title = "Journal Lines",
            navigationIcon = Icons.AutoMirrored.Rounded.ArrowBack,
            onNavigationClick = onDismiss,
            navigationContentDescription = "Back"
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (journalLines.isEmpty()) {

            EmptyJournalLinesState()

        } else {

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                items(
                    items = groupedLines,
                    key = { pair ->
                        pair.first
                    }
                ) { pair ->

                    val journalEntryId = pair.first
                    val lines = pair.second

                    JournalEntryCard(
                        journalEntry = journalEntryMap[journalEntryId],
                        journalEntryId = journalEntryId,
                        lines = lines,
                        accountMap = accountMap
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}


/**
 * Displays one complete double-entry journal entry.
 */
@Composable
private fun JournalEntryCard(
    journalEntry: JournalEntry?,
    journalEntryId: Int,
    lines: List<JournalLine>,
    accountMap: Map<Int, ChartOfAccount>
) {

    val totalDebit = lines.sumOf { line ->
        line.debit
    }

    val totalCredit = lines.sumOf { line ->
        line.credit
    }

    val isBalanced =
        kotlin.math.abs(totalDebit - totalCredit) < 0.01

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
            modifier = Modifier.fillMaxWidth()
        ) {

            /*
             * JOURNAL ENTRY HEADER
             */
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            MaterialTheme.colorScheme.primaryContainer
                        ),
                    contentAlignment = Alignment.Center
                ) {

                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.MenuBook,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {

                    Text(
                        text = journalEntry
                            ?.description
                            ?.takeIf { description ->
                                description.isNotBlank()
                            }
                            ?: "Journal Entry #$journalEntryId",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(3.dp))

                    Text(
                        text = buildJournalEntrySubtitle(
                            journalEntry = journalEntry,
                            journalEntryId = journalEntryId
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                /*
                 * Balanced / Unbalanced badge.
                 */
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(
                            if (isBalanced) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.errorContainer
                            }
                        )
                        .padding(
                            horizontal = 9.dp,
                            vertical = 6.dp
                        )
                ) {

                    Text(
                        text = if (isBalanced) {
                            "Balanced"
                        } else {
                            "Unbalanced"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isBalanced) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onErrorContainer
                        }
                    )
                }
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant
            )

            /*
             * JOURNAL LINES
             */
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {

                lines.forEach { line ->

                    JournalLineRow(
                        line = line,
                        account = accountMap[line.accountId]
                    )
                }
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant
            )

            /*
             * TOTALS
             */
            JournalTotalsRow(
                totalDebit = totalDebit,
                totalCredit = totalCredit
            )
        }
    }
}


/**
 * Displays one JournalLine.
 */
@Composable
private fun JournalLineRow(
    line: JournalLine,
    account: ChartOfAccount?
) {

    val accountName =
        account?.accountName ?: "Account #${line.accountId}"

    val accountCode =
        account?.accountCode ?: ""

    val hasDebit = line.debit > 0.0
    val hasCredit = line.credit > 0.0

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 16.dp,
                vertical = 12.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(
                    MaterialTheme.colorScheme.secondaryContainer
                ),
            contentAlignment = Alignment.Center
        ) {

            Icon(
                imageVector = if (hasDebit) {
                    Icons.Rounded.AccountBalance
                } else {
                    Icons.Rounded.SwapHoriz
                },
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {

            Text(
                text = accountName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )

            if (accountCode.isNotBlank()) {

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = accountCode,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (line.description.isNotBlank()) {

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = line.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Column(
            horizontalAlignment = Alignment.End
        ) {

            when {
                hasDebit -> {

                    Text(
                        text = "Debit",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Text(
                        text = formatAmount(line.debit),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                hasCredit -> {

                    Text(
                        text = "Credit",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )

                    Text(
                        text = formatAmount(line.credit),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                else -> {

                    Text(
                        text = "—",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}


/**
 * Displays total debit and total credit.
 */
@Composable
private fun JournalTotalsRow(
    totalDebit: Double,
    totalCredit: Double
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {

        Column {

            Text(
                text = "TOTAL DEBIT",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = formatAmount(totalDebit),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        }

        Column(
            horizontalAlignment = Alignment.End
        ) {

            Text(
                text = "TOTAL CREDIT",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary
            )

            Text(
                text = formatAmount(totalCredit),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}


/**
 * Empty state.
 */
@Composable
private fun EmptyJournalLinesState() {

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {

            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ReceiptLong,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "No Journal Lines",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Journal entries and their debit and credit lines will appear here.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}


/**
 * Builds the journal entry subtitle.
 */
private fun buildJournalEntrySubtitle(
    journalEntry: JournalEntry?,
    journalEntryId: Int
): String {

    if (journalEntry == null) {
        return "Journal Entry #$journalEntryId"
    }

    val parts = mutableListOf<String>()

    if (journalEntry.entryDate.isNotBlank()) {
        parts.add(journalEntry.entryDate)
    }

    if (journalEntry.referenceNumber.isNotBlank()) {
        parts.add("Ref: ${journalEntry.referenceNumber}")
    }

    if (parts.isEmpty()) {
        return "Journal Entry #${journalEntry.id}"
    }

    return parts.joinToString(" • ")
}


/**
 * Formats monetary amounts.
 */
private fun formatAmount(
    amount: Double
): String {
    return String.format(
        Locale.getDefault(),
        "%,.2f",
        amount
    )
}