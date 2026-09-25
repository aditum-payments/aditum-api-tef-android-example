package br.com.aditum.paytrial.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import br.com.aditum.data.v2.enums.SendReceiptType
import br.com.aditum.data.v2.model.Charge
import br.com.aditum.data.v2.model.cancelation.CancelationRequest
import br.com.aditum.paytrial.app.Labels
import br.com.aditum.paytrial.app.MainViewModel
import br.com.aditum.paytrial.app.OpKind
import br.com.aditum.paytrial.app.ResultState
import br.com.aditum.paytrial.app.Screen
import br.com.aditum.paytrial.designsystem.*

/** Dados legíveis de uma cobrança: cartão só com os 4 últimos e portador em iniciais. */
fun chargeRows(c: Charge): List<Pair<String, String>> = listOfNotNull(
    Labels.brand(c.brand).takeIf { it.isNotBlank() }?.let { "Bandeira" to it },
    "Forma" to (Labels.paymentType(c.paymentType) + if (c.installmentNumber > 1) " · ${Labels.installmentType(c.installmentType)} ${c.installmentNumber}x" else ""),
    c.nsu?.let { "NSU" to it }, c.authorizationCode?.takeIf { it.isNotBlank() }?.let { "Autorização" to it },
    c.cardNumber?.takeIf { it.isNotBlank() }?.let { "Cartão" to maskedCard(it) }, c.cardholderName?.takeIf { it.isNotBlank() }?.let { "Portador" to maskedHolder(it) },
    c.merchantChargeId?.takeIf { it.isNotBlank() }?.let { "Pedido" to it }, c.creationDateTime?.let { "Data" to Labels.dateTime(it) },
    c.acquirer.takeIf { it.name != "Undefined" }?.let { "Adquirente" to it.name },
)
val MONO_KEYS = setOf("NSU", "Autorização", "Cartão", "Pedido")

/**
 * Resultado — tela do integrador nos dois modos (no App2App a Aditum não mostra resultado). Nenhuma ação é
 * obrigatória: Fechar e Voltar saem; a venda não confirmada fica em Confirmar, na tela inicial.
 */
@Composable
fun ResultScreen(vm: MainViewModel, onRaw: (Long, Int) -> Unit) {
    val settings by vm.settings.state.collectAsState()
    val r = vm.result ?: run { LaunchedEffect(Unit) { vm.home() }; return }
    var tab by remember { mutableIntStateOf(0) }
    var send by remember { mutableStateOf(false) }
    var undoAsk by remember { mutableStateOf(false) }
    val c = r.charge
    val isPay = r.kind == OpKind.PAY
    val title = when {
        r.undone -> "Venda desfeita"; !r.approved -> if (isPay) "Pagamento negado" else "Cancelamento negado"
        !isPay -> "Cancelamento aprovado"; r.confirmed -> "Pagamento confirmado"; else -> "Pagamento aprovado"
    }
    PtScaffold(title, onClose = { vm.home() }, wide = true, bottomBar = { ResultFooter(vm) }) {
        if (settings.integrationMode && r.traceId != null) PtTabs(listOf("Resumo", "Requisição", "Resposta"), tab) { if (it > 0) onRaw(r.traceId, it - 1) }   // as abas cruas abrem o par requisição/resposta
        // tela larga: situação e ações à esquerda, os dados da venda à direita
        TwoPane(first = {
            ResultHero(r)
            when {
                r.failure != null -> Banner("A chamada ao serviço falhou", Tone.Error, r.failure)
                r.errors.isNotEmpty() -> r.errors.forEach { Banner(it.message?.ifBlank { null } ?: "Erro", Tone.Error, listOfNotNull(it.description?.takeIf { d -> d.isNotBlank() && d != it.message }, it.code?.let { k -> "código $k" }).joinToString(" · ").ifBlank { null }) }
            }
            if (c != null) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                when {
                    r.undone -> StatusTag("Desfeita", Tone.Info, PtIcons.Undo)
                    isPay && r.approved && !r.confirmed -> StatusTag("Aguardando confirmação do integrador", Tone.Warning, PtIcons.Pending)
                    isPay && r.approved -> StatusTag("Confirmada", Tone.Success, PtIcons.Success)
                    else -> c.chargeStatus?.let { StatusTag(Labels.chargeStatus(it), Tone.Neutral) }
                }
            }
            if (LocalWidthClass.current == WidthClass.Expanded) SaleActions(vm, r, onSend = { send = true }, onUndo = { undoAsk = true })
        }, second = {
            if (c != null) KeyValueCard(chargeRows(c), MONO_KEYS)
            // no celular as ações ficam depois dos dados, como antes
            if (LocalWidthClass.current != WidthClass.Expanded) SaleActions(vm, r, onSend = { send = true }, onUndo = { undoAsk = true })
        })
    }
    if (send && c != null) SendReceiptSheet(vm, c) { send = false }
    if (undoAsk && c != null) PtDialog("Desfazer a venda?", "A venda de ${Money.brl(c.amount)} é desfeita no autorizador (desfazimento) e não será cobrada.", "Desfazer",
        onConfirm = { undoAsk = false; vm.startCancel(CancelationRequest(nsu = c.nsu.orEmpty(), isReversal = true), c.amount) }, onDismiss = { undoAsk = false }, destructive = true)
}

@Composable
private fun ResultHero(r: ResultState) {
    val ok = r.approved && !r.undone
    val (icon, color) = when {
        r.undone -> PtIcons.Undo to PtTheme.status.info
        ok && r.kind == OpKind.PAY && !r.confirmed -> PtIcons.Success to PtTheme.status.success
        ok -> PtIcons.Confirm to PtTheme.status.success
        else -> PtIcons.Error to MaterialTheme.colorScheme.error
    }
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        PtIcon(icon, tint = color, size = 56.dp)
        Text(when { r.undone -> "Desfeita"; ok -> "Aprovado"; else -> "Não aprovado" }, style = MaterialTheme.typography.headlineSmall, color = color)
        Text(Money.brl(r.charge?.amount ?: r.amount), style = MaterialTheme.typography.displaySmall, textAlign = TextAlign.Center)
    }
}

/**
 * Ações **desta venda**, juntas logo abaixo dos dados dela: primeiro o que o integrador faz com a transação — Desfazer e
 * Confirmar enquanto ela aguarda confirmação, Cancelar depois; Tentar de novo quando foi negada —, e embaixo o
 * comprovante (Imprimir e Enviar). O rodapé fica só com a navegação do app, para não se confundir com elas.
 */
@Composable
private fun SaleActions(vm: MainViewModel, r: ResultState, onSend: () -> Unit, onUndo: () -> Unit) {
    val c = r.charge
    val busy = vm.busy != null
    val isPay = r.kind == OpKind.PAY
    val live = r.approved && !r.undone && c != null
    val retry = isPay && !r.approved && r.paymentRequest != null
    if (!live && !retry) return
    SectionLabel("Esta venda")
    if (live && c != null) {
        if (isPay) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (!r.confirmed) {
                PtButton("Desfazer", onUndo, Modifier.weight(1f), PtButtonKind.Outlined, PtIcons.Undo, enabled = !busy)
                PtButton("Confirmar", { vm.confirm(c.nsu.orEmpty()) }, Modifier.weight(1f), PtButtonKind.Primary, PtIcons.Confirm, enabled = !busy)
            } else PtButton("Cancelar venda", { vm.push(Screen.Cancel(c.nsu.orEmpty())) }, Modifier.weight(1f), PtButtonKind.Outlined, PtIcons.Cancel)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PtButton(if (vm.hasPrinter) "Imprimir" else "Comprovante", { vm.push(Screen.Receipt(c)) }, Modifier.weight(1f), PtButtonKind.Tonal, if (vm.hasPrinter) PtIcons.Print else PtIcons.Receipt)
            PtButton("Enviar", onSend, Modifier.weight(1f), PtButtonKind.Tonal, PtIcons.Send)
        }
    } else if (retry && r.paymentRequest != null) {
        PtButton("Tentar de novo", { vm.startPay(r.paymentRequest.copy(merchantChargeId = vm.newChargeId())) }, Modifier.fillMaxWidth(), PtButtonKind.Outlined, PtIcons.Refresh, enabled = !busy)
    }
}

/** Rodapé do resultado: só a navegação do app — Finalizar (volta ao início) e Nova venda. */
@Composable
private fun ResultFooter(vm: MainViewModel) = ActionRow(
    secondary = { PtButton("Finalizar", { vm.home() }, Modifier.weight(1f), PtButtonKind.Outlined, PtIcons.Home) },
    primary = { PtButton("Nova venda", { vm.home(); vm.push(Screen.Amount()) }, Modifier.weight(1f), icon = PtIcons.Sale) })

/** Comprovante: via em botões divididos, prévia no papel, Imprimir pelo `IPrinterSdk` ou Enviar. */
@Composable
fun ReceiptScreen(vm: MainViewModel, c: Charge) {
    var via by remember { mutableIntStateOf(1) }
    var send by remember { mutableStateOf(false) }
    val lines = when (via) { 0 -> c.merchantReceipt.orEmpty(); 1 -> c.cardholderReceipt.orEmpty(); else -> c.merchantReceipt.orEmpty() + listOf("", "- - - - - - - - - - - - - - - -", "") + c.cardholderReceipt.orEmpty() }
    PtScaffold("Comprovante", onBack = { vm.pop() }, bottomBar = {
        // sem impressora (getPrinterSdk() nulo) o comprovante sai só por envio
        if (vm.hasPrinter) ActionRow(secondary = { PtButton("Enviar", { send = true }, Modifier.weight(1f), PtButtonKind.Outlined, PtIcons.Send) },
            primary = { PtButton(if (vm.busy == "print") "Imprimindo…" else "Imprimir", { vm.print(c, via != 1, via != 0) }, Modifier.weight(1f), icon = PtIcons.Print, enabled = vm.busy == null) })
        else PtButton("Enviar comprovante", { send = true }, Modifier.fillMaxWidth(), icon = PtIcons.Send)
    }) {
        SplitChoice(listOf(Triple(0, "Loja", PtIcons.Store), Triple(1, "Cliente", PtIcons.Person), Triple(2, "Ambas", PtIcons.Receipt)), via, { via = it })
        if (lines.isEmpty()) EmptyBlock("A transação não trouxe esta via", PtIcons.Receipt) else ReceiptView(lines, logo = vm.receiptLogo)
        if (!vm.hasPrinter) Banner("Terminal sem impressora", Tone.Info, "getDeviceSdk().printerSdk veio nulo: o comprovante sai por e-mail ou SMS.")
        Text("Charge.${if (via == 0) "merchantReceipt" else if (via == 1) "cardholderReceipt" else "merchantReceipt + cardholderReceipt"} → bitmap 384 px → IPrinterSdk.print",
            style = MonoStyle, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    if (send) SendReceiptSheet(vm, c) { send = false }
}

/** Envio do comprovante do cliente por e-mail ou SMS (`sendReceipt`). */
@Composable
fun SendReceiptSheet(vm: MainViewModel, c: Charge, onDismiss: () -> Unit) {
    var method by remember { mutableStateOf(SendReceiptType.Email) }
    var to by remember { mutableStateOf("") }
    val valid = if (method == SendReceiptType.Email) Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$").matches(to.trim()) else to.filter(Char::isDigit).length in 10..11
    PtSheet("Enviar comprovante", onDismiss, actions = {
        PtButton("Cancelar", onDismiss, Modifier.weight(1f), PtButtonKind.Outlined)
        PtButton(if (vm.busy == "sendReceipt") "Enviando…" else "Enviar", { vm.sendReceipt(c, method, if (method == SendReceiptType.SMS) to.filter(Char::isDigit) else to) { ok -> if (ok) onDismiss() } },
            Modifier.weight(1f), icon = PtIcons.Send, enabled = valid && vm.busy == null)
    }) {
        Segmented(listOf(SendReceiptType.Email to "E-mail", SendReceiptType.SMS to "SMS"), method, { method = it; to = "" })
        if (method == SendReceiptType.Email) PtField(to, { to = it.trim() }, "E-mail do cliente", keyboard = KeyboardType.Email)
        else PtField(to, { to = it.filter(Char::isDigit).take(11) }, "Celular com DDD", keyboard = KeyboardType.Phone, supporting = "Só números, 10 ou 11 dígitos")
    }
}
