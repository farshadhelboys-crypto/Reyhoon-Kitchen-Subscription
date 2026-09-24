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
import com.reyhoon.kitchen.data.ApiClient
import com.reyhoon.kitchen.data.ApiConfig
import com.reyhoon.kitchen.data.AppRepository
import com.reyhoon.kitchen.data.Customer
import com.reyhoon.kitchen.data.OrderItem
import com.reyhoon.kitchen.ui.theme.GreenPrimary
import com.reyhoon.kitchen.ui.theme.OrangeSecondary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewOrderScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val customers = AppRepository.customers
    val menu = AppRepository.menuItems.filter { it.isAvailable }
    val scope = rememberCoroutineScope()

    var selectedCustomer by remember { mutableStateOf<Customer?>(null) }
    var quantities by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var paidNow by remember { mutableStateOf("") }
    var resultMessage by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }

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
    val customerCredit = selectedCustomer?.credit ?: 0L
    val afterCredit = (total - customerCredit).coerceAtLeast(0)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("سفارش دستی (تلفنی)", fontWeight = FontWeight.Bold) },
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
            Text("انتخاب مشتری", fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(6.dp))
            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                OutlinedTextField(
                    value = selectedCustomer?.let {
                        buildString {
                            append(it.name)
                            if (it.credit > 0) append(" | اعتبار: ${AppRepository.formatPrice(it.credit)}")
                            if (it.debt > 0) append(" | بدهی: ${AppRepository.formatPrice(it.debt)}")
                        }
                    } ?: "انتخاب کنید",
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) }
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    customers.forEach { c ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(c.name, fontWeight = FontWeight.SemiBold)
                                    Text("تلفن: ${c.phone} | کد: ${c.subscriptionCode ?: "—"}")
                                }
                            },
                            onClick = {
                                selectedCustomer = c
                                expanded = false
                            }
                        )
                    }
                }
            }

            if (selectedCustomer != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text("انتخاب غذا", fontWeight = FontWeight.SemiBold)
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(menu, key = { it.id }) { food ->
                        Card(shape = RoundedCornerShape(10.dp)) {
                            Column(modifier = Modifier.padding(10.dp).fillMaxWidth()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(food.name, fontWeight = FontWeight.Medium)
                                        Text(
                                            "${AppRepository.formatPrice(food.price)} تومان",
                                            color = OrangeSecondary,
                                            style = MaterialTheme.typography.bodySmall
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
                                    Text("$qty", fontWeight = FontWeight.Bold, modifier = Modifier.width(24.dp))
                                    IconButton(onClick = {
                                        quantities = quantities.toMutableMap().apply { put(food.id, qty + 1) }
                                    }) { Icon(Icons.Default.Add, null) }
                                }
                                if (food.extraSkewerPrice > 0) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("🍢 سیخ اضافه", style = MaterialTheme.typography.bodyMedium)
                                        Spacer(modifier = Modifier.weight(1f))
                                        Text(
                                            "${AppRepository.formatPrice(food.extraSkewerPrice)}",
                                            color = OrangeSecondary,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                        val sk = "${food.id}__skewer"
                                        val sq = quantities[sk] ?: 0
                                        IconButton(
                                            onClick = {
                                                quantities = quantities.toMutableMap().apply {
                                                    if (sq <= 1) remove(sk) else put(sk, sq - 1)
                                                }
                                            },
                                            enabled = sq > 0
                                        ) { Icon(Icons.Default.Remove, null) }
                                        Text("$sq", fontWeight = FontWeight.Bold, modifier = Modifier.width(24.dp))
                                        IconButton(onClick = {
                                            quantities = quantities.toMutableMap().apply { put(sk, sq + 1) }
                                        }) { Icon(Icons.Default.Add, null) }
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
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = paidNow == afterCredit.toString(),
                            onClick = { paidNow = afterCredit.toString() },
                            label = { Text("قابل پرداخت: ${AppRepository.formatPrice(afterCredit)}", fontWeight = FontWeight.Bold) }
                        )
                        FilterChip(
                            selected = paidNow == "0",
                            onClick = { paidNow = "0" },
                            label = { Text("نسیه (۰)") }
                        )
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
                            val c = selectedCustomer ?: return@Button
                            val paid = paidNow.toLongOrNull() ?: 0L
                            scope.launch {
                                saving = true
                                val result = AppRepository.createOrder(c, cartItems, paid)
                                if (ApiConfig.isConfigured) {
                                    ApiClient.createOrder(
                                        customerId = c.id,
                                        items = cartItems,
                                        paidNow = paid,
                                        note = result.order.note,
                                        source = "kitchen"
                                    )
                                }
                                resultMessage = result.message
                                quantities = emptyMap()
                                paidNow = ""
                                saving = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        enabled = cartItems.isNotEmpty() && !saving,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (saving) CircularProgressIndicator(modifier = Modifier.size(22.dp), color = MaterialTheme.colorScheme.onPrimary)
                        else Text("ثبت سفارش", fontWeight = FontWeight.Bold)
                    }
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
