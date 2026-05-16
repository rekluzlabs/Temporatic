package com.rekluzlabs.temporatic.presentation.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rekluzlabs.temporatic.R
import com.rekluzlabs.temporatic.presentation.viewmodel.TimerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: TimerViewModel
) {
    val seconds by viewModel.timerSeconds.collectAsState()
    var customSeconds by remember { mutableStateOf("") }
    
    val durations = listOf(5, 10, 15, 20)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Spacer(modifier = Modifier.height(48.dp))
            
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.displayLarge.copy(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.tertiary
                        )
                    )
                ),
                textAlign = TextAlign.Center
            )
            
            Text(
                text = "Select capture delay",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Duration Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                durations.forEach { duration ->
                    DurationChip(
                        seconds = duration,
                        isSelected = seconds == duration,
                        onClick = { 
                            viewModel.setTimerSeconds(duration)
                            customSeconds = ""
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            
            // Custom Duration
            OutlinedTextField(
                value = customSeconds,
                onValueChange = { 
                    if (it.length <= 3) {
                        customSeconds = it
                        it.toIntOrNull()?.let { s -> viewModel.setTimerSeconds(s) }
                    }
                },
                label = { Text(stringResource(R.string.custom_duration)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                )
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Start Button
            Button(
                onClick = { viewModel.startCountdown(seconds) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = stringResource(R.string.start_timer),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun DurationChip(
    seconds: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
    val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    
    Box(
        modifier = modifier
            .height(60.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "${seconds}s",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            ),
            color = contentColor
        )
    }
}
