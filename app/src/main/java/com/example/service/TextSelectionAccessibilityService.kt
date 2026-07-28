package com.example.service

import android.accessibilityservice.AccessibilityService
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

        if (event.eventType == AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED) {
            val now = SystemClock.elapsedRealtime()
            // Avoid duplicate triggers in short time windows (e.g., 600ms)
            if (now - lastEventTime < 600) return

            val rawSelection = extractSelectedText(event)
            if (!rawSelection.isNullOrBlank()) {
                val cleanWord = cleanSelectedWord(rawSelection)
                if (cleanWord.isNotBlank() && cleanWord.length in 2..100 && cleanWord != lastSelectedText) {
                    lastSelectedText = cleanWord
                    lastEventTime = now
                    openFloatingDictionary(cleanWord)
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

    private fun openFloatingDictionary(word: String) {
        if (Settings.canDrawOverlays(this)) {
            val intent = Intent(this, FloatingDictionaryService::class.java).apply {
                putExtra("EXTRA_WORD", word)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startService(intent)
        }
    }

    override fun onInterrupt() {
        // Required for AccessibilityService
    }
}
