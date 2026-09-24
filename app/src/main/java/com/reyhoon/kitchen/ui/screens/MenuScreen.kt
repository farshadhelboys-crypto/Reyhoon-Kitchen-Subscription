package com.reyhoon.kitchen.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reyhoon.kitchen.data.ApiClient
import com.reyhoon.kitchen.data.ApiConfig
import com.reyhoon.kitchen.data.AppRepository
import com.reyhoon.kitchen.data.MenuCategories
import com.reyhoon.kitchen.data.OrderItem
import com.reyhoon.kitchen.ui.theme.GreenPrimary
import com.reyhoon.kitchen.ui.theme.OrangeSecondary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val customer = AppRepository.currentCustomer.value
    var menu by remember { mutableStateOf(AppRepository.menuItems.filter { it.isAvailable }) }
    var quantities by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var paidNow by remember { mutableStateOf("") }
    var resultMessage by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        if (ApiConfig.isConfigured) {
            val remote = ApiClient.fetchMenu()
            if (remote.isNotEmpty()) {
                AppRepository.menuItems.clear()
                AppRepository.menuItems.addAll(remote)
                menu = remote.filter { it.isAvailable }
            }
        } else {
            menu = AppRepository.menuItems.filter { it.isAvailable }
        }
    }

    val cartItems = remember(quantities, menu) {
        menu.flatMap { food ->
            val list = mutableListOf<OrderItem>()
            val q = quantities[food.id] ?: 0
            if (q > 0) list.add(OrderItem(food.id, food.name, food.price, q))
            val sq = quantities["${food.id}__skewer"] ?: 0
            if (sq > 0 && food.extraSkewerPrice > 0) {
                list.add(OrderItem("${food.id}__skewer", "سیخ اضافه (${food.name})", food.extraSkewerPrice, sq))
            }
            list
        }
    }
    val total = cartItems.sumOf { it.total }
    val customerCredit = customer?.credit ?: 0L
    val afterCredit = (total - customerCredit).coerceAtLeast(0)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ثبت سفارش حضوری", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "بازگشت") }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = GreenPrimary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            if (customer != null) {
                Text(
                    "مشتری: ${customer.name} | کد: ${customer.subscriptionCode ?: "—"}",
                    fontWeight = FontWeight.SemiBold
                )
                if (customerCredit > 0) {
                    Text(
                        "اعتبار: ${AppRepository.formatPrice(customerCredit)} تومان",
                        color = GreenPrimary,
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            val grouped = menu.groupBy { it.category.ifBlank { "عمومی" } }
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                grouped.forEach { (cat, list) ->
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 6.dp)
                        ) {
                            Text(MenuCategories.emoji(cat), fontSize = 26.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                cat.ifBlank { "عمومی" },
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = GreenPrimary
                            )
                        }
                    }
                    items(list, key = { it.id }) { food ->
                        Card(shape = RoundedCornerShape(10.dp)) {
                            Column(modifier = Modifier.padding(12.dp).fillMaxWidth()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(MenuCategories.emoji(food.category), fontSize = 22.sp)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(food.name, fontWeight = FontWeight.Medium)
                                        Text(
                                            "${AppRepository.formatPrice(food.price)} تومان",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = OrangeSecondary
                                        )
                                    }
                                    val qty = quantities[food.id] ?: 0
                                    IconButton(
                                        onClick = {
                                            quantities = quantities.toMutableMap().apply {
                                                if (qty <= 1) remove(food.id) else put(food.id, qty - 1)
                                            }
                                        },
                                        enabled = qty > 0
                                    ) { Icon(Icons.Default.Remove, null) }
                                    Text("$qty", fontWeight = FontWeight.Bold, modifier = Modifier.width(28.dp))
                                    IconButton(onClick = {
                                        quantities = quantities.toMutableMap().apply { put(food.id, qty + 1) }
                                    }) { Icon(Icons.Default.Add, null) }
                                }
                                if (food.extraSkewerPrice > 0) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("🍢", fontSize = 18.sp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("سیخ اضافه", fontWeight = FontWeight.Medium)
                                            Text(
                                                "${AppRepository.formatPrice(food.extraSkewerPrice)} تومان",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = OrangeSecondary
                                            )
                                        }
                                        val skKey = "${food.id}__skewer"
                                        val sq = quantities[skKey] ?: 0
                                        IconButton(
                                            onClick = {
                                                quantities = quantities.toMutableMap().apply {
                                                    if (sq <= 1) remove(skKey) else put(skKey, sq - 1)
                                                }
                                            },
                                            enabled = sq > 0
                                        ) { Icon(Icons.Default.Remove, null) }
                                        Text("$sq", fontWeight = FontWeight.Bold, modifier = Modifier.width(28.dp))
                                        IconButton(onClick = {
                                            quantities = quantities.toMutableMap().apply { put(skKey, sq + 1) }
                                        }) { Icon(Icons.Default.Add, null) }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (total > 0) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text(
                    "جمع غذا: ${AppRepository.formatPrice(total)} تومان",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = OrangeSecondary
                )
                if (customerCredit > 0) {
                    Text(
                        "پس از کسر اعتبار: ${AppRepository.formatPrice(afterCredit)} تومان",
                        style = MaterialTheme.typography.bodyMedium,
                        color = GreenPrimary
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("برای پر کردن فیلد روی مبلغ بزنید:", style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = paidNow == afterCredit.toString(),
                        onClick = { paidNow = afterCredit.toString() },
                        label = {
                            Text(
                                "قابل پرداخت: ${AppRepository.formatPrice(afterCredit)}",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    )
                    FilterChip(
                        selected = paidNow == "0",
                        onClick = { paidNow = "0" },
                        label = { Text("نسیه (۰)") }
                    )
                    if (total != afterCredit) {
                        FilterChip(
                            selected = paidNow == total.toString(),
                            onClick = { paidNow = total.toString() },
                            label = { Text("کل: ${AppRepository.formatPrice(total)}") }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = paidNow,
                    onValueChange = { paidNow = it.filter { c -> c.isDigit() } },
                    label = { Text("مبلغ دریافتی از مشتری (تومان)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        val c = customer ?: return@Button
                        val paid = paidNow.toLongOrNull() ?: 0L
                        scope.launch {
                            loading = true
                            if (ApiConfig.isConfigured) {
                                val remote = ApiClient.createOrder(
                                    customerId = c.id,
                                    items = cartItems,
                                    paidNow = paid,
                                    source = "kitchen"
                                )
                                if (remote != null) {
                                    val local = AppRepository.createOrder(c, cartItems, paid)
                                    quantities = emptyMap()
                                    paidNow = ""
                                    resultMessage = local.message + "\n✓ روی سرور ذخیره شد"
                                } else {
                                    val local = AppRepository.createOrder(c, cartItems, paid)
                                    quantities = emptyMap()
                                    paidNow = ""
                                    resultMessage = local.message + "\n⚠ ممکن است روی سرور ذخیره نشده باشد"
                                }
                            } else {
                                val local = AppRepository.createOrder(c, cartItems, paid)
                                quantities = emptyMap()
                                paidNow = ""
                                resultMessage = local.message
                            }
                            loading = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    enabled = cartItems.isNotEmpty() && !loading,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (loading) CircularProgressIndicator(modifier = Modifier.size(22.dp), color = MaterialTheme.colorScheme.onPrimary)
                    else Text("ثبت سفارش", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    resultMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = { resultMessage = null },
            title = { Text("نتیجه") },
            text = { Text(msg) },
            confirmButton = {
                Button(onClick = { resultMessage = null; onBack() }) { Text("باشه") }
            }
        )
    }
}
