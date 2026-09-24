package br.com.aditum.paytrial.ui

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import br.com.aditum.paytrial.BuildConfig
import br.com.aditum.paytrial.app.Labels
import br.com.aditum.paytrial.app.MainViewModel
import br.com.aditum.paytrial.app.Screen
import br.com.aditum.paytrial.client.AditumClient
import br.com.aditum.paytrial.client.TraceEntry
import br.com.aditum.paytrial.designsystem.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Mais: terminal e bandeiras (do `init`), manutenção e ferramentas de desenvolvedor. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MoreScreen(vm: MainViewModel) {
    val settings by vm.settings.state.collectAsState()
    var deactivate by remember { mutableStateOf(false) }
    val t = vm.terminal
    PtScaffold("Mais", onBack = { vm.pop() }, wide = true) {
        TwoPane(first = {
        SectionLabel("Terminal")
        KeyValueCard(listOfNotNull(
            t?.model?.let { "Modelo" to listOfNotNull(t.manufacturerName, it).joinToString(" ") }, t?.serialNumber?.let { "Série" to it },
            t?.osVersion?.let { "Sistema" to it }, t?.specVersion?.let { "Especificação ABECS" to it },
            "Aproximação" to if (t?.contactlessSupported == true) "sim" else "não", t?.partNumber?.let { "Part number" to it },
        ).ifEmpty { listOf("Terminal" to "sem dados do init") }, setOf("Série", "Part number"))
        if (vm.brands.isNotEmpty()) {
            SectionLabel("Bandeiras aceitas (${vm.brands.size})")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { vm.brands.forEach { BrandPlate(Labels.brand(it)) } }
        }
        }, second = {
        SectionLabel("Operações")
        ListRow("Atualizar tabelas", icon = PtIcons.Tables, details = listOf("updateEmvTables"), onClick = { vm.updateTables() })
        ListRow("Recarregar estabelecimento", icon = PtIcons.Refresh, details = listOf("getMerchantData"), onClick = { vm.reloadMerchant() })
        ListRow("Reconectar ao serviço", icon = PtIcons.Plug, details = listOf("unbind + bind"), onClick = { vm.reconnect() })
        ListRow("Desativar terminal", icon = PtIcons.Logout, details = listOf("deactivate · pede confirmação"), onClick = { deactivate = true })
        SectionLabel("Desenvolvedor")
        ListRow("Requisição livre", icon = PtIcons.Edit, details = listOf("pay() com qualquer combinação de operação, forma e parcelamento, sem validação"), trailing = { PtIcon(PtIcons.Chevron) }, onClick = { vm.push(Screen.FreeRequest()) })
        ListRow("Desenvolvedor", icon = PtIcons.Bug, details = listOf(if (settings.integrationMode) "Modo de integração ligado" else "Modo de integração desligado"), trailing = { PtIcon(PtIcons.Chevron) }, onClick = { vm.push(Screen.Developer) })
        SectionLabel("Sobre")
        KeyValueCard(listOf("Aditum Pay Trial" to BuildConfig.VERSION_NAME, "Build" to BuildConfig.GIT_HASH, "Dispositivo do build" to BuildConfig.DEVICE.ifBlank { "não especificado" }, "Ambiente" to BuildConfig.ENVIRONMENT, "Pacote" to BuildConfig.APPLICATION_ID,
            "Serviço" to AditumClient.SERVICE_PACKAGE, "Telas" to if (settings.integratorScreens) "do integrador (useOnlySdk)" else "da Aditum"), setOf("Build", "Pacote", "Serviço"))
        if (vm.busy != null) LinearProgressIndicator(Modifier.fillMaxWidth())
        })
    }
    if (deactivate) PtDialog("Desativar o terminal?", "O terminal deixa de vender até ser ativado de novo com um código do portal.", "Desativar",
        onConfirm = { deactivate = false; vm.deactivate() }, onDismiss = { deactivate = false }, destructive = true)
}

/** Desenvolvedor: o modo de integração e as ferramentas de depuração. */
@Composable
fun DeveloperScreen(vm: MainViewModel) {
    val settings by vm.settings.state.collectAsState()
    val entries by vm.trace.entries.collectAsState()
    val context = LocalContext.current
    var clear by remember { mutableStateOf(false) }
    PtScaffold("Desenvolvedor", onBack = { vm.pop() }) {
        SwitchRow("Modo de integração", settings.integrationMode, { v -> vm.settings.update { it.copy(integrationMode = v) } }, "Chip do inspetor, abas cruas no resultado e payload dos callbacks")
        SwitchRow("Prévia antes de enviar", settings.previewBeforeSend, { v -> vm.settings.update { it.copy(previewBeforeSend = v) } }, "Mostra a requisição e pede Enviar antes de cada pagamento")
        SwitchRow("Comprovantes no JSON", settings.receiptsInJson, { v -> vm.settings.update { it.copy(receiptsInJson = v) } }, "Linha a linha, já censurados; não há opção para tirar a censura")
        HorizontalDivider()
        ListRow("Console de eventos", icon = PtIcons.Console, details = listOf("${entries.size} entradas nesta sessão"), trailing = { PtIcon(PtIcons.Chevron) }, onClick = { vm.push(Screen.Console) })
        ListRow("Exportar sessão (.json)", icon = PtIcons.Share, details = listOf("Todas as chamadas, para anexar num chamado"), onClick = {
            val send = Intent(Intent.ACTION_SEND).setType("application/json").putExtra(Intent.EXTRA_SUBJECT, "Aditum Pay Trial — sessão").putExtra(Intent.EXTRA_TEXT, vm.trace.export())
            context.startActivity(Intent.createChooser(send, "Exportar sessão"))
        })
        ListRow("Limpar console", icon = PtIcons.Trash, onClick = { clear = true })
        ListRow("Pagamento duplicado (−1994)", icon = PtIcons.Bug, details = listOf("Dispara dois pay() a 50 ms: reproduz a trava de operação simultânea"), onClick = { vm.duplicatePayTest() })
    }
    if (clear) PtDialog("Limpar o console?", "As ${entries.size} entradas desta sessão são apagadas.", "Limpar", onConfirm = { clear = false; vm.trace.clear() }, onDismiss = { clear = false })
}

/** Console de eventos: toda chamada, resposta e callback, com hora, duração e tela; tocar abre os dados crus. */
@Composable
fun ConsoleScreen(vm: MainViewModel, onOpen: (Long) -> Unit) {
    val entries by vm.trace.entries.collectAsState()
    val clipboard = LocalClipboardManager.current
    var filter by remember { mutableStateOf("all") }
    val shown = entries.filter { when (filter) { "calls" -> it.kind == TraceEntry.Kind.CALL; "callbacks" -> it.kind == TraceEntry.Kind.CALLBACK; "errors" -> it.failed; else -> true } }.asReversed()
    val fmt = remember { SimpleDateFormat("HH:mm:ss.SSS", Locale.US) }
    PtScaffold("Console", onBack = { vm.pop() }, scroll = false, actions = {
        IconButton(onClick = { clipboard.setText(AnnotatedString(vm.trace.export())); vm.say("Sessão copiada") }) { PtIcon(PtIcons.Copy, "Copiar a sessão") }
    }) {
        ChipRow(listOf("all" to "Tudo", "calls" to "Chamadas", "callbacks" to "Callbacks", "errors" to "Erros"), setOf(filter), { filter = it })
        if (shown.isEmpty()) EmptyBlock("Nada registrado ainda", PtIcons.Console)
        LazyColumn(Modifier.weight(1f)) {
            items(shown, key = { it.id }) { e ->
                val (tone, mark) = when { e.failed -> Tone.Error to "✕"; e.kind == TraceEntry.Kind.CALLBACK -> Tone.Warning to "⇠"; e.running -> Tone.Info to "…"; else -> Tone.Success to "←" }
                ListRow(e.method, leading = { StatusTag(mark, tone) },
                    details = listOf("${fmt.format(Date(e.startedAt))} · ${e.screen}${e.durationMs?.takeIf { e.kind == TraceEntry.Kind.CALL }?.let { " · $it ms" } ?: ""}"),
                    monoDetail = (e.error ?: e.response ?: e.request)?.replace(Regex("\\s+"), " ")?.take(80), onClick = { onOpen(e.id) })
            }
        }
    }
}
