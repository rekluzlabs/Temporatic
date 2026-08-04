package com.rekluzlabs.temporatic

import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.documentfile.provider.DocumentFile
import coil.compose.AsyncImage
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.rekluzlabs.temporatic.manager.StorageManager
import com.rekluzlabs.temporatic.ui.theme.TemporaticTheme
import com.rekluzlabs.temporatic.service.TemporaticService
import com.rekluzlabs.temporatic.service.ScreenshotObserverService
import com.rekluzlabs.temporatic.service.ScreenshotProcessingOrchestrator
import com.rekluzlabs.temporatic.ui.AppDetailScreen
import com.rekluzlabs.temporatic.ui.AppListScreen
import com.rekluzlabs.temporatic.ui.ScreenshotGalleryScreen
import com.rekluzlabs.temporatic.ui.SettingsScreen
import com.rekluzlabs.temporatic.ui.SetupWizardScreen
import com.rekluzlabs.temporatic.utils.FirstRunManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var orchestrator: ScreenshotProcessingOrchestrator
    @Inject lateinit var storageManager: StorageManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Start screenshot listener orchestrator
        orchestrator.startListening()

        // Start screenshot content observer service
        val observerServiceIntent = Intent(this, ScreenshotObserverService::class.java)
        startForegroundService(observerServiceIntent)

        // Initialize default storage directory
        storageManager.initializeDefaultDirectory()

        enableEdgeToEdge()

        setContent {
            val context = LocalContext.current
            var showWizard by remember { mutableStateOf(!FirstRunManager.isSetupCompleted(context)) }

            if (showWizard) {
                TemporaticTheme(darkTheme = true) {
                    SetupWizardScreen(
                        storageManager = storageManager,
                        onComplete = { showWizard = false }
                    )
                }
            } else {
                val prefs = remember { context.getSharedPreferences("temporatic", android.content.Context.MODE_PRIVATE) }
                var darkModeSetting by remember { mutableStateOf(prefs.getString("dark_mode", "system") ?: "system") }

                DisposableEffect(prefs) {
                    val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { p, key ->
                        if (key == "dark_mode") {
                            darkModeSetting = p?.getString("dark_mode", "system") ?: "system"
                        }
                    }
                    prefs.registerOnSharedPreferenceChangeListener(listener)
                    onDispose {
                        prefs.unregisterOnSharedPreferenceChangeListener(listener)
                    }
                }

                val darkTheme = when (darkModeSetting) {
                    "dark" -> true
                    "light" -> false
                    else -> androidx.compose.foundation.isSystemInDarkTheme()
                }

                TemporaticTheme(darkTheme = darkTheme) {
                    val navController = rememberNavController()
                    var selectedTab by remember { mutableIntStateOf(0) }

                    Surface(modifier = Modifier.fillMaxSize()) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                when (selectedTab) {
                                    0 -> {
                                        NavHost(
                                            navController = navController,
                                            startDestination = "main_ui",
                                            modifier = Modifier.fillMaxSize()
                                        ) {
                                            composable("main_ui") {
                                                TemporaticUI(
                                                    onPickFolder = { launchFolderPicker() },
                                                    onArmCapture = { requestMediaProjection() },
                                                    storageManager = storageManager,
                                                    onViewScreenshots = { navController.navigate("gallery") }
                                                )
                                            }
                                            composable("app_list") {
                                                AppListScreen(
                                                    onBack = { navController.popBackStack() },
                                                    onAllScreenshots = { navController.navigate("gallery") },
                                                    onAppSelected = { appLabel ->
                                                        navController.navigate("app_detail/$appLabel")
                                                    }
                                                )
                                            }
                                            composable("gallery") {
                                                ScreenshotGalleryScreen(
                                                    onBack = { navController.popBackStack() },
                                                    onScreenshotSelected = { _ ->
                                                        // TODO: Navigate to viewer screen
                                                    }
                                                )
                                            }
                                            composable(
                                                route = "app_detail/{appLabel}",
                                                arguments = listOf(navArgument("appLabel") { type = NavType.StringType })
                                            ) { backStackEntry ->
                                                val appLabel = backStackEntry.arguments?.getString("appLabel") ?: return@composable
                                                AppDetailScreen(
                                                    appLabel = appLabel,
                                                    onBack = { navController.popBackStack() },
                                                    onScreenshotSelected = { _ ->
                                                        // TODO: Navigate to viewer screen
                                                    }
                                                )
                                            }
                                        }
                                    }
                                    1 -> {
                                        SettingsScreen(
                                            modifier = Modifier.fillMaxSize(),
                                            onPickFolder = { launchFolderPicker() },
                                            storageManager = storageManager
                                        )
                                    }
                                }
                            }

                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                tonalElevation = 3.dp
                            ) {
                                NavigationBarItem(
                                    icon = { Icon(Icons.Default.Home, contentDescription = "Home", modifier = Modifier.size(22.dp)) },
                                    label = { Text("Home", fontSize = 11.sp) },
                                    selected = selectedTab == 0,
                                    onClick = { selectedTab = 0 }
                                )
                                NavigationBarItem(
                                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings", modifier = Modifier.size(22.dp)) },
                                    label = { Text("Settings", fontSize = 11.sp) },
                                    selected = selectedTab == 1,
                                    onClick = { selectedTab = 1 }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun launchFolderPicker() {
        folderPickerLauncher.launch(null)
    }

    private val folderPickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { uri ->
        if (uri != null) {
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            storageManager.saveFolderUri(uri)
        }
    }

    private fun requestMediaProjection() {
        val manager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        projectionLauncher.launch(manager.createScreenCaptureIntent())
    }

    private val projectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            val intent = Intent(this, TemporaticService::class.java).apply {
                putExtra("resultCode", result.resultCode)
                putExtra("resultData", result.data)
            }
            startForegroundService(intent)
        } else {
            Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
        }
    }

}

@Composable
fun TemporaticUI(
    onPickFolder: () -> Unit,
    onArmCapture: () -> Unit,
    storageManager: StorageManager,
    onViewScreenshots: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var isCaptureArmed by remember { mutableStateOf(TemporaticService.hasActiveProjection()) }

    // Refresh status periodically
    androidx.compose.runtime.LaunchedEffect(Unit) {
        while(true) {
            isCaptureArmed = TemporaticService.hasActiveProjection()
            kotlinx.coroutines.delay(1000)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Temporatic",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Gameplay Screenshot Utility",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
            AsyncImage(
                model = R.mipmap.ic_launcher,
                contentDescription = "App icon",
                modifier = Modifier
                    .size(64.dp)
                    .align(Alignment.CenterEnd)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .verticalScroll(androidx.compose.foundation.rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = {
                    if (isCaptureArmed) {
                        context.stopService(Intent(context, TemporaticService::class.java))
                    } else {
                        onArmCapture()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = if (isCaptureArmed) ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                ) else ButtonDefaults.buttonColors(),
                shape = MaterialTheme.shapes.large
            ) {
                Text(
                    if (isCaptureArmed) "Stop Temporatic Screenshot" 
                    else "Start Temporatic Screenshot", 
                    style = MaterialTheme.typography.titleMedium
                )
            }

            if (isCaptureArmed) {
                Spacer(modifier = Modifier.height(12.dp))
                
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Temporatic Live Capture Active", style = MaterialTheme.typography.titleSmall)
                            Text("Tap the camera button", style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp))
                        }
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            var organizeByApp by remember { mutableStateOf(
                context.getSharedPreferences("temporatic", Context.MODE_PRIVATE)
                    .getBoolean("organize_by_app", false)
            ) }

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Organize by App",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            if (organizeByApp) "Live captures will be saved in app-named folders"
                            else "All captures go to the same folder",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = organizeByApp,
                        onCheckedChange = { checked ->
                            organizeByApp = checked
                            context.getSharedPreferences("temporatic", Context.MODE_PRIVATE)
                                .edit()
                                .putBoolean("organize_by_app", checked)
                                .apply()
                        }
                    )
                }
            }

            if (organizeByApp) {
                Spacer(modifier = Modifier.height(4.dp))
                val usageAccessLink = buildAnnotatedString {
                    append("Requires Usage Access permission. ")
                    val link = LinkAnnotation.Clickable(
                        tag = "open_usage_access",
                        styles = TextLinkStyles(
                            style = SpanStyle(
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                textDecoration = TextDecoration.Underline
                            )
                        ),
                        linkInteractionListener = {
                            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                            context.startActivity(intent)
                        }
                    )
                    withLink(link) {
                        append("Open Settings → App Access")
                    }
                }
                Text(
                    text = usageAccessLink,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onViewScreenshots,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary
                ),
                shape = MaterialTheme.shapes.large
            ) {
                Text("View Captured Screenshots", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

