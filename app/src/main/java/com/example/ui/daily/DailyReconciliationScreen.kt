package com.example.ui.daily

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import com.example.data.model.ExpenseRecord
import com.example.ui.BudgetUiState
import com.example.ui.theme.BudgetDanger
import com.example.ui.theme.BudgetSafe
import com.example.ui.theme.BudgetWarning
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val currencyFormatter = DecimalFormat("#,##0.00")

@Composable
fun DailyReconciliationScreen(
    state: BudgetUiState,
    onReconcileDay: (date: String, spent: Double, allowance: Double, remaining: Double, notes: String) -> Unit,
    onDeleteExpense: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    var selectedDate by remember { mutableStateOf(state.todayDate.ifEmpty { dateFormat.format(Date()) }) }
    var auditNotes by remember { mutableStateOf("") }

    val dayExpenses = remember(state.expenses, selectedDate) {
        state.expenses.filter { it.date == selectedDate }
    }

    val totalSpentDay = remember(dayExpenses) {
        dayExpenses.sumOf { it.amount }
    }

    val unusedDayBalance = remember(state.dailyAllowance, totalSpentDay) {
        state.dailyAllowance - totalSpentDay
    }

    val existingSummary = remember(state.dailySummaries, selectedDate) {
        state.dailySummaries.find { it.date == selectedDate }
    }

    val isMatched = existingSummary?.status == "matched"

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Date Selector Bar
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            val cal = Calendar.getInstance()
                            cal.time = dateFormat.parse(selectedDate) ?: Date()
                            cal.add(Calendar.DAY_OF_MONTH, -1)
                            selectedDate = dateFormat.format(cal.time)
                        }
                    ) {
                        Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Previous Day", modifier = Modifier.size(18.dp))
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (selectedDate == state.todayDate) "Today ($selectedDate)" else selectedDate,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isMatched) "Status: Matched & Closed" else "Status: Pending End-of-Day Match",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isMatched) BudgetSafe else BudgetWarning
                        )
                    }

                    IconButton(
                        onClick = {
                            val cal = Calendar.getInstance()
                            cal.time = dateFormat.parse(selectedDate) ?: Date()
                            cal.add(Calendar.DAY_OF_MONTH, 1)
                            selectedDate = dateFormat.format(cal.time)
                        }
                    ) {
                        Icon(Icons.Default.ArrowForwardIos, contentDescription = "Next Day", modifier = Modifier.size(18.dp))
                    }
                }
            }
        }

        // End of Day Tally & Reconciliation Card
        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Daily Balance Reconciliation",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Match daily spending against your monthly budget allowance",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TallyItem(label = "Daily Allowance", value = "KSh ${currencyFormatter.format(state.dailyAllowance)}")
                        TallyItem(
                            label = "Spent on $selectedDate",
                            value = "KSh ${currencyFormatter.format(totalSpentDay)}",
                            valueColor = if (unusedDayBalance < 0) BudgetDanger else MaterialTheme.colorScheme.onSurface
                        )
                        TallyItem(
                            label = "Unused Balance",
                            value = "KSh ${currencyFormatter.format(unusedDayBalance)}",
                            valueColor = if (unusedDayBalance >= 0) BudgetSafe else BudgetDanger
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = auditNotes,
                        onValueChange = { auditNotes = it },
                        label = { Text("End-of-day notes / reconciliation comment") },
                        placeholder = { Text("e.g. Matched receipts with Naivas and Tuktuk rides") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            onReconcileDay(
                                selectedDate,
                                totalSpentDay,
                                state.dailyAllowance,
                                unusedDayBalance,
                                auditNotes.trim()
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("match_end_of_day_button"),
                        shape = RoundedCornerShape(25.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isMatched) BudgetSafe else MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isMatched) "Update Daily Match & Balance" else "Match & Close Day",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Itemized Expenses for the Day
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Itemized Spending (${dayExpenses.size} items)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Total: KSh ${currencyFormatter.format(totalSpentDay)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        if (dayExpenses.isEmpty()) {
            item {
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No expenses recorded on $selectedDate.\nAll daily allowance is unused!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(dayExpenses, key = { it.id }) { expense ->
                ExpenseItemRow(
                    expense = expense,
                    onDelete = { onDeleteExpense(expense.id) }
                )
            }
        }

        // Past Daily Summaries History
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Past Reconciled Days History",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        items(state.dailySummaries.take(10), key = { it.id }) { summary ->
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
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.EventAvailable, contentDescription = null, tint = BudgetSafe, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(summary.date, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        }
                        if (summary.notes.isNotBlank()) {
                            Text(summary.notes, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Spent: KSh ${currencyFormatter.format(summary.totalSpentToday)}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = if (summary.remainingDayBalance >= 0) {
                                "+KSh ${currencyFormatter.format(summary.remainingDayBalance)} unused"
                            } else {
                                "-KSh ${currencyFormatter.format(-summary.remainingDayBalance)} over"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (summary.remainingDayBalance >= 0) BudgetSafe else BudgetDanger
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TallyItem(label: String, value: String, valueColor: Color = MaterialTheme.colorScheme.onSurface) {
    Column {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = valueColor)
    }
}

@Composable
fun ExpenseItemRow(
    expense: ExpenseRecord,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = expense.categoryName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                if (expense.notes.isNotBlank()) {
                    Text(
                        text = expense.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (expense.fromIncomeSourceName.isNotBlank()) {
                    Text(
                        text = "From: ${expense.fromIncomeSourceName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "KSh ${currencyFormatter.format(expense.amount)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Expense",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
