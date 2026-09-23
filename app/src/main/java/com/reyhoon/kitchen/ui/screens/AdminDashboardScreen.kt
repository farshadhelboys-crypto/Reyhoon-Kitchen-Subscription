package com.reyhoon.kitchen.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.ListAlt
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
import kotlinx.coroutines.launch

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
    val scope = rememberCoroutineScope()
    var selectedPeriod by remember { mutableStateOf("روزانه") }
    var lastSeenAt by remember { mutableStateOf(System.currentTimeMillis() - 30_000) }
    var pendingCount by remember { mutableIntStateOf(0) }
    var pendingName by remember { mutableStateOf("") }
    var salesTick by remember { mutableIntStateOf(0) }
    var showResetConfirm by remember { mutableStateOf(false) }
    var resetMsg by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        NotificationHelper.ensureChannels(context)
        while (true) {
            if (ApiConfig.isConfigured) {
                val remote = ApiClient.fetchOrders()
                remote.forEach { r ->
                    val idx = AppRepository.orders.indexOfFirst { it.id == r.id }
                    if (idx >= 0) AppRepository.orders[idx] = r
                    else AppRepository.orders.add(0, r)
                }
                salesTick++
                val fresh = ApiClient.fetchNewOrders(lastSeenAt)
                if (fresh.isNotEmpty()) {
                    NotificationHelper.onNewOrdersDetected(
                        context, fresh.map { it.id }, fresh.first().customerName
                    )
                    pendingCount = fresh.size
                    pendingName = fresh.first().customerName
                    lastSeenAt = maxOf(lastSeenAt, fresh.maxOf { it.createdAt })
                } else if (NotificationHelper.pendingAlarm) {
                    NotificationHelper.onNewOrdersDetected(context, emptyList(), pendingName)
                }
            }
            delay(8_000)
        }
    }

    val summary = remember(selectedPeriod, salesTick, AppRepository.orders.size) {
        AppRepository.getSalesSummary(selectedPeriod)
    }
    val totalDebt = remember(salesTick, AppRepository.customers.size, AppRepository.orders.size) {
        AppRepository.totalCustomerDebt()
    }
    val totalCredit = remember(salesTick, AppRepository.customers.size) {
        AppRepository.totalCustomerCredit()
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
            if (NotificationHelper.pendingAlarm || pendingCount > 0) {
                Card(
                    onClick = onNavigateToOrders,
                    colors = CardDefaults.cardColors(containerColor = OrangeSecondary.copy(alpha = 0.25f)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.NotificationsActive, null, tint = OrangeSecondary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("سفارش آنلاین جدید", fontWeight = FontWeight.Bold)
                            Text(
                                if (pendingName.isNotBlank())
                                    "$pendingName — برای قطع آلارم اینجا بزنید"
                                else "برای مشاهده و قطع آلارم ضربه بزنید"
                            )
                        }
                        Text("مشاهده", fontWeight = FontWeight.Bold, color = OrangeSecondary)
                    }
                }
            } else {
                Card(
                    onClick = onNavigateToOrders,
                    colors = CardDefaults.cardColors(containerColor = GreenMid.copy(alpha = 0.12f)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.ListAlt, null, tint = GreenMid)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("مشاهده سفارش‌های آنلاین", fontWeight = FontWeight.SemiBold)
                    }
                }
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
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard("فروش کل", AppRepository.formatPrice(summary.totalSales), "${summary.orderCount} سفارش", Modifier.weight(1f), GreenMid)
                StatCard("دریافتی", AppRepository.formatPrice(summary.totalPaid), "تومان", Modifier.weight(1f), OrangeSecondary)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard("بدهی دوره", AppRepository.formatPrice(summary.totalDebt), "تومان", Modifier.weight(1f), MaterialTheme.colorScheme.error)
                StatCard("کل بدهی", AppRepository.formatPrice(totalDebt), "تومان", Modifier.weight(1f), MaterialTheme.colorScheme.error)
            }
            StatCard("کل اعتبار مشتریان", AppRepository.formatPrice(totalCredit), "بدهی آشپزخانه به مشتری", Modifier.fillMaxWidth(), GreenMid)

            Spacer(modifier = Modifier.height(8.dp))
            Text("عملیات سریع", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            AdminActionButton(Icons.Filled.NotificationsActive, "سفارش‌های آنلاین", "آلارم فقط با باز کردن این صفحه قطع می‌شود", onNavigateToOrders)
            AdminActionButton(Icons.Filled.RestaurantMenu, "مدیریت منو + دسته‌بندی", "چلو / خورشت / نوشیدنی / مخلفات ...", onNavigateToMenuManage)
            AdminActionButton(Icons.Filled.Star, "امتیازات پیک", "امتیاز مشتریان", onNavigateToRatings)
            AdminActionButton(Icons.Filled.AddShoppingCart, "ثبت سفارش (انتخاب مشتری)", "تلفنی / حضوری", onNavigateToNewOrder)
            AdminActionButton(Icons.Filled.People, "مشتریان و بدهی", "افزودن و تسویه", onNavigateToCustomers)

            Spacer(modifier = Modifier.height(16.dp))
            Text("تنظیمات خطرناک", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.error)
            Card(
                onClick = { showResetConfirm = true },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.DeleteForever, null, tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("پاک کردن تمام داده‌های سرور", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                        Text("منو، مشتریان، سفارش‌ها، پرداخت‌ها، امتیازات — ریست کامل")
                    }
                }
            }
            resetMsg?.let { Text(it, fontWeight = FontWeight.Bold, color = GreenMid) }
        }
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("⚠️ ریست کامل سرور") },
            text = { Text("همه داده‌ها از سرور و این دستگاه پاک می‌شوند.\nاین عمل برگشت‌پذیر نیست!") },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            showResetConfirm = false
                            var ok = true
                            if (ApiConfig.isConfigured) ok = ApiClient.resetAllData()
                            AppRepository.clearAllLocal()
                            salesTick++
                            resetMsg = if (ok) "✓ همه داده‌ها پاک شدند" else "خطا در سرور — داده محلی پاک شد"
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("پاک کردن همه چیز") }
            },
            dismissButton = { TextButton(onClick = { showResetConfirm = false }) { Text("انصراف") } }
        )
    }
}

@Composable
private fun StatCard(
    title: String, value: String, subtitle: String,
    modifier: Modifier = Modifier, color: Color
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
    icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit
) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = GreenMid, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall)
            }
            Icon(Icons.Filled.ChevronLeft, null)
        }
    }
}
