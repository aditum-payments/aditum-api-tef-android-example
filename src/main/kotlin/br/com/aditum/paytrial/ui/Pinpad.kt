package br.com.aditum.paytrial.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import br.com.aditum.data.v2.model.PinpadDevice
import br.com.aditum.paytrial.app.MainViewModel
import br.com.aditum.paytrial.designsystem.*

/**
 * Pinpad externo do TefSDK (contrato 3.0.0): pinpads na USB e aparelhos Bluetooth pareados (o pareamento é nas
 * Configurações do Android), a escolha salva no serviço (`selectPinpad`) e o estado (`getTerminalStatus`).
 *
 * **USB plugada bloqueia o Bluetooth** (Kelvin, 25/09): a opção continua na tela, mas buscar ou tocar num aparelho
 * Bluetooth só explica que é preciso tirar o pinpad da USB antes; a tela vigia a USB e, quando ele sai, libera e já
 * busca. Com o Bluetooth do aparelho **desligado** a busca não acha nada e a lista ficava só com a USB, sem dizer por
 * quê (Pi 4, 25/09): agora a tela avisa e leva às configurações de Bluetooth.
 */
@Composable
fun PinpadScreen(vm: MainViewModel) {
    val context = LocalContext.current
    var bluetoothOn by remember { mutableStateOf(bluetoothEnabled(context)) }
    var usbFirst by remember { mutableStateOf(false) }
    // abre com o que já se conhece (USB + pareados) e só busca Bluetooth sem pinpad na USB; sair da tela para a busca
    LaunchedEffect(Unit) { vm.loadPinpads().join(); if (!vm.usbPinpadPlugged && bluetoothOn) vm.scanPinpads() }
    DisposableEffect(Unit) { onDispose { vm.stopPinpadScan() } }
    val usb = vm.usbPinpadPlugged
    // vigia: com a USB plugada (ou o Bluetooth desligado), relê a cada 2 s; a USB sair libera e já inicia a busca
    LaunchedEffect(usb, bluetoothOn) {
        if (!usb && bluetoothOn) return@LaunchedEffect
        while (true) {
            kotlinx.coroutines.delay(2_000)
            bluetoothOn = bluetoothEnabled(context)
            vm.refreshPinpads()
            if (!vm.usbPinpadPlugged && bluetoothOn) { usbFirst = false; vm.scanPinpads(); break }
        }
    }
    val list = vm.pinpads
    val status = vm.terminalStatus
    var forget by remember { mutableStateOf<PinpadDevice?>(null) }
    fun searchBluetooth() { when { usb -> usbFirst = true; !bluetoothOn -> openBluetoothSettings(context); else -> vm.scanPinpads() } }
    PtScaffold("Pinpad", onBack = { vm.pop() }, wide = true, actions = {
        IconButton(onClick = { searchBluetooth() }, enabled = !vm.scanning) { PtIcon(PtIcons.Refresh, "Buscar de novo") }
    }) {
        TwoPane(first = {
            SectionLabel("Último pinpad usado")
            val now = status?.takeIf { it.externalPinpad }
            // sem afirmar "conectado": o enlace só existe durante a operação (não há consulta periódica)
            if (now == null) Banner("Sem pinpad externo", Tone.Info, "Este terminal tem o pinpad embutido.")
            else Banner(listOfNotNull(now.pinpadModel?.ifBlank { null }, now.pinpadSerialNumber?.ifBlank { null }?.let { "série $it" }).joinToString(" · ").ifBlank { "Nenhum pinpad usado ainda" }, Tone.Neutral,
                listOfNotNull(now.pinpadTransport?.let { if (it == "BLUETOOTH") "Bluetooth" else "USB" }, now.pinpadName).joinToString(" · ").ifBlank { null },
                if (now.pinpadTransport == "BLUETOOTH") PtIcons.Bluetooth else PtIcons.Usb)
            Text("Automático: o pinpad da USB; sem ele, um Bluetooth já pareado (inclusive pelas Configurações do Android) com porta serial conecta sozinho. Qualquer aparelho da lista conecta quando você toca nele (não pareado: o Android pede o pareamento).",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }, second = {
            SectionLabel("Escolha")
            if (vm.scanning) { Text("Buscando aparelhos por perto… (30 s) — toque em um para conectar", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant); LinearProgressIndicator(Modifier.fillMaxWidth()) }
            if (vm.busy == "selectPinpad") { Text("Conectando… (Bluetooth não pareado: aceite o pareamento no Android)", style = MaterialTheme.typography.bodyMedium); LinearProgressIndicator(Modifier.fillMaxWidth()) }
            // a opção Bluetooth fica sempre: com USB plugada ou Bluetooth desligado, ela explica o que fazer
            if (usb) ListRow("Bluetooth", icon = PtIcons.Bluetooth, details = listOf("remova o pinpad da USB para buscar e conectar por Bluetooth"), onClick = { usbFirst = true })
            else if (!bluetoothOn) ListRow("Bluetooth desligado", icon = PtIcons.Bluetooth, details = listOf("toque para ligar nas configurações; a busca começa quando ele ligar"), onClick = { openBluetoothSettings(context) })
            ListRow("Automático", icon = PtIcons.Refresh, details = listOf("USB; sem ele, Bluetooth pareado com porta serial"),
                trailing = { if (list != null && list.none { it.selected }) PtIcon(PtIcons.Confirm, "escolhido") }, onClick = { vm.choosePinpad(null) })
            // trocar para USB mesmo sem ele plugado agora: vale na próxima operação
            val usbChosen = list?.any { it.selected && it.transport != "BLUETOOTH" } == true
            if (list?.none { it.transport != "BLUETOOTH" } != false) ListRow("Só USB", icon = PtIcons.Usb, details = listOf("o pinpad plugado na USB (nenhum agora)"),
                trailing = { if (usbChosen) PtIcon(PtIcons.Confirm, "escolhido") }, onClick = { vm.choosePinpad(PinpadDevice(transport = "USB", id = "usb", name = "USB")) })
            when {
                list == null -> LinearProgressIndicator(Modifier.fillMaxWidth())
                list.isEmpty() && !vm.scanning -> Text("Nada na USB, nenhum pareado e nada respondeu à busca. Deixe o pinpad pronto para parear e toque em buscar de novo.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                else -> list.sortedWith(compareByDescending<PinpadDevice> { it.selected }.thenByDescending { it.transport != "BLUETOOTH" }.thenByDescending { it.paired }.thenByDescending { it.serialPort }).forEach { d -> PinpadRow(d, onConnect = { if (usb && d.transport == "BLUETOOTH") usbFirst = true else vm.choosePinpad(d) }, onDisconnect = { vm.disconnectPinpad(d) }, onForget = { forget = d }) }
            }
        })
    }
    if (usbFirst) PtDialog("Remova o pinpad da USB", "Com um pinpad plugado na USB, o Bluetooth fica bloqueado. Tire o cabo USB: a busca por Bluetooth começa sozinha assim que ele sair.",
        "Entendi", onConfirm = { usbFirst = false }, onDismiss = { usbFirst = false })
    forget?.let { d -> PtDialog("Esquecer ${d.name}?", "O pareamento com este aparelho é desfeito no Android. Para usar de novo, é preciso parear outra vez.", "Esquecer",
        onConfirm = { forget = null; vm.forgetPinpad(d) }, onDismiss = { forget = null }, destructive = true) }
}

@Composable
private fun PinpadRow(d: PinpadDevice, onConnect: () -> Unit, onDisconnect: () -> Unit, onForget: () -> Unit) {
    val bt = d.transport == "BLUETOOTH"
    Column {
        ListRow(d.name.ifBlank { d.id }, icon = if (bt) PtIcons.Bluetooth else PtIcons.Usb,
            details = listOf(listOfNotNull(if (bt) "Bluetooth" else "USB", d.address, if (d.connected) "último usado" else null,
                if (bt) (if (d.paired) "pareado" else "não pareado") else null, if (bt && d.nearby) "por perto" else null, if (bt && d.serialPort) "porta serial" else null).joinToString(" · ")),
            trailing = { if (d.selected) PtIcon(PtIcons.Confirm, "escolhido") }, onClick = onConnect)
        // conectar é tocar na linha; desconectar vale para o escolhido/conectado; esquecer, para Bluetooth pareado
        if (bt && (d.connected || d.selected || d.paired)) Row(Modifier.fillMaxWidth().padding(start = 56.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (d.connected || d.selected) TextButton(onClick = onDisconnect) { Text("Desconectar") }
            if (d.paired) TextButton(onClick = onForget) { Text("Esquecer", color = MaterialTheme.colorScheme.error) }
        }
    }
}

/** Adaptador Bluetooth ligado (`isEnabled` não pede permissão de execução). Sem Bluetooth no aparelho: `false`. */
private fun bluetoothEnabled(context: android.content.Context): Boolean =
    runCatching { (context.getSystemService(android.content.Context.BLUETOOTH_SERVICE) as? android.bluetooth.BluetoothManager)?.adapter?.isEnabled == true }.getOrDefault(false)

/** Configurações de Bluetooth do sistema (ligar por lá não pede a permissão BLUETOOTH_CONNECT do `ACTION_REQUEST_ENABLE`). */
private fun openBluetoothSettings(context: android.content.Context) {
    runCatching { context.startActivity(android.content.Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)) }
}
