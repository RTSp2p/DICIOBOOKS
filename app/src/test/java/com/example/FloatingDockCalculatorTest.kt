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
    private val topInset = 88       // Status bar + cutout
    private val bottomInset = 120    // Navigation bar / Gesture bar
    private val leftInset = 0
    private val rightInset = 0

    @Test
    fun calculateInitialPosition_centersHorizontallyAndRestsFullyVisibleAboveNavBar() {
        val (initialX, initialY) = FloatingDockCalculator.calculateInitialPosition(
            bubbleWidth = bubbleWidth,
            bubbleHeight = bubbleHeight,
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            bottomInset = bottomInset,
            bottomPaddingPx = 40,
            topInset = topInset,
            leftInset = leftInset,
            rightInset = rightInset
        )

        // Horizontalmente centralizado: (1080 - 160) / 2 = 460
        assertEquals(460, initialX)
        // 100% visível acima da barra de navegação: (2400 - 120) - 160 - 40 = 2080
        assertEquals(2080, initialY)
        assertTrue(initialY + bubbleHeight <= screenHeight - bottomInset)
        assertTrue(initialY >= topInset)
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
            thresholdPx = 50,
            topInset = topInset,
            bottomInset = bottomInset,
            leftInset = leftInset,
            rightInset = rightInset
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
            thresholdPx = 50,
            visibleFraction = 0.5f,
            topInset = topInset,
            bottomInset = bottomInset,
            leftInset = leftInset,
            rightInset = rightInset
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
            thresholdPx = 50,
            visibleFraction = 0.5f,
            topInset = topInset,
            bottomInset = bottomInset,
            leftInset = leftInset,
            rightInset = rightInset
        )

        assertTrue(result.isDocked)
        assertEquals(DockEdge.RIGHT, result.edge)
        assertEquals(1000, result.x) // 1080 - 80 = 1000 (metade visível na tela)
        assertEquals(600, result.y)
    }

    @Test
    fun resolveReleasePosition_whenNearTopEdge_doesNotDockAndStaysFreeInPlace() {
        // Solto próximo ao topo (mas longe das bordas laterais x = 400)
        val result = FloatingDockCalculator.resolveReleasePosition(
            currentX = 400,
            currentY = 90,
            bubbleWidth = bubbleWidth,
            bubbleHeight = bubbleHeight,
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            thresholdPx = 50,
            visibleFraction = 0.5f,
            topInset = topInset,
            bottomInset = bottomInset,
            leftInset = leftInset,
            rightInset = rightInset
        )

        // Não deve acolher no topo!
        assertFalse(result.isDocked)
        assertNull(result.edge)
        assertEquals(400, result.x)
        assertEquals(90, result.y)
    }

    @Test
    fun resolveReleasePosition_whenNearBottomEdge_doesNotDockAndStaysFreeInPlace() {
        // Solto próximo à base (mas longe das bordas laterais x = 400)
        val result = FloatingDockCalculator.resolveReleasePosition(
            currentX = 400,
            currentY = 2100,
            bubbleWidth = bubbleWidth,
            bubbleHeight = bubbleHeight,
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            thresholdPx = 50,
            visibleFraction = 0.5f,
            topInset = topInset,
            bottomInset = bottomInset,
            leftInset = leftInset,
            rightInset = rightInset
        )

        // Não deve acolher na base!
        assertFalse(result.isDocked)
        assertNull(result.edge)
        assertEquals(400, result.x)
        assertEquals(2100, result.y)
    }

    @Test
    fun calculateNearestEdge_docksToClosestHorizontalEdge() {
        // Mais próximo da esquerda (x = 300)
        val leftResult = FloatingDockCalculator.calculateNearestEdge(
            currentX = 300,
            currentY = 800,
            bubbleWidth = bubbleWidth,
            bubbleHeight = bubbleHeight,
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            topInset = topInset,
            bottomInset = bottomInset
        )
        assertEquals(DockEdge.LEFT, leftResult.edge)
        assertEquals(-80, leftResult.x)
        assertEquals(800, leftResult.y)

        // Mais próximo da direita (x = 700)
        val rightResult = FloatingDockCalculator.calculateNearestEdge(
            currentX = 700,
            currentY = 800,
            bubbleWidth = bubbleWidth,
            bubbleHeight = bubbleHeight,
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            topInset = topInset,
            bottomInset = bottomInset
        )
        assertEquals(DockEdge.RIGHT, rightResult.edge)
        assertEquals(1000, rightResult.x)
        assertEquals(800, rightResult.y)
    }

    @Test
    fun isPositionDocked_correctlyDetectsLateralDockedState() {
        // Na esquerda acolhida com 50% oculto (x = -80)
        assertTrue(FloatingDockCalculator.isPositionDocked(-80, 500, bubbleWidth, bubbleHeight, screenWidth, screenHeight))
        // Na direita acolhida com 50% oculto (x = 1000)
        assertTrue(FloatingDockCalculator.isPositionDocked(1000, 500, bubbleWidth, bubbleHeight, screenWidth, screenHeight))
        // Livre no centro
        assertFalse(FloatingDockCalculator.isPositionDocked(400, 500, bubbleWidth, bubbleHeight, screenWidth, screenHeight))
    }

    @Test
    fun undockPosition_bringsIconFullyVisibleOntoSafeScreenArea() {
        // Desacolhe da esquerda -> safeLeft + marginPx = 20
        val (undockLeftX, undockLeftY) = FloatingDockCalculator.undockPosition(
            currentX = -80,
            currentY = 800,
            bubbleWidth = bubbleWidth,
            bubbleHeight = bubbleHeight,
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            marginPx = 20,
            topInset = topInset,
            bottomInset = bottomInset,
            leftInset = leftInset,
            rightInset = rightInset
        )
        assertEquals(20, undockLeftX)
        assertEquals(800, undockLeftY)

        // Desacolhe da direita -> screenWidth - bubbleWidth - marginPx = 1080 - 160 - 20 = 900
        val (undockRightX, undockRightY) = FloatingDockCalculator.undockPosition(
            currentX = 1000,
            currentY = 800,
            bubbleWidth = bubbleWidth,
            bubbleHeight = bubbleHeight,
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            marginPx = 20,
            topInset = topInset,
            bottomInset = bottomInset,
            leftInset = leftInset,
            rightInset = rightInset
        )
        assertEquals(900, undockRightX)
        assertEquals(800, undockRightY)
    }

    @Test
    fun clampDragPosition_allowsLateralHalfOffscreenAndKeepsVerticalInSafeRange() {
        val clamped = FloatingDockCalculator.clampDragPosition(
            newX = -500,
            newY = 3000,
            bubbleWidth = bubbleWidth,
            bubbleHeight = bubbleHeight,
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            topInset = topInset,
            bottomInset = bottomInset,
            leftInset = leftInset,
            rightInset = rightInset
        )

        // Min X is -80 (-bubbleWidth/2)
        assertEquals(-80, clamped.first)
        // Max Y is safeBottom - bubbleHeight = (2400 - 120) - 160 = 2120
        assertEquals(2120, clamped.second)
    }
}
