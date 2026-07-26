package com.example.service

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.animation.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.compose.material.icons.filled.CloseFullscreen
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.Alignment
import androidx.compose.animation.core.*
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.*
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.DictionaryUiState
import com.example.DictionaryViewModel
import com.example.DictionaryResultContent
import com.example.ui.theme.MyApplicationTheme
import java.util.Locale

@Composable
fun ProcessingIndicator() {
    var isAnimating by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        while (true) {
            isAnimating = !isAnimating
            kotlinx.coroutines.delay(800)
        }
    }
    
    val animatedScale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isAnimating) 1.2f else 0.8f,
        animationSpec = androidx.compose.animation.core.tween(800, easing = androidx.compose.animation.core.FastOutSlowInEasing)
    )
    
    val animatedAlpha by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isAnimating) 1f else 0.3f,
        animationSpec = androidx.compose.animation.core.tween(800, easing = androidx.compose.animation.core.FastOutSlowInEasing)
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .scale(animatedScale).alpha(animatedAlpha),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Processando...",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Processando tradução...",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = animatedAlpha),
            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
        )
    }
}

class FloatingDictionaryService : Service(), LifecycleOwner, SavedStateRegistryOwner, ViewModelStoreOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private val store = ViewModelStore()

    private var windowManager: WindowManager? = null
    private var composeView: View? = null
    private lateinit var viewModel: DictionaryViewModel
    private var tts: TextToSpeech? = null

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry
    override val viewModelStore: ViewModelStore get() = store

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        
        viewModel = ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory.getInstance(application))[DictionaryViewModel::class.java]

        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale("pt", "BR")
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (lifecycleRegistry.currentState == Lifecycle.State.CREATED) {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        }
        
        val word = intent?.getStringExtra("EXTRA_WORD")
        showFloatingWindow(word)
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun showFloatingWindow(word: String?) {
        if (composeView != null) {
            // Already showing, just update word if provided
            if (word != null) {
                viewModel.showBottomSheet(word)
            }
            return
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_DIM_BEHIND or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            dimAmount = 0.5f
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        }

        val frameLayout = FrameLayout(this)

        
        frameLayout.setViewTreeLifecycleOwner(this)
        frameLayout.setViewTreeSavedStateRegistryOwner(this)
        frameLayout.setViewTreeViewModelStoreOwner(this)
        
        if (word != null) {
            viewModel.showBottomSheet(word)
        }

        val view = ComposeView(this).apply {
            setContent {
                
                val themeMode by viewModel.themeMode.collectAsState()
                val darkTheme = when (themeMode) {
                    com.example.ThemeMode.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
                    com.example.ThemeMode.LIGHT -> false
                    com.example.ThemeMode.DARK -> true
                }
                MyApplicationTheme(darkTheme = darkTheme) {

                    val uiState by viewModel.uiState.collectAsState()
                    val isCurrentWordFavorite by viewModel.isCurrentWordFavorite.collectAsState()
                    var isVisible by remember { mutableStateOf(false) }
                    var isMinimized by remember { mutableStateOf(word == null) }
                    val scope = rememberCoroutineScope()
                    
                    LaunchedEffect(Unit) {
                        isVisible = true
                    }

                    LaunchedEffect(isMinimized) {
                        if (isMinimized) {
                            params.flags = params.flags and WindowManager.LayoutParams.FLAG_DIM_BEHIND.inv()
                            params.flags = params.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                            params.width = WindowManager.LayoutParams.WRAP_CONTENT
                            params.height = WindowManager.LayoutParams.WRAP_CONTENT
                            windowManager?.updateViewLayout(frameLayout, params)
                        } else {
                            params.flags = params.flags or WindowManager.LayoutParams.FLAG_DIM_BEHIND
                            params.flags = params.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()
                            params.width = WindowManager.LayoutParams.MATCH_PARENT
                            params.height = WindowManager.LayoutParams.WRAP_CONTENT
                            windowManager?.updateViewLayout(frameLayout, params)
                        }
                    }
                    
                    if (isMinimized) {
                        FloatingActionButton(
                            onClick = { isMinimized = false },
                            modifier = Modifier
                                .padding(16.dp)
                                .pointerInput(Unit) {
                                    detectDragGestures { change, dragAmount ->
                                        change.consume()
                                        params.y = Math.max(0, params.y - dragAmount.y.toInt())
                                        params.x += dragAmount.x.toInt()
                                        windowManager?.updateViewLayout(frameLayout, params)
                                    }
                                }
                        ) {
                            Icon(imageVector = Icons.Default.Search, contentDescription = "Maximizar")
                        }
                    } else {
                        AnimatedVisibility(
                            visible = isVisible,
                            enter = androidx.compose.animation.slideInVertically(initialOffsetY = { it }) + androidx.compose.animation.fadeIn(),
                            exit = androidx.compose.animation.slideOutVertically(targetOffsetY = { it }) + androidx.compose.animation.fadeOut()
                        ) {
                            Surface(
                                modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp),
                                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                                color = MaterialTheme.colorScheme.surfaceContainer,
                                shadowElevation = 16.dp
                            ) {
                                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                                    val scale = (maxWidth / 400.dp).coerceIn(0.5f, 1f)
                                    val currentDensity = LocalDensity.current
                                    val newDensity = Density(currentDensity.density * scale, currentDensity.fontScale * scale)
                                    
                                    CompositionLocalProvider(LocalDensity provides newDensity) {
                                        Box(modifier = Modifier.fillMaxWidth()) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 24.dp, vertical = 16.dp)
                                                    .windowInsetsPadding(WindowInsets.navigationBars)
                                                    .imePadding()
                                                    .verticalScroll(rememberScrollState())
                                            ) {
                                                // Drag handle mockup
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .pointerInput(Unit) {
                                                            detectDragGestures { change, dragAmount ->
                                                                change.consume()
                                                                params.y = Math.max(0, params.y - dragAmount.y.toInt())
                                                                params.x += dragAmount.x.toInt()
                                                                windowManager?.updateViewLayout(frameLayout, params)
                                                            }
                                                        },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .width(32.dp)
                                                            .height(4.dp)
                                                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(50))
                                                    )
                                                }
                                                
                                                Spacer(modifier = Modifier.height(16.dp))
                                                
                                                when (val state = uiState) {
                                                    is DictionaryUiState.Idle -> {
                                                        var textToSearch by remember { mutableStateOf("") }
                                                        Column(
                                                            modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp),
                                                            horizontalAlignment = Alignment.CenterHorizontally,
                                                            verticalArrangement = Arrangement.Center
                                                        ) {
                                                            OutlinedTextField(
                                                                value = textToSearch,
                                                                onValueChange = { textToSearch = it },
                                                                label = { Text("Pesquisar palavra") },
                                                                modifier = Modifier.fillMaxWidth()
                                                            )
                                                            Spacer(modifier = Modifier.height(16.dp))
                                                            Row(
                                                                modifier = Modifier.fillMaxWidth(),
                                                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                                                            ) {
                                                                OutlinedButton(
                                                                    onClick = {
                                                                        scope.launch {
                                                                            isVisible = false
                                                                            kotlinx.coroutines.delay(300)
                                                                            stopSelf()
                                                                        }
                                                                    },
                                                                    modifier = Modifier.weight(1f)
                                                                ) {
                                                                    Text("Fechar")
                                                                }
                                                                Button(
                                                                    onClick = { 
                                                                        if (textToSearch.isNotBlank()) {
                                                                            viewModel.showBottomSheet(textToSearch)
                                                                        }
                                                                    },
                                                                    modifier = Modifier.weight(1f)
                                                                ) {
                                                                    Text("Buscar")
                                                                }
                                                            }
                                                        }
                                                    }
                                                    is DictionaryUiState.Loading -> {
                                                        Box(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .height(200.dp),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            ProcessingIndicator()
                                                        }
                                                    }
                                                    is DictionaryUiState.Error -> {
                                                        Box(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .height(200.dp),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Text(
                                                                text = "Erro: ${state.message}",
                                                                color = MaterialTheme.colorScheme.error,
                                                                style = MaterialTheme.typography.bodyLarge
                                                            )
                                                        }
                                                    }
                                                    is DictionaryUiState.Success -> {
                                                        DictionaryResultContent(
                                                            state = state,
                                                            tts = tts,
                                                            isFavorite = isCurrentWordFavorite,
                                                            onToggleFavorite = { viewModel.toggleFavorite(state.result.word, state.result.definition) },
                                                            onClose = {
                                                                scope.launch {
                                                                    isVisible = false
                                                                    kotlinx.coroutines.delay(300)
                                                                    stopSelf()
                                                                }
                                                            }
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    
                                    // Resize handle
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(16.dp)
                                            .size(32.dp)
                                            .pointerInput(Unit) {
                                                detectDragGestures(
                                                    onDragStart = {
                                                        if (params.width == WindowManager.LayoutParams.MATCH_PARENT) {
                                                            params.width = frameLayout.width
                                                        }
                                                        if (params.height == WindowManager.LayoutParams.WRAP_CONTENT) {
                                                            params.height = frameLayout.height
                                                        }
                                                    },
                                                    onDrag = { change, dragAmount ->
                                                        change.consume()
                                                        params.height -= dragAmount.y.toInt()
                                                        params.width += (dragAmount.x.toInt() * 2)
                                                        
                                                        // Min sizes
                                                        if (params.height < 200) params.height = 200
                                                        if (params.width < 200) params.width = 200
                                                        
                                                        windowManager?.updateViewLayout(frameLayout, params)
                                                    }
                                                )
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.OpenWith,
                                            contentDescription = "Redimensionar",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    
                                    // Minimize handle
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopStart)
                                            .padding(16.dp)
                                            .size(32.dp)
                                            .clickable { isMinimized = true },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CloseFullscreen,
                                            contentDescription = "Minimizar",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        
        frameLayout.addView(view)
        
        composeView = frameLayout
        windowManager?.addView(frameLayout, params)
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        
        store.clear()
        tts?.stop()
        tts?.shutdown()
        
        composeView?.let {
            windowManager?.removeView(it)
        }
        composeView = null
    }
}
