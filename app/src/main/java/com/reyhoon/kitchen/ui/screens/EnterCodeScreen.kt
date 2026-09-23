package com.reyhoon.kitchen.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reyhoon.kitchen.R
import com.reyhoon.kitchen.data.MockData
import com.reyhoon.kitchen.data.Subscription
import com.reyhoon.kitchen.ui.theme.GreenPrimary

@Composable
fun EnterCodeScreen(
    onCodeVerified: (Subscription) -> Unit,
    modifier: Modifier = Modifier
) {
    var code by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val isCompact = maxWidth < 600.dp
        val horizontalPadding = if (isCompact) 24.dp else 48.dp
        val cardMaxWidth = if (isCompact) maxWidth else 480.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = horizontalPadding)
                .padding(vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Kitchen,
                contentDescription = null,
                modifier = Modifier.size(if (isCompact) 72.dp else 96.dp),
                tint = GreenPrimary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.welcome),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "آشپزخانه ریحون",
                style = MaterialTheme.typography.titleLarge,
                color = GreenPrimary,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(32.dp))

            Card(
                modifier = Modifier
                    .widthIn(max = cardMaxWidth)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.enter_code_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = code,
                        onValueChange = {
                            code = it.uppercase()
                            errorMessage = null
                        },
                        label = { Text(stringResource(R.string.enter_code_hint)) },
                        singleLine = true,
                        isError = errorMessage != null,
                        supportingText = {
                            if (errorMessage != null) {
                                Text(errorMessage!!, color = MaterialTheme.colorScheme.error)
                            } else {
                                Text(stringResource(R.string.code_example))
                            }
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                                verifyCode(code, onCodeVerified) { errorMessage = it; isLoading = false }
                            }
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = {
                            isLoading = true
                            focusManager.clearFocus()
                            verifyCode(code, onCodeVerified) { errorMessage = it; isLoading = false }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        enabled = code.isNotBlank() && !isLoading,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = stringResource(R.string.verify_code),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "کدهای نمونه: REYHOON123  |  REYHOON456  |  TEST001",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun verifyCode(
    code: String,
    onSuccess: (Subscription) -> Unit,
    onError: (String) -> Unit
) {
    val sub = MockData.findSubscription(code)
    if (sub != null && sub.isActive) {
        onSuccess(sub)
    } else {
        onError("کد اشتراک نامعتبر یا منقضی شده است")
    }
}
