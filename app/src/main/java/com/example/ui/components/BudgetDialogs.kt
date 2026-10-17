package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.Category
import com.example.data.model.IncomeSource
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val currencyFormatter = DecimalFormat("#,##0.00")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseDialog(
    categories: List<Category>,
    incomeSources: List<IncomeSource>,
    onDismiss: () -> Unit,
    onConfirm: (categoryId: String, categoryName: String, amount: Double, date: String, fromIncomeSourceId: String, fromIncomeSourceName: String, notes: String) -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    var amountText by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(categories.firstOrNull()) }
    var selectedIncomeSource by remember { mutableStateOf(incomeSources.firstOrNull()) }
    var dateText by remember { mutableStateOf(dateFormat.format(Date())) }
    var notesText by remember { mutableStateOf("") }
    var categoryExpanded by remember { mutableStateOf(false) }
    var incomeSourceExpanded by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Record Daily Expense",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Amount Input
                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it
                        errorMessage = null
                    },
                    label = { Text("Amount (KSh / Cents)") },
                    placeholder = { Text("0.00") },
                    leadingIcon = {
                        Icon(Icons.Default.Payments, contentDescription = null)
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("expense_amount_input"),
                    singleLine = true,
                    isError = errorMessage != null
                )

                // Category Selector
                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedCategory?.let { "${it.name} (Budget: KSh ${currencyFormatter.format(it.monthlyBudget)})" } ?: "Select Category",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Expense Category") },
                        leadingIcon = { Icon(Icons.Default.Category, contentDescription = null) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                            .testTag("expense_category_selector")
                    )
                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        categories.forEach { category ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(category.name, fontWeight = FontWeight.SemiBold)
                                        Text(
                                            "Budget: KSh ${currencyFormatter.format(category.monthlyBudget)}" +
                                                if (category.notes.isNotBlank()) " • ${category.notes}" else "",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                onClick = {
                                    selectedCategory = category
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }

                // Income Source taken from
                ExposedDropdownMenuBox(
                    expanded = incomeSourceExpanded,
                    onExpandedChange = { incomeSourceExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedIncomeSource?.let { "Take from: ${it.name} (KSh ${currencyFormatter.format(it.expectedAmount)})" } ?: "General Account / Cash",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Taking From Income Pool") },
                        leadingIcon = { Icon(Icons.Default.MonetizationOn, contentDescription = null) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = incomeSourceExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                            .testTag("expense_income_source_selector")
                    )
                    ExposedDropdownMenu(
                        expanded = incomeSourceExpanded,
                        onDismissRequest = { incomeSourceExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("General Account / M-Pesa / Cash") },
                            onClick = {
                                selectedIncomeSource = null
                                incomeSourceExpanded = false
                            }
                        )
                        incomeSources.forEach { source ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(source.name, fontWeight = FontWeight.SemiBold)
                                        Text(
                                            "Expected: KSh ${currencyFormatter.format(source.expectedAmount)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                onClick = {
                                    selectedIncomeSource = source
                                    incomeSourceExpanded = false
                                }
                            )
                        }
                    }
                }

                // Date
                OutlinedTextField(
                    value = dateText,
                    onValueChange = { dateText = it },
                    label = { Text("Date (YYYY-MM-DD)") },
                    leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Notes
                OutlinedTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    label = { Text("Notes / Store / Item (Optional)") },
                    placeholder = { Text("e.g. Naivas breakfast milk & pastry") },
                    leadingIcon = { Icon(Icons.Default.Description, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull()
                    if (amount == null || amount <= 0) {
                        errorMessage = "Please enter a valid amount greater than 0"
                        return@Button
                    }
                    if (selectedCategory == null) {
                        errorMessage = "Please select a category"
                        return@Button
                    }
                    onConfirm(
                        selectedCategory!!.id,
                        selectedCategory!!.name,
                        amount,
                        dateText.trim(),
                        selectedIncomeSource?.id ?: "",
                        selectedIncomeSource?.name ?: "General Account",
                        notesText.trim()
                    )
                },
                modifier = Modifier.testTag("confirm_add_expense_button")
            ) {
                Text("Save Expense")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddIncomeDialog(
    incomeSources: List<IncomeSource>,
    onDismiss: () -> Unit,
    onConfirm: (sourceName: String, amount: Double, date: String, incomeSourceId: String, notes: String) -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    var selectedSource by remember { mutableStateOf(incomeSources.firstOrNull()) }
    var amountText by remember { mutableStateOf(selectedSource?.expectedAmount?.toString() ?: "") }
    var dateText by remember { mutableStateOf(dateFormat.format(Date())) }
    var notesText by remember { mutableStateOf("") }
    var sourceExpanded by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Record Income Received",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Source Selector
                ExposedDropdownMenuBox(
                    expanded = sourceExpanded,
                    onExpandedChange = { sourceExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedSource?.let { "${it.name} (Expected: KSh ${currencyFormatter.format(it.expectedAmount)})" } ?: "Custom Source",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Income Stream") },
                        leadingIcon = { Icon(Icons.Default.MonetizationOn, contentDescription = null) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sourceExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = sourceExpanded,
                        onDismissRequest = { sourceExpanded = false }
                    ) {
                        incomeSources.forEach { source ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(source.name, fontWeight = FontWeight.SemiBold)
                                        Text(
                                            "Expected: KSh ${currencyFormatter.format(source.expectedAmount)}" +
                                                if (source.notes.isNotBlank()) " • ${source.notes}" else "",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                onClick = {
                                    selectedSource = source
                                    amountText = source.expectedAmount.toString()
                                    sourceExpanded = false
                                }
                            )
                        }
                    }
                }

                // Amount
                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it
                        errorMessage = null
                    },
                    label = { Text("Amount Received (KSh)") },
                    placeholder = { Text("17500.00") },
                    leadingIcon = { Icon(Icons.Default.Payments, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Date
                OutlinedTextField(
                    value = dateText,
                    onValueChange = { dateText = it },
                    label = { Text("Date Received (YYYY-MM-DD)") },
                    leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Notes
                OutlinedTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    label = { Text("Reference / Deposit Notes (Optional)") },
                    placeholder = { Text("e.g. M-Pesa ref or bank deposit") },
                    leadingIcon = { Icon(Icons.Default.Description, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull()
                    if (amount == null || amount <= 0) {
                        errorMessage = "Please enter a valid amount"
                        return@Button
                    }
                    val sourceName = selectedSource?.name ?: "General Income"
                    val sourceId = selectedSource?.id ?: ""
                    onConfirm(sourceName, amount, dateText.trim(), sourceId, notesText.trim())
                }
            ) {
                Text("Save Income")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun EditCategoryDialog(
    category: Category? = null,
    onDismiss: () -> Unit,
    onConfirm: (name: String, budget: Double, notes: String, icon: String, colorHex: String) -> Unit
) {
    var name by remember { mutableStateOf(category?.name ?: "") }
    var budgetText by remember { mutableStateOf(category?.monthlyBudget?.toString() ?: "") }
    var notes by remember { mutableStateOf(category?.notes ?: "") }
    var selectedColor by remember { mutableStateOf(category?.colorHex ?: "#1E88E5") }
    var selectedIcon by remember { mutableStateOf(category?.icon ?: "category") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val presetColors = listOf("#1E88E5", "#3949AB", "#00ACC1", "#00897B", "#43A047", "#7CB342", "#C0CA33", "#FBC02D", "#FB8C00", "#F4511E", "#E53935", "#8E24AA", "#6D4C41", "#039BE5")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (category == null) "Add Category" else "Edit Category")
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Category Name") },
                    placeholder = { Text("e.g. Cooking gas") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = budgetText,
                    onValueChange = { budgetText = it },
                    label = { Text("Monthly Budget (KSh)") },
                    placeholder = { Text("2900") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Formula / Notes (from budget PDF)") },
                    placeholder = { Text("e.g. 15000+4000+1000 or (((80*3)*52)/12)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Pick Badge Color", style = MaterialTheme.typography.labelMedium)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(presetColors) { hex ->
                        val color = Color(android.graphics.Color.parseColor(hex))
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(color)
                                .clickable { selectedColor = hex },
                            contentAlignment = Alignment.Center
                        ) {
                            if (selectedColor == hex) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }

                if (errorMessage != null) {
                    Text(errorMessage ?: "", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        errorMessage = "Category name cannot be empty"
                        return@Button
                    }
                    val budget = budgetText.toDoubleOrNull()
                    if (budget == null || budget < 0) {
                        errorMessage = "Please enter a valid monthly budget"
                        return@Button
                    }
                    onConfirm(name.trim(), budget, notes.trim(), selectedIcon, selectedColor)
                }
            ) {
                Text(if (category == null) "Create" else "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun EditIncomeSourceDialog(
    source: IncomeSource? = null,
    onDismiss: () -> Unit,
    onConfirm: (name: String, amount: Double, notes: String) -> Unit
) {
    var name by remember { mutableStateOf(source?.name ?: "") }
    var amountText by remember { mutableStateOf(source?.expectedAmount?.toString() ?: "") }
    var notes by remember { mutableStateOf(source?.notes ?: "") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (source == null) "Add Income Stream" else "Edit Income Stream")
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Stream Name") },
                    placeholder = { Text("e.g. Income Stream 1") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Expected Monthly Amount (KSh)") },
                    placeholder = { Text("17500") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Stream Notes (Optional)") },
                    placeholder = { Text("e.g. Fixed earning") },
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMessage != null) {
                    Text(errorMessage ?: "", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        errorMessage = "Stream name cannot be empty"
                        return@Button
                    }
                    val amount = amountText.toDoubleOrNull()
                    if (amount == null || amount < 0) {
                        errorMessage = "Please enter a valid earning amount"
                        return@Button
                    }
                    onConfirm(name.trim(), amount, notes.trim())
                }
            ) {
                Text(if (source == null) "Create" else "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
