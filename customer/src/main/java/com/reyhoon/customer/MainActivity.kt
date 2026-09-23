package com.reyhoon.customer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
    var showNewCode by remember { mutableStateOf<String?>(null) }

    when {
        customer == null -> WelcomeScreen(
            onLoggedIn = { customer = it },
            onRegistered = { c ->
                showNewCode = c.subscriptionCode
                customer = c
            }
        )
        showNewCode != null -> NewCodeDialog(
            code = showNewCode!!,
            onDismiss = { showNewCode = null }
        )
        else -> MainTabs(customer = customer!!, onLogout = { customer = null })
    }
}

@Composable
fun NewCodeDialog(code: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("اشتراک شما ساخته شد", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("کد اشتراک خود را یادداشت کنید:")
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    code,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2E7D32),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("دفعه بعد با همین کد وارد شوید.")
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("متوجه شدم — بریم منو") }
        }
    )
}

@Composable
fun WelcomeScreen(
    onLoggedIn: (Customer) -> Unit,
    onRegistered: (Customer) -> Unit
) {
    var mode by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF1F8E9))
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("ریحان", fontSize = 34.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
        Text("سفارش آنلاین غذا", fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
        Spacer(modifier = Modifier.height(28.dp))

        if (mode == null) {
            Button(
                onClick = { mode = "new" },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(14.dp)
            ) { Text("مشتری جدید هستم", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = { mode = "existing" },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(14.dp)
            ) { Text("کد اشتراک دارم", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
        }

        if (mode == "existing") {
            ExistingLogin(onLoggedIn = onLoggedIn, onBack = { mode = null })
        }
        if (mode == "new") {
            NewRegister(onRegistered = onRegistered, onBack = { mode = null })
        }
    }
}

@Composable
fun ExistingLogin(onLoggedIn: (Customer) -> Unit, onBack: () -> Unit) {
    var code by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Text("ورود با کد اشتراک", fontWeight = FontWeight.Bold, fontSize = 18.sp)
    Spacer(modifier = Modifier.height(12.dp))
    OutlinedTextField(
        value = code,
        onValueChange = { code = it.filter { ch -> ch.isDigit() }; error = null },
        label = { Text("کد اشتراک (فقط عدد)") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
    error?.let { Text(it, color = Color(0xFFC62828), fontWeight = FontWeight.Bold) }
    Spacer(modifier = Modifier.height(12.dp))
    Button(
        onClick = {
            scope.launch {
                loading = true
                val c = ApiClient.login(code)
                loading = false
                if (c != null) onLoggedIn(c) else error = "کد یافت نشد"
            }
        },
        enabled = code.isNotBlank() && !loading,
        modifier = Modifier.fillMaxWidth().height(50.dp)
    ) {
        if (loading) CircularProgressIndicator(Modifier = Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
        else Text("ورود", fontWeight = FontWeight.Bold)
    }
    TextButton(onClick = onBack) { Text("بازگشت") }
}

@Composable
fun NewRegister(onRegistered: (Customer) -> Unit, onBack: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var street by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Text("ثبت‌نام سریع", fontWeight = FontWeight.Bold, fontSize = 18.sp)
    Text("فوراً کد اشتراک می‌گیرید و می‌توانید سفارش دهید", style = MaterialTheme.typography.bodyMedium)
    Spacer(modifier = Modifier.height(12.dp))
    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("نام") }, singleLine = true, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(value = phone, onValueChange = { phone = it.filter { ch -> ch.isDigit() } }, label = { Text("تلفن") }, singleLine = true, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(value = street, onValueChange = { street = it }, label = { Text("آدرس کامل") }, singleLine = true, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(value = city, onValueChange = { city = it }, label = { Text("شهر") }, singleLine = true, modifier = Modifier.fillMaxWidth())
    error?.let { Text(it, color = Color(0xFFC62828), fontWeight = FontWeight.Bold) }
    Spacer(modifier = Modifier.height(12.dp))
    Button(
        onClick = {
            scope.launch {
                loading = true
                val c = ApiClient.register(name.trim(), phone.trim(), street.trim(), city.trim())
                loading = false
                if (c != null) onRegistered(c) else error = "خطا در ثبت‌نام — اینترنت را چک کنید"
            }
        },
        enabled = name.isNotBlank() && phone.length >= 10 && !loading,
        modifier = Modifier.fillMaxWidth().height(50.dp)
    ) {
        if (loading) CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
        else Text("ثبت و ورود", fontWeight = FontWeight.Bold)
    }
    TextButton(onClick = onBack) { Text("بازگشت") }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainTabs(customer: Customer, onLogout: () -> Unit) {
    var tab by remember { mutableIntStateOf(0) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("سلام ${customer.name}", fontWeight = FontWeight.Bold)
                        Text(
                            "کد: ${customer.subscriptionCode ?: "—"} | بدهی: ${fmt(customer.debt)}",
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
                NavigationBarItem(selected = tab == 0, onClick = { tab = 0 }, icon = { Text("منو") }, label = { Text("سفارش") })
                NavigationBarItem(selected = tab == 1, onClick = { tab = 1 }, icon = { Text("پیگیری") }, label = { Text("سفارش‌ها") })
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (tab) {
                0 -> MenuOrderTab(customer)
                1 -> OrdersTab(customer)
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

    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        Text("آدرس: ${customer.address.full()}", fontWeight = FontWeight.SemiBold)
        if (customer.credit > 0) {
            Text("اعتبار: ${fmt(customer.credit)} تومان از سفارش کسر می‌شود", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(8.dp))
        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(menu, key = { it.id }) { f ->
                Card(shape = RoundedCornerShape(12.dp)) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(f.name, fontWeight = FontWeight.Bold)
                            Text("${fmt(f.price)} تومان", color = Color(0xFFE65100), fontWeight = FontWeight.Bold)
                        }
                        IconButton(onClick = {
                            qty = qty.toMutableMap().apply { put(f.id, ((qty[f.id] ?: 0) - 1).coerceAtLeast(0)) }
                        }) { Icon(Icons.Filled.Remove, null) }
                        Text("${qty[f.id] ?: 0}", fontWeight = FontWeight.Bold)
                        IconButton(onClick = {
                            qty = qty.toMutableMap().apply { put(f.id, (qty[f.id] ?: 0) + 1) }
                        }) { Icon(Icons.Filled.Add, null) }
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
                        msg = if (ok) "سفارش ثبت شد ✓ آشپزخانه مطلع شد" else "خطا در ثبت"
                        if (ok) qty = emptyMap()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) { Text("ثبت سفارش", fontWeight = FontWeight.Bold) }
        }
        msg?.let { Text(it, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32)) }
    }
}

@Composable
fun OrdersTab(customer: Customer) {
    var orders by remember { mutableStateOf<List<Order>>(emptyList()) }
    var rateOrderId by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(customer.id) {
        while (true) {
            orders = ApiClient.orders(customer.id)
            delay(8_000)
        }
    }

    fun ts(t: Long?) =
        if (t == null || t <= 0) "—"
        else SimpleDateFormat("yyyy/MM/dd HH:mm", Locale("fa")).format(Date(t))

    LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("سفارش‌های من", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                IconButton(onClick = { scope.launch { orders = ApiClient.orders(customer.id) } }) {
                    Icon(Icons.Filled.Refresh, null)
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
                    Text("${fmt(o.totalAmount)} تومان", fontWeight = FontWeight.Bold, color = Color(0xFFE65100))
                    if (o.status != "delivered") {
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                scope.launch {
                                    if (ApiClient.confirmDelivered(o.id)) {
                                        orders = ApiClient.orders(customer.id)
                                        rateOrderId = o.id
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("تحویل گرفتم", fontWeight = FontWeight.Bold) }
                    } else if (!o.rated) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { rateOrderId = o.id },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("به پیک امتیاز بده") }
                    } else {
                        Text("امتیاز ثبت شد ✓", color = Color(0xFF2E7D32))
                    }
                }
            }
        }
    }

    rateOrderId?.let { oid ->
        RateDialog(
            onDismiss = { rateOrderId = null },
            onSubmit = { stars, comment ->
                scope.launch {
                    ApiClient.rateOrder(oid, stars, comment)
                    orders = ApiClient.orders(customer.id)
                    rateOrderId = null
                }
            }
        )
    }
}

@Composable
fun RateDialog(onDismiss: () -> Unit, onSubmit: (Int, String) -> Unit) {
    var stars by remember { mutableIntStateOf(5) }
    var comment by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("به پیک چه امتیازی می‌دهید؟", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                    (1..5).forEach { n ->
                        IconButton(onClick = { stars = n }) {
                            Icon(
                                Icons.Filled.Star,
                                contentDescription = null,
                                tint = if (n <= stars) Color(0xFFFFC107) else Color(0xFFBDBDBD),
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text("نظر (اختیاری)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSubmit(stars, comment) }) { Text("ثبت امتیاز") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("بعداً") }
        }
    )
}

private fun fmt(n: Long) = "%,d".format(n).replace(',', '٬')
