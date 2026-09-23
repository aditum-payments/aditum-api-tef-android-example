package br.com.aditum.paytrial.client

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.os.IBinder
import android.os.Process
import android.util.Log
import br.com.aditum.IAditumSdkService
import br.com.aditum.data.v2.IPaymentCallback
import br.com.aditum.data.v2.enums.AbecsCommands
import br.com.aditum.data.v2.enums.PrintStatus
import br.com.aditum.data.v2.enums.TransactionStatus
import br.com.aditum.data.v2.model.Charge
import br.com.aditum.data.v2.model.MerchantData
import br.com.aditum.data.v2.model.callbacks.GetClearDataFinishedCallback
import br.com.aditum.data.v2.model.callbacks.GetClearDataRequest
import br.com.aditum.data.v2.model.callbacks.GetMenuSelectionFinishedCallback
import br.com.aditum.data.v2.model.callbacks.GetMenuSelectionRequest
import br.com.aditum.data.v2.model.cancelation.CancelationRequest
import br.com.aditum.data.v2.model.cancelation.CancelationResponse
import br.com.aditum.data.v2.model.cancelation.CancelationResponseCallback
import br.com.aditum.data.v2.model.deactivation.DeactivationResponseCallback
import br.com.aditum.data.v2.model.init.InitRequest
import br.com.aditum.data.v2.model.init.InitResponse
import br.com.aditum.data.v2.model.init.InitResponseCallback
import br.com.aditum.data.v2.model.init.UpdateEmvTablesCallback
import br.com.aditum.data.v2.model.payment.PaymentRequest
import br.com.aditum.data.v2.model.payment.PaymentResponse
import br.com.aditum.data.v2.model.payment.PaymentResponseCallback
import br.com.aditum.data.v2.model.report.ReportRequest
import br.com.aditum.data.v2.model.report.ReportResponse
import br.com.aditum.data.v2.model.report.ReportResponseCallback
import br.com.aditum.data.v2.model.transactions.ChargeRequest
import br.com.aditum.data.v2.model.transactions.ChargeResponse
import br.com.aditum.data.v2.model.transactions.ChargeResponseCallback
import br.com.aditum.data.v2.model.transactions.ConfirmTransactionCallback
import br.com.aditum.data.v2.model.transactions.PendingTransactionsCallback
import br.com.aditum.data.v2.model.transactions.SendReceiptRequest
import br.com.aditum.device.callbacks.IPrintStatusCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Resultado de uma chamada ao serviço: o valor, ou a mensagem do que deu errado (serviço caído, tempo esgotado…). */
sealed interface Res<out T> {
    data class Ok<T>(val value: T) : Res<T>
    data class Fail(val message: String) : Res<Nothing>
}

/** Eventos do [IPaymentCallback] que não pedem resposta. */
sealed interface PaymentEvent {
    val at: Long
    data class Notification(val message: String, val status: TransactionStatus?, val command: AbecsCommands?, override val at: Long = System.currentTimeMillis()) : PaymentEvent
    data class Pin(val message: String, val length: Int, override val at: Long = System.currentTimeMillis()) : PaymentEvent
    data class Qr(val code: String, val expirationSeconds: Int, override val at: Long = System.currentTimeMillis()) : PaymentEvent
}

/** Pedidos do serviço que o integrador responde (só chegam com "Telas do integrador", `useOnlySdk = true`). */
sealed interface Prompt {
    class ClearData(val request: GetClearDataRequest, val answer: (String) -> Unit, val abort: () -> Unit) : Prompt
    class Menu(val request: GetMenuSelectionRequest, val choose: (Int) -> Unit, val abort: () -> Unit) : Prompt
}

/**
 * Cliente do serviço App2App da Aditum — o código que um integrador escreve sobre o `.aar` `AditumSdkIntegration`.
 * Não depende de nada do app de exemplo além do rastreio.
 *
 * - **Conexão:** `startForegroundService` + `bindService` na action `br.com.aditum.AditumSdkService` do pacote
 *   `br.com.aditum.smartpostef`; detecta a morte do serviço (`linkToDeath`) e religa sozinho, com espera crescente.
 * - **Callbacks:** um único [IPaymentCallback], registrado de novo a cada conexão (o serviço guarda um por cliente).
 *   Notificações, PIN e QR viram [events]; pedidos de dado digitado e de menu viram [prompt]. Tudo chega nas threads
 *   do Binder; os fluxos são seguros entre threads e a UI coleta na principal.
 * - **Chamadas:** cada método do contrato vira uma função `suspend` com tempo limite, executada fora da thread de UI,
 *   e cada chamada fica no [trace] com requisição, resposta (censuradas) e duração.
 */
class AditumClient(context: Context, val trace: TraceLog) {
    companion object {
        const val SERVICE_PACKAGE = "br.com.aditum.smartpostef"
        const val SERVICE_ACTION = "br.com.aditum.AditumSdkService"
        const val BIND_PERMISSION = "br.com.aditum.smartpostef.permission.BIND_TEF"
        private const val TAG = "PayTrial"
        private const val MIN = 60_000L
    }

    sealed interface Connection {
        data object Disconnected : Connection
        data object Connecting : Connection
        data object Connected : Connection
        data class Unavailable(val reason: String) : Connection
    }

    private val app = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _connection = MutableStateFlow<Connection>(Connection.Disconnected)
    val connection: StateFlow<Connection> get() = _connection
    private val _events = MutableSharedFlow<PaymentEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<PaymentEvent> get() = _events
    private val _prompt = MutableStateFlow<Prompt?>(null)
    val prompt: StateFlow<Prompt?> get() = _prompt

    /** Tela em uso, só para o rastreio saber de onde partiu cada chamada (chip do inspetor). */
    @Volatile var screen: String = ""

    @Volatile private var service: IAditumSdkService? = null
    private var bound = false
    private var wanted = false
    private var retryJob: Job? = null
    private var backoffMs = 1_000L

    private val death = IBinder.DeathRecipient {
        Log.w(TAG, "serviço da Aditum morreu; religando")
        service = null; _connection.value = Connection.Connecting
        scope.launch { unbindQuietly(); scheduleReconnect() }
    }

    private val connectionCallback = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val svc = IAditumSdkService.Stub.asInterface(binder) ?: return
            runCatching { binder?.linkToDeath(death, 0) }
            service = svc; backoffMs = 1_000L
            runCatching { svc.registerPaymentCallback(paymentCallback) }.onFailure { Log.w(TAG, "registerPaymentCallback falhou", it) }
            trace.callback("serviceConnected", screen, "{\"package\": \"$SERVICE_PACKAGE\", \"registerPaymentCallback\": true}")
            _connection.value = Connection.Connected
        }
        override fun onServiceDisconnected(name: ComponentName?) {
            service = null; _connection.value = Connection.Connecting
            trace.callback("serviceDisconnected", screen, null)
        }
        override fun onBindingDied(name: ComponentName?) { service = null; scope.launch { unbindQuietly(); scheduleReconnect() } }
        override fun onNullBinding(name: ComponentName?) { _connection.value = Connection.Unavailable("O serviço recusou a ligação (onBind devolveu null).") }
    }

    /** Liga ao serviço e mantém ligado (religa sozinho se cair) até [disconnect]. */
    fun connect() {
        wanted = true
        if (bound) return
        _connection.value = Connection.Connecting
        val intent = Intent(SERVICE_ACTION).setPackage(SERVICE_PACKAGE)
        // o serviço é de primeiro plano: iniciar antes de ligar mantém-no vivo entre operações, como o app atual
        runCatching { if (Build.VERSION.SDK_INT >= 26) app.startForegroundService(intent) else app.startService(intent) }
            .onFailure { Log.w(TAG, "startService falhou: ${it.message}") }
        val bind = runCatching { app.bindService(intent, connectionCallback, Context.BIND_AUTO_CREATE) }
        bound = bind.getOrDefault(false)
        if (!bound) {
            _connection.value = Connection.Unavailable(diagnose(intent, bind.exceptionOrNull()))
            scheduleReconnect()
        }
    }

    /**
     * Por que a ligação falhou, na ordem em que um integrador tropeça: serviço ausente, app da Aditum na versão
     * standalone (mesmo pacote, sem o serviço) ou permissão `BIND_TEF` não concedida. A permissão é criada pelo APK
     * do serviço, e o Android não a concede a um app instalado **antes** dele: nesse caso só reinstalar o integrador.
     */
    private fun diagnose(intent: Intent, error: Throwable?): String {
        val pm = app.packageManager
        val installed = runCatching { pm.getPackageInfo(SERVICE_PACKAGE, 0) }.isSuccess
        val retry = " Este app tenta de novo sozinho."
        return when {
            !installed -> "App2App da Aditum não instalado neste terminal ($SERVICE_PACKAGE). Instale o APK App2App.$retry"
            pm.queryIntentServices(intent, 0).isEmpty() -> "O app da Aditum deste terminal é a versão standalone, sem o serviço App2App. Troque pelo APK App2App.$retry"
            app.checkPermission(BIND_PERMISSION, Process.myPid(), Process.myUid()) != PackageManager.PERMISSION_GRANTED ->
                "Sem a permissão BIND_TEF: este app foi instalado antes do App2App, e o Android não concede a permissão depois. Reinstale o Aditum Pay Trial."
            else -> "O serviço recusou a ligação${error?.message?.let { " ($it)" } ?: ""}.$retry"
        }
    }

    fun disconnect() { wanted = false; retryJob?.cancel(); scope.launch { unbindQuietly() }; _connection.value = Connection.Disconnected }

    private fun unbindQuietly() { if (bound) runCatching { app.unbindService(connectionCallback) }; bound = false; service = null }

    private fun scheduleReconnect() {
        if (!wanted) return
        retryJob?.cancel()
        retryJob = scope.launch { delay(backoffMs); backoffMs = (backoffMs * 2).coerceAtMost(30_000L); connect() }
    }

    // ---------------------------------------------------------------- callbacks do serviço

    private val paymentCallback = object : IPaymentCallback.Stub() {
        override fun notification(message: String?, transactionStatus: TransactionStatus?, command: AbecsCommands?) {
            trace.callback("notification", screen, "{\"message\": ${q(message)}, \"transactionStatus\": ${q(transactionStatus?.name)}, \"command\": ${q(command?.name)}}")
            _events.tryEmit(PaymentEvent.Notification(message.orEmpty(), transactionStatus, command))
        }
        override fun pinNotification(message: String?, length: Int) {
            trace.callback("pinNotification", screen, "{\"message\": ${q(message)}, \"length\": $length}")
            _events.tryEmit(PaymentEvent.Pin(message.orEmpty(), length))
        }
        override fun startGetClearData(request: GetClearDataRequest?, callback: GetClearDataFinishedCallback?) {
            trace.callback("startGetClearData", screen, ContractJson.redacted(request))
            val req = request ?: GetClearDataRequest()
            _prompt.value = Prompt.ClearData(req,
                answer = { v -> _prompt.value = null; trace.callback("getClearDataFinished", screen, Redactor.typed(v)); scope.launch(Dispatchers.IO) { runCatching { callback?.getClearDataFinished(v) } } },
                abort = { _prompt.value = null; trace.callback("getClearDataAborted", screen, null); scope.launch(Dispatchers.IO) { runCatching { callback?.getClearDataAborted() } } })
        }
        override fun startGetMenuSelection(request: GetMenuSelectionRequest?, callback: GetMenuSelectionFinishedCallback?) {
            trace.callback("startGetMenuSelection", screen, ContractJson.redacted(request))
            val req = request ?: GetMenuSelectionRequest()
            _prompt.value = Prompt.Menu(req,
                choose = { i -> _prompt.value = null; trace.callback("getMenuSelectionFinished", screen, "{\"index\": $i}"); scope.launch(Dispatchers.IO) { runCatching { callback?.getMenuSelectionFinished(i) } } },
                abort = { _prompt.value = null; trace.callback("getMenuSelectionAborted", screen, null); scope.launch(Dispatchers.IO) { runCatching { callback?.getMenuSelectionAborted() } } })
        }
        override fun qrCodeGenerated(qrCode: String?, expirationTime: Int) {
            trace.callback("qrCodeGenerated", screen, "{\"qrCode\": \"(${qrCode?.length ?: 0} caracteres)\", \"expirationTime\": $expirationTime}")
            _events.tryEmit(PaymentEvent.Qr(qrCode.orEmpty(), expirationTime))
        }
    }

    // ---------------------------------------------------------------- chamadas

    private suspend fun <T> call(method: String, request: Any?, timeoutMs: Long, invoke: (IAditumSdkService, (T) -> Unit) -> Unit): Res<T> {
        val id = trace.begin(method, screen, if (request == null) null else ContractJson.redacted(request))
        val svc = service ?: return Res.Fail("Serviço da Aditum desconectado").also { trace.end(id, null, it.message) }
        return try {
            val value = withTimeout(timeoutMs) {
                withContext(Dispatchers.IO) {
                    suspendCancellableCoroutine<T> { cont ->
                        try { invoke(svc) { v -> if (cont.isActive) cont.resume(v) } } catch (e: Exception) { if (cont.isActive) cont.resumeWithException(e) }
                    }
                }
            }
            trace.end(id, ContractJson.redacted(value), null); Res.Ok(value)
        } catch (e: TimeoutCancellationException) {
            Res.Fail("Sem resposta do serviço em ${timeoutMs / 1000} s").also { trace.end(id, null, it.message) }
        } catch (e: Exception) {
            Log.w(TAG, "$method falhou", e)
            Res.Fail("Falha ao chamar $method: ${e.message ?: e.javaClass.simpleName}").also { trace.end(id, null, it.message) }
        }
    }

    suspend fun init(req: InitRequest): Res<InitResponse> = call("init", req, 4 * MIN) { s, done -> s.init(req, object : InitResponseCallback.Stub() { override fun onResponse(r: InitResponse?) = done(r ?: InitResponse()) }) }
    suspend fun pay(req: PaymentRequest): Res<PaymentResponse> = call("pay", req, 5 * MIN) { s, done -> s.pay(req, object : PaymentResponseCallback.Stub() { override fun onResponse(r: PaymentResponse?) = done(r ?: PaymentResponse()) }) }
    suspend fun cancel(req: CancelationRequest): Res<CancelationResponse> = call("cancel", req, 5 * MIN) { s, done -> s.cancel(req, object : CancelationResponseCallback.Stub() { override fun onResponse(r: CancelationResponse?) = done(r ?: CancelationResponse()) }) }
    suspend fun charge(req: ChargeRequest): Res<ChargeResponse> = call("charge", req, MIN / 2) { s, done -> s.charge(req, object : ChargeResponseCallback.Stub() { override fun onResponse(r: ChargeResponse?) = done(r ?: ChargeResponse()) }) }
    suspend fun pendingTransactions(): Res<List<Charge>> = call("pendingTransactions", null, MIN / 2) { s, done -> s.pendingTransactions(object : PendingTransactionsCallback.Stub() { override fun onResponse(r: MutableList<Charge>?) = done(r.orEmpty()) }) }
    suspend fun confirmTransaction(nsu: String): Res<Boolean> = call("confirmTransaction", mapOf("nsu" to nsu), MIN / 2) { s, done -> s.confirmTransaction(nsu, object : ConfirmTransactionCallback.Stub() { override fun onResponse(ok: Boolean) = done(ok) }) }
    suspend fun updateEmvTables(): Res<Boolean> = call("updateEmvTables", null, 5 * MIN) { s, done -> s.updateEmvTables(object : UpdateEmvTablesCallback.Stub() { override fun onResponse(ok: Boolean) = done(ok) }) }
    suspend fun deactivate(): Res<Boolean> = call("deactivate", null, 2 * MIN) { s, done -> s.deactivate(object : DeactivationResponseCallback.Stub() { override fun onResponse(ok: Boolean) = done(ok) }) }
    suspend fun generateReport(req: ReportRequest): Res<ReportResponse> = call("generateReport", req, MIN) { s, done -> s.generateReport(req, object : ReportResponseCallback.Stub() { override fun onResponse(r: ReportResponse?) = done(r ?: ReportResponse()) }) }
    /** Síncrono no contrato: roda fora da thread de UI. */
    suspend fun merchantData(): Res<MerchantData> = call("getMerchantData", null, MIN / 2) { s, done -> done(s.merchantData ?: MerchantData()) }
    suspend fun sendReceipt(req: SendReceiptRequest): Res<Boolean> = call("sendReceipt", req, MIN / 2) { s, done -> done(s.sendReceipt(req)) }
    /** O terminal tem impressora? O contrato responde com `getDeviceSdk().printerSdk` nulo quando não tem. */
    suspend fun hasPrinter(): Res<Boolean> = call("getDeviceSdk", null, MIN / 2) { s, done -> done(s.deviceSdk?.printerSdk != null) }
    /** Imprime um bitmap pela impressora do terminal (`getDeviceSdk().printerSdk`). */
    suspend fun print(bitmap: Bitmap): Res<PrintStatus> = call("print", mapOf("bitmap" to "${bitmap.width}x${bitmap.height}"), MIN) { s, done ->
        val printer = s.deviceSdk?.printerSdk ?: throw IllegalStateException("terminal sem impressora")
        printer.print(bitmap, object : IPrintStatusCallback.Stub() { override fun finished(status: PrintStatus?) = done(status ?: PrintStatus.Unknown) })
    }
    /** Interrompe a operação em curso. Não espera resposta: o resultado chega pela chamada interrompida. */
    fun abortOperation() {
        val id = trace.begin("abortOperation", screen, null)
        scope.launch(Dispatchers.IO) { val r = runCatching { service?.abortOperation() }; trace.end(id, "null", r.exceptionOrNull()?.message ?: if (service == null) "Serviço da Aditum desconectado" else null) }
    }

    private fun q(s: String?): String = if (s == null) "null" else org.json.JSONObject.quote(s)
}
