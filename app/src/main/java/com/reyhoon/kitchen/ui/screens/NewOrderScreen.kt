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
import com.reyhoon.kitchen.data.AppRepository
import com.reyhoon.kitchen.data.Customer
import com.reyhoon.kitchen.data.FoodItem
import com.reyhoon.kitchen.data.OrderItem
import com.reyhoon.kitchen.ui.theme.GreenPrimary
import com.reyhoon.kitchen.ui.theme.OrangeSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewOrderScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val customers = AppRepository.customers
    val menu = AppRepository.menuItems.filter { it.isAvailable }

    var selectedCustomer by remember { mutableStateOf<Customer?>(null) }
    var quantities by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var paidNow by remember { mutableStateOf("") }
    var resultMessage by remember { mutableStateOf<String?>(null) }

    val cartItems = remember(quantities, menu) {
        menu.mapNotNull { food ->
            val q = quantities[food.id] ?: 0
            if (q > 0) OrderItem(food.id, food.name, food.price, q) else null
        }
    }
    val total = cartItems.sumOf { it.total }
    val customerCredit = selectedCustomer?.credit ?: 0L
    val afterCredit = (total - customerCredit).coerceAtLeast(0)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ثبت سفارش جدید", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "بازگشت")
                    }
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
            Spacer(modifier = Modifier.height(8.dp))
            if (customers.isEmpty()) {
                Text("ابتدا مشتری اضافه کنید.", color = MaterialTheme.colorScheme.error)
            } else {
                var expanded by remember { mutableStateOf(false) }
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
                                        Text("${c.name} (${c.phone})")
                                        if (c.credit > 0) {
                                            Text(
                                                "اعتبار: ${AppRepository.formatPrice(c.credit)} تومان",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = GreenPrimary
                                            )
                                        }
                                        if (c.debt > 0) {
                                            Text(
                                                "بدهی: ${AppRepository.formatPrice(c.debt)} تومان",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        }
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
            }

            if (selectedCustomer != null && customerCredit > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = GreenPrimary.copy(alpha = 0.12f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        "این مشتری ${AppRepository.formatPrice(customerCredit)} تومان اعتبار دارد و از مبلغ سفارش کسر می‌شود.",
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = GreenPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("انتخاب غذاها", fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(menu, key = { it.id }) { food ->
                    FoodQtyRow(
                        food = food,
                        qty = quantities[food.id] ?: 0,
                        onChange = { newQty ->
                            quantities = quantities.toMutableMap().apply {
                                if (newQty <= 0) remove(food.id) else put(food.id, newQty)
                            }
                        }
                    )
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
                OutlinedTextField(
                    value = paidNow,
                    onValueChange = { paidNow = it.filter { c -> c.isDigit() } },
                    label = { Text("مبلغ دریافتی از مشتری (تومان)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    supportingText = {
                        Text("می‌توانید بیشتر از مبلغ وارد کنید → مازاد به‌عنوان اعتبار مشتری ذخیره می‌شود")
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        val c = selectedCustomer ?: return@Button
                        val paid = paidNow.toLongOrNull() ?: 0L
                        val result = AppRepository.createOrder(c, cartItems, paid)
                        resultMessage = result.message
                        selectedCustomer = AppRepository.findCustomer(c.id)
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    enabled = selectedCustomer != null && cartItems.isNotEmpty(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("ثبت سفارش")
                }
            }
        }
    }

    resultMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = {
                resultMessage = null
                onBack()
            },
            title = { Text("نتیجه ثبت سفارش") },
            text = { Text(msg) },
            confirmButton = {
                Button(onClick = {
                    resultMessage = null
                    onBack()
                }) { Text("باشه") }
            }
        )
    }
}

@Composable
private fun FoodQtyRow(food: FoodItem, qty: Int, onChange: (Int) -> Unit) {
    Card(shape = RoundedCornerShape(10.dp)) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(food.name, fontWeight = FontWeight.Medium)
                Text(
                    "${AppRepository.formatPrice(food.price)} تومان",
                    style = MaterialTheme.typography.bodySmall,
                    color = OrangeSecondary
                )
            }
            IconButton(onClick = { onChange(qty - 1) }, enabled = qty > 0) {
                Icon(Icons.Default.Remove, null)
            }
            Text("$qty", fontWeight = FontWeight.Bold, modifier = Modifier.width(24.dp))
            IconButton(onClick = { onChange(qty + 1) }) {
                Icon(Icons.Default.Add, null)
            }
        }
    }
}
