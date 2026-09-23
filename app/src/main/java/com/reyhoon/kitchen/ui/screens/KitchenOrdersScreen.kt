package com.reyhoon.kitchen.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.reyhoon.kitchen.data.ApiClient
import com.reyhoon.kitchen.data.ApiConfig
import com.reyhoon.kitchen.data.AppRepository
import com.reyhoon.kitchen.data.Order
import com.reyhoon.kitchen.data.OrderStatus
import com.reyhoon.kitchen.ui.theme.GreenMid
import com.reyhoon.kitchen.ui.theme.OrangeSecondary
import com.reyhoon.kitchen.util.NotificationHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KitchenOrdersScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var orders by remember { mutableStateOf<List<Order>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var lastSeenAt by remember { mutableStateOf(System.currentTimeMillis() - 60_000) }
    var alertText by remember { mutableStateOf<String?>(null) }
    var online by remember { mutableStateOf(ApiConfig.isConfigured) }

    fun formatTs(ts: Long?): String {
        if (ts == null || ts <= 0) return "—"
        return SimpleDateFormat("yyyy/MM/dd HH:mm", Locale("fa")).format(Date(ts))
    }

    suspend fun refresh(fromPoll: Boolean = false) {
        if (!fromPoll) loading = true
        if (ApiConfig.isConfigured) {
            online = ApiClient.health()
            val fresh = ApiClient.fetchNewOrders(lastSeenAt)
            if (fresh.isNotEmpty()) {
                val name = fresh.first().customerName
                alertText = "سفارش جدید دارید! ${fresh.size} مورد — $name"
                NotificationHelper.notifyNewOrder(context, fresh.size, name)
                lastSeenAt = maxOf(lastSeenAt, fresh.maxOf { it.createdAt })
            }
            orders = ApiClient.fetchOrders()
        } else {
            online = false
            orders = AppRepository.orders.toList()
        }
        loading = false
    }

    LaunchedEffect(Unit) {
        NotificationHelper.ensureChannels(context)
        refresh()
        while (true) {
            delay(8_000)
            if (ApiConfig.isConfigured) refresh(fromPoll = true)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("سفارش‌های آنلاین", fontWeight = FontWeight.Bold)
                        Text(
                            if (online) "متصل — هر ۸ ثانیه چک می‌شود"
                            else if (ApiConfig.isConfigured) "قطع ارتباط"
                            else "آفلاین",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت")
                    }
                },
                actions = {
                    IconButton(onClick = { scope.launch { refresh() } }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "بروزرسانی")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = GreenMid,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        modifier = modifier
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            alertText?.let { msg ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = OrangeSecondary.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth().padding(12.dp)
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.NotificationsActive, null, tint = OrangeSecondary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(msg, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        TextButton(onClick = { alertText = null }) { Text("باشه") }
                    }
                }
            }
            if (loading && orders.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (orders.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("سفارشی نیست", style = MaterialTheme.typography.titleMedium)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(orders, key = { it.id }) { order ->
                        OrderCard(order, ::formatTs) { status ->
                            scope.launch {
                                if (ApiConfig.isConfigured) {
                                    ApiClient.updateOrderStatus(order.id, status, byKitchen = true)
                                    orders = ApiClient.fetchOrders()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OrderCard(order: Order, formatTs: (Long?) -> String, onStatus: (String) -> Unit) {
    Card(shape = RoundedCornerShape(14.dp)) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(order.customerName, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                AssistChip(onClick = {}, label = { Text(order.statusEnum.labelFa) })
            }
            if (order.customerPhone.isNotBlank()) {
                Text("تلفن: ${order.customerPhone}", style = MaterialTheme.typography.bodyMedium)
            }
            Text("ساعت ثبت: ${formatTs(order.createdAt)}", style = MaterialTheme.typography.bodyMedium)
            if (order.deliveredAt != null) {
                Text(
                    "ساعت تحویل: ${formatTs(order.deliveredAt)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = GreenMid
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            order.items.forEach { item -> Text("• ${item.foodName} × ${item.quantity}") }
            Text(
                "مبلغ: ${AppRepository.formatPrice(order.totalAmount)} تومان",
                fontWeight = FontWeight.Bold,
                color = OrangeSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                if (order.status == OrderStatus.REGISTERED.key) {
                    FilledTonalButton(
                        onClick = { onStatus(OrderStatus.PREPARING.key) },
                        modifier = Modifier.weight(1f)
                    ) { Text("آماده‌سازی") }
                }
                if (order.status == OrderStatus.PREPARING.key || order.status == OrderStatus.REGISTERED.key) {
                    FilledTonalButton(
                        onClick = { onStatus(OrderStatus.SHIPPED.key) },
                        modifier = Modifier.weight(1f)
                    ) { Text("ارسال") }
                }
                if (order.status != OrderStatus.DELIVERED.key) {
                    Button(
                        onClick = { onStatus(OrderStatus.DELIVERED.key) },
                        modifier = Modifier.weight(1f)
                    ) { Text("تحویل شد") }
                }
            }
        }
    }
}
