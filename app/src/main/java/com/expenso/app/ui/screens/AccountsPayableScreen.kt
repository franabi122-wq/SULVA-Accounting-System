package com.expenso.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Payments

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll


import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.expenso.app.data.ExpensoViewModel
import com.expenso.app.ui.components.ExpensoTopBar

@Composable
fun AccountsPayableScreen(
    viewModel: ExpensoViewModel,
    onNavigateToSuppliers: () -> Unit,
    onNavigateToSupplierInvoices: () -> Unit,
    onNavigateToSupplierPayments: () -> Unit,
    onNavigateToAPAging: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {

        ExpensoTopBar(
            title = "Accounts Payable",
            navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
            onNavigationClick = onDismiss,
            navigationContentDescription = "Back"
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {

            Text(
                text = "Manage money owed to suppliers.",
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Payables Overview",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                PayableSummaryCard(
                    title = "Supplier Bills",
                    icon = Icons.Default.Description,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToSupplierInvoices
                )

                PayableSummaryCard(
                    title = "Payments",
                    icon = Icons.Default.Payments,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToSupplierPayments
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Accounts Payable Management",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            PayableMenuItem(
                title = "Suppliers",
                subtitle = "Manage supplier accounts",
                icon = Icons.Default.People,
                onClick = onNavigateToSuppliers
            )

            Spacer(modifier = Modifier.height(10.dp))

            PayableMenuItem(
                title = "Supplier Invoices",
                subtitle = "Record and manage supplier bills",
                icon = Icons.Default.Description,
                onClick = onNavigateToSupplierInvoices
            )

            Spacer(modifier = Modifier.height(10.dp))

            PayableMenuItem(
                title = "Supplier Payments",
                subtitle = "Record payments made to suppliers",
                icon = Icons.Default.Payments,
                onClick = onNavigateToSupplierPayments
            )

            Spacer(modifier = Modifier.height(10.dp))

            PayableMenuItem(
                title = "AP Aging",
                subtitle = "View outstanding supplier balances",
                icon = Icons.Default.AccountBalance,
                onClick = onNavigateToAPAging
            )
        }
    }
}

//code starts here
@Composable
private fun PayableSummaryCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        ),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Icon(
                imageVector = icon,
                contentDescription = title
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Open",
                fontSize = 12.sp
            )
        }
    }
}
//code ends here

@Composable
private fun PayableMenuItem(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Icon(
                imageVector = icon,
                contentDescription = title
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp)
            ) {

                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = subtitle,
                    fontSize = 13.sp
                )
            }

            TextButton(
                onClick = onClick
            ) {
                Text("Open")
            }
        }
    }
}