package br.com.aditum.paytrial.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import br.com.aditum.paytrial.app.Labels
import br.com.aditum.paytrial.app.MainViewModel
import br.com.aditum.paytrial.app.Screen
import br.com.aditum.paytrial.client.Prompt
import br.com.aditum.paytrial.client.TraceEntry
import br.com.aditum.paytrial.designsystem.*
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject

/** Raiz do Aditum Pay Trial: a tela do topo da pilha e o que fica por cima de qualquer tela. */
@Composable
fun PayTrialRoot(vm: MainViewModel) {
    val settings by vm.settings.state.collectAsState()
    val prompt by vm.client.prompt.collectAsState()
    val snack = remember { SnackbarHostState() }
    LaunchedEffect(Unit) { vm.messages.collect { snack.currentSnackbarData?.dismiss(); snack.showSnackbar(it) } }
    var rawId by remember { mutableStateOf<Long?>(null) }
    var rawTab by remember { mutableIntStateOf(1) }

    // Voltar do sistema: na operação, interrompe; no resultado, sai sem agir (a venda não confirmada fica pendente)
    BackHandler(enabled = vm.stack.size > 1 || vm.current is Screen.Operation) {
        when (vm.current) { Screen.Operation -> vm.abort(); Screen.Result -> vm.home(); else -> vm.pop() }
    }

    // chip do inspetor, na barra do topo: a última transação (pay/cancel), de qualquer tela (modo de integração)
    val entries by vm.trace.entries.collectAsState()
    val lastTx = entries.lastOrNull { it.kind == TraceEntry.Kind.CALL && it.method in TRANSACTION_CALLS }
    val chip: (@Composable RowScope.() -> Unit)? = if (settings.integrationMode && lastTx != null && vm.current !is Screen.Console) {
        { InspectorChip(inspectorText(lastTx), Modifier.padding(end = 8.dp)) { rawTab = 1; rawId = lastTx.id } }
    } else null

    Box(Modifier.fillMaxSize()) {
        CompositionLocalProvider(LocalTopBarExtra provides chip) {
            when (val s = vm.current) {
                Screen.Connect -> ConnectScreen(vm)
                Screen.Home -> HomeScreen(vm)
                is Screen.Amount -> AmountScreen(vm, s.op)
                is Screen.Checkout -> CheckoutScreen(vm, s.amount, s.op)
                Screen.Operation -> OperationScreen(vm)
                Screen.Result -> ResultScreen(vm, onRaw = { id, t -> rawTab = t; rawId = id })
                is Screen.Receipt -> ReceiptScreen(vm, s.charge)
                is Screen.Cancel -> CancelScreen(vm, s.nsu)
                is Screen.Lookup -> LookupScreen(vm, s.nsu)
                Screen.Confirm -> ConfirmScreen(vm)
                Screen.Report -> ReportScreen(vm)
                is Screen.FreeRequest -> FreeRequestScreen(vm, s.base)
                Screen.More -> MoreScreen(vm)
                Screen.Developer -> DeveloperScreen(vm)
                Screen.Console -> ConsoleScreen(vm, onOpen = { rawTab = 1; rawId = it })
            }
        }
        // barra de processo: qualquer chamada em curso ganha aviso na hora (depois de 300 ms, para as instantâneas não
        // piscarem), com a última `notification` que o serviço mandou desde que ela começou. Venda e cancelamento têm a
        // tela de operação e ficam de fora.
        val running = entries.filter { it.running && it.method !in TRANSACTION_CALLS }
        val first = running.firstOrNull()
        var shown by remember { mutableStateOf<TraceEntry?>(null) }
        LaunchedEffect(first?.id) { if (first == null) shown = null else { delay(300); shown = first } }
        val notice = vm.notice
        val bar = shown
        val barWidth = Modifier.widthIn(max = LocalWidthClass.current.contentMaxWidth)
        if (bar != null && first != null) ProcessBar(Labels.callTitle(bar.method), notice?.takeIf { it.at >= bar.startedAt }?.message, bar.method, running.size - 1,
            Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 76.dp).then(barWidth))
        else SnackbarHost(snack, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 76.dp).then(barWidth)) { PtSnackbar(it.visuals.message) }
    }
    // pedidos do serviço com "Telas do integrador": uma folha nova a cada pedido (sem herdar o texto do anterior)
    prompt?.let { p -> key(p) { PromptSheet(p, onAbortOperation = { vm.abort() }) } }
    rawId?.let { id -> vm.trace.find(id)?.let { RawSheet(it, settings.receiptsInJson, rawTab) { rawId = null } } ?: run { rawId = null } }
}

/** Chamadas que o chip acompanha: as que movem dinheiro. */
val TRANSACTION_CALLS = setOf("pay", "cancel")

fun inspectorText(e: TraceEntry): String = e.method + when { e.running -> "  …"; e.failed -> "  ✕ ${e.durationMs} ms"; else -> "  ← ${e.durationMs} ms" }

/** Pedido de dado digitado (CVV, 4 últimos, documento) ou de menu, respeitando tamanho, alfanumérico e tempo. */
@Composable
private fun PromptSheet(p: Prompt, onAbortOperation: () -> Unit) {
    when (p) {
        is Prompt.ClearData -> {
            val r = p.request
            var value by remember { mutableStateOf("") }
            val min = r.minSize ?: 1; val max = (r.maxSize ?: 32).coerceAtLeast(min)
            val left = rememberCountdown(r.timeout ?: 0) { p.abort() }
            PtSheet(r.title?.ifBlank { null } ?: r.description ?: "Informe o dado", onDismiss = {}, dismissible = false, actions = {
                PtButton("Cancelar", { p.abort(); onAbortOperation() }, Modifier.weight(1f), PtButtonKind.Outlined)
                PtButton("Confirmar", { p.answer(value) }, Modifier.weight(1f), enabled = value.length in min..max)
            }) {
                if (!r.description.isNullOrBlank() && !r.title.isNullOrBlank()) Text(r.description!!, style = MaterialTheme.typography.bodyMedium)
                PtField(value, { v -> value = (if (r.alphaNumeric) v else v.filter(Char::isDigit)).take(max) }, r.messageIndex?.name ?: "Dado",
                    keyboard = if (r.alphaNumeric) KeyboardType.Text else KeyboardType.NumberPassword,
                    supporting = listOfNotNull(if (min == max) "$min caracteres" else "De $min a $max caracteres", left?.let { "expira em ${it}s" }).joinToString(" · "))
            }
        }
        is Prompt.Menu -> {
            val r = p.request
            val left = rememberCountdown(r.timeout ?: 0) { p.abort() }
            PtSheet(r.title?.ifBlank { null } ?: "Selecione", onDismiss = {}, dismissible = false, actions = {
                PtButton("Cancelar", { p.abort(); onAbortOperation() }, Modifier.weight(1f), PtButtonKind.Outlined)
            }) {
                left?.let { Text("Expira em ${it}s", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                r.optionList.orEmpty().forEachIndexed { i, o -> ListRow(o, trailing = { PtIcon(PtIcons.Chevron) }, onClick = { p.choose(i) }) }   // índice 0-based, como o contrato
            }
        }
    }
}

/** Contagem regressiva em segundos; null quando não há tempo limite. Chama [onZero] uma vez ao zerar. */
@Composable
fun rememberCountdown(seconds: Int, onZero: () -> Unit): Int? {
    if (seconds <= 0) return null
    var left by remember { mutableIntStateOf(seconds) }
    LaunchedEffect(Unit) { while (left > 0) { delay(1000); left-- }; onZero() }
    return left
}

/** Dados crus de uma chamada: Requisição, Resposta (ou Erro), já censurados, com Copiar. */
@Composable
fun RawSheet(e: TraceEntry, receiptsInJson: Boolean, initialTab: Int = 1, onDismiss: () -> Unit) {
    val clipboard = LocalClipboardManager.current
    var tab by remember(e.id) { mutableIntStateOf(initialTab) }
    val tabs = listOf("Requisição", if (e.failed) "Erro" else "Resposta")
    val text = (if (tab == 0) e.request else e.error ?: e.response)?.let { if (receiptsInJson) it else stripReceipts(it) } ?: "(sem conteúdo)"
    PtSheet(e.method, onDismiss, actions = {
        PtButton("Copiar", { clipboard.setText(AnnotatedString(text)) }, Modifier.weight(1f), PtButtonKind.Outlined, PtIcons.Copy)
        PtButton("Fechar", onDismiss, Modifier.weight(1f))
    }) {
        Text("${e.kind.name.lowercase()} · tela ${e.screen} · ${e.durationMs?.let { "$it ms" } ?: "em curso"}", style = MonoStyle, color = MaterialTheme.colorScheme.onSurfaceVariant)
        PtTabs(tabs, tab) { tab = it }
        JsonBlock(text)
    }
}

/** Com "Comprovantes no JSON" desligado, as vias viram só a contagem de linhas. */
fun stripReceipts(json: String): String = runCatching {
    fun walk(v: Any?): Any? = when (v) {
        is JSONObject -> { v.keys().asSequence().toList().forEach { k -> val x = v.opt(k); if ((k == "merchantReceipt" || k == "cardholderReceipt") && x is JSONArray) v.put(k, "(${x.length()} linhas)") else walk(x) }; v }
        is JSONArray -> { for (i in 0 until v.length()) walk(v.opt(i)); v }
        else -> v
    }
    if (json.trimStart().startsWith("[")) (walk(JSONArray(json)) as JSONArray).toString(2) else (walk(JSONObject(json)) as JSONObject).toString(2)
}.getOrDefault(json)
