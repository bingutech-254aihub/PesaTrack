package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.Category
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class BudgetRepositoryRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun createCategory_authenticatedOwner_createsCategory() = runBlocking {
    signInTestUser(ALICE_EMAIL)
    val repository = BudgetRepository(firestore, auth)

    val result = withTimeout(DEFAULT_TIMEOUT_MS) {
      repository.createCategory(
        name = "Rent",
        monthlyBudget = 20000.0,
        notes = "15000+4000+1000"
      )
    }
    assertTrue("Category creation should succeed", result.isSuccess)
    val catId = result.getOrThrow()
    assertTrue(catId.isNotEmpty())
  }

  @Test
  fun observeExpenses_authenticatedOwner_emitsExpenses() = runBlocking {
    signInTestUser(ALICE_EMAIL)
    val repository = BudgetRepository(firestore, auth)

    val expResult = withTimeout(DEFAULT_TIMEOUT_MS) {
      repository.recordExpense(
        categoryId = "cat_test_1",
        categoryName = "Daily breakfast at Naivas",
        amount = 80.0,
        date = "2026-10-17"
      )
    }
    assertTrue("Expense recording should succeed", expResult.isSuccess)
    val expId = expResult.getOrThrow()

    val emitted = withTimeout(FLOW_TIMEOUT_MS) {
      repository.observeExpenses().first { list -> list.any { it.id == expId } }
    }
    assertTrue(emitted.any { it.id == expId && it.amount == 80.0 })
  }

  @Test
  fun crossUserAccess_bobCannotReadAliceCategory() = runBlocking {
    val aliceUid = signInTestUser(ALICE_EMAIL)
    val aliceRepo = BudgetRepository(firestore, auth)
    val catId = withTimeout(DEFAULT_TIMEOUT_MS) {
      aliceRepo.createCategory("Private Mwembe", 8000.0).getOrThrow()
    }

    signInTestUser(BOB_EMAIL)
    try {
      withTimeout(DEFAULT_TIMEOUT_MS) {
        firestore.collection("users").document(aliceUid).collection("categories").document(catId).get().await()
      }
      fail("Expected FirebaseFirestoreException for cross-user read")
    } catch (e: FirebaseFirestoreException) {
      assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, e.code)
    }
  }

  @Test
  fun unauthenticatedAccess_failsWithPermissionDenied() = runBlocking {
    val aliceUid = signInTestUser(ALICE_EMAIL)
    val aliceRepo = BudgetRepository(firestore, auth)
    val catId = withTimeout(DEFAULT_TIMEOUT_MS) {
      aliceRepo.createCategory("Electricity", 4000.0).getOrThrow()
    }

    auth.signOut()
    try {
      withTimeout(DEFAULT_TIMEOUT_MS) {
        firestore.collection("users").document(aliceUid).collection("categories").document(catId).get().await()
      }
      fail("Expected FirebaseFirestoreException for unauthenticated read")
    } catch (e: FirebaseFirestoreException) {
      assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, e.code)
    }
  }

  private companion object {
    const val ALICE_EMAIL = "alice@example.com"
    const val BOB_EMAIL = "bob@example.com"
    const val DEFAULT_TIMEOUT_MS = 5000L
    const val FLOW_TIMEOUT_MS = 5000L
  }
}
