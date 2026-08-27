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
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete

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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import com.expenso.app.data.ChartOfAccount
import com.expenso.app.data.ExpensoViewModel
import com.expenso.app.data.JournalEntry
import com.expenso.app.data.JournalLine
//drop-down menu related imports
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
//import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MenuAnchorType
//ends here
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class JournalLineInput(
    val id: Long = System.nanoTime(),
    val accountId: Int? = null,
    val debit: String = "",
    val credit: String = "",
    val description: String = ""
)

@Composable
fun JournalEntriesScreen(
    viewModel: ExpensoViewModel,
    onDismiss: () -> Unit
) {
    BackHandler {
        onDismiss()
    }

    var entryDate by remember {
        mutableStateOf(
            SimpleDateFormat(
                "yyyy-MM-dd",
                Locale.getDefault()
            ).format(Date())
        )
    }

    var referenceNumber by remember {
        mutableStateOf("")
    }

    var description by remember {
        mutableStateOf("")
    }

    var errorMessage by remember {
        mutableStateOf<String?>(null)
    }

    val chartOfAccounts by viewModel.allChartOfAccounts.collectAsState()

    val journalLines = remember {
        mutableStateListOf(
            JournalLineInput(),
            JournalLineInput()
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        com.expenso.app.ui.components.ExpensoTopBar(
            title = "Journal Entries"
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Create Journal Entry",
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = entryDate,
            onValueChange = { entryDate = it },
            modifier = Modifier.fillMaxWidth(),
            label = {
                Text("Date")
            },
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = referenceNumber,
            onValueChange = { referenceNumber = it },
            modifier = Modifier.fillMaxWidth(),
            label = {
                Text("Reference Number")
            },
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            modifier = Modifier.fillMaxWidth(),
            label = {
                Text("Description")
            },
            singleLine = false
        )

        //code starts here
        errorMessage?.let { message ->
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        //code ends here

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Journal Lines",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            items(
                items = journalLines,
                key = { line -> line.id }
            ) { line ->

                JournalLineEditor(
                    line = line,
                    chartOfAccounts = chartOfAccounts,
                    canDelete = journalLines.size > 2,
                    onDelete = {
                        journalLines.remove(line)
                    },
                    onUpdate = { updatedLine ->
                        val index = journalLines.indexOf(line)

                        if (index >= 0) {
                            journalLines[index] = updatedLine
                        }
                    }
                )
            }

            item {

                Button(
                    onClick = {
                        journalLines.add(JournalLineInput())
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = null
                    )

                    Spacer(modifier = Modifier.padding(4.dp))

                    Text("Add Line")
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        // We will connect this to the ViewModel
                        try {
                            val parsedLines = journalLines.map { line ->

                                require(line.accountId != null) {
                                    "Please select an account for every journal line."
                                }

                                val debit = line.debit.toDoubleOrNull() ?: 0.0
                                val credit = line.credit.toDoubleOrNull() ?: 0.0

                                JournalLine(
                                    journalEntryId = 0,
                                    accountId = line.accountId,
                                    debit = debit,
                                    credit = credit,
                                    description = line.description
                                )
                            }

                            val totalDebit = parsedLines.sumOf { it.debit }
                            val totalCredit = parsedLines.sumOf { it.credit }

                            require(
                                kotlin.math.abs(totalDebit - totalCredit) < 0.01
                            ) {
                                "Journal entry is not balanced. Debit: $totalDebit, Credit: $totalCredit"
                            }

                            val entry = JournalEntry(
                                entryDate = entryDate,
                                referenceNumber = referenceNumber,
                                description = description,
                                createdAt = SimpleDateFormat(
                                    "yyyy-MM-dd HH:mm:ss",
                                    Locale.getDefault()
                                ).format(Date())
                            )

                            viewModel.createJournalEntry(
                                entry = entry,
                                lines = parsedLines
                            )

                            onDismiss()

                        } catch (e: Exception) {
                            errorMessage = e.message
                        }
                        // after confirming the screen compiles.
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.MenuBook,
                        contentDescription = null
                    )

                    Spacer(modifier = Modifier.padding(4.dp))

                    Text("Save Journal Entry")
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
//code starts here
private fun JournalLineEditor(
    line: JournalLineInput,
    chartOfAccounts: List<ChartOfAccount>,
    canDelete: Boolean,
    onDelete: () -> Unit,
    onUpdate: (JournalLineInput) -> Unit
) {
    var accountExpanded by remember {
        mutableStateOf(false)
    }

    val selectedAccount = chartOfAccounts.find { account ->
        account.id == line.accountId
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {

        Column(
            modifier = Modifier.padding(12.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth()
            ) {

                Text(
                    text = "Journal Line",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f)
                )

                if (canDelete) {
                    IconButton(
                        onClick = onDelete
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Delete,
                            contentDescription = "Delete line"
                        )
                    }
                }
            }

     //AccountID code starts here
            ExposedDropdownMenuBox(
                expanded = accountExpanded,
                onExpandedChange = {
                    accountExpanded = !accountExpanded
                }
            ) {
                OutlinedTextField(
                    value = selectedAccount?.let {
                        "${it.accountCode} - ${it.accountName}"
                    } ?: "",
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(
                            type = MenuAnchorType.PrimaryNotEditable,
                            enabled = true
                        ),
                    label = {
                        Text("Account")
                    },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(
                            expanded = accountExpanded
                        )
                    },
                    singleLine = true
                )

                ExposedDropdownMenu(
                    expanded = accountExpanded,
                    onDismissRequest = {
                        accountExpanded = false
                    }
                ) {
                    chartOfAccounts.forEach { account ->

                        DropdownMenuItem(
                            text = {
                                Text(
                                    "${account.accountCode} - ${account.accountName}"
                                )
                            },
                            onClick = {
                                onUpdate(
                                    line.copy(
                                        accountId = account.id
                                    )
                                )

                                accountExpanded = false
                            }
                        )
                    }
                }
            }
            //code ends here

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = line.debit,
                onValueChange = { value ->
                    onUpdate(
                        line.copy(debit = value)
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text("Debit")
                },
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = line.credit,
                onValueChange = { value ->
                    onUpdate(
                        line.copy(credit = value)
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text("Credit")
                },
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = line.description,
                onValueChange = { value ->
                    onUpdate(
                        line.copy(description = value)
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text("Line Description")
                },
                singleLine = true
            )
        }
    }
}