package com.rekluzlabs.temporatic.ui

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Share
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.rekluzlabs.temporatic.data.ScreenshotRecord

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDetailScreen(
    appLabel: String,
    viewModel: ScreenshotViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onScreenshotSelected: (ScreenshotRecord) -> Unit
) {
    val screenshots = viewModel.getScreenshotsForApp(appLabel)
        .collectAsState(initial = emptyList<ScreenshotRecord>()).value
    val selectedIds by viewModel.selectedIds.collectAsState()
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Delete Screenshots") },
            text = { Text("Are you sure you want to delete ${selectedIds.size} selected screenshots from $appLabel? This will permanently remove them from your device.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteSelected()
                        showDeleteConfirmation = false
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (selectedIds.isEmpty()) appLabel else "${selectedIds.size} selected") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (selectedIds.isNotEmpty()) viewModel.clearSelection() else onBack()
                    }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    if (selectedIds.isNotEmpty()) {
                        IconButton(onClick = {
                            if (selectedIds.size == screenshots.size) {
                                viewModel.selectNoneScreenshots()
                            } else {
                                viewModel.selectAllScreenshots(screenshots.map { it.id })
                            }
                        }) {
                            Icon(
                                Icons.Default.DoneAll,
                                contentDescription = if (selectedIds.size == screenshots.size) "Deselect all" else "Select all"
                            )
                        }
                        IconButton(onClick = { showDeleteConfirmation = true }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete selected",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            if (screenshots.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("No screenshots for this app yet")
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (selectedIds.size == screenshots.size) {
                                viewModel.selectNoneScreenshots()
                            } else {
                                viewModel.selectAllScreenshots(screenshots.map { it.id })
                            }
                        }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (selectedIds.size == screenshots.size && screenshots.isNotEmpty()) 
                            Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                        contentDescription = "Select All",
                        tint = if (selectedIds.size == screenshots.size && screenshots.isNotEmpty()) 
                            MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = if (selectedIds.size == screenshots.size && screenshots.isNotEmpty()) "Deselect All" else "Select All",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f)
                    )
                    
                    if (selectedIds.isNotEmpty()) {
                        IconButton(onClick = { showDeleteConfirmation = true }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Selected",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(screenshots) { screenshot ->
                        ScreenshotThumbnail(
                            screenshot = screenshot,
                            isSelected = selectedIds.contains(screenshot.id),
                            onToggleSelection = { viewModel.toggleSelection(screenshot.id) },
                            onClick = {
                                if (selectedIds.isNotEmpty()) {
                                    viewModel.toggleSelection(screenshot.id)
                                } else {
                                    onScreenshotSelected(screenshot)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ScreenshotThumbnail(
    screenshot: ScreenshotRecord,
    isSelected: Boolean = false,
    onToggleSelection: () -> Unit = {},
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val uri = try {
        Uri.parse(screenshot.filePath)
    } catch (e: Exception) {
        null
    }

    Box(
        modifier = Modifier
            .aspectRatio(0.5625f)
            .then(
                if (isSelected) Modifier.background(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                    MaterialTheme.shapes.small
                ) else Modifier
            )
            .clip(MaterialTheme.shapes.small)
            .clickable { onClick() }
    ) {
        AsyncImage(
            model = uri,
            contentDescription = screenshot.filename,
            modifier = Modifier.matchParentSize(),
            contentScale = ContentScale.Crop
        )
        if (isSelected) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
            )
        }
        IconButton(
            onClick = onToggleSelection,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(4.dp)
                .size(28.dp)
                .background(
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                    MaterialTheme.shapes.small
                )
        ) {
            Icon(
                imageVector = if (isSelected) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                contentDescription = if (isSelected) "Deselect" else "Select",
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
        if (uri != null) {
            IconButton(
                onClick = {
                    com.rekluzlabs.temporatic.utils.ShareHelper.shareUri(context, uri)
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp)
                    .size(28.dp)
                    .background(
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                        MaterialTheme.shapes.small
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Share",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
