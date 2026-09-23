package com.reyhoon.kitchen.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.reyhoon.kitchen.data.ApiClient
import com.reyhoon.kitchen.data.ApiConfig
import com.reyhoon.kitchen.ui.theme.GreenPrimary
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RatingsScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    var result by remember { mutableStateOf(ApiClient.RatingsResult(emptyList(), 0.0, 0)) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun load() {
        scope.launch {
            loading = true
            result = if (ApiConfig.isConfigured) ApiClient.fetchRatings()
            else ApiClient.RatingsResult(emptyList(), 0.0, 0)
            loading = false
        }
    }

    LaunchedEffect(Unit) { load() }

    fun ts(t: Long) =
        if (t <= 0) "—" else SimpleDateFormat("yyyy/MM/dd HH:mm", Locale("fa")).format(Date(t))

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("امتیازات پیک", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "بازگشت")
                    }
                },
                actions = {
                    IconButton(onClick = { load() }) {
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
        modifier = modifier
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = GreenPrimary.copy(alpha = 0.12f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("میانگین امتیاز پیک", fontWeight = FontWeight.SemiBold)
                    Text(
                        if (result.count == 0) "هنوز امتیازی ثبت نشده"
                        else "${result.average} از ۵  (${result.count} نظر)",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            if (loading && result.ratings.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (result.ratings.isEmpty()) {
                Text("مشتری هنوز به پیک امتیاز نداده است.")
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(result.ratings, key = { it.id }) { r ->
                        Card(shape = RoundedCornerShape(12.dp)) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(r.customerName, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                    Row {
                                        repeat(5) { i ->
                                            Icon(
                                                Icons.Default.Star,
                                                contentDescription = null,
                                                tint = if (i < r.rating) Color(0xFFFFC107) else Color(0xFFBDBDBD),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                                Text(ts(r.createdAt), style = MaterialTheme.typography.bodySmall)
                                if (r.comment.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(r.comment)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
