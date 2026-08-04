package com.rekluzlabs.temporatic.ui

import android.content.ClipData
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import coil.compose.AsyncImage
import com.rekluzlabs.temporatic.data.ScreenshotRepository
import com.rekluzlabs.temporatic.utils.ShareHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CapturePreviewManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: ScreenshotRepository
) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var composeView: ComposeView? = null
    private var overlayParams: WindowManager.LayoutParams? = null

    private val handler = Handler(Looper.getMainLooper())
    private val dismissRunnable = Runnable { hide() }
    private val tagScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    /**
     * Expand the overlay window to full-screen so the inline tag dialog can fill the display.
     * Also removes FLAG_NOT_FOCUSABLE so the keyboard can appear.
     */
    private fun expandToFullscreen() {
        val params = overlayParams ?: return
        val view = composeView ?: return
        params.height = WindowManager.LayoutParams.MATCH_PARENT
        params.flags = params.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()
        try { windowManager.updateViewLayout(view, params) } catch (_: Exception) {}
    }

    /**
     * Shrink the overlay back to the toolbar height and restore non-focusable.
     */
    private fun collapseToToolbar() {
        val params = overlayParams ?: return
        val view = composeView ?: return
        params.height = WindowManager.LayoutParams.WRAP_CONTENT
        params.flags = params.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        try { windowManager.updateViewLayout(view, params) } catch (_: Exception) {}
    }

    fun show(uri: Uri, filename: String, recordId: String) {
        Log.d("CapturePreviewManager", "show() called with uri=$uri")
        handler.removeCallbacks(dismissRunnable)

        if (!Settings.canDrawOverlays(context)) {
            Log.w("CapturePreviewManager", "Overlay permission not granted")
            return
        }

        if (composeView != null) {
            hide()
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM
            y = 32
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
        overlayParams = params

        val view = ComposeView(context).also {
            setupMockLifecycle(it)
            it.setContent {
                var showTagDialog by remember { mutableStateOf(false) }

                Box(
                    modifier = if (showTagDialog) Modifier.fillMaxSize() else Modifier.fillMaxWidth().wrapContentHeight(),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    // ── Toolbar (always visible) ──────────────────────────────
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        CapturePreviewContent(
                            uri = uri,
                            onShare = { share(uri) },
                            onCrop = { crop(uri) },
                            onTag = {
                                handler.removeCallbacks(dismissRunnable)
                                expandToFullscreen()
                                showTagDialog = true
                            },
                            onPreview = { preview(uri) },
                            onDelete = { delete(uri, recordId) },
                            onDismiss = { hide() }
                        )
                    }

                    // ── Inline tag dialog (no android.app.Dialog spawned) ─────
                    if (showTagDialog) {
                        InlineTagDialog(
                            onDismiss = {
                                showTagDialog = false
                                collapseToToolbar()
                                handler.postDelayed(dismissRunnable, 3000)
                            },
                            onConfirm = { tags ->
                                showTagDialog = false
                                collapseToToolbar()
                                tagScope.launch {
                                    try {
                                        repository.updateTags(recordId, tags)
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(context, "Tags updated", Toast.LENGTH_SHORT).show()
                                            hide()
                                        }
                                    } catch (e: Exception) {
                                        Log.e("CapturePreviewManager", "Tag update failed", e)
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(context, "Failed to update tags", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }

        try {
            windowManager.addView(view, params)
            composeView = view
            Log.d("CapturePreviewManager", "Overlay view added successfully")
        } catch (e: Exception) {
            Log.e("CapturePreviewManager", "Failed to add overlay view", e)
            return
        }

        handler.postDelayed(dismissRunnable, 5000)
    }

    fun hide() {
        handler.removeCallbacks(dismissRunnable)
        composeView?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {
                Log.w("CapturePreviewManager", "Error removing overlay view", e)
            }
            composeView = null
        }
        overlayParams = null
    }

    private fun share(uri: Uri) {
        handler.removeCallbacks(dismissRunnable)
        try {
            ShareHelper.shareUri(context, uri)
        } catch (e: Exception) {
            Log.e("CapturePreviewManager", "share failed", e)
            Toast.makeText(context, "Failed to share screenshot", Toast.LENGTH_SHORT).show()
        }
        handler.postDelayed({ hide() }, 500)
    }

    private fun crop(uri: Uri) {
        handler.removeCallbacks(dismissRunnable)
        try {
            // Samsung editors (and most Android photo editors) refuse to edit images via
            // FileProvider URIs from private internal storage — they return "editing not
            // supported for this image". Copying to MediaStore gives a proper
            // content://media/... URI that all editors can read and write freely.
            val editUri = copyToMediaStore(uri) ?: run {
                Toast.makeText(context, "Could not prepare image for editing", Toast.LENGTH_SHORT).show()
                hide()
                return
            }
            val editIntent = Intent(Intent.ACTION_EDIT).apply {
                setDataAndType(editUri, "image/*")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(editIntent, "Edit Screenshot").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            Log.e("CapturePreviewManager", "crop failed", e)
            Toast.makeText(context, "No image editor found", Toast.LENGTH_SHORT).show()
        }
        hide()
    }

    private fun preview(uri: Uri) {
        handler.removeCallbacks(dismissRunnable)
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "image/*")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                clipData = ClipData.newRawUri("", uri)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("CapturePreviewManager", "preview failed", e)
            Toast.makeText(context, "No image viewer found", Toast.LENGTH_SHORT).show()
        }
        hide()
    }

    private fun delete(uri: Uri, recordId: String) {
        handler.removeCallbacks(dismissRunnable)
        tagScope.launch {
            try {
                repository.deleteById(recordId)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Screenshot deleted", Toast.LENGTH_SHORT).show()
                    hide()
                }
            } catch (e: Exception) {
                Log.e("CapturePreviewManager", "delete failed", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Failed to delete screenshot", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /**
     * Copies the screenshot at [sourceUri] into MediaStore under Pictures/Temporatic.
     * Returns the new MediaStore URI, or null on failure.
     *
     * Why: FileProvider URIs pointing to private app storage (filesDir) cannot be
     * written to by external editors — they check the URI authority and reject it.
     * MediaStore URIs are always writable by any app that holds the URI grant.
     */
    private fun copyToMediaStore(sourceUri: Uri): Uri? {
        return try {
            val filename = "Temporatic_edit_${System.currentTimeMillis()}.png"
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(
                    MediaStore.Images.Media.RELATIVE_PATH,
                    Environment.DIRECTORY_PICTURES + "/Temporatic"
                )
            }
            val resolver = context.contentResolver
            val destUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            if (destUri != null) {
                resolver.openInputStream(sourceUri)?.use { input ->
                    resolver.openOutputStream(destUri)?.use { output ->
                        input.copyTo(output)
                    }
                }
            }
            destUri
        } catch (e: Exception) {
            Log.e("CapturePreviewManager", "copyToMediaStore failed", e)
            null
        }
    }

    private fun setupMockLifecycle(view: View) {
        val lifecycleOwner = object : LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {
            override val lifecycle: Lifecycle = LifecycleRegistry(this)
            override val viewModelStore: ViewModelStore = ViewModelStore()
            private val savedStateRegistryController = SavedStateRegistryController.create(this).apply {
                performRestore(null)
            }
            override val savedStateRegistry: SavedStateRegistry = savedStateRegistryController.savedStateRegistry
        }

        // Move lifecycle to RESUMED AFTER saved state registry initialization
        (lifecycleOwner.lifecycle as LifecycleRegistry).currentState = Lifecycle.State.RESUMED

        view.setViewTreeLifecycleOwner(lifecycleOwner)
        view.setViewTreeViewModelStoreOwner(lifecycleOwner)
        view.setViewTreeSavedStateRegistryOwner(lifecycleOwner)
    }
}

/**
 * A fully inline dialog rendered within the overlay ComposeView itself.
 *
 * This intentionally does NOT use Compose's AlertDialog because that composable
 * internally creates an android.app.Dialog window, which requires an Activity
 * window token. When called from an Application context overlay there is no
 * Activity token, causing a fatal BadTokenException.
 *
 * Instead we render a semi-transparent backdrop + a card entirely inside the
 * existing overlay window — no second window is ever created.
 */
@Composable
fun InlineTagDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            // Tap on the scrim to dismiss, but don't propagate to views beneath
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
                // Stop taps on the card from closing the dialog via the scrim
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { /* consume */ },
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF2A2A2E),
            tonalElevation = 8.dp
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    "Add Tags",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Tags (comma separated)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedLabelColor = Color(0xFF8AB4F8),
                        unfocusedLabelColor = Color.Gray,
                        focusedBorderColor = Color(0xFF8AB4F8),
                        unfocusedBorderColor = Color.Gray
                    )
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = Color(0xFF8AB4F8))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(onClick = { onConfirm(text) }) {
                        Text("Save", color = Color(0xFF8AB4F8))
                    }
                }
            }
        }
    }
}

@Composable
private fun CapturePreviewContent(
    uri: Uri,
    onShare: () -> Unit,
    onCrop: () -> Unit,
    onTag: () -> Unit,
    onPreview: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    Surface(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .height(64.dp)
            .widthIn(max = 400.dp),
        shape = CircleShape,
        color = Color(0xFF202124),
        tonalElevation = 8.dp,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 4.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Screenshot Thumbnail (Circular)
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .padding(4.dp)
                    .clip(CircleShape)
                    .background(Color.DarkGray)
                    .clickable { onPreview() }
            ) {
                AsyncImage(
                    model = uri,
                    contentDescription = "Preview",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Action Buttons
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onCrop) {
                    Icon(Icons.Default.Crop, contentDescription = "Edit", tint = Color.White)
                }
                IconButton(onClick = onTag) {
                    Icon(Icons.Default.Label, contentDescription = "Tag", tint = Color.White)
                }
                IconButton(onClick = onShare) {
                    Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.White)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.White)
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Close Button
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Dismiss",
                    tint = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
