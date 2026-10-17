package com.example.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
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
import androidx.compose.ui.unit.dp
import com.example.data.model.Category
import com.example.data.model.IncomeSource
import com.example.ui.BudgetUiState
import com.example.ui.components.EditCategoryDialog
import com.example.ui.components.EditIncomeSourceDialog
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import java.text.DecimalFormat

private val currencyFormatter = DecimalFormat("#,##0.00")

@Composable
fun AdminPanelScreen(
    state: BudgetUiState,
    onAddCategory: (String, Double, String, String, String) -> Unit,
    onUpdateCategory: (Category) -> Unit,
    onDeleteCategory: (String) -> Unit,
    onAddIncomeSource: (String, Double, String) -> Unit,
    onUpdateIncomeSource: (IncomeSource) -> Unit,
    onDeleteIncomeSource: (String) -> Unit,
    onResetDefaults: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    var editingCategory by remember { mutableStateOf<Category?>(null) }
    var isAddingCategory by remember { mutableStateOf(false) }

    var editingIncomeSource by remember { mutableStateOf<IncomeSource?>(null) }
    var isAddingIncomeSource by remember { mutableStateOf(false) }

    var showResetConfirm by remember { mutableStateOf(false) }

    val currentUser = Firebase.auth.currentUser

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Admin Header Card
        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AdminPanelSettings,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Backend Admin Control",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = currentUser?.email ?: "admin@bingutech.co.ke",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Currency: Kenya Shillings (KES)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // Section: Income Streams Management
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Income Streams (${state.incomeSources.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Manage fixed earnings: 17,500 + 40,000 + 50,000",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(
                    onClick = { isAddingIncomeSource = true },
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Stream")
                }
            }
        }

        items(state.incomeSources, key = { it.id }) { source ->
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(source.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        Text("Expected: KSh ${currencyFormatter.format(source.expectedAmount)}", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                        if (source.notes.isNotBlank()) {
                            Text(source.notes, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Row {
                        IconButton(onClick = { editingIncomeSource = source }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Stream", modifier = Modifier.size(20.dp))
                        }
                        IconButton(onClick = { onDeleteIncomeSource(source.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Stream", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }

        // Section: Category Management
        item {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Expense Categories (${state.categories.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Manage budgets, formulas & notes",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(
                    onClick = { isAddingCategory = true },
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("admin_add_category_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Category")
                }
            }
        }

        items(state.categories, key = { it.id }) { cat ->
            val color = try {
                Color(android.graphics.Color.parseColor(cat.colorHex))
            } catch (e: Exception) {
                Color(0xFF1E88E5)
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(color)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(cat.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                            Text("Budget: KSh ${currencyFormatter.format(cat.monthlyBudget)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                            if (cat.notes.isNotBlank()) {
                                Text(cat.notes, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    Row {
                        IconButton(onClick = { editingCategory = cat }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Category", modifier = Modifier.size(20.dp))
                        }
                        IconButton(onClick = { onDeleteCategory(cat.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Category", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }

        // Reset to Defaults & Account Actions
        item {
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "System Administration",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = { showResetConfirm = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Restore 14 PDF Preset Categories")
            }

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = onSignOut,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_sign_out_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            ) {
                Icon(Icons.Default.ExitToApp, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sign Out of Account")
            }
        }
    }

    // Category Dialogs
    if (isAddingCategory) {
        EditCategoryDialog(
            category = null,
            onDismiss = { isAddingCategory = false },
            onConfirm = { name, budget, notes, icon, colorHex ->
                onAddCategory(name, budget, notes, icon, colorHex)
                isAddingCategory = false
            }
        )
    }

    editingCategory?.let { cat ->
        EditCategoryDialog(
            category = cat,
            onDismiss = { editingCategory = null },
            onConfirm = { name, budget, notes, icon, colorHex ->
                onUpdateCategory(cat.copy(name = name, monthlyBudget = budget, notes = notes, icon = icon, colorHex = colorHex))
                editingCategory = null
            }
        )
    }

    // Income Source Dialogs
    if (isAddingIncomeSource) {
        EditIncomeSourceDialog(
            source = null,
            onDismiss = { isAddingIncomeSource = false },
            onConfirm = { name, amount, notes ->
                onAddIncomeSource(name, amount, notes)
                isAddingIncomeSource = false
            }
        )
    }

    editingIncomeSource?.let { src ->
        EditIncomeSourceDialog(
            source = src,
            onDismiss = { editingIncomeSource = null },
            onConfirm = { name, amount, notes ->
                onUpdateIncomeSource(src.copy(name = name, expectedAmount = amount, notes = notes))
                editingIncomeSource = null
            }
        )
    }

    // Reset Confirmation
    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("Restore Default Preset Categories?") },
            text = { Text("This will re-populate all 14 categories (Rent, Rent tax, Maid, Internet, Tuktuk, Mwembe, DTB, Electricity, EMR/Hosting/Google, Petrol, Insurance, Water, Naivas Breakfast, Cooking gas) and the 3 income streams from your budget brief.") },
            confirmButton = {
                Button(
                    onClick = {
                        onResetDefaults()
                        showResetConfirm = false
                    }
                ) {
                    Text("Confirm Restore")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
