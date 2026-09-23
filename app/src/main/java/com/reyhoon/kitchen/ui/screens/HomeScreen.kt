package com.reyhoon.kitchen.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.reyhoon.kitchen.data.ApiClient
import com.reyhoon.kitchen.data.ApiConfig
import com.reyhoon.kitchen.data.AppRepository
import com.reyhoon.kitchen.data.Order
import com.reyhoon.kitchen.data.OrderStatus
import com.reyhoon.kitchen.ui.components.ReyhoonLogo
import com.reyhoon.kitchen.ui.theme.GreenMid
import com.reyhoon.kitchen.ui.theme.GreenPale
import com.reyhoon.kitchen.ui.theme.OrangeSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToMenu: () -> Unit,
    onNavigateToAddress: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val customer by AppRepository.currentCustomer
    var orders by remember { mutableStateOf<List<Order>>(emptyList()) }
    var payOrder by remember { mutableStateOf<Order?>(null) }
    val scope = rememberCoroutineScope()

    suspend fun reloadOrders(id: String) {
        val local = AppRepository.orders.filter { it.customerId == id }
        if (ApiConfig.isConfigured) {
            val remote = ApiClient.fetchOrders(id)
            val byId = linkedMapOf<String, Order>()
            local.forEach { byId[it.id] = it }
            remote.forEach { byId[it.id] = it }
            orders = byId.values.sortedByDescending { it.createdAt }
            val code = customer?.subscriptionCode
            if (!code.isNullOrBlank()) {
                val refreshed = ApiClient.fetchCustomerByCode(code)
                if (refreshed != null) {
                    AppRepository.updateCustomer(refreshed)
                    AppRepository.currentCustomer.value = refreshed
                }
            }
        } else {
            orders = local.sortedByDescending { it.createdAt }
        }
    }

    LaunchedEffect(customer?.id) {
        val id = customer?.id ?: return@LaunchedEffect
        while (true) {
            reloadOrders(id)
            delay(8_000)
        }
    }

    fun formatTs(ts: Long?): String {
        if (ts == null || ts <= 0) return "—"
        return SimpleDateFormat("HH:mm", Locale("fa")).format(Date(ts))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ReyhoonLogo(size = 36.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("ریحون", fontWeight = FontWeight.Bold)
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
        if (customer == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("لطفاً دوباره وارد شوید")
            }
            return@Scaffold
        }

        val displayDebt = AppRepository.recalculateDebt(customer!!.id).coerceAtLeast(customer!!.debt)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Brush.verticalGradient(listOf(GreenPale.copy(alpha = 0.5f), MaterialTheme.colorScheme.background)))
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = GreenMid.copy(alpha = 0.12f))
            ) {
                Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    ReyhoonLogo(size = 56.dp)
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text("مشتری: ${customer!!.name}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("کد: ${customer!!.subscriptionCode ?: "—"}", style = MaterialTheme.typography.bodyMedium)
                        if (customer!!.phone.isNotBlank()) {
                            Text("تلفن: ${customer!!.phone}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (displayDebt > 0) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f) else GreenPale
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("بدهی")
                        Text(
                            if (displayDebt > 0) "${AppRepository.formatPrice(displayDebt)} ت" else "۰",
                            fontWeight = FontWeight.Bold,
                            color = if (displayDebt > 0) MaterialTheme.colorScheme.error else GreenMid
                        )
                    }
                }
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = GreenPale)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("اعتبار")
                        Text(
                            if (customer!!.credit > 0) "${AppRepository.formatPrice(customer!!.credit)} ت" else "۰",
                            fontWeight = FontWeight.Bold, color = GreenMid
                        )
                    }
                }
            }

            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), onClick = onNavigateToAddress) {
                Row(modifier = Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, null, tint = OrangeSecondary, modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("آدرس تحویل", fontWeight = FontWeight.SemiBold)
                        Text(customer!!.address.fullAddress(), style = MaterialTheme.typography.bodyMedium)
                    }
                    Icon(Icons.Default.ChevronLeft, null)
                }
            }

            Button(
                onClick = onNavigateToMenu,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.AddShoppingCart, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("ثبت سفارش جدید برای این مشتری", fontWeight = FontWeight.Bold)
            }

            Text("سفارش‌های این مشتری", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Text("می‌توانید مبلغ دریافتی ثبت کنید تا بدهی کم شود", style = MaterialTheme.typography.bodySmall)

            if (orders.isEmpty()) {
                Text("هنوز سفارشی ثبت نشده", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            } else {
                orders.take(20).forEach { o ->
                    Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(o.statusEnum.labelFa, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                Text(formatTs(o.createdAt), style = MaterialTheme.typography.bodySmall)
                            }
                            if (o.source.isNotBlank()) {
                                Text("منبع: ${if (o.source == "kitchen") "حضوری" else "آنلاین"}", style = MaterialTheme.typography.bodySmall)
                            }
                            o.items.forEach { Text("• ${it.foodName} × ${it.quantity}") }
                            Text(
                                "جمع: ${AppRepository.formatPrice(o.totalAmount)} | دریافتی: ${AppRepository.formatPrice(o.paidAmount)}",
                                color = OrangeSecondary, fontWeight = FontWeight.SemiBold
                            )
                            if (o.remaining > 0) {
                                Text("باقیمانده: ${AppRepository.formatPrice(o.remaining)}", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                            }
                            if (o.status != OrderStatus.DELIVERED.key) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    if (o.status == OrderStatus.REGISTERED.key) {
                                        FilledTonalButton(onClick = {
                                            scope.launch {
                                                if (ApiConfig.isConfigured)
                                                    ApiClient.updateOrderStatus(o.id, OrderStatus.PREPARING.key, byKitchen = true)
                                                val idx = AppRepository.orders.indexOfFirst { it.id == o.id }
                                                if (idx >= 0) AppRepository.orders[idx] = AppRepository.orders[idx].copy(status = OrderStatus.PREPARING.key)
                                                orders = orders.map { if (it.id == o.id) it.copy(status = OrderStatus.PREPARING.key) else it }
                                            }
                                        }) { Text("آماده‌سازی") }
                                    }
                                    if (o.status != OrderStatus.SHIPPED.key && o.status != OrderStatus.DELIVERED.key) {
                                        FilledTonalButton(onClick = {
                                            scope.launch {
                                                if (ApiConfig.isConfigured)
                                                    ApiClient.updateOrderStatus(o.id, OrderStatus.SHIPPED.key, byKitchen = true)
                                                val idx = AppRepository.orders.indexOfFirst { it.id == o.id }
                                                if (idx >= 0) AppRepository.orders[idx] = AppRepository.orders[idx].copy(status = OrderStatus.SHIPPED.key)
                                                orders = orders.map { if (it.id == o.id) it.copy(status = OrderStatus.SHIPPED.key) else it }
                                            }
                                        }) { Text("ارسال") }
                                    }
                                    Button(onClick = {
                                        scope.launch {
                                            if (ApiConfig.isConfigured)
                                                ApiClient.updateOrderStatus(o.id, OrderStatus.DELIVERED.key, byKitchen = true)
                                            val idx = AppRepository.orders.indexOfFirst { it.id == o.id }
                                            if (idx >= 0) AppRepository.orders[idx] = AppRepository.orders[idx].copy(status = OrderStatus.DELIVERED.key)
                                            orders = orders.map { if (it.id == o.id) it.copy(status = OrderStatus.DELIVERED.key) else it }
                                        }
                                    }) { Text("تحویل") }
                                }
                            }
                            if (o.remaining > 0) {
                                Spacer(modifier = Modifier.height(6.dp))
                                OutlinedButton(onClick = { payOrder = o }, modifier = Modifier.fillMaxWidth()) {
                                    Icon(Icons.Default.Payments, null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("ثبت مبلغ دریافتی")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    payOrder?.let { o ->
        var amount by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { payOrder = null },
            title = { Text("ثبت دریافت — ${o.customerName}") },
            text = {
                Column {
                    Text("جمع: ${AppRepository.formatPrice(o.totalAmount)}")
                    Text("دریافتی قبلی: ${AppRepository.formatPrice(o.paidAmount)}")
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
                        if (ApiConfig.isConfigured) ApiClient.recordPayment(o.customerId, pay, "دریافت سفارش حضوری")
                        AppRepository.recordPayment(o.customerId, pay, o.id, "دریافت سفارش حضوری")
                        payOrder = null
                        customer?.id?.let { reloadOrders(it) }
                    }
                }) { Text("ثبت") }
            },
            dismissButton = { TextButton(onClick = { payOrder = null }) { Text("انصراف") } }
        )
    }
}
