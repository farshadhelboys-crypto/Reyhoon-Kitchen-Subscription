package com.reyhoon.kitchen.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.reyhoon.kitchen.data.Subscription
import com.reyhoon.kitchen.ui.theme.GreenPrimary
import com.reyhoon.kitchen.ui.theme.OrangeSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    subscription: Subscription,
    onNavigateToMenu: () -> Unit,
    onNavigateToAddress: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ریحون", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "خروج")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = GreenPrimary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
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

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(contentPadding),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Welcome Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "سلام ${subscription.customerName} عزیز 👋",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "اشتراک شما فعال است",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }

                // Subscription Info
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CardMembership, contentDescription = null, tint = GreenPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("اطلاعات اشتراک", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        InfoRow("پلن", subscription.planName)
                        InfoRow("کد اشتراک", subscription.code)
                        InfoRow("شروع", subscription.startDate)
                        InfoRow("پایان", subscription.endDate)
                    }
                }

                // Address Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    onClick = onNavigateToAddress
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.LocationOn, contentDescription = null, tint = OrangeSecondary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("آدرس تحویل", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            }
                            Icon(Icons.Default.Edit, contentDescription = "ویرایش", tint = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = subscription.address.fullAddress(),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        if (subscription.address.notes.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "یادداشت: ${subscription.address.notes}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "تلفن: ${subscription.address.phone}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                // Action Buttons
                if (isWide) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Button(
                            onClick = onNavigateToMenu,
                            modifier = Modifier.weight(1f).height(56.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.RestaurantMenu, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("مشاهده منو")
                        }
                        OutlinedButton(
                            onClick = onNavigateToAddress,
                            modifier = Modifier.weight(1f).height(56.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.EditLocation, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("ویرایش آدرس")
                        }
                    }
                } else {
                    Button(
                        onClick = onNavigateToMenu,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.RestaurantMenu, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("مشاهده منوی غذاها")
                    }
                    OutlinedButton(
                        onClick = onNavigateToAddress,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.EditLocation, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("ویرایش آدرس تحویل")
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}
