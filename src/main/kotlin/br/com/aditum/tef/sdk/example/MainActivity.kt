package br.com.aditum.tef.sdk.example

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import br.com.aditum.IAditumSdkService
import br.com.aditum.data.v2.IPaymentCallback
import br.com.aditum.data.v2.enums.AbecsCommands
import br.com.aditum.data.v2.enums.InstallmentType
import br.com.aditum.data.v2.enums.PaymentType
import br.com.aditum.data.v2.enums.TransactionStatus
import br.com.aditum.data.v2.model.callbacks.GetClearDataFinishedCallback
import br.com.aditum.data.v2.model.callbacks.GetClearDataRequest
import br.com.aditum.data.v2.model.callbacks.GetMenuSelectionFinishedCallback
import br.com.aditum.data.v2.model.callbacks.GetMenuSelectionRequest
import br.com.aditum.data.v2.model.cancelation.CancelationRequest
import br.com.aditum.data.v2.model.cancelation.CancelationResponse
import br.com.aditum.data.v2.model.cancelation.CancelationResponseCallback
import br.com.aditum.data.v2.model.init.InitRequest
import br.com.aditum.data.v2.model.init.InitResponse
import br.com.aditum.data.v2.model.init.InitResponseCallback
import br.com.aditum.data.v2.model.payment.PaymentRequest
import br.com.aditum.data.v2.model.payment.PaymentResponse
import br.com.aditum.data.v2.model.payment.PaymentResponseCallback

/**
 * App de exemplo do SDK App2App (substitui o `PaymentExample`): vincula ao `br.com.aditum.AditumSdkService`,
 * registra o `IPaymentCallback`, faz `init`, `pay` e `cancel` e mostra as notificações. Uma tela, sem frameworks.
 */
class MainActivity : ComponentActivity() {
    private var service: IAditumSdkService? = null
    private val log = mutableStateListOf<String>()
    private var lastNsu by mutableStateOf("")
    private var bound by mutableStateOf(false)

    private val callback = object : IPaymentCallback.Stub() {
        override fun notification(message: String?, transactionStatus: TransactionStatus?, command: AbecsCommands?) { add("↳ $transactionStatus/$command: $message") }
        override fun pinNotification(message: String?, length: Int) { add("↳ PIN $message ${"•".repeat(length)}") }
        override fun startGetClearData(req: GetClearDataRequest?, cb: GetClearDataFinishedCallback?) { add("↳ getClearData ${req?.title}: respondendo 12345678909"); cb?.getClearDataFinished("12345678909") }
        override fun startGetMenuSelection(req: GetMenuSelectionRequest?, cb: GetMenuSelectionFinishedCallback?) { add("↳ menu ${req?.title} ${req?.optionList}: escolhendo 0"); cb?.getMenuSelectionFinished(0) }
        override fun qrCodeGenerated(qrCode: String?, expirationTime: Int) { add("↳ QR gerado (${qrCode?.length} chars, ${expirationTime}s)") }
    }

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) { service = IAditumSdkService.Stub.asInterface(binder); bound = true; add("serviço conectado"); service?.registerPaymentCallback(callback) }
        override fun onServiceDisconnected(name: ComponentName?) { service = null; bound = false; add("serviço desconectado") }
    }

    private fun add(s: String) { runOnUiThread { log.add(0, s); if (log.size > 200) log.removeAt(log.lastIndex) } }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val intent = Intent("br.com.aditum.AditumSdkService").setPackage("br.com.aditum.smartpostef")
        bindService(intent, connection, Context.BIND_AUTO_CREATE)
        setContent {
            MaterialTheme {
                Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Aditum SDK · exemplo", style = MaterialTheme.typography.titleLarge)
                    Text(if (bound) "Conectado ao serviço" else "Aguardando serviço…")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { init() }, enabled = bound) { Text("init") }
                        Button(onClick = { pay(1000) }, enabled = bound) { Text("pay R$ 10,00") }
                        Button(onClick = { cancel() }, enabled = bound && lastNsu.isNotBlank()) { Text("cancel") }
                        OutlinedButton(onClick = { service?.abortOperation() }, enabled = bound) { Text("abort") }
                    }
                    HorizontalDivider()
                    androidx.compose.foundation.lazy.LazyColumn(Modifier.weight(1f)) { items(log.size) { i -> Text(log[i], style = MaterialTheme.typography.bodySmall) } }
                }
            }
        }
    }

    private fun init() {
        service?.init(InitRequest(applicationToken = "example-token", applicationName = "PaymentExample", applicationVersion = "3.0.0", useOnlySdk = true), object : InitResponseCallback.Stub() {
            override fun onResponse(response: InitResponse?) { add("init: initialized=${response?.initialized} terminal=${response?.terminalInfo?.model} erros=${response?.errors?.map { it.message }}") }
        })
    }

    private fun pay(amount: Long) {
        service?.pay(PaymentRequest(amount = amount, paymentType = PaymentType.Credit, installmentType = InstallmentType.None, installmentNumber = 1, merchantChargeId = "EX-${System.currentTimeMillis()}"), object : PaymentResponseCallback.Stub() {
            override fun onResponse(response: PaymentResponse?) { lastNsu = response?.charge?.nsu.orEmpty(); add("pay: approved=${response?.isApproved} nsu=${response?.charge?.nsu} erros=${response?.errors?.map { "${it.code} ${it.message}" }}") }
        })
    }

    private fun cancel() {
        service?.cancel(CancelationRequest(nsu = lastNsu), object : CancelationResponseCallback.Stub() {
            override fun onResponse(response: CancelationResponse?) { add("cancel: canceled=${response?.canceled} erros=${response?.errors?.map { it.message }}") }
        })
    }

    override fun onDestroy() { runCatching { unbindService(connection) }; super.onDestroy() }
}
