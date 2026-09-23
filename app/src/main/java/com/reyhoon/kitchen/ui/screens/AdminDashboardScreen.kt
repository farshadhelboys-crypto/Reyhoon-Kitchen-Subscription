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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.reyhoon.kitchen.data.AppRepository
import com.reyhoon.kitchen.ui.components.ReyhoonLogo
import com.reyhoon.kitchen.ui.theme.GreenMid
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
    // Recompose when orders/customers change
    val orderCount = AppRepository.orders.size
    val customerCount = AppRepository.customers.size
    val summary = remember(selectedPeriod, orderCount) {
        AppRepository.getSalesSummary(selectedPeriod)
    }
    val totalDebt = remember(orderCount, customerCount) { AppRepository.totalCustomerDebt() }
    val totalCredit = remember(orderCount, customerCount) { AppRepository.totalCustomerCredit() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ReyhoonLogo(size = 34.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("پنل ادمین ریحون", fontWeight = FontWeight.Bold)
                    }
                },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.AutoMirrored.Filled.Logout, "خروج")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = GreenMid,
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

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("روزانه", "هفتگی", "ماهانه").forEach { period ->
                    FilterChip(
                        selected = selectedPeriod == period,
                        onClick = { selectedPeriod = period },
                        label = { Text(period) }
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard("فروش کل", AppRepository.formatPrice(summary.totalSales), "${summary.orderCount} سفارش", Modifier.weight(1f), GreenMid)
                StatCard("دریافتی", AppRepository.formatPrice(summary.totalPaid), "تومان", Modifier.weight(1f), OrangeSecondary)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard("بدهی دوره", AppRepository.formatPrice(summary.totalDebt), "تومان", Modifier.weight(1f), MaterialTheme.colorScheme.error)
                StatCard("کل بدهی مشتریان", AppRepository.formatPrice(totalDebt), "تومان", Modifier.weight(1f), MaterialTheme.colorScheme.error)
            }

            StatCard(
                title = "کل اعتبار مشتریان",
                value = AppRepository.formatPrice(totalCredit),
                subtitle = "بدهی آشپزخانه به مشتریان",
                modifier = Modifier.fillMaxWidth(),
                color = GreenMid
            )

            Spacer(modifier = Modifier.height(8.dp))
            Text("عملیات سریع", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

            AdminActionButton(Icons.Default.AddShoppingCart, "ثبت سفارش جدید", "سفارش + پرداخت جزئی / اعتبار", onNavigateToNewOrder)
            AdminActionButton(Icons.Default.RestaurantMenu, "مدیریت منوی غذا", "افزودن، ویرایش، حذف آیتم", onNavigateToMenuManage)
            AdminActionButton(Icons.Default.People, "مشتریان و بدهی", "افزودن مشتری، آدرس، تسویه و اعتبار", onNavigateToCustomers)
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    color: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.12f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium, color = color)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        }
    }
}

@Composable
private fun AdminActionButton(
    icon: ImageVector,
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
            Icon(icon, null, tint = GreenMid, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f))
            }
            Icon(Icons.Default.ChevronLeft, null)
        }
    }
}
