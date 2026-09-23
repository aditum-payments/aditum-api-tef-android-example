@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package br.com.aditum.paytrial.designsystem

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter

// ---------------------------------------------------------------- estrutura

/**
 * Tela do Aditum Pay Trial: barra superior com o título simples à esquerda (sem pílula, para não parecer a tela da
 * Aditum), Voltar ou Fechar, ações à direita e rodapé fixo. O conteúdo rola quando [scroll] é verdadeiro.
 */
/** Ação fixa da barra do topo em qualquer tela (o chip do inspetor), fornecida pela raiz do app. */
val LocalTopBarExtra = compositionLocalOf<(@Composable RowScope.() -> Unit)?> { null }

@Composable
fun PtScaffold(
    title: String, onBack: (() -> Unit)? = null, onClose: (() -> Unit)? = null, actions: @Composable RowScope.() -> Unit = {},
    bottomBar: @Composable (() -> Unit)? = null, snackbarHost: SnackbarHostState? = null, scroll: Boolean = true,
    floating: @Composable (BoxScope.() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    when {
                        onBack != null -> IconButton(onClick = onBack) { PtIcon(PtIcons.Back, "Voltar") }
                        onClose != null -> IconButton(onClick = onClose) { PtIcon(PtIcons.Close, "Fechar") }
                    }
                },
                actions = { LocalTopBarExtra.current?.invoke(this); actions() },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
        bottomBar = { if (bottomBar != null) Surface(color = MaterialTheme.colorScheme.surface) { Box(Modifier.navigationBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp)) { bottomBar() } } },
        snackbarHost = { if (snackbarHost != null) SnackbarHost(snackbarHost) { PtSnackbar(it.visuals.message) } },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            val base = Modifier.fillMaxSize().padding(horizontal = 16.dp)
            Column(if (scroll) base.verticalScroll(rememberScrollState()) else base, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Spacer(Modifier.height(4.dp)); content(); Spacer(Modifier.height(if (floating != null) 88.dp else 16.dp))
            }
            floating?.invoke(this)
        }
    }
}

@Composable
fun PtIcon(icon: ImageVector, contentDescription: String? = null, modifier: Modifier = Modifier, tint: Color = LocalContentColor.current, size: Dp = 24.dp) =
    Icon(icon, contentDescription, modifier.size(size), tint = tint)

/** Sobretítulo de seção: 11 sp, caixa alta, espaçado. */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) =
    Text(text.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = modifier.padding(top = 4.dp))

// ---------------------------------------------------------------- botões

enum class PtButtonKind { Primary, Tonal, Outlined, Text, Danger }

/** Botão do design system: 48 dp de altura, cantos de 6 dp, ícone opcional à esquerda. */
@Composable
fun PtButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, kind: PtButtonKind = PtButtonKind.Primary, icon: ImageVector? = null, enabled: Boolean = true) {
    val content: @Composable RowScope.() -> Unit = {
        if (icon != null) { PtIcon(icon, size = 20.dp); Spacer(Modifier.width(8.dp)) }
        Text(text, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
    val m = modifier.heightIn(min = 48.dp)
    val shape = MaterialTheme.shapes.medium
    when (kind) {
        PtButtonKind.Primary -> Button(onClick, m, enabled, shape = shape, content = content)
        PtButtonKind.Tonal -> FilledTonalButton(onClick, m, enabled, shape = shape,
            colors = ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer), content = content)
        PtButtonKind.Outlined -> OutlinedButton(onClick, m, enabled, shape = shape, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline), content = content)
        PtButtonKind.Text -> TextButton(onClick, m, enabled, shape = shape, content = content)
        PtButtonKind.Danger -> Button(onClick, m, enabled, shape = shape,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error, contentColor = MaterialTheme.colorScheme.onError), content = content)
    }
}

/** Rodapé com uma ação secundária (esquerda) e a principal (direita), dividindo a largura. */
@Composable
fun ActionRow(primary: @Composable RowScope.() -> Unit, secondary: (@Composable RowScope.() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) { secondary?.invoke(this); primary() }
}

// ---------------------------------------------------------------- campos e controles

/** Campo de texto do design system: rótulo, ajuda ou erro embaixo, teclado pelo tipo. */
@Composable
fun PtField(
    value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier = Modifier, supporting: String? = null, error: String? = null,
    keyboard: KeyboardType = KeyboardType.Text, secret: Boolean = false, enabled: Boolean = true, singleLine: Boolean = true, placeholder: String? = null,
) {
    OutlinedTextField(
        value = value, onValueChange = onValueChange, modifier = modifier.fillMaxWidth(), label = { Text(label) }, enabled = enabled, singleLine = singleLine,
        isError = error != null, shape = MaterialTheme.shapes.small, placeholder = placeholder?.let { { Text(it) } },
        supportingText = (error ?: supporting)?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(keyboardType = keyboard),
        visualTransformation = if (secret) PasswordVisualTransformation() else VisualTransformation.None,
    )
}

/** Linha com chave liga/desliga: título, detalhe e o estado; desabilitada explica o motivo no detalhe. */
@Composable
fun SwitchRow(title: String, checked: Boolean, onChange: (Boolean) -> Unit, detail: String? = null, enabled: Boolean = true) {
    Row(Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).clickable(enabled = enabled, role = Role.Switch) { onChange(!checked) }.padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
            if (detail != null) Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChange, enabled = enabled)
    }
}

/** Botões divididos com ícone (as vias Loja | Cliente | Ambas): a escolhida fica no tom da marca. */
@Composable
fun <T> SplitChoice(options: List<Triple<T, String, ImageVector>>, selected: T, onSelect: (T) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        options.forEach { (value, label, icon) ->
            val on = value == selected
            val bg = if (on) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest
            val fg = if (on) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
            Row(Modifier.weight(1f).heightIn(min = 48.dp).clip(MaterialTheme.shapes.medium).background(bg).clickable(role = Role.RadioButton) { onSelect(value) },
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                PtIcon(icon, size = 18.dp, tint = fg)
                Box(Modifier.padding(horizontal = 8.dp).width(1.dp).height(18.dp).background(fg.copy(alpha = 0.35f)))
                Text(label, style = MaterialTheme.typography.labelLarge, color = fg, maxLines = 1)
            }
        }
    }
}

/** Escolha única curta (Venda | Pré-autorização | Captura; NSU | Referência). */
@Composable
fun <T> Segmented(options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit, modifier: Modifier = Modifier) {
    SingleChoiceSegmentedButtonRow(modifier.fillMaxWidth()) {
        options.forEachIndexed { i, (v, label) ->
            SegmentedButton(selected = v == selected, onClick = { onSelect(v) }, shape = SegmentedButtonDefaults.itemShape(i, options.size, MaterialTheme.shapes.medium),
                icon = {}, label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelLarge) })
        }
    }
}

/** Chips de filtro (período, status). */
@Composable
fun <T> ChipRow(options: List<Pair<T, String>>, selected: Set<T>, onToggle: (T) -> Unit, modifier: Modifier = Modifier) {
    FlowRow(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        options.forEach { (v, label) ->
            val on = v in selected
            FilterChip(selected = on, onClick = { onToggle(v) }, label = { Text(label) }, shape = MaterialTheme.shapes.small,
                leadingIcon = if (on) { { PtIcon(PtIcons.Check, size = 16.dp) } } else null)
        }
    }
}

/** Escolha única em chips que quebram linha: para listas longas (todos os valores de um enum do contrato). */
@Composable
fun <T> ChoiceChips(options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit, modifier: Modifier = Modifier) {
    FlowRow(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        options.forEach { (v, label) ->
            val on = v == selected
            FilterChip(selected = on, onClick = { onSelect(v) }, label = { Text(label) }, shape = MaterialTheme.shapes.small,
                leadingIcon = if (on) { { PtIcon(PtIcons.Check, size = 16.dp) } } else null)
        }
    }
}

/** Abas (Resumo | Requisição | Resposta). */
@Composable
fun PtTabs(labels: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    TabRow(selectedTabIndex = selected, containerColor = MaterialTheme.colorScheme.surface) {
        labels.forEachIndexed { i, l -> Tab(selected = i == selected, onClick = { onSelect(i) }, text = { Text(l, style = MaterialTheme.typography.labelLarge) }) }
    }
}

/** Contador com − e + (parcelas, partes). */
@Composable
fun StepperRow(value: Int, min: Int, max: Int, onChange: (Int) -> Unit, caption: String? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
        FilledTonalIconButton(onClick = { onChange(value - 1) }, enabled = value > min, shape = MaterialTheme.shapes.medium) { PtIcon(PtIcons.Minus, "Menos") }
        Text("$value", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.widthIn(min = 56.dp), textAlign = TextAlign.Center)
        FilledTonalIconButton(onClick = { onChange(value + 1) }, enabled = value < max, shape = MaterialTheme.shapes.medium) { PtIcon(PtIcons.Plus, "Mais") }
    }
    if (caption != null) Text(caption, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
}

// ---------------------------------------------------------------- grade, listas e dados

data class Shortcut(val id: String, val label: String, val icon: ImageVector, val highlight: Boolean = false, val badge: Int = 0, val enabled: Boolean = true)

/** Grade de atalhos em 3 colunas; o atalho em destaque ocupa duas. */
@Composable
fun ShortcutGrid(items: List<Shortcut>, onSelect: (Shortcut) -> Unit) {
    val rows = mutableListOf<MutableList<Shortcut>>(); var used = 0
    items.forEach { s -> val w = if (s.highlight) 2 else 1; if (rows.isEmpty() || used + w > 3) { rows.add(mutableListOf()); used = 0 }; rows.last().add(s); used += w }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { s ->
                    val bg = if (s.highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh
                    val fg = if (s.highlight) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                    val ic = if (s.highlight) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
                    Box(Modifier.weight(if (s.highlight) 2f else 1f).height(84.dp).clip(MaterialTheme.shapes.large).background(bg)
                        .clickable(enabled = s.enabled) { onSelect(s) }.padding(8.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            PtIcon(s.icon, tint = ic.copy(alpha = if (s.enabled) 1f else 0.38f), size = 28.dp)
                            Text(s.label, style = MaterialTheme.typography.labelLarge, color = fg.copy(alpha = if (s.enabled) 1f else 0.38f), textAlign = TextAlign.Center, maxLines = 2)
                        }
                        if (s.badge > 0) Text("${s.badge}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onError,
                            modifier = Modifier.align(Alignment.TopEnd).clip(MaterialTheme.shapes.small).background(MaterialTheme.colorScheme.error).padding(horizontal = 6.dp))
                    }
                }
                repeat(3 - row.sumOf { if (it.highlight) 2 else 1 }) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

/**
 * Linha de lista: ícone (ou desenho próprio) numa caixa, título, linhas de detalhe (a última pode ser mono, para o
 * NSU) e um valor ou ação à direita. Tudo centralizado na vertical.
 */
@Composable
fun ListRow(
    title: String, modifier: Modifier = Modifier, icon: ImageVector? = null, leading: (@Composable () -> Unit)? = null, details: List<String> = emptyList(),
    monoDetail: String? = null, value: String? = null, trailing: (@Composable () -> Unit)? = null, onClick: (() -> Unit)? = null,
) {
    Row(modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically) {
        when {
            leading != null -> { leading(); Spacer(Modifier.width(12.dp)) }
            icon != null -> {
                Box(Modifier.size(40.dp).clip(MaterialTheme.shapes.medium).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                    PtIcon(icon, tint = MaterialTheme.colorScheme.onPrimaryContainer, size = 22.dp)
                }
                Spacer(Modifier.width(12.dp))
            }
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            details.filter { it.isNotBlank() }.forEach { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            if (monoDetail != null) Text(monoDetail, style = MonoStyle, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, softWrap = false)
        }
        if (value != null) Text(value, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(start = 8.dp))
        if (trailing != null) { Spacer(Modifier.width(4.dp)); trailing() }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

/** Pares rótulo/valor num cartão (dados da cobrança, do terminal). Valores [mono] em JetBrains Mono. */
@Composable
fun KeyValueCard(rows: List<Pair<String, String>>, mono: Set<String> = emptySet(), modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().clip(MaterialTheme.shapes.large).background(MaterialTheme.colorScheme.surfaceContainerHigh).padding(horizontal = 14.dp, vertical = 8.dp)) {
        rows.forEach { (k, v) ->
            Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(k, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(12.dp))
                Text(v, style = if (k in mono) MonoStyle.copy(fontSize = MaterialTheme.typography.bodyMedium.fontSize) else MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), textAlign = TextAlign.End)
            }
        }
    }
}

// ---------------------------------------------------------------- estado e avisos

enum class Tone { Success, Warning, Error, Info, Neutral }

@Composable
private fun toneColors(t: Tone): Pair<Color, Color> {
    val s = PtTheme.status; val c = MaterialTheme.colorScheme
    return when (t) {
        Tone.Success -> s.successContainer to s.onSuccessContainer
        Tone.Warning -> s.warningContainer to s.onWarningContainer
        Tone.Error -> c.errorContainer to c.onErrorContainer
        Tone.Info -> s.infoContainer to s.onInfoContainer
        Tone.Neutral -> c.primaryContainer to c.onPrimaryContainer
    }
}

/** Etiqueta de estado (Confirmada, Aguardando confirmação, Negada). */
@Composable
fun StatusTag(text: String, tone: Tone, icon: ImageVector? = null) {
    val (bg, fg) = toneColors(tone)
    Row(Modifier.clip(MaterialTheme.shapes.small).background(bg).padding(horizontal = 8.dp, vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        if (icon != null) { PtIcon(icon, tint = fg, size = 14.dp); Spacer(Modifier.width(4.dp)) }
        Text(text, style = MaterialTheme.typography.labelMedium, color = fg)
    }
}

/** Aviso em faixa: ícone, título e detalhe. */
@Composable
fun Banner(title: String, tone: Tone, text: String? = null, icon: ImageVector = when (tone) { Tone.Success -> PtIcons.Success; Tone.Warning -> PtIcons.Warning; Tone.Error -> PtIcons.Error; else -> PtIcons.Info }) {
    val (bg, fg) = toneColors(tone)
    Row(Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).background(bg).padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        PtIcon(icon, tint = fg, size = 20.dp)
        Column { Text(title, style = MaterialTheme.typography.titleSmall, color = fg); if (text != null) Text(text, style = MaterialTheme.typography.bodySmall, color = fg) }
    }
}

/** Aviso passageiro em cor invertida ao tema. */
@Composable
fun PtSnackbar(message: String) {
    Row(Modifier.padding(12.dp).fillMaxWidth().clip(MaterialTheme.shapes.medium).background(MaterialTheme.colorScheme.inverseSurface).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        PtIcon(PtIcons.Info, tint = MaterialTheme.colorScheme.inversePrimary, size = 18.dp)
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.inverseOnSurface)
    }
}

@Composable
fun LoadingBlock(text: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = 32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        CircularProgressIndicator(); Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun EmptyBlock(text: String, icon: ImageVector = PtIcons.Info) {
    Column(Modifier.fillMaxWidth().padding(vertical = 32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        PtIcon(icon, tint = MaterialTheme.colorScheme.outline, size = 40.dp); Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}

// ---------------------------------------------------------------- operação

data class TimelineStep(val title: String, val detail: String? = null, val current: Boolean = false)

/** Linha do tempo da operação: um passo por notificação; o atual em destaque. */
@Composable
fun Timeline(steps: List<TimelineStep>) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        steps.forEach { s ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.padding(top = 6.dp).size(10.dp).clip(MaterialTheme.shapes.extraSmall).background(if (s.current) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline))
                Column {
                    Text(s.title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = if (s.current) FontWeight.Bold else FontWeight.Normal),
                        color = if (s.current) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
                    if (s.detail != null) Text(s.detail, style = MonoStyle.copy(fontSize = MaterialTheme.typography.labelSmall.fontSize), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

/** Valor grande com rótulo; erro de limite embaixo. */
@Composable
fun AmountDisplay(amountCents: Long, label: String = "Valor", error: String? = null) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(Money.brl(amountCents), style = MaterialTheme.typography.displaySmall)
        if (error != null) Text(error, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
    }
}

/** Teclado numérico do valor: 1–9, 00, 0 e apagar (toque longo zera). */
@Composable
fun AmountKeypad(onDigit: (String) -> Unit, onBackspace: () -> Unit, onClear: () -> Unit) {
    val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "00", "0", "⌫")
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        keys.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { k ->
                    Box(Modifier.weight(1f).height(54.dp).clip(MaterialTheme.shapes.medium).background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .clickable { if (k == "⌫") onBackspace() else onDigit(k) }, contentAlignment = Alignment.Center) {
                        if (k == "⌫") PtIcon(PtIcons.Backspace, "Apagar") else Text(k, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold))
                    }
                }
            }
        }
        TextButton(onClick = onClear, modifier = Modifier.align(Alignment.End)) { Text("Limpar") }
    }
}

// ---------------------------------------------------------------- sobreposições

/** Folha inferior com título, conteúdo e ações fixas embaixo (PIN, QR, dado digitado, menu, envio). */
@Composable
fun PtSheet(title: String, onDismiss: () -> Unit, actions: (@Composable RowScope.() -> Unit)? = null, dismissible: Boolean = true, content: @Composable ColumnScope.() -> Unit) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true, confirmValueChange = { dismissible || it != SheetValue.Hidden })
    ModalBottomSheet(onDismissRequest = { if (dismissible) onDismiss() }, sheetState = state, shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 16.dp).navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) { content() }
            if (actions != null) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), content = actions)
        }
    }
}

/** Diálogo de confirmação; [destructive] pinta a ação no vermelho do erro. */
@Composable
fun PtDialog(title: String, text: String, confirm: String, onConfirm: () -> Unit, onDismiss: () -> Unit, destructive: Boolean = false, dismiss: String = "Voltar") {
    AlertDialog(onDismissRequest = onDismiss, shape = MaterialTheme.shapes.extraLarge,
        icon = if (destructive) { { PtIcon(PtIcons.Warning, tint = MaterialTheme.colorScheme.error) } } else null,
        title = { Text(title, style = MaterialTheme.typography.titleLarge) }, text = { Text(text, style = MaterialTheme.typography.bodyMedium) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirm, color = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(dismiss) } })
}

// ---------------------------------------------------------------- dados crus, comprovante, QR

/** Bloco de JSON com cores de sintaxe (chave, texto, número) e o que foi censurado em vermelho. Selecionável. */
@Composable
fun JsonBlock(json: String, modifier: Modifier = Modifier) {
    val key = Color(0xFFCDBDFC); val str = Color(0xFF9FE3B4); val num = Color(0xFFFFD28A); val red = Color(0xFFFFB4AB)
    val text = remember(json) {
        buildAnnotatedString {
            val re = Regex("\"(?:[^\"\\\\]|\\\\.)*\"(\\s*:)?|-?\\d+(?:\\.\\d+)?|true|false|null")
            var i = 0
            for (m in re.findAll(json)) {
                append(json.substring(i, m.range.first))
                val v = m.value
                val color = when { m.groupValues[1].isNotEmpty() -> key; v.startsWith("\"") && v.contains('•') -> red; v.startsWith("\"") -> str; else -> num }
                pushStyle(SpanStyle(color = color)); append(v); pop(); i = m.range.last + 1
            }
            append(json.substring(i))
        }
    }
    SelectionContainer {
        Box(modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).background(Color(0xFF17151C)).horizontalScroll(rememberScrollState()).padding(12.dp)) {
            Text(text, style = MonoStyle.copy(fontSize = MaterialTheme.typography.labelMedium.fontSize), color = Color(0xFFE9E4F5), softWrap = false)
        }
    }
}

/** Comprovante na largura do papel (32 colunas), sempre em papel branco. */
@Composable
fun ReceiptView(lines: List<String>, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().clip(MaterialTheme.shapes.small).background(Color.White).border(1.dp, Color(0xFFE3DFE8), MaterialTheme.shapes.small).horizontalScroll(rememberScrollState()).padding(12.dp)) {
        Text(lines.joinToString("\n"), style = MonoStyle, color = Color(0xFF222222), softWrap = false)
    }
}

/** QR Code desenhado de verdade (zxing), com margem branca. */
@Composable
fun QrCode(content: String, size: Dp = 220.dp) {
    val bitmap = remember(content) {
        runCatching {
            val px = 512
            val m = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, px, px, mapOf(EncodeHintType.MARGIN to 1))
            Bitmap.createBitmap(px, px, Bitmap.Config.RGB_565).apply { for (x in 0 until px) for (y in 0 until px) setPixel(x, y, if (m[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE) }.asImageBitmap()
        }.getOrNull()
    }
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        if (bitmap != null) Image(bitmap, "QR Code", Modifier.size(size).clip(MaterialTheme.shapes.small).background(Color.White).padding(8.dp))
        else Text("QR Code inválido", color = MaterialTheme.colorScheme.error)
    }
}

/** Chip do inspetor (modo de integração): a última chamada da tela; tocar abre o par requisição/resposta. */
@Composable
fun InspectorChip(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Row(modifier.clip(MaterialTheme.shapes.medium).background(Color(0xFF1E1B26)).clickable(onClick = onClick).padding(horizontal = 8.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        PtIcon(PtIcons.Json, tint = Color(0xFFCDBDFC), size = 16.dp)
        Text(text, style = MonoStyle.copy(fontSize = MaterialTheme.typography.labelSmall.fontSize), color = Color(0xFFE9E4F5), maxLines = 1)
    }
}

/** Placa branca com o nome da bandeira (cor de destaque para as mais comuns). */
@Composable
fun BrandPlate(name: String) {
    val color = when (name.uppercase()) { "VISA" -> Color(0xFF1A1F71); "MASTERCARD", "MAESTRO" -> Color(0xFFC2410C); "ELO" -> Color(0xFF111111); "AMEX" -> Color(0xFF2E77BC); else -> Color(0xFF333333) }
    Box(Modifier.widthIn(min = 52.dp).clip(MaterialTheme.shapes.small).background(Color.White).border(1.dp, Color(0xFFE3DFE8), MaterialTheme.shapes.small).padding(horizontal = 6.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center) {
        Text(name.uppercase().take(10), style = MaterialTheme.typography.labelSmall.copy(letterSpacing = MaterialTheme.typography.labelSmall.letterSpacing), color = color, maxLines = 1)
    }
}
