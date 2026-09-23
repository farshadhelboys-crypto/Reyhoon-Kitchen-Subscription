package com.reyhoon.kitchen.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
    var online by remember { mutableStateOf(ApiConfig.isConfigured) }
    var payOrder by remember { mutableStateOf<Order?>(null) }

    LaunchedEffect(Unit) {
        NotificationHelper.acknowledgeOrdersViewed(context)
    }

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
                lastSeenAt = maxOf(lastSeenAt, fresh.maxOf { it.createdAt })
            }
            val remote = ApiClient.fetchOrders()
            remote.forEach { r ->
                val idx = AppRepository.orders.indexOfFirst { it.id == r.id }
                if (idx >= 0) AppRepository.orders[idx] = r
                else AppRepository.orders.add(0, r)
            }
            val byId = linkedMapOf<String, Order>()
            AppRepository.orders.forEach { byId[it.id] = it }
            remote.forEach { byId[it.id] = it }
            orders = byId.values.sortedByDescending { it.createdAt }
        } else {
            online = false
            orders = AppRepository.orders.toList()
        }
        loading = false
    }

    LaunchedEffect(Unit) {
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
                        Text("سفارش‌ها (آنلاین + حضوری)", fontWeight = FontWeight.Bold)
                        Text(
                            if (online) "متصل" else "آفلاین / قطع",
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
                        OrderCard(
                            order = order,
                            formatTs = ::formatTs,
                            onStatus = { status ->
                                scope.launch {
                                    if (ApiConfig.isConfigured) {
                                        ApiClient.updateOrderStatus(order.id, status, byKitchen = true)
                                    }
                                    val idx = AppRepository.orders.indexOfFirst { it.id == order.id }
                                    if (idx >= 0) {
                                        AppRepository.orders[idx] = AppRepository.orders[idx].copy(status = status)
                                    }
                                    refresh()
                                }
                            },
                            onPay = { payOrder = order }
                        )
                    }
                }
            }
        }
    }

    payOrder?.let { o ->
        var amount by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { payOrder = null },
            title = { Text("ثبت دریافت وجه — ${o.customerName}") },
            text = {
                Column {
                    Text("جمع: ${AppRepository.formatPrice(o.totalAmount)} | دریافتی: ${AppRepository.formatPrice(o.paidAmount)}")
                    Text("باقیمانده: ${AppRepository.formatPrice(o.remaining)}", fontWeight = FontWeight.Bold, color = OrangeSecondary)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = it.filter { ch -> ch.isDigit() } },
                        label = { Text("مبلغ دریافتی (تومان)") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val pay = amount.toLongOrNull() ?: 0L
                    if (pay <= 0) return@Button
                    scope.launch {
                        if (ApiConfig.isConfigured) {
                            ApiClient.recordPayment(o.customerId, pay, "دریافت سفارش")
                        }
                        AppRepository.recordPayment(o.customerId, pay, o.id)
                        payOrder = null
                        refresh()
                    }
                }) { Text("ثبت دریافت") }
            },
            dismissButton = { TextButton(onClick = { payOrder = null }) { Text("انصراف") } }
        )
    }
}

@Composable
private fun OrderCard(
    order: Order,
    formatTs: (Long?) -> String,
    onStatus: (String) -> Unit,
    onPay: () -> Unit
) {
    Card(shape = RoundedCornerShape(14.dp)) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(order.customerName, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                AssistChip(onClick = {}, label = { Text(order.statusEnum.labelFa) })
            }
            if (order.customerPhone.isNotBlank()) {
                Text("تلفن: ${order.customerPhone}", fontWeight = FontWeight.SemiBold)
            }
            if (order.customerAddress.isNotBlank()) {
                Text("آدرس: ${order.customerAddress}")
            }
            Text("ثبت: ${formatTs(order.createdAt)}")
            if (order.deliveredAt != null) Text("تحویل: ${formatTs(order.deliveredAt)}", color = GreenMid)
            order.items.forEach { Text("• ${it.foodName} × ${it.quantity}") }
            Text(
                "جمع: ${AppRepository.formatPrice(order.totalAmount)} | دریافتی: ${AppRepository.formatPrice(order.paidAmount)}",
                fontWeight = FontWeight.Bold,
                color = OrangeSecondary
            )
            if (order.remaining > 0) {
                Text("باقیمانده: ${AppRepository.formatPrice(order.remaining)}", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                if (order.status == OrderStatus.REGISTERED.key) {
                    FilledTonalButton(onClick = { onStatus(OrderStatus.PREPARING.key) }, modifier = Modifier.weight(1f)) { Text("آماده‌سازی") }
                }
                if (order.status == OrderStatus.PREPARING.key || order.status == OrderStatus.REGISTERED.key) {
                    FilledTonalButton(onClick = { onStatus(OrderStatus.SHIPPED.key) }, modifier = Modifier.weight(1f)) { Text("ارسال") }
                }
                if (order.status != OrderStatus.DELIVERED.key) {
                    Button(onClick = { onStatus(OrderStatus.DELIVERED.key) }, modifier = Modifier.weight(1f)) { Text("تحویل") }
                }
            }
            if (order.remaining > 0) {
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedButton(onClick = onPay, modifier = Modifier.fillMaxWidth()) { Text("ثبت مبلغ دریافتی") }
            }
        }
    }
}
