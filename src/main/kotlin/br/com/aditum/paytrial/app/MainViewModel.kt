package br.com.aditum.paytrial.app

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import br.com.aditum.data.v2.enums.CardBrand
import br.com.aditum.data.v2.enums.PayOperationType
import br.com.aditum.data.v2.enums.SendReceiptType
import br.com.aditum.data.v2.model.Charge
import br.com.aditum.data.v2.model.ErrorMessage
import br.com.aditum.data.v2.model.MerchantData
import br.com.aditum.data.v2.model.PinpadMessages
import br.com.aditum.data.v2.model.TerminalInfo
import br.com.aditum.data.v2.model.cancelation.CancelationRequest
import br.com.aditum.data.v2.model.init.InitRequest
import br.com.aditum.data.v2.model.payment.PaymentRequest
import br.com.aditum.data.v2.model.report.ReportRequest
import br.com.aditum.data.v2.model.report.ReportResponse
import br.com.aditum.data.v2.model.transactions.ChargeRequest
import br.com.aditum.data.v2.model.transactions.SendReceiptRequest
import br.com.aditum.paytrial.BuildConfig
import br.com.aditum.paytrial.PayTrialApp
import br.com.aditum.paytrial.client.PaymentEvent
import br.com.aditum.paytrial.client.Res
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import java.util.UUID

/** Telas do app. [tag] vai para o rastreio (de onde partiu cada chamada). */
sealed interface Screen {
    val tag: String
    data object Connect : Screen { override val tag = "Conectar" }
    data object Home : Screen { override val tag = "Início" }
    data class Amount(val op: PayOperationType = PayOperationType.Authorization) : Screen { override val tag = "Valor" }
    data class Checkout(val amount: Long, val op: PayOperationType) : Screen { override val tag = "Pagamento" }
    data object Operation : Screen { override val tag = "Operação" }
    data object Result : Screen { override val tag = "Resultado" }
    data class Receipt(val charge: Charge) : Screen { override val tag = "Comprovante" }
    data class Cancel(val nsu: String = "") : Screen { override val tag = "Cancelar" }
    data class Lookup(val nsu: String = "") : Screen { override val tag = "Consultar" }
    data object Confirm : Screen { override val tag = "Confirmar" }
    data object Report : Screen { override val tag = "Relatório" }
    /** Requisição livre: qualquer combinação dos campos do `PaymentRequest`, sem validação ([base] = o pedido do Pagamento). */
    data class FreeRequest(val base: PaymentRequest? = null) : Screen { override val tag = "Requisição livre" }
    data object More : Screen { override val tag = "Mais" }
    data object Developer : Screen { override val tag = "Desenvolvedor" }
    data object Console : Screen { override val tag = "Console" }
}

enum class OpKind { PAY, CANCEL }

data class Step(val title: String, val message: String, val detail: String?)
data class QrState(val code: String, val totalSeconds: Int, val receivedAt: Long)

/** Operação em curso (venda ou cancelamento): passos das notificações, PIN e QR. */
data class OpState(val kind: OpKind, val amount: Long, val title: String, val steps: List<Step> = emptyList(), val pinLength: Int? = null, val qr: QrState? = null)

/** Resultado da última operação. [traceId] aponta a chamada no rastreio (abas Requisição/Resposta). */
data class ResultState(
    val kind: OpKind, val approved: Boolean, val charge: Charge?, val errors: List<ErrorMessage>, val traceId: Long?,
    val amount: Long, val paymentRequest: PaymentRequest? = null, val failure: String? = null,
    val confirmed: Boolean = false, val undone: Boolean = false,
)

/**
 * Estado do Aditum Pay Trial: navegação, sessão com o estabelecimento (depois do `init`), operação em curso e
 * resultado. Um só ViewModel da activity, que sobrevive à rotação no meio da venda (o original perdia pedido e
 * callbacks). Toda chamada ao serviço passa pelo [client].
 */
class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val pt = app as PayTrialApp
    val client = pt.client
    val trace = pt.trace
    val settings = pt.settings

    var stack by mutableStateOf(listOf<Screen>(Screen.Connect)); private set
    val current: Screen get() = stack.last()

    var merchant by mutableStateOf<MerchantData?>(null); private set
    var terminal by mutableStateOf<TerminalInfo?>(null); private set
    var brands by mutableStateOf<List<CardBrand>>(emptyList()); private set
    var initializing by mutableStateOf(false); private set
    var initError by mutableStateOf<String?>(null); private set

    var op by mutableStateOf<OpState?>(null); private set
    var result by mutableStateOf<ResultState?>(null); private set
    val recent = mutableStateListOf<Charge>()
    var pendingCount by mutableStateOf(0); private set
    /** Chamada em curso fora de operação (consulta, confirmação, impressão…), para travar botões. */
    var busy by mutableStateOf<String?>(null); private set
    /** Última `notification` do serviço, de qualquer chamada (tabelas, init…): a barra de processo mostra a mais recente. */
    var notice by mutableStateOf<PaymentEvent.Notification?>(null); private set

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val messages: SharedFlow<String> get() = _messages
    fun say(text: String) { _messages.tryEmit(text) }

    init {
        viewModelScope.launch { client.events.collect { e -> if (e is PaymentEvent.Notification) notice = e; onEvent(e) } }
        client.screen = current.tag
    }

    // ---------------------------------------------------------------- navegação

    fun push(s: Screen) { stack = stack + s; client.screen = s.tag }
    fun replace(s: Screen) { stack = stack.dropLast(1) + s; client.screen = s.tag }
    fun pop() { if (stack.size > 1) { stack = stack.dropLast(1); client.screen = current.tag } }
    fun home() { stack = listOf(Screen.Home); client.screen = Screen.Home.tag; refreshPending() }
    private fun reset(s: Screen) { stack = listOf(s); client.screen = s.tag }

    // ---------------------------------------------------------------- sessão

    fun init() {
        val s = settings.state.value
        initializing = true; initError = null
        viewModelScope.launch {
            val req = InitRequest(
                applicationToken = BuildConfig.PARTNER_TOKEN, applicationName = "Aditum Pay Trial", applicationVersion = BuildConfig.VERSION_NAME,
                activationCode = s.activationCode.filter(Char::isLetterOrDigit).ifBlank { null },
                pinpadMessages = PinpadMessages(approvedMessage = s.approvedMessage.ifBlank { null }, declinedMessage = s.declinedMessage.ifBlank { null },
                    initializationMessage = s.initializationMessage.ifBlank { null }, mainMessage = s.mainMessage.ifBlank { null }, processingMessage = s.processingMessage.ifBlank { null }),
                useOnlySdk = s.integratorScreens,
            )
            when (val r = client.init(req)) {
                is Res.Ok -> if (r.value.initialized) {
                    terminal = r.value.terminalInfo; brands = r.value.availableBrands.orEmpty()
                    loadMerchant(); reset(Screen.Home); refreshPending()
                    // inicializado com aviso (ex.: pediu as telas da Aditum num APK sem elas e o serviço seguiu com as do integrador)
                    r.value.errors?.firstOrNull()?.let { say("Aviso do serviço: " + errorText(it)) }
                } else initError = r.value.errors?.firstOrNull()?.let { errorText(it) } ?: "O serviço não inicializou o terminal"
                is Res.Fail -> initError = r.message
            }
            initializing = false
        }
    }

    suspend fun loadMerchant(): Boolean = when (val r = client.merchantData()) { is Res.Ok -> { merchant = r.value; true }; is Res.Fail -> { say(r.message); false } }
    fun reloadMerchant() = viewModelScope.launch { if (loadMerchant()) say("Estabelecimento recarregado") }

    fun refreshPending() = viewModelScope.launch { (client.pendingTransactions() as? Res.Ok)?.let { pendingCount = it.value.size } }

    // ---------------------------------------------------------------- venda e cancelamento

    fun newChargeId(): String = "PED-" + UUID.randomUUID().toString().substring(0, 8).uppercase()

    fun startPay(req: PaymentRequest) {
        op = OpState(OpKind.PAY, req.amount, Labels.operation(req.operationType))
        push(Screen.Operation)
        viewModelScope.launch {
            val r = client.pay(req)
            val traceId = trace.entries.value.lastOrNull { it.method == "pay" }?.id
            result = when (r) {
                is Res.Ok -> ResultState(OpKind.PAY, r.value.isApproved, r.value.charge, r.value.errors.orEmpty(), traceId, req.amount, req)
                is Res.Fail -> ResultState(OpKind.PAY, false, null, emptyList(), traceId, req.amount, req, failure = r.message)
            }
            result?.charge?.takeIf { result?.approved == true }?.let { c ->
                recent.add(0, c); if (recent.size > 5) recent.removeAt(recent.lastIndex)
                // no App2App a confirmação é do integrador (como no TEF desktop): aprovada fica pendente até confirmTransaction.
                // Quem diz se está pendente é o serviço; sem resposta, trata como pendente e oferece Confirmar.
                val pending = (client.pendingTransactions() as? Res.Ok)?.value?.also { pendingCount = it.size }?.any { it.nsu == c.nsu } ?: true
                result = result?.copy(confirmed = !pending)
            }
            op = null; replaceOperationWith(Screen.Result)
        }
    }

    /** Dois `pay` com o mesmo pedido a 50 ms: reproduz a trava de operação simultânea (erro −1994). */
    fun duplicatePayTest() {
        val req = PaymentRequest(amount = 100, paymentType = br.com.aditum.data.v2.enums.PaymentType.Credit, installmentType = br.com.aditum.data.v2.enums.InstallmentType.None, installmentNumber = 1, merchantChargeId = newChargeId())
        viewModelScope.launch { launch { client.pay(req) }; delay(50); val r = client.pay(req); say(when (r) { is Res.Ok -> r.value.errors?.firstOrNull()?.let { "Segundo pay: ${errorText(it)}" } ?: "Segundo pay aceito"; is Res.Fail -> r.message }) }
    }

    fun startCancel(req: CancelationRequest, amount: Long) {
        op = OpState(OpKind.CANCEL, amount, if (req.isReversal) "Desfazimento" else "Cancelamento")
        push(Screen.Operation)
        viewModelScope.launch {
            val r = client.cancel(req)
            val traceId = trace.entries.value.lastOrNull { it.method == "cancel" }?.id
            val previous = result
            result = when (r) {
                is Res.Ok -> ResultState(OpKind.CANCEL, r.value.canceled, r.value.charge, r.value.errors.orEmpty(), traceId, amount, undone = req.isReversal && r.value.canceled)
                is Res.Fail -> ResultState(OpKind.CANCEL, false, null, emptyList(), traceId, amount, failure = r.message)
            }
            // desfazer a partir do resultado: o mesmo resultado volta marcado como desfeito (só se for a mesma venda)
            if (req.isReversal && previous?.charge?.nsu == req.nsu && r is Res.Ok && r.value.canceled) result = previous.copy(undone = true, traceId = traceId)
            op = null; replaceOperationWith(Screen.Result); refreshPending()
        }
    }

    private fun replaceOperationWith(s: Screen) { stack = stack.filterNot { it is Screen.Operation || it is Screen.Result || it is Screen.Checkout || it is Screen.Amount } + s; client.screen = s.tag }

    fun abort() = client.abortOperation()

    private fun onEvent(e: PaymentEvent) {
        val o = op ?: return
        op = when (e) {
            is PaymentEvent.Notification -> o.copy(steps = o.steps + Step(Labels.step(e.status), e.message, "notification · ${e.status?.name} · ${e.command?.name}"), pinLength = if (e.status == br.com.aditum.data.v2.enums.TransactionStatus.GettingPin) o.pinLength else null)
            is PaymentEvent.Pin -> o.copy(pinLength = e.length)
            is PaymentEvent.Qr -> o.copy(qr = QrState(e.code, e.expirationSeconds, e.at), steps = o.steps + Step("QR Code gerado", "Aguardando leitura", "qrCodeGenerated · ${e.expirationSeconds} s"))
        }
    }

    // ---------------------------------------------------------------- pós-venda

    fun confirm(nsu: String, then: (Boolean) -> Unit = {}) {
        busy = "confirmTransaction"
        viewModelScope.launch {
            val r = client.confirmTransaction(nsu)
            val ok = r is Res.Ok && r.value
            // o contrato só devolve Boolean: no `false`, as pendências dizem se a venda ainda espera confirmação
            val stillPending = if (ok || r is Res.Fail) null else (client.pendingTransactions() as? Res.Ok)?.value?.any { it.nsu == nsu }
            busy = null
            when {
                ok -> { markConfirmed(nsu); say("Venda confirmada") }
                r is Res.Fail -> say(r.message)
                stillPending == false -> { markConfirmed(nsu); say("O serviço respondeu false, mas a venda já não estava pendente: já tinha sido confirmada") }
                stillPending == true -> say("O serviço respondeu false e a venda continua pendente: tente de novo")
                else -> say("O serviço respondeu false")
            }
            refreshPending(); then(ok)
        }
    }

    private fun markConfirmed(nsu: String) { if (result?.charge?.nsu == nsu) result = result?.copy(confirmed = true) }

    /** Confirma uma lista, uma depois da outra (o serviço trata uma operação por vez). */
    fun confirmAll(nsus: List<String>, done: () -> Unit) {
        busy = "confirmTransaction"
        viewModelScope.launch {
            val ok = nsus.count { (client.confirmTransaction(it) as? Res.Ok)?.value == true }
            busy = null; say("$ok de ${nsus.size} confirmadas"); refreshPending(); done()
        }
    }

    suspend fun findCharge(nsu: String?, merchantChargeId: String?): Res<Charge?> {
        busy = "charge"
        val r = client.charge(ChargeRequest(nsu = nsu?.ifBlank { null }, merchantChargeId = merchantChargeId?.ifBlank { null })); busy = null
        return when (r) { is Res.Ok -> Res.Ok(r.value.charge); is Res.Fail -> r }
    }

    suspend fun pendings(): Res<List<Charge>> { busy = "pendingTransactions"; return client.pendingTransactions().also { busy = null; (it as? Res.Ok)?.let { p -> pendingCount = p.value.size } } }

    suspend fun report(req: ReportRequest): Res<ReportResponse> { busy = "generateReport"; return client.generateReport(req).also { busy = null } }

    /** Imprime as vias escolhidas pelo `IPrinterSdk`, uma depois da outra. */
    fun print(charge: Charge, merchantVia: Boolean, customerVia: Boolean) {
        busy = "print"
        viewModelScope.launch {
            var msg = "Comprovante impresso"
            for (lines in listOfNotNull(if (merchantVia) charge.merchantReceipt else null, if (customerVia) charge.cardholderReceipt else null)) {
                if (lines.isEmpty()) { msg = "A transação não trouxe essa via"; continue }
                when (val r = client.print(ReceiptPrinter.render(lines))) {
                    is Res.Ok -> if (r.value != br.com.aditum.data.v2.enums.PrintStatus.Ok) { msg = Labels.printStatus(r.value); break }
                    is Res.Fail -> { msg = if ("impressora" in r.message) "Este terminal não tem impressora: envie o comprovante" else r.message; break }
                }
            }
            busy = null; say(msg)
        }
    }

    fun sendReceipt(charge: Charge, method: SendReceiptType, to: String, done: (Boolean) -> Unit) {
        busy = "sendReceipt"
        viewModelScope.launch {
            val r = client.sendReceipt(SendReceiptRequest(method, to.trim(), charge.transactionId ?: charge.nsu.orEmpty())); busy = null
            val ok = r is Res.Ok && r.value
            say(if (ok) "Comprovante enviado" else (r as? Res.Fail)?.message ?: "O serviço não enviou o comprovante"); done(ok)
        }
    }

    fun updateTables() { busy = "updateEmvTables"; viewModelScope.launch { val r = client.updateEmvTables(); busy = null; say(if (r is Res.Ok && r.value) "Tabelas atualizadas" else (r as? Res.Fail)?.message ?: "As tabelas não foram atualizadas") } }

    fun deactivate() {
        busy = "deactivate"
        viewModelScope.launch {
            val r = client.deactivate(); busy = null
            if (r is Res.Ok && r.value) { merchant = null; terminal = null; brands = emptyList(); recent.clear(); result = null; reset(Screen.Connect); say("Terminal desativado") }
            else say((r as? Res.Fail)?.message ?: "O serviço não desativou o terminal")
        }
    }

    fun reconnect() { client.disconnect(); client.connect() }

    companion object {
        fun errorText(e: ErrorMessage): String = listOfNotNull(e.message?.takeIf { it.isNotBlank() }, e.description?.takeIf { it.isNotBlank() && it != e.message }).joinToString(" · ").ifBlank { "Erro" } + (e.code?.let { " (código $it)" } ?: "")
    }
}
