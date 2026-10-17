package com.example.ui.report

import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.BudgetUiState
import com.example.ui.CategorySpendSummary
import com.example.ui.IncomePoolSummary
import com.example.ui.daily.ExpenseItemRow
import com.example.ui.theme.BudgetDanger
import com.example.ui.theme.BudgetSafe
import com.example.ui.theme.BudgetWarning
import com.example.ui.theme.IncomeGreen
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val currencyFormatter = DecimalFormat("#,##0.00")

@Composable
fun MonthlyReportScreen(
    state: BudgetUiState,
    onMonthChange: (String) -> Unit,
    onDeleteExpense: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val monthFormat = remember { SimpleDateFormat("yyyy-MM", Locale.getDefault()) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf<String?>(null) }

    val filteredExpenses = remember(state.expenses, searchQuery, selectedCategoryFilter) {
        state.expenses.filter { exp ->
            val matchesCategory = selectedCategoryFilter == null || exp.categoryId == selectedCategoryFilter
            val matchesSearch = searchQuery.isBlank() ||
                exp.categoryName.contains(searchQuery, ignoreCase = true) ||
                exp.notes.contains(searchQuery, ignoreCase = true)
            matchesCategory && matchesSearch
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Month Selector Header
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
                            cal.time = monthFormat.parse(state.selectedMonth) ?: Date()
                            cal.add(Calendar.MONTH, -1)
                            onMonthChange(monthFormat.format(cal.time))
                        }
                    ) {
                        Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Previous Month", modifier = Modifier.size(18.dp))
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Monthly Report: ${state.selectedMonth}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Net: KSh ${currencyFormatter.format(state.netBalance)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (state.netBalance >= 0) BudgetSafe else BudgetDanger,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    IconButton(
                        onClick = {
                            val cal = Calendar.getInstance()
                            cal.time = monthFormat.parse(state.selectedMonth) ?: Date()
                            cal.add(Calendar.MONTH, 1)
                            onMonthChange(monthFormat.format(cal.time))
                        }
                    ) {
                        Icon(Icons.Default.ArrowForwardIos, contentDescription = "Next Month", modifier = Modifier.size(18.dp))
                    }
                }
            }
        }

        // Visual Donut Chart Card
        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Spending Distribution",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    if (state.totalSpentMonth > 0) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            SpendingDonutChart(
                                categorySummaries = state.categorySummaries,
                                totalSpent = state.totalSpentMonth
                            )
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Total Spent",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "KSh ${currencyFormatter.format(state.totalSpentMonth)}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No expenses logged for this month yet.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Income Pools Breakdown (17,500, 40,000, 50,000)
        item {
            Text(
                text = "Income Streams & Allocations (KSh 107,500)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        items(state.incomePoolSummaries, key = { it.source.id }) { pool ->
            IncomePoolCard(pool = pool)
        }

        // Detailed Budget Variance Table
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Category Budget Variance Table",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Actual spent vs Expected monthly limit from PDF",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        items(state.categorySummaries, key = { it.category.id }) { catSummary ->
            VarianceRowCard(summary = catSummary)
        }

        // Full Transaction History Header
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "All Transactions (${filteredExpenses.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search transactions...") },
                placeholder = { Text("Search category, store or note") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                singleLine = true
            )
        }

        items(filteredExpenses, key = { it.id }) { expense ->
            ExpenseItemRow(
                expense = expense,
                onDelete = { onDeleteExpense(expense.id) }
            )
        }
    }
}

@Composable
fun SpendingDonutChart(
    categorySummaries: List<CategorySpendSummary>,
    totalSpent: Double,
    modifier: Modifier = Modifier
) {
    val activeSlices = remember(categorySummaries, totalSpent) {
        categorySummaries.filter { it.spent > 0 }.map { item ->
            val color = try {
                Color(android.graphics.Color.parseColor(item.category.colorHex))
            } catch (e: Exception) {
                Color(0xFF1E88E5)
            }
            val sweep = ((item.spent / totalSpent) * 360f).toFloat()
            color to sweep
        }
    }

    Canvas(modifier = modifier.size(190.dp)) {
        var startAngle = -90f
        val strokeWidth = 28.dp.toPx()

        for ((color, sweep) in activeSlices) {
            drawArc(
                color = color,
                startAngle = startAngle,
                sweepAngle = sweep,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
            )
            startAngle += sweep
        }
    }
}

@Composable
fun IncomePoolCard(pool: IncomePoolSummary) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(pool.source.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    if (pool.source.notes.isNotBlank()) {
                        Text(pool.source.notes, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(IncomeGreen.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Expected: KSh ${currencyFormatter.format(pool.expected)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = IncomeGreen,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Received Inflow", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("KSh ${currencyFormatter.format(pool.received)}", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                }
                Column {
                    Text("Taken / Spent", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("KSh ${currencyFormatter.format(pool.taken)}", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                }
                Column {
                    Text("Available Pool", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "KSh ${currencyFormatter.format(pool.remainingPool)}",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (pool.remainingPool >= 0) BudgetSafe else BudgetDanger
                    )
                }
            }
        }
    }
}

@Composable
fun VarianceRowCard(summary: CategorySpendSummary) {
    val statusColor = when {
        summary.isOverBudget -> BudgetDanger
        summary.percentUsed > 85f -> BudgetWarning
        else -> BudgetSafe
    }

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
            Column(modifier = Modifier.weight(1.2f)) {
                Text(summary.category.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                if (summary.category.notes.isNotBlank()) {
                    Text(
                        summary.category.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End, modifier = Modifier.weight(1f)) {
                Text(
                    text = "Spent: KSh ${currencyFormatter.format(summary.spent)}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "Budget: KSh ${currencyFormatter.format(summary.budget)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Box(
                modifier = Modifier
                    .padding(start = 8.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(statusColor.copy(alpha = 0.15f))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (summary.isOverBudget) {
                        "-${summary.percentUsed.toInt()}%"
                    } else {
                        "${summary.percentUsed.toInt()}%"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = statusColor
                )
            }
        }
    }
}
