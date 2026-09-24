package com.reyhoon.kitchen.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
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
import com.reyhoon.kitchen.data.PriceTiers
import com.reyhoon.kitchen.ui.theme.GreenPrimary
import com.reyhoon.kitchen.ui.theme.OrangeSecondary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminMenuScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    var items by remember { mutableStateOf<List<FoodItem>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var showDialog by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<FoodItem?>(null) }
    val scope = rememberCoroutineScope()

    suspend fun refresh() {
        loading = true
        if (ApiConfig.isConfigured) {
            val remote = ApiClient.fetchMenu()
            items = remote
            AppRepository.menuItems.clear()
            AppRepository.menuItems.addAll(remote)
            message = "همگام با سرور — ${remote.size} غذا"
        } else {
            items = AppRepository.menuItems.toList()
            message = "آفلاین"
        }
        loading = false
    }

    LaunchedEffect(Unit) { refresh() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("مدیریت منو", fontWeight = FontWeight.Bold)
                        Text("اقتصادی / غیر اقتصادی در هر دسته", style = MaterialTheme.typography.bodySmall)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "بازگشت")
                    }
                },
                actions = {
                    IconButton(onClick = { scope.launch { refresh() } }) {
                        Icon(Icons.Default.Refresh, "بروزرسانی")
                    }
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
            FloatingActionButton(onClick = { editing = null; showDialog = true }, containerColor = GreenPrimary) {
                Icon(Icons.Default.Add, "افزودن", tint = MaterialTheme.colorScheme.onPrimary)
            }
        },
        modifier = modifier
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            message?.let {
                Text(it, modifier = Modifier.padding(16.dp), fontWeight = FontWeight.SemiBold, color = GreenPrimary)
            }
            if (loading && items.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else if (items.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("هنوز غذایی نیست. با + اضافه کنید.")
                }
            } else {
                val grouped = items.groupBy { it.category.ifBlank { "عمومی" } }
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    grouped.forEach { (cat, list) ->
                        item {
                            Text(
                                MenuCategories.label(cat),
                                fontWeight = FontWeight.Bold,
                                color = GreenPrimary,
                                fontSize = 18.sp,
                                modifier = Modifier.padding(vertical = 6.dp)
                            )
                        }
                        listOf(PriceTiers.ECONOMY, PriceTiers.REGULAR).forEach { tier ->
                            val tierList = list.filter { it.priceTier == tier }
                            if (tierList.isNotEmpty()) {
                                item(key = cat + "_" + tier + "_h") {
                                    Text(
                                        PriceTiers.label(tier),
                                        fontWeight = FontWeight.SemiBold,
                                        color = OrangeSecondary,
                                        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                                    )
                                }
                                items(tierList, key = { it.id }) { item ->
                                    Card(shape = RoundedCornerShape(12.dp)) {
                                        Row(
                                            modifier = Modifier.padding(14.dp).fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(MenuCategories.emoji(item.category), fontSize = 28.sp)
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(item.name, fontWeight = FontWeight.SemiBold)
                                                Text(
                                                    buildString {
                                                        append(item.priceTierLabel)
                                                        append(" | ${AppRepository.formatPrice(item.price)} تومان")
                                                        if (item.extraSkewerPrice > 0)
                                                            append(" | سیخ: ${AppRepository.formatPrice(item.extraSkewerPrice)}")
                                                    },
                                                    color = OrangeSecondary, fontWeight = FontWeight.Medium
                                                )
                                            }
                                            IconButton(onClick = { editing = item; showDialog = true }) {
                                                Icon(Icons.Default.Edit, "ویرایش")
                                            }
                                            IconButton(onClick = {
                                                scope.launch {
                                                    if (ApiConfig.isConfigured) ApiClient.deleteMenuItem(item.id)
                                                    else AppRepository.deleteFood(item.id)
                                                    refresh()
                                                }
                                            }) {
                                                Icon(Icons.Default.Delete, "حذف", tint = MaterialTheme.colorScheme.error)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        FoodEditDialog(
            initial = editing,
            onDismiss = { showDialog = false },
            onSave = { food ->
                scope.launch {
                    val result = if (ApiConfig.isConfigured) {
                        if (editing != null) ApiClient.updateMenuItem(food) else ApiClient.createMenuItem(food)
                    } else {
                        if (editing != null) AppRepository.updateFood(food) else AppRepository.addFood(food)
                        food
                    }
                    if (result != null) {
                        message = "ذخیره شد — ${MenuCategories.label(food.category)} / ${food.priceTierLabel}"
                        showDialog = false
                        refresh()
                    } else message = "خطا در ذخیره"
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FoodEditDialog(
    initial: FoodItem?,
    onDismiss: () -> Unit,
    onSave: (FoodItem) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var price by remember { mutableStateOf(initial?.price?.toString() ?: "") }
    var skewerPrice by remember {
        mutableStateOf(
            if ((initial?.extraSkewerPrice ?: 0L) > 0) initial!!.extraSkewerPrice.toString() else ""
        )
    }
    var category by remember { mutableStateOf(initial?.category ?: MenuCategories.ALL.first()) }
    var priceTier by remember { mutableStateOf(initial?.priceTier ?: PriceTiers.REGULAR) }
    var desc by remember { mutableStateOf(initial?.description ?: "") }
    var catExpanded by remember { mutableStateOf(false) }
    var tierExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "افزودن غذا" else "ویرایش غذا") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("نام غذا") }, singleLine = true)
                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it.filter { c -> c.isDigit() } },
                    label = { Text("قیمت (تومان)") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = skewerPrice,
                    onValueChange = { skewerPrice = it.filter { c -> c.isDigit() } },
                    label = { Text("قیمت سیخ اضافه (اختیاری)") },
                    supportingText = { Text("برای کباب‌ها — اگر خالی باشد گزینه سیخ نمایش داده نمی‌شود") },
                    singleLine = true
                )
                ExposedDropdownMenuBox(expanded = catExpanded, onExpandedChange = { catExpanded = it }) {
                    OutlinedTextField(
                        value = MenuCategories.label(category),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("دسته") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(catExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = catExpanded, onDismissRequest = { catExpanded = false }) {
                        MenuCategories.ALL.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(MenuCategories.label(cat)) },
                                onClick = { category = cat; catExpanded = false }
                            )
                        }
                    }
                }
                ExposedDropdownMenuBox(expanded = tierExpanded, onExpandedChange = { tierExpanded = it }) {
                    OutlinedTextField(
                        value = PriceTiers.label(priceTier),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("ستون قیمت") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(tierExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = tierExpanded, onDismissRequest = { tierExpanded = false }) {
                        PriceTiers.ALL.forEach { t ->
                            DropdownMenuItem(
                                text = { Text(PriceTiers.label(t)) },
                                onClick = { priceTier = t; tierExpanded = false }
                            )
                        }
                    }
                }
                OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("توضیحات") }, maxLines = 2)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val p = price.toLongOrNull() ?: 0L
                    val sk = skewerPrice.toLongOrNull() ?: 0L
                    if (name.isNotBlank() && p > 0) {
                        onSave(
                            FoodItem(
                                id = initial?.id ?: java.util.UUID.randomUUID().toString(),
                                name = name.trim(),
                                description = desc.trim(),
                                price = p,
                                category = category,
                                isAvailable = initial?.isAvailable ?: true,
                                extraSkewerPrice = sk,
                                priceTier = priceTier
                            )
                        )
                    }
                },
                enabled = name.isNotBlank() && (price.toLongOrNull() ?: 0) > 0
            ) { Text("ذخیره") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } }
    )
}
