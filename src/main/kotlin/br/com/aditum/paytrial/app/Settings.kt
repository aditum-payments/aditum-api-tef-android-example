package br.com.aditum.paytrial.app

import android.content.Context
import br.com.aditum.paytrial.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Preferências do Aditum Pay Trial, guardadas no aparelho. */
data class PayTrialSettings(
    val activationCode: String = "",
    /** true = "Telas do integrador" (`useOnlySdk`): o app desenha PIN, QR, dado digitado e menu. Sempre true no build TefSDK. */
    val integratorScreens: Boolean = !BuildConfig.ADITUM_SCREENS,
    /**
     * Vazio de fábrica: o serviço deixa no pinpad parado o logo do estabelecimento, ou o nome dele (como o app atual).
     * Preenchida, a mensagem do integrador vence o nome — o logo continua tendo prioridade.
     */
    val mainMessage: String = "",
    val processingMessage: String = "",
    val approvedMessage: String = "",
    val declinedMessage: String = "",
    val initializationMessage: String = "",
    /** Modo de integração: chip do inspetor, abas cruas e payload dos callbacks. Ligado de fábrica no build debug; nunca na distribuição. */
    val integrationMode: Boolean = BuildConfig.DEBUG && !BuildConfig.DISTRIBUTION,
    /** Mostra a requisição e pede "Enviar" antes de cada pagamento. */
    val previewBeforeSend: Boolean = false,
    /** Comprovantes linha a linha no JSON (já censurados). */
    val receiptsInJson: Boolean = true,
    /** Onde o comprovante sai: "TERMINAL" (`IPrinterSdk` do terminal) ou "USB" (impressora térmica externa, ESC/POS). */
    val printerTarget: String = "TERMINAL",
    /** Impressora USB escolhida (VID:PID, ex.: "20d1:7008" da Elgin i9). */
    val usbPrinter: String = "",
    /** Papel da impressora externa: "MM80" (576 pontos) ou "MM58" (384). */
    val printerPaper: String = "MM80",
) {
    val externalPrinter: Boolean get() = printerTarget == "USB" && usbPrinter.isNotBlank()
}

class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("paytrial", Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(load())
    val state: StateFlow<PayTrialSettings> get() = _state

    private fun load() = PayTrialSettings(
        activationCode = prefs.getString("activationCode", "") ?: "",
        integratorScreens = !BuildConfig.ADITUM_SCREENS || prefs.getBoolean("integratorScreens", false),
        // "ADITUM PAY TRIAL" era o padrão antigo: quem nunca mudou passa a ver o estabelecimento no pinpad
        mainMessage = (prefs.getString("mainMessage", "") ?: "").takeUnless { it == "ADITUM PAY TRIAL" }.orEmpty(),
        processingMessage = prefs.getString("processingMessage", "") ?: "",
        approvedMessage = prefs.getString("approvedMessage", "") ?: "",
        declinedMessage = prefs.getString("declinedMessage", "") ?: "",
        initializationMessage = prefs.getString("initializationMessage", "") ?: "",
        // distribuição (lojista): as ferramentas de integração ficam desligadas por mais que a preferência guardada diga o contrário
        integrationMode = !BuildConfig.DISTRIBUTION && prefs.getBoolean("integrationMode", BuildConfig.DEBUG),
        previewBeforeSend = !BuildConfig.DISTRIBUTION && prefs.getBoolean("previewBeforeSend", false),
        receiptsInJson = prefs.getBoolean("receiptsInJson", true),
        printerTarget = prefs.getString("printerTarget", "TERMINAL") ?: "TERMINAL",
        usbPrinter = prefs.getString("usbPrinter", "") ?: "",
        printerPaper = prefs.getString("printerPaper", "MM80") ?: "MM80",
    )

    fun update(f: (PayTrialSettings) -> PayTrialSettings) {
        val n = f(_state.value); _state.value = n
        prefs.edit().putString("activationCode", n.activationCode).putBoolean("integratorScreens", n.integratorScreens)
            .putString("mainMessage", n.mainMessage).putString("processingMessage", n.processingMessage).putString("approvedMessage", n.approvedMessage)
            .putString("declinedMessage", n.declinedMessage).putString("initializationMessage", n.initializationMessage)
            .putBoolean("integrationMode", n.integrationMode).putBoolean("previewBeforeSend", n.previewBeforeSend).putBoolean("receiptsInJson", n.receiptsInJson)
            .putString("printerTarget", n.printerTarget).putString("usbPrinter", n.usbPrinter).putString("printerPaper", n.printerPaper).apply()
    }
}
