package com.expenso.app.ui.screens

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.expenso.app.data.ExpensoViewModel
import com.expenso.app.data.Supplier
import com.expenso.app.ui.components.ExpensoTopBar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SupplierManagementScreen(
    viewModel: ExpensoViewModel,
    onDismiss: () -> Unit
) {
    val suppliers by viewModel.allSuppliers.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var showFormDialog by remember { mutableStateOf(false) }
    var selectedSupplier by remember { mutableStateOf<Supplier?>(null) }
    var supplierToDelete by remember { mutableStateOf<Supplier?>(null) }

    val filteredSuppliers = suppliers.filter { supplier ->
        supplier.supplierCode.contains(searchQuery, ignoreCase = true) ||
                supplier.name.contains(searchQuery, ignoreCase = true) ||
                supplier.phone.contains(searchQuery, ignoreCase = true) ||
                supplier.email.contains(searchQuery, ignoreCase = true)
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {

        ExpensoTopBar(
            title = "Suppliers",
            actionIcon = Icons.Default.Add,
            onActionClick = {
                selectedSupplier = null
                showFormDialog = true
            },
            actionContentDescription = "Add Supplier",
            navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
            onNavigationClick = onDismiss,
            navigationContentDescription = "Back"
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {

            Text(
                text = "Manage your supplier accounts.",
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text("Search suppliers...")
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search"
                    )
                },
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (filteredSuppliers.isEmpty()) {

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Text(
                        text = if (searchQuery.isBlank()) {
                            "No suppliers yet."
                        } else {
                            "No suppliers found."
                        },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    if (searchQuery.isBlank()) {
                        Text(
                            text = "Tap + to add your first supplier.",
                            fontSize = 14.sp
                        )
                    }
                }

            } else {

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {

                    items(
                        items = filteredSuppliers,
                       // key = { suppliers -> "supplier_${suppliers.id}" }
                        key = { supplier -> "supplier_${supplier.id}" }
                    ) { supplier ->

                        SupplierCard(
                            supplier = supplier,
                            onEdit = {
                                selectedSupplier = supplier
                                showFormDialog = true
                            },
                            onDelete = {
                                supplierToDelete = supplier
                            }
                        )
                    }
                }
            }
        }
    }

    if (showFormDialog) {

        SupplierFormDialog(
            supplier = selectedSupplier,
            onDismiss = {
                showFormDialog = false
                selectedSupplier = null
            },
            onSave = { supplier ->

                if (selectedSupplier == null) {

                    viewModel.addSupplier(
                        supplier = supplier,
                        onSuccess = {
                            showFormDialog = false
                            selectedSupplier = null
                        }
                    )

                } else {

                    viewModel.updateSupplier(
                        supplier = supplier,
                        onSuccess = {
                            showFormDialog = false
                            selectedSupplier = null
                        }
                    )
                }
            }
        )
    }

    supplierToDelete?.let { supplier ->

        AlertDialog(
            onDismissRequest = {
                supplierToDelete = null
            },
            title = {
                Text("Delete Supplier")
            },
            text = {
                Text(
                    "Are you sure you want to delete ${supplier.name}?"
                )
            },
            confirmButton = {

                Button(
                    onClick = {

                        viewModel.deleteSupplier(
                            supplier = supplier,
                            onSuccess = {
                                supplierToDelete = null
                            }
                        )
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {

                OutlinedButton(
                    onClick = {
                        supplierToDelete = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SupplierCard(
    supplier: Supplier,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
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
                        text = supplier.name,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = supplier.supplierCode,
                        fontSize = 13.sp
                    )
                }

                IconButton(
                    onClick = onEdit
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Supplier"
                    )
                }

                IconButton(
                    onClick = onDelete
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Supplier"
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (supplier.phone.isNotBlank()) {
                Text(
                    text = "Phone: ${supplier.phone}",
                    fontSize = 14.sp
                )
            }

            if (supplier.email.isNotBlank()) {
                Text(
                    text = "Email: ${supplier.email}",
                    fontSize = 14.sp
                )
            }

            if (supplier.address.isNotBlank()) {
                Text(
                    text = "Address: ${supplier.address}",
                    fontSize = 14.sp
                )
            }

            if (supplier.notes.isNotBlank()) {

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Notes: ${supplier.notes}",
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (supplier.isActive) {
                    "Active"
                } else {
                    "Inactive"
                },
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun SupplierFormDialog(
    supplier: Supplier?,
    onDismiss: () -> Unit,
    onSave: (Supplier) -> Unit
) {
    var supplierCode by remember(supplier) {
        mutableStateOf(supplier?.supplierCode ?: "")
    }

    var name by remember(supplier) {
        mutableStateOf(supplier?.name ?: "")
    }

    var phone by remember(supplier) {
        mutableStateOf(supplier?.phone ?: "")
    }

    var email by remember(supplier) {
        mutableStateOf(supplier?.email ?: "")
    }

    var address by remember(supplier) {
        mutableStateOf(supplier?.address ?: "")
    }

    var notes by remember(supplier) {
        mutableStateOf(supplier?.notes ?: "")
    }

    var isActive by remember(supplier) {
        mutableStateOf(supplier?.isActive ?: true)
    }

    var errorMessage by remember {
        mutableStateOf("")
    }

    AlertDialog(
        onDismissRequest = onDismiss,

        title = {
            Text(
                if (supplier == null) {
                    "Add Supplier"
                } else {
                    "Edit Supplier"
                }
            )
        },

        text = {

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                OutlinedTextField(
                    value = supplierCode,
                    onValueChange = {
                        supplierCode = it
                    },
                    label = {
                        Text("Supplier Code")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                    },
                    label = {
                        Text("Supplier Name")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = {
                        phone = it
                    },
                    label = {
                        Text("Phone")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it
                    },
                    label = {
                        Text("Email")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = address,
                    onValueChange = {
                        address = it
                    },
                    label = {
                        Text("Address")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = {
                        notes = it
                    },
                    label = {
                        Text("Notes")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Text(
                        text = "Active Supplier",
                        modifier = Modifier.weight(1f)
                    )

                    Switch(
                        checked = isActive,
                        onCheckedChange = {
                            isActive = it
                        }
                    )
                }

                if (errorMessage.isNotBlank()) {

                    Text(
                        text = errorMessage,
                        fontSize = 13.sp
                    )
                }
            }
        },

        confirmButton = {

            Button(
                onClick = {

                    if (supplierCode.isBlank()) {
                        errorMessage = "Supplier code is required."
                        return@Button
                    }

                    if (name.isBlank()) {
                        errorMessage = "Supplier name is required."
                        return@Button
                    }

                    val now =
                        SimpleDateFormat(
                            "yyyy-MM-dd HH:mm:ss",
                            Locale.getDefault()
                        ).format(Date())

                    val supplierToSave = Supplier(
                        id = supplier?.id ?: 0,
                        supplierCode = supplierCode.trim(),
                        name = name.trim(),
                        phone = phone.trim(),
                        email = email.trim(),
                        address = address.trim(),
                        notes = notes.trim(),
                        isActive = isActive,
                        createdAt = supplier?.createdAt ?: now
                    )

                    onSave(supplierToSave)
                }
            ) {
                Text("Save")
            }
        },

        dismissButton = {

            OutlinedButton(
                onClick = onDismiss
            ) {
                Text("Cancel")
            }
        }
    )
}