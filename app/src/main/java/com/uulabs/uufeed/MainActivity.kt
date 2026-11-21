package com.uulabs.uufeed

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.uulabs.uufeed.demo.installDebugCapture
import com.uulabs.uufeed.ui.FeedApp
import com.uulabs.uufeed.ui.FeedViewModel
import com.uulabs.uufeed.ui.theme.UuFeedTheme

class MainActivity : ComponentActivity() {
    private val viewModel: FeedViewModel by viewModels {
        (application as UuFeedApp).graph.viewModelFactory
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installDebugCapture(this)
        val bars = android.graphics.Color.parseColor("#100E16")
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(bars),
            navigationBarStyle = SystemBarStyle.dark(bars),
        )
        setContent {
            UuFeedTheme {
                FeedApp(viewModel)
            }
        }
    }
}
