package com.rekluzlabs.temporatic.presentation.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rekluzlabs.temporatic.presentation.viewmodel.TimerViewModel

@Composable
fun HomeScreen(
    viewModel: TimerViewModel = viewModel()
) {
    val seconds by viewModel.timerSeconds.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Screenshot Timer: $seconds seconds",
            style = MaterialTheme.typography.headlineMedium
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Slider(
            value = seconds.toFloat(),
            onValueChange = { viewModel.setTimerSeconds(it.toInt()) },
            valueRange = 1f..30f,
            steps = 29
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(onClick = { /* TODO: Start Service */ }) {
            Text("Start Capture")
        }
    }
}
