package com.reyhoon.kitchen.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reyhoon.kitchen.data.ApiClient
import com.reyhoon.kitchen.data.ApiConfig
import com.reyhoon.kitchen.data.AppRepository
import com.reyhoon.kitchen.ui.components.ReyhoonLogo
import com.reyhoon.kitchen.ui.theme.GreenMid
import com.reyhoon.kitchen.ui.theme.GreenPrimary
import com.reyhoon.kitchen.ui.theme.OrangeSecondary
import com.reyhoon.kitchen.util.NotificationHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    onNavigateToOrders: () -> Unit,
    onNavigateToMenu: () -> Unit,
    onNavigateToCustomers: () -> Unit,
    onNavigateToNewOrder: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedPeriod by remember { mutableStateOf("day") }
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
                }
                // اگر آلارم acknowledge شده، بنر را هم بردار
                if (!NotificationHelper.pendingAlarm) {
                    pendingCount = 0
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
                        Column {
                            Text("داشبورد ریحون", fontWeight = FontWeight.Bold)
                            Text("آشپزخانه", style = MaterialTheme.typography.bodySmall)
                        }
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
                        Icon(Icons.Filled.NotificationsActive, null, tint = GreenPrimary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("سفارش‌های آنلاین", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        Text("باز کردن", color = GreenPrimary, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Text("گزارش فروش", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("day" to "امروز", "week" to "هفته", "month" to "ماه").forEach { (key, label) ->
                    FilterChip(
                        selected = selectedPeriod == key,
                        onClick = { selectedPeriod = key },
                        label = { Text(label) }
                    )
                }
            }
            Card(shape = RoundedCornerShape(14.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("تعداد سفارش: ${summary.orderCount}", fontWeight = FontWeight.Medium)
                    Text("جمع فروش: ${AppRepository.formatPrice(summary.totalSales)} تومان")
                    Text(
                        "دریافتی نقد: ${AppRepository.formatPrice(summary.totalPaid)} تومان",
                        color = GreenPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "مانده: ${AppRepository.formatPrice(summary.totalDebt)} تومان",
                        color = OrangeSecondary
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.weight(1f)) {
                    Column(Modifier.padding(12.dp)) {
                        Text("بدهی مشتریان", fontWeight = FontWeight.SemiBold)
                        Text(AppRepository.formatPrice(totalDebt), color = OrangeSecondary, fontWeight = FontWeight.Bold)
                    }
                }
                Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.weight(1f)) {
                    Column(Modifier.padding(12.dp)) {
                        Text("اعتبار مشتریان", fontWeight = FontWeight.SemiBold)
                        Text(AppRepository.formatPrice(totalCredit), color = GreenPrimary, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Text("میانبرها", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            AdminActionButton(Icons.Filled.NotificationsActive, "سفارش‌های آنلاین", "آلارم با باز کردن این صفحه قطع می‌شود", onNavigateToOrders)
            AdminActionButton(Icons.Filled.Restaurant, "سفارش حضوری / تلفنی", "ثبت سفارش دستی", onNavigateToNewOrder)
            AdminActionButton(Icons.Filled.MenuBook, "مدیریت منو", "قیمت و دسته‌بندی", onNavigateToMenu)
            AdminActionButton(Icons.Filled.People, "مشتریان", "بدهی، اعتبار، کد اشتراک", onNavigateToCustomers)
            AdminActionButton(Icons.Filled.Settings, "تنظیمات API", "آدرس سرور Cloudflare", onNavigateToSettings)

            HorizontalDivider()
            TextButton(
                onClick = { showResetConfirm = true },
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Filled.DeleteForever, null)
                Spacer(Modifier.width(6.dp))
                Text("بازنشانی داده‌ها (خطرناک)")
            }
            resetMsg?.let { Text(it, color = GreenPrimary, fontWeight = FontWeight.SemiBold) }
        }
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("بازنشانی کامل؟", fontWeight = FontWeight.Bold) },
            text = { Text("همه سفارش‌ها و پرداخت‌ها پاک می‌شود. این عمل برگشت‌پذیر نیست.") },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            showResetConfirm = false
                            if (ApiConfig.isConfigured) {
                                val ok = ApiClient.resetAllData()
                                resetMsg = if (ok) "سرور پاک شد" else "خطا در پاک کردن سرور"
                            }
                            AppRepository.orders.clear()
                            AppRepository.payments.clear()
                            salesTick++
                            resetMsg = (resetMsg ?: "") + " | حافظه محلی پاک شد"
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("بله، پاک کن") }
            },
            dismissButton = { TextButton(onClick = { showResetConfirm = false }) { Text("انصراف") } }
        )
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
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = GreenPrimary)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
