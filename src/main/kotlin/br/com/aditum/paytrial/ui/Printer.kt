package br.com.aditum.paytrial.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import br.com.aditum.paytrial.app.MainViewModel
import br.com.aditum.paytrial.designsystem.*
import kotlinx.coroutines.launch

/**
 * Impressora, configuração à parte: o comprovante sai na impressora do terminal (`IPrinterSdk`) ou numa térmica
 * **externa** pela USB do aparelho (host/OTG), como imagem ESC/POS — genérico, sem SDK de fabricante (Elgin/Bematech
 * i9, Epson, Tanca… de classe USB padrão).
 */
@Composable
fun PrinterScreen(vm: MainViewModel) {
    val settings by vm.settings.state.collectAsState()
    var printers by remember { mutableStateOf(vm.usbPrinter.list()) }
    val scope = rememberCoroutineScope()
    PtScaffold("Impressora", onBack = { vm.pop() }, bottomBar = {
        if (settings.externalPrinter) PtButton(if (vm.busy == "print") "Imprimindo…" else "Imprimir teste", { vm.printUsbTest(settings) }, Modifier.fillMaxWidth(),
            PtButtonKind.Outlined, PtIcons.Print, enabled = vm.busy == null)
    }) {
        SectionLabel("Onde imprimir")
        ChoiceChips(listOf("TERMINAL" to "Impressora do terminal", "USB" to "Térmica externa USB"), settings.printerTarget, { v -> vm.settings.update { it.copy(printerTarget = v) } })
        if (settings.printerTarget == "TERMINAL") {
            Text(if (vm.terminalPrinter) "getDeviceSdk().printerSdk: o comprovante vai como bitmap para a impressora do terminal."
                else "Este terminal não tem impressora (printerSdk nulo): escolha a térmica externa ou envie o comprovante por e-mail/SMS.",
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            SectionLabel("Impressoras USB conectadas")
            if (printers.isEmpty()) EmptyBlock("Nenhuma impressora USB. Ligue a impressora (com a fonte dela) pelo cabo OTG e toque em Procurar.", PtIcons.Usb)
            printers.forEach { p ->
                ListRow(p.name, icon = PtIcons.Usb, details = listOf(p.key), value = if (settings.usbPrinter == p.key) "Selecionada" else null, onClick = {
                    // escolher já pede a permissão do Android para o dispositivo
                    scope.launch { if (vm.usbPrinter.ensurePermission(p.device)) vm.settings.update { it.copy(usbPrinter = p.key) } else vm.say("Sem permissão para usar a impressora USB") }
                })
            }
            PtButton("Procurar de novo", { printers = vm.usbPrinter.list() }, Modifier.fillMaxWidth(), PtButtonKind.Tonal, PtIcons.Refresh)
            SectionLabel("Papel")
            ChoiceChips(listOf("MM80" to "80 mm (576 pontos)", "MM58" to "58 mm (384 pontos)"), settings.printerPaper, { v -> vm.settings.update { it.copy(printerPaper = v) } })
            if (settings.usbPrinter.isNotBlank() && printers.none { it.key == settings.usbPrinter })
                Banner("Impressora escolhida desconectada", Tone.Warning, "${settings.usbPrinter}: ligue o cabo; a escolha fica guardada.")
            Text("Comprovante em imagem ESC/POS (GS v 0) com o logo do estabelecimento e corte entre as vias. Vale para impressoras térmicas USB de classe padrão.",
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
