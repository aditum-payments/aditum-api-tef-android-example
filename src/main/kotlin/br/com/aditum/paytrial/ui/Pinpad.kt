package br.com.aditum.paytrial.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import br.com.aditum.data.v2.model.PinpadDevice
import br.com.aditum.paytrial.app.MainViewModel
import br.com.aditum.paytrial.designsystem.*

/**
 * Pinpad externo do TefSDK (contrato 3.0.0): pinpads na USB e aparelhos Bluetooth pareados (o pareamento é nas
 * Configurações do Android), a escolha salva no serviço (`selectPinpad`) e o estado (`getTerminalStatus`).
 */
@Composable
fun PinpadScreen(vm: MainViewModel) {
    LaunchedEffect(Unit) { vm.loadPinpads() }
    val list = vm.pinpads
    val status = vm.terminalStatus
    PtScaffold("Pinpad", onBack = { vm.pop() }, wide = true, actions = {
        IconButton(onClick = { vm.loadPinpads() }) { PtIcon(PtIcons.Refresh, "Atualizar a lista") }
    }) {
        TwoPane(first = {
            SectionLabel("Agora")
            val now = status?.takeIf { it.externalPinpad }
            if (now == null) Banner("Sem pinpad externo", Tone.Info, "Este terminal tem o pinpad embutido.")
            else Banner(
                if (now.pinpadConnected) "Pinpad conectado" else "Pinpad desconectado", if (now.pinpadConnected) Tone.Success else Tone.Warning,
                listOfNotNull(now.pinpadTransport?.let { if (it == "BLUETOOTH") "Bluetooth" else "USB" }, now.pinpadName, now.pinpadModel, now.pinpadSerialNumber?.let { "série $it" }).joinToString(" · ").ifBlank { "ainda não conectou nesta sessão" },
                if (now.pinpadTransport == "BLUETOOTH") PtIcons.Bluetooth else PtIcons.Usb,
            )
            Text("O automático usa o pinpad da USB e, sem ele, um Bluetooth pareado com nome de pinpad conhecido. Para outro aparelho Bluetooth, pareie nas Configurações do Android e escolha aqui.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }, second = {
            SectionLabel("Escolha")
            ListRow("Automático", icon = PtIcons.Refresh, details = listOf("selectPinpad(null)"),
                trailing = { if (list != null && list.none { it.selected }) PtIcon(PtIcons.Confirm, "escolhido") }, onClick = { vm.choosePinpad(null) })
            when {
                list == null -> LinearProgressIndicator(Modifier.fillMaxWidth())
                list.isEmpty() -> Text("Nenhum pinpad na USB e nenhum aparelho Bluetooth pareado.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                else -> list.forEach { d -> PinpadRow(d) { vm.choosePinpad(d) } }
            }
        })
    }
}

@Composable
private fun PinpadRow(d: PinpadDevice, onClick: () -> Unit) {
    val bt = d.transport == "BLUETOOTH"
    ListRow(d.name.ifBlank { d.id }, icon = if (bt) PtIcons.Bluetooth else PtIcons.Usb,
        details = listOf(listOfNotNull(if (bt) "Bluetooth" else "USB", d.address, if (d.connected) "conectado" else null, if (bt && !d.likelyPinpad) "nome não é de pinpad" else null).joinToString(" · ")),
        trailing = { if (d.selected) PtIcon(PtIcons.Confirm, "escolhido") }, onClick = onClick)
}
