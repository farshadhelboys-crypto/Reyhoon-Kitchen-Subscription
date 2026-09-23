package com.reyhoon.kitchen.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.reyhoon.kitchen.data.AppRepository
import com.reyhoon.kitchen.ui.theme.GreenPrimary
import com.reyhoon.kitchen.ui.theme.OrangeSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToMenu: () -> Unit,
    onNavigateToAddress: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val customer by AppRepository.currentCustomer

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("ریحون", fontWeight = FontWeight.Bold)
                },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "خروج")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = GreenPrimary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        modifier = modifier
    ) { padding ->
        if (customer == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("لطفاً دوباره وارد شوید")
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Welcome
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = GreenPrimary.copy(alpha = 0.12f))
            ) {
                Column(Modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "سلام ${customer!!.name} عزیز 👋",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier = Modifier.height(4.dp))
                    Text(
                        text = if (customer!!.debt > 0)
                            "بدهی فعلی: ${AppRepository.formatPrice(customer!!.debt)} تومان"
                        else
                            "بدهی ندارید ✓",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (customer!!.debt > 0) OrangeSecondary else GreenPrimary
                    )
                }
            }

            // Address card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                onClick = onNavigateToAddress
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.LocationOn, null, tint = OrangeSecondary, modifier = Modifier.size(28.dp))
                    Spacer(Modifier = Modifier.width(12.dp))
                    Column(Modifier = Modifier.weight(1f)) {
                        Text("آدرس تحویل", fontWeight = FontWeight.SemiBold)
                        Text(
                            customer!!.address.fullAddress(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                        )
                    }
                    Icon(Icons.Default.ChevronLeft, null)
                }
            }

            // Quick actions
            Button(
                onClick = onNavigateToMenu,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.RestaurantMenu, null)
                Spacer(Modifier = Modifier.width(8.dp))
                Text("مشاهده منوی غذا", fontWeight = FontWeight.Medium)
            }

            OutlinedButton(
                onClick = onNavigateToAddress,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.EditLocation, null)
                Spacer(Modifier = Modifier.width(8.dp))
                Text("ویرایش آدرس")
            }

            // Debt info
            if (customer!!.debt > 0) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f))
                ) {
                    Column(Modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AccountBalanceWallet, null, tint = MaterialTheme.colorScheme.error)
                            Spacer(Modifier = Modifier.width(8.dp))
                            Text("وضعیت بدهی", fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(Modifier = Modifier.height(8.dp))
                        Text(
                            "مبلغ باقی‌مانده: ${AppRepository.formatPrice(customer!!.debt)} تومان",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            "برای تسویه با ادمین هماهنگ کنید.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}
