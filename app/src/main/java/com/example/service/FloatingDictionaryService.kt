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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.animation.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.compose.material.icons.filled.CloseFullscreen
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Alignment
import androidx.compose.animation.core.*
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
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
            text = "Pesquisando...",
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
                    val floatingFontSize by viewModel.floatingFontSize.collectAsState()
                    val floatingIconOpacity by viewModel.floatingIconOpacity.collectAsState()
                    val targetLanguage by viewModel.targetLanguage.collectAsState()
                    var isVisible by remember { mutableStateOf(false) }
                    var isMinimized by remember { mutableStateOf(word == null) }
                    val scope = rememberCoroutineScope()
                    
                    LaunchedEffect(Unit) {
                        isVisible = true
                    }

                    LaunchedEffect(targetLanguage) {
                        tts?.language = targetLanguage.locale
                    }

                    val displayMetrics = resources.displayMetrics
                    val screenWidth = displayMetrics.widthPixels
                    val screenHeight = displayMetrics.heightPixels
                    val initialFabW = if (frameLayout.width > 0) frameLayout.width else 150
                    val initialFabH = if (frameLayout.height > 0) frameLayout.height else 150

                    val initialFabX = (screenWidth - initialFabW) / 2
                    val initialFabY = screenHeight - initialFabH / 2

                    var lastFabX by remember { mutableIntStateOf(initialFabX) }
                    var lastFabY by remember { mutableIntStateOf(initialFabY) }

                    LaunchedEffect(isMinimized) {
                        if (isMinimized) {
                            delay(200L)
                            params.flags = params.flags and WindowManager.LayoutParams.FLAG_DIM_BEHIND.inv()
                            params.flags = params.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                            params.width = WindowManager.LayoutParams.WRAP_CONTENT
                            params.height = WindowManager.LayoutParams.WRAP_CONTENT
                            params.gravity = Gravity.TOP or Gravity.START
                            params.x = lastFabX
                            params.y = lastFabY
                            windowManager?.updateViewLayout(frameLayout, params)
                        } else {
                            params.flags = params.flags or WindowManager.LayoutParams.FLAG_DIM_BEHIND
                            params.flags = params.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()
                            params.width = WindowManager.LayoutParams.MATCH_PARENT
                            params.height = WindowManager.LayoutParams.WRAP_CONTENT
                            params.gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                            params.x = 0
                            params.y = 0
                            windowManager?.updateViewLayout(frameLayout, params)
                        }
                    }
                    
                    var interactionCount by remember { mutableIntStateOf(0) }

                    fun dockToNearestEdge() {
                        val metrics = resources.displayMetrics
                        val sWidth = metrics.widthPixels
                        val sHeight = metrics.heightPixels
                        val fabW = if (frameLayout.width > 0) frameLayout.width else 150
                        val fabH = if (frameLayout.height > 0) frameLayout.height else 150

                        val maxUsableX = (sWidth - fabW).coerceAtLeast(0)
                        val maxUsableY = (sHeight - fabH).coerceAtLeast(0)

                        val currentX = params.x
                        val currentY = params.y

                        // Compute targets for docking with half the icon visible:
                        val leftTargetX = -fabW / 2
                        val leftTargetY = currentY.coerceIn(0, maxUsableY)
                        val distLeft = kotlin.math.abs(currentX - leftTargetX)

                        val rightTargetX = sWidth - fabW / 2
                        val rightTargetY = currentY.coerceIn(0, maxUsableY)
                        val distRight = kotlin.math.abs(currentX - rightTargetX)

                        val topTargetX = currentX.coerceIn(0, maxUsableX)
                        val topTargetY = -fabH / 2
                        val distTop = kotlin.math.abs(currentY - topTargetY)

                        val bottomTargetX = currentX.coerceIn(0, maxUsableX)
                        val bottomTargetY = sHeight - fabH / 2
                        val distBottom = kotlin.math.abs(currentY - bottomTargetY)

                        val minDist = minOf(distLeft, distRight, distTop, distBottom)

                        when (minDist) {
                            distLeft -> {
                                params.x = leftTargetX
                                params.y = leftTargetY
                            }
                            distRight -> {
                                params.x = rightTargetX
                                params.y = rightTargetY
                            }
                            distTop -> {
                                params.x = topTargetX
                                params.y = topTargetY
                            }
                            distBottom -> {
                                params.x = bottomTargetX
                                params.y = bottomTargetY
                            }
                        }
                        lastFabX = params.x
                        lastFabY = params.y
                        windowManager?.updateViewLayout(frameLayout, params)
                    }

                    LaunchedEffect(isMinimized, interactionCount) {
                        if (isMinimized) {
                            delay(60000L)
                            dockToNearestEdge()
                        }
                    }

                    AnimatedContent(
                        targetState = isMinimized,
                        transitionSpec = {
                            if (targetState) {
                                (fadeIn(animationSpec = tween(durationMillis = 200, easing = LinearOutSlowInEasing)) +
                                 scaleIn(initialScale = 0.6f, animationSpec = tween(durationMillis = 200))) togetherWith
                                (fadeOut(animationSpec = tween(durationMillis = 180, easing = FastOutLinearInEasing)) +
                                 slideOutVertically(targetOffsetY = { it / 2 }, animationSpec = tween(durationMillis = 180)) +
                                 scaleOut(targetScale = 0.92f, animationSpec = tween(durationMillis = 180)))
                            } else {
                                (fadeIn(animationSpec = tween(durationMillis = 250, easing = LinearOutSlowInEasing)) +
                                 slideInVertically(initialOffsetY = { it / 2 }, animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing)) +
                                 scaleIn(initialScale = 0.92f, animationSpec = tween(durationMillis = 250))) togetherWith
                                (fadeOut(animationSpec = tween(durationMillis = 150, easing = FastOutLinearInEasing)) +
                                 scaleOut(targetScale = 0.6f, animationSpec = tween(durationMillis = 150)))
                            }
                        },
                        label = "FloatingWindowTransition"
                    ) { minimized ->
                        if (minimized) {
                            FloatingActionButton(
                                onClick = {},
                                shape = CircleShape,
                                modifier = Modifier
                                    .graphicsLayer { alpha = floatingIconOpacity }
                                    .padding(4.dp)
                                    .pointerInput(Unit) {
                                        awaitEachGesture {
                                            val down = awaitFirstDown(requireUnconsumed = false)
                                            var totalDx = 0f
                                            var totalDy = 0f
                                            var isDrag = false
                                            val touchSlop = viewConfiguration.touchSlop

                                            interactionCount++

                                            do {
                                                val event = awaitPointerEvent()
                                                val pointerChange = event.changes.find { it.id == down.id } ?: break
                                                if (pointerChange.pressed) {
                                                    val posChange = pointerChange.positionChange()
                                                    totalDx += posChange.x
                                                    totalDy += posChange.y

                                                    if (!isDrag && kotlin.math.hypot(totalDx, totalDy) > touchSlop) {
                                                        isDrag = true
                                                    }

                                                    if (isDrag) {
                                                        pointerChange.consume()
                                                        val displayMetrics = resources.displayMetrics
                                                        val screenWidth = displayMetrics.widthPixels
                                                        val screenHeight = displayMetrics.heightPixels
                                                        val fabW = if (frameLayout.width > 0) frameLayout.width else 150
                                                        val fabH = if (frameLayout.height > 0) frameLayout.height else 150

                                                        params.x = (params.x + posChange.x.toInt())
                                                            .coerceIn(-fabW / 2, screenWidth - fabW / 2)
                                                        params.y = (params.y + posChange.y.toInt())
                                                            .coerceIn(-fabH / 2, screenHeight - fabH / 2)
                                                        lastFabX = params.x
                                                        lastFabY = params.y
                                                        windowManager?.updateViewLayout(frameLayout, params)
                                                    }
                                                } else {
                                                    if (isDrag) {
                                                        dockToNearestEdge()
                                                        interactionCount++
                                                    } else {
                                                        isMinimized = false
                                                        interactionCount++
                                                    }
                                                    break
                                                }
                                            } while (event.changes.any { it.pressed })
                                        }
                                    }
                            ) {
                                Icon(imageVector = Icons.Default.Search, contentDescription = "Maximizar")
                            }
                        } else {
                            val dragOffsetX = remember { androidx.compose.animation.core.Animatable(0f) }
                            val density = LocalDensity.current
                            val dismissThresholdPx = with(density) { 150.dp.toPx() } // distância do arrasto até fechar ( fecha mais rapido > valor > demora mais)

                            AnimatedVisibility(
                                visible = isVisible,
                                enter = androidx.compose.animation.slideInVertically(initialOffsetY = { it }) + androidx.compose.animation.fadeIn(),
                                exit = androidx.compose.animation.slideOutVertically(targetOffsetY = { it }) + androidx.compose.animation.fadeOut()
                            ) {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 200.dp)
                                        .graphicsLayer {
                                            translationX = dragOffsetX.value
                                            val absX = kotlin.math.abs(dragOffsetX.value)
                                            alpha = (1f - (absX / (dismissThresholdPx * 2f))).coerceIn(0.15f, 1f)
                                        },
                                    shape = RoundedCornerShape(28.dp),
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
                                                    // Drag handle with horizontal swipe to dismiss gesture and vertical window move
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .pointerInput(Unit) {
                                                                detectDragGestures(
                                                                    onDragEnd = {
                                                                        val currentOffsetX = dragOffsetX.value
                                                                        if (kotlin.math.abs(currentOffsetX) > dismissThresholdPx) {
                                                                            scope.launch {
                                                                                val screenWidth = resources.displayMetrics.widthPixels.toFloat()
                                                                                val targetX = if (currentOffsetX > 0) currentOffsetX + screenWidth else currentOffsetX - screenWidth
                                                                                launch { dragOffsetX.animateTo(targetX, tween(200)) }
                                                                                isVisible = false
                                                                                delay(220)
                                                                                stopSelf()
                                                                            }
                                                                        } else {
                                                                            scope.launch {
                                                                                dragOffsetX.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow))
                                                                            }
                                                                        }
                                                                    },
                                                                    onDragCancel = {
                                                                        scope.launch {
                                                                            dragOffsetX.animateTo(0f)
                                                                        }
                                                                    },
                                                                    onDrag = { change, dragAmount ->
                                                                        change.consume()
                                                                        val displayMetrics = resources.displayMetrics
                                                                        val screenWidth = displayMetrics.widthPixels
                                                                        val screenHeight = displayMetrics.heightPixels

                                                                        val currentW = if (frameLayout.width > 0) frameLayout.width else (if (params.width > 0 && params.width != WindowManager.LayoutParams.MATCH_PARENT) params.width else screenWidth)
                                                                        val currentH = if (frameLayout.height > 0) frameLayout.height else (if (params.height > 0 && params.height != WindowManager.LayoutParams.WRAP_CONTENT) params.height else 400)

                                                                        val maxDeltaX = ((screenWidth - currentW) / 2).coerceAtLeast(0)
                                                                        val maxY = (screenHeight - currentH).coerceAtLeast(0)

                                                                        // Movimento vertical (params.y) delimitado pelas bordas superior e inferior da tela
                                                                        if (kotlin.math.abs(dragAmount.y) > 0.5f) {
                                                                            params.y = (params.y - dragAmount.y.toInt()).coerceIn(0, maxY)
                                                                        }
                                                                        // Movimento horizontal (params.x) se a janela for menor que a tela
                                                                        if (kotlin.math.abs(dragAmount.x) > 0.5f && maxDeltaX > 0) {
                                                                            params.x = (params.x + dragAmount.x.toInt()).coerceIn(-maxDeltaX, maxDeltaX)
                                                                        }
                                                                        windowManager?.updateViewLayout(frameLayout, params)

                                                                        // Movimento horizontal acumula o deslocamento para fechar por deslizamento
                                                                        scope.launch {
                                                                            dragOffsetX.snapTo(dragOffsetX.value + dragAmount.x)
                                                                        }
                                                                    }
                                                                )
                                                            },
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Column(
                                                            horizontalAlignment = Alignment.CenterHorizontally,
                                                            modifier = Modifier.padding(vertical = 4.dp)
                                                        ) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .width(36.dp)
                                                                    .height(5.dp)
                                                                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(50))
                                                            )
                                                            Spacer(modifier = Modifier.height(2.dp))
                                                            Text(
                                                                text = "◄ Deslize para fechar ►",
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                                            )
                                                        }
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
                                                            androidx.compose.foundation.layout.Column(
                                                                modifier = Modifier
                                                                    .fillMaxWidth()
                                                                    .padding(24.dp),
                                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                                verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
                                                            ) {
                                                                Text(
                                                                    text = "Erro: ${state.message}",
                                                                    color = MaterialTheme.colorScheme.error,
                                                                    style = MaterialTheme.typography.bodyLarge,
                                                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                                                    modifier = Modifier.padding(bottom = 16.dp)
                                                                )
                                                                Button(
                                                                    onClick = {
                                                                        scope.launch {
                                                                            isVisible = false
                                                                            viewModel.hideBottomSheet()
                                                                        }
                                                                    }
                                                                ) {
                                                                    Text("Fechar")
                                                                }
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
                                                                },
                                                                fontSizeSp = floatingFontSize,
                                                                currentLanguage = targetLanguage,
                                                                onSelectLanguage = { lang ->
                                                                    viewModel.setTargetLanguage(lang)
                                                                    viewModel.showBottomSheet(state.result.word)
                                                                }
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        
                                        // Theme toggle button
                                        IconButton(
                                            onClick = {
                                                val nextTheme = when (themeMode) {
                                                    com.example.ThemeMode.SYSTEM -> com.example.ThemeMode.LIGHT
                                                    com.example.ThemeMode.LIGHT -> com.example.ThemeMode.DARK
                                                    com.example.ThemeMode.DARK -> com.example.ThemeMode.SYSTEM
                                                }
                                                viewModel.setThemeMode(nextTheme)
                                            },
                                            modifier = Modifier
                                                .align(Alignment.TopStart)
                                                .padding(top = 12.dp, start = 50.dp)
                                                .size(36.dp)
                                        ) {
                                            val (icon, description) = when (themeMode) {
                                                com.example.ThemeMode.SYSTEM -> Pair(Icons.Default.BrightnessAuto, "Tema: Sistema")
                                                com.example.ThemeMode.LIGHT -> Pair(Icons.Default.LightMode, "Tema: Claro")
                                                com.example.ThemeMode.DARK -> Pair(Icons.Default.DarkMode, "Tema: Escuro")
                                            }
                                            Icon(
                                                imageVector = icon,
                                                contentDescription = description,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(20.dp)
                                            )
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
                                                            val displayMetrics = resources.displayMetrics
                                                            val screenWidth = displayMetrics.widthPixels
                                                            val screenHeight = displayMetrics.heightPixels

                                                            params.height -= dragAmount.y.toInt()
                                                            params.width += (dragAmount.x.toInt() * 2)
                                                            
                                                            val minSizePx = (200 * currentDensity.density).toInt()
                                                            if (params.height < minSizePx) params.height = minSizePx
                                                            if (params.height > screenHeight) params.height = screenHeight

                                                            if (params.width < minSizePx) params.width = minSizePx
                                                            if (params.width > screenWidth) params.width = screenWidth

                                                            val currentW = if (frameLayout.width > 0) frameLayout.width else params.width
                                                            val currentH = if (frameLayout.height > 0) frameLayout.height else params.height

                                                            val maxDeltaX = ((screenWidth - currentW) / 2).coerceAtLeast(0)
                                                            val maxY = (screenHeight - currentH).coerceAtLeast(0)

                                                            params.x = params.x.coerceIn(-maxDeltaX, maxDeltaX)
                                                            params.y = params.y.coerceIn(0, maxY)

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
