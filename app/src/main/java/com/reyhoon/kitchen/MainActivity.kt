package com.reyhoon.kitchen

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.reyhoon.kitchen.navigation.ReyhoonNavGraph
import com.reyhoon.kitchen.ui.theme.ReyhoonKitchenTheme
import com.reyhoon.kitchen.util.NotificationHelper

class MainActivity : ComponentActivity() {

    private val notifPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* granted or not — polling still works in-app */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NotificationHelper.ensureChannels(this)
        requestNotifPermission()

        val openOrders = intent?.getBooleanExtra("open_orders", false) == true

        enableEdgeToEdge()
        setContent {
            ReyhoonKitchenTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ReyhoonNavGraph(openOrders = openOrders)
                }
            }
        }
    }

    private fun requestNotifPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
