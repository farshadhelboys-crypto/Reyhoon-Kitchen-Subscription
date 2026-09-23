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
import com.reyhoon.kitchen.data.AppRepository
import com.reyhoon.kitchen.ui.theme.GreenPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddressScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val customer = AppRepository.currentCustomer.value
    if (customer == null) {
        onBack()
        return
    }

    var street by remember { mutableStateOf(customer.address.street) }
    var city by remember { mutableStateOf(customer.address.city) }
    var postalCode by remember { mutableStateOf(customer.address.postalCode) }
    var notes by remember { mutableStateOf(customer.address.notes) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ویرایش آدرس", fontWeight = FontWeight.Bold) },
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = street,
                onValueChange = { street = it },
                label = { Text("آدرس کامل") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
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
                value = notes,
                onValueChange = { notes = it },
                label = { Text("یادداشت (اختیاری)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                maxLines = 2
            )

            Button(
                onClick = {
                    val newAddr = Address(
                        street = street.trim(),
                        city = city.trim(),
                        postalCode = postalCode.trim(),
                        notes = notes.trim()
                    )
                    val updated = customer.copy(address = newAddr)
                    AppRepository.updateCustomer(updated)
                    AppRepository.currentCustomer.value = updated
                    onBack()
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = street.isNotBlank() && city.isNotBlank()
            ) {
                Text("ذخیره آدرس")
            }
        }
    }
}
