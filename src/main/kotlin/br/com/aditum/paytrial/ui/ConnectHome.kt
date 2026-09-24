package br.com.aditum.paytrial.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import br.com.aditum.data.v2.enums.PayOperationType
import br.com.aditum.paytrial.app.Labels
import br.com.aditum.paytrial.app.MainViewModel
import br.com.aditum.paytrial.app.Screen
import br.com.aditum.paytrial.client.AditumClient
import br.com.aditum.paytrial.client.Redactor
import br.com.aditum.paytrial.designsystem.*

/** Conectar: estado do serviço, código de ativação, quem desenha as telas e as mensagens do pinpad; chama `init`. */
@Composable
fun ConnectScreen(vm: MainViewModel) {
    val settings by vm.settings.state.collectAsState()
    val conn by vm.client.connection.collectAsState()
    var messages by remember { mutableStateOf(false) }
    val code = settings.activationCode
    PtScaffold("Conectar à Aditum", wide = true, bottomBar = {
        PtButton(if (vm.initializing) "Iniciando…" else "Iniciar", { vm.init() }, Modifier.fillMaxWidth(), icon = PtIcons.Plug,
            enabled = conn is AditumClient.Connection.Connected && !vm.initializing)
    }) {
        TwoPane(first = {
        ConnectionBanner(conn)
        PtField(code, { v -> vm.settings.update { it.copy(activationCode = v.filter(Char::isDigit).take(9)) } }, "Código de ativação", keyboard = KeyboardType.Number,
            supporting = "9 dígitos, do portal do estabelecimento. Em branco, o serviço usa o do build.")
        }, second = {
        SectionLabel("Quem desenha as telas")
        if (!vm.aditumScreensAvailable) Banner("Telas do integrador", Tone.Info, "O TefSDK não tem as telas da Aditum: este app desenha PIN, QR Code, dado digitado e menus (useOnlySdk = true).")
        else Segmented(listOf(false to "Telas da Aditum", true to "Telas do integrador"), settings.integratorScreens, { v -> vm.settings.update { it.copy(integratorScreens = v) } })
        if (vm.aditumScreensAvailable) Text(if (settings.integratorScreens) "Este app desenha PIN, QR Code, dado digitado e menus (useOnlySdk = true)." else "O app da Aditum abre por cima durante a operação e fecha no fim (useOnlySdk = false).",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        ListRow("Mensagens do pinpad", icon = PtIcons.Terminal, details = listOf(settings.mainMessage.ifBlank { "Padrão do serviço" }), trailing = { PtIcon(PtIcons.Chevron) }, onClick = { messages = true })
        vm.initError?.let { Banner("Não foi possível iniciar", Tone.Error, it) }
        if (vm.initializing) LinearProgressIndicator(Modifier.fillMaxWidth())
        })
    }
    if (messages) PtSheet("Mensagens do pinpad", { messages = false }, actions = { PtButton("Pronto", { messages = false }, Modifier.weight(1f)) }) {
        Text("Enviadas no init (PinpadMessages). Em branco, vale a do serviço.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        PtField(settings.mainMessage, { v -> vm.settings.update { it.copy(mainMessage = v.take(32)) } }, "Principal (tela em espera)")
        PtField(settings.processingMessage, { v -> vm.settings.update { it.copy(processingMessage = v.take(32)) } }, "Processando")
        PtField(settings.approvedMessage, { v -> vm.settings.update { it.copy(approvedMessage = v.take(32)) } }, "Aprovada")
        PtField(settings.declinedMessage, { v -> vm.settings.update { it.copy(declinedMessage = v.take(32)) } }, "Negada")
        PtField(settings.initializationMessage, { v -> vm.settings.update { it.copy(initializationMessage = v.take(32)) } }, "Inicialização")
    }
}

@Composable
fun ConnectionBanner(c: AditumClient.Connection) = when (c) {
    AditumClient.Connection.Connected -> Banner("Serviço conectado", Tone.Success, AditumClient.SERVICE_PACKAGE)
    AditumClient.Connection.Connecting -> Banner("Conectando ao serviço…", Tone.Info, "Religa sozinho se o serviço cair.")
    AditumClient.Connection.Disconnected -> Banner("Serviço desconectado", Tone.Warning)
    is AditumClient.Connection.Unavailable -> Banner("Serviço indisponível", Tone.Error, c.reason)
}

/** Início: identidade do estabelecimento ativado, atalhos e as últimas vendas desta sessão. */
@Composable
fun HomeScreen(vm: MainViewModel) {
    val settings by vm.settings.state.collectAsState()
    val conn by vm.client.connection.collectAsState()
    val m = vm.merchant
    LaunchedEffect(Unit) { vm.refreshPending() }
    PtScaffold(m?.fantasyName?.ifBlank { null } ?: "Aditum Pay Trial", wide = true, actions = {
        if (settings.integrationMode) IconButton(onClick = { vm.push(Screen.Console) }) { PtIcon(PtIcons.Console, "Console de eventos") }
    }) {
        TwoPane(first = {
        if (conn !is AditumClient.Connection.Connected) ConnectionBanner(conn)
        Banner(listOfNotNull(m?.document?.let { "CNPJ $it" }, vm.terminal?.model).joinToString(" · ").ifBlank { "Terminal ativado" }, Tone.Neutral,
            if (settings.integratorScreens) "Telas do integrador" else "Telas da Aditum", PtIcons.Store)
        ShortcutGrid(listOf(
            Shortcut("sale", "Nova venda", PtIcons.Sale, highlight = true), Shortcut("cancel", "Cancelar", PtIcons.Cancel),
            Shortcut("preauth", "Pré-autorização", PtIcons.PreAuth, enabled = m?.preAuthorizationEnabled == true),
            Shortcut("confirm", "Confirmar", PtIcons.Confirm, badge = vm.pendingCount), Shortcut("lookup", "Consultar", PtIcons.Lookup),
            Shortcut("report", "Relatório", PtIcons.Report), Shortcut("more", "Mais", PtIcons.More),
        )) { s ->
            when (s.id) {
                "sale" -> vm.push(Screen.Amount()); "cancel" -> vm.push(Screen.Cancel()); "preauth" -> vm.push(Screen.Amount(PayOperationType.PreAuthorization))
                "confirm" -> vm.push(Screen.Confirm); "lookup" -> vm.push(Screen.Lookup()); "report" -> vm.push(Screen.Report); "more" -> vm.push(Screen.More)
            }
        }
        }, second = {
        if (vm.recent.isNotEmpty()) {
            SectionLabel("Últimas transações")
            vm.recent.forEach { c ->
                ListRow("${Labels.paymentType(c.paymentType)}${if (c.installmentNumber > 1) " ${c.installmentNumber}x" else ""}", icon = Labels.paymentIcon(c.paymentType),
                    details = listOf(listOf(Labels.time(c.creationDateTime), Labels.brand(c.brand)).filter { it.isNotBlank() }.joinToString(" · ")),
                    monoDetail = c.nsu?.let { "NSU $it" }, value = Money.brl(c.amount), onClick = { vm.push(Screen.Lookup(c.nsu.orEmpty())) })
            }
        }
        })
    }
}

/** Cartão mascarado para exibição (só os 4 últimos) e portador em iniciais: a mesma regra do rastreio. */
fun maskedCard(n: String?): String = Redactor.pan(n) ?: "—"
fun maskedHolder(n: String?): String = Redactor.name(n) ?: "—"
