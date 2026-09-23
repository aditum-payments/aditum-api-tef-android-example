package br.com.aditum.paytrial

import android.app.Application
import br.com.aditum.paytrial.app.SettingsStore
import br.com.aditum.paytrial.client.AditumClient
import br.com.aditum.paytrial.client.TraceLog

/** Um cliente e um rastreio por processo; a ligação ao serviço começa com o app e se mantém. */
class PayTrialApp : Application() {
    val trace = TraceLog()
    val client by lazy { AditumClient(this, trace) }
    val settings by lazy { SettingsStore(this) }
    override fun onCreate() { super.onCreate(); client.connect() }
}
