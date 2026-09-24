package br.com.aditum.paytrial.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import br.com.aditum.data.v2.model.PinpadDevice
import br.com.aditum.paytrial.app.MainViewModel
import br.com.aditum.paytrial.designsystem.*

/**
 * Pinpad externo do TefSDK (contrato 3.0.0): pinpads na USB e aparelhos Bluetooth pareados (o pareamento é nas
 * Configurações do Android), a escolha salva no serviço (`selectPinpad`) e o estado (`getTerminalStatus`).
 */
@Composable
fun PinpadScreen(vm: MainViewModel) {
    // abre com o que já se conhece (USB + pareados) e já começa a buscar; sair da tela para a busca
    LaunchedEffect(Unit) { vm.loadPinpads().join(); vm.scanPinpads() }
    DisposableEffect(Unit) { onDispose { vm.stopPinpadScan() } }
    val list = vm.pinpads
    val status = vm.terminalStatus
    var forget by remember { mutableStateOf<PinpadDevice?>(null) }
    PtScaffold("Pinpad", onBack = { vm.pop() }, wide = true, actions = {
        IconButton(onClick = { vm.scanPinpads() }, enabled = !vm.scanning) { PtIcon(PtIcons.Refresh, "Buscar de novo") }
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
            Text("Automático: o pinpad da USB; sem ele, um Bluetooth já pareado (inclusive pelas Configurações do Android) com porta serial conecta sozinho. Qualquer aparelho da lista conecta quando você toca nele (não pareado: o Android pede o pareamento).",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }, second = {
            SectionLabel("Escolha")
            if (vm.scanning) { Text("Buscando aparelhos por perto… (30 s) — toque em um para conectar", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant); LinearProgressIndicator(Modifier.fillMaxWidth()) }
            if (vm.busy == "selectPinpad") { Text("Conectando… (Bluetooth não pareado: aceite o pareamento no Android)", style = MaterialTheme.typography.bodyMedium); LinearProgressIndicator(Modifier.fillMaxWidth()) }
            ListRow("Automático", icon = PtIcons.Refresh, details = listOf("selectPinpad(null)"),
                trailing = { if (list != null && list.none { it.selected }) PtIcon(PtIcons.Confirm, "escolhido") }, onClick = { vm.choosePinpad(null) })
            when {
                list == null -> LinearProgressIndicator(Modifier.fillMaxWidth())
                list.isEmpty() && !vm.scanning -> Text("Nada na USB, nenhum pareado e nada respondeu à busca. Deixe o pinpad pronto para parear e toque em buscar de novo.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                else -> list.sortedWith(compareByDescending<PinpadDevice> { it.selected }.thenByDescending { it.transport != "BLUETOOTH" }.thenByDescending { it.paired }.thenByDescending { it.serialPort }).forEach { d -> PinpadRow(d, onConnect = { vm.choosePinpad(d) }, onDisconnect = { vm.disconnectPinpad(d) }, onForget = { forget = d }) }
            }
        })
    }
    forget?.let { d -> PtDialog("Esquecer ${d.name}?", "O pareamento com este aparelho é desfeito no Android. Para usar de novo, é preciso parear outra vez.", "Esquecer",
        onConfirm = { forget = null; vm.forgetPinpad(d) }, onDismiss = { forget = null }, destructive = true) }
}

@Composable
private fun PinpadRow(d: PinpadDevice, onConnect: () -> Unit, onDisconnect: () -> Unit, onForget: () -> Unit) {
    val bt = d.transport == "BLUETOOTH"
    Column {
        ListRow(d.name.ifBlank { d.id }, icon = if (bt) PtIcons.Bluetooth else PtIcons.Usb,
            details = listOf(listOfNotNull(if (bt) "Bluetooth" else "USB", d.address, if (d.connected) "conectado" else null,
                if (bt) (if (d.paired) "pareado" else "não pareado") else null, if (bt && d.nearby) "por perto" else null, if (bt && d.serialPort) "porta serial" else null).joinToString(" · ")),
            trailing = { if (d.selected) PtIcon(PtIcons.Confirm, "escolhido") }, onClick = onConnect)
        // conectar é tocar na linha; desconectar vale para o escolhido/conectado; esquecer, para Bluetooth pareado
        if (bt && (d.connected || d.selected || d.paired)) Row(Modifier.fillMaxWidth().padding(start = 56.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (d.connected || d.selected) TextButton(onClick = onDisconnect) { Text("Desconectar") }
            if (d.paired) TextButton(onClick = onForget) { Text("Esquecer", color = MaterialTheme.colorScheme.error) }
        }
    }
}
