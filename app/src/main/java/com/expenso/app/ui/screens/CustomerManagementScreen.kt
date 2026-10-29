package com.expenso.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.expenso.app.data.Customer
import com.expenso.app.data.ExpensoViewModel
import com.expenso.app.ui.components.ExpensoTopBar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CustomerManagementScreen(
    viewModel: ExpensoViewModel,
    onDismiss: () -> Unit
) {
    BackHandler {
        onDismiss()
    }

    val customers by viewModel.allCustomers.collectAsStateWithLifecycle()

    var searchQuery by remember {
        mutableStateOf("")
    }

    var showCustomerForm by remember {
        mutableStateOf(false)
    }

    var customerToEdit by remember {
        mutableStateOf<Customer?>(null)
    }

    var customerToDelete by remember {
        mutableStateOf<Customer?>(null)
    }

    val filteredCustomers = customers.filter { customer ->
        val query = searchQuery.trim()

        query.isEmpty() ||
                customer.customerCode.contains(query, ignoreCase = true) ||
                customer.name.contains(query, ignoreCase = true) ||
                customer.phone.contains(query, ignoreCase = true) ||
                customer.email.contains(query, ignoreCase = true)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {

        Spacer(modifier = Modifier.height(32.dp))

        ExpensoTopBar(
            title = "Customers",
            navigationIcon = Icons.AutoMirrored.Rounded.ArrowBack,
            onNavigationClick = onDismiss,
            navigationContentDescription = "Back"
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Customers",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Manage your customer accounts.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(
                onClick = {
                    customerToEdit = null
                    showCustomerForm = true
                }
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = "Add customer"
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

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
                Text("Search customers...")
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (filteredCustomers.isEmpty()) {

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Rounded.People,
                    contentDescription = null,
                    modifier = Modifier
                        .height(64.dp)
                        .width(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = if (searchQuery.isBlank()) {
                        "No customers yet"
                    } else {
                        "No customers found"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (searchQuery.isBlank()) {
                        "Tap + to add your first customer."
                    } else {
                        "Try a different search."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (searchQuery.isBlank()) {
                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            customerToEdit = null
                            showCustomerForm = true
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = null
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text("Add Customer")
                    }
                }
            }

        } else {

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                items(
                    items = filteredCustomers
                ) { customer ->

                    CustomerCard(
                        customer = customer,

                        onEdit = {
                            customerToEdit = customer
                            showCustomerForm = true
                        },

                        onDelete = {
                            customerToDelete = customer
                        }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }

    if (showCustomerForm) {
        CustomerFormDialog(
            customer = customerToEdit,

            onDismiss = {
                showCustomerForm = false
                customerToEdit = null
            },

            onSave = { customer ->
                if (customer.id == 0) {
                    viewModel.addCustomer(customer)
                } else {
                    viewModel.updateCustomer(customer)
                }

                showCustomerForm = false
                customerToEdit = null
            }
        )
    }

    customerToDelete?.let { customer ->

        AlertDialog(
            onDismissRequest = {
                customerToDelete = null
            },

            title = {
                Text("Delete Customer")
            },

            text = {
                Text(
                    "Are you sure you want to delete ${customer.name}?"
                )
            },

            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteCustomer(customer)
                        customerToDelete = null
                    }
                ) {
                    Text("Delete")
                }
            },

            dismissButton = {
                TextButton(
                    onClick = {
                        customerToDelete = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun CustomerCard(
    customer: Customer,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Column(
                    modifier = Modifier.weight(1f)
                ) {

                    Text(
                        text = customer.customerCode,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = customer.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(
                    onClick = onEdit
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = "Edit customer"
                    )
                }

                IconButton(
                    onClick = onDelete
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = "Delete customer"
                    )
                }
            }

            if (customer.phone.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Phone: ${customer.phone}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            if (customer.email.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Email: ${customer.email}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            if (customer.address.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Address: ${customer.address}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (customer.isActive) {
                    "Active"
                } else {
                    "Inactive"
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (customer.isActive) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun CustomerFormDialog(
    customer: Customer?,
    onDismiss: () -> Unit,
    onSave: (Customer) -> Unit
) {
    var customerCode by remember(customer) {
        mutableStateOf(customer?.customerCode ?: "")
    }

    var name by remember(customer) {
        mutableStateOf(customer?.name ?: "")
    }

    var phone by remember(customer) {
        mutableStateOf(customer?.phone ?: "")
    }

    var email by remember(customer) {
        mutableStateOf(customer?.email ?: "")
    }

    var address by remember(customer) {
        mutableStateOf(customer?.address ?: "")
    }

    var notes by remember(customer) {
        mutableStateOf(customer?.notes ?: "")
    }

    var isActive by remember(customer) {
        mutableStateOf(customer?.isActive ?: true)
    }

    var errorMessage by remember(customer) {
        mutableStateOf("")
    }

    AlertDialog(
        onDismissRequest = onDismiss,

        title = {
            Text(
                text = if (customer == null) {
                    "Add Customer"
                } else {
                    "Edit Customer"
                }
            )
        },

        text = {

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {

                item {
                    OutlinedTextField(
                        value = customerCode,
                        onValueChange = {
                            customerCode = it
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = {
                            Text("Customer Code")
                        }
                    )
                }

                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            name = it
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = {
                            Text("Customer Name")
                        }
                    )
                }

                item {
                    OutlinedTextField(
                        value = phone,
                        onValueChange = {
                            phone = it
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Phone
                        ),
                        label = {
                            Text("Phone")
                        }
                    )
                }

                item {
                    OutlinedTextField(
                        value = email,
                        onValueChange = {
                            email = it
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email
                        ),
                        label = {
                            Text("Email")
                        }
                    )
                }

                item {
                    OutlinedTextField(
                        value = address,
                        onValueChange = {
                            address = it
                        },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        label = {
                            Text("Address")
                        }
                    )
                }

                item {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = {
                            notes = it
                        },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        label = {
                            Text("Notes")
                        }
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "Active Customer",
                                fontWeight = FontWeight.SemiBold
                            )

                            Text(
                                text = if (isActive) {
                                    "Customer can receive invoices"
                                } else {
                                    "Customer is inactive"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Switch(
                            checked = isActive,
                            onCheckedChange = {
                                isActive = it
                            }
                        )
                    }
                }

                if (errorMessage.isNotBlank()) {
                    item {
                        Text(
                            text = errorMessage,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        },

        confirmButton = {
            Button(
                onClick = {

                    if (customerCode.isBlank()) {
                        errorMessage = "Customer code is required."
                        return@Button
                    }

                    if (name.isBlank()) {
                        errorMessage = "Customer name is required."
                        return@Button
                    }

                    val now = SimpleDateFormat(
                        "yyyy-MM-dd HH:mm:ss",
                        Locale.getDefault()
                    ).format(Date())

                    val customerToSave = Customer(
                        id = customer?.id ?: 0,
                        customerCode = customerCode.trim(),
                        name = name.trim(),
                        phone = phone.trim(),
                        email = email.trim(),
                        address = address.trim(),
                        notes = notes.trim(),
                        isActive = isActive,
                        createdAt = customer?.createdAt ?: now
                    )
                    onSave(customerToSave)
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