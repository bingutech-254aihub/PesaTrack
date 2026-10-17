package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.Category
import com.example.data.model.DailySummary
import com.example.data.model.DefaultBudgetData
import com.example.data.model.ExpenseRecord
import com.example.data.model.IncomeRecord
import com.example.data.model.IncomeSource
import com.example.data.model.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class BudgetRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    // --- CATEGORIES ---

    fun observeCategories(): Flow<List<Category>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/categories"
        emitAll(
            db.collection("users").document(uid).collection("categories")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(Category::class.java).filter { !it.isArchived }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun createCategory(
        name: String,
        monthlyBudget: Double,
        notes: String = "",
        icon: String = "category",
        colorHex: String = "#1E88E5"
    ): Result<String> {
        return try {
            val uid = requireUserId()
            val colRef = db.collection("users").document(uid).collection("categories")
            val docRef = colRef.document(UUID.randomUUID().toString())
            val payload = mapOf(
                "userId" to uid,
                "name" to name.trim(),
                "monthlyBudget" to monthlyBudget,
                "notes" to notes.trim(),
                "icon" to icon,
                "colorHex" to colorHex,
                "isArchived" to false,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "users/uid/categories")
            Result.failure(e)
        }
    }

    suspend fun updateCategory(category: Category): Result<Unit> {
        return try {
            val uid = requireUserId()
            require(category.id.isNotBlank()) { "Category ID must not be blank" }
            val docRef = db.collection("users").document(uid).collection("categories").document(category.id)
            val updates = mapOf(
                "name" to category.name.trim(),
                "monthlyBudget" to category.monthlyBudget,
                "notes" to category.notes.trim(),
                "icon" to category.icon,
                "colorHex" to category.colorHex,
                "isArchived" to category.isArchived,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/uid/categories/${category.id}")
            Result.failure(e)
        }
    }

    suspend fun deleteCategory(categoryId: String): Result<Unit> {
        return try {
            val uid = requireUserId()
            val docRef = db.collection("users").document(uid).collection("categories").document(categoryId)
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "users/uid/categories/$categoryId")
            Result.failure(e)
        }
    }

    // --- INCOME SOURCES ---

    fun observeIncomeSources(): Flow<List<IncomeSource>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/income_sources"
        emitAll(
            db.collection("users").document(uid).collection("income_sources")
                .snapshots()
                .map { snapshot -> snapshot.toObjects(IncomeSource::class.java) }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun createIncomeSource(
        name: String,
        expectedAmount: Double,
        notes: String = ""
    ): Result<String> {
        return try {
            val uid = requireUserId()
            val colRef = db.collection("users").document(uid).collection("income_sources")
            val docRef = colRef.document(UUID.randomUUID().toString())
            val payload = mapOf(
                "userId" to uid,
                "name" to name.trim(),
                "expectedAmount" to expectedAmount,
                "receivedAmount" to 0.0,
                "notes" to notes.trim(),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "users/uid/income_sources")
            Result.failure(e)
        }
    }

    suspend fun updateIncomeSource(source: IncomeSource): Result<Unit> {
        return try {
            val uid = requireUserId()
            val docRef = db.collection("users").document(uid).collection("income_sources").document(source.id)
            val updates = mapOf(
                "name" to source.name.trim(),
                "expectedAmount" to source.expectedAmount,
                "receivedAmount" to source.receivedAmount,
                "notes" to source.notes.trim(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/uid/income_sources/${source.id}")
            Result.failure(e)
        }
    }

    suspend fun deleteIncomeSource(sourceId: String): Result<Unit> {
        return try {
            val uid = requireUserId()
            val docRef = db.collection("users").document(uid).collection("income_sources").document(sourceId)
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "users/uid/income_sources/$sourceId")
            Result.failure(e)
        }
    }

    // --- INCOMES (DEPOSITS/RECEIVED) ---

    fun observeIncomes(): Flow<List<IncomeRecord>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/incomes"
        emitAll(
            db.collection("users").document(uid).collection("incomes")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(IncomeRecord::class.java).sortedByDescending { it.timestamp }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun recordIncome(
        sourceName: String,
        amount: Double,
        date: String,
        timestamp: Long = System.currentTimeMillis(),
        incomeSourceId: String = "",
        notes: String = ""
    ): Result<String> {
        return try {
            val uid = requireUserId()
            val colRef = db.collection("users").document(uid).collection("incomes")
            val docRef = colRef.document(UUID.randomUUID().toString())
            val payload = mutableMapOf<String, Any>(
                "userId" to uid,
                "sourceName" to sourceName.trim(),
                "amount" to amount,
                "date" to date,
                "timestamp" to timestamp,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (incomeSourceId.isNotBlank()) payload["incomeSourceId"] = incomeSourceId
            if (notes.isNotBlank()) payload["notes"] = notes.trim()

            docRef.set(payload).await()

            // Update receivedAmount on source if specified
            if (incomeSourceId.isNotBlank()) {
                val sourceRef = db.collection("users").document(uid).collection("income_sources").document(incomeSourceId)
                db.runTransaction { tx ->
                    val snapshot = tx.get(sourceRef)
                    val currentReceived = snapshot.getDouble("receivedAmount") ?: 0.0
                    tx.update(sourceRef, "receivedAmount", currentReceived + amount, "updatedAt", FieldValue.serverTimestamp())
                }.await()
            }

            Result.success(docRef.id)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "users/uid/incomes")
            Result.failure(e)
        }
    }

    suspend fun deleteIncome(income: IncomeRecord): Result<Unit> {
        return try {
            val uid = requireUserId()
            val docRef = db.collection("users").document(uid).collection("incomes").document(income.id)
            docRef.delete().await()

            if (income.incomeSourceId.isNotBlank()) {
                val sourceRef = db.collection("users").document(uid).collection("income_sources").document(income.incomeSourceId)
                db.runTransaction { tx ->
                    val snapshot = tx.get(sourceRef)
                    if (snapshot.exists()) {
                        val currentReceived = snapshot.getDouble("receivedAmount") ?: 0.0
                        val newAmount = (currentReceived - income.amount).coerceAtLeast(0.0)
                        tx.update(sourceRef, "receivedAmount", newAmount, "updatedAt", FieldValue.serverTimestamp())
                    }
                }.await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "users/uid/incomes/${income.id}")
            Result.failure(e)
        }
    }

    // --- EXPENSES (CENT LEVEL SPENDING IN REAL TIME) ---

    fun observeExpenses(): Flow<List<ExpenseRecord>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/expenses"
        emitAll(
            db.collection("users").document(uid).collection("expenses")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(ExpenseRecord::class.java).sortedByDescending { it.timestamp }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun recordExpense(
        categoryId: String,
        categoryName: String,
        amount: Double,
        date: String,
        fromIncomeSourceId: String = "",
        fromIncomeSourceName: String = "",
        timestamp: Long = System.currentTimeMillis(),
        notes: String = ""
    ): Result<String> {
        return try {
            val uid = requireUserId()
            val colRef = db.collection("users").document(uid).collection("expenses")
            val docRef = colRef.document(UUID.randomUUID().toString())
            val payload = mutableMapOf<String, Any>(
                "userId" to uid,
                "categoryId" to categoryId,
                "categoryName" to categoryName.trim(),
                "amount" to amount,
                "date" to date,
                "timestamp" to timestamp,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (fromIncomeSourceId.isNotBlank()) payload["fromIncomeSourceId"] = fromIncomeSourceId
            if (fromIncomeSourceName.isNotBlank()) payload["fromIncomeSourceName"] = fromIncomeSourceName.trim()
            if (notes.isNotBlank()) payload["notes"] = notes.trim()

            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "users/uid/expenses")
            Result.failure(e)
        }
    }

    suspend fun deleteExpense(expenseId: String): Result<Unit> {
        return try {
            val uid = requireUserId()
            val docRef = db.collection("users").document(uid).collection("expenses").document(expenseId)
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "users/uid/expenses/$expenseId")
            Result.failure(e)
        }
    }

    // --- DAILY SUMMARIES (END-OF-DAY MATCHING) ---

    fun observeDailySummaries(): Flow<List<DailySummary>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/daily_summaries"
        emitAll(
            db.collection("users").document(uid).collection("daily_summaries")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(DailySummary::class.java).sortedByDescending { it.date }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun recordDailySummary(
        date: String,
        totalSpentToday: Double,
        allocatedDailyBudget: Double,
        remainingDayBalance: Double,
        status: String = "matched",
        notes: String = ""
    ): Result<Unit> {
        return try {
            val uid = requireUserId()
            val docRef = db.collection("users").document(uid).collection("daily_summaries").document(date)
            val existing = docRef.get().await()
            val payload = mutableMapOf<String, Any>(
                "userId" to uid,
                "date" to date,
                "totalSpentToday" to totalSpentToday,
                "allocatedDailyBudget" to allocatedDailyBudget,
                "remainingDayBalance" to remainingDayBalance,
                "status" to status,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (notes.isNotBlank()) payload["notes"] = notes.trim()

            if (existing.exists()) {
                docRef.update(payload).await()
            } else {
                payload["createdAt"] = FieldValue.serverTimestamp()
                docRef.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "users/uid/daily_summaries/$date")
            Result.failure(e)
        }
    }

    // --- SEED DEFAULT KENYAN BUDGET DATA ---

    suspend fun seedDefaultDataIfEmpty(): Result<Unit> {
        return try {
            val uid = requireUserId()
            val catCol = db.collection("users").document(uid).collection("categories")
            val existingCats = catCol.limit(1).get().await()
            if (existingCats.isEmpty) {
                // Seed 14 categories from PDF
                for (cat in DefaultBudgetData.defaultCategories) {
                    val docRef = catCol.document(UUID.randomUUID().toString())
                    val payload = mapOf(
                        "userId" to uid,
                        "name" to cat.name,
                        "monthlyBudget" to cat.monthlyBudget,
                        "notes" to cat.notes,
                        "icon" to cat.icon,
                        "colorHex" to cat.colorHex,
                        "isArchived" to false,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    docRef.set(payload).await()
                }
            }

            val sourceCol = db.collection("users").document(uid).collection("income_sources")
            val existingSources = sourceCol.limit(1).get().await()
            if (existingSources.isEmpty) {
                // Seed 3 income streams
                for (src in DefaultBudgetData.defaultIncomeSources) {
                    val docRef = sourceCol.document(UUID.randomUUID().toString())
                    val payload = mapOf(
                        "userId" to uid,
                        "name" to src.name,
                        "expectedAmount" to src.expectedAmount,
                        "receivedAmount" to 0.0,
                        "notes" to src.notes,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    docRef.set(payload).await()
                }
            }

            // Seed user profile
            val profRef = db.collection("users").document(uid).collection("profile").document("settings")
            val profSnap = profRef.get().await()
            if (!profSnap.exists()) {
                profRef.set(
                    mapOf(
                        "userId" to uid,
                        "currency" to "KES",
                        "defaultMonthlyIncome" to 107500.0,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "users/uid/seed")
            Result.failure(e)
        }
    }
}
