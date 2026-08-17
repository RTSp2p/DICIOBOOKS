package com.example.service.floating

import kotlin.math.abs

/**
 * Representa a borda para onde o ícone flutuante foi acolhido.
 */
enum class DockEdge {
    LEFT,
    RIGHT,
    TOP,
    BOTTOM
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
 * Calculador puro de lógica matemática de ancoragem, bordas e posicionamento para o ícone flutuante.
 * Totalmente desacoplado do Android WindowManager para garantir 100% de testabilidade unitária.
 */
object FloatingDockCalculator {

    /**
     * Calcula a posição inicial padrão do ícone flutuante:
     * Centralizado horizontalmente na borda inferior e 100% visível (com margem segura).
     */
    fun calculateInitialPosition(
        bubbleWidth: Int,
        bubbleHeight: Int,
        screenWidth: Int,
        screenHeight: Int,
        bottomInset: Int = 0,
        bottomPaddingPx: Int = 40
    ): Pair<Int, Int> {
        val fabW = if (bubbleWidth > 0) bubbleWidth else 160
        val fabH = if (bubbleHeight > 0) bubbleHeight else 160

        val initialX = ((screenWidth - fabW) / 2).coerceAtLeast(0)
        val usableBottom = screenHeight - bottomInset
        val initialY = (usableBottom - fabH - bottomPaddingPx).coerceAtLeast(0)

        return Pair(initialX, initialY)
    }

    /**
     * Calcula a menor distância da posição atual do ícone para as 4 bordas da tela
     * (Esquerda, Direita, Topo e Inferior), posicionando o ícone na borda mais próxima
     * com metade dele visível (ou a fração especificada), respeitando os insets do sistema.
     */
    fun calculateNearestEdge(
        currentX: Int,
        currentY: Int,
        bubbleWidth: Int,
        bubbleHeight: Int,
        screenWidth: Int,
        screenHeight: Int,
        topInset: Int = 0,
        bottomInset: Int = 0,
        visibleFraction: Float = 0.5f
    ): DockPosition {
        val fabW = if (bubbleWidth > 0) bubbleWidth else 160
        val fabH = if (bubbleHeight > 0) bubbleHeight else 160

        val usableTop = topInset
        val usableBottom = screenHeight - bottomInset
        val maxUsableX = (screenWidth - fabW).coerceAtLeast(0)
        val maxUsableY = (usableBottom - fabH).coerceAtLeast(usableTop)

        val hiddenWidth = (fabW * (1f - visibleFraction)).toInt()
        val visibleWidth = (fabW * visibleFraction).toInt()
        val hiddenHeight = (fabH * (1f - visibleFraction)).toInt()
        val visibleHeight = (fabH * visibleFraction).toInt()

        // 1. Borda Esquerda (LEFT): 50% visível na tela
        val leftTargetX = -hiddenWidth
        val leftTargetY = currentY.coerceIn(usableTop, maxUsableY)
        val distLeft = abs(currentX - leftTargetX)

        // 2. Borda Direita (RIGHT): 50% visível na tela
        val rightTargetX = screenWidth - visibleWidth
        val rightTargetY = currentY.coerceIn(usableTop, maxUsableY)
        val distRight = abs(currentX - rightTargetX)

        // 3. Borda Superior (TOP): 50% visível no topo
        val topTargetX = currentX.coerceIn(0, maxUsableX)
        val topTargetY = usableTop - hiddenHeight
        val distTop = abs(currentY - topTargetY)

        // 4. Borda Inferior (BOTTOM): exatamente 50% visível acima da barra de navegação/borda inferior
        val bottomTargetX = currentX.coerceIn(0, maxUsableX)
        val bottomTargetY = usableBottom - visibleHeight
        val distBottom = abs(currentY - bottomTargetY)

        val minDist = minOf(distLeft, distRight, distTop, distBottom)

        return when (minDist) {
            distLeft -> DockPosition(leftTargetX, leftTargetY, DockEdge.LEFT)
            distRight -> DockPosition(rightTargetX, rightTargetY, DockEdge.RIGHT)
            distTop -> DockPosition(topTargetX, topTargetY, DockEdge.TOP)
            else -> DockPosition(bottomTargetX, bottomTargetY, DockEdge.BOTTOM)
        }
    }

    /**
     * Resolve a posição ao soltar o ícone após o arrasto:
     * - Se for arrastado contra qualquer uma das 4 bordas (dentro do limiar de acolhimento),
     *   é acolhido de imediato naquela borda com metade visível.
     * - Se for solto em qualquer outro ponto da tela, permanece livre exatamente onde foi deixado.
     *
     * @param thresholdPx Distância em pixels considerada como "pressionado contra a borda" (padrão: 1/3 do ícone).
     */
    fun resolveReleasePosition(
        currentX: Int,
        currentY: Int,
        bubbleWidth: Int,
        bubbleHeight: Int,
        screenWidth: Int,
        screenHeight: Int,
        topInset: Int = 0,
        bottomInset: Int = 0,
        thresholdPx: Int? = null,
        visibleFraction: Float = 0.5f
    ): DockPosition {
        val fabW = if (bubbleWidth > 0) bubbleWidth else 160
        val fabH = if (bubbleHeight > 0) bubbleHeight else 160
        val snapMargin = thresholdPx ?: (fabW / 3).coerceAtLeast(40)

        val usableTop = topInset
        val usableBottom = screenHeight - bottomInset
        val maxUsableX = (screenWidth - fabW).coerceAtLeast(0)
        val maxUsableY = (usableBottom - fabH).coerceAtLeast(usableTop)

        val hiddenWidth = (fabW * (1f - visibleFraction)).toInt()
        val visibleWidth = (fabW * visibleFraction).toInt()
        val hiddenHeight = (fabH * (1f - visibleFraction)).toInt()
        val visibleHeight = (fabH * visibleFraction).toInt()

        // Distâncias reais até as 4 bordas úteis da tela
        val distToLeftBorder = currentX
        val distToRightBorder = screenWidth - (currentX + fabW)
        val distToTopBorder = currentY - usableTop
        val distToBottomBorder = usableBottom - (currentY + fabH)

        val isNearLeft = distToLeftBorder <= snapMargin
        val isNearRight = distToRightBorder <= snapMargin
        val isNearTop = distToTopBorder <= snapMargin
        val isNearBottom = distToBottomBorder <= snapMargin

        // Se foi pressionado contra alguma borda, acolhe na mais próxima com 50% visível
        if (isNearLeft || isNearRight || isNearTop || isNearBottom) {
            val candidateDistances = mutableListOf<Pair<DockEdge, Int>>()
            if (isNearLeft) candidateDistances.add(DockEdge.LEFT to distToLeftBorder)
            if (isNearRight) candidateDistances.add(DockEdge.RIGHT to distToRightBorder)
            if (isNearTop) candidateDistances.add(DockEdge.TOP to distToTopBorder)
            if (isNearBottom) candidateDistances.add(DockEdge.BOTTOM to distToBottomBorder)

            val closestEdge = candidateDistances.minByOrNull { it.second }?.first ?: DockEdge.RIGHT

            return when (closestEdge) {
                DockEdge.LEFT -> DockPosition(-hiddenWidth, currentY.coerceIn(usableTop, maxUsableY), DockEdge.LEFT)
                DockEdge.RIGHT -> DockPosition(screenWidth - visibleWidth, currentY.coerceIn(usableTop, maxUsableY), DockEdge.RIGHT)
                DockEdge.TOP -> DockPosition(currentX.coerceIn(0, maxUsableX), usableTop - hiddenHeight, DockEdge.TOP)
                DockEdge.BOTTOM -> DockPosition(currentX.coerceIn(0, maxUsableX), usableBottom - visibleHeight, DockEdge.BOTTOM)
            }
        }

        // Solto no espaço livre: permanece na posição exata (limitada dentro da área visível da tela)
        return DockPosition(
            x = currentX.coerceIn(0, maxUsableX),
            y = currentY.coerceIn(usableTop, maxUsableY),
            edge = null
        )
    }

    /**
     * Limita o movimento do ícone durante o arrasto manual para não se perder além dos limites da tela.
     */
    fun clampDragPosition(
        newX: Int,
        newY: Int,
        bubbleWidth: Int,
        bubbleHeight: Int,
        screenWidth: Int,
        screenHeight: Int,
        topInset: Int = 0,
        bottomInset: Int = 0
    ): Pair<Int, Int> {
        val fabW = if (bubbleWidth > 0) bubbleWidth else 160
        val fabH = if (bubbleHeight > 0) bubbleHeight else 160

        val usableTop = topInset
        val usableBottom = screenHeight - bottomInset

        val minX = -fabW / 2
        val maxX = screenWidth - fabW / 2
        val minY = usableTop - fabH / 2
        val maxY = usableBottom - fabH / 2

        return Pair(
            newX.coerceIn(minX, maxX),
            newY.coerceIn(minY, maxY)
        )
    }
}
