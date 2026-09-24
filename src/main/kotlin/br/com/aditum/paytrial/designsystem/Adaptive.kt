package br.com.aditum.paytrial.designsystem

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Faixa de largura da janela, nos cortes do Material 3: o app roda em POS com tela de celular (P2, GPOS) e em PDV Android
 * com tela de tablet. No compacto tudo ocupa a largura; do médio em diante o conteúdo fica numa coluna centralizada, e as
 * grades ganham colunas.
 */
enum class WidthClass { Compact, Medium, Expanded }

val LocalWidthClass = staticCompositionLocalOf { WidthClass.Compact }

fun widthClassOf(width: Dp): WidthClass = when { width < 600.dp -> WidthClass.Compact; width < 840.dp -> WidthClass.Medium; else -> WidthClass.Expanded }

/** Largura máxima da coluna de conteúdo (formulários, listas, resultado): em tablet, não estica de lado a lado. */
val WidthClass.contentMaxWidth: Dp get() = when (this) { WidthClass.Compact -> Dp.Infinity; WidthClass.Medium -> 640.dp; WidthClass.Expanded -> 760.dp }

/** Colunas da grade de atalhos da tela inicial (o destaque ocupa duas). */
val WidthClass.shortcutColumns: Int get() = when (this) { WidthClass.Compact -> 3; WidthClass.Medium -> 4; WidthClass.Expanded -> 6 }

/** Colunas da grade de formas de pagamento. */
val WidthClass.optionColumns: Int get() = when (this) { WidthClass.Compact -> 3; WidthClass.Medium -> 4; WidthClass.Expanded -> 5 }

/** Mede a janela e entrega a faixa a todas as telas. */
@Composable
fun ProvideWidthClass(content: @Composable () -> Unit) {
    BoxWithConstraints { CompositionLocalProvider(LocalWidthClass provides widthClassOf(maxWidth)) { content() } }
}
