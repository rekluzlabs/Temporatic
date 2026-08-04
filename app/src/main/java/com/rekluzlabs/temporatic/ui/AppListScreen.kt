package com.rekluzlabs.temporatic.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.Delete
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppListScreen(
    viewModel: ScreenshotViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onAllScreenshots: () -> Unit,
    onAppSelected: (String) -> Unit
) {
    val apps = viewModel.allApps.collectAsState(initial = emptyList<String>()).value
    val totalCount by viewModel.totalCount.collectAsState(initial = 0)
    val selectedAppLabels by viewModel.selectedAppLabels.collectAsState()
    val isAllSelected by viewModel.isAllSelected.collectAsState()
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    val hasSelection = isAllSelected || selectedAppLabels.isNotEmpty()

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Confirm Deletion") },
            text = { 
                Text(
                    if (isAllSelected) "This will delete ALL screenshots and their folders from your device. This action cannot be undone."
                    else "This will delete the selected app folders and all screenshots inside them. This action cannot be undone."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteSelectedApps(selectedAppLabels.toList(), isAllSelected)
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
                title = {
                    Text(
                        if (!hasSelection) "Screenshots"
                        else if (isAllSelected) "All Screenshots selected"
                        else "${selectedAppLabels.size} selected"
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (hasSelection) viewModel.clearAppSelection() else onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (hasSelection) {
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
                .padding(16.dp)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (hasSelection) {
                            viewModel.toggleAllSelection()
                        } else {
                            onAllScreenshots()
                        }
                    },
                colors = CardDefaults.cardColors(
                    containerColor = if (isAllSelected) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surface
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isAllSelected) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                        contentDescription = if (isAllSelected) "Selected" else "Not selected",
                        tint = if (isAllSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.size(12.dp))
                    Text(
                        "All Screenshots",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        "($totalCount)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (totalCount == 0) {
                Text(
                    "No screenshots yet.\nStart a Live Capture or take a system screenshot.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp)
                )
            } else if (apps.isEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Screenshots are saved without app grouping. Enable 'Organize by App' in Settings to group by app.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    "By App",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                LazyColumn {
                    items(apps) { appLabel ->
                        AppListItem(
                            appLabel = appLabel,
                            viewModel = viewModel,
                            isSelected = selectedAppLabels.contains(appLabel),
                            isSelectionMode = hasSelection,
                            onClick = {
                                if (hasSelection) {
                                    viewModel.toggleAppSelection(appLabel)
                                } else {
                                    onAppSelected(appLabel)
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
fun AppListItem(
    appLabel: String,
    viewModel: ScreenshotViewModel,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit
) {
    val count = viewModel.countByApp(appLabel)
        .collectAsState(initial = 0).value

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isSelected) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                contentDescription = if (isSelected) "Selected" else "Not selected",
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.size(12.dp))
            Text(
                appLabel,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            Text(
                "($count)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
