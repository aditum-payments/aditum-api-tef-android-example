package br.com.aditum.paytrial.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
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
val WidthClass.shortcutColumns: Int get() = when (this) { WidthClass.Compact -> 3; else -> 4 }

/** Colunas da grade de formas de pagamento. */
val WidthClass.optionColumns: Int get() = when (this) { WidthClass.Compact -> 3; else -> 4 }

/** Largura máxima de uma tela em duas colunas ([TwoPane]) no expandido. */
val WideContentMaxWidth: Dp = 1240.dp

/** Mede a janela e entrega a faixa a todas as telas. */
@Composable
fun ProvideWidthClass(content: @Composable () -> Unit) {
    BoxWithConstraints { CompositionLocalProvider(LocalWidthClass provides widthClassOf(maxWidth)) { content() } }
}

/**
 * Duas colunas em tela larga (PDV com tela de tablet, a partir de 840 dp): [first] à esquerda e [second] à direita, em
 * vez de esticar uma coluna só. Abaixo disso, uma embaixo da outra, como no celular. Use em tela com `PtScaffold(wide = true)`.
 */
@Composable
fun ColumnScope.TwoPane(first: @Composable ColumnScope.() -> Unit, second: @Composable ColumnScope.() -> Unit, firstWeight: Float = 1f, secondWeight: Float = 1f) {
    if (LocalWidthClass.current == WidthClass.Expanded) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
        Column(Modifier.weight(firstWeight), verticalArrangement = Arrangement.spacedBy(12.dp), content = first)
        Column(Modifier.weight(secondWeight), verticalArrangement = Arrangement.spacedBy(12.dp), content = second)
    } else { first(); second() }
}

