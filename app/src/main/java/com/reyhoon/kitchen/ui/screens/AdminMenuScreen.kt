package com.reyhoon.kitchen.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.reyhoon.kitchen.data.AppRepository
import com.reyhoon.kitchen.data.FoodItem
import com.reyhoon.kitchen.ui.theme.GreenPrimary
import com.reyhoon.kitchen.ui.theme.OrangeSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminMenuScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddDialog by remember { mutableStateOf(false) }
    val items = AppRepository.menuItems

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("مدیریت منو", fontWeight = FontWeight.Bold) },
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
                Icon(Icons.Default.Add, "افزودن", tint = MaterialTheme.colorScheme.onPrimary)
            }
        },
        modifier = modifier
    ) { padding ->
        if (items.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("هنوز غذایی ثبت نشده. با + اضافه کنید.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(padding)
            ) {
                items(items, key = { it.id }) { item ->
                    Card(shape = RoundedCornerShape(12.dp)) {
                        Row(
                            modifier = Modifier.padding(14.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.name, fontWeight = FontWeight.SemiBold)
                                Text(item.category, style = MaterialTheme.typography.bodySmall)
                                Text(
                                    "${AppRepository.formatPrice(item.price)} تومان",
                                    color = OrangeSecondary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            IconButton(onClick = { AppRepository.deleteFood(item.id) }) {
                                Icon(Icons.Default.Delete, "حذف", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddFoodDialog(
            onDismiss = { showAddDialog = false },
            onSave = { food ->
                AppRepository.addFood(food)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun AddFoodDialog(onDismiss: () -> Unit, onSave: (FoodItem) -> Unit) {
    var name by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("عمومی") }
    var desc by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("افزودن غذا") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("نام غذا") }, singleLine = true)
                OutlinedTextField(value = price, onValueChange = { price = it.filter { c -> c.isDigit() } }, label = { Text("قیمت (تومان)") }, singleLine = true)
                OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("دسته") }, singleLine = true)
                OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("توضیحات") }, maxLines = 2)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val p = price.toLongOrNull() ?: 0L
                    if (name.isNotBlank() && p > 0) {
                        onSave(FoodItem(name = name.trim(), description = desc.trim(), price = p, category = category.trim().ifBlank { "عمومی" }))
                    }
                },
                enabled = name.isNotBlank() && (price.toLongOrNull() ?: 0) > 0
            ) { Text("ذخیره") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("انصراف") }
        }
    )
}
