package br.com.aditum.paytrial.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import br.com.aditum.data.v2.enums.InstallmentType
import br.com.aditum.data.v2.enums.PayOperationType
import br.com.aditum.data.v2.model.payment.PaymentRequest
import br.com.aditum.paytrial.app.Labels
import br.com.aditum.paytrial.app.MainViewModel
import br.com.aditum.paytrial.app.PaymentOption
import br.com.aditum.paytrial.app.PaymentRules
import br.com.aditum.paytrial.app.Screen
import br.com.aditum.paytrial.client.ContractJson
import br.com.aditum.paytrial.designsystem.*
import kotlinx.coroutines.delay

/** Valor: só o valor, no teclado do app. */
@Composable
fun AmountScreen(vm: MainViewModel, op: PayOperationType) {
    var cents by rememberSaveable { mutableLongStateOf(0L) }
    PtScaffold(Labels.operation(op), onBack = { vm.pop() }, scroll = false, bottomBar = {
        PtButton("Continuar", { vm.push(Screen.Checkout(cents, op)) }, Modifier.fillMaxWidth(), icon = PtIcons.Forward, enabled = cents > 0)
    }) {
        Spacer(Modifier.height(16.dp))
        AmountDisplay(cents)
        Spacer(Modifier.weight(1f))
        AmountKeypad(onDigit = { cents = Money.push(cents, it) }, onBackspace = { cents /= 10 }, onClear = { cents = 0 })
    }
}

/**
 * Pagamento: o pedido montado a partir do `MerchantData` (operações e formas que o estabelecimento permite, limites
 * da restrição), validado antes de chamar `pay`. A referência do pedido é gerada sozinha e fica em "Mais opções".
 */
@Composable
fun CheckoutScreen(vm: MainViewModel, amount: Long, initialOp: PayOperationType) {
    val settings by vm.settings.state.collectAsState()
    val m = vm.merchant
    val options = remember(m) { PaymentRules.options(m) }
    val operations = remember(m) { PaymentRules.operations(m) }
    var op by remember { mutableStateOf(if (initialOp in operations) initialOp else PayOperationType.Authorization) }
    var option by remember { mutableStateOf(options.firstOrNull()) }
    var instType by remember(option) { mutableStateOf(option?.installmentTypes?.first() ?: InstallmentType.None) }
    val range = option?.let { PaymentRules.installmentRange(it, instType) } ?: 1..1
    var installments by remember(option, instType) { mutableIntStateOf(range.first) }
    var contactless by remember { mutableStateOf(m?.contaclessEnabled != false) }
    var typed by remember { mutableStateOf(false) }
    var originalNsu by remember { mutableStateOf("") }
    var chargeId by remember { mutableStateOf(vm.newChargeId()) }
    var origin by remember { mutableStateOf("") }
    var more by remember { mutableStateOf(false) }
    var preview by remember { mutableStateOf(false) }

    val error = PaymentRules.validate(amount, option, instType, installments, op, originalNsu)
    fun request() = PaymentRequest(
        amount = amount, isQrCode = option?.isQr == true, operationType = op, paymentType = option?.paymentType ?: br.com.aditum.data.v2.enums.PaymentType.Undefined,
        installmentType = if (option?.isQr == true) InstallmentType.None else instType, installmentNumber = if (instType == InstallmentType.None) 1 else installments,
        merchantChargeId = chargeId.trim(), origin = origin.trim(), originalNsu = if (PaymentRules.needsOriginalNsu(op)) originalNsu.trim() else "",
        acquirer = option?.acquirer ?: br.com.aditum.data.v2.enums.Acquirer.Undefined, scheme = option?.scheme.orEmpty(), manualEntry = typed, allowContactless = contactless,
    )
    fun send() { if (settings.previewBeforeSend) preview = true else vm.startPay(request()) }

    PtScaffold("Pagamento", onBack = { vm.pop() }, actions = {
        if (settings.integrationMode) IconButton(onClick = { preview = true }) { PtIcon(PtIcons.Json, "Ver a requisição") }
    }, bottomBar = {
        PtButton("Pagar ${Money.brl(amount)}", ::send, Modifier.fillMaxWidth(), icon = PtIcons.Sale, enabled = error == null)
    }) {
        Text(Money.brl(amount), style = MaterialTheme.typography.displaySmall, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        if (operations.size > 1) Segmented(operations.map { it to Labels.operation(it) }, op, { op = it })
        if (PaymentRules.needsOriginalNsu(op)) PtField(originalNsu, { originalNsu = it.filter(Char::isDigit) }, "NSU da pré-autorização", keyboard = KeyboardType.Number)
        SectionLabel("Forma de pagamento")
        OptionTiles(options, option) { option = it }
        val o = option
        if (o != null && !o.isQr && o.installmentTypes.size > 1) {
            SectionLabel("Parcelamento")
            Segmented(o.installmentTypes.map { it to Labels.installmentType(it) }, instType, { instType = it })
        }
        if (o != null && (instType == InstallmentType.Merchant || instType == InstallmentType.Issuer)) {
            StepperRow(installments, range.first, range.last, { installments = it }, "${range.first} a ${range.last} parcelas · ${installments} × ${Money.brl(amount / installments.coerceAtLeast(1))}")
        }
        SwitchRow("Aproximação", contactless, { contactless = it }, if (m?.contaclessEnabled == false) "O estabelecimento não permite" else "allowContactless", enabled = m?.contaclessEnabled != false)
        SwitchRow("Venda digitada", typed, { typed = it }, if (m?.typedPanEnabled == true) "manualEntry: os dados do cartão são pedidos um a um" else "O estabelecimento não permite (typedPanEnabled)", enabled = m?.typedPanEnabled == true)
        ListRow("Mais opções", icon = PtIcons.Settings, details = listOf("Referência $chargeId"), trailing = { PtIcon(if (more) PtIcons.ChevronDown else PtIcons.Chevron) }, onClick = { more = !more })
        if (more) {
            PtField(chargeId, { chargeId = it.take(40) }, "Referência do pedido (merchantChargeId)", supporting = "Gerada a cada venda. Repita uma para testar referência duplicada.")
            PtButton("Gerar outra referência", { chargeId = vm.newChargeId() }, kind = PtButtonKind.Text, icon = PtIcons.Refresh)
            PtField(origin, { origin = it.take(40) }, "Origem (origin)", supporting = "Opcional: identificação livre do canal de venda")
            KeyValueCard(listOf("Moeda" to "986 (BRL)", "Adquirente" to (o?.acquirer?.name ?: "—"), "Esquema" to (o?.scheme?.ifBlank { "—" } ?: "—")))
        }
        if (error != null && amount > 0) Text(error, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
    }
    if (preview) RequestPreviewSheet(ContractJson.redacted(request()), onDismiss = { preview = false }, onSend = if (error == null) { { preview = false; vm.startPay(request()) } } else null)
}

@Composable
private fun OptionTiles(options: List<PaymentOption>, selected: PaymentOption?, onSelect: (PaymentOption) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { o ->
                    val on = o.key == selected?.key
                    Column(Modifier.weight(1f).height(72.dp).clip(MaterialTheme.shapes.large)
                        .background(if (on) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh)
                        .then(if (on) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, MaterialTheme.shapes.large) else Modifier)
                        .clickable(role = Role.RadioButton) { onSelect(o) }.padding(6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        PtIcon(Labels.paymentIcon(o.paymentType), tint = if (on) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary)
                        Text(o.label, style = MaterialTheme.typography.labelMedium, maxLines = 2, textAlign = TextAlign.Center,
                            color = if (on) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface)
                    }
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

/** Prévia da requisição: o `PaymentRequest` exato (censurado) antes de chamar `pay`. */
@Composable
fun RequestPreviewSheet(json: String, onDismiss: () -> Unit, onSend: (() -> Unit)?) {
    val clipboard = LocalClipboardManager.current
    PtSheet("Requisição que será enviada", onDismiss, actions = {
        PtButton("Copiar", { clipboard.setText(AnnotatedString(json)) }, Modifier.weight(1f), PtButtonKind.Outlined, PtIcons.Copy)
        if (onSend != null) PtButton("Enviar", onSend, Modifier.weight(1f), icon = PtIcons.Send)
    }) {
        Text("IAditumSdkService.pay(PaymentRequest)", style = MonoStyle, color = MaterialTheme.colorScheme.onSurfaceVariant)
        JsonBlock(json)
    }
}

/**
 * Operação em curso. Cada `notification` vira um passo; com "Telas do integrador" o app desenha PIN e QR aqui; com
 * "Telas da Aditum" esta tela fica atrás do app da Aditum e só registra o progresso. Cancelar e Voltar interrompem.
 */
@Composable
fun OperationScreen(vm: MainViewModel) {
    val settings by vm.settings.state.collectAsState()
    val op = vm.op ?: return
    val last = op.steps.lastOrNull()
    PtScaffold(op.title, bottomBar = { PtButton("Cancelar operação", { vm.abort() }, Modifier.fillMaxWidth(), PtButtonKind.Outlined, PtIcons.Close) }) {
        if (!settings.integratorScreens) Banner("Operação na tela da Aditum", Tone.Info, "O app da Aditum está por cima; ao terminar, volta para cá com o resultado.")
        AmountDisplay(op.amount)
        Banner(last?.title ?: "Iniciando", Tone.Neutral, last?.message?.ifBlank { null } ?: "Aguardando o serviço", if (op.kind.name == "PAY") PtIcons.Contactless else PtIcons.Cancel)
        if (op.steps.size > 1 || settings.integrationMode) {
            SectionLabel("Linha do tempo")
            val shown = op.steps.takeLast(8)
            Timeline(shown.mapIndexed { i, st -> TimelineStep(st.title, if (settings.integrationMode) st.detail else st.message.ifBlank { null }, current = i == shown.lastIndex) })
        }
        LinearProgressIndicator(Modifier.fillMaxWidth())
    }
    if (settings.integratorScreens && op.pinLength != null) PtSheet("Senha do cartão", onDismiss = {}, dismissible = false, actions = {
        PtButton("Cancelar", { vm.abort() }, Modifier.weight(1f), PtButtonKind.Outlined)
    }) {
        Text("Digite a senha no pinpad", style = MaterialTheme.typography.bodyMedium)
        Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.Center) {
            repeat(op.pinLength.coerceIn(0, 12)) { Box(Modifier.padding(horizontal = 6.dp).size(16.dp).clip(MaterialTheme.shapes.extraSmall).background(MaterialTheme.colorScheme.primary)) }
            if (op.pinLength == 0) Text("—", style = MaterialTheme.typography.headlineSmall)
        }
    }
    val qr = op.qr
    if (settings.integratorScreens && qr != null) {
        var left by remember(qr) { mutableIntStateOf(qr.totalSeconds - ((System.currentTimeMillis() - qr.receivedAt) / 1000).toInt()) }
        LaunchedEffect(qr) { while (left > 0) { delay(1000); left-- } }   // tempo ≤ 0 não fecha sozinho (defeito do original)
        PtSheet("Peça ao cliente para ler o código", onDismiss = {}, dismissible = false, actions = {
            PtButton("Cancelar", { vm.abort() }, Modifier.weight(1f), PtButtonKind.Outlined)
        }) {
            QrCode(qr.code)
            Text(if (qr.totalSeconds > 0) "Expira em ${left.coerceAtLeast(0) / 60}:${(left.coerceAtLeast(0) % 60).toString().padStart(2, '0')}" else "Sem prazo informado",
                style = MaterialTheme.typography.titleMedium, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            Text("A consulta do pagamento é automática.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        }
    }
}
