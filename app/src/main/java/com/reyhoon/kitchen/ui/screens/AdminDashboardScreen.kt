package com.reyhoon.kitchen.ui.screens

import androidx.compose.foundation.layout.*
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
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.reyhoon.kitchen.data.ApiClient
import com.reyhoon.kitchen.data.ApiConfig
import com.reyhoon.kitchen.data.AppRepository
import com.reyhoon.kitchen.ui.components.ReyhoonLogo
import com.reyhoon.kitchen.ui.theme.GreenMid
import com.reyhoon.kitchen.ui.theme.OrangeSecondary
import com.reyhoon.kitchen.util.NotificationHelper
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    onNavigateToMenuManage: () -> Unit,
    onNavigateToCustomers: () -> Unit,
    onNavigateToNewOrder: () -> Unit,
    onNavigateToOrders: () -> Unit,
    onNavigateToRatings: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedPeriod by remember { mutableStateOf("روزانه") }
    val orderCount = AppRepository.orders.size
    val customerCount = AppRepository.customers.size
    val summary = remember(selectedPeriod, orderCount) {
        AppRepository.getSalesSummary(selectedPeriod)
    }
    val totalDebt = remember(orderCount, customerCount) { AppRepository.totalCustomerDebt() }
    val totalCredit = remember(orderCount, customerCount) { AppRepository.totalCustomerCredit() }
    var lastSeenAt by remember { mutableStateOf(System.currentTimeMillis() - 30_000) }
    var liveAlert by remember { mutableStateOf<String?>(null) }

    // حتی وقتی داخل داشبورد هستید سفارش جدید آلارم می‌دهد
    LaunchedEffect(Unit) {
        NotificationHelper.ensureChannels(context)
        while (true) {
            if (ApiConfig.isConfigured) {
                val fresh = ApiClient.fetchNewOrders(lastSeenAt)
                if (fresh.isNotEmpty()) {
                    val name = fresh.first().customerName
                    liveAlert = "سفارش جدید دارید! ${fresh.size} — $name"
                    NotificationHelper.notifyNewOrder(context, fresh.size, name)
                    lastSeenAt = maxOf(lastSeenAt, fresh.maxOf { it.createdAt })
                }
            }
            delay(8_000)
        }
    }

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
            liveAlert?.let { msg ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = OrangeSecondary.copy(alpha = 0.2f)),
                    onClick = onNavigateToOrders
                ) {
                    Text(
                        msg,
                        modifier = Modifier.padding(12.dp),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

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
                        "آنلاین: ${ApiConfig.baseUrl}\nآلارم سفارش هر ۸ ثانیه فعال است"
                    else
                        "آدرس Worker تنظیم نشده",
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

            AdminActionButton(Icons.Filled.NotificationsActive, "سفارش‌های آنلاین + آلارم", "نوتیف و صدا هنگام سفارش جدید", onNavigateToOrders)
            AdminActionButton(Icons.Filled.RestaurantMenu, "مدیریت منو (همگام با سرور)", "افزودن/ویرایش/حذف — اپ مشتری به‌روز می‌شود", onNavigateToMenuManage)
            AdminActionButton(Icons.Filled.Star, "امتیازات پیک", "امتیازهایی که مشتری بعد از تحویل می‌دهد", onNavigateToRatings)
            AdminActionButton(Icons.Filled.AddShoppingCart, "ثبت سفارش جدید", "سفارش تلفنی / حضوری", onNavigateToNewOrder)
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
