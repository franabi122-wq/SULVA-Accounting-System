package com.expenso.app.ui.screens

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.expenso.app.data.ChartOfAccount
import com.expenso.app.data.ExpensoViewModel
import com.expenso.app.data.JournalEntry
import com.expenso.app.data.JournalLine
import com.expenso.app.ui.components.ExpensoTopBar
import androidx.compose.material3.AlertDialog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs


// ============================================================
// JOURNAL LINE INPUT
// ============================================================

data class JournalLineInput(
    val id: Long = System.nanoTime(),
    val accountId: Int? = null,
    val debit: String = "",
    val credit: String = "",
    val description: String = ""
)


// ============================================================
// JOURNAL ENTRIES SCREEN
// ============================================================

@Composable
fun JournalEntriesScreen(
    viewModel: ExpensoViewModel,
    onDismiss: () -> Unit
) {
    android.util.Log.d(
        "EXPENSO_JOURNAL",
        "NEW JournalEntriesScreen loaded"
    )

    var entryToDelete by remember {
        mutableStateOf<JournalEntry?>(null)
    }

    var showCreateForm by remember {
        mutableStateOf(false)
    }

    var searchQuery by remember {
        mutableStateOf("")
    }



    // --------------------------------------------------------
    // REAL DATA FROM THE VIEWMODEL
    // --------------------------------------------------------

    val chartOfAccounts by viewModel.allChartOfAccounts.collectAsState()
    val journalEntries by viewModel.allJournalEntries.collectAsState()
    val journalLines by viewModel.allJournalLines.collectAsState()

    // --------------------------------------------------------
    // SEARCH
    // --------------------------------------------------------

    val filteredEntries = remember(
        journalEntries,
        searchQuery
    ) {
        if (searchQuery.isBlank()) {
            journalEntries
        } else {
            val query = searchQuery.trim()

            journalEntries.filter { entry ->
                entry.referenceNumber.contains(
                    query,
                    ignoreCase = true
                ) ||
                        entry.description.contains(
                            query,
                            ignoreCase = true
                        ) ||
                        entry.entryDate.contains(
                            query,
                            ignoreCase = true
                        )
            }
        }
    }

    // --------------------------------------------------------
    // CREATE FORM
    // --------------------------------------------------------

    if (showCreateForm) {
        CreateJournalEntryContent(
            viewModel = viewModel,
            chartOfAccounts = chartOfAccounts,
            onClose = {
                showCreateForm = false
            }
        )

        return
    }

    // --------------------------------------------------------
    // MAIN SCREEN
    // --------------------------------------------------------

    Scaffold(
        topBar = {
            ExpensoTopBar(
                title = "Journal Entries",
                navigationIcon = Icons.AutoMirrored.Rounded.ArrowBack,
                onNavigationClick = onDismiss,
                navigationContentDescription = "Back"
            )
        }
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(
                    MaterialTheme.colorScheme.background
                )
        ) {

            // ------------------------------------------------
            // HEADER
            // ------------------------------------------------

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 16.dp,
                        vertical = 16.dp
                    )
            ) {

                Text(
                    text = "Journal Entries",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold
                )

                Spacer(
                    modifier = Modifier.height(4.dp)
                )

                Text(
                    text = "Record and review your accounting transactions.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(
                    modifier = Modifier.height(16.dp)
                )

                // --------------------------------------------
                // NEW ENTRY BUTTON
                // --------------------------------------------

                Button(
                    onClick = {
                        showCreateForm = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {

                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = null
                    )

                    Spacer(
                        modifier = Modifier.width(8.dp)
                    )

                    Text(
                        text = "New Journal Entry",
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(
                    modifier = Modifier.height(14.dp)
                )

                // --------------------------------------------
                // SEARCH
                // --------------------------------------------

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
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    searchQuery = ""
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Clear search"
                                )
                            }
                        }
                    },
                    placeholder = {
                        Text("Search journal entries...")
                    },
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(
                    modifier = Modifier.height(10.dp)
                )

                Text(
                    text = if (searchQuery.isBlank()) {
                        "${journalEntries.size} journal entries"
                    } else {
                        "${filteredEntries.size} result(s)"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // ------------------------------------------------
            // EMPTY STATE
            // ------------------------------------------------

            if (filteredEntries.isEmpty()) {

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {

                        Icon(
                            imageVector =
                                Icons.AutoMirrored.Rounded.MenuBook,
                            contentDescription = null,
                            modifier = Modifier.size(58.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )

                        Spacer(
                            modifier = Modifier.height(14.dp)
                        )

                        Text(
                            text = if (searchQuery.isBlank()) {
                                "No Journal Entries Yet"
                            } else {
                                "No Matching Entries"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(
                            modifier = Modifier.height(6.dp)
                        )

                        Text(
                            text = if (searchQuery.isBlank()) {
                                "Create your first journal entry to begin recording transactions."
                            } else {
                                "Try another reference, description, or date."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (searchQuery.isBlank()) {

                            Spacer(
                                modifier = Modifier.height(16.dp)
                            )

                            OutlinedButton(
                                onClick = {
                                    showCreateForm = true
                                },
                                shape = RoundedCornerShape(12.dp)
                            ) {

                                Icon(
                                    imageVector = Icons.Rounded.Add,
                                    contentDescription = null
                                )

                                Spacer(
                                    modifier = Modifier.width(6.dp)
                                )

                                Text("Create Journal Entry")
                            }
                        }
                    }
                }

            } else {

                // ------------------------------------------------
                // ENTRY LIST
                // ------------------------------------------------

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        bottom = 28.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {

                    items(
                        items = filteredEntries,
                        key = { entry ->
                            entry.id
                        }
                    ) { entry ->

                        val lines = journalLines.filter { line ->
                            line.journalEntryId == entry.id
                        }

                        JournalEntryCard(
                            entry = entry,
                            journalLines = lines,
                            chartOfAccounts = chartOfAccounts,
                            onDelete = {
                                entryToDelete = entry
                            }
                        )
                    }
                }
            }
        }
    }

    //delete code starts here
    entryToDelete?.let { entry ->

        AlertDialog(
            onDismissRequest = {
                entryToDelete = null
            },
            title = {
                Text("Delete Journal Entry?")
            },
            text = {
                Text(
                    "This will permanently delete this journal entry " +
                            "and its associated journal lines."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteJournalEntry(
                            entry = entry,
                            onSuccess = {
                                entryToDelete = null
                            },
                            onError = {
                                entryToDelete = null
                            }
                        )
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        entryToDelete = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
    //ends here
}
//Journal Entries screen code ends here


// ============================================================
// CREATE JOURNAL ENTRY
// ============================================================

@Composable
private fun CreateJournalEntryContent(
    viewModel: ExpensoViewModel,
    chartOfAccounts: List<ChartOfAccount>,
    onClose: () -> Unit
) {

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

    var journalLines by remember {
        mutableStateOf(
            listOf(
                JournalLineInput(),
                JournalLineInput()
            )
        )
    }

    var errorMessage by remember {
        mutableStateOf<String?>(null)
    }

    // --------------------------------------------------------
    // TOTALS
    // --------------------------------------------------------

    val totalDebit = journalLines.sumOf {
        it.debit.toDoubleOrNull() ?: 0.0
    }

    val totalCredit = journalLines.sumOf {
        it.credit.toDoubleOrNull() ?: 0.0
    }

    val difference = abs(totalDebit - totalCredit)

    val isBalanced =
        totalDebit > 0.0 &&
                difference < 0.01 &&
                totalCredit > 0.0

    // --------------------------------------------------------
    // SCREEN
    // --------------------------------------------------------

    Scaffold(
        topBar = {
            ExpensoTopBar(
                title = "New Journal Entry",
                navigationIcon = Icons.AutoMirrored.Rounded.ArrowBack,
                onNavigationClick = onClose,
                navigationContentDescription = "Back"
            )
        }
    ) { paddingValues ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 16.dp,
                bottom = 32.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // ------------------------------------------------
            // INTRO CARD
            // ------------------------------------------------

            item {

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor =
                            MaterialTheme.colorScheme.primaryContainer
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
                                Icons.AutoMirrored.Rounded.MenuBook,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint =
                                MaterialTheme.colorScheme.onPrimaryContainer
                        )

                        Spacer(
                            modifier = Modifier.width(12.dp)
                        )

                        Column(
                            modifier = Modifier.weight(1f)
                        ) {

                            Text(
                                text = "Create Journal Entry",
                                style =
                                    MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color =
                                    MaterialTheme.colorScheme.onPrimaryContainer
                            )

                            Spacer(
                                modifier = Modifier.height(4.dp)
                            )

                            Text(
                                text =
                                    "Record the transaction and ensure that total debits equal total credits.",
                                style =
                                    MaterialTheme.typography.bodySmall,
                                color =
                                    MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            // ------------------------------------------------
            // BASIC INFORMATION
            // ------------------------------------------------

            item {

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {

                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {

                        Text(
                            text = "Basic Information",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(
                            modifier = Modifier.height(12.dp)
                        )

                        OutlinedTextField(
                            value = entryDate,
                            onValueChange = {
                                entryDate = it
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            label = {
                                Text("Entry Date")
                            },
                            placeholder = {
                                Text("yyyy-MM-dd")
                            },
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(
                            modifier = Modifier.height(10.dp)
                        )

                        OutlinedTextField(
                            value = referenceNumber,
                            onValueChange = {
                                referenceNumber = it
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            label = {
                                Text("Reference Number")
                            },
                            placeholder = {
                                Text("e.g. JV-001")
                            },
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(
                            modifier = Modifier.height(10.dp)
                        )

                        OutlinedTextField(
                            value = description,
                            onValueChange = {
                                description = it
                            },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            label = {
                                Text("Description")
                            },
                            placeholder = {
                                Text("Describe this transaction...")
                            },
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }

            // ------------------------------------------------
            // JOURNAL LINES HEADER
            // ------------------------------------------------

            item {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {

                        Text(
                            text = "Journal Lines",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "Enter the debit and credit accounts.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            journalLines =
                                journalLines + JournalLineInput()
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {

                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = null
                        )

                        Spacer(
                            modifier = Modifier.width(4.dp)
                        )

                        Text("Add Line")
                    }
                }
            }

            // ------------------------------------------------
            // JOURNAL LINE EDITORS
            // ------------------------------------------------

            items(
                items = journalLines,
                key = { it.id }
            ) { line ->

                JournalLineEditor(
                    line = line,
                    chartOfAccounts = chartOfAccounts,
                    canRemove = journalLines.size > 2,
                    onChange = { updatedLine ->

                        journalLines =
                            journalLines.map {
                                if (it.id == line.id) {
                                    updatedLine
                                } else {
                                    it
                                }
                            }
                    },
                    onRemove = {

                        journalLines =
                            journalLines.filterNot {
                                it.id == line.id
                            }
                    }
                )
            }

            // ------------------------------------------------
            // BALANCE SUMMARY
            // ------------------------------------------------

            item {

                JournalBalanceSummary(
                    totalDebit = totalDebit,
                    totalCredit = totalCredit,
                    difference = difference,
                    isBalanced = isBalanced
                )
            }

            // ------------------------------------------------
            // ERROR MESSAGE
            // ------------------------------------------------

            item {

                if (errorMessage != null) {

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor =
                                MaterialTheme.colorScheme.errorContainer
                        )
                    ) {

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            Icon(
                                imageVector = Icons.Rounded.Warning,
                                contentDescription = null,
                                tint =
                                    MaterialTheme.colorScheme.onErrorContainer
                            )

                            Spacer(
                                modifier = Modifier.width(8.dp)
                            )

                            Text(
                                text = errorMessage ?: "",
                                modifier = Modifier.weight(1f),
                                color =
                                    MaterialTheme.colorScheme.onErrorContainer,
                                style =
                                    MaterialTheme.typography.bodyMedium
                            )

                            TextButton(
                                onClick = {
                                    errorMessage = null
                                }
                            ) {
                                Text("Dismiss")
                            }
                        }
                    }
                }
            }

            // ------------------------------------------------
            // SAVE JOURNAL ENTRY
            // ------------------------------------------------

            item {

                Button(
                    onClick = {

                        errorMessage = null

                        when {

                            description.isBlank() -> {
                                errorMessage =
                                    "Please enter a transaction description."
                            }

                            chartOfAccounts.isEmpty() -> {
                                errorMessage =
                                    "No chart of accounts are available."
                            }

                            journalLines.any {
                                it.accountId == null
                            } -> {
                                errorMessage =
                                    "Please select an account for every journal line."
                            }

                            journalLines.any {

                                val debit =
                                    it.debit.toDoubleOrNull() ?: 0.0

                                val credit =
                                    it.credit.toDoubleOrNull() ?: 0.0

                                debit <= 0.0 && credit <= 0.0
                            } -> {
                                errorMessage =
                                    "Every journal line must contain a debit or credit amount."
                            }

                            journalLines.any {

                                val debit =
                                    it.debit.toDoubleOrNull() ?: 0.0

                                val credit =
                                    it.credit.toDoubleOrNull() ?: 0.0

                                debit > 0.0 && credit > 0.0
                            } -> {
                                errorMessage =
                                    "A journal line cannot contain both debit and credit."
                            }

                            !isBalanced -> {
                                errorMessage =
                                    "The journal entry must be balanced. Total debits must equal total credits."
                            }

                            else -> {

                                val now =
                                    SimpleDateFormat(
                                        "yyyy-MM-dd HH:mm:ss",
                                        Locale.getDefault()
                                    ).format(Date())

                                val entry =
                                    JournalEntry(
                                        entryDate = entryDate.trim(),
                                        referenceNumber =
                                            referenceNumber.trim(),
                                        description =
                                            description.trim(),
                                        createdAt = now
                                    )

                                val parsedLines =
                                    journalLines.mapNotNull { input ->

                                        val accountId =
                                            input.accountId
                                                ?: return@mapNotNull null

                                        val debit =
                                            input.debit
                                                .toDoubleOrNull()
                                                ?: 0.0

                                        val credit =
                                            input.credit
                                                .toDoubleOrNull()
                                                ?: 0.0

                                        JournalLine(
                                            journalEntryId = 0,
                                            accountId = accountId,
                                            debit = debit,
                                            credit = credit,
                                            description =
                                                input.description.trim()
                                        )
                                    }

                                viewModel.createJournalEntry(
                                    entry = entry,
                                    lines = parsedLines,
                                    onSuccess = {
                                        onClose()
                                    },
                                    onError = { message ->
                                        errorMessage = message
                                    }
                                )
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    enabled = true,
                    shape = RoundedCornerShape(14.dp)
                ) {

                    Text(
                        text = "Save Journal Entry",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
//paddingValues code ends here


}


// ============================================================
// JOURNAL LINE EDITOR
// ============================================================

@Composable
private fun JournalLineEditor(
    line: JournalLineInput,
    chartOfAccounts: List<ChartOfAccount>,
    canRemove: Boolean,
    onChange: (JournalLineInput) -> Unit,
    onRemove: () -> Unit
) {

    var accountMenuExpanded by remember {
        mutableStateOf(false)
    }

    val selectedAccount =
        chartOfAccounts.firstOrNull {
            it.id == line.accountId
        }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor =
                MaterialTheme.colorScheme.surface
        )
    ) {

        Column(
            modifier = Modifier.padding(14.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Text(
                    text = "Journal Line",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                if (canRemove) {

                    IconButton(
                        onClick = onRemove
                    ) {

                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Remove line"
                        )
                    }
                }
            }

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            // ACCOUNT DROPDOWN

            Box(
                modifier = Modifier.fillMaxWidth()
            ) {

                OutlinedButton(
                    onClick = {
                        accountMenuExpanded =
                            !accountMenuExpanded
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {

                    Text(
                        text = selectedAccount?.let {
                            "${it.accountCode} — ${it.accountName}"
                        } ?: "Select Account",
                        modifier = Modifier.weight(1f)
                    )
                }

                DropdownMenu(
                    expanded = accountMenuExpanded,
                    onDismissRequest = {
                        accountMenuExpanded = false
                    }
                ) {

                    if (chartOfAccounts.isEmpty()) {

                        DropdownMenuItem(
                            text = {
                                Text("No accounts available")
                            },
                            onClick = {
                                accountMenuExpanded = false
                            }
                        )

                    } else {

                        chartOfAccounts
                            .filter { it.isActive }
                            .forEach { account ->

                                DropdownMenuItem(
                                    text = {

                                        Column {

                                            Text(
                                                text =
                                                    "${account.accountCode} — ${account.accountName}",
                                                fontWeight =
                                                    FontWeight.Medium
                                            )

                                            Text(
                                                text = account.accountType,
                                                style =
                                                    MaterialTheme.typography.bodySmall,
                                                color =
                                                    MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    },
                                    onClick = {

                                        onChange(
                                            line.copy(
                                                accountId = account.id
                                            )
                                        )

                                        accountMenuExpanded = false
                                    }
                                )
                            }
                    }
                }
            }

            Spacer(
                modifier = Modifier.height(10.dp)
            )

            // DEBIT AND CREDIT

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                OutlinedTextField(
                    value = line.debit,
                    onValueChange = { value ->

                        onChange(
                            line.copy(
                                debit = value,
                                credit =
                                    if (value.isNotBlank()) {
                                        ""
                                    } else {
                                        line.credit
                                    }
                            )
                        )
                    },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    label = {
                        Text("Debit")
                    },
                    keyboardOptions =
                        KeyboardOptions(
                            keyboardType =
                                KeyboardType.Decimal
                        ),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = line.credit,
                    onValueChange = { value ->

                        onChange(
                            line.copy(
                                credit = value,
                                debit =
                                    if (value.isNotBlank()) {
                                        ""
                                    } else {
                                        line.debit
                                    }
                            )
                        )
                    },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    label = {
                        Text("Credit")
                    },
                    keyboardOptions =
                        KeyboardOptions(
                            keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(12.dp)
                )
             //first outlined for line.credit code ends here

                //duplicate line.credit code starts here

                //second line code ends here
            }

            Spacer(
                modifier = Modifier.height(10.dp)
            )

            OutlinedTextField(
                value = line.description,
                onValueChange = { value ->

                    onChange(
                        line.copy(
                            description = value
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = {
                    Text("Line Description")
                },
                placeholder = {
                    Text("Optional")
                },
                shape = RoundedCornerShape(12.dp)
            )
        }
    }
}


// ============================================================
// BALANCE SUMMARY
// ============================================================

@Composable
private fun JournalBalanceSummary(
    totalDebit: Double,
    totalCredit: Double,
    difference: Double,
    isBalanced: Boolean
) {

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor =
                if (isBalanced) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.errorContainer
                }
        )
    ) {

        Column(
            modifier = Modifier.padding(16.dp)
        ) {

            Text(
                text = "Journal Balance",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.SpaceBetween
            ) {

                Column {

                    Text(
                        text = "Total Debit",
                        style =
                            MaterialTheme.typography.bodySmall
                    )

                    Text(
                        text = formatAmount(totalDebit),
                        style =
                            MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column(
                    horizontalAlignment = Alignment.End
                ) {

                    Text(
                        text = "Total Credit",
                        style =
                            MaterialTheme.typography.bodySmall
                    )

                    Text(
                        text = formatAmount(totalCredit),
                        style =
                            MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(10.dp)
            )

            HorizontalDivider()

            Spacer(
                modifier = Modifier.height(10.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.SpaceBetween
            ) {

                Text(
                    text = if (isBalanced) {
                        "Status"
                    } else {
                        "Difference"
                    },
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = if (isBalanced) {
                        "✓ Balanced"
                    } else {
                        formatAmount(difference)
                    },
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}


// ============================================================
// JOURNAL ENTRY CARD
// ============================================================

@Composable
private fun JournalEntryCard(
    entry: JournalEntry,
    journalLines: List<JournalLine>,
    chartOfAccounts: List<ChartOfAccount>,
    onDelete: () -> Unit
) {

    var expanded by remember {
        mutableStateOf(false)
    }

    val accountMap = remember(chartOfAccounts) {
        chartOfAccounts.associateBy {
            it.id
        }
    }

    val totalDebit =
        journalLines.sumOf {
            it.debit
        }

    val totalCredit =
        journalLines.sumOf {
            it.credit
        }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 1.dp
        )
    ) {

        Column(
            modifier = Modifier.padding(16.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Column(
                    modifier = Modifier.weight(1f)
                ) {

                    Text(
                        text = entry.description.ifBlank {
                            "Journal Entry"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(
                        modifier = Modifier.height(4.dp)
                    )

                    Text(
                        text = buildString {

                            append(entry.entryDate)

                            if (entry.referenceNumber.isNotBlank()) {
                                append(" • ")
                                append(entry.referenceNumber)
                            }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color =
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = onDelete
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Journal Entry"
                    )
                }

                IconButton(
                    onClick = {
                        expanded = !expanded
                    }
                ) {

                    Icon(
                        imageVector =
                            if (expanded) {
                                Icons.Rounded.ExpandLess
                            } else {
                                Icons.Rounded.ExpandMore
                            },
                        contentDescription =
                            if (expanded) {
                                "Collapse"
                            } else {
                                "Expand"
                            }
                    )

                }
            }

            Spacer(
                modifier = Modifier.height(10.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.SpaceBetween
            ) {

                Column {

                    Text(
                        text = "Debit",
                        style =
                            MaterialTheme.typography.bodySmall,
                        color =
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = formatAmount(totalDebit),
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Column(
                    horizontalAlignment = Alignment.End
                ) {

                    Text(
                        text = "Credit",
                        style =
                            MaterialTheme.typography.bodySmall,
                        color =
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = formatAmount(totalCredit),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (expanded) {

                Spacer(
                    modifier = Modifier.height(14.dp)
                )

                HorizontalDivider()

                Spacer(
                    modifier = Modifier.height(12.dp)
                )

                if (journalLines.isEmpty()) {

                    Text(
                        text = "No journal lines found.",
                        style =
                            MaterialTheme.typography.bodyMedium,
                        color =
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )

                } else {

                    journalLines.forEachIndexed { index, line ->

                        val account =
                            accountMap[line.accountId]

                        Column(
                            modifier = Modifier.fillMaxWidth()
                        ) {

                            Text(
                                text =
                                    "${index + 1}. ${
                                        account?.accountName
                                            ?: "Unknown Account"
                                    }",
                                fontWeight = FontWeight.SemiBold
                            )

                            Text(
                                text =
                                    account?.accountCode
                                        ?: "Account ${line.accountId}",
                                style =
                                    MaterialTheme.typography.bodySmall,
                                color =
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (line.description.isNotBlank()) {

                                Spacer(
                                    modifier = Modifier.height(3.dp)
                                )

                                Text(
                                    text = line.description,
                                    style =
                                        MaterialTheme.typography.bodySmall
                                )
                            }

                            Spacer(
                                modifier = Modifier.height(4.dp)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement =
                                    Arrangement.SpaceBetween
                            ) {

                                Text("Debit")

                                Text(
                                    text =
                                        formatAmount(line.debit),
                                    fontWeight =
                                        FontWeight.Medium
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement =
                                    Arrangement.SpaceBetween
                            ) {

                                Text("Credit")

                                Text(
                                    text =
                                        formatAmount(line.credit),
                                    fontWeight =
                                        FontWeight.Medium
                                )
                            }

                            if (index < journalLines.lastIndex) {

                                Spacer(
                                    modifier = Modifier.height(10.dp)
                                )

                                HorizontalDivider()

                                Spacer(
                                    modifier = Modifier.height(10.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}


// ============================================================
// AMOUNT FORMATTER
// ============================================================

private fun formatAmount(
    amount: Double
): String {

    return String.format(
        Locale.getDefault(),
        "GH₵ %.2f",
        amount
    )
}