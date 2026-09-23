package com.reyhoon.kitchen.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.reyhoon.kitchen.data.AppRepository
import com.reyhoon.kitchen.ui.screens.*

object Routes {
    const val ENTER = "enter"
    const val HOME = "home"
    const val MENU = "menu"
    const val ADDRESS = "address"
    const val ADMIN = "admin"
    const val ADMIN_MENU = "admin_menu"
    const val ADMIN_CUSTOMERS = "admin_customers"
    const val NEW_ORDER = "new_order"
    const val KITCHEN_ORDERS = "kitchen_orders"
}

@Composable
fun ReyhoonNavGraph(modifier: Modifier = Modifier) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.ENTER,
        modifier = modifier
    ) {
        composable(Routes.ENTER) {
            EnterCodeScreen(
                onCustomerEntered = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.ENTER) { inclusive = true }
                    }
                },
                onAdminEntered = {
                    navController.navigate(Routes.ADMIN) {
                        popUpTo(Routes.ENTER) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.HOME) {
            HomeScreen(
                onNavigateToMenu = { navController.navigate(Routes.MENU) },
                onNavigateToAddress = { navController.navigate(Routes.ADDRESS) },
                onLogout = {
                    AppRepository.currentCustomer.value = null
                    navController.navigate(Routes.ENTER) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.MENU) {
            MenuScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.ADDRESS) {
            AddressScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.ADMIN) {
            AdminDashboardScreen(
                onNavigateToMenuManage = { navController.navigate(Routes.ADMIN_MENU) },
                onNavigateToCustomers = { navController.navigate(Routes.ADMIN_CUSTOMERS) },
                onNavigateToNewOrder = { navController.navigate(Routes.NEW_ORDER) },
                onNavigateToOrders = { navController.navigate(Routes.KITCHEN_ORDERS) },
                onLogout = {
                    AppRepository.isAdmin.value = false
                    navController.navigate(Routes.ENTER) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.ADMIN_MENU) {
            AdminMenuScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.ADMIN_CUSTOMERS) {
            AdminCustomersScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.NEW_ORDER) {
            NewOrderScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.KITCHEN_ORDERS) {
            KitchenOrdersScreen(onBack = { navController.popBackStack() })
        }
    }
}
