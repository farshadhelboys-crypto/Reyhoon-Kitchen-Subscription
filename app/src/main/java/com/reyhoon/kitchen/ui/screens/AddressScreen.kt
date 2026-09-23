package com.reyhoon.kitchen.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.reyhoon.kitchen.data.Address
import com.reyhoon.kitchen.data.Subscription
import com.reyhoon.kitchen.ui.theme.GreenPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddressScreen(
    subscription: Subscription,
    onSave: (Address) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var street by remember { mutableStateOf(subscription.address.street) }
    var city by remember { mutableStateOf(subscription.address.city) }
    var postalCode by remember { mutableStateOf(subscription.address.postalCode) }
    var phone by remember { mutableStateOf(subscription.address.phone) }
    var notes by remember { mutableStateOf(subscription.address.notes) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ویرایش آدرس", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت")
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
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            val isWide = maxWidth >= 600.dp
            val contentPadding = if (isWide) 32.dp else 16.dp
            val maxFormWidth = if (isWide) 520.dp else maxWidth

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(contentPadding),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Card(
                    modifier = Modifier.widthIn(max = maxFormWidth).fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = street,
                            onValueChange = { street = it },
                            label = { Text("آدرس کامل / خیابان") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = false,
                            maxLines = 3
                        )
                        OutlinedTextField(
                            value = city,
                            onValueChange = { city = it },
                            label = { Text("شهر") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = postalCode,
                            onValueChange = { postalCode = it },
                            label = { Text("کد پستی") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("شماره تماس") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            label = { Text("یادداشت (اختیاری)") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = false,
                            maxLines = 2
                        )
                    }
                }

                Button(
                    onClick = {
                        onSave(
                            Address(
                                street = street.trim(),
                                city = city.trim(),
                                postalCode = postalCode.trim(),
                                phone = phone.trim(),
                                notes = notes.trim()
                            )
                        )
                    },
                    modifier = Modifier
                        .widthIn(max = maxFormWidth)
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = street.isNotBlank() && city.isNotBlank() && phone.isNotBlank()
                ) {
                    Text("ذخیره آدرس", fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}
