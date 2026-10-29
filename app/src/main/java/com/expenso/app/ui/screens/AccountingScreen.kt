package com.expenso.app.ui.screens

import android.R.attr.title
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Book
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.expenso.app.data.ExpensoViewModel
import com.expenso.app.ui.components.ExpensoMenuItem


@Composable
fun AccountingScreen(
    viewModel: ExpensoViewModel,
    onNavigateToChartOfAccounts: () -> Unit,
    onNavigateToJournalEntries: () -> Unit,
    onNavigateToJournalLines: () -> Unit,
    onNavigateToGeneralLedger: () -> Unit,

    // NEW: Trial Balance navigation
    onNavigateToTrialBalance: () -> Unit,
    onNavigateToAccountsReceivable: () -> Unit,
    onNavigateToAccountsPayable: () -> Unit,
    onDismiss: () -> Unit
) {
    BackHandler {
        onDismiss()
    }

    val accountingItems = listOf(

        AccountingItem(
            title = "Chart of Accounts",
            subtitle = "Manage your accounting accounts",
            icon = Icons.Rounded.AccountBalance,
            onClick = onNavigateToChartOfAccounts
        ),

        AccountingItem(
            title = "Journal Entries",
            subtitle = "Create and manage accounting entries",
            icon = Icons.AutoMirrored.Rounded.MenuBook,
            onClick = onNavigateToJournalEntries
        ),

        AccountingItem(
            title = "Journal Lines",
            subtitle = "View debit and credit lines",
            icon = Icons.Rounded.Book,
            onClick = onNavigateToJournalLines
        ),

        AccountingItem(
            title = "General Ledger",
            subtitle = "View account activity and balances",
            icon = Icons.Rounded.AccountBalance,
            onClick = onNavigateToGeneralLedger
        ),

        // NEW: Trial Balance
        AccountingItem(
            title = "Trial Balance",
            subtitle = "Check debit and credit balances",
            icon = Icons.Rounded.Calculate,
            onClick = onNavigateToTrialBalance
        ),

        AccountingItem(
            title = "Accounts Receivable",
            subtitle = "Track money owed to the business",
            icon = Icons.Rounded.AccountBalanceWallet,
            onClick = onNavigateToAccountsReceivable
        ),

        AccountingItem(
            title = "Accounts Payable",
            subtitle = "Track money the business owes",
            icon = Icons.Rounded.ReceiptLong,
            onClick = onNavigateToAccountsPayable
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {

        Spacer(modifier = Modifier.height(32.dp))

        com.expenso.app.ui.components.ExpensoTopBar(
            title = "Accounting",
            navigationIcon = Icons.AutoMirrored.Rounded.ArrowBack,
            onNavigationClick = onDismiss,
            navigationContentDescription = "Back"
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Accounting",
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Manage journal entries, ledgers and accounting balances.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(20.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 0.dp
            )
        ) {

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {

                items(accountingItems) { item ->

                    ExpensoMenuItem(
                        icon = item.icon,
                        title = item.title,
                        subtitle = item.subtitle,
                        iconTint = MaterialTheme.colorScheme.primary,
                        iconBgColor = MaterialTheme.colorScheme.primary.copy(
                            alpha = 0.15f
                        ),
                        onClick = item.onClick
                    )
                }
            }
        }
    }
}

private data class AccountingItem(
    val title: String,
    val subtitle: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val onClick: () -> Unit
)