package br.com.aditum.paytrial.app

import android.content.Context
import br.com.aditum.paytrial.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Preferências do Aditum Pay Trial, guardadas no aparelho. */
data class PayTrialSettings(
    val activationCode: String = "",
    /** true = "Telas do integrador" (`useOnlySdk`): o app desenha PIN, QR, dado digitado e menu. */
    val integratorScreens: Boolean = false,
    val mainMessage: String = "ADITUM PAY TRIAL",
    val processingMessage: String = "",
    val approvedMessage: String = "",
    val declinedMessage: String = "",
    val initializationMessage: String = "",
    /** Modo de integração: chip do inspetor, abas cruas e payload dos callbacks. Ligado de fábrica no build debug. */
    val integrationMode: Boolean = BuildConfig.DEBUG,
    /** Mostra a requisição e pede "Enviar" antes de cada pagamento. */
    val previewBeforeSend: Boolean = false,
    /** Comprovantes linha a linha no JSON (já censurados). */
    val receiptsInJson: Boolean = true,
)

class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("paytrial", Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(load())
    val state: StateFlow<PayTrialSettings> get() = _state

    private fun load() = PayTrialSettings(
        activationCode = prefs.getString("activationCode", "") ?: "",
        integratorScreens = prefs.getBoolean("integratorScreens", false),
        mainMessage = prefs.getString("mainMessage", "ADITUM PAY TRIAL") ?: "",
        processingMessage = prefs.getString("processingMessage", "") ?: "",
        approvedMessage = prefs.getString("approvedMessage", "") ?: "",
        declinedMessage = prefs.getString("declinedMessage", "") ?: "",
        initializationMessage = prefs.getString("initializationMessage", "") ?: "",
        integrationMode = prefs.getBoolean("integrationMode", BuildConfig.DEBUG),
        previewBeforeSend = prefs.getBoolean("previewBeforeSend", false),
        receiptsInJson = prefs.getBoolean("receiptsInJson", true),
    )

    fun update(f: (PayTrialSettings) -> PayTrialSettings) {
        val n = f(_state.value); _state.value = n
        prefs.edit().putString("activationCode", n.activationCode).putBoolean("integratorScreens", n.integratorScreens)
            .putString("mainMessage", n.mainMessage).putString("processingMessage", n.processingMessage).putString("approvedMessage", n.approvedMessage)
            .putString("declinedMessage", n.declinedMessage).putString("initializationMessage", n.initializationMessage)
            .putBoolean("integrationMode", n.integrationMode).putBoolean("previewBeforeSend", n.previewBeforeSend).putBoolean("receiptsInJson", n.receiptsInJson).apply()
    }
}
