package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Category
import com.example.data.model.DailySummary
import com.example.data.model.ExpenseRecord
import com.example.data.model.IncomeRecord
import com.example.data.model.IncomeSource
import com.example.data.repository.BudgetRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class CategorySpendSummary(
    val category: Category,
    val spent: Double,
    val budget: Double,
    val remaining: Double,
    val percentUsed: Float,
    val isOverBudget: Boolean
)

data class IncomePoolSummary(
    val source: IncomeSource,
    val expected: Double,
    val received: Double,
    val taken: Double,
    val remainingPool: Double
)

private data class RawBudgetData(
    val categories: List<Category>,
    val incomeSources: List<IncomeSource>,
    val incomes: List<IncomeRecord>,
    val expenses: List<ExpenseRecord>,
    val dailySummaries: List<DailySummary>
)

data class BudgetUiState(
    val isLoading: Boolean = true,
    val selectedMonth: String = "", // e.g. "2026-10"
    val todayDate: String = "", // e.g. "2026-10-17"
    val categories: List<Category> = emptyList(),
    val incomeSources: List<IncomeSource> = emptyList(),
    val incomes: List<IncomeRecord> = emptyList(),
    val expenses: List<ExpenseRecord> = emptyList(),
    val dailySummaries: List<DailySummary> = emptyList(),
    val totalMonthlyBudget: Double = 0.0,
    val totalExpectedIncome: Double = 0.0,
    val totalReceivedIncome: Double = 0.0,
    val totalSpentMonth: Double = 0.0,
    val remainingBudget: Double = 0.0,
    val netBalance: Double = 0.0,
    val todaySpent: Double = 0.0,
    val dailyAllowance: Double = 0.0,
    val todayRemaining: Double = 0.0,
    val isTodayMatched: Boolean = false,
    val categorySummaries: List<CategorySpendSummary> = emptyList(),
    val incomePoolSummaries: List<IncomePoolSummary> = emptyList(),
    val userFeedbackMessage: String? = null
)

class BudgetViewModel(
    private val repository: BudgetRepository
) : ViewModel() {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val monthFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())

    private val _selectedMonth = MutableStateFlow(monthFormat.format(Date()))
    val selectedMonth = _selectedMonth.asStateFlow()

    private val _userFeedback = MutableStateFlow<String?>(null)

    init {
        // Bootstrap user default data (14 PDF categories & 3 preset streams) if database is empty
        viewModelScope.launch {
            repository.seedDefaultDataIfEmpty()
        }
    }

    private val rawDataFlow = combine(
        repository.observeCategories(),
        repository.observeIncomeSources(),
        repository.observeIncomes(),
        repository.observeExpenses(),
        repository.observeDailySummaries()
    ) { rawCats, rawSources, rawIncomes, rawExpenses, rawSummaries ->
        RawBudgetData(rawCats, rawSources, rawIncomes, rawExpenses, rawSummaries)
    }

    val uiState: StateFlow<BudgetUiState> = combine(
        rawDataFlow,
        _selectedMonth,
        _userFeedback
    ) { rawData, month, feedback ->

        val rawCats = rawData.categories
        val rawSources = rawData.incomeSources
        val rawIncomes = rawData.incomes
        val rawExpenses = rawData.expenses
        val rawSummaries = rawData.dailySummaries

        val today = dateFormat.format(Date())
        val daysInMonth = Calendar.getInstance().getActualMaximum(Calendar.DAY_OF_MONTH).coerceAtLeast(28)

        // Filter current month data
        val monthExpenses = rawExpenses.filter { it.date.startsWith(month) }
        val monthIncomes = rawIncomes.filter { it.date.startsWith(month) }

        val totalBudget = rawCats.sumOf { it.monthlyBudget }
        val totalExpectedInc = rawSources.sumOf { it.expectedAmount }
        val totalReceivedInc = monthIncomes.sumOf { it.amount }
        val totalSpent = monthExpenses.sumOf { it.amount }
        val remainingMonthlyBudget = totalBudget - totalSpent
        val netBal = totalReceivedInc - totalSpent

        // Daily allowance based on monthly budget
        val dailyTarget = if (daysInMonth > 0) totalBudget / daysInMonth else 0.0
        val todayExpenses = rawExpenses.filter { it.date == today }
        val todaySpentAmount = todayExpenses.sumOf { it.amount }
        val todayRem = dailyTarget - todaySpentAmount

        val todaySummary = rawSummaries.find { it.date == today }
        val isMatched = todaySummary?.status == "matched"

        // Category breakdown
        val catSummaries = rawCats.map { cat ->
            val spentOnCat = monthExpenses.filter { it.categoryId == cat.id }.sumOf { it.amount }
            val remainingOnCat = cat.monthlyBudget - spentOnCat
            val percent = if (cat.monthlyBudget > 0) {
                ((spentOnCat / cat.monthlyBudget) * 100).toFloat()
            } else 0f
            CategorySpendSummary(
                category = cat,
                spent = spentOnCat,
                budget = cat.monthlyBudget,
                remaining = remainingOnCat,
                percentUsed = percent,
                isOverBudget = spentOnCat > cat.monthlyBudget
            )
        }

        // Income source utilization (how much taken from each income stream)
        val poolSummaries = rawSources.map { src ->
            val receivedForSource = monthIncomes.filter { it.incomeSourceId == src.id }.sumOf { it.amount }
            val takenFromSource = monthExpenses.filter { it.fromIncomeSourceId == src.id }.sumOf { it.amount }
            val availablePool = receivedForSource - takenFromSource
            IncomePoolSummary(
                source = src,
                expected = src.expectedAmount,
                received = receivedForSource,
                taken = takenFromSource,
                remainingPool = availablePool
            )
        }

        BudgetUiState(
            isLoading = false,
            selectedMonth = month,
            todayDate = today,
            categories = rawCats,
            incomeSources = rawSources,
            incomes = monthIncomes,
            expenses = monthExpenses,
            dailySummaries = rawSummaries,
            totalMonthlyBudget = totalBudget,
            totalExpectedIncome = totalExpectedInc,
            totalReceivedIncome = totalReceivedInc,
            totalSpentMonth = totalSpent,
            remainingBudget = remainingMonthlyBudget,
            netBalance = netBal,
            todaySpent = todaySpentAmount,
            dailyAllowance = dailyTarget,
            todayRemaining = todayRem,
            isTodayMatched = isMatched,
            categorySummaries = catSummaries,
            incomePoolSummaries = poolSummaries,
            userFeedbackMessage = feedback
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = BudgetUiState()
    )

    fun setSelectedMonth(month: String) {
        _selectedMonth.value = month
    }

    fun clearFeedback() {
        _userFeedback.value = null
    }

    // --- EXPENSES ---

    fun addExpense(
        categoryId: String,
        categoryName: String,
        amount: Double,
        date: String = dateFormat.format(Date()),
        fromIncomeSourceId: String = "",
        fromIncomeSourceName: String = "",
        notes: String = ""
    ) {
        viewModelScope.launch {
            val result = repository.recordExpense(
                categoryId = categoryId,
                categoryName = categoryName,
                amount = amount,
                date = date,
                fromIncomeSourceId = fromIncomeSourceId,
                fromIncomeSourceName = fromIncomeSourceName,
                notes = notes
            )
            if (result.isSuccess) {
                _userFeedback.value = "Recorded KSh $amount for $categoryName"
            } else {
                _userFeedback.value = "Failed to record expense: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    fun deleteExpense(expenseId: String) {
        viewModelScope.launch {
            val result = repository.deleteExpense(expenseId)
            if (result.isSuccess) {
                _userFeedback.value = "Expense deleted"
            }
        }
    }

    // --- INCOMES ---

    fun recordIncome(
        sourceName: String,
        amount: Double,
        date: String = dateFormat.format(Date()),
        incomeSourceId: String = "",
        notes: String = ""
    ) {
        viewModelScope.launch {
            val result = repository.recordIncome(
                sourceName = sourceName,
                amount = amount,
                date = date,
                incomeSourceId = incomeSourceId,
                notes = notes
            )
            if (result.isSuccess) {
                _userFeedback.value = "Received income KSh $amount from $sourceName"
            } else {
                _userFeedback.value = "Failed to record income: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    fun deleteIncome(income: IncomeRecord) {
        viewModelScope.launch {
            val result = repository.deleteIncome(income)
            if (result.isSuccess) {
                _userFeedback.value = "Income record removed"
            }
        }
    }

    // --- END OF DAY RECONCILIATION ---

    fun reconcileDay(
        date: String,
        totalSpent: Double,
        dailyAllowance: Double,
        remainingBalance: Double,
        notes: String = ""
    ) {
        viewModelScope.launch {
            val result = repository.recordDailySummary(
                date = date,
                totalSpentToday = totalSpent,
                allocatedDailyBudget = dailyAllowance,
                remainingDayBalance = remainingBalance,
                status = "matched",
                notes = notes
            )
            if (result.isSuccess) {
                _userFeedback.value = "Day $date reconciled and matched successfully!"
            } else {
                _userFeedback.value = "Failed to save daily summary"
            }
        }
    }

    // --- CATEGORY MANAGEMENT (ADMIN) ---

    fun addCategory(
        name: String,
        monthlyBudget: Double,
        notes: String = "",
        icon: String = "category",
        colorHex: String = "#1E88E5"
    ) {
        viewModelScope.launch {
            val result = repository.createCategory(name, monthlyBudget, notes, icon, colorHex)
            if (result.isSuccess) {
                _userFeedback.value = "Category '$name' added"
            }
        }
    }

    fun updateCategory(category: Category) {
        viewModelScope.launch {
            val result = repository.updateCategory(category)
            if (result.isSuccess) {
                _userFeedback.value = "Category updated"
            }
        }
    }

    fun deleteCategory(categoryId: String) {
        viewModelScope.launch {
            val result = repository.deleteCategory(categoryId)
            if (result.isSuccess) {
                _userFeedback.value = "Category deleted"
            }
        }
    }

    // --- INCOME STREAM MANAGEMENT (ADMIN) ---

    fun addIncomeSource(name: String, expectedAmount: Double, notes: String = "") {
        viewModelScope.launch {
            val result = repository.createIncomeSource(name, expectedAmount, notes)
            if (result.isSuccess) {
                _userFeedback.value = "Income stream '$name' created"
            }
        }
    }

    fun updateIncomeSource(source: IncomeSource) {
        viewModelScope.launch {
            val result = repository.updateIncomeSource(source)
            if (result.isSuccess) {
                _userFeedback.value = "Income stream updated"
            }
        }
    }

    fun deleteIncomeSource(sourceId: String) {
        viewModelScope.launch {
            val result = repository.deleteIncomeSource(sourceId)
            if (result.isSuccess) {
                _userFeedback.value = "Income stream deleted"
            }
        }
    }

    fun resetToDefaults() {
        viewModelScope.launch {
            repository.seedDefaultDataIfEmpty()
            _userFeedback.value = "Default categories and income streams restored"
        }
    }
}
