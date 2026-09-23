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
import com.reyhoon.kitchen.data.FoodItem
import com.reyhoon.kitchen.data.MenuCategories
import com.reyhoon.kitchen.data.OrderItem
import com.reyhoon.kitchen.ui.theme.GreenPrimary
import com.reyhoon.kitchen.ui.theme.OrangeSecondary
import kotlinx.coroutines.launch

/**
 * ثبت سفارش دستی برای مشتری که با کد اشتراک وارد شده
 * (مخصوص حضور حضوری مشتری در آشپزخانه)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val customer by AppRepository.currentCustomer
    var menu by remember { mutableStateOf<List<FoodItem>>(emptyList()) }
    var quantities by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var paidNow by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var resultMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        loading = true
        menu = if (ApiConfig.isConfigured) {
            val remote = ApiClient.fetchMenu()
            if (remote.isNotEmpty()) {
                AppRepository.menuItems.clear()
                AppRepository.menuItems.addAll(remote)
                remote
            } else {
                AppRepository.menuItems.filter { it.isAvailable }
            }
        } else {
            AppRepository.menuItems.filter { it.isAvailable }
        }
        loading = false
    }

    val cartItems = remember(quantities, menu) {
        menu.mapNotNull { food ->
            val q = quantities[food.id] ?: 0
            if (q > 0) OrderItem(food.id, food.name, food.price, q) else null
        }
    }
    val total = cartItems.sumOf { it.total }
    val credit = customer?.credit ?: 0L
    val afterCredit = (total - credit).coerceAtLeast(0)
    val grouped = remember(menu) { menu.groupBy { it.category.ifBlank { "عمومی" } } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("ثبت سفارش", fontWeight = FontWeight.Bold)
                        customer?.let {
                            Text(
                                "${it.name} — کد ${it.subscriptionCode ?: "—"}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                },
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
        if (customer == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("مشتری انتخاب نشده — با کد اشتراک وارد شوید")
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            if (credit > 0) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = GreenPrimary.copy(alpha = 0.12f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "اعتبار مشتری: ${AppRepository.formatPrice(credit)} تومان از مبلغ سفارش کسر می‌شود.",
                        modifier = Modifier.padding(12.dp),
                        color = GreenPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            if (loading && menu.isEmpty()) {
                Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (menu.isEmpty()) {
                Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Text("منو خالی است — از پنل ادمین غذا اضافه کنید")
                }
            } else {
                Text("انتخاب غذاها (دسته‌بندی‌شده)", fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    grouped.forEach { (cat, list) ->
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = GreenPrimary.copy(alpha = 0.12f)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(MenuCategories.emoji(cat), fontSize = 26.sp)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        cat,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 17.sp,
                                        color = GreenPrimary
                                    )
                                }
                            }
                        }
                        items(list, key = { it.id }) { food ->
                            Card(shape = RoundedCornerShape(10.dp)) {
                                Row(
                                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
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
                                        quantities = quantities.toMutableMap().apply {
                                            put(food.id, qty + 1)
                                        }
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
                if (credit > 0) {
                    Text(
                        "پس از کسر اعتبار: ${AppRepository.formatPrice(afterCredit)} تومان",
                        color = GreenPrimary,
                        fontWeight = FontWeight.SemiBold
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
                        Text("۰ = نسیه کامل | بیشتر از مبلغ = اعتبار برای بعد")
                    }
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
                                    note = "سفارش حضوری با کد اشتراک",
                                    source = "kitchen"
                                )
                                if (remote != null) {
                                    val local = AppRepository.createOrder(c, cartItems, paid)
                                    resultMessage = local.message
                                    quantities = emptyMap()
                                    paidNow = ""
                                } else {
                                    val local = AppRepository.createOrder(c, cartItems, paid)
                                    resultMessage = local.message + "\n(هشدار: ممکن است روی سرور ذخیره نشده باشد)"
                                    quantities = emptyMap()
                                    paidNow = ""
                                }
                            } else {
                                val local = AppRepository.createOrder(c, cartItems, paid)
                                resultMessage = local.message
                                quantities = emptyMap()
                                paidNow = ""
                            }
                            loading = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    enabled = cartItems.isNotEmpty() && !loading,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (loading) CircularProgressIndicator(modifier = Modifier.size(22.dp), color = MaterialTheme.colorScheme.onPrimary)
                    else Text("ثبت سفارش برای ${customer!!.name}", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    resultMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = { resultMessage = null },
            title = { Text("سفارش ثبت شد") },
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
