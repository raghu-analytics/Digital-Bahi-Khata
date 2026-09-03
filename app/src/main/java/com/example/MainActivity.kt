package com.example

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.core.content.FileProvider
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.db.AppDatabase
import com.example.data.model.Transaction
import com.example.ui.navigation.Screen
import com.example.ui.screens.AppSettingsScreen
import com.example.ui.screens.CustomerDetailScreen
import com.example.ui.screens.CustomerListScreen
import com.example.ui.screens.DataManagementScreen
import com.example.ui.screens.HelpFaqScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MasterInfoScreen
import com.example.ui.screens.MonthlyReportScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.ShopDetailsScreen
import com.example.ui.screens.TransactionEntryScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.BahiKhataViewModel
import com.example.ui.viewmodel.UiEvent
import kotlinx.coroutines.flow.collectLatest
import java.io.File

class MainActivity : ComponentActivity() {

    private val viewModel: BahiKhataViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                val navController = rememberNavController()

                LaunchedEffect(Unit) {
                    viewModel.eventFlow.collectLatest { event ->
                        when (event) {
                            is UiEvent.ShowToast -> {
                                Toast.makeText(this@MainActivity, event.message, Toast.LENGTH_SHORT).show()
                            }
                            is UiEvent.PdfGenerated -> {
                                shareFile(event.file, "application/pdf", event.title)
                            }
                            is UiEvent.ExcelGenerated -> {
                                shareFile(event.file, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "बही-खाता 3-Sheets Excel (.xlsx) रिपोर्ट")
                            }
                            is UiEvent.BackupCompleted -> {
                                shareFile(event.file, "application/octet-stream", "बही-खाता डेटाबेस बैकअप")
                            }
                            is UiEvent.RestoreCompleted -> {
                                // Handled via toast
                            }
                            is UiEvent.RestartApp -> {
                                AppDatabase.resetDatabaseInstance()
                                val intent = Intent(this@MainActivity, MainActivity::class.java).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                }
                                startActivity(intent)
                                finish()
                            }
                        }
                    }
                }

                val navigateHome: () -> Unit = {
                    if (!navController.popBackStack(Screen.Home.route, inclusive = false)) {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = Screen.Home.route,
                    modifier = Modifier.fillMaxSize()
                ) {
                    // 1. HOME DASHBOARD
                    composable(Screen.Home.route) {
                        HomeScreen(
                            viewModel = viewModel,
                            onNavigateToCustomerDetail = { customerId ->
                                navController.navigate(Screen.CustomerDetail.createRoute(customerId))
                            },
                            onNavigateToTransactionEntry = { customerId, type ->
                                navController.navigate(Screen.TransactionEntry.createRoute(customerId, type))
                            },
                            onNavigateToMonthlyReport = {
                                navController.navigate(Screen.MonthlyReport.route)
                            },
                            onNavigateToCustomerList = {
                                navController.navigate(Screen.CustomerList.route)
                            },
                            onNavigateToSettings = {
                                navController.navigate(Screen.Settings.route)
                            },
                            onNavigateToHelpFaq = {
                                navController.navigate(Screen.HelpFaq.route)
                            }
                        )
                    }

                    // 2. CUSTOMER DETAIL
                    composable(
                        route = Screen.CustomerDetail.route,
                        arguments = listOf(
                            navArgument("customerId") { type = NavType.LongType }
                        )
                    ) { backStackEntry ->
                        val customerId = backStackEntry.arguments?.getLong("customerId") ?: 0L
                        CustomerDetailScreen(
                            customerId = customerId,
                            viewModel = viewModel,
                            onNavigateBack = { navController.popBackStack() },
                            onNavigateHome = navigateHome,
                            onNavigateToTransactionEntry = { cId, type ->
                                navController.navigate(Screen.TransactionEntry.createRoute(cId, type))
                            },
                            onNavigateToSettings = {
                                navController.navigate(Screen.Settings.route)
                            },
                            onNavigateToMonthlyReport = {
                                navController.navigate(Screen.MonthlyReport.route)
                            }
                        )
                    }

                    // 3. TRANSACTION ENTRY
                    composable(
                        route = Screen.TransactionEntry.route,
                        arguments = listOf(
                            navArgument("customerId") {
                                type = NavType.StringType
                                nullable = true
                                defaultValue = null
                            },
                            navArgument("type") {
                                type = NavType.StringType
                                defaultValue = Transaction.TYPE_CREDIT
                            }
                        )
                    ) { backStackEntry ->
                        val custIdString = backStackEntry.arguments?.getString("customerId")
                        val custId = custIdString?.toLongOrNull()
                        val type = backStackEntry.arguments?.getString("type") ?: Transaction.TYPE_CREDIT

                        TransactionEntryScreen(
                            preselectedCustomerId = custId,
                            initialType = type,
                            viewModel = viewModel,
                            onNavigateBack = { navController.popBackStack() },
                            onNavigateHome = navigateHome,
                            onNavigateToCustomerDetail = { cId ->
                                navController.navigate(Screen.CustomerDetail.createRoute(cId)) {
                                    popUpTo(Screen.TransactionEntry.route) { inclusive = true }
                                }
                            }
                        )
                    }

                    // 4. MONTHLY REPORT
                    composable(Screen.MonthlyReport.route) {
                        MonthlyReportScreen(
                            viewModel = viewModel,
                            onNavigateBack = { navController.popBackStack() },
                            onNavigateHome = navigateHome,
                            onNavigateToCustomerDetail = { customerId ->
                                navController.navigate(Screen.CustomerDetail.createRoute(customerId))
                            }
                        )
                    }

                    // 4.1 GRAHAK SUCHI (CUSTOMER DIRECTORY LIST)
                    composable(Screen.CustomerList.route) {
                        CustomerListScreen(
                            viewModel = viewModel,
                            onNavigateBack = { navController.popBackStack() },
                            onNavigateHome = navigateHome
                        )
                    }

                    // 5. SETTINGS MAIN MENU
                    composable(Screen.Settings.route) {
                        SettingsScreen(
                            onNavigateBack = { navController.popBackStack() },
                            onNavigateHome = navigateHome,
                            onNavigateToShopDetails = {
                                navController.navigate(Screen.ShopDetails.route)
                            },
                            onNavigateToDataManagement = {
                                navController.navigate(Screen.DataManagement.route)
                            },
                            onNavigateToHelpFaq = {
                                navController.navigate(Screen.HelpFaq.route)
                            },
                            onNavigateToAppSettings = {
                                navController.navigate(Screen.AppSettings.route)
                            },
                            onNavigateToMasterInfo = {
                                navController.navigate(Screen.MasterInfo.route)
                            }
                        )
                    }

                    // 5.1 SHOP & OWNER DETAILS
                    composable(Screen.ShopDetails.route) {
                        ShopDetailsScreen(
                            viewModel = viewModel,
                            onNavigateBack = { navController.popBackStack() },
                            onNavigateHome = navigateHome
                        )
                    }

                    // 5.2 DATA MANAGEMENT
                    composable(Screen.DataManagement.route) {
                        DataManagementScreen(
                            viewModel = viewModel,
                            onNavigateBack = { navController.popBackStack() },
                            onNavigateHome = navigateHome
                        )
                    }

                    // 5.3 APP SETTINGS (PLACEHOLDER)
                    composable(Screen.AppSettings.route) {
                        AppSettingsScreen(
                            onNavigateBack = { navController.popBackStack() },
                            onNavigateHome = navigateHome
                        )
                    }

                    // 5.4 MASTER INFO
                    composable(Screen.MasterInfo.route) {
                        MasterInfoScreen(
                            viewModel = viewModel,
                            onNavigateBack = { navController.popBackStack() },
                            onNavigateHome = navigateHome
                        )
                    }

                    // 6. HELP & FAQ
                    composable(Screen.HelpFaq.route) {
                        HelpFaqScreen(
                            viewModel = viewModel,
                            onNavigateBack = { navController.popBackStack() },
                            onNavigateHome = navigateHome
                        )
                    }
                }
            }
        }
    }

    private fun shareFile(file: File, mimeType: String, subject: String) {
        try {
            val uri = FileProvider.getUriForFile(
                this,
                "${applicationContext.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, subject)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, subject))
        } catch (e: Exception) {
            Toast.makeText(this, "फ़ाइल खोलने में त्रुटि: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }
}

