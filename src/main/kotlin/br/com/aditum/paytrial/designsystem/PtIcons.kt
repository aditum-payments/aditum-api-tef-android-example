package br.com.aditum.paytrial.designsystem

// GERADO POR tools/icons.py — NÃO EDITAR À MÃO. Tabler Icons 3.48.0 (MIT, © Paweł Kuna), caixa 24x24.

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

private const val FILL = true
private const val LINE = false

/** Monta o vetor: preenchido ou em linha (traço 2, pontas e junções redondas, como no Tabler). */
private fun icon(name: String, filled: Boolean, vararg paths: String): ImageVector =
    ImageVector.Builder(name = name, defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply {
        for (d in paths) {
            if (filled) addPath(pathData = addPathNodes(d), fill = SolidColor(Color.Black))
            else addPath(pathData = addPathNodes(d), fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        }
    }.build()

/** Ícones do Aditum Pay Trial (Tabler). Use com `Icon(PtIcons.Print, …)`: a cor vem do `tint`. */
object PtIcons {
    /** Tabler `cash-banknote` (linha). */
    val Sale: ImageVector by lazy { icon("Sale", LINE, "M9 12 a3 3 0 1 0 6 0 a3 3 0 0 0 -6 0", "M3 8 a2 2 0 0 1 2 -2 h14 a2 2 0 0 1 2 2 v8 a2 2 0 0 1 -2 2 h-14 a2 2 0 0 1 -2 -2 l0 -8", "M18 12 h0.01", "M6 12 h0.01") }
    /** Tabler `square-x` (linha). */
    val Cancel: ImageVector by lazy { icon("Cancel", LINE, "M3 5 a2 2 0 0 1 2 -2 h14 a2 2 0 0 1 2 2 v14 a2 2 0 0 1 -2 2 h-14 a2 2 0 0 1 -2 -2 v-14", "M9 9 l6 6 m0 -6 l-6 6") }
    /** Tabler `clock` (linha). */
    val PreAuth: ImageVector by lazy { icon("PreAuth", LINE, "M3 12 a9 9 0 1 0 18 0 a9 9 0 0 0 -18 0", "M12 7 v5 l3 3") }
    /** Tabler `circle-check` (linha). */
    val Confirm: ImageVector by lazy { icon("Confirm", LINE, "M3 12 a9 9 0 1 0 18 0 a9 9 0 1 0 -18 0", "M9 12 l2 2 l4 -4") }
    /** Tabler `search` (linha). */
    val Lookup: ImageVector by lazy { icon("Lookup", LINE, "M3 10 a7 7 0 1 0 14 0 a7 7 0 1 0 -14 0", "M21 21 l-6 -6") }
    /** Tabler `chart-pie` (linha). */
    val Report: ImageVector by lazy { icon("Report", LINE, "M10 3.2 a9 9 0 1 0 10.8 10.8 a1 1 0 0 0 -1 -1 h-6.8 a2 2 0 0 1 -2 -2 v-7 a0.9 0.9 0 0 0 -1 -0.8", "M15 3.5 a9 9 0 0 1 5.5 5.5 h-4.5 a1 1 0 0 1 -1 -1 v-4.5") }
    /** Tabler `receipt` (linha). */
    val Receipt: ImageVector by lazy { icon("Receipt", LINE, "M5 21 v-16 a2 2 0 0 1 2 -2 h10 a2 2 0 0 1 2 2 v16 l-3 -2 l-2 2 l-2 -2 l-2 2 l-2 -2 l-3 2 m4 -14 h6 m-6 4 h6 m-2 4 h2") }
    /** Tabler `category` (linha). */
    val More: ImageVector by lazy { icon("More", LINE, "M4 4 h6 v6 h-6 l0 -6", "M14 4 h6 v6 h-6 l0 -6", "M4 14 h6 v6 h-6 l0 -6", "M14 17 a3 3 0 1 0 6 0 a3 3 0 1 0 -6 0") }
    /** Tabler `printer` (linha). */
    val Print: ImageVector by lazy { icon("Print", LINE, "M17 17 h2 a2 2 0 0 0 2 -2 v-4 a2 2 0 0 0 -2 -2 h-14 a2 2 0 0 0 -2 2 v4 a2 2 0 0 0 2 2 h2", "M17 9 v-4 a2 2 0 0 0 -2 -2 h-6 a2 2 0 0 0 -2 2 v4", "M7 15 a2 2 0 0 1 2 -2 h6 a2 2 0 0 1 2 2 v4 a2 2 0 0 1 -2 2 h-6 a2 2 0 0 1 -2 -2 l0 -4") }
    /** Tabler `send` (linha). */
    val Send: ImageVector by lazy { icon("Send", LINE, "M10 14 l11 -11", "M21 3 l-6.5 18 a0.55 0.55 0 0 1 -1 0 l-3.5 -7 l-7 -3.5 a0.55 0.55 0 0 1 0 -1 l18 -6.5") }
    /** Tabler `mail` (linha). */
    val Mail: ImageVector by lazy { icon("Mail", LINE, "M3 7 a2 2 0 0 1 2 -2 h14 a2 2 0 0 1 2 2 v10 a2 2 0 0 1 -2 2 h-14 a2 2 0 0 1 -2 -2 v-10", "M3 7 l9 6 l9 -6") }
    /** Tabler `message` (linha). */
    val Sms: ImageVector by lazy { icon("Sms", LINE, "M8 9 h8", "M8 13 h6", "M18 4 a3 3 0 0 1 3 3 v8 a3 3 0 0 1 -3 3 h-5 l-5 3 v-3 h-2 a3 3 0 0 1 -3 -3 v-8 a3 3 0 0 1 3 -3 h12") }
    /** Tabler `copy` (linha). */
    val Copy: ImageVector by lazy { icon("Copy", LINE, "M7 9.667 a2.667 2.667 0 0 1 2.667 -2.667 h8.666 a2.667 2.667 0 0 1 2.667 2.667 v8.666 a2.667 2.667 0 0 1 -2.667 2.667 h-8.666 a2.667 2.667 0 0 1 -2.667 -2.667 l0 -8.666", "M4.012 16.737 a2.005 2.005 0 0 1 -1.012 -1.737 v-10 c0 -1.1 0.9 -2 2 -2 h10 c0.75 0 1.158 0.385 1.5 1") }
    /** Tabler `share` (linha). */
    val Share: ImageVector by lazy { icon("Share", LINE, "M3 12 a3 3 0 1 0 6 0 a3 3 0 1 0 -6 0", "M15 6 a3 3 0 1 0 6 0 a3 3 0 1 0 -6 0", "M15 18 a3 3 0 1 0 6 0 a3 3 0 1 0 -6 0", "M8.7 10.7 l6.6 -3.4", "M8.7 13.3 l6.6 3.4") }
    /** Tabler `arrow-back-up` (linha). */
    val Undo: ImageVector by lazy { icon("Undo", LINE, "M9 14 l-4 -4 l4 -4", "M5 10 h11 a4 4 0 1 1 0 8 h-1") }
    /** Tabler `refresh` (linha). */
    val Refresh: ImageVector by lazy { icon("Refresh", LINE, "M20 11 a8.1 8.1 0 0 0 -15.5 -2 m-0.5 -4 v4 h4", "M4 13 a8.1 8.1 0 0 0 15.5 2 m0.5 4 v-4 h-4") }
    /** Tabler `credit-card` (linha). */
    val Credit: ImageVector by lazy { icon("Credit", LINE, "M3 8 a3 3 0 0 1 3 -3 h12 a3 3 0 0 1 3 3 v8 a3 3 0 0 1 -3 3 h-12 a3 3 0 0 1 -3 -3 l0 -8", "M3 10 l18 0", "M7 15 l0.01 0", "M11 15 l2 0") }
    /** Tabler `building-bank` (linha). */
    val Debit: ImageVector by lazy { icon("Debit", LINE, "M3 21 l18 0", "M3 10 l18 0", "M5 6 l7 -3 l7 3", "M4 10 l0 11", "M20 10 l0 11", "M8 14 l0 3", "M12 14 l0 3", "M16 14 l0 3") }
    /** Tabler `ticket` (linha). */
    val Voucher: ImageVector by lazy { icon("Voucher", LINE, "M15 5 l0 2", "M15 11 l0 2", "M15 17 l0 2", "M5 5 h14 a2 2 0 0 1 2 2 v3 a2 2 0 0 0 0 4 v3 a2 2 0 0 1 -2 2 h-14 a2 2 0 0 1 -2 -2 v-3 a2 2 0 0 0 0 -4 v-3 a2 2 0 0 1 2 -2") }
    /** Tabler `qrcode` (linha). */
    val Pix: ImageVector by lazy { icon("Pix", LINE, "M4 5 a1 1 0 0 1 1 -1 h4 a1 1 0 0 1 1 1 v4 a1 1 0 0 1 -1 1 h-4 a1 1 0 0 1 -1 -1 l0 -4", "M7 17 l0 0.01", "M14 5 a1 1 0 0 1 1 -1 h4 a1 1 0 0 1 1 1 v4 a1 1 0 0 1 -1 1 h-4 a1 1 0 0 1 -1 -1 l0 -4", "M7 7 l0 0.01", "M4 15 a1 1 0 0 1 1 -1 h4 a1 1 0 0 1 1 1 v4 a1 1 0 0 1 -1 1 h-4 a1 1 0 0 1 -1 -1 l0 -4", "M17 7 l0 0.01", "M14 14 l3 0", "M20 14 l0 0.01", "M14 14 l0 3", "M14 20 l3 0", "M17 17 l3 0", "M20 17 l0 3") }
    /** Tabler `wallet` (linha). */
    val Wallet: ImageVector by lazy { icon("Wallet", LINE, "M17 8 v-3 a1 1 0 0 0 -1 -1 h-10 a2 2 0 0 0 0 4 h12 a1 1 0 0 1 1 1 v3 m0 4 v3 a1 1 0 0 1 -1 1 h-12 a2 2 0 0 1 -2 -2 v-12", "M20 12 v4 h-4 a2 2 0 0 1 0 -4 h4") }
    /** Tabler `nfc` (linha). */
    val Contactless: ImageVector by lazy { icon("Contactless", LINE, "M11 20 a3 3 0 0 1 -3 -3 v-11 l5 5", "M13 4 a3 3 0 0 1 3 3 v11 l-5 -5", "M4 7 a3 3 0 0 1 3 -3 h10 a3 3 0 0 1 3 3 v10 a3 3 0 0 1 -3 3 h-10 a3 3 0 0 1 -3 -3 l0 -10") }
    /** Tabler `building-store` (linha). */
    val Store: ImageVector by lazy { icon("Store", LINE, "M3 21 l18 0", "M3 7 v1 a3 3 0 0 0 6 0 v-1 m0 1 a3 3 0 0 0 6 0 v-1 m0 1 a3 3 0 0 0 6 0 v-1 h-18 l2 -4 h14 l2 4", "M5 21 l0 -10.15", "M19 21 l0 -10.15", "M9 21 v-4 a2 2 0 0 1 2 -2 h2 a2 2 0 0 1 2 2 v4") }
    /** Tabler `user` (linha). */
    val Person: ImageVector by lazy { icon("Person", LINE, "M8 7 a4 4 0 1 0 8 0 a4 4 0 0 0 -8 0", "M6 21 v-2 a4 4 0 0 1 4 -4 h4 a4 4 0 0 1 4 4 v2") }
    /** Tabler `lock` (linha). */
    val Lock: ImageVector by lazy { icon("Lock", LINE, "M5 13 a2 2 0 0 1 2 -2 h10 a2 2 0 0 1 2 2 v6 a2 2 0 0 1 -2 2 h-10 a2 2 0 0 1 -2 -2 v-6", "M11 16 a1 1 0 1 0 2 0 a1 1 0 0 0 -2 0", "M8 11 v-4 a4 4 0 1 1 8 0 v4") }
    /** Tabler `device-mobile` (linha). */
    val Terminal: ImageVector by lazy { icon("Terminal", LINE, "M6 5 a2 2 0 0 1 2 -2 h8 a2 2 0 0 1 2 2 v14 a2 2 0 0 1 -2 2 h-8 a2 2 0 0 1 -2 -2 v-14", "M11 4 h2", "M12 17 v0.01") }
    /** Tabler `bluetooth` (linha). */
    val Bluetooth: ImageVector by lazy { icon("Bluetooth", LINE, "M7 8 l10 8 l-5 4 l0 -16 l5 4 l-10 8") }
    /** Tabler `usb` (linha). */
    val Usb: ImageVector by lazy { icon("Usb", LINE, "M10 19 a2 2 0 1 0 4 0 a2 2 0 1 0 -4 0", "M12 17 v-11.5", "M7 10 v3 l5 3", "M12 14.5 l5 -2 v-2.5", "M16 10 h2 v-2 h-2 z", "M6 9 a1 1 0 1 0 2 0 a1 1 0 1 0 -2 0", "M10 5.5 h4 l-2 -2.5 z") }
    /** Tabler `download` (linha). */
    val Tables: ImageVector by lazy { icon("Tables", LINE, "M4 17 v2 a2 2 0 0 0 2 2 h12 a2 2 0 0 0 2 -2 v-2", "M7 11 l5 5 l5 -5", "M12 4 l0 12") }
    /** Tabler `logout` (linha). */
    val Logout: ImageVector by lazy { icon("Logout", LINE, "M14 8 v-2 a2 2 0 0 0 -2 -2 h-7 a2 2 0 0 0 -2 2 v12 a2 2 0 0 0 2 2 h7 a2 2 0 0 0 2 -2 v-2", "M9 12 h12 l-3 -3", "M18 15 l3 -3") }
    /** Tabler `circle-check` (linha). */
    val Success: ImageVector by lazy { icon("Success", LINE, "M3 12 a9 9 0 1 0 18 0 a9 9 0 1 0 -18 0", "M9 12 l2 2 l4 -4") }
    /** Tabler `info-circle` (linha). */
    val Info: ImageVector by lazy { icon("Info", LINE, "M3 12 a9 9 0 1 0 18 0 a9 9 0 0 0 -18 0", "M12 9 h0.01", "M11 12 h1 v4 h1") }
    /** Tabler `alert-triangle` (linha). */
    val Warning: ImageVector by lazy { icon("Warning", LINE, "M12 9 v4", "M10.363 3.591 l-8.106 13.534 a1.914 1.914 0 0 0 1.636 2.871 h16.214 a1.914 1.914 0 0 0 1.636 -2.87 l-8.106 -13.536 a1.914 1.914 0 0 0 -3.274 0", "M12 16 h0.01") }
    /** Tabler `circle-x` (linha). */
    val Error: ImageVector by lazy { icon("Error", LINE, "M3 12 a9 9 0 1 0 18 0 a9 9 0 1 0 -18 0", "M10 10 l4 4 m0 -4 l-4 4") }
    /** Tabler `hourglass` (linha). */
    val Pending: ImageVector by lazy { icon("Pending", LINE, "M6.5 7 h11", "M6.5 17 h11", "M6 20 v-2 a6 6 0 1 1 12 0 v2 a1 1 0 0 1 -1 1 h-10 a1 1 0 0 1 -1 -1", "M6 4 v2 a6 6 0 1 0 12 0 v-2 a1 1 0 0 0 -1 -1 h-10 a1 1 0 0 0 -1 1") }
    /** Tabler `file-code` (linha). */
    val Console: ImageVector by lazy { icon("Console", LINE, "M14 3 v4 a1 1 0 0 0 1 1 h4", "M17 21 h-10 a2 2 0 0 1 -2 -2 v-14 a2 2 0 0 1 2 -2 h7 l5 5 v11 a2 2 0 0 1 -2 2", "M10 13 l-1 2 l1 2", "M14 13 l1 2 l-1 2") }
    /** Tabler `code-circle` (linha). */
    val Json: ImageVector by lazy { icon("Json", LINE, "M10 14 l-2 -2 l2 -2", "M14 10 l2 2 l-2 2", "M3 12 a9 9 0 1 0 18 0 a9 9 0 1 0 -18 0") }
    /** Tabler `bug` (linha). */
    val Bug: ImageVector by lazy { icon("Bug", LINE, "M9 9 v-1 a3 3 0 0 1 6 0 v1", "M8 9 h8 a6 6 0 0 1 1 3 v3 a5 5 0 0 1 -10 0 v-3 a6 6 0 0 1 1 -3", "M3 13 l4 0", "M17 13 l4 0", "M12 20 l0 -6", "M4 19 l3.35 -2", "M20 19 l-3.35 -2", "M4 7 l3.75 2.4", "M20 7 l-3.75 2.4") }
    /** Tabler `home` (linha). */
    val Home: ImageVector by lazy { icon("Home", LINE, "M5 12 l-2 0 l9 -9 l9 9 l-2 0", "M5 12 v7 a2 2 0 0 0 2 2 h10 a2 2 0 0 0 2 -2 v-7", "M9 21 v-6 a2 2 0 0 1 2 -2 h2 a2 2 0 0 1 2 2 v6") }
    /** Tabler `filter` (linha). */
    val Filter: ImageVector by lazy { icon("Filter", LINE, "M4 4 h16 v2.172 a2 2 0 0 1 -0.586 1.414 l-4.414 4.414 v7 l-6 2 v-8.5 l-4.48 -4.928 a2 2 0 0 1 -0.52 -1.345 v-2.227") }
    /** Tabler `calendar` (linha). */
    val Calendar: ImageVector by lazy { icon("Calendar", LINE, "M4 7 a2 2 0 0 1 2 -2 h12 a2 2 0 0 1 2 2 v12 a2 2 0 0 1 -2 2 h-12 a2 2 0 0 1 -2 -2 v-12", "M16 3 v4", "M8 3 v4", "M4 11 h16", "M11 15 h1", "M12 15 v3") }
    /** Tabler `settings` (linha). */
    val Settings: ImageVector by lazy { icon("Settings", LINE, "M10.325 4.317 c0.426 -1.756 2.924 -1.756 3.35 0 a1.724 1.724 0 0 0 2.573 1.066 c1.543 -0.94 3.31 0.826 2.37 2.37 a1.724 1.724 0 0 0 1.065 2.572 c1.756 0.426 1.756 2.924 0 3.35 a1.724 1.724 0 0 0 -1.066 2.573 c0.94 1.543 -0.826 3.31 -2.37 2.37 a1.724 1.724 0 0 0 -2.572 1.065 c-0.426 1.756 -2.924 1.756 -3.35 0 a1.724 1.724 0 0 0 -2.573 -1.066 c-1.543 0.94 -3.31 -0.826 -2.37 -2.37 a1.724 1.724 0 0 0 -1.065 -2.572 c-1.756 -0.426 -1.756 -2.924 0 -3.35 a1.724 1.724 0 0 0 1.066 -2.573 c-0.94 -1.543 0.826 -3.31 2.37 -2.37 c1 0.608 2.296 0.07 2.572 -1.065", "M9 12 a3 3 0 1 0 6 0 a3 3 0 0 0 -6 0") }
    /** Tabler `key` (linha). */
    val Key: ImageVector by lazy { icon("Key", LINE, "M16.555 3.843 l3.602 3.602 a2.877 2.877 0 0 1 0 4.069 l-2.643 2.643 a2.877 2.877 0 0 1 -4.069 0 l-0.301 -0.301 l-6.558 6.558 a2 2 0 0 1 -1.239 0.578 l-0.175 0.008 h-1.172 a1 1 0 0 1 -0.993 -0.883 l-0.007 -0.117 v-1.172 a2 2 0 0 1 0.467 -1.284 l0.119 -0.13 l0.414 -0.414 h2 v-2 h2 v-2 l2.144 -2.144 l-0.301 -0.301 a2.877 2.877 0 0 1 0 -4.069 l2.643 -2.643 a2.877 2.877 0 0 1 4.069 0", "M15 9 h0.01") }
    /** Tabler `wifi` (linha). */
    val Wifi: ImageVector by lazy { icon("Wifi", LINE, "M12 18 l0.01 0", "M9.172 15.172 a4 4 0 0 1 5.656 0", "M6.343 12.343 a8 8 0 0 1 11.314 0", "M3.515 9.515 c4.686 -4.687 12.284 -4.687 17 0") }
    /** Tabler `plug-connected` (linha). */
    val Plug: ImageVector by lazy { icon("Plug", LINE, "M7 12 l5 5 l-1.5 1.5 a3.536 3.536 0 1 1 -5 -5 l1.5 -1.5", "M17 12 l-5 -5 l1.5 -1.5 a3.536 3.536 0 1 1 5 5 l-1.5 1.5", "M3 21 l2.5 -2.5", "M18.5 5.5 l2.5 -2.5", "M10 11 l-2 2", "M13 14 l-2 2") }
    /** Tabler `calculator` (linha). */
    val Calculator: ImageVector by lazy { icon("Calculator", LINE, "M4 5 a2 2 0 0 1 2 -2 h12 a2 2 0 0 1 2 2 v14 a2 2 0 0 1 -2 2 h-12 a2 2 0 0 1 -2 -2 l0 -14", "M8 8 a1 1 0 0 1 1 -1 h6 a1 1 0 0 1 1 1 v1 a1 1 0 0 1 -1 1 h-6 a1 1 0 0 1 -1 -1 l0 -1", "M8 14 l0 0.01", "M12 14 l0 0.01", "M16 14 l0 0.01", "M8 17 l0 0.01", "M12 17 l0 0.01", "M16 17 l0 0.01") }
    /** Tabler `backspace` (linha). */
    val Backspace: ImageVector by lazy { icon("Backspace", LINE, "M20 6 a1 1 0 0 1 1 1 v10 a1 1 0 0 1 -1 1 h-11 l-5 -5 a1.5 1.5 0 0 1 0 -2 l5 -5 l11 0", "M12 10 l4 4 m0 -4 l-4 4") }
    /** Tabler `trash` (linha). */
    val Trash: ImageVector by lazy { icon("Trash", LINE, "M4 7 l16 0", "M10 11 l0 6", "M14 11 l0 6", "M5 7 l1 12 a2 2 0 0 0 2 2 h8 a2 2 0 0 0 2 -2 l1 -12", "M9 7 v-3 a1 1 0 0 1 1 -1 h4 a1 1 0 0 1 1 1 v3") }
    /** Tabler `pencil` (linha). */
    val Edit: ImageVector by lazy { icon("Edit", LINE, "M4 20 h4 l10.5 -10.5 a2.828 2.828 0 1 0 -4 -4 l-10.5 10.5 v4", "M13.5 6.5 l4 4") }
    /** Tabler `link` (linha). */
    val Link: ImageVector by lazy { icon("Link", LINE, "M9 15 l6 -6", "M11 6 l0.463 -0.536 a5 5 0 0 1 7.071 7.072 l-0.534 0.464", "M13 18 l-0.397 0.534 a5.068 5.068 0 0 1 -7.127 0 a4.972 4.972 0 0 1 0 -7.071 l0.524 -0.463") }
    /** Tabler `unlink` (linha). */
    val Unlink: ImageVector by lazy { icon("Unlink", LINE, "M17 22 v-2", "M9 15 l6 -6", "M11 6 l0.463 -0.536 a5 5 0 0 1 7.071 7.072 l-0.534 0.464", "M13 18 l-0.397 0.534 a5.068 5.068 0 0 1 -7.127 0 a4.972 4.972 0 0 1 0 -7.071 l0.524 -0.463", "M20 17 h2", "M2 7 h2", "M7 2 v2") }
    /** Tabler `arrow-left` (linha). */
    val Back: ImageVector by lazy { icon("Back", LINE, "M5 12 l14 0", "M5 12 l6 6", "M5 12 l6 -6") }
    /** Tabler `arrow-right` (linha). */
    val Forward: ImageVector by lazy { icon("Forward", LINE, "M5 12 l14 0", "M13 18 l6 -6", "M13 6 l6 6") }
    /** Tabler `chevron-right` (linha). */
    val Chevron: ImageVector by lazy { icon("Chevron", LINE, "M9 6 l6 6 l-6 6") }
    /** Tabler `chevron-down` (linha). */
    val ChevronDown: ImageVector by lazy { icon("ChevronDown", LINE, "M6 9 l6 6 l6 -6") }
    /** Tabler `x` (linha). */
    val Close: ImageVector by lazy { icon("Close", LINE, "M18 6 l-12 12", "M6 6 l12 12") }
    /** Tabler `plus` (linha). */
    val Plus: ImageVector by lazy { icon("Plus", LINE, "M12 5 l0 14", "M5 12 l14 0") }
    /** Tabler `minus` (linha). */
    val Minus: ImageVector by lazy { icon("Minus", LINE, "M5 12 l14 0") }
    /** Tabler `check` (linha). */
    val Check: ImageVector by lazy { icon("Check", LINE, "M5 12 l5 5 l10 -10") }
}
