package com.reyhoon.kitchen.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.reyhoon.kitchen.data.AppRepository
import com.reyhoon.kitchen.ui.theme.GreenPrimary
import com.reyhoon.kitchen.ui.theme.OrangeSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    onNavigateToMenuManage: () -> Unit,
    onNavigateToCustomers: () -> Unit,
    onNavigateToNewOrder: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedPeriod by remember { mutableStateOf("روزانه") }
    val summary = remember(selectedPeriod, AppRepository.orders.size) {
        AppRepository.getSalesSummary(selectedPeriod)
    }
    val totalDebt = AppRepository.totalCustomerDebt()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("پنل ادمین ریحون", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.AutoMirrored.Filled.Logout, "خروج")
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("گزارش فروش", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

            // Period chips
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("روزانه", "هفتگی", "ماهانه").forEach { period ->
                    FilterChip(
                        selected = selectedPeriod == period,
                        onClick = { selectedPeriod = period },
                        label = { Text(period) }
                    )
                }
            }

            // Sales cards
            Row(Modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard(
                    title = "فروش کل",
                    value = AppRepository.formatPrice(summary.totalSales),
                    subtitle = "${summary.orderCount} سفارش",
                    modifier = Modifier.weight(1f),
                    color = GreenPrimary
                )
                StatCard(
                    title = "دریافتی",
                    value = AppRepository.formatPrice(summary.totalPaid),
                    subtitle = "تومان",
                    modifier = Modifier.weight(1f),
                    color = OrangeSecondary
                )
            }

            Row(Modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard(
                    title = "بدهی دوره",
                    value = AppRepository.formatPrice(summary.totalDebt),
                    subtitle = "تومان",
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.error
                )
                StatCard(
                    title = "کل بدهی مشتریان",
                    value = AppRepository.formatPrice(totalDebt),
                    subtitle = "تومان",
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text("عملیات سریع", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

            AdminActionButton(
                icon = Icons.Default.AddShoppingCart,
                title = "ثبت سفارش جدید",
                subtitle = "سفارش + پرداخت جزئی / بدهی",
                onClick = onNavigateToNewOrder
            )
            AdminActionButton(
                icon = Icons.Default.RestaurantMenu,
                title = "مدیریت منوی غذا",
                subtitle = "افزودن، ویرایش، حذف آیتم",
                onClick = onNavigateToMenuManage
            )
            AdminActionButton(
                icon = Icons.Default.People,
                title = "مشتریان و بدهی",
                subtitle = "افزودن مشتری، آدرس، تسویه بدهی",
                onClick = onNavigateToCustomers
            )
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    color: androidx.compose.ui.graphics.Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.12f))
    ) {
        Column(Modifier = Modifier.padding(14.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium, color = color)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        }
    }
}

@Composable
private fun AdminActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = GreenPrimary, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f))
            }
            Icon(Icons.Default.ChevronLeft, null)
        }
    }
}
