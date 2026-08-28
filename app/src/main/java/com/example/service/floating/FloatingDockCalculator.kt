package com.example.service.floating

import kotlin.math.abs

/**
 * Representa a borda física para onde o ícone flutuante foi acolhido.
 */
enum class DockEdge {
    LEFT,
    RIGHT,
    TOP,     // Acolhimento na borda superior (código comentado abaixo)
    BOTTOM   // Acolhimento na borda inferior (código comentado abaixo)
}

/**
 * Posição calculada para o ícone flutuante.
 * @param isDocked Verdadeiro se o ícone foi acolhido na borda (metade visível), falso se permanece livre na tela.
 */
data class DockPosition(
    val x: Int,
    val y: Int,
    val edge: DockEdge?,
    val isDocked: Boolean = edge != null
)

/**
 * Calculador puro de lógica matemática de ancoragem e acolhimento do ícone flutuante.
 * 
 * Atualmente ativo:
 * - Acolhimento exclusivo nas bordas laterais (Esquerda e Direita) com 50% visível.
 * - Bordas Superior e Inferior preservadas como código comentado no arquivo para fácil reativação futura.
 * - Totalmente desacoplado do Android WindowManager para garantir 100% de testabilidade unitária.
 */
object FloatingDockCalculator {

    /**
     * Calcula a posição inicial padrão do ícone flutuante:
     * Centralizado horizontalmente na tela e 100% visível na área segura acima da barra de navegação/gestos.
     */
    fun calculateInitialPosition(
        bubbleWidth: Int,
        bubbleHeight: Int,
        screenWidth: Int,
        screenHeight: Int,
        bottomInset: Int = 0,
        bottomPaddingPx: Int = 40,
        topInset: Int = 0,
        leftInset: Int = 0,
        rightInset: Int = 0
    ): Pair<Int, Int> {
        val fabW = if (bubbleWidth > 0) bubbleWidth else 160
        val fabH = if (bubbleHeight > 0) bubbleHeight else 160

        val safeLeft = leftInset.coerceAtLeast(0)
        val safeRight = (screenWidth - rightInset).coerceAtLeast(safeLeft + fabW)
        val safeTop = topInset.coerceAtLeast(0)
        val safeBottom = (screenHeight - bottomInset).coerceAtLeast(safeTop + fabH)

        val minUsableX = safeLeft
        val maxUsableX = (safeRight - fabW).coerceAtLeast(minUsableX)
        val minUsableY = safeTop
        val maxUsableY = (safeBottom - fabH).coerceAtLeast(minUsableY)

        val initialX = (safeLeft + (safeRight - safeLeft - fabW) / 2).coerceIn(minUsableX, maxUsableX)
        val initialY = (safeBottom - fabH - bottomPaddingPx).coerceIn(minUsableY, maxUsableY)

        return Pair(initialX, initialY)
    }

    /**
     * Verifica se o ícone está acolhido nas bordas da tela.
     */
    fun isPositionDocked(
        currentX: Int,
        currentY: Int,
        bubbleWidth: Int,
        bubbleHeight: Int,
        screenWidth: Int,
        screenHeight: Int,
        topInset: Int = 0,
        bottomInset: Int = 0,
        leftInset: Int = 0,
        rightInset: Int = 0
    ): Boolean {
        val fabW = if (bubbleWidth > 0) bubbleWidth else 160
        val maxUsableX = screenWidth - fabW

        // ATIVO: Acolhimento apenas nas bordas laterais (Esquerda e Direita)
        val isDockedHorizontally = currentX < 0 || currentX > maxUsableX

        /* 
        // [CÓDIGO COMENTADO - ACOLHIMENTO NAS BORDAS SUPERIOR E INFERIOR]
        // Para reativar acolhimento superior e inferior, descomente as linhas abaixo:
        val fabH = if (bubbleHeight > 0) bubbleHeight else 160
        val maxUsableY = screenHeight - fabH
        val isDockedVertically = currentY < 0 || currentY > maxUsableY
        return isDockedHorizontally || isDockedVertically
        */

        return isDockedHorizontally
    }

    /**
     * Ao sofrer pressão contínua (> 2s) em estado acolhido, move o ícone para ficar
     * 100% visível na área segura da tela para permitir arraste livre.
     */
    fun undockPosition(
        currentX: Int,
        currentY: Int,
        bubbleWidth: Int,
        bubbleHeight: Int,
        screenWidth: Int,
        screenHeight: Int,
        marginPx: Int = 20,
        topInset: Int = 0,
        bottomInset: Int = 0,
        leftInset: Int = 0,
        rightInset: Int = 0
    ): Pair<Int, Int> {
        val fabW = if (bubbleWidth > 0) bubbleWidth else 160
        val fabH = if (bubbleHeight > 0) bubbleHeight else 160

        val safeLeft = leftInset.coerceAtLeast(0)
        val safeRight = (screenWidth - rightInset).coerceAtLeast(safeLeft + fabW)
        val safeTop = topInset.coerceAtLeast(0)
        val safeBottom = (screenHeight - bottomInset).coerceAtLeast(safeTop + fabH)

        val minUsableX = safeLeft
        val maxUsableX = (safeRight - fabW).coerceAtLeast(minUsableX)
        val minUsableY = safeTop
        val maxUsableY = (safeBottom - fabH).coerceAtLeast(minUsableY)

        val targetX = when {
            currentX < minUsableX -> safeLeft + marginPx
            currentX > maxUsableX -> maxUsableX - marginPx
            else -> currentX.coerceIn(minUsableX, maxUsableX)
        }

        /*
        // [CÓDIGO COMENTADO - DESACOLHIMENTO DAS BORDAS SUPERIOR E INFERIOR]
        // Para reativar o desacolhimento vertical completo, substitua o targetY abaixo por:
        val targetY = when {
            currentY < minUsableY -> safeTop + marginPx
            currentY > maxUsableY -> maxUsableY - marginPx
            else -> currentY.coerceIn(minUsableY, maxUsableY)
        }
        */
        val targetY = currentY.coerceIn(minUsableY, maxUsableY)

        return Pair(targetX, targetY)
    }

    /**
     * Calcula a menor distância da posição atual do ícone para as bordas da tela,
     * posicionando o ícone na borda com exatamente 50% visível na tela e 50% além da borda.
     */
    fun calculateNearestEdge(
        currentX: Int,
        currentY: Int,
        bubbleWidth: Int,
        bubbleHeight: Int,
        screenWidth: Int,
        screenHeight: Int,
        visibleFraction: Float = 0.5f,
        topInset: Int = 0,
        bottomInset: Int = 0,
        leftInset: Int = 0,
        rightInset: Int = 0
    ): DockPosition {
        val fabW = if (bubbleWidth > 0) bubbleWidth else 160
        val fabH = if (bubbleHeight > 0) bubbleHeight else 160

        val safeTop = topInset.coerceAtLeast(0)
        val safeBottom = (screenHeight - bottomInset).coerceAtLeast(safeTop + fabH)
        val minUsableY = safeTop
        val maxUsableY = (safeBottom - fabH).coerceAtLeast(minUsableY)

        val hiddenWidth = (fabW * (1f - visibleFraction)).toInt()
        val visibleWidth = (fabW * visibleFraction).toInt()

        // 1. Borda Esquerda (LEFT): 50% visível na tela (x = -hiddenWidth)
        val leftTargetX = -hiddenWidth
        val distLeft = abs(currentX - leftTargetX)

        // 2. Borda Direita (RIGHT): 50% visível na tela (x = screenWidth - visibleWidth)
        val rightTargetX = screenWidth - visibleWidth
        val distRight = abs(currentX - rightTargetX)

        /*
        // [CÓDIGO COMENTADO - CÁLCULO DE ACOLHIMENTO SUPERIOR E INFERIOR]
        // Para reativar acolhimento em todas as 4 bordas, descomente este bloco:
        val maxUsableX = (screenWidth - fabW).coerceAtLeast(0)
        val hiddenHeight = (fabH * (1f - visibleFraction)).toInt()
        val visibleHeight = (fabH * visibleFraction).toInt()
        
        // 3. Borda Superior (TOP): 50% visível no topo (y = -hiddenHeight)
        val topTargetX = currentX.coerceIn(0, maxUsableX)
        val topTargetY = -hiddenHeight
        val distTop = abs(currentY - topTargetY)

        // 4. Borda Inferior (BOTTOM): 50% visível na base (y = screenHeight - visibleHeight)
        val bottomTargetX = currentX.coerceIn(0, maxUsableX)
        val bottomTargetY = screenHeight - visibleHeight
        val distBottom = abs(currentY - bottomTargetY)

        val minDist = minOf(distLeft, distRight, distTop, distBottom)
        return when (minDist) {
            distLeft -> DockPosition(leftTargetX, currentY.coerceIn(0, maxUsableY), DockEdge.LEFT)
            distRight -> DockPosition(rightTargetX, currentY.coerceIn(0, maxUsableY), DockEdge.RIGHT)
            distTop -> DockPosition(topTargetX, topTargetY, DockEdge.TOP)
            else -> DockPosition(bottomTargetX, bottomTargetY, DockEdge.BOTTOM)
        }
        */

        val targetY = currentY.coerceIn(minUsableY, maxUsableY)

        return if (distLeft <= distRight) {
            DockPosition(leftTargetX, targetY, DockEdge.LEFT)
        } else {
            DockPosition(rightTargetX, targetY, DockEdge.RIGHT)
        }
    }

    /**
     * Resolve a posição ao soltar o ícone após o arrasto:
     * - Se for arrastado contra as bordas laterais (dentro do limiar de acolhimento),
     *   é acolhido de imediato naquela lateral com exatamente metade visível na tela (50%).
     * - Se for solto no espaço livre, permanece livre onde foi deixado.
     *
     * @param thresholdPx Distância em pixels considerada como "pressionado contra a borda lateral" (padrão: 1/3 do ícone).
     */
    fun resolveReleasePosition(
        currentX: Int,
        currentY: Int,
        bubbleWidth: Int,
        bubbleHeight: Int,
        screenWidth: Int,
        screenHeight: Int,
        thresholdPx: Int? = null,
        visibleFraction: Float = 0.5f,
        topInset: Int = 0,
        bottomInset: Int = 0,
        leftInset: Int = 0,
        rightInset: Int = 0
    ): DockPosition {
        val fabW = if (bubbleWidth > 0) bubbleWidth else 160
        val fabH = if (bubbleHeight > 0) bubbleHeight else 160
        val snapMargin = thresholdPx ?: (fabW / 3).coerceAtLeast(40)

        val safeTop = topInset.coerceAtLeast(0)
        val safeBottom = (screenHeight - bottomInset).coerceAtLeast(safeTop + fabH)
        val minUsableY = safeTop
        val maxUsableY = (safeBottom - fabH).coerceAtLeast(minUsableY)

        val maxUsableX = (screenWidth - fabW).coerceAtLeast(0)

        val hiddenWidth = (fabW * (1f - visibleFraction)).toInt()
        val visibleWidth = (fabW * visibleFraction).toInt()

        // Distâncias reais até as bordas laterais
        val distToLeftBorder = currentX
        val distToRightBorder = screenWidth - (currentX + fabW)

        val isNearLeft = distToLeftBorder <= snapMargin
        val isNearRight = distToRightBorder <= snapMargin

        /*
        // [CÓDIGO COMENTADO - DETECÇÃO DE ARRASTO CONTRA BORDAS SUPERIOR E INFERIOR]
        // Para reativar o acolhimento imediato nas bordas superior e inferior, descomente este bloco:
        val hiddenHeight = (fabH * (1f - visibleFraction)).toInt()
        val visibleHeight = (fabH * visibleFraction).toInt()
        val distToTopBorder = currentY
        val distToBottomBorder = screenHeight - (currentY + fabH)
        val isNearTop = distToTopBorder <= snapMargin
        val isNearBottom = distToBottomBorder <= snapMargin

        if (isNearLeft || isNearRight || isNearTop || isNearBottom) {
            val candidateDistances = mutableListOf<Pair<DockEdge, Int>>()
            if (isNearLeft) candidateDistances.add(DockEdge.LEFT to distToLeftBorder)
            if (isNearRight) candidateDistances.add(DockEdge.RIGHT to distToRightBorder)
            if (isNearTop) candidateDistances.add(DockEdge.TOP to distToTopBorder)
            if (isNearBottom) candidateDistances.add(DockEdge.BOTTOM to distToBottomBorder)

            val closestEdge = candidateDistances.minByOrNull { it.second }?.first ?: DockEdge.RIGHT
            return when (closestEdge) {
                DockEdge.LEFT -> DockPosition(-hiddenWidth, currentY.coerceIn(0, maxUsableY), DockEdge.LEFT)
                DockEdge.RIGHT -> DockPosition(screenWidth - visibleWidth, currentY.coerceIn(0, maxUsableY), DockEdge.RIGHT)
                DockEdge.TOP -> DockPosition(currentX.coerceIn(0, maxUsableX), -hiddenHeight, DockEdge.TOP)
                DockEdge.BOTTOM -> DockPosition(currentX.coerceIn(0, maxUsableX), screenHeight - visibleHeight, DockEdge.BOTTOM)
            }
        }
        */

        val targetY = currentY.coerceIn(minUsableY, maxUsableY)

        // Se foi pressionado contra a borda lateral esquerda ou direita, acolhe na lateral mais próxima
        if (isNearLeft || isNearRight) {
            return if (distToLeftBorder <= distToRightBorder) {
                DockPosition(-hiddenWidth, targetY, DockEdge.LEFT)
            } else {
                DockPosition(screenWidth - visibleWidth, targetY, DockEdge.RIGHT)
            }
        }

        // Solto no espaço livre: permanece na posição exata dentro dos limites da tela
        return DockPosition(
            x = currentX.coerceIn(0, maxUsableX),
            y = targetY,
            edge = null
        )
    }

    /**
     * Limita o movimento do ícone durante o arrasto manual:
     * - Eixo horizontal: permite arrastar até metade do ícone para fora nas laterais (esquerda/direita)
     *   para facilitar o gesto de acolhimento.
     * - Eixo vertical: limitado à área visível e segura da tela (respeitando insets de status bar e nav bar).
     */
    fun clampDragPosition(
        newX: Int,
        newY: Int,
        bubbleWidth: Int,
        bubbleHeight: Int,
        screenWidth: Int,
        screenHeight: Int,
        topInset: Int = 0,
        bottomInset: Int = 0,
        leftInset: Int = 0,
        rightInset: Int = 0
    ): Pair<Int, Int> {
        val fabW = if (bubbleWidth > 0) bubbleWidth else 160
        val fabH = if (bubbleHeight > 0) bubbleHeight else 160

        val minX = -fabW / 2
        val maxX = screenWidth - fabW / 2

        /*
        // [CÓDIGO COMENTADO - ARRASTO ATÉ METADE PARA FORA NO TOPO E BASE]
        // Para reativar o arrasto livre até 50% fora nas bordas superior e inferior, descomente:
        // val minY = -fabH / 2
        // val maxY = screenHeight - fabH / 2
        */

        val safeTop = topInset.coerceAtLeast(0)
        val safeBottom = (screenHeight - bottomInset).coerceAtLeast(safeTop + fabH)
        val minY = safeTop
        val maxY = (safeBottom - fabH).coerceAtLeast(minY)

        return Pair(
            newX.coerceIn(minX, maxX),
            newY.coerceIn(minY, maxY)
        )
    }
}
