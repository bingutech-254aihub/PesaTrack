package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class Category(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val monthlyBudget: Double = 0.0,
    val notes: String = "",
    val icon: String = "category",
    val colorHex: String = "#1E88E5",
    val isArchived: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class IncomeSource(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val expectedAmount: Double = 0.0,
    val receivedAmount: Double = 0.0,
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class IncomeRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val incomeSourceId: String = "",
    val sourceName: String = "",
    val amount: Double = 0.0,
    val date: String = "", // YYYY-MM-DD
    val timestamp: Long = 0L,
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ExpenseRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val categoryId: String = "",
    val categoryName: String = "",
    val amount: Double = 0.0,
    val fromIncomeSourceId: String = "",
    val fromIncomeSourceName: String = "",
    val date: String = "", // YYYY-MM-DD
    val timestamp: Long = 0L,
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class DailySummary(
    @DocumentId val id: String = "", // YYYY-MM-DD
    val userId: String = "",
    val date: String = "",
    val totalSpentToday: Double = 0.0,
    val allocatedDailyBudget: Double = 0.0,
    val remainingDayBalance: Double = 0.0,
    val status: String = "unmatched", // "matched" or "unmatched"
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val currency: String = "KES",
    val defaultMonthlyIncome: Double = 107500.0,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

// Default seed categories based on the user's brief & PDF
object DefaultBudgetData {
    val defaultCategories = listOf(
        Category(
            name = "Rent",
            monthlyBudget = 20000.0,
            notes = "15000+4000+1000",
            icon = "home",
            colorHex = "#1E88E5"
        ),
        Category(
            name = "Rent tax",
            monthlyBudget = 3000.0,
            notes = "excludes tudor 2nd floor & stadium",
            icon = "receipt_long",
            colorHex = "#3949AB"
        ),
        Category(
            name = "Maid",
            monthlyBudget = 9000.0,
            notes = "Monthly helper salary",
            icon = "cleaning_services",
            colorHex = "#00ACC1"
        ),
        Category(
            name = "Internet",
            monthlyBudget = 2000.0,
            notes = "rubbish internet",
            icon = "wifi",
            colorHex = "#00897B"
        ),
        Category(
            name = "Tuktuk",
            monthlyBudget = 8670.0,
            notes = "((((100+walk+(walk+50)+(50+walk)+(walk+50)+100+50))x5days)x52weeks)/12months)",
            icon = "two_wheeler",
            colorHex = "#43A047"
        ),
        Category(
            name = "Mwembe",
            monthlyBudget = 8000.0,
            notes = "Mwembe expenses",
            icon = "store",
            colorHex = "#7CB342"
        ),
        Category(
            name = "DTB",
            monthlyBudget = 45000.0,
            notes = "Diamond Trust Bank loan/repayment",
            icon = "account_balance",
            colorHex = "#C0CA33"
        ),
        Category(
            name = "Electricity",
            monthlyBudget = 4000.0,
            notes = "KPLC power tokens",
            icon = "bolt",
            colorHex = "#FBC02D"
        ),
        Category(
            name = "EMR/Hosting/Google",
            monthlyBudget = 4000.0,
            notes = "EMR, Cloud hosting, and Google Workspace",
            icon = "cloud",
            colorHex = "#FB8C00"
        ),
        Category(
            name = "Petrol",
            monthlyBudget = 2000.0,
            notes = "Vehicle fuel",
            icon = "local_gas_station",
            colorHex = "#F4511E"
        ),
        Category(
            name = "Insurance",
            monthlyBudget = 1500.0,
            notes = "Monthly premium allocation",
            icon = "health_and_safety",
            colorHex = "#6D4C41"
        ),
        Category(
            name = "Monthly water instalment",
            monthlyBudget = 2000.0,
            notes = "The tudor outstanding water bill",
            icon = "water_drop",
            colorHex = "#039BE5"
        ),
        Category(
            name = "Daily breakfast at Naivas",
            monthlyBudget = 1040.0,
            notes = "(((80*3)*52)/12)",
            icon = "restaurant",
            colorHex = "#E53935"
        ),
        Category(
            name = "Cooking gas",
            monthlyBudget = 2900.0,
            notes = "Gas refill cylinder",
            icon = "propane_tank",
            colorHex = "#8E24AA"
        )
    )

    val defaultIncomeSources = listOf(
        IncomeSource(
            name = "Income Stream 1",
            expectedAmount = 17500.0,
            notes = "Fixed salary / stipend"
        ),
        IncomeSource(
            name = "Income Stream 2",
            expectedAmount = 40000.0,
            notes = "Primary client or contract"
        ),
        IncomeSource(
            name = "Income Stream 3",
            expectedAmount = 50000.0,
            notes = "Main business or retainer"
        )
    )
}
