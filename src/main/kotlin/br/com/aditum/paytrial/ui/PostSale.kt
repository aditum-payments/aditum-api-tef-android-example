package br.com.aditum.paytrial.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import br.com.aditum.data.v2.enums.ChargeStatus
import br.com.aditum.data.v2.model.Charge
import br.com.aditum.data.v2.model.cancelation.CancelationRequest
import br.com.aditum.data.v2.model.report.ReportRequest
import br.com.aditum.data.v2.model.report.ReportResponse
import br.com.aditum.paytrial.app.Labels
import br.com.aditum.paytrial.app.MainViewModel
import br.com.aditum.paytrial.app.Screen
import br.com.aditum.paytrial.client.Res
import br.com.aditum.paytrial.designsystem.*
import kotlinx.coroutines.launch
import java.util.Calendar

/** Cancelar: busca a cobrança antes (`charge`), parcial limitado ao valor original, desfazimento opcional. */
@Composable
fun CancelScreen(vm: MainViewModel, prefill: String) {
    val scope = rememberCoroutineScope()
    var nsu by remember { mutableStateOf(prefill) }
    var found by remember { mutableStateOf<Charge?>(null) }
    var notFound by remember { mutableStateOf<String?>(null) }
    var partial by remember { mutableStateOf(false) }
    var partialCents by remember { mutableLongStateOf(0L) }
    var reversal by remember { mutableStateOf(false) }
    var ask by remember { mutableStateOf(false) }
    val allowPartial = vm.merchant?.allowPartialCancel == true
    fun search() = scope.launch {
        notFound = null; found = null
        when (val r = vm.findCharge(nsu, null)) { is Res.Ok -> if (r.value != null) found = r.value else notFound = "Nenhuma transação com o NSU $nsu"; is Res.Fail -> notFound = r.message }
    }
    LaunchedEffect(Unit) { if (prefill.isNotBlank()) search() }
    val c = found
    val amount = if (partial) partialCents else c?.amount ?: 0L
    val error = when { c == null -> null; partial && partialCents <= 0 -> "Informe o valor a cancelar"; partial && partialCents > c.amount -> "Até ${Money.brl(c.amount)}"; else -> null }
    PtScaffold("Cancelar venda", onBack = { vm.pop() }, wide = true, bottomBar = {
        PtButton("Cancelar ${Money.brl(amount)}", { ask = true }, Modifier.fillMaxWidth(), PtButtonKind.Danger, PtIcons.Cancel, enabled = c != null && error == null && vm.busy == null)
    }) {
        TwoPane(first = {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PtField(nsu, { nsu = it.filter(Char::isDigit) }, "NSU", Modifier.weight(1f), keyboard = KeyboardType.Number)
                PtButton("Buscar", { search() }, kind = PtButtonKind.Tonal, icon = PtIcons.Lookup, enabled = nsu.isNotBlank() && vm.busy == null)
            }
            if (vm.busy == "charge") LinearProgressIndicator(Modifier.fillMaxWidth())
            notFound?.let { Banner(it, Tone.Warning) }
            if (c != null) {
                c.chargeStatus?.let { StatusTag(Labels.chargeStatus(it), if (it == ChargeStatus.Canceled) Tone.Error else Tone.Neutral) }
                KeyValueCard(listOf("Valor" to Money.brl(c.amount)) + chargeRows(c), MONO_KEYS)
            }
        }, second = {
            if (c != null) {
                SwitchRow("Cancelamento parcial", partial, { partial = it }, if (allowPartial) "isPartial + entryAmount" else "O estabelecimento não permite (allowPartialCancel)", enabled = allowPartial)
                if (partial) {
                    AmountDisplay(partialCents, "Valor a cancelar", error)
                    AmountKeypad(onDigit = { partialCents = Money.push(partialCents, it) }, onBackspace = { partialCents /= 10 }, onClear = { partialCents = 0 })
                }
                SwitchRow("Desfazimento (estorno)", reversal, { reversal = it }, "isReversal: desfaz em vez de cancelar")
            }
        })
    }
    if (ask && c != null) PtDialog("Cancelar a venda?", "${if (partial) "Cancelamento parcial de ${Money.brl(amount)}" else "Cancelamento de ${Money.brl(amount)}"} do NSU ${c.nsu}.", "Cancelar venda",
        onConfirm = { ask = false; vm.startCancel(CancelationRequest(nsu = c.nsu.orEmpty(), isReversal = reversal, isPartial = partial, entryAmount = if (partial) partialCents else 0L), amount) },
        onDismiss = { ask = false }, destructive = true, dismiss = "Voltar")
}

/** Consultar: por NSU ou pela referência do pedido do integrador; ações sobre a cobrança achada. */
@Composable
fun LookupScreen(vm: MainViewModel, prefill: String) {
    val scope = rememberCoroutineScope()
    var byRef by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf(prefill) }
    var found by remember { mutableStateOf<Charge?>(null) }
    var pending by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf<String?>(null) }
    var send by remember { mutableStateOf(false) }
    fun search() = scope.launch {
        msg = null; found = null; pending = false
        when (val r = vm.findCharge(if (byRef) null else query, if (byRef) query else null)) { is Res.Ok -> if (r.value != null) found = r.value else msg = "Nenhuma transação encontrada"; is Res.Fail -> msg = r.message }
        // Confirmar só faz sentido para o que ainda aguarda o integrador (`pendingTransactions`)
        found?.nsu?.let { nsu -> pending = (vm.pendings() as? Res.Ok)?.value?.any { it.nsu == nsu } == true }
    }
    LaunchedEffect(Unit) { if (prefill.isNotBlank()) search() }
    PtScaffold("Consultar", onBack = { vm.pop() }, wide = true) {
        TwoPane(first = {
        Segmented(listOf(false to "NSU", true to "Referência do pedido"), byRef, { byRef = it; query = "" })
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PtField(query, { query = if (byRef) it.take(40) else it.filter(Char::isDigit) }, if (byRef) "Referência (merchantChargeId)" else "NSU", Modifier.weight(1f),
                keyboard = if (byRef) KeyboardType.Text else KeyboardType.Number)
            PtButton("Buscar", { search() }, kind = PtButtonKind.Tonal, icon = PtIcons.Lookup, enabled = query.isNotBlank() && vm.busy == null)
        }
        if (vm.busy == "charge") LinearProgressIndicator(Modifier.fillMaxWidth())
        msg?.let { Banner(it, Tone.Warning) }
        }, second = {
        found?.let { c ->
            c.chargeStatus?.let { StatusTag(Labels.chargeStatus(it), if (it == ChargeStatus.Pending) Tone.Warning else Tone.Neutral) }
            KeyValueCard(listOf("Valor" to Money.brl(c.amount)) + chargeRows(c), MONO_KEYS)
            if (pending) {
                StatusTag("Aguardando confirmação do integrador", Tone.Warning)
                ListRow("Confirmar", icon = PtIcons.Confirm, details = listOf("confirmTransaction"), onClick = { vm.confirm(c.nsu.orEmpty()) { search() } })
            }
            ListRow(if (vm.hasPrinter) "Reimprimir" else "Ver comprovante", icon = if (vm.hasPrinter) PtIcons.Print else PtIcons.Receipt, onClick = { vm.push(Screen.Receipt(c)) })
            ListRow("Enviar comprovante", icon = PtIcons.Send, onClick = { send = true })
            ListRow("Cancelar", icon = PtIcons.Cancel, onClick = { vm.push(Screen.Cancel(c.nsu.orEmpty())) })
        }
        })
    }
    if (send) found?.let { SendReceiptSheet(vm, it) { send = false } }
}

/**
 * Confirmar: as vendas que aguardam a confirmação do integrador (`pendingTransactions`), com Confirmar e Desfazer em
 * cada uma — no App2App, como no TEF desktop, a confirmação é manual.
 */
@Composable
fun ConfirmScreen(vm: MainViewModel) {
    val scope = rememberCoroutineScope()
    var list by remember { mutableStateOf<List<Charge>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var undo by remember { mutableStateOf<Charge?>(null) }
    fun load() = scope.launch { when (val r = vm.pendings()) { is Res.Ok -> { list = r.value; error = null }; is Res.Fail -> error = r.message } }
    LaunchedEffect(Unit) { load() }
    PtScaffold("Confirmar", onBack = { vm.pop() }, actions = { IconButton(onClick = { load() }) { PtIcon(PtIcons.Refresh, "Atualizar") } }, bottomBar = {
        val l = list.orEmpty()
        if (l.size > 1) PtButton("Confirmar todas", { vm.confirmAll(l.mapNotNull { it.nsu }) { load() } }, Modifier.fillMaxWidth(), PtButtonKind.Tonal, PtIcons.Confirm, enabled = vm.busy == null)
    }) {
        error?.let { Banner("Não foi possível listar", Tone.Error, it) }
        when {
            list == null && error == null -> LoadingBlock("Buscando vendas pendentes…")
            list?.isEmpty() == true -> EmptyBlock("Nenhuma venda aguardando confirmação", PtIcons.Confirm)
            else -> {
                if (!list.isNullOrEmpty()) Banner("${list!!.size} venda(s) aguardando confirmação", Tone.Warning, "Confirme ou desfaça cada uma.")
                list.orEmpty().forEach { c ->
                    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        ListRow("${Labels.paymentType(c.paymentType)}${if (c.installmentNumber > 1) " ${c.installmentNumber}x" else ""} · ${Money.brl(c.amount)}", icon = Labels.paymentIcon(c.paymentType),
                            details = listOf(listOf(Labels.dateTime(c.creationDateTime), Labels.brand(c.brand)).filter { it.isNotBlank() }.joinToString(" · ")), monoDetail = c.nsu?.let { "NSU $it" })
                        ActionRow(secondary = { PtButton("Desfazer", { undo = c }, Modifier.weight(1f), PtButtonKind.Outlined, PtIcons.Undo, enabled = vm.busy == null) },
                            primary = { PtButton("Confirmar", { vm.confirm(c.nsu.orEmpty()) { load() } }, Modifier.weight(1f), PtButtonKind.Tonal, PtIcons.Confirm, enabled = vm.busy == null) })
                    }
                }
            }
        }
    }
    undo?.let { c -> PtDialog("Desfazer a venda?", "A venda de ${Money.brl(c.amount)} (NSU ${c.nsu}) é desfeita no autorizador.", "Desfazer",
        onConfirm = { undo = null; vm.startCancel(CancelationRequest(nsu = c.nsu.orEmpty(), isReversal = true), c.amount) }, onDismiss = { undo = null }, destructive = true) }
}

private enum class Period(val label: String, val days: Int) { TODAY("Hoje", 0), YESTERDAY("Ontem", 1), WEEK("7 dias", 7), MONTH("30 dias", 30) }

/** Relatório (`generateReport`): período e status, totais e a lista; tocar numa linha abre a consulta (reimprimir). */
@Composable
fun ReportScreen(vm: MainViewModel) {
    var period by remember { mutableStateOf(Period.TODAY) }
    var statuses by remember { mutableStateOf(setOf(ChargeStatus.Authorized, ChargeStatus.Canceled, ChargeStatus.PreAuthorized)) }
    var data by remember { mutableStateOf<ReportResponse?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(period, statuses) {
        data = null; error = null
        val (begin, end) = bounds(period)
        when (val r = vm.report(ReportRequest(beginDate = begin, endDate = end, statusList = ArrayList(statuses)))) { is Res.Ok -> data = r.value; is Res.Fail -> error = r.message }
    }
    PtScaffold("Relatório", onBack = { vm.pop() }) {
        ChipRow(Period.entries.map { it to it.label }, setOf(period), { period = it })
        ChipRow(listOf(ChargeStatus.Authorized to "Aprovadas", ChargeStatus.Canceled to "Canceladas", ChargeStatus.PreAuthorized to "Pré-autorizadas", ChargeStatus.Pending to "Pendentes"),
            statuses, { s -> statuses = if (s in statuses && statuses.size > 1) statuses - s else statuses + s })
        error?.let { Banner("Não foi possível gerar", Tone.Error, it) }
        val d = data
        if (d == null && error == null) LoadingBlock("Gerando relatório…")
        if (d != null) {
            KeyValueCard(listOf("Aprovadas (${d.totalNumberAuthorized})" to Money.brl(d.totalAuthorizedAmount), "Pré-autorizadas (${d.totalNumberPreAuthorized})" to Money.brl(d.totalPreAuthorizedAmount),
                "Canceladas (${d.totalNumberCanceled})" to Money.brl(d.totalCanceledAmount)))
            if (d.transactions.isEmpty()) EmptyBlock("Nenhuma transação no período", PtIcons.Report)
            d.transactions.forEach { t ->
                ListRow("${Labels.paymentType(t.paymentType)}${if (t.installmentNumber > 1) " ${t.installmentNumber}x" else ""} · ${Money.brl(t.amount)}",
                    leading = { if (Labels.brand(t.brand).isNotBlank()) BrandPlate(Labels.brand(t.brand)) else PtIcon(Labels.paymentIcon(t.paymentType), tint = MaterialTheme.colorScheme.primary) },
                    details = listOf("${Labels.dateTime(t.dateTime)} · ${Labels.chargeStatus(t.chargeStatus)}"), monoDetail = t.nsu?.let { "NSU $it" },
                    trailing = { PtIcon(PtIcons.Chevron) }, onClick = { t.nsu?.let { vm.push(Screen.Lookup(it)) } })
            }
        }
    }
}

private fun bounds(p: Period): Pair<java.util.Date, java.util.Date> {
    val start = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0); add(Calendar.DAY_OF_MONTH, -p.days) }
    val end = if (p == Period.YESTERDAY) Calendar.getInstance().apply { time = start.time; add(Calendar.DAY_OF_MONTH, 1); add(Calendar.MILLISECOND, -1) } else Calendar.getInstance()
    return start.time to end.time
}
