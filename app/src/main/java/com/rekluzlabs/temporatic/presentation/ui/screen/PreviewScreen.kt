package com.rekluzlabs.temporatic.presentation.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rekluzlabs.temporatic.R
import com.rekluzlabs.temporatic.presentation.viewmodel.TimerViewModel

@Composable
fun PreviewScreen(
    viewModel: TimerViewModel
) {
    val bitmap by viewModel.capturedBitmap.collectAsState()
    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val context = androidx.compose.ui.platform.LocalContext.current
            val screenshotFile by viewModel.screenshotFile.collectAsState()

            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Capture Preview",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            // Screenshot Preview Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(9f/16f) // Standard phone aspect ratio
                    .clip(RoundedCornerShape(24.dp)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap!!.asImageBitmap(),
                        contentDescription = "Captured Screenshot",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Screenshot will appear here",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Actions
            Button(
                onClick = { 
                    screenshotFile?.let { 
                        com.rekluzlabs.temporatic.utils.ClipboardHelper.copyBitmapToClipboard(context, it) 
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = screenshotFile != null
            ) {
                Text(stringResource(R.string.copy_to_clipboard))
            }
            
            OutlinedButton(
                onClick = { 
                    screenshotFile?.let { 
                        com.rekluzlabs.temporatic.utils.ShareHelper.shareScreenshot(context, it) 
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = screenshotFile != null
            ) {
                Text(stringResource(R.string.share))
            }

            val selectedFolderUri by viewModel.selectedFolderUri.collectAsState()
            
            TextButton(
                onClick = { 
                    if (selectedFolderUri != null) {
                        viewModel.saveToSelectedFolder()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = bitmap != null
            ) {
                Text(if (selectedFolderUri != null) stringResource(R.string.save_to_folder) else "No folder selected")
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = { viewModel.reset() },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            ) {
                Text(stringResource(R.string.close))
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
