package com.reyhoon.kitchen.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.reyhoon.kitchen.data.Address
import com.reyhoon.kitchen.data.AppRepository
import com.reyhoon.kitchen.data.Customer
import com.reyhoon.kitchen.ui.theme.GreenPrimary
import com.reyhoon.kitchen.ui.theme.OrangeSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminCustomersScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var paymentCustomer by remember { mutableStateOf<Customer?>(null) }
    val customers = AppRepository.customers

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("مشتریان و بدهی", fontWeight = FontWeight.Bold) },
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
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }, containerColor = GreenPrimary) {
                Icon(Icons.Default.Add, "افزودن مشتری", tint = MaterialTheme.colorScheme.onPrimary)
            }
        },
        modifier = modifier
    ) { padding ->
        if (customers.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("مشتری‌ای ثبت نشده. با + اضافه کنید.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(padding)
            ) {
                items(customers, key = { it.id }) { c ->
                    Card(shape = RoundedCornerShape(12.dp)) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(c.name, fontWeight = FontWeight.SemiBold)
                                    Text(c.phone, style = MaterialTheme.typography.bodySmall)
                                    if (!c.subscriptionCode.isNullOrBlank()) {
                                        Text("کد: ${c.subscriptionCode}", style = MaterialTheme.typography.bodySmall, color = GreenPrimary)
                                    }
                                    Text(
                                        c.address.fullAddress(),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )
                                }
                                IconButton(onClick = { AppRepository.deleteCustomer(c.id) }) {
                                    Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
                                }
                            }
                            if (c.debt > 0) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        "بدهی: ${AppRepository.formatPrice(c.debt)} تومان",
                                        color = OrangeSecondary,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.weight(1f)
                                    )
                                    FilledTonalButton(onClick = { paymentCustomer = c }) {
                                        Icon(Icons.Default.Payments, null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("تسویه")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddCustomerDialog(
            onDismiss = { showAddDialog = false },
            onSave = {
                AppRepository.addCustomer(it)
                showAddDialog = false
            }
        )
    }

    paymentCustomer?.let { c ->
        PaymentDialog(
            customer = c,
            onDismiss = { paymentCustomer = null },
            onPay = { amount ->
                AppRepository.recordPayment(c.id, amount, note = "تسویه توسط ادمین")
                paymentCustomer = null
            }
        )
    }
}

@Composable
private fun AddCustomerDialog(onDismiss: () -> Unit, onSave: (Customer) -> Unit) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var street by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("افزودن مشتری") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("نام") }, singleLine = true)
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("تلفن") }, singleLine = true)
                OutlinedTextField(value = code, onValueChange = { code = it.uppercase() }, label = { Text("کد اشتراک (اختیاری)") }, singleLine = true)
                OutlinedTextField(value = street, onValueChange = { street = it }, label = { Text("آدرس") }, singleLine = true)
                OutlinedTextField(value = city, onValueChange = { city = it }, label = { Text("شهر") }, singleLine = true)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && phone.isNotBlank()) {
                        onSave(
                            Customer(
                                name = name.trim(),
                                phone = phone.trim(),
                                subscriptionCode = code.trim().ifBlank { null },
                                address = Address(street = street.trim(), city = city.trim())
                            )
                        )
                    }
                },
                enabled = name.isNotBlank() && phone.isNotBlank()
            ) { Text("ذخیره") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } }
    )
}

@Composable
private fun PaymentDialog(customer: Customer, onDismiss: () -> Unit, onPay: (Long) -> Unit) {
    var amount by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تسویه بدهی - ${customer.name}") },
        text = {
            Column {
                Text("بدهی فعلی: ${AppRepository.formatPrice(customer.debt)} تومان")
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it.filter { c -> c.isDigit() } },
                    label = { Text("مبلغ پرداختی (تومان)") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val a = amount.toLongOrNull() ?: 0L
                    if (a > 0) onPay(a)
                },
                enabled = (amount.toLongOrNull() ?: 0) > 0
            ) { Text("ثبت پرداخت") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } }
    )
}
