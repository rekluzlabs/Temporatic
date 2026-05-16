package com.rekluzlabs.temporatic

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rekluzlabs.temporatic.presentation.ui.screen.CountdownScreen
import com.rekluzlabs.temporatic.presentation.ui.screen.HomeScreen
import com.rekluzlabs.temporatic.presentation.ui.screen.PreviewScreen
import com.rekluzlabs.temporatic.presentation.ui.theme.TemporaticTheme
import com.rekluzlabs.temporatic.presentation.viewmodel.TimerViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val screenshotLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val viewModel: TimerViewModel by viewModels()
            viewModel.setMediaProjectionIntent(result.data)
        }
    }

    private val folderPickerLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            val viewModel: TimerViewModel by viewModels()
            viewModel.setSelectedFolderUri(uri)
        }
    }

    fun requestFolderSelection() {
        folderPickerLauncher.launch(null)
    }

    fun requestMediaProjection() {
        val manager = getSystemService(MEDIA_PROJECTION_SERVICE) as android.media.projection.MediaProjectionManager
        val intent = manager.createScreenCaptureIntent()
        screenshotLauncher.launch(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: TimerViewModel = viewModel()
            val currentScreen by viewModel.currentScreen.collectAsState()

            TemporaticTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    androidx.compose.animation.AnimatedContent(
                        targetState = currentScreen,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(300)) togetherWith
                                    fadeOut(animationSpec = tween(300))
                        },
                        label = "ScreenTransition"
                    ) { screen ->
                        when (screen) {
                            "home" -> HomeScreen(
                                viewModel, 
                                onStartRequested = { requestMediaProjection() },
                                onSelectFolderRequested = { requestFolderSelection() }
                            )
                            "countdown" -> CountdownScreen(viewModel)
                            "preview" -> PreviewScreen(viewModel)
                        }
                    }
                }
            }
        }
    }
}
