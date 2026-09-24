package com.reyhoon.kitchen.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
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
    LaunchedEffect(Unit) {
        NotificationHelper.acknowledgeOrdersViewed(context)
    }

    var orders by remember { mutableStateOf<List<Order>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var payOrder by remember { mutableStateOf<Order?>(null) }
    var payAmount by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val df = remember { SimpleDateFormat("yyyy/MM/dd HH:mm", Locale("fa")) }
    fun formatTs(ts: Long?): String = if (ts == null || ts <= 0) "—" else df.format(Date(ts))

    suspend fun refresh() {
        loading = true
        try {
            if (ApiConfig.isConfigured) {
                val remote = ApiClient.fetchOrders()
                orders = remote.sortedByDescending { it.createdAt }
            } else {
                orders = AppRepository.orders.sortedByDescending { it.createdAt }
            }
        } catch (_: Exception) {
            orders = AppRepository.orders.sortedByDescending { it.createdAt }
        }
        loading = false
    }

    LaunchedEffect(Unit) {
        refresh()
        while (true) {
            delay(8000)
            refresh()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("سفارش‌های آشپزخانه", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "بازگشت") }
                },
                actions = {
                    IconButton(onClick = { scope.launch { refresh() } }) {
                        Icon(Icons.Default.Refresh, "بروزرسانی")
                    }
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
        if (loading && orders.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (orders.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("سفارشی نیست")
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize().padding(padding)
            ) {
                items(orders, key = { it.id }) { order ->
                    OrderCard(
                        order = order,
                        formatTs = ::formatTs,
                        onStatus = { status ->
                            scope.launch {
                                if (ApiConfig.isConfigured) {
                                    val updated = ApiClient.updateOrderStatus(order.id, status, byKitchen = true)
                                    if (updated != null) refresh()
                                }
                                NotificationHelper.acknowledgeOrdersViewed(context)
                            }
                        },
                        onPay = {
                            payOrder = order
                            payAmount = order.remaining.toString()
                        }
                    )
                }
            }
        }
    }

    payOrder?.let { o ->
        AlertDialog(
            onDismissRequest = { payOrder = null },
            title = { Text("ثبت مبلغ دریافتی") },
            text = {
                Column {
                    Text("${o.customerName} — باقیمانده: ${AppRepository.formatPrice(o.remaining)}")
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = payAmount,
                        onValueChange = { payAmount = it.filter { c -> c.isDigit() } },
                        label = { Text("مبلغ (تومان)") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val amt = payAmount.toLongOrNull() ?: 0L
                    if (amt > 0) {
                        scope.launch {
                            if (ApiConfig.isConfigured) ApiClient.recordPayment(o.customerId, amt)
                            AppRepository.recordPayment(o.customerId, amt, o.id)
                            payOrder = null
                            refresh()
                        }
                    }
                }) { Text("ثبت") }
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
    val callCtx = LocalContext.current
    Card(shape = RoundedCornerShape(12.dp)) {
        Column(Modifier.padding(14.dp).fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(order.customerName, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                AssistChip(onClick = {}, label = { Text(order.statusEnum.labelFa) })
            }
            if (order.customerPhone.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "تلفن: ${order.customerPhone}",
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    FilledTonalButton(
                        onClick = {
                            val raw = order.customerPhone.filter { it.isDigit() || it == '+' }
                            if (raw.isNotBlank()) {
                                try {
                                    callCtx.startActivity(
                                        Intent(Intent.ACTION_DIAL, Uri.parse("tel:$raw"))
                                    )
                                } catch (_: Exception) { }
                            }
                        }
                    ) { Text("تماس با مشتری") }
                }
            }
            if (order.customerAddress.isNotBlank()) {
                Text("آدرس: ${order.customerAddress}")
            }
            Text("ثبت: ${formatTs(order.createdAt)}")
            if (order.deliveredAt != null) Text("تحویل: ${formatTs(order.deliveredAt)}", color = GreenMid)
            run {
                val tiers = order.items.map { it.priceTier }.distinct()
                val label = when {
                    order.items.isEmpty() -> null
                    tiers.size == 1 && tiers.first() == "economy" -> "اقتصادی"
                    tiers.size == 1 -> "غیر اقتصادی"
                    else -> "مختلط (اقتصادی و غیر اقتصادی)"
                }
                if (label != null) {
                    Text(
                        "نوع سفارش: $label",
                        fontWeight = FontWeight.Bold,
                        color = OrangeSecondary
                    )
                }
            }
            order.items.forEach {
                Text("• ${it.foodName} × ${it.quantity} (${it.priceTierLabel})")
            }
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
            if (order.status != OrderStatus.CANCELLED.key) {
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
            } else {
                Text("این سفارش توسط مشتری لغو شده", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
            }
            if (order.remaining > 0 && order.status != OrderStatus.CANCELLED.key) {
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedButton(onClick = onPay, modifier = Modifier.fillMaxWidth()) {
                    Text("ثبت مبلغ دریافتی")
                }
            }
        }
    }
}
