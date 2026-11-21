package com.lunchy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.lunchy.ui.LunchApp
import com.lunchy.ui.LunchViewModel
import com.lunchy.ui.theme.LunchyTheme

class MainActivity : ComponentActivity() {
    private val viewModel: LunchViewModel by viewModels {
        (application as LunchyApp).graph.viewModelFactory
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LunchyTheme {
                LunchApp(viewModel)
            }
        }
    }
}
