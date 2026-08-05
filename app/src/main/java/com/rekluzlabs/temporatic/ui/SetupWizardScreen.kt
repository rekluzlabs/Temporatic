package com.rekluzlabs.temporatic.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.rekluzlabs.temporatic.R
import com.rekluzlabs.temporatic.manager.StorageManager
import com.rekluzlabs.temporatic.utils.PermissionHelper

private data class SetupStep(
    val id: Int,
    val title: String,
    val description: String
)

private val steps = listOf(
    SetupStep(0, "Welcome", "Let's get Temporatic ready"),
    SetupStep(1, "Notifications", "Required for background service"),
    SetupStep(2, "Storage Access", "Save screenshots to your device"),
    SetupStep(3, "Display Over Apps", "Floating capture button"),
    SetupStep(4, "Usage Access", "Identify foreground app"),
    SetupStep(5, "Save Location", "Choose where to save"),
    SetupStep(6, "All Set!", "You're ready to capture")
)

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun SetupWizardScreen(
    storageManager: StorageManager,
    onComplete: () -> Unit
) {
    val context = LocalContext.current
    var currentStep by remember { mutableIntStateOf(0) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text("Setup Temporatic", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )

            StepIndicator(currentStep = currentStep, totalSteps = steps.size - 1)

            Spacer(modifier = Modifier.height(8.dp))
            AsyncImage(
                model = R.mipmap.ic_launcher,
                contentDescription = "Temporatic logo",
                modifier = Modifier
                    .size(150.dp)
                    .align(Alignment.CenterHorizontally)
            )
            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .verticalScroll(rememberScrollState()),
                contentAlignment = Alignment.Center
            ) {
                when (currentStep) {
                    0 -> WelcomeStep(onNext = { currentStep = 1 })
                    1 -> NotificationStep(
                        onGranted = { currentStep = 2 },
                        onSkip = { currentStep = 2 }
                    )
                    2 -> StorageStep(
                        onGranted = { currentStep = 3 },
                        onSkip = { currentStep = 3 }
                    )
                    3 -> OverlayStep(
                        onGranted = { currentStep = 4 }
                    )
                    4 -> UsageAccessStep(
                        onGranted = { currentStep = 5 },
                        onSkip = { currentStep = 5 }
                    )
                    5 -> SaveLocationStep(
                        storageManager = storageManager,
                        onDone = { currentStep = 6 }
                    )
                    6 -> DoneStep(
                        onFinish = {
                            com.rekluzlabs.temporatic.utils.FirstRunManager.markSetupCompleted(context)
                            onComplete()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun StepIndicator(currentStep: Int, totalSteps: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until totalSteps) {
            val isCompleted = i < currentStep
            val isCurrent = i == currentStep
            val color = when {
                isCompleted -> MaterialTheme.colorScheme.primary
                isCurrent -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(4.dp)
                    .padding(end = if (i < totalSteps - 1) 4.dp else 0.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(color)
            )
        }
    }
}

@Composable
private fun StepContainer(
    title: String,
    description: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(32.dp))
        content()
    }
}

@Composable
private fun WelcomeStep(onNext: () -> Unit) {
    StepContainer(
        title = "Welcome to Temporatic",
        description = "Let's set up everything you need to start capturing screenshots from any app. This will only take a minute."
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "What you'll get:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                listOf(
                    "Floating capture button over any app",
                    "One-tap screenshots without leaving your app",
                    "Auto-organized folders by app name",
                    "Quick share to social media or email"
                ).forEach { feature ->
                    Row(
                        verticalAlignment = Alignment.Top,
                        modifier = Modifier.padding(vertical = 2.dp)
                    ) {
                        Text("• ", color = MaterialTheme.colorScheme.primary)
                        Text(feature, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Get Started", fontSize = 16.sp)
        }
    }
}

@Composable
private fun NotificationStep(
    onGranted: () -> Unit,
    onSkip: () -> Unit
) {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(PermissionHelper.hasNotificationPermission(context))
    }

    val permLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        if (granted) onGranted()
    }

    LaunchedEffect(Unit) {
        while (true) {
            hasPermission = PermissionHelper.hasNotificationPermission(context)
            if (hasPermission) { onGranted(); return@LaunchedEffect }
            kotlinx.coroutines.delay(1000)
        }
    }

    val isRequired = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    StepContainer(
        title = "Notification Permission",
        description = if (isRequired) {
            "Temporatic needs to show a persistent notification to keep its background capture service running, even when you're using other apps."
        } else {
            "Your device automatically allows notifications. You're all set!"
        }
    ) {
        StatusCard(
            label = "Notifications",
            isGranted = hasPermission || !isRequired
        )
        Spacer(modifier = Modifier.height(24.dp))
        if (isRequired && !hasPermission) {
            Button(
                onClick = { permLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Grant Permission", fontSize = 16.sp)
            }
        } else {
            Button(
                onClick = onGranted,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Continue", fontSize = 16.sp)
            }
        }
        if (isRequired && !hasPermission) {
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = onSkip) {
                Text("Skip (not recommended)", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun StorageStep(
    onGranted: () -> Unit,
    onSkip: () -> Unit
) {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(PermissionHelper.hasStoragePermission(context))
    }

    val permLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        if (granted) onGranted()
    }

    LaunchedEffect(Unit) {
        while (true) {
            hasPermission = PermissionHelper.hasStoragePermission(context)
            if (hasPermission) { onGranted(); return@LaunchedEffect }
            kotlinx.coroutines.delay(1000)
        }
    }

    StepContainer(
        title = "Storage Access",
        description = "Temporatic needs access to your photos and media to save the screenshots you capture."
    ) {
        StatusCard(
            label = "Read Media / Storage",
            isGranted = hasPermission
        )
        Spacer(modifier = Modifier.height(24.dp))
        if (!hasPermission) {
            val perm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                Manifest.permission.READ_MEDIA_IMAGES
            else
                Manifest.permission.READ_EXTERNAL_STORAGE
            Button(
                onClick = { permLauncher.launch(perm) },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Grant Permission", fontSize = 16.sp)
            }
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = onSkip) {
                Text("Skip (not recommended)", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            Button(
                onClick = onGranted,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Continue", fontSize = 16.sp)
            }
        }
    }
}

@Composable
private fun OverlayStep(onGranted: () -> Unit) {
    val context = LocalContext.current
    var canDrawOverlays by remember { mutableStateOf(Settings.canDrawOverlays(context)) }

    LaunchedEffect(Unit) {
        while (true) {
            canDrawOverlays = Settings.canDrawOverlays(context)
            if (canDrawOverlays) { onGranted(); return@LaunchedEffect }
            kotlinx.coroutines.delay(1000)
        }
    }

    StepContainer(
        title = "Display Over Other Apps",
        description = "This allows the floating camera button to appear on top of any app you're using, so you can capture screenshots instantly."
    ) {
        StatusCard(
            label = "Floating Button Overlay",
            isGranted = canDrawOverlays
        )
        Spacer(modifier = Modifier.height(24.dp))
        if (!canDrawOverlays) {
            Button(
                onClick = {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${context.packageName}")
                    )
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Open Settings to Enable", fontSize = 16.sp)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Toggle \"Allow display over other apps\" to on",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        } else {
            Button(
                onClick = onGranted,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Continue", fontSize = 16.sp)
            }
        }
    }
}

@Composable
private fun UsageAccessStep(
    onGranted: () -> Unit,
    onSkip: () -> Unit
) {
    val context = LocalContext.current
    var hasUsageAccess by remember { mutableStateOf(hasUsageAccessPermission(context)) }

    LaunchedEffect(Unit) {
        while (true) {
            hasUsageAccess = hasUsageAccessPermission(context)
            if (hasUsageAccess) { onGranted(); return@LaunchedEffect }
            kotlinx.coroutines.delay(1000)
        }
    }

    StepContainer(
        title = "Usage Access Permission",
        description = "Required to detect which app is in the foreground. This enables the \"Organize by App\" feature which sorts screenshots into app-named folders."
    ) {
        StatusCard(
            label = "Usage Access",
            isGranted = hasUsageAccess
        )
        Spacer(modifier = Modifier.height(16.dp))
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                "Used only to identify the current foreground app name. No other usage data is collected.",
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        if (!hasUsageAccess) {
            Button(
                onClick = {
                    context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS, Uri.parse("package:${context.packageName}")))
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Open Usage Access Settings", fontSize = 16.sp)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Find \"Temporatic\" in the list and enable it",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))
            TextButton(onClick = onSkip) {
                Text(
                    "Skip (organized folders won't work)",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Button(
                onClick = onGranted,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Continue", fontSize = 16.sp)
            }
        }
    }
}

@Composable
private fun SaveLocationStep(
    storageManager: StorageManager,
    onDone: () -> Unit
) {
    val context = LocalContext.current
    var folderUri by remember { mutableStateOf(storageManager.getSavedFolderUri()) }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            storageManager.saveFolderUri(uri)
            folderUri = uri
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            folderUri = storageManager.getSavedFolderUri()
            kotlinx.coroutines.delay(1000)
        }
    }

    StepContainer(
        title = "Save Location",
        description = "Choose where your screenshots will be saved. You can pick any folder, or use the default DCIM/Temporatic location."
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (folderUri != null)
                    MaterialTheme.colorScheme.surfaceVariant
                else
                    MaterialTheme.colorScheme.secondaryContainer
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Current location:",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.weight(1f)
                    )
                    if (folderUri != null) {
                        Text("✓", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    folderUri?.path ?: "Default: DCIM/Temporatic",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = { folderPickerLauncher.launch(null) },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = if (folderUri != null)
                ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.outline)
            else
                ButtonDefaults.buttonColors()
        ) {
            Text(
                if (folderUri == null) "Pick a Folder" else "Change Folder",
                fontSize = 16.sp
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onDone,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Continue", fontSize = 16.sp)
        }
    }
}

@Composable
private fun DoneStep(onFinish: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "You're All Set!",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Temporatic is ready to go. Here's a quick recap:",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(32.dp))

        val context = LocalContext.current
        val checks = listOf(
            "Notifications" to (PermissionHelper.hasNotificationPermission(context) || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU),
            "Storage Access" to PermissionHelper.hasStoragePermission(context),
            "Display Over Apps" to Settings.canDrawOverlays(context),
            "Usage Access" to hasUsageAccessPermission(context),
            "Save Location" to (com.rekluzlabs.temporatic.utils.FirstRunManager.isSetupCompleted(context) || true)
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                checks.forEach { (label, ok) ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            if (ok) "✓" else "○",
                            color = if (ok) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(24.dp)
                        )
                        Text(
                            label,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (ok) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onFinish,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text("Start Using Temporatic", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun StatusCard(label: String, isGranted: Boolean) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isGranted)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            if (isGranted) {
                Text("✓ Granted", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
            } else {
                Text("Not yet set", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private fun hasUsageAccessPermission(context: Context): Boolean {
    try {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as android.app.AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                context.packageName
            )
        }
        return mode == android.app.AppOpsManager.MODE_ALLOWED
    } catch (e: Exception) {
        return false
    }
}
