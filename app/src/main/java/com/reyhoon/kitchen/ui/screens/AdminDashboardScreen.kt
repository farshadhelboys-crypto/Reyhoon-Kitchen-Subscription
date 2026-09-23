package com.reyhoon.kitchen.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.reyhoon.kitchen.data.ApiConfig
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
    onNavigateToOrders: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedPeriod by remember { mutableStateOf("روزانه") }
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
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "خروج")
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
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (ApiConfig.isConfigured)
                        GreenMid.copy(alpha = 0.12f)
                    else
                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = if (ApiConfig.isConfigured)
                        "آنلاین: ${ApiConfig.baseUrl}"
                    else
                        "آدرس Worker خالی است — فایل ApiConfig.kt را پر کنید تا سفارش آنلاین و آلارم فعال شود.",
                    modifier = Modifier.padding(12.dp),
                    fontWeight = FontWeight.SemiBold
                )
            }

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
                StatCard("کل بدهی", AppRepository.formatPrice(totalDebt), "تومان", Modifier.weight(1f), MaterialTheme.colorScheme.error)
            }
            StatCard("کل اعتبار مشتریان", AppRepository.formatPrice(totalCredit), "بدهی آشپزخانه به مشتری", Modifier.fillMaxWidth(), GreenMid)

            Spacer(modifier = Modifier.height(8.dp))
            Text("عملیات سریع", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

            AdminActionButton(Icons.Filled.NotificationsActive, "سفارش‌های آنلاین + آلارم", "وضعیت: ثبت / آماده‌سازی / ارسال / تحویل", onNavigateToOrders)
            AdminActionButton(Icons.Filled.AddShoppingCart, "ثبت سفارش جدید", "سفارش دستی در آشپزخانه", onNavigateToNewOrder)
            AdminActionButton(Icons.Filled.RestaurantMenu, "مدیریت منوی محلی", "همگام با پنل HTML سرور", onNavigateToMenuManage)
            AdminActionButton(Icons.Filled.People, "مشتریان و بدهی", "افزودن مشتری و تسویه", onNavigateToCustomers)
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
            Text(subtitle, style = MaterialTheme.typography.bodySmall)
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
            Icon(icon, contentDescription = null, tint = GreenMid, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall)
            }
            Icon(Icons.Filled.ChevronLeft, contentDescription = null)
        }
    }
}
