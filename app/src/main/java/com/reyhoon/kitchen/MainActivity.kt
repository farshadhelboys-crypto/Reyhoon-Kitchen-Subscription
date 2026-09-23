package com.reyhoon.kitchen

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.reyhoon.kitchen.navigation.ReyhoonNavGraph
import com.reyhoon.kitchen.ui.theme.ReyhoonKitchenTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ReyhoonKitchenTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ReyhoonNavGraph()
                }
            }
        }
    }
}
