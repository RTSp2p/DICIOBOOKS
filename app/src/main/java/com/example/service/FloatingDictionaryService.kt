package com.example.service

import android.app.Service
import android.content.ClipboardManager
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.DictionaryViewModel
import com.example.service.floating.FloatingWindowContent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

class FloatingDictionaryService : Service(), LifecycleOwner, SavedStateRegistryOwner, ViewModelStoreOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private val store = ViewModelStore()

    private var windowManager: WindowManager? = null
    private var composeView: View? = null
    private lateinit var viewModel: DictionaryViewModel
    private var tts: TextToSpeech? = null

    private val detectedCopiedTextFlow = MutableStateFlow<String?>(null)
    private var lastHandledClipText: String? = null
    private var clipboardManager: ClipboardManager? = null
    private var clipListener: ClipboardManager.OnPrimaryClipChangedListener? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var clipPollingJob: Job? = null
    private val isMinimizedStateRef = mutableStateOf<MutableState<Boolean>?>(null)
    private var onOutsideTouchAction: (() -> Unit)? = null

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry
    override val viewModelStore: ViewModelStore get() = store

    fun checkClipboard() {
        val prefs = getSharedPreferences("dictionary_prefs", MODE_PRIVATE)
        val isClipboardEnabled = prefs.getBoolean("is_clipboard_listener_enabled", true)
        if (!isClipboardEnabled) return
        try {
            val cm = clipboardManager ?: (getSystemService(CLIPBOARD_SERVICE) as? ClipboardManager)?.also { clipboardManager = it }
            if (cm != null && cm.hasPrimaryClip()) {
                val clip = cm.primaryClip
                if (clip != null && clip.itemCount > 0) {
                    val item = clip.getItemAt(0)
                    val rawText = (item.text?.toString() ?: item.coerceToText(this)?.toString())?.trim()
                    if (!rawText.isNullOrBlank() && rawText != lastHandledClipText && rawText.length in 1..300) {
                        lastHandledClipText = rawText
                        detectedCopiedTextFlow.value = rawText
                    }
                }
            }
        } catch (e: Exception) {
            // Handled for background privacy constraints on Android 10+
        }
    }

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[DictionaryViewModel::class.java]

        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale("pt", "BR")
            }
        }

        clipboardManager = getSystemService(CLIPBOARD_SERVICE) as? ClipboardManager
        clipListener = ClipboardManager.OnPrimaryClipChangedListener {
            checkClipboard()
        }
        clipListener?.let { clipboardManager?.addPrimaryClipChangedListener(it) }

        clipPollingJob = serviceScope.launch {
            while (true) {
                delay(1200L)
                checkClipboard()
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (lifecycleRegistry.currentState == Lifecycle.State.CREATED) {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        }

        val word = intent?.getStringExtra("EXTRA_WORD")
        val copiedText = intent?.getStringExtra("EXTRA_COPIED_TEXT")

        if (!copiedText.isNullOrBlank()) {
            lastHandledClipText = copiedText.trim()
            detectedCopiedTextFlow.value = copiedText.trim()
        }

        if (!word.isNullOrBlank()) {
            isMinimizedStateRef.value?.value = false
            viewModel.showBottomSheet(word)
        }

        checkClipboard()
        showFloatingWindow(word)
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun showFloatingWindow(word: String?) {
        if (composeView != null) {
            if (word != null) {
                isMinimizedStateRef.value?.value = false
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
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }

        val frameLayout = FrameLayout(this).apply {
            fitsSystemWindows = false
            clipToPadding = false
            clipChildren = false
            setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_OUTSIDE) {
                    onOutsideTouchAction?.invoke()
                }
                false
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                addOnLayoutChangeListener { v, left, top, right, bottom, _, _, _, _ ->
                    val w = right - left
                    val h = bottom - top
                    if (w > 0 && h > 0) {
                        v.systemGestureExclusionRects = listOf(android.graphics.Rect(0, 0, w, h))
                    }
                }
            }
        }

        frameLayout.setViewTreeLifecycleOwner(this)
        frameLayout.setViewTreeSavedStateRegistryOwner(this)
        frameLayout.setViewTreeViewModelStoreOwner(this)

        if (word != null) {
            viewModel.showBottomSheet(word)
        }

        val view = ComposeView(this).apply {
            fitsSystemWindows = false
            clipToPadding = false
            clipChildren = false
            setContent {
                FloatingWindowContent(
                    viewModel = viewModel,
                    tts = tts,
                    params = params,
                    frameLayout = frameLayout,
                    windowManager = windowManager,
                    resources = resources,
                    detectedCopiedTextFlow = detectedCopiedTextFlow,
                    initialWord = word,
                    isMinimizedStateRef = isMinimizedStateRef,
                    onCheckClipboard = { checkClipboard() },
                    onStopService = { stopSelf() },
                    registerOutsideTouchHandler = { handler ->
                        onOutsideTouchAction = handler
                    }
                )
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

        clipPollingJob?.cancel()
        serviceScope.cancel()
        clipListener?.let { clipboardManager?.removePrimaryClipChangedListener(it) }

        store.clear()
        tts?.stop()
        tts?.shutdown()

        composeView?.let {
            windowManager?.removeView(it)
        }
        composeView = null
    }
}
