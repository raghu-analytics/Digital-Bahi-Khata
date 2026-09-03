package com.example.ui.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object CustomerDetail : Screen("customer_detail/{customerId}") {
        fun createRoute(customerId: Long) = "customer_detail/$customerId"
    }
    object TransactionEntry : Screen("transaction_entry?customerId={customerId}&type={type}") {
        fun createRoute(customerId: Long? = null, type: String = "credit") =
            if (customerId != null) "transaction_entry?customerId=$customerId&type=$type"
            else "transaction_entry?type=$type"
    }
    object MonthlyReport : Screen("monthly_report")
    object CustomerList : Screen("customer_list")
    object Settings : Screen("settings")
    object HelpFaq : Screen("help_faq")
    object ShopDetails : Screen("settings/shop_details")
    object DataManagement : Screen("settings/data_management")
    object AppSettings : Screen("settings/app_settings")
    object MasterInfo : Screen("settings/master_info")
}
