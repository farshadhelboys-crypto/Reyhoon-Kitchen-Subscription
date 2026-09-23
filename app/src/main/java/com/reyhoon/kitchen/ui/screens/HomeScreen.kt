package com.reyhoon.kitchen.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.reyhoon.kitchen.data.AppRepository
import com.reyhoon.kitchen.ui.components.ReyhoonLogo
import com.reyhoon.kitchen.ui.theme.GreenMid
import com.reyhoon.kitchen.ui.theme.GreenPale
import com.reyhoon.kitchen.ui.theme.OrangeSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToMenu: () -> Unit,
    onNavigateToAddress: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val customer by AppRepository.currentCustomer

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ReyhoonLogo(size = 36.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("ریحون", fontWeight = FontWeight.Bold)
                    }
                },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "خروج")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = GreenMid,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        modifier = modifier
    ) { padding ->
        if (customer == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("لطفاً دوباره وارد شوید")
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(
                    Brush.verticalGradient(
                        listOf(GreenPale.copy(alpha = 0.5f), MaterialTheme.colorScheme.background)
                    )
                )
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = GreenMid.copy(alpha = 0.12f))
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ReyhoonLogo(size = 56.dp)
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "سلام ${customer!!.name} عزیز 👋",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "به آشپزخانه ریحون خوش آمدید",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            // Balance cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (customer!!.debt > 0)
                            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f)
                        else GreenPale
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("بدهی شما", style = MaterialTheme.typography.labelMedium)
                        Text(
                            if (customer!!.debt > 0)
                                "${AppRepository.formatPrice(customer!!.debt)} ت"
                            else "۰",
                            fontWeight = FontWeight.Bold,
                            color = if (customer!!.debt > 0) MaterialTheme.colorScheme.error else GreenMid
                        )
                    }
                }
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = GreenPale)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("اعتبار شما", style = MaterialTheme.typography.labelMedium)
                        Text(
                            if (customer!!.credit > 0)
                                "${AppRepository.formatPrice(customer!!.credit)} ت"
                            else "۰",
                            fontWeight = FontWeight.Bold,
                            color = GreenMid
                        )
                    }
                }
            }

            if (customer!!.credit > 0) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = GreenMid.copy(alpha = 0.1f))
                ) {
                    Text(
                        "در سفارش بعدی، مبلغ ${AppRepository.formatPrice(customer!!.credit)} تومان بابت اعتبار شما از قیمت غذا کسر خواهد شد.",
                        modifier = Modifier.padding(14.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = GreenMid
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                onClick = onNavigateToAddress
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.LocationOn, null, tint = OrangeSecondary, modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("آدرس تحویل", fontWeight = FontWeight.SemiBold)
                        Text(
                            customer!!.address.fullAddress(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                        )
                    }
                    Icon(Icons.Default.ChevronLeft, null)
                }
            }

            Button(
                onClick = onNavigateToMenu,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.RestaurantMenu, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("مشاهده منوی غذا", fontWeight = FontWeight.Medium)
            }

            OutlinedButton(
                onClick = onNavigateToAddress,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.EditLocation, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("ویرایش آدرس")
            }
        }
    }
}
