package com.reyhoon.kitchen.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.reyhoon.kitchen.data.Address
import com.reyhoon.kitchen.data.Subscription
import com.reyhoon.kitchen.ui.screens.AddressScreen
import com.reyhoon.kitchen.ui.screens.EnterCodeScreen
import com.reyhoon.kitchen.ui.screens.HomeScreen
import com.reyhoon.kitchen.ui.screens.MenuScreen

object Routes {
    const val ENTER_CODE = "enter_code"
    const val HOME = "home"
    const val MENU = "menu"
    const val ADDRESS = "address"
}

@Composable
fun ReyhoonNavGraph(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    var currentSubscription by remember { mutableStateOf<Subscription?>(null) }

    NavHost(
        navController = navController,
        startDestination = Routes.ENTER_CODE,
        modifier = modifier
    ) {
        composable(Routes.ENTER_CODE) {
            EnterCodeScreen(
                onCodeVerified = { sub ->
                    currentSubscription = sub
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.ENTER_CODE) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.HOME) {
            currentSubscription?.let { sub ->
                HomeScreen(
                    subscription = sub,
                    onNavigateToMenu = { navController.navigate(Routes.MENU) },
                    onNavigateToAddress = { navController.navigate(Routes.ADDRESS) },
                    onLogout = {
                        currentSubscription = null
                        navController.navigate(Routes.ENTER_CODE) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
        }
        composable(Routes.MENU) {
            MenuScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.ADDRESS) {
            currentSubscription?.let { sub ->
                AddressScreen(
                    subscription = sub,
                    onSave = { newAddress: Address ->
                        currentSubscription = sub.copy(address = newAddress)
                        navController.popBackStack()
                    },
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
