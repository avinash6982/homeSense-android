package com.avinash.homesense

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.avinash.homesense.ui.climate.ClimateScreen
import com.avinash.homesense.ui.theme.HomeSenseTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HomeSenseApp()
        }
    }
}

@Composable
private fun HomeSenseApp() {
    HomeSenseTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            ClimateScreen()
        }
    }
}
