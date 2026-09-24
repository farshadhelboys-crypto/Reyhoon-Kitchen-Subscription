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
import com.reyhoon.kitchen.ui.theme.GreenPrimary
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
    var orders by remember { mutableStateOf<List<Order>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var payOrder by remember { mutableStateOf<Order?>(null) }
    val scope = rememberCoroutineScope()
    val df = remember { SimpleDateFormat("HH:mm yyyy/MM/dd", Locale("fa")) }

    // باز شدن این صفحه = قطع قطعی آلارم و نوتیفیکیشن
    LaunchedEffect(Unit) {
        NotificationHelper.acknowledgeOrdersViewed(context)
    }

    suspend fun refresh() {
        loading = true
        if (ApiConfig.isConfigured) {
            val remote = ApiClient.fetchOrders()
            if (remote.isNotEmpty()) {
                AppRepository.orders.clear()
                AppRepository.orders.addAll(remote)
            }
        }
        orders = AppRepository.orders
            .filter { it.source != "prior_debt" }
            .sortedByDescending { it.createdAt }
        loading = false
    }

    LaunchedEffect(Unit) {
        refresh()
        while (true) {
            delay(12_000)
            refresh()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("سفارش‌ها (آنلاین + حضوری)", fontWeight = FontWeight.Bold)
                        Text(
                            "آلارم با ورود به این صفحه قطع می‌شود",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "بازگشت") }
                },
                actions = {
                    IconButton(onClick = { scope.launch { refresh() } }) { Icon(Icons.Default.Refresh, null) }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = GreenPrimary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        modifier = modifier
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            message?.let {
                Text(it, modifier = Modifier.padding(12.dp), color = GreenPrimary, fontWeight = FontWeight.SemiBold)
            }
            if (loading && orders.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else if (orders.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("سفارشی نیست")
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(orders, key = { it.id }) { order ->
                        OrderCard(
                            order = order,
                            formatTs = { ts -> df.format(Date(ts)) },
                            onStatus = { status ->
                                scope.launch {
                                    if (ApiConfig.isConfigured) {
                                        val updated = ApiClient.updateOrderStatus(order.id, status, byKitchen = true)
                                        NotificationHelper.acknowledgeOrdersViewed(context)
                                        if (updated != null) {
                                            val idx = AppRepository.orders.indexOfFirst { it.id == order.id }
                                            if (idx >= 0) AppRepository.orders[idx] = updated
                                        }
                                    } else {
                                        val idx = AppRepository.orders.indexOfFirst { it.id == order.id }
                                        if (idx >= 0) {
                                            AppRepository.orders[idx] = AppRepository.orders[idx].copy(status = status)
                                        }
                                        NotificationHelper.acknowledgeOrdersViewed(context)
                                    }
                                    refresh()
                                    message = "وضعیت به‌روز شد"
                                }
                            },
                            onPay = { payOrder = order }
                        )
                    }
                }
            }
        }
    }

    payOrder?.let { order ->
        var amount by remember { mutableStateOf("") }
        val remaining = order.remaining
        AlertDialog(
            onDismissRequest = { payOrder = null },
            title = { Text("ثبت مبلغ دریافتی") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("باقیمانده: ${AppRepository.formatPrice(remaining)} تومان")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = amount == remaining.toString(),
                            onClick = { amount = remaining.toString() },
                            label = { Text("کل باقیمانده") }
                        )
                    }
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = it.filter { c -> c.isDigit() } },
                        label = { Text("مبلغ (تومان)") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val paid = amount.toLongOrNull() ?: 0L
                        if (paid <= 0) return@Button
                        scope.launch {
                            if (ApiConfig.isConfigured) {
                                ApiClient.recordPayment(order.customerId, paid, "دریافت سفارش")
                            }
                            AppRepository.recordPayment(order.customerId, paid, order.id)
                            payOrder = null
                            refresh()
                            message = "پرداخت ثبت شد"
                        }
                    }
                ) { Text("ثبت") }
            },
            dismissButton = { TextButton(onClick = { payOrder = null }) { Text("انصراف") } }
        )
    }
}

@Composable
private fun OrderCard(
    order: Order,
    formatTs: (Long) -> String,
    onStatus: (String) -> Unit,
    onPay: () -> Unit
) {
    Card(shape = RoundedCornerShape(12.dp)) {
        Column(Modifier.padding(14.dp).fillMaxWidth()) {
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
                buildString {
                    append("جمع: ${AppRepository.formatPrice(order.totalAmount)}")
                    append(" | نقد: ${AppRepository.formatPrice(order.cashReceived)}")
                    if (order.creditApplied > 0) append(" | اعتبار: ${AppRepository.formatPrice(order.creditApplied)}")
                },
                fontWeight = FontWeight.Bold,
                color = OrangeSecondary
            )
            if (order.remaining > 0) {
                Text(
                    "باقیمانده: ${AppRepository.formatPrice(order.remaining)}",
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                if (order.status == OrderStatus.REGISTERED.key) {
                    FilledTonalButton(onClick = { onStatus(OrderStatus.PREPARING.key) }, modifier = Modifier.weight(1f)) {
                        Text("آماده‌سازی")
                    }
                }
                if (order.status == OrderStatus.PREPARING.key) {
                    FilledTonalButton(onClick = { onStatus(OrderStatus.SHIPPED.key) }, modifier = Modifier.weight(1f)) {
                        Text("ارسال")
                    }
                }
                if (order.status == OrderStatus.SHIPPED.key) {
                    Button(onClick = { onStatus(OrderStatus.DELIVERED.key) }, modifier = Modifier.weight(1f)) {
                        Text("تحویل")
                    }
                }
            }
            if (order.remaining > 0) {
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedButton(onClick = onPay, modifier = Modifier.fillMaxWidth()) {
                    Text("ثبت مبلغ دریافتی")
                }
            }
        }
    }
}
