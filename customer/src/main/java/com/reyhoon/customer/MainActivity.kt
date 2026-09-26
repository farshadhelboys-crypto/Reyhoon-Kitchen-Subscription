package com.reyhoon.customer

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object SessionPrefs {
    private const val PREF = "reyhoon_customer_session"
    private const val KEY_CODE = "saved_subscription_code"
    private const val KEY_REMEMBER = "remember_me"
    fun saveCode(ctx: Context, code: String) {
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit()
            .putString(KEY_CODE, code).putBoolean(KEY_REMEMBER, true).apply()
    }
    fun clear(ctx: Context) {
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().clear().apply()
    }
    fun getSavedCode(ctx: Context): String? {
        val p = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        if (!p.getBoolean(KEY_REMEMBER, false)) return null
        return p.getString(KEY_CODE, null)?.takeIf { it.isNotBlank() }
    }
}

class MainActivity : ComponentActivity() {
    private val notifPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ensureChannel(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        enableEdgeToEdge()
        setContent {
            MaterialTheme(colorScheme = lightColorScheme(
                primary = Color(0xFF2E7D32), secondary = Color(0xFFE65100),
                background = Color(0xFFF1F8E9), surface = Color.White,
                onPrimary = Color.White, onBackground = Color(0xFF0A0A0A), onSurface = Color(0xFF0A0A0A)
            )) {
                Surface(modifier = Modifier.fillMaxSize()) { CustomerApp() }
            }
        }
    }
    companion object {
        const val CHANNEL = "reyhoon_customer_status"
        fun ensureChannel(ctx: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val ch = NotificationChannel(CHANNEL, "وضعیت سفارش ریحون", NotificationManager.IMPORTANCE_HIGH)
                ctx.getSystemService(NotificationManager::class.java)?.createNotificationChannel(ch)
            }
        }
        fun notifyStatus(ctx: Context, title: String, body: String) {
            ensureChannel(ctx)
            val n = NotificationCompat.Builder(ctx, CHANNEL)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title).setContentText(body)
                .setPriority(NotificationCompat.PRIORITY_HIGH).setAutoCancel(true)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)).build()
            try { NotificationManagerCompat.from(ctx).notify((System.currentTimeMillis() % Int.MAX_VALUE).toInt(), n) } catch (_: SecurityException) {}
        }
    }
}

@Composable
fun CustomerApp() {
    val context = LocalContext.current
    var customer by remember { mutableStateOf<Customer?>(null) }
    var showNewCode by remember { mutableStateOf<String?>(null) }
    var autoLoginTried by remember { mutableStateOf(false) }
    var autoLoginLoading by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (autoLoginTried) return@LaunchedEffect
        autoLoginTried = true
        val code = SessionPrefs.getSavedCode(context)
        if (code != null) {
            autoLoginLoading = true
            val c = ApiClient.login(code)
            autoLoginLoading = false
            if (c != null) customer = c else SessionPrefs.clear(context)
        }
    }
    when {
        autoLoginLoading -> Box(Modifier.fillMaxSize().background(Color(0xFFF1F8E9)), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = Color(0xFF2E7D32))
                Spacer(modifier = Modifier.height(16.dp))
                Text("در حال ورود خودکار...", fontWeight = FontWeight.SemiBold, color = Color(0xFF2E7D32))
            }
        }
        customer == null -> WelcomeScreen(onLoggedIn = { customer = it }, onRegistered = { c -> showNewCode = c.subscriptionCode; customer = c })
        showNewCode != null -> NewCodeDialog(code = showNewCode!!, onDismiss = { showNewCode = null })
        else -> MainTabs(customer = customer!!, onLogout = { SessionPrefs.clear(context); customer = null })
    }
}

@Composable
fun NewCodeDialog(code: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    LaunchedEffect(code) { if (code.isNotBlank()) SessionPrefs.saveCode(context, code) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("اشتراک شما ساخته شد", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("کد اشتراک خود را یادداشت کنید:")
                Spacer(modifier = Modifier.height(12.dp))
                Text(code, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                Text("کد ذخیره شد — دفعه بعد خودکار وارد می‌شوید.")
            }
        },
        confirmButton = { Button(onClick = onDismiss) { Text("متوجه شدم — بریم منو") } }
    )
}

@Composable
fun KitchenContactFooter() {
    val context = LocalContext.current
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Text("آدرس: رشت · توشیبا · خیابان پاستور ۱ · آشپزخانه ریحون", style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center, color = Color(0xFF424242))
        Text("تلفن آشپزخانه: ۰۹۹۱۹۷۲۷۸۴۱", fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
        Text("توسعه‌دهنده: فرشاد پورمحمد", style = MaterialTheme.typography.bodySmall, color = Color(0xFF616161))
        TextButton(onClick = { try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/farshad_pm_org"))) } catch (_: Exception) {} }) {
            Text("✈ تلگرام", color = Color(0xFF0088CC), fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun WelcomeScreen(onLoggedIn: (Customer) -> Unit, onRegistered: (Customer) -> Unit) {
    var mode by remember { mutableStateOf<String?>(null) }
    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF1F8E9)).padding(24.dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("ریحون", fontSize = 34.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
        Text("سفارش آنلاین غذا", fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
        Spacer(modifier = Modifier.height(28.dp))
        if (mode == null) {
            Button(onClick = { mode = "new" }, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(14.dp)) { Text("مشتری جدید هستم", fontWeight = FontWeight.Bold) }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(onClick = { mode = "existing" }, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(14.dp)) { Text("کد اشتراک دارم", fontWeight = FontWeight.Bold) }
        }
        if (mode == "existing") ExistingLogin(onLoggedIn = onLoggedIn, onBack = { mode = null })
        if (mode == "new") NewRegister(onRegistered = onRegistered, onBack = { mode = null })
        Spacer(modifier = Modifier.height(32.dp))
        KitchenContactFooter()
    }
}

@Composable
fun ExistingLogin(onLoggedIn: (Customer) -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    var code by remember { mutableStateOf("") }
    var rememberMe by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    Text("ورود با کد اشتراک", fontWeight = FontWeight.Bold, fontSize = 18.sp)
    OutlinedTextField(value = code, onValueChange = { code = it.filter { ch -> ch.isDigit() }; error = null }, label = { Text("کد اشتراک") }, singleLine = true, modifier = Modifier.fillMaxWidth())
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = rememberMe, onCheckedChange = { rememberMe = it }, colors = CheckboxDefaults.colors(checkedColor = Color(0xFF2E7D32)))
        Text("مرا به خاطر داشته باش", fontWeight = FontWeight.SemiBold)
    }
    error?.let { Text(it, color = Color(0xFFC62828), fontWeight = FontWeight.Bold) }
    Button(onClick = {
        scope.launch {
            loading = true
            val c = ApiClient.login(code)
            loading = false
            if (c != null) { if (rememberMe) SessionPrefs.saveCode(context, code) else SessionPrefs.clear(context); onLoggedIn(c) }
            else error = "کد یافت نشد"
        }
    }, enabled = code.isNotBlank() && !loading, modifier = Modifier.fillMaxWidth().height(50.dp)) {
        if (loading) CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.White) else Text("ورود", fontWeight = FontWeight.Bold)
    }
    TextButton(onClick = onBack) { Text("بازگشت") }
}

@Composable
fun NewRegister(onRegistered: (Customer) -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var street by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var lat by remember { mutableStateOf<Double?>(null) }
    var lng by remember { mutableStateOf<Double?>(null) }
    var locMsg by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val locPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val ok = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (ok) {
            val pair = readLastLocation(context)
            if (pair != null) {
                lat = pair.first
                lng = pair.second
                locMsg = "موقعیت ثبت شد ✓"
            } else {
                locMsg = "موقعیت یافت نشد — GPS را روشن کنید و دوباره بزنید"
            }
        } else {
            locMsg = "دسترسی موقعیت لازم است"
        }
    }
    Text("ثبت‌نام سریع", fontWeight = FontWeight.Bold, fontSize = 18.sp)
    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("نام") }, singleLine = true, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(value = phone, onValueChange = { phone = it.filter { ch -> ch.isDigit() } }, label = { Text("تلفن") }, singleLine = true, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(value = street, onValueChange = { street = it }, label = { Text("آدرس متنی") }, singleLine = true, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(value = city, onValueChange = { city = it }, label = { Text("شهر") }, singleLine = true, modifier = Modifier.fillMaxWidth())
    Spacer(modifier = Modifier.height(8.dp))
    Text("لوکیشن روی نقشه (الزامی)", fontWeight = FontWeight.Bold, color = Color(0xFFC62828))
    Text("برای ارسال پیک باید موقعیت دقیق ثبت شود.", style = MaterialTheme.typography.bodySmall)
    Button(
        onClick = {
            val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
            if (fine || coarse) {
                val pair = readLastLocation(context)
                if (pair != null) {
                    lat = pair.first
                    lng = pair.second
                    locMsg = "موقعیت ثبت شد ✓"
                } else {
                    locMsg = "موقعیت یافت نشد — GPS را روشن کنید"
                }
            } else {
                locPermission.launch(arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ))
            }
        },
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0))
    ) { Text("📍 ثبت موقعیت فعلی از GPS", fontWeight = FontWeight.Bold) }
    if (lat != null && lng != null) {
        Text("مختصات: ${"%.5f".format(lat)} , ${"%.5f".format(lng)}", fontWeight = FontWeight.SemiBold, color = Color(0xFF2E7D32))
        TextButton(onClick = {
            try {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("geo:$lat,$lng?q=$lat,$lng(منزل)")))
            } catch (_: Exception) { }
        }) { Text("مشاهده روی نقشه") }
    }
    locMsg?.let { Text(it, color = if (lat != null) Color(0xFF2E7D32) else Color(0xFFC62828), fontWeight = FontWeight.SemiBold) }
    error?.let { Text(it, color = Color(0xFFC62828), fontWeight = FontWeight.Bold) }
    Button(
        onClick = {
            val la = lat
            val ln = lng
            if (la == null || ln == null) {
                error = "ابتدا لوکیشن را از GPS ثبت کنید"
                return@Button
            }
            scope.launch {
                loading = true
                val c = ApiClient.register(name.trim(), phone.trim(), street.trim(), city.trim(), la, ln)
                loading = false
                if (c != null) onRegistered(c) else error = "خطا در ثبت‌نام"
            }
        },
        enabled = name.isNotBlank() && phone.length >= 10 && lat != null && lng != null && !loading,
        modifier = Modifier.fillMaxWidth().height(50.dp)
    ) {
        if (loading) CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.White)
        else Text("ثبت و ورود", fontWeight = FontWeight.Bold)
    }
    TextButton(onClick = onBack) { Text("بازگشت") }
}

@Suppress("MissingPermission")
private fun readLastLocation(context: Context): Pair<Double, Double>? {
    return try {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
        val providers = listOf(
            android.location.LocationManager.GPS_PROVIDER,
            android.location.LocationManager.NETWORK_PROVIDER
        )
        var best: android.location.Location? = null
        for (p in providers) {
            try {
                val l = lm.getLastKnownLocation(p) ?: continue
                if (best == null || l.accuracy < best!!.accuracy) best = l
            } catch (_: Exception) { }
        }
        best?.let { it.latitude to it.longitude }
    } catch (_: Exception) { null }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainTabs(customer: Customer, onLogout: () -> Unit) {
    var tab by remember { mutableIntStateOf(0) }
    var showLogoutConfirm by remember { mutableStateOf(false) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Column { Text("سلام ${customer.name}", fontWeight = FontWeight.Bold); Text("کد: ${customer.subscriptionCode ?: "—"} | بدهی: ${fmt(customer.debt)}", style = MaterialTheme.typography.bodySmall) } },
                actions = { IconButton(onClick = { showLogoutConfirm = true }) { Icon(Icons.AutoMirrored.Filled.Logout, "خروج") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF2E7D32), titleContentColor = Color.White, actionIconContentColor = Color.White)
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
            when (tab) { 0 -> MenuOrderTab(customer); 1 -> OrdersTab(customer) }
        }
    }
    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            title = { Text("خروج از حساب", fontWeight = FontWeight.Bold) },
            text = { Text("با خروج، دفعه بعد باید دوباره کد را وارد کنید.") },
            confirmButton = { Button(onClick = { showLogoutConfirm = false; onLogout() }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))) { Text("خروج") } },
            dismissButton = { TextButton(onClick = { showLogoutConfirm = false }) { Text("انصراف") } }
        )
    }
}

private fun categoryEmoji(cat: String): String {
    val c = cat.trim()
    return when {
        c.contains("چلو") -> "🍚"
        c.contains("کباب") -> "🍢"
        c.contains("خورشت") -> "🍲"
        c.contains("نوشید") -> "🥤"
        c.contains("مخلف") -> "🥗"
        else -> "🍽️"
    }
}

@Composable
fun MenuOrderTab(customer: Customer) {
    var menu by remember { mutableStateOf<List<FoodItem>>(emptyList()) }
    var qty by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var tierFilter by remember { mutableStateOf<String?>(null) }
    var msg by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) { while (true) { menu = ApiClient.menu(); delay(15_000) } }
    val filteredMenu = if (tierFilter == null) menu else menu.filter { it.priceTier == tierFilter }
    val cart = filteredMenu.flatMap { f ->
        val list = mutableListOf<OrderItem>()
        val q = qty[f.id] ?: 0
        if (q > 0) list.add(OrderItem(f.id, f.name, f.price, q, f.priceTier))
        val sq = qty["${f.id}__skewer"] ?: 0
        if (sq > 0 && f.extraSkewerPrice > 0) list.add(OrderItem("${f.id}__skewer", "سیخ اضافه (${f.name})", f.extraSkewerPrice, sq, f.priceTier))
        list
    }
    val total = cart.sumOf { it.unitPrice * it.quantity }
    val grouped = filteredMenu.groupBy { it.category.ifBlank { "عمومی" } }
    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        Text("آدرس: ${customer.address.full()}", fontWeight = FontWeight.SemiBold)
        if (customer.credit > 0) Text("اعتبار: ${fmt(customer.credit)} تومان", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            FilterChip(selected = tierFilter == null, onClick = { tierFilter = null }, label = { Text("همه") })
            FilterChip(selected = tierFilter == "economy", onClick = { tierFilter = "economy" }, label = { Text("اقتصادی") })
            FilterChip(selected = tierFilter == "regular", onClick = { tierFilter = "regular" }, label = { Text("غیر اقتصادی") })
        }
        Spacer(modifier = Modifier.height(8.dp))
        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            grouped.forEach { (cat, list) ->
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)), shape = RoundedCornerShape(10.dp)) {
                        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(categoryEmoji(cat), fontSize = 26.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(cat.ifBlank { "عمومی" }, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Color(0xFF2E7D32))
                        }
                    }
                }
                items(list, key = { it.id }) { f ->
                    Card(shape = RoundedCornerShape(12.dp)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(categoryEmoji(f.category), fontSize = 22.sp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(f.name, fontWeight = FontWeight.Bold)
                                    if (f.description.isNotBlank()) {
                                        Text(f.description, style = MaterialTheme.typography.bodySmall, color = Color(0xFF616161))
                                    }
                                    Text("${fmt(f.price)} تومان", color = Color(0xFFE65100), fontWeight = FontWeight.Bold)
                                }
                                IconButton(onClick = { qty = qty.toMutableMap().apply { put(f.id, ((qty[f.id] ?: 0) - 1).coerceAtLeast(0)) } }) { Icon(Icons.Filled.Remove, null) }
                                Text("${qty[f.id] ?: 0}", fontWeight = FontWeight.Bold)
                                IconButton(onClick = { qty = qty.toMutableMap().apply { put(f.id, (qty[f.id] ?: 0) + 1) } }) { Icon(Icons.Filled.Add, null) }
                            }
                            if (f.extraSkewerPrice > 0) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("🍢 سیخ اضافه", fontWeight = FontWeight.Medium)
                                    Spacer(modifier = Modifier.weight(1f))
                                    Text("${fmt(f.extraSkewerPrice)}", color = Color(0xFFE65100), fontWeight = FontWeight.Bold)
                                    val sk = "${f.id}__skewer"
                                    IconButton(onClick = { qty = qty.toMutableMap().apply { put(sk, ((qty[sk] ?: 0) - 1).coerceAtLeast(0)) } }) { Icon(Icons.Filled.Remove, null) }
                                    Text("${qty[sk] ?: 0}", fontWeight = FontWeight.Bold)
                                    IconButton(onClick = { qty = qty.toMutableMap().apply { put(sk, (qty[sk] ?: 0) + 1) } }) { Icon(Icons.Filled.Add, null) }
                                }
                            }
                        }
                    }
                }
            }
        }
        if (total > 0) {
            Text("جمع: ${fmt(total)} تومان", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Button(onClick = { scope.launch { val ok = ApiClient.placeOrder(customer.id, cart); msg = if (ok) "سفارش ثبت شد ✓" else "خطا"; if (ok) qty = emptyMap() } }, modifier = Modifier.fillMaxWidth().height(50.dp)) { Text("ثبت سفارش", fontWeight = FontWeight.Bold) }
        }
        msg?.let { Text(it, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32)) }
        KitchenContactFooter()
    }
}

@Composable
fun OrdersTab(customer: Customer) {
    val context = LocalContext.current
    var orders by remember { mutableStateOf<List<Order>>(emptyList()) }
    var rateOrderId by remember { mutableStateOf<String?>(null) }
    var knownStatus by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(customer.id) {
        while (true) {
            val list = ApiClient.orders(customer.id)
            list.forEach { o ->
                val prev = knownStatus[o.id]
                if (prev != null && prev != o.status) MainActivity.notifyStatus(context, "ریحون", "وضعیت: ${o.statusFa}")
            }
            knownStatus = list.associate { it.id to it.status }
            orders = list
            delay(10_000)
        }
    }
    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        Text("پیگیری سفارش‌ها", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
            items(orders, key = { it.id }) { o ->
                Card(shape = RoundedCornerShape(12.dp)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(o.statusFa, fontWeight = FontWeight.Bold, color = if (o.status == "cancelled") Color(0xFFC62828) else Color(0xFF2E7D32))
                        o.items.forEach { Text("• ${it.foodName} × ${it.quantity}") }
                        Text("جمع: ${fmt(o.totalAmount)} تومان", fontWeight = FontWeight.SemiBold)
                        if (o.canCancel) {
                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        if (ApiClient.cancelOrder(o.id)) {
                                            orders = ApiClient.orders(customer.id)
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFC62828))
                            ) { Text("لغو سفارش", fontWeight = FontWeight.Bold) }
                        }
                        if (o.status == "shipped") {
                            Button(onClick = { scope.launch { if (ApiClient.confirmDelivered(o.id)) { orders = ApiClient.orders(customer.id); rateOrderId = o.id } } }, modifier = Modifier.fillMaxWidth()) { Text("تحویل گرفتم") }
                        }
                        if (o.status == "delivered" && !o.rated) {
                            TextButton(onClick = { rateOrderId = o.id }) { Text("امتیاز به پیک") }
                        }
                        if (o.status == "cancelled") {
                            Text("این سفارش لغو شده است", color = Color(0xFFC62828), fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
        KitchenContactFooter()
    }
    rateOrderId?.let { oid ->
        var stars by remember { mutableIntStateOf(5) }
        var comment by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { rateOrderId = null },
            title = { Text("امتیاز به پیک") },
            text = {
                Column {
                    Row { (1..5).forEach { i -> IconButton(onClick = { stars = i }) { Icon(Icons.Filled.Star, null, tint = if (i <= stars) Color(0xFFFFC107) else Color(0xFFBDBDBD)) } } }
                    OutlinedTextField(value = comment, onValueChange = { comment = it }, label = { Text("نظر") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = { Button(onClick = { scope.launch { ApiClient.rateOrder(oid, stars, comment); orders = ApiClient.orders(customer.id); rateOrderId = null } }) { Text("ثبت") } },
            dismissButton = { TextButton(onClick = { rateOrderId = null }) { Text("بعداً") } }
        )
    }
}

private fun fmt(n: Long): String = "%,d".format(n).replace(',', '،')
