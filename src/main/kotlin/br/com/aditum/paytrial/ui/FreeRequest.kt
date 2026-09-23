package br.com.aditum.paytrial.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import br.com.aditum.data.v2.enums.Acquirer
import br.com.aditum.data.v2.enums.InstallmentType
import br.com.aditum.data.v2.enums.PayOperationType
import br.com.aditum.data.v2.enums.PaymentType
import br.com.aditum.data.v2.model.payment.PaymentRequest
import br.com.aditum.paytrial.app.MainViewModel
import br.com.aditum.paytrial.client.ContractJson
import br.com.aditum.paytrial.designsystem.*

/**
 * Requisição livre: todos os campos do `PaymentRequest` com todos os valores dos enums do contrato, **sem validação** —
 * o pedido vai ao `pay()` exatamente como está. Serve para ver o que o serviço faz com pedido incompleto ou fora das
 * regras do estabelecimento: com as Telas da Aditum, o que falta (valor, forma, parcelamento, NSU da pré-autorização)
 * passa pelas telas do standalone; com as do integrador, o serviço responde o erro.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FreeRequestScreen(vm: MainViewModel, base: PaymentRequest?) {
    val settings by vm.settings.state.collectAsState()
    var amount by remember { mutableStateOf((base?.amount ?: 0L).toString()) }
    var op by remember { mutableStateOf(base?.operationType ?: PayOperationType.Authorization) }
    var type by remember { mutableStateOf(base?.paymentType ?: PaymentType.Undefined) }
    var instType by remember { mutableStateOf(base?.installmentType ?: InstallmentType.Undefined) }
    var installments by remember { mutableIntStateOf(base?.installmentNumber ?: 0) }
    var qr by remember { mutableStateOf(base?.isQrCode ?: false) }
    var contactless by remember { mutableStateOf(base?.allowContactless ?: true) }
    var typed by remember { mutableStateOf(base?.manualEntry ?: false) }
    var nsu by remember { mutableStateOf(base?.originalNsu.orEmpty()) }
    var chargeId by remember { mutableStateOf(base?.merchantChargeId?.ifBlank { null } ?: vm.newChargeId()) }
    var origin by remember { mutableStateOf(base?.origin.orEmpty()) }
    var scheme by remember { mutableStateOf(base?.scheme.orEmpty()) }
    var acquirer by remember { mutableStateOf(base?.acquirer ?: Acquirer.Undefined) }
    var currency by remember { mutableStateOf((base?.currency ?: 986).toString()) }
    var preview by remember { mutableStateOf(false) }

    val cents = amount.toLongOrNull() ?: 0L
    fun request() = PaymentRequest(
        amount = cents, isQrCode = qr, operationType = op, paymentType = type, installmentType = instType, installmentNumber = installments,
        merchantChargeId = chargeId.trim(), origin = origin.trim(), originalNsu = nsu.trim(), currency = currency.toIntOrNull() ?: 986,
        acquirer = acquirer, scheme = scheme.trim(), manualEntry = typed, allowContactless = contactless,
    )
    fun send() { if (settings.previewBeforeSend) preview = true else vm.startPay(request()) }
    // atalhos para os pedidos que o serviço tem de completar (Telas da Aditum) ou recusar (Telas do integrador)
    fun preset(block: () -> Unit) { block(); chargeId = vm.newChargeId() }

    PtScaffold("Requisição livre", onBack = { vm.pop() }, actions = {
        IconButton(onClick = { preview = true }) { PtIcon(PtIcons.Json, "Ver a requisição") }
    }, bottomBar = {
        PtButton("Enviar pay() · ${Money.brl(cents)}", ::send, Modifier.fillMaxWidth(), icon = PtIcons.Send)
    }) {
        Banner("Sem validação", Tone.Info, "O pedido vai ao serviço exatamente assim. ${if (settings.integratorScreens) "Com as Telas do integrador, o que faltar volta como erro." else "Com as Telas da Aditum, o que faltar passa pelas telas do standalone antes do cartão."}")
        SectionLabel("Atalhos")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            AssistChip(onClick = { preset { amount = "0" } }, label = { Text("Sem valor") })
            AssistChip(onClick = { preset { type = PaymentType.Undefined; instType = InstallmentType.Undefined; installments = 0; qr = false } }, label = { Text("Sem forma") })
            AssistChip(onClick = { preset { type = PaymentType.Credit; instType = InstallmentType.Undefined; installments = 0; qr = false } }, label = { Text("Crédito sem parcelamento") })
            AssistChip(onClick = { preset { op = PayOperationType.PreAuthorizationCapture; nsu = "" } }, label = { Text("Captura sem NSU") })
            AssistChip(onClick = { preset { amount = "0"; op = PayOperationType.Authorization; type = PaymentType.Undefined; instType = InstallmentType.Undefined; installments = 0; qr = false; nsu = "" } }, label = { Text("Tudo em branco") })
        }

        SectionLabel("Valor (amount, centavos)")
        PtField(amount, { amount = it.filter(Char::isDigit).take(12) }, "amount", keyboard = KeyboardType.Number, supporting = Money.brl(cents))
        SectionLabel("Operação (operationType)")
        ChoiceChips(PayOperationType.values().map { it to it.name }, op, { op = it })
        PtField(nsu, { nsu = it.filter(Char::isDigit) }, "originalNsu", keyboard = KeyboardType.Number, supporting = "NSU da pré-autorização (captura e incremento)")
        SectionLabel("Forma de pagamento (paymentType)")
        ChoiceChips(PaymentType.values().map { it to it.name }, type, { type = it })
        SwitchRow("QR Code", qr, { qr = it }, "isQrCode (Pix e carteiras)")
        PtField(scheme, { scheme = it.take(40) }, "scheme", supporting = "Esquema da carteira/QR, quando houver")
        SectionLabel("Parcelamento (installmentType)")
        ChoiceChips(InstallmentType.values().map { it to it.name }, instType, { instType = it })
        StepperRow(installments, 0, 99, { installments = it }, "installmentNumber: $installments")
        SectionLabel("Cartão")
        SwitchRow("Aproximação", contactless, { contactless = it }, "allowContactless")
        SwitchRow("Venda digitada", typed, { typed = it }, "manualEntry")
        SectionLabel("Adquirente (acquirer)")
        ChoiceChips(Acquirer.values().map { it to it.name }, acquirer, { acquirer = it })
        SectionLabel("Identificação")
        PtField(chargeId, { chargeId = it.take(40) }, "merchantChargeId")
        PtButton("Gerar outra referência", { chargeId = vm.newChargeId() }, kind = PtButtonKind.Text, icon = PtIcons.Refresh)
        PtField(origin, { origin = it.take(40) }, "origin")
        PtField(currency, { currency = it.filter(Char::isDigit).take(3) }, "currency", keyboard = KeyboardType.Number, supporting = "986 = BRL")
    }
    if (preview) RequestPreviewSheet(ContractJson.redacted(request()), onDismiss = { preview = false }, onSend = { preview = false; vm.startPay(request()) })
}
