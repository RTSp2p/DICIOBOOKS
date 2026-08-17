package com.example

import com.example.service.floating.DockEdge
import com.example.service.floating.FloatingDockCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FloatingDockCalculatorTest {

    private val screenWidth = 1080
    private val screenHeight = 2400
    private val bubbleWidth = 160
    private val bubbleHeight = 160
    private val topInset = 80
    private val bottomInset = 120

    @Test
    fun calculateInitialPosition_centersHorizontallyAndRestsFullyVisibleAtBottom() {
        val (initialX, initialY) = FloatingDockCalculator.calculateInitialPosition(
            bubbleWidth = bubbleWidth,
            bubbleHeight = bubbleHeight,
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            bottomInset = bottomInset,
            bottomPaddingPx = 40
        )

        // Horizontalmente centralizado: (1080 - 160) / 2 = 460
        assertEquals(460, initialX)
        // 100% visível acima da barra de navegação: (2400 - 120) - 160 - 40 = 2080
        assertEquals(2080, initialY)
        // O topo do ícone fica em 2080 e a base em 2080 + 160 = 2240, que é <= usableBottom (2280)
        assertTrue(initialY + bubbleHeight <= screenHeight - bottomInset)
    }

    @Test
    fun resolveReleasePosition_whenInFreeArea_staysInPlaceWithoutDocking() {
        // Solto no meio da tela (x = 400, y = 800)
        val result = FloatingDockCalculator.resolveReleasePosition(
            currentX = 400,
            currentY = 800,
            bubbleWidth = bubbleWidth,
            bubbleHeight = bubbleHeight,
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            topInset = topInset,
            bottomInset = bottomInset,
            thresholdPx = 50
        )

        assertFalse(result.isDocked)
        assertNull(result.edge)
        assertEquals(400, result.x)
        assertEquals(800, result.y)
    }

    @Test
    fun resolveReleasePosition_whenPushedAgainstLeftEdge_docksImmediatelyToLeft() {
        // Pressionado contra a borda esquerda (x = 10 <= 50)
        val result = FloatingDockCalculator.resolveReleasePosition(
            currentX = 10,
            currentY = 600,
            bubbleWidth = bubbleWidth,
            bubbleHeight = bubbleHeight,
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            topInset = topInset,
            bottomInset = bottomInset,
            thresholdPx = 50,
            visibleFraction = 0.5f
        )

        assertTrue(result.isDocked)
        assertEquals(DockEdge.LEFT, result.edge)
        assertEquals(-80, result.x) // Metade oculta à esquerda (80px visível)
        assertEquals(600, result.y)
    }

    @Test
    fun resolveReleasePosition_whenPushedAgainstRightEdge_docksImmediatelyToRight() {
        // Pressionado contra a borda direita (screenWidth - bubbleWidth = 920, x = 910)
        val result = FloatingDockCalculator.resolveReleasePosition(
            currentX = 910,
            currentY = 600,
            bubbleWidth = bubbleWidth,
            bubbleHeight = bubbleHeight,
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            topInset = topInset,
            bottomInset = bottomInset,
            thresholdPx = 50,
            visibleFraction = 0.5f
        )

        assertTrue(result.isDocked)
        assertEquals(DockEdge.RIGHT, result.edge)
        assertEquals(1000, result.x) // 1080 - 80 = 1000 (metade visível na tela)
        assertEquals(600, result.y)
    }

    @Test
    fun resolveReleasePosition_whenPushedAgainstTopEdge_docksWithExactHalfVisibleAtTop() {
        // Pressionado contra o topo (currentY = 90, topInset = 80 -> dist = 10 <= 50)
        val result = FloatingDockCalculator.resolveReleasePosition(
            currentX = 400,
            currentY = 90,
            bubbleWidth = bubbleWidth,
            bubbleHeight = bubbleHeight,
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            topInset = topInset,
            bottomInset = bottomInset,
            thresholdPx = 50,
            visibleFraction = 0.5f
        )

        assertTrue(result.isDocked)
        assertEquals(DockEdge.TOP, result.edge)
        assertEquals(400, result.x)
        // topInset - hiddenHeight = 80 - 80 = 0 (exatamente 50% visível a partir do status bar)
        assertEquals(0, result.y)
    }

    @Test
    fun resolveReleasePosition_whenPushedAgainstBottomEdge_docksWithExactHalfVisibleAtBottom() {
        // usableBottom = 2400 - 120 = 2280. Icon at y = 2110 (2280 - 160 = 2120, dist = 10 <= 50)
        val result = FloatingDockCalculator.resolveReleasePosition(
            currentX = 400,
            currentY = 2110,
            bubbleWidth = bubbleWidth,
            bubbleHeight = bubbleHeight,
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            topInset = topInset,
            bottomInset = bottomInset,
            thresholdPx = 50,
            visibleFraction = 0.5f
        )

        assertTrue(result.isDocked)
        assertEquals(DockEdge.BOTTOM, result.edge)
        assertEquals(400, result.x)
        // usableBottom - visibleHeight = 2280 - 80 = 2200
        // Icone vai de 2200 a 2360. A tela útil termina em 2280 -> Exatamente 80px (50%) visível!
        assertEquals(2200, result.y)
    }

    @Test
    fun calculateNearestEdge_docksToClosestEdgeAfterTimeout() {
        // Teste de timeout (60s): ícone próximo à esquerda
        val result = FloatingDockCalculator.calculateNearestEdge(
            currentX = 50,
            currentY = 1000,
            bubbleWidth = bubbleWidth,
            bubbleHeight = bubbleHeight,
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            topInset = topInset,
            bottomInset = bottomInset,
            visibleFraction = 0.5f
        )

        assertEquals(DockEdge.LEFT, result.edge)
        assertEquals(-80, result.x)
        assertEquals(1000, result.y)
    }

    @Test
    fun clampDragPosition_constrainsWithinVisibleAndInsetBounds() {
        val clamped = FloatingDockCalculator.clampDragPosition(
            newX = -500,
            newY = 3000,
            bubbleWidth = bubbleWidth,
            bubbleHeight = bubbleHeight,
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            topInset = topInset,
            bottomInset = bottomInset
        )

        // Min X is -80 (-bubbleWidth/2)
        assertEquals(-80, clamped.first)
        // Max Y is usableBottom - bubbleHeight/2 = (2400 - 120) - 80 = 2200
        assertEquals(2200, clamped.second)
    }
}
