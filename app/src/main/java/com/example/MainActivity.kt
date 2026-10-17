package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.repository.BudgetRepository
import com.example.ui.BudgetViewModel
import com.example.ui.admin.AdminPanelScreen
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.signOut
import com.example.ui.components.AddExpenseDialog
import com.example.ui.components.AddIncomeDialog
import com.example.ui.daily.DailyReconciliationScreen
import com.example.ui.dashboard.DashboardScreen
import com.example.ui.report.MonthlyReportScreen
import com.example.ui.theme.PesaTrackTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

enum class AppScreen(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val testTag: String) {
    DASHBOARD("Dashboard", Icons.Default.Dashboard, "nav_dashboard"),
    DAILY("Daily Match", Icons.Default.Today, "nav_daily"),
    REPORTS("Reports", Icons.Default.Assessment, "nav_reports"),
    ADMIN("Admin Panel", Icons.Default.AdminPanelSettings, "nav_admin")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PesaTrackTheme {
                PesaTrackApp()
            }
        }
    }
}

@Composable
fun PesaTrackApp() {
    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose {
            Firebase.auth.removeAuthStateListener(listener)
        }
    }

    if (currentUser == null) {
        AuthScreen(
            onAuthSuccess = {
                currentUser = Firebase.auth.currentUser
            }
        )
    } else {
        MainBudgetApp(currentUserId = currentUser!!.uid)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainBudgetApp(currentUserId: String) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    val repository = remember(currentUserId) { BudgetRepository(context) }
    val viewModel = remember(currentUserId) { BudgetViewModel(repository) }

    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var currentScreen by remember { mutableStateOf(AppScreen.DASHBOARD) }

    var showAddExpenseDialog by remember { mutableStateOf(false) }
    var showAddIncomeDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    // Display feedback messages
    LaunchedEffect(state.userFeedbackMessage) {
        state.userFeedbackMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearFeedback()
        }
    }

    // Handle back button on secondary screens
    if (currentScreen != AppScreen.DASHBOARD) {
        BackHandler {
            currentScreen = AppScreen.DASHBOARD
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "PesaTrack",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar {
                AppScreen.values().forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.title) },
                        label = { Text(screen.title) },
                        selected = currentScreen == screen,
                        onClick = { currentScreen = screen },
                        modifier = Modifier.testTag(screen.testTag)
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (state.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(48.dp))
                }
            } else {
                when (currentScreen) {
                    AppScreen.DASHBOARD -> DashboardScreen(
                        state = state,
                        onAddExpenseClick = { showAddExpenseDialog = true },
                        onAddIncomeClick = { showAddIncomeDialog = true },
                        onCategoryClick = { /* Could open filtered view or add expense */
                            showAddExpenseDialog = true
                        },
                        onReconcileTodayClick = {
                            currentScreen = AppScreen.DAILY
                        }
                    )
                    AppScreen.DAILY -> DailyReconciliationScreen(
                        state = state,
                        onReconcileDay = { date, spent, allowance, remaining, notes ->
                            viewModel.reconcileDay(date, spent, allowance, remaining, notes)
                        },
                        onDeleteExpense = { id ->
                            viewModel.deleteExpense(id)
                        }
                    )
                    AppScreen.REPORTS -> MonthlyReportScreen(
                        state = state,
                        onMonthChange = { month ->
                            viewModel.setSelectedMonth(month)
                        },
                        onDeleteExpense = { id ->
                            viewModel.deleteExpense(id)
                        }
                    )
                    AppScreen.ADMIN -> AdminPanelScreen(
                        state = state,
                        onAddCategory = { name, budget, notes, icon, colorHex ->
                            viewModel.addCategory(name, budget, notes, icon, colorHex)
                        },
                        onUpdateCategory = { cat ->
                            viewModel.updateCategory(cat)
                        },
                        onDeleteCategory = { id ->
                            viewModel.deleteCategory(id)
                        },
                        onAddIncomeSource = { name, amount, notes ->
                            viewModel.addIncomeSource(name, amount, notes)
                        },
                        onUpdateIncomeSource = { src ->
                            viewModel.updateIncomeSource(src)
                        },
                        onDeleteIncomeSource = { id ->
                            viewModel.deleteIncomeSource(id)
                        },
                        onResetDefaults = {
                            viewModel.resetToDefaults()
                        },
                        onSignOut = {
                            signOut(
                                context = context,
                                credentialManager = credentialManager,
                                onSignOutComplete = {
                                    // listener in AuthScreen / PesaTrackApp catches state
                                },
                                scope = coroutineScope
                            )
                        }
                    )
                }
            }
        }
    }

    if (showAddExpenseDialog) {
        AddExpenseDialog(
            categories = state.categories,
            incomeSources = state.incomeSources,
            onDismiss = { showAddExpenseDialog = false },
            onConfirm = { catId, catName, amount, date, sourceId, sourceName, notes ->
                viewModel.addExpense(
                    categoryId = catId,
                    categoryName = catName,
                    amount = amount,
                    date = date,
                    fromIncomeSourceId = sourceId,
                    fromIncomeSourceName = sourceName,
                    notes = notes
                )
                showAddExpenseDialog = false
            }
        )
    }

    if (showAddIncomeDialog) {
        AddIncomeDialog(
            incomeSources = state.incomeSources,
            onDismiss = { showAddIncomeDialog = false },
            onConfirm = { sourceName, amount, date, sourceId, notes ->
                viewModel.recordIncome(
                    sourceName = sourceName,
                    amount = amount,
                    date = date,
                    incomeSourceId = sourceId,
                    notes = notes
                )
                showAddIncomeDialog = false
            }
        )
    }
}
