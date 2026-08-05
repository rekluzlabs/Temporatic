package com.rekluzlabs.temporatic.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.rekluzlabs.temporatic.R
import com.rekluzlabs.temporatic.manager.StorageManager

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    onPickFolder: () -> Unit = {},
    storageManager: StorageManager? = null
) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("temporatic", Context.MODE_PRIVATE)

    var captureSoundEnabled by remember {
        mutableStateOf(prefs.getBoolean("capture_sound_enabled", false))
    }

    var vibrateEnabled by remember {
        mutableStateOf(prefs.getBoolean("vibrate_enabled", true))
    }

    var vibrationIntensity by remember {
        mutableIntStateOf(prefs.getInt("vibration_intensity", 128))
    }

    var shareAfterCapture by remember {
        mutableStateOf(prefs.getBoolean("share_after_capture", true))
    }

    var watermarkEnabled by remember {
        mutableStateOf(prefs.getBoolean("watermark_enabled", true))
    }

    var floatingButtonEnabled by remember {
        mutableStateOf(prefs.getBoolean("floating_button_enabled", true))
    }

    var floatingButtonSize by remember {
        mutableIntStateOf(prefs.getInt("floating_button_size", 2))
    }

    var floatingButtonLocked by remember {
        mutableStateOf(prefs.getBoolean("floating_button_locked", false))
    }

    var floatingButtonColor by remember {
        mutableIntStateOf(prefs.getInt("floating_button_color", 0xFF6200EE.toInt()))
    }

    var floatingButtonTransparency by remember {
        mutableFloatStateOf(prefs.getFloat("floating_button_transparency", 1.0f))
    }

    var fileFormat by remember {
        mutableStateOf(prefs.getString("file_format", "PNG") ?: "PNG")
    }

    var imageQuality by remember {
        mutableIntStateOf(prefs.getInt("image_quality", 100))
    }

    var resizeScale by remember {
        mutableFloatStateOf(prefs.getFloat("resize_scale", 1.0f))
    }

    var darkMode by remember {
        mutableStateOf(prefs.getString("dark_mode", "system") ?: "system")
    }

    var canDrawOverlays by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    var folderUri by remember { mutableStateOf(storageManager?.getSavedFolderUri()) }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        while(true) {
            canDrawOverlays = Settings.canDrawOverlays(context)
            folderUri = storageManager?.getSavedFolderUri()
            kotlinx.coroutines.delay(1000)
        }
    }

    val versionName = try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName
    } catch (e: Exception) {
        "Unknown"
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            "Settings",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        // App Theme
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Theme Mode", modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                Row {
                    listOf("system", "light", "dark").forEach { mode ->
                        FilterChip(
                            selected = darkMode == mode,
                            onClick = {
                                darkMode = mode
                                prefs.edit().putString("dark_mode", mode).apply()
                            },
                            label = { Text(mode.replaceFirstChar { it.uppercase() }) },
                            modifier = Modifier.padding(horizontal = 2.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Floating Button Settings
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Floating Button", style = MaterialTheme.typography.titleMedium)
                        Text("Show capture button on screen", style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(
                        checked = floatingButtonEnabled,
                        onCheckedChange = {
                            floatingButtonEnabled = it
                            prefs.edit().putBoolean("floating_button_enabled", it).apply()
                        }
                    )
                }
                
                if (floatingButtonEnabled) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Size", modifier = Modifier.weight(1f))
                        Row {
                            listOf(1, 2, 3).forEach { size ->
                                FilterChip(
                                    selected = floatingButtonSize == size,
                                    onClick = {
                                        floatingButtonSize = size
                                        prefs.edit().putInt("floating_button_size", size).apply()
                                    },
                                    label = { Text(when(size) { 1 -> "S"; 2 -> "M"; else -> "L" }) },
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Opacity", modifier = Modifier.weight(1f))
                        Slider(
                            value = floatingButtonTransparency,
                            onValueChange = {
                                floatingButtonTransparency = it
                                prefs.edit().putFloat("floating_button_transparency", it).apply()
                            },
                            valueRange = 0.2f..1.0f,
                            modifier = Modifier.weight(2f)
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Color", modifier = Modifier.weight(1f))
                        Row {
                            val colors = listOf(0xFF6200EE, 0xFF03DAC5, 0xFFFF0266, 0xFF4CAF50, 0xFFFFEB3B)
                            colors.forEach { color ->
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .padding(4.dp)
                                        .background(Color(color), CircleShape)
                                        .clickable {
                                            floatingButtonColor = color.toInt()
                                            prefs.edit().putInt("floating_button_color", color.toInt()).apply()
                                        }
                                        .border(
                                            width = if (floatingButtonColor == color.toInt()) 2.dp else 0.dp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            shape = CircleShape
                                        )
                                )
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Lock Position", modifier = Modifier.weight(1f))
                        Switch(
                            checked = floatingButtonLocked,
                            onCheckedChange = {
                                floatingButtonLocked = it
                                prefs.edit().putBoolean("floating_button_locked", it).apply()
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Image Settings
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Image Settings", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Format")
                        Spacer(modifier = Modifier.height(4.dp))
                        Row {
                            listOf("PNG", "JPG").forEach { format ->
                                FilterChip(
                                    selected = fileFormat == format,
                                    onClick = {
                                        fileFormat = format
                                        prefs.edit().putString("file_format", format).apply()
                                    },
                                    label = { Text(format) },
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )
                            }
                        }
                    }
                }

                if (fileFormat == "JPG") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Quality ($imageQuality%)", modifier = Modifier.weight(1f))
                        Slider(
                            value = imageQuality.toFloat(),
                            onValueChange = {
                                imageQuality = it.toInt()
                                prefs.edit().putInt("image_quality", it.toInt()).apply()
                            },
                            valueRange = 10f..100f,
                            modifier = Modifier.weight(2f)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Resize")
                        Spacer(modifier = Modifier.height(4.dp))
                        Row {
                            listOf(1.0f, 0.75f, 0.5f, 0.25f).forEach { scale ->
                                FilterChip(
                                    selected = resizeScale == scale,
                                    onClick = {
                                        resizeScale = scale
                                        prefs.edit().putFloat("resize_scale", scale).apply()
                                    },
                                    label = { Text(if(scale == 1.0f) "None" else "${(scale*100).toInt()}%") },
                                    modifier = Modifier.padding(horizontal = 2.dp)
                                )
                            }
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Timestamp watermark")
                        Text(
                            "(Date/Time/Device/Android Version)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = watermarkEnabled,
                        onCheckedChange = {
                            watermarkEnabled = it
                            prefs.edit().putBoolean("watermark_enabled", it).apply()
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Feedback Settings
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Capture Sound", modifier = Modifier.weight(1f))
                    Switch(
                        checked = captureSoundEnabled,
                        onCheckedChange = {
                            captureSoundEnabled = it
                            prefs.edit().putBoolean("capture_sound_enabled", it).apply()
                        }
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Vibrate", modifier = Modifier.weight(1f))
                    Switch(
                        checked = vibrateEnabled,
                        onCheckedChange = {
                            vibrateEnabled = it
                            prefs.edit().putBoolean("vibrate_enabled", it).apply()
                        }
                    )
                }
                if (vibrateEnabled) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Vibration Intensity", modifier = Modifier.weight(1f))
                        Text("${(vibrationIntensity * 100 / 255)}%", style = MaterialTheme.typography.bodyMedium)
                    }
                    Slider(
                        value = vibrationIntensity.toFloat(),
                        onValueChange = {
                            vibrationIntensity = it.toInt()
                            prefs.edit().putInt("vibration_intensity", it.toInt()).apply()
                        },
                        valueRange = 0f..255f,
                        steps = 50,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Share Settings
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Preview After Capture", style = MaterialTheme.typography.titleMedium)
                    Text("Show screenshot preview overlay with share option after each capture", style = MaterialTheme.typography.bodySmall)
                }
                Switch(
                    checked = shareAfterCapture,
                    onCheckedChange = {
                        shareAfterCapture = it
                        prefs.edit().putBoolean("share_after_capture", it).apply()
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // System Permissions
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (canDrawOverlays) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.tertiaryContainer
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "1. Display Over Apps",
                        style = MaterialTheme.typography.titleSmall,
                        color = if (canDrawOverlays) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.weight(1f)
                    )
                    if (canDrawOverlays) {
                        Text("✓", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
                    }
                }
                Text(
                    "Required for flash effect and floating controls.",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = if (canDrawOverlays) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                )
                if (!canDrawOverlays) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Button(
                        onClick = {
                            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
                            context.startActivity(intent)
                        },
                        modifier = Modifier.align(Alignment.End).height(32.dp)
                    ) {
                        Text("Grant Permission", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (folderUri != null) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.secondaryContainer
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "2. Save Location",
                        style = MaterialTheme.typography.titleSmall,
                        color = if (folderUri != null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.weight(1f)
                    )
                    if (folderUri != null) {
                        Text("✓", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
                    }
                }
                Text(
                    folderUri?.path ?: "Using Default (DCIM/Temporatic)",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Button(
                    onClick = onPickFolder,
                    modifier = Modifier.align(Alignment.End).height(32.dp),
                    colors = if (folderUri != null) ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.outline) else ButtonDefaults.buttonColors()
                ) {
                    Text(if (folderUri == null) "Pick Custom Folder" else "Change Folder", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        val annotatedBatteryTip = buildAnnotatedString {
            append("For best results ensure that you ")
            val link = LinkAnnotation.Clickable(
                tag = "open_settings",
                styles = TextLinkStyles(
                    style = SpanStyle(
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        textDecoration = TextDecoration.Underline
                    )
                ),
                linkInteractionListener = {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                }
            )
            withLink(link) {
                append("disable battery optimization")
            }
            append(" in your device settings")
        }

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = annotatedBatteryTip,
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        HorizontalDivider()

        Spacer(modifier = Modifier.height(24.dp))

        AsyncImage(
            model = R.drawable.rl_goldlogo_t,
            contentDescription = "Rekuz Labs logo",
            modifier = Modifier
                .size(80.dp)
                .align(Alignment.CenterHorizontally)
                .clickable {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://rekluzlabs.github.io/"))
                    context.startActivity(intent)
                }
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Created by Rekluz Labs",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "rekluzlabs@gmail.com",
            style = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.primary,
                textDecoration = TextDecoration.Underline
            ),
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:rekluzlabs@gmail.com"))
                    context.startActivity(intent)
                },
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Copyright 2026",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Version $versionName",
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
    }
}
