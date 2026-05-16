package com.rekluzlabs.temporatic.presentation.ui.screen

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rekluzlabs.temporatic.R
import com.rekluzlabs.temporatic.presentation.viewmodel.TimerViewModel

@Composable
fun CountdownScreen(
    viewModel: TimerViewModel
) {
    val countdown by viewModel.countdownState.collectAsState()
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(R.string.timer_running),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            AnimatedContent(
                targetState = countdown,
                transitionSpec = {
                    fadeIn() + scaleIn() togetherWith fadeOut() + scaleOut()
                },
                label = "CountdownAnimation"
            ) { targetCount ->
                Text(
                    text = targetCount.toString(),
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = 140.sp,
                        fontWeight = FontWeight.ExtraBold
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            
            Spacer(modifier = Modifier.height(48.dp))
            
            Text(
                text = stringResource(R.string.minimize_instruction),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp),
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
        }
    }
}
