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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.reyhoon.kitchen.data.Address
import com.reyhoon.kitchen.data.ApiClient
import com.reyhoon.kitchen.data.ApiConfig
import com.reyhoon.kitchen.data.AppRepository
import com.reyhoon.kitchen.data.Customer
import com.reyhoon.kitchen.ui.theme.GreenPrimary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminCustomersScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    var showAddDialog by remember { mutableStateOf(false) }
    var paymentCustomer by remember { mutableStateOf<Customer?>(null) }
    var deleteConfirm by remember { mutableStateOf<Customer?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    val customers = AppRepository.customers
    val scope = rememberCoroutineScope()

    suspend fun refresh() {
        loading = true
        if (ApiConfig.isConfigured) {
            val remote = ApiClient.fetchCustomers()
            AppRepository.customers.clear()
            AppRepository.customers.addAll(remote)
            message = "همگام با سرور — ${remote.size} مشتری"
        }
        loading = false
    }

    LaunchedEffect(Unit) { refresh() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("مشتریان و بدهی", fontWeight = FontWeight.Bold)
                        Text("ساخت / حذف / تسویه روی سرور", style = MaterialTheme.typography.bodySmall)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "بازگشت") }
                },
                actions = {
                    IconButton(onClick = { scope.launch { refresh() } }) { Icon(Icons.Default.Refresh, "بروزرسانی") }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = GreenPrimary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
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
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            message?.let {
                Text(it, modifier = Modifier.padding(12.dp), fontWeight = FontWeight.SemiBold, color = GreenPrimary)
            }
            if (loading && customers.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else if (customers.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("مشتری‌ای ثبت نشده. با + اضافه کنید.")
                }
            } else {
                LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(customers, key = { it.id }) { c ->
                        Card(shape = RoundedCornerShape(12.dp)) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(c.name, fontWeight = FontWeight.SemiBold)
                                        Text("تلفن: ${c.phone}", style = MaterialTheme.typography.bodySmall)
                                        if (!c.subscriptionCode.isNullOrBlank()) {
                                            Text("کد اشتراک: ${c.subscriptionCode}", style = MaterialTheme.typography.bodyMedium, color = GreenPrimary, fontWeight = FontWeight.Bold)
                                        }
                                        Text("آدرس: ${c.address.fullAddress()}", style = MaterialTheme.typography.bodySmall)
                                    }
                                    IconButton(onClick = { deleteConfirm = c }) {
                                        Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                val realDebt = AppRepository.recalculateDebt(c.id).coerceAtLeast(c.debt)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (realDebt > 0) {
                                        AssistChip(
                                            onClick = { paymentCustomer = c },
                                            label = { Text("بدهی: ${AppRepository.formatPrice(realDebt)} — بزن برای تسویه") },
                                            colors = AssistChipDefaults.assistChipColors(labelColor = MaterialTheme.colorScheme.error)
                                        )
                                    }
                                    if (c.credit > 0) {
                                        AssistChip(onClick = { }, label = { Text("اعتبار: ${AppRepository.formatPrice(c.credit)}") },
                                            colors = AssistChipDefaults.assistChipColors(labelColor = GreenPrimary))
                                    }
                                }
                                if (realDebt > 0) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    FilledTonalButton(onClick = { paymentCustomer = c }, modifier = Modifier.fillMaxWidth()) {
                                        Icon(Icons.Default.Payments, null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("تسویه بدهی")
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
        var name by remember { mutableStateOf("") }
        var phone by remember { mutableStateOf("") }
        var code by remember { mutableStateOf("") }
        var street by remember { mutableStateOf("") }
        var city by remember { mutableStateOf("") }
        var priorDebt by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("افزودن مشتری") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("کد خالی = تولید خودکار ۶ رقمی روی سرور", style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("نام") }, singleLine = true)
                    OutlinedTextField(value = phone, onValueChange = { phone = it.filter { ch -> ch.isDigit() } }, label = { Text("تلفن") }, singleLine = true)
                    OutlinedTextField(value = code, onValueChange = { code = it.filter { ch -> ch.isDigit() } }, label = { Text("کد اشتراک (اختیاری)") }, singleLine = true)
                    OutlinedTextField(value = street, onValueChange = { street = it }, label = { Text("آدرس") }, singleLine = true)
                    OutlinedTextField(value = city, onValueChange = { city = it }, label = { Text("شهر") }, singleLine = true)
                    OutlinedTextField(
                        value = priorDebt,
                        onValueChange = { priorDebt = it.filter { ch -> ch.isDigit() } },
                        label = { Text("بدهی قبلی (تومان) — اختیاری") },
                        supportingText = { Text("اگر از قبل بدهکار است وارد کنید؛ بعداً با تسویه کسر می‌شود") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank() && phone.isNotBlank()) {
                            scope.launch {
                                loading = true
                                val debtAmt = priorDebt.toLongOrNull() ?: 0L
                                val draft = Customer(
                                    name = name.trim(),
                                    phone = phone.trim(),
                                    subscriptionCode = code.trim().ifBlank { null },
                                    debt = debtAmt,
                                    address = Address(street = street.trim(), city = city.trim())
                                )
                                var saved = draft
                                if (ApiConfig.isConfigured) {
                                    val remote = ApiClient.createCustomer(draft)
                                    if (remote != null) {
                                        saved = remote
                                        message = buildString {
                                            append("مشتری روی سرور — کد: ${remote.subscriptionCode}")
                                            if (debtAmt > 0) append(" | بدهی قبلی: ${AppRepository.formatPrice(debtAmt)}")
                                        }
                                        val remoteOrders = ApiClient.fetchOrders(remote.id)
                                        if (remoteOrders.isNotEmpty()) {
                                            remoteOrders.forEach { o ->
                                                val i = AppRepository.orders.indexOfFirst { it.id == o.id }
                                                if (i >= 0) AppRepository.orders[i] = o
                                                else AppRepository.orders.add(0, o)
                                            }
                                        } else if (debtAmt > 0) {
                                            AppRepository.ensurePriorDebtOrder(saved)
                                        }
                                    } else {
                                        message = "خطا در سرور — فقط محلی"
                                        if (debtAmt > 0) AppRepository.ensurePriorDebtOrder(saved)
                                    }
                                } else if (debtAmt > 0) {
                                    AppRepository.ensurePriorDebtOrder(saved)
                                }
                                AppRepository.addCustomer(saved)
                                showAddDialog = false
                                loading = false
                                refresh()
                            }
                        }
                    },
                    enabled = name.isNotBlank() && phone.isNotBlank()
                ) { Text("ذخیره") }
            },
            dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text("انصراف") } }
        )
    }

    deleteConfirm?.let { c ->
        AlertDialog(
            onDismissRequest = { deleteConfirm = null },
            title = { Text("حذف مشتری") },
            text = { Text("«${c.name}» از سرور و اپ حذف شود؟") },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            if (ApiConfig.isConfigured) {
                                val ok = ApiClient.deleteCustomer(c.id)
                                message = if (ok) "مشتری از سرور حذف شد" else "خطا در حذف از سرور"
                            }
                            AppRepository.deleteCustomer(c.id)
                            deleteConfirm = null
                            refresh()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("حذف قطعی") }
            },
            dismissButton = { TextButton(onClick = { deleteConfirm = null }) { Text("انصراف") } }
        )
    }

    paymentCustomer?.let { c ->
        val debt = AppRepository.recalculateDebt(c.id).coerceAtLeast(c.debt)
        var amount by remember(c.id, debt) { mutableStateOf(if (debt > 0) debt.toString() else "") }
        AlertDialog(
            onDismissRequest = { paymentCustomer = null },
            title = { Text("تسویه بدهی - ${c.name}") },
            text = {
                Column {
                    Text("فقط بعد از تسویه کامل، مازاد به اعتبار می‌رود.", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("برای پر کردن فیلد، روی مبلغ بزنید:", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(6.dp))
                    FilterChip(
                        selected = amount == debt.toString(),
                        onClick = { amount = debt.toString() },
                        label = {
                            Text(
                                "کل بدهی: ${AppRepository.formatPrice(debt)} تومان",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = it.filter { ch -> ch.isDigit() } },
                        label = { Text("مبلغ (تومان)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val a = amount.toLongOrNull() ?: 0L
                    if (a > 0) scope.launch {
                        if (ApiConfig.isConfigured) ApiClient.recordPayment(c.id, a, "تسویه توسط ادمین")
                        AppRepository.recordPayment(c.id, a, note = "تسویه توسط ادمین")
                        paymentCustomer = null
                        refresh()
                    }
                }, enabled = (amount.toLongOrNull() ?: 0) > 0) { Text("ثبت پرداخت") }
            },
            dismissButton = { TextButton(onClick = { paymentCustomer = null }) { Text("انصراف") } }
        )
    }
}
