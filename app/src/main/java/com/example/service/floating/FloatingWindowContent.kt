package com.example.service.floating

import android.content.res.Resources
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalFocusManager
import com.example.DictionaryViewModel
import com.example.ThemeMode
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Gerenciador de apresentação da janela flutuante e bolha na camada de UI.
 * Integra o ViewModel e o ciclo de vida do WindowManager com o FloatingDockCalculator.
 */
@Composable
fun FloatingWindowContent(
    viewModel: DictionaryViewModel,
    tts: TextToSpeech?,
    params: WindowManager.LayoutParams,
    frameLayout: FrameLayout,
    windowManager: WindowManager?,
    resources: Resources,
    detectedCopiedTextFlow: MutableStateFlow<String?>,
    initialWord: String?,
    isMinimizedStateRef: MutableState<MutableState<Boolean>?>,
    onCheckClipboard: () -> Unit,
    onStopService: () -> Unit,
    registerOutsideTouchHandler: ((() -> Unit)?) -> Unit
) {
    val themeMode by viewModel.themeMode.collectAsState()
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    MyApplicationTheme(darkTheme = darkTheme) {
        val focusManager = LocalFocusManager.current
        val focusRequester = remember { FocusRequester() }

        // Outside touch handler para remover foco do teclado ao tocar fora
        DisposableEffect(Unit) {
            val handler = {
                focusManager.clearFocus(force = true)
                if ((params.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE) == 0) {
                    params.flags = params.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    windowManager?.updateViewLayout(frameLayout, params)
                }
            }
            registerOutsideTouchHandler(handler)
            onDispose {
                registerOutsideTouchHandler(null)
            }
        }

        val uiState by viewModel.uiState.collectAsState()
        val isCurrentWordFavorite by viewModel.isCurrentWordFavorite.collectAsState()
        val floatingFontSize by viewModel.floatingFontSize.collectAsState()
        val floatingIconOpacity by viewModel.floatingIconOpacity.collectAsState()
        val floatingWindowOpacity by viewModel.floatingWindowOpacity.collectAsState()
        val targetLanguage by viewModel.targetLanguage.collectAsState()
        val searchMode by viewModel.searchMode.collectAsState()

        val detectedCopiedText by detectedCopiedTextFlow.collectAsState()

        LaunchedEffect(detectedCopiedText) {
            if (detectedCopiedText != null) {
                delay(10000L)
                detectedCopiedTextFlow.value = null
            }
        }

        var isVisible by remember { mutableStateOf(false) }
        var isMinimized by remember { mutableStateOf(initialWord == null) }
        val minimizedHolder = remember { mutableStateOf(isMinimized) }

        DisposableEffect(isMinimized) {
            minimizedHolder.value = isMinimized
            isMinimizedStateRef.value = minimizedHolder
            if (!isMinimized) {
                onCheckClipboard()
            }
            onDispose { }
        }

        LaunchedEffect(Unit) {
            isVisible = true
        }

        LaunchedEffect(targetLanguage) {
            tts?.language = targetLanguage.locale
        }

        val displayMetrics = resources.displayMetrics
        val density = displayMetrics.density
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels

        // Tamanho estável e determinístico da bolha (56dp + 8dp de padding = 64dp)
        val bubbleSizePx = (64 * density).toInt()

        // Detecção precisa de insets de Status Bar (topo) e Navigation Bar (base)
        val topInset: Int = remember(screenWidth, screenHeight) {
            var insetValue = 0
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R && windowManager != null) {
                try {
                    val insets = windowManager.currentWindowMetrics.windowInsets.getInsetsIgnoringVisibility(
                        android.view.WindowInsets.Type.statusBars() or android.view.WindowInsets.Type.displayCutout()
                    )
                    insetValue = insets.top
                } catch (_: Exception) { }
            }
            if (insetValue <= 0) {
                val statusResId = resources.getIdentifier("status_bar_height", "dimen", "android")
                if (statusResId > 0) {
                    insetValue = resources.getDimensionPixelSize(statusResId)
                }
            }
            insetValue
        }

        val bottomInset: Int = remember(screenWidth, screenHeight) {
            var insetValue = 0
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R && windowManager != null) {
                try {
                    val insets = windowManager.currentWindowMetrics.windowInsets.getInsetsIgnoringVisibility(
                        android.view.WindowInsets.Type.navigationBars()
                    )
                    insetValue = insets.bottom
                } catch (_: Exception) { }
            }
            if (insetValue <= 0) {
                val navResId = resources.getIdentifier("navigation_bar_height", "dimen", "android")
                if (navResId > 0) {
                    insetValue = resources.getDimensionPixelSize(navResId)
                }
            }
            insetValue
        }

        // Posição inicial: centralizado horizontalmente na borda inferior e 100% visível
        val initialPos = remember(screenWidth, screenHeight, bubbleSizePx, bottomInset) {
            FloatingDockCalculator.calculateInitialPosition(
                bubbleWidth = bubbleSizePx,
                bubbleHeight = bubbleSizePx,
                screenWidth = screenWidth,
                screenHeight = screenHeight,
                bottomInset = bottomInset,
                bottomPaddingPx = (16 * density).toInt()
            )
        }

        var lastFabX by remember { mutableIntStateOf(initialPos.first) }
        var lastFabY by remember { mutableIntStateOf(initialPos.second) }
        var interactionCount by remember { mutableIntStateOf(0) }

        // Função de acolhimento (docking) para a borda mais próxima (após 60s de inatividade)
        fun dockToNearestEdge() {
            val dockPosition = FloatingDockCalculator.calculateNearestEdge(
                currentX = params.x,
                currentY = params.y,
                bubbleWidth = bubbleSizePx,
                bubbleHeight = bubbleSizePx,
                screenWidth = screenWidth,
                screenHeight = screenHeight,
                topInset = topInset,
                bottomInset = bottomInset,
                visibleFraction = 0.5f
            )

            params.x = dockPosition.x
            params.y = dockPosition.y
            lastFabX = params.x
            lastFabY = params.y
            windowManager?.updateViewLayout(frameLayout, params)
        }

        // Função ao soltar o arrasto: acolhe imediatamente se pressionado contra a borda, ou permanece no local
        fun handleDragRelease() {
            val resolvedPosition = FloatingDockCalculator.resolveReleasePosition(
                currentX = params.x,
                currentY = params.y,
                bubbleWidth = bubbleSizePx,
                bubbleHeight = bubbleSizePx,
                screenWidth = screenWidth,
                screenHeight = screenHeight,
                topInset = topInset,
                bottomInset = bottomInset,
                thresholdPx = (bubbleSizePx / 3).coerceAtLeast((24 * density).toInt()),
                visibleFraction = 0.5f
            )

            params.x = resolvedPosition.x
            params.y = resolvedPosition.y
            lastFabX = params.x
            lastFabY = params.y
            windowManager?.updateViewLayout(frameLayout, params)
        }

        // Configuração de LayoutParams ao alternar entre minimizado (bolha) e expandido (janela)
        LaunchedEffect(isMinimized) {
            if (isMinimized) {
                delay(180L)
                params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                        WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                params.width = WindowManager.LayoutParams.WRAP_CONTENT
                params.height = WindowManager.LayoutParams.WRAP_CONTENT
                params.gravity = Gravity.TOP or Gravity.START
                params.x = lastFabX
                params.y = lastFabY
                windowManager?.updateViewLayout(frameLayout, params)
            } else {
                params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                        WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
                params.width = WindowManager.LayoutParams.MATCH_PARENT
                params.height = WindowManager.LayoutParams.WRAP_CONTENT
                params.gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                params.x = 0
                params.y = 0
                windowManager?.updateViewLayout(frameLayout, params)
            }
        }

        // Temporizador de Inatividade de 60 Segundos: recolhe automaticamente para a borda mais próxima
        LaunchedEffect(isMinimized, interactionCount) {
            if (isMinimized) {
                delay(60000L)
                dockToNearestEdge()
            }
        }

        fun ensureFocusable() {
            if ((params.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE) != 0) {
                params.flags = params.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()
                windowManager?.updateViewLayout(frameLayout, params)
            }
        }

        fun releaseFocusable() {
            if ((params.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE) == 0) {
                params.flags = params.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                windowManager?.updateViewLayout(frameLayout, params)
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
                FloatingBubbleView(
                    detectedCopiedText = detectedCopiedText,
                    floatingIconOpacity = floatingIconOpacity,
                    onCopiedTextClick = { clipText ->
                        detectedCopiedTextFlow.value = null
                        viewModel.showBottomSheet(clipText)
                        isMinimized = false
                    },
                    onDismissCopiedBadge = {
                        detectedCopiedTextFlow.value = null
                    },
                    onExpandWindow = {
                        isMinimized = false
                    },
                    onCheckClipboard = onCheckClipboard,
                    onDragDelta = { dx, dy ->
                        val (clampedX, clampedY) = FloatingDockCalculator.clampDragPosition(
                            newX = params.x + dx.toInt(),
                            newY = params.y + dy.toInt(),
                            bubbleWidth = bubbleSizePx,
                            bubbleHeight = bubbleSizePx,
                            screenWidth = screenWidth,
                            screenHeight = screenHeight,
                            topInset = topInset,
                            bottomInset = bottomInset
                        )

                        params.x = clampedX
                        params.y = clampedY
                        lastFabX = params.x
                        lastFabY = params.y
                        windowManager?.updateViewLayout(frameLayout, params)
                    },
                    onDragEnd = {
                        handleDragRelease()
                    },
                    onInteraction = {
                        interactionCount++
                    }
                )
            } else {
                FloatingExpandedView(
                    isVisible = isVisible,
                    uiState = uiState,
                    themeMode = themeMode,
                    searchMode = searchMode,
                    targetLanguage = targetLanguage,
                    floatingWindowOpacity = floatingWindowOpacity,
                    floatingFontSize = floatingFontSize,
                    isCurrentWordFavorite = isCurrentWordFavorite,
                    detectedCopiedText = detectedCopiedText,
                    focusRequester = focusRequester,
                    tts = tts,
                    screenWidthPx = resources.displayMetrics.widthPixels.toFloat(),
                    onDismissRequest = onStopService,
                    onMinimize = { isMinimized = true },
                    onThemeToggle = { viewModel.setThemeMode(it) },
                    onSearchModeChange = { viewModel.setSearchMode(it) },
                    onTargetLanguageChange = { viewModel.setTargetLanguage(it) },
                    onSearchWord = { viewModel.showBottomSheet(it) },
                    onResetToSearch = { viewModel.resetToSearch() },
                    onToggleFavorite = { word, def -> viewModel.toggleFavorite(word, def) },
                    onDismissCopiedText = { detectedCopiedTextFlow.value = null },
                    onEnsureFocusable = { ensureFocusable() },
                    onReleaseFocusable = { releaseFocusable() },
                    onCheckClipboard = onCheckClipboard,
                    onWindowMove = { dx, dy ->
                        val displayMetrics = resources.displayMetrics
                        val sW = displayMetrics.widthPixels
                        val sH = displayMetrics.heightPixels

                        val currentW = if (frameLayout.width > 0) frameLayout.width else (if (params.width > 0 && params.width != WindowManager.LayoutParams.MATCH_PARENT) params.width else sW)
                        val currentH = if (frameLayout.height > 0) frameLayout.height else (if (params.height > 0 && params.height != WindowManager.LayoutParams.WRAP_CONTENT) params.height else 400)

                        val maxDeltaX = ((sW - currentW) / 2).coerceAtLeast(0)
                        val maxY = (sH - currentH).coerceAtLeast(0)

                        if (kotlin.math.abs(dy) > 0.5f) {
                            params.y = (params.y - dy.toInt()).coerceIn(0, maxY)
                        }
                        if (kotlin.math.abs(dx) > 0.5f && maxDeltaX > 0) {
                            params.x = (params.x + dx.toInt()).coerceIn(-maxDeltaX, maxDeltaX)
                        }
                        windowManager?.updateViewLayout(frameLayout, params)
                    },
                    onWindowResizeStart = {
                        if (params.width == WindowManager.LayoutParams.MATCH_PARENT) {
                            params.width = frameLayout.width
                        }
                        if (params.height == WindowManager.LayoutParams.WRAP_CONTENT) {
                            params.height = frameLayout.height
                        }
                    },
                    onWindowResize = { dx, dy, densityVal ->
                        val displayMetrics = resources.displayMetrics
                        val sW = displayMetrics.widthPixels
                        val sH = displayMetrics.heightPixels

                        params.height -= dy.toInt()
                        params.width += (dx.toInt() * 2)

                        val minSizePx = (200 * densityVal).toInt()
                        if (params.height < minSizePx) params.height = minSizePx
                        if (params.height > sH) params.height = sH

                        if (params.width < minSizePx) params.width = minSizePx
                        if (params.width > sW) params.width = sW

                        val currentW = if (frameLayout.width > 0) frameLayout.width else params.width
                        val currentH = if (frameLayout.height > 0) frameLayout.height else params.height

                        val maxDeltaX = ((sW - currentW) / 2).coerceAtLeast(0)
                        val maxY = (sH - currentH).coerceAtLeast(0)

                        params.x = params.x.coerceIn(-maxDeltaX, maxDeltaX)
                        params.y = params.y.coerceIn(0, maxY)

                        windowManager?.updateViewLayout(frameLayout, params)
                    }
                )
            }
        }
    }
}
