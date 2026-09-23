package com.reyhoon.customer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = Color(0xFF2E7D32),
                    secondary = Color(0xFFE65100),
                    background = Color(0xFFF1F8E9),
                    surface = Color.White,
                    onPrimary = Color.White,
                    onBackground = Color(0xFF0A0A0A),
                    onSurface = Color(0xFF0A0A0A)
                )
            ) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    CustomerApp()
                }
            }
        }
    }
}

@Composable
fun CustomerApp() {
    var customer by remember { mutableStateOf<Customer?>(null) }
    if (customer == null) {
        LoginScreen(onLogin = { customer = it })
    } else {
        MainTabs(customer = customer!!, onLogout = { customer = null })
    }
}

@Composable
fun LoginScreen(onLogin: (Customer) -> Unit) {
    var code by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF1F8E9))
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("ریحان", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
        Text("اپ مشتریان", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(8.dp))
        if (!ApiConfig.isConfigured) {
            Text(
                "آدرس سرور تنظیم نشده. در ApiConfig.kt مقدار baseUrl را بگذارید.",
                color = Color(0xFFC62828),
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        OutlinedTextField(
            value = code,
            onValueChange = { code = it.uppercase().trim(); error = null },
            label = { Text("کد اشتراک") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            isError = error != null,
            supportingText = { error?.let { Text(it, color = Color(0xFFC62828)) } }
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                scope.launch {
                    loading = true
                    val c = ApiClient.login(code)
                    loading = false
                    if (c != null) onLogin(c) else error = "کد یافت نشد یا سرور در دسترس نیست"
                }
            },
            enabled = code.isNotBlank() && !loading && ApiConfig.isConfigured,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            if (loading) {
                CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
            } else {
                Text("ورود", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainTabs(customer: Customer, onLogout: () -> Unit) {
    var tab by remember { mutableIntStateOf(0) }
    val freshCustomer = customer

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("سلام ${freshCustomer.name}", fontWeight = FontWeight.Bold)
                        Text(
                            "بدهی: ${fmt(freshCustomer.debt)} | اعتبار: ${fmt(freshCustomer.credit)}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.AutoMirrored.Filled.Logout, null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF2E7D32),
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == 0,
                    onClick = { tab = 0 },
                    icon = { Text("منو") },
                    label = { Text("منو و سفارش") }
                )
                NavigationBarItem(
                    selected = tab == 1,
                    onClick = { tab = 1 },
                    icon = { Text("سفارش") },
                    label = { Text("وضعیت سفارش") }
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (tab) {
                0 -> MenuOrderTab(freshCustomer)
                1 -> OrdersTab(freshCustomer)
            }
        }
    }
}

@Composable
fun MenuOrderTab(customer: Customer) {
    var menu by remember { mutableStateOf<List<FoodItem>>(emptyList()) }
    var qty by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var msg by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) { menu = ApiClient.menu() }

    val cart = menu.mapNotNull { f ->
        val q = qty[f.id] ?: 0
        if (q > 0) OrderItem(f.id, f.name, f.price, q) else null
    }
    val total = cart.sumOf { it.unitPrice * it.quantity }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        Text("آدرس: ${customer.address.full()}", fontWeight = FontWeight.SemiBold)
        if (customer.credit > 0) {
            Text(
                "اعتبار شما ${fmt(customer.credit)} تومان از سفارش کسر می‌شود",
                color = Color(0xFF2E7D32),
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(menu, key = { it.id }) { f ->
                Card(shape = RoundedCornerShape(12.dp)) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(f.name, fontWeight = FontWeight.Bold)
                            Text(
                                "${fmt(f.price)} تومان",
                                color = Color(0xFFE65100),
                                fontWeight = FontWeight.Bold
                            )
                        }
                        IconButton(onClick = {
                            qty = qty.toMutableMap().apply {
                                put(f.id, ((qty[f.id] ?: 0) - 1).coerceAtLeast(0))
                            }
                        }) { Icon(Icons.Default.Remove, null) }
                        Text("${qty[f.id] ?: 0}", fontWeight = FontWeight.Bold)
                        IconButton(onClick = {
                            qty = qty.toMutableMap().apply {
                                put(f.id, (qty[f.id] ?: 0) + 1)
                            }
                        }) { Icon(Icons.Default.Add, null) }
                    }
                }
            }
        }
        if (total > 0) {
            Text("جمع: ${fmt(total)} تومان", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Button(
                onClick = {
                    scope.launch {
                        val ok = ApiClient.placeOrder(customer.id, cart)
                        msg = if (ok) "سفارش ثبت شد" else "خطا در ثبت سفارش"
                        if (ok) qty = emptyMap()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text("ثبت سفارش", fontWeight = FontWeight.Bold)
            }
        }
        msg?.let {
            Text(it, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
        }
    }
}

@Composable
fun OrdersTab(customer: Customer) {
    var orders by remember { mutableStateOf<List<Order>>(emptyList()) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(customer.id) {
        while (true) {
            orders = ApiClient.orders(customer.id)
            delay(10_000)
        }
    }

    fun ts(t: Long?): String =
        if (t == null || t <= 0) "—"
        else SimpleDateFormat("yyyy/MM/dd HH:mm", Locale("fa")).format(Date(t))

    LazyColumn(
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("سفارش‌های من", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                IconButton(onClick = { scope.launch { orders = ApiClient.orders(customer.id) } }) {
                    Icon(Icons.Default.Refresh, null)
                }
            }
        }
        items(orders, key = { it.id }) { o ->
            Card(shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(o.statusFa, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                    Text("ثبت: ${ts(o.createdAt)}")
                    if (o.deliveredAt != null) Text("تحویل: ${ts(o.deliveredAt)}")
                    o.items.forEach { Text("• ${it.foodName} × ${it.quantity}") }
                    Text(
                        "${fmt(o.totalAmount)} تومان",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE65100)
                    )
                    if (o.status != "delivered") {
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                scope.launch {
                                    if (ApiClient.confirmDelivered(o.id)) {
                                        orders = ApiClient.orders(customer.id)
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("تحویل گرفتم", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

private fun fmt(n: Long) = "%,d".format(n).replace(',', '٬')
