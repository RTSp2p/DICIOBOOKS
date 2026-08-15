package com.example.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class TextSelectionAccessibilityService : AccessibilityService() {

    private var lastSelectedText: String? = null
    private var lastEventTime: Long = 0

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val prefs = getSharedPreferences("dictionary_prefs", Context.MODE_PRIVATE)
        val isClipboardEnabled = prefs.getBoolean("is_clipboard_listener_enabled", true)
        val now = SystemClock.elapsedRealtime()

        // 1. Detect copy button click in floating selection toolbar or app menus
        if (event.eventType == AccessibilityEvent.TYPE_VIEW_CLICKED) {
            val text = event.text.joinToString(" ")
            val contentDesc = event.contentDescription?.toString() ?: ""
            val isCopyAction = text.contains("Copiar", ignoreCase = true) ||
                    text.contains("Copy", ignoreCase = true) ||
                    contentDesc.contains("Copiar", ignoreCase = true) ||
                    contentDesc.contains("Copy", ignoreCase = true)

            if (isCopyAction && !lastSelectedText.isNullOrBlank() && isClipboardEnabled) {
                sendCopiedTextToFloatingService(lastSelectedText!!)
            }
        }

        // 2. Detect text selection changed in other applications
        if (event.eventType == AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED) {
            if (now - lastEventTime < 400) return

            val rawSelection = extractSelectedText(event)
            if (!rawSelection.isNullOrBlank()) {
                val cleanWord = cleanSelectedWord(rawSelection)
                if (cleanWord.isNotBlank() && cleanWord.length in 1..300 && cleanWord != lastSelectedText) {
                    lastSelectedText = cleanWord
                    lastEventTime = now
                    if (isClipboardEnabled) {
                        sendCopiedTextToFloatingService(cleanWord)
                    }
                }
            }
        }
    }

    private fun extractSelectedText(event: AccessibilityEvent): String? {
        val fromIndex = event.fromIndex
        val toIndex = event.toIndex

        if (fromIndex >= 0 && toIndex > fromIndex) {
            // Check text list attached to accessibility event
            val eventTextSequence = event.text.joinToString(" ")
            if (toIndex <= eventTextSequence.length) {
                return eventTextSequence.substring(fromIndex, toIndex)
            }

            // Check source node text
            val node: AccessibilityNodeInfo? = event.source
            if (node != null) {
                val nodeText = node.text?.toString()
                node.recycle()
                if (nodeText != null && toIndex <= nodeText.length) {
                    return nodeText.substring(fromIndex, toIndex)
                }
            }
        }
        return null
    }

    private fun cleanSelectedWord(raw: String): String {
        return raw.trim()
            .trimStart { !it.isLetterOrDigit() }
            .trimEnd { !it.isLetterOrDigit() }
    }

    private fun sendCopiedTextToFloatingService(text: String) {
        if (Settings.canDrawOverlays(this)) {
            val intent = Intent(this, FloatingDictionaryService::class.java).apply {
                putExtra("EXTRA_COPIED_TEXT", text)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startService(intent)
        }
    }

    override fun onInterrupt() {
        // Required for AccessibilityService
    }
}
