package com.reyhoon.kitchen.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reyhoon.kitchen.data.ApiClient
import com.reyhoon.kitchen.data.ApiConfig
import com.reyhoon.kitchen.data.AppRepository
import com.reyhoon.kitchen.ui.components.ReyhoonLogo
import com.reyhoon.kitchen.ui.theme.GreenMid
import com.reyhoon.kitchen.ui.theme.GreenPale
import com.reyhoon.kitchen.ui.theme.OrangeSecondary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnterCodeScreen(
    onCodeEntered: () -> Unit,
    onAdminLogin: () -> Unit,
    modifier: Modifier = Modifier
) {
    var code by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun submit() {
        val trimmed = code.trim()
        if (trimmed.isBlank() || !trimmed.all { it.isDigit() }) {
            errorMessage = "کد اشتراک فقط عدد است"
            return
        }
        scope.launch {
            loading = true
            errorMessage = null
            val customer = if (ApiConfig.isConfigured) {
                ApiClient.fetchCustomerByCode(trimmed)
            } else {
                AppRepository.findCustomerByCode(trimmed)
            }
            loading = false
            if (customer != null) {
                AppRepository.currentCustomer.value = customer
                if (AppRepository.customers.none { it.id == customer.id }) {
                    AppRepository.customers.add(customer)
                }
                onCodeEntered()
            } else {
                errorMessage = "کد اشتراک یافت نشد"
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(GreenPale, MaterialTheme.colorScheme.background)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            ReyhoonLogo(size = 110.dp)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "خوش آمدید",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Medium,
                color = GreenMid.copy(alpha = 0.85f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "به آشپزخانه ریحون",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = GreenMid
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "ورود با کد اشتراک برای ثبت سفارش حضوری",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                elevation = CardDefaults.cardElevation(8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("ورود مشتری حضوری", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "کد اشتراک مشتری را بزنید و سفارش ثبت کنید",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    OutlinedTextField(
                        value = code,
                        onValueChange = {
                            code = it.filter { ch -> ch.isDigit() }.take(10)
                            errorMessage = null
                        },
                        label = { Text("کد اشتراک (عدد)") },
                        singleLine = true,
                        isError = errorMessage != null,
                        supportingText = {
                            if (errorMessage != null) {
                                Text(errorMessage!!, color = MaterialTheme.colorScheme.error)
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Button(
                        onClick = { submit() },
                        enabled = code.isNotBlank() && !loading,
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GreenMid)
                    ) {
                        if (loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("ورود و ثبت سفارش", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            TextButton(onClick = onAdminLogin) {
                Icon(Icons.Default.AdminPanelSettings, null, tint = OrangeSecondary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("ورود ادمین / حسابداری", color = OrangeSecondary, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
