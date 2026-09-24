package br.com.aditum.paytrial.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import br.com.aditum.data.v2.enums.PayOperationType
import br.com.aditum.paytrial.app.ACTIVATION_CODE_LENGTH
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
            enabled = conn is AditumClient.Connection.Connected && !vm.initializing && code.length == ACTIVATION_CODE_LENGTH)
    }) {
        TwoPane(first = {
        ConnectionBanner(conn)
        PtField(code, { v -> vm.settings.update { it.copy(activationCode = v.filter(Char::isDigit).take(9)) } }, "Código de ativação", keyboard = KeyboardType.Number,
            supporting = "9 dígitos, do portal do estabelecimento. Obrigatório.")
        }, second = {
        SectionLabel("Quem desenha as telas")
        if (!vm.aditumScreensAvailable) Banner("Telas do integrador", Tone.Info, "O TefSDK não tem as telas da Aditum: este app desenha PIN, QR Code, dado digitado e menus (useOnlySdk = true).")
        else Segmented(listOf(false to "Telas da Aditum", true to "Telas do integrador"), settings.integratorScreens, { v -> vm.settings.update { it.copy(integratorScreens = v) } })
        if (vm.aditumScreensAvailable) Text(if (settings.integratorScreens) "Este app desenha PIN, QR Code, dado digitado e menus (useOnlySdk = true)." else "O app da Aditum abre por cima durante a operação e fecha no fim (useOnlySdk = false).",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        // TefSDK: o pinpad (USB ou Bluetooth pareado) pode ser escolhido antes do init — sem ele o init nem abre o pinpad
        LaunchedEffect(conn) { if (conn is AditumClient.Connection.Connected) vm.refreshTerminalStatus(traced = false) }
        if (vm.terminalStatus?.externalPinpad == true) ListRow("Pinpad", icon = if (vm.terminalStatus?.pinpadTransport == "BLUETOOTH") PtIcons.Bluetooth else PtIcons.Usb,
            details = listOf("USB ou Bluetooth pareado · automático se nada for escolhido"), trailing = { PtIcon(PtIcons.Chevron) }, onClick = { vm.push(Screen.Pinpad) })
        ListRow("Mensagens do pinpad", icon = PtIcons.Terminal, details = listOf(settings.mainMessage.ifBlank { "Logo ou nome do estabelecimento" }), trailing = { PtIcon(PtIcons.Chevron) }, onClick = { messages = true })
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
        // pinpad externo (TefSDK): o status dele no lugar de "Telas…"; em POS, com pinpad embutido, não faz sentido mostrar
        // sem consulta periódica: o app não sabe a cada instante se o pinpad está de pé, então o card mostra só o último
        // pinpad usado (modelo e série), para se ter ideia de qual é — tocar abre a gestão de pinpads (TefSDK)
        LaunchedEffect(Unit) { vm.refreshTerminalStatus(traced = false) }
        val status = vm.terminalStatus
        val external = status?.externalPinpad == true
        MerchantCard(listOfNotNull(m?.document?.let { "CNPJ ${cnpj(it)}" }, vm.terminal?.model?.ifBlank { null }).joinToString(" · ").ifBlank { "Terminal ativado" },
            vm.merchantLogo,
            pinpad = status?.takeIf { it.externalPinpad }?.let { st -> st.pinpadTransport to listOfNotNull(st.pinpadModel?.ifBlank { null }, st.pinpadSerialNumber?.ifBlank { null }?.let { "série $it" }).joinToString(" · ").ifBlank { "nenhum pinpad usado ainda" } },
            screens = if (settings.integratorScreens) "Telas do integrador" else "Telas da Aditum",
            onClick = if (external) ({ vm.push(Screen.Pinpad) }) else null)
        ShortcutGrid(listOf(
            Shortcut("sale", "Nova venda", PtIcons.Sale, highlight = true), Shortcut("cancel", "Cancelar", PtIcons.Cancel),
            Shortcut("preauth", "Pré-autorização", PtIcons.PreAuth, enabled = m?.preAuthorizationEnabled == true),
            Shortcut("confirm", "Confirmar", PtIcons.Confirm, badge = vm.pendingCount), Shortcut("lookup", "Consultar", PtIcons.Lookup),
            Shortcut("report", "Relatório", PtIcons.Report), Shortcut("more", "Mais", PtIcons.More),
        ) + listOfNotNull(if (external) Shortcut("pinpad", "Pinpad", if (status?.pinpadTransport == "BLUETOOTH") PtIcons.Bluetooth else PtIcons.Usb) else null)) { s ->
            when (s.id) {
                "sale" -> vm.push(Screen.Amount()); "cancel" -> vm.push(Screen.Cancel()); "preauth" -> vm.push(Screen.Amount(PayOperationType.PreAuthorization))
                "confirm" -> vm.push(Screen.Confirm); "lookup" -> vm.push(Screen.Lookup()); "report" -> vm.push(Screen.Report); "more" -> vm.push(Screen.More); "pinpad" -> vm.push(Screen.Pinpad)
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
        } else if (LocalWidthClass.current == WidthClass.Expanded) {
            // em tablet a coluna aparece desde o início, em vez de um vazio ao lado da grade
            SectionLabel("Últimas transações")
            Text("Nenhuma transação nesta sessão. As vendas aprovadas aparecem aqui, com o NSU para consultar.",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        })
    }
}

/** Cartão mascarado para exibição (só os 4 últimos) e portador em iniciais: a mesma regra do rastreio. */
fun maskedCard(n: String?): String = Redactor.pan(n) ?: "—"
fun maskedHolder(n: String?): String = Redactor.name(n) ?: "—"

/**
 * Card do estabelecimento, como o da home do standalone (`MerchantHeader`): logo em faixa — altura fixa, largura pela
 * proporção da própria imagem, sem fundo, sobre a cor do card — e, com pinpad externo (TefSDK), o estado dele em chip.
 * Sem logo baixado, o da Aditum (o mesmo fallback do standalone). Em tablet a faixa cresce.
 */
@Composable
fun MerchantCard(title: String, logo: android.graphics.Bitmap?, pinpad: Pair<String?, String>?, screens: String, onClick: (() -> Unit)? = null) {
    val (height, maxWidth) = when (LocalWidthClass.current) { WidthClass.Compact -> 44.dp to 132.dp; WidthClass.Medium -> 56.dp to 180.dp; WidthClass.Expanded -> 72.dp to 240.dp }
    val colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    val content: @Composable () -> Unit = {
        androidx.compose.foundation.layout.Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            val painter = if (logo != null) androidx.compose.ui.graphics.painter.BitmapPainter(logo.asImageBitmap()) else androidx.compose.ui.res.painterResource(br.com.aditum.paytrial.R.drawable.aditum_logo)
            androidx.compose.foundation.Image(painter, "Logo do estabelecimento", Modifier.height(height).widthIn(max = maxWidth), contentScale = androidx.compose.ui.layout.ContentScale.Fit)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                if (pinpad != null) {
                    // último pinpad usado, sem dizer "conectado": o enlace só existe durante a operação
                    val (transport, details) = pinpad
                    androidx.compose.foundation.layout.Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        PtIcon(when (transport) { "BLUETOOTH" -> PtIcons.Bluetooth; "USB" -> PtIcons.Usb; else -> PtIcons.Terminal }, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, size = 16.dp); Spacer(Modifier.width(4.dp))
                        Text("Pinpad $details", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else Text(screens, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (onClick != null) PtIcon(PtIcons.Chevron, "Gerenciar pinpads", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    if (onClick != null) androidx.compose.material3.Card(onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium, colors = colors) { content() }
    else androidx.compose.material3.Card(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium, colors = colors) { content() }
}

/** CNPJ com máscara (00.000.000/0000-00); outro formato volta como veio. */
private fun cnpj(d: String): String = d.filter(Char::isDigit).let { if (it.length == 14) "${it.take(2)}.${it.substring(2, 5)}.${it.substring(5, 8)}/${it.substring(8, 12)}-${it.takeLast(2)}" else d }
