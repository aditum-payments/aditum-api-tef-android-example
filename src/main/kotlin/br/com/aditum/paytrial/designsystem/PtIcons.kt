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
    /** Tabler `cash-banknote` (preenchido). */
    val Sale: ImageVector by lazy { icon("Sale", FILL, "M19 5 a3 3 0 0 1 3 3 v8 a3 3 0 0 1 -3 3 h-14 a3 3 0 0 1 -3 -3 v-8 a3 3 0 0 1 3 -3 z M12 9 a3 3 0 0 0 -2.996 2.85 l-0.004 0.15 a3 3 0 1 0 3 -3 m6.01 2 h-0.01 a1 1 0 0 0 0 2 h0.01 a1 1 0 0 0 0 -2 m-12 0 h-0.01 a1 1 0 1 0 0.01 2 a1 1 0 0 0 0 -2") }
    /** Tabler `square-x` (preenchido). */
    val Cancel: ImageVector by lazy { icon("Cancel", FILL, "M19 2 h-14 a3 3 0 0 0 -3 3 v14 a3 3 0 0 0 3 3 h14 a3 3 0 0 0 3 -3 v-14 a3 3 0 0 0 -3 -3 z M9.613 8.21 l0.094 0.083 l2.293 2.292 l2.293 -2.292 a1 1 0 0 1 1.497 1.32 l-0.083 0.094 l-2.292 2.293 l2.292 2.293 a1 1 0 0 1 -1.32 1.497 l-0.094 -0.083 l-2.293 -2.292 l-2.293 2.292 a1 1 0 0 1 -1.497 -1.32 l0.083 -0.094 l2.292 -2.293 l-2.292 -2.293 a1 1 0 0 1 1.32 -1.497 z") }
    /** Tabler `clock` (preenchido). */
    val PreAuth: ImageVector by lazy { icon("PreAuth", FILL, "M17 3.34 a10 10 0 1 1 -14.995 8.984 l-0.005 -0.324 l0.005 -0.324 a10 10 0 0 1 14.995 -8.336 z M12 6 a1 1 0 0 0 -0.993 0.883 l-0.007 0.117 v5 l0.009 0.131 a1 1 0 0 0 0.197 0.477 l0.087 0.1 l3 3 l0.094 0.082 a1 1 0 0 0 1.226 0 l0.094 -0.083 l0.083 -0.094 a1 1 0 0 0 0 -1.226 l-0.083 -0.094 l-2.707 -2.708 v-4.585 l-0.007 -0.117 a1 1 0 0 0 -0.993 -0.883 z") }
    /** Tabler `circle-check` (preenchido). */
    val Confirm: ImageVector by lazy { icon("Confirm", FILL, "M17 3.34 a10 10 0 1 1 -14.995 8.984 l-0.005 -0.324 l0.005 -0.324 a10 10 0 0 1 14.995 -8.336 z M15.707 9.293 a1 1 0 0 0 -1.32 -0.083 l-0.094 0.083 l-3.293 3.292 l-1.293 -1.292 l-0.094 -0.083 a1 1 0 0 0 -1.403 1.403 l0.083 0.094 l2 2 l0.094 0.083 a1 1 0 0 0 1.226 0 l0.094 -0.083 l4 -4 l0.083 -0.094 a1 1 0 0 0 -0.083 -1.32 z") }
    /** Tabler `search` (preenchido). */
    val Lookup: ImageVector by lazy { icon("Lookup", FILL, "M14 3.072 a8 8 0 0 1 2.32 11.834 l5.387 5.387 a1 1 0 0 1 -1.414 1.414 l-5.388 -5.387 a8 8 0 1 1 -0.905 -13.249") }
    /** Tabler `chart-pie` (preenchido). */
    val Report: ImageVector by lazy { icon("Report", FILL, "M9.883 2.207 a1.9 1.9 0 0 1 2.087 1.522 l0.025 0.167 l0.005 0.104 v7 a1 1 0 0 0 0.883 0.993 l0.117 0.007 h6.8 a2 2 0 0 1 2 2 a1 1 0 0 1 -0.026 0.226 a10 10 0 1 1 -12.27 -11.933 l0.27 -0.067 l0.11 -0.02 z", "M14 3.5 v5.5 a1 1 0 0 0 1 1 h5.5 a1 1 0 0 0 0.943 -1.332 a10 10 0 0 0 -6.11 -6.111 a1 1 0 0 0 -1.333 0.943 z") }
    /** Tabler `receipt` (preenchido). */
    val Receipt: ImageVector by lazy { icon("Receipt", FILL, "M17 2 a3 3 0 0 1 3 3 v16 a1 1 0 0 1 -1.555 0.832 l-2.318 -1.545 l-1.42 1.42 a1 1 0 0 1 -1.32 0.083 l-0.094 -0.083 l-1.293 -1.292 l-1.293 1.292 a1 1 0 0 1 -1.32 0.083 l-0.094 -0.083 l-1.421 -1.42 l-2.317 1.545 l-0.019 0.012 l-0.054 0.03 l-0.028 0.017 l-0.054 0.023 l-0.05 0.023 l-0.049 0.015 l-0.06 0.019 l-0.052 0.009 l-0.057 0.011 l-0.084 0.006 l-0.026 0.003 h-0.022 l-0.049 -0.003 h-0.039 l-0.013 -0.003 h-0.016 l-0.041 -0.008 l-0.038 -0.005 l-0.015 -0.005 l-0.018 -0.002 l-0.034 -0.011 l-0.04 -0.01 l-0.019 -0.007 l-0.015 -0.004 l-0.029 -0.013 l-0.04 -0.015 l-0.021 -0.011 l-0.013 -0.005 l-0.028 -0.016 l-0.036 -0.018 l-0.014 -0.01 l-0.018 -0.01 l-0.038 -0.027 l-0.022 -0.014 l-0.01 -0.009 l-0.02 -0.014 l-0.045 -0.041 l-0.012 -0.008 l-0.024 -0.024 l-0.035 -0.039 l-0.02 -0.02 l-0.007 -0.011 l-0.011 -0.012 l-0.032 -0.045 l-0.02 -0.025 l-0.012 -0.019 l-0.03 -0.054 l-0.017 -0.028 l-0.023 -0.054 l-0.023 -0.05 a1 1 0 0 1 -0.034 -0.108 l-0.01 -0.057 l-0.01 -0.053 l-0.009 -0.132 v-16 a3 3 0 0 1 3 -3 z M15 14 h-2 a1 1 0 0 0 0 2 h2 a1 1 0 0 0 0 -2 m0 -4 h-6 a1 1 0 0 0 0 2 h6 a1 1 0 0 0 0 -2 m0 -4 h-6 a1 1 0 1 0 0 2 h6 a1 1 0 0 0 0 -2") }
    /** Tabler `category` (preenchido). */
    val More: ImageVector by lazy { icon("More", FILL, "M10 3 h-6 a1 1 0 0 0 -1 1 v6 a1 1 0 0 0 1 1 h6 a1 1 0 0 0 1 -1 v-6 a1 1 0 0 0 -1 -1 z", "M20 3 h-6 a1 1 0 0 0 -1 1 v6 a1 1 0 0 0 1 1 h6 a1 1 0 0 0 1 -1 v-6 a1 1 0 0 0 -1 -1 z", "M10 13 h-6 a1 1 0 0 0 -1 1 v6 a1 1 0 0 0 1 1 h6 a1 1 0 0 0 1 -1 v-6 a1 1 0 0 0 -1 -1 z", "M17 13 a4 4 0 1 1 -3.995 4.2 l-0.005 -0.2 l0.005 -0.2 a4 4 0 0 1 3.995 -3.8 z") }
    /** Tabler `printer` (linha). */
    val Print: ImageVector by lazy { icon("Print", LINE, "M17 17 h2 a2 2 0 0 0 2 -2 v-4 a2 2 0 0 0 -2 -2 h-14 a2 2 0 0 0 -2 2 v4 a2 2 0 0 0 2 2 h2", "M17 9 v-4 a2 2 0 0 0 -2 -2 h-6 a2 2 0 0 0 -2 2 v4", "M7 15 a2 2 0 0 1 2 -2 h6 a2 2 0 0 1 2 2 v4 a2 2 0 0 1 -2 2 h-6 a2 2 0 0 1 -2 -2 l0 -4") }
    /** Tabler `send` (preenchido). */
    val Send: ImageVector by lazy { icon("Send", FILL, "M21.864 3.549 l-6.454 17.868 a1.55 1.55 0 0 1 -1.41 0.903 a1.54 1.54 0 0 1 -1.394 -0.874 l-2.88 -5.759 z M20.45 2.135 l-12.139 12.138 l-5.728 -2.864 a1.55 1.55 0 0 1 -0.903 -1.409 c0 -0.606 0.353 -1.157 0.981 -1.44 z") }
    /** Tabler `mail` (preenchido). */
    val Mail: ImageVector by lazy { icon("Mail", FILL, "M22 7.535 v9.465 a3 3 0 0 1 -2.824 2.995 l-0.176 0.005 h-14 a3 3 0 0 1 -2.995 -2.824 l-0.005 -0.176 v-9.465 l9.445 6.297 l0.116 0.066 a1 1 0 0 0 0.878 0 l0.116 -0.066 l9.445 -6.297 z", "M19 4 c1.08 0 2.027 0.57 2.555 1.427 l-9.555 6.37 l-9.555 -6.37 a2.999 2.999 0 0 1 2.354 -1.42 l0.201 -0.007 h14 z") }
    /** Tabler `message` (preenchido). */
    val Sms: ImageVector by lazy { icon("Sms", FILL, "M18 3 a4 4 0 0 1 4 4 v8 a4 4 0 0 1 -4 4 h-4.724 l-4.762 2.857 a1 1 0 0 1 -1.508 -0.743 l-0.006 -0.114 v-2 h-1 a4 4 0 0 1 -3.995 -3.8 l-0.005 -0.2 v-8 a4 4 0 0 1 4 -4 z M14 12 h-6 a1 1 0 0 0 0 2 h6 a1 1 0 0 0 0 -2 m2 -4 h-8 a1 1 0 1 0 0 2 h8 a1 1 0 0 0 0 -2") }
    /** Tabler `copy` (preenchido). */
    val Copy: ImageVector by lazy { icon("Copy", FILL, "M20.926 7.074 a3.67 3.67 0 0 1 1.074 2.593 v8.666 a3.667 3.667 0 0 1 -3.667 3.667 h-8.666 a3.667 3.667 0 0 1 -3.667 -3.667 v-8.666 q0 -0.053 0.005 -0.102 a3.66 3.66 0 0 1 3.662 -3.565 h8.666 c0.973 0 1.905 0.386 2.593 1.074", "M17.374 3.514 a1 1 0 1 1 -1.748 0.972 c-0.221 -0.398 -0.342 -0.486 -0.626 -0.486 h-10 c-0.548 0 -1 0.452 -1 1 v9.998 c0 0.36 0.194 0.692 0.507 0.87 a1 1 0 1 1 -0.99 1.738 a3 3 0 0 1 -1.517 -2.606 v-10 c0 -1.652 1.348 -3 3 -3 h10 c1.094 0 1.828 0.533 2.374 1.514") }
    /** Tabler `share` (linha). */
    val Share: ImageVector by lazy { icon("Share", LINE, "M3 12 a3 3 0 1 0 6 0 a3 3 0 1 0 -6 0", "M15 6 a3 3 0 1 0 6 0 a3 3 0 1 0 -6 0", "M15 18 a3 3 0 1 0 6 0 a3 3 0 1 0 -6 0", "M8.7 10.7 l6.6 -3.4", "M8.7 13.3 l6.6 3.4") }
    /** Tabler `arrow-back-up` (linha). */
    val Undo: ImageVector by lazy { icon("Undo", LINE, "M9 14 l-4 -4 l4 -4", "M5 10 h11 a4 4 0 1 1 0 8 h-1") }
    /** Tabler `refresh` (linha). */
    val Refresh: ImageVector by lazy { icon("Refresh", LINE, "M20 11 a8.1 8.1 0 0 0 -15.5 -2 m-0.5 -4 v4 h4", "M4 13 a8.1 8.1 0 0 0 15.5 2 m0.5 4 v-4 h-4") }
    /** Tabler `credit-card` (preenchido). */
    val Credit: ImageVector by lazy { icon("Credit", FILL, "M22 10 v6 a4 4 0 0 1 -4 4 h-12 a4 4 0 0 1 -4 -4 v-6 h20 z M7.01 14 h-0.01 a1 1 0 1 0 0.01 2 a1 1 0 0 0 0 -2 m5.99 0 h-2 a1 1 0 0 0 0 2 h2 a1 1 0 0 0 0 -2 z M18 4 a4 4 0 0 1 4 4 h-20 a4 4 0 0 1 4 -4 h12 z") }
    /** Tabler `building-bank` (linha). */
    val Debit: ImageVector by lazy { icon("Debit", LINE, "M3 21 l18 0", "M3 10 l18 0", "M5 6 l7 -3 l7 3", "M4 10 l0 11", "M20 10 l0 11", "M8 14 l0 3", "M12 14 l0 3", "M16 14 l0 3") }
    /** Tabler `ticket` (preenchido). */
    val Voucher: ImageVector by lazy { icon("Voucher", FILL, "M14 4 v2 a1 1 0 0 0 2 0 v-2 h3 a3 3 0 0 1 3 3 v3 a1 1 0 0 1 -0.883 0.993 l-0.117 0.007 a1 1 0 0 0 -0.117 1.993 l0.117 0.007 a1 1 0 0 1 1 1 v3 a3 3 0 0 1 -3 3 h-3 v-2 a1 1 0 0 0 -0.883 -0.993 l-0.117 -0.007 a1 1 0 0 0 -1 1 v2 h-9 a3 3 0 0 1 -3 -3 v-3 a1 1 0 0 1 0.883 -0.993 l0.117 -0.007 a1 1 0 0 0 0.117 -1.993 l-0.117 -0.007 a1 1 0 0 1 -1 -1 v-3 a2.995 2.995 0 0 1 2.727 -2.985 l0.222 -0.014 z M15 10 a1 1 0 0 0 -1 1 v2 a1 1 0 0 0 2 0 v-2 a1 1 0 0 0 -1 -1") }
    /** Tabler `qrcode` (linha). */
    val Pix: ImageVector by lazy { icon("Pix", LINE, "M4 5 a1 1 0 0 1 1 -1 h4 a1 1 0 0 1 1 1 v4 a1 1 0 0 1 -1 1 h-4 a1 1 0 0 1 -1 -1 l0 -4", "M7 17 l0 0.01", "M14 5 a1 1 0 0 1 1 -1 h4 a1 1 0 0 1 1 1 v4 a1 1 0 0 1 -1 1 h-4 a1 1 0 0 1 -1 -1 l0 -4", "M7 7 l0 0.01", "M4 15 a1 1 0 0 1 1 -1 h4 a1 1 0 0 1 1 1 v4 a1 1 0 0 1 -1 1 h-4 a1 1 0 0 1 -1 -1 l0 -4", "M17 7 l0 0.01", "M14 14 l3 0", "M20 14 l0 0.01", "M14 14 l0 3", "M14 20 l3 0", "M17 17 l3 0", "M20 17 l0 3") }
    /** Tabler `wallet` (linha). */
    val Wallet: ImageVector by lazy { icon("Wallet", LINE, "M17 8 v-3 a1 1 0 0 0 -1 -1 h-10 a2 2 0 0 0 0 4 h12 a1 1 0 0 1 1 1 v3 m0 4 v3 a1 1 0 0 1 -1 1 h-12 a2 2 0 0 1 -2 -2 v-12", "M20 12 v4 h-4 a2 2 0 0 1 0 -4 h4") }
    /** Tabler `nfc` (linha). */
    val Contactless: ImageVector by lazy { icon("Contactless", LINE, "M11 20 a3 3 0 0 1 -3 -3 v-11 l5 5", "M13 4 a3 3 0 0 1 3 3 v11 l-5 -5", "M4 7 a3 3 0 0 1 3 -3 h10 a3 3 0 0 1 3 3 v10 a3 3 0 0 1 -3 3 h-10 a3 3 0 0 1 -3 -3 l0 -10") }
    /** Tabler `building-store` (linha). */
    val Store: ImageVector by lazy { icon("Store", LINE, "M3 21 l18 0", "M3 7 v1 a3 3 0 0 0 6 0 v-1 m0 1 a3 3 0 0 0 6 0 v-1 m0 1 a3 3 0 0 0 6 0 v-1 h-18 l2 -4 h14 l2 4", "M5 21 l0 -10.15", "M19 21 l0 -10.15", "M9 21 v-4 a2 2 0 0 1 2 -2 h2 a2 2 0 0 1 2 2 v4") }
    /** Tabler `user` (preenchido). */
    val Person: ImageVector by lazy { icon("Person", FILL, "M12 2 a5 5 0 1 1 -5 5 l0.005 -0.217 a5 5 0 0 1 4.995 -4.783 z", "M14 14 a5 5 0 0 1 5 5 v1 a2 2 0 0 1 -2 2 h-10 a2 2 0 0 1 -2 -2 v-1 a5 5 0 0 1 5 -5 h4 z") }
    /** Tabler `lock` (preenchido). */
    val Lock: ImageVector by lazy { icon("Lock", FILL, "M12 2 a5 5 0 0 1 5 5 v3 a3 3 0 0 1 3 3 v6 a3 3 0 0 1 -3 3 h-10 a3 3 0 0 1 -3 -3 v-6 a3 3 0 0 1 3 -3 v-3 a5 5 0 0 1 5 -5 m0 12 a2 2 0 0 0 -1.995 1.85 l-0.005 0.15 a2 2 0 1 0 2 -2 m0 -10 a3 3 0 0 0 -3 3 v3 h6 v-3 a3 3 0 0 0 -3 -3") }
    /** Tabler `device-mobile` (preenchido). */
    val Terminal: ImageVector by lazy { icon("Terminal", FILL, "M16 2 a3 3 0 0 1 2.995 2.824 l0.005 0.176 v14 a3 3 0 0 1 -2.824 2.995 l-0.176 0.005 h-8 a3 3 0 0 1 -2.995 -2.824 l-0.005 -0.176 v-14 a3 3 0 0 1 2.824 -2.995 l0.176 -0.005 h8 z M12 16 a1 1 0 0 0 -0.993 0.883 l-0.007 0.117 l0.007 0.127 a1 1 0 0 0 1.986 0 l0.007 -0.117 l-0.007 -0.127 a1 1 0 0 0 -0.993 -0.883 z M13 4 h-2 l-0.117 0.007 a1 1 0 0 0 0 1.986 l0.117 0.007 h2 l0.117 -0.007 a1 1 0 0 0 0 -1.986 l-0.117 -0.007 z") }
    /** Tabler `download` (preenchido). */
    val Tables: ImageVector by lazy { icon("Tables", FILL, "M20 16 a1 1 0 0 1 1 1 v2 a3 3 0 0 1 -3 3 h-12 a3 3 0 0 1 -3 -3 v-2 a1 1 0 0 1 2 0 v2 a1 1 0 0 0 1 1 h12 a1 1 0 0 0 1 -1 v-2 a1 1 0 0 1 1 -1 m-8 -13 a1 1 0 0 1 1 1 v9.585 l3.293 -3.292 a1 1 0 0 1 1.414 1.414 l-5 5 a1 1 0 0 1 -0.09 0.08 l0.09 -0.08 a1 1 0 0 1 -0.674 0.292 l-0.033 0.001 h-0.032 l-0.054 -0.004 l0.086 0.004 a1 1 0 0 1 -0.617 -0.213 a1 1 0 0 1 -0.09 -0.08 l-5 -5 a1 1 0 0 1 1.414 -1.414 l3.293 3.292 v-9.585 a1 1 0 0 1 1 -1") }
    /** Tabler `logout` (linha). */
    val Logout: ImageVector by lazy { icon("Logout", LINE, "M14 8 v-2 a2 2 0 0 0 -2 -2 h-7 a2 2 0 0 0 -2 2 v12 a2 2 0 0 0 2 2 h7 a2 2 0 0 0 2 -2 v-2", "M9 12 h12 l-3 -3", "M18 15 l3 -3") }
    /** Tabler `circle-check` (preenchido). */
    val Success: ImageVector by lazy { icon("Success", FILL, "M17 3.34 a10 10 0 1 1 -14.995 8.984 l-0.005 -0.324 l0.005 -0.324 a10 10 0 0 1 14.995 -8.336 z M15.707 9.293 a1 1 0 0 0 -1.32 -0.083 l-0.094 0.083 l-3.293 3.292 l-1.293 -1.292 l-0.094 -0.083 a1 1 0 0 0 -1.403 1.403 l0.083 0.094 l2 2 l0.094 0.083 a1 1 0 0 0 1.226 0 l0.094 -0.083 l4 -4 l0.083 -0.094 a1 1 0 0 0 -0.083 -1.32 z") }
    /** Tabler `info-circle` (preenchido). */
    val Info: ImageVector by lazy { icon("Info", FILL, "M12 2 c5.523 0 10 4.477 10 10 a10 10 0 0 1 -19.995 0.324 l-0.005 -0.324 l0.004 -0.28 c0.148 -5.393 4.566 -9.72 9.996 -9.72 z M12 11 h-1 l-0.117 0.007 a1 1 0 0 0 0 1.986 l0.117 0.007 v3 l0.007 0.117 a1 1 0 0 0 0.876 0.876 l0.117 0.007 h1 l0.117 -0.007 a1 1 0 0 0 0.876 -0.876 l0.007 -0.117 l-0.007 -0.117 a1 1 0 0 0 -0.764 -0.857 l-0.112 -0.02 l-0.117 -0.006 v-3 l-0.007 -0.117 a1 1 0 0 0 -0.876 -0.876 l-0.117 -0.007 z M12.01 8 l-0.127 0.007 a1 1 0 0 0 0 1.986 l0.117 0.007 l0.127 -0.007 a1 1 0 0 0 0 -1.986 l-0.117 -0.007 z") }
    /** Tabler `alert-triangle` (preenchido). */
    val Warning: ImageVector by lazy { icon("Warning", FILL, "M12 1.67 c0.955 0 1.845 0.467 2.39 1.247 l0.105 0.16 l8.114 13.548 a2.914 2.914 0 0 1 -2.307 4.363 l-0.195 0.008 h-16.225 a2.914 2.914 0 0 1 -2.582 -4.2 l0.099 -0.185 l8.11 -13.538 a2.914 2.914 0 0 1 2.491 -1.403 z M12.01 15 l-0.127 0.007 a1 1 0 0 0 0 1.986 l0.117 0.007 l0.127 -0.007 a1 1 0 0 0 0 -1.986 l-0.117 -0.007 z M12 8 a1 1 0 0 0 -0.993 0.883 l-0.007 0.117 v4 l0.007 0.117 a1 1 0 0 0 1.986 0 l0.007 -0.117 v-4 l-0.007 -0.117 a1 1 0 0 0 -0.993 -0.883 z") }
    /** Tabler `circle-x` (preenchido). */
    val Error: ImageVector by lazy { icon("Error", FILL, "M17 3.34 a10 10 0 1 1 -14.995 8.984 l-0.005 -0.324 l0.005 -0.324 a10 10 0 0 1 14.995 -8.336 z M10.511 9.14 a1 1 0 0 0 -1.218 1.567 l1.292 1.293 l-1.292 1.293 l-0.083 0.094 a1 1 0 0 0 1.497 1.32 l1.293 -1.292 l1.293 1.292 l0.094 0.083 a1 1 0 0 0 1.32 -1.497 l-1.292 -1.293 l1.292 -1.293 l0.083 -0.094 a1 1 0 0 0 -1.497 -1.32 l-1.293 1.292 l-1.293 -1.292 l-0.094 -0.083 z") }
    /** Tabler `hourglass` (preenchido). */
    val Pending: ImageVector by lazy { icon("Pending", FILL, "M17 2 a2 2 0 0 1 1.995 1.85 l0.005 0.15 v2 a6.996 6.996 0 0 1 -3.393 6 a6.994 6.994 0 0 1 3.388 5.728 l0.005 0.272 v2 a2 2 0 0 1 -1.85 1.995 l-0.15 0.005 h-10 a2 2 0 0 1 -1.995 -1.85 l-0.005 -0.15 v-2 a6.996 6.996 0 0 1 3.393 -6 a6.994 6.994 0 0 1 -3.388 -5.728 l-0.005 -0.272 v-2 a2 2 0 0 1 1.85 -1.995 l0.15 -0.005 h10 z") }
    /** Tabler `file-code` (preenchido). */
    val Console: ImageVector by lazy { icon("Console", FILL, "M12 2 l0.117 0.007 a1 1 0 0 1 0.876 0.876 l0.007 0.117 v4 l0.005 0.15 a2 2 0 0 0 1.838 1.844 l0.157 0.006 h4 l0.117 0.007 a1 1 0 0 1 0.876 0.876 l0.007 0.117 v9 a3 3 0 0 1 -2.824 2.995 l-0.176 0.005 h-10 a3 3 0 0 1 -2.995 -2.824 l-0.005 -0.176 v-14 a3 3 0 0 1 2.824 -2.995 l0.176 -0.005 z M10.447 13.106 a1 1 0 0 0 -1.341 0.447 l-1 2 a1 1 0 0 0 0 0.894 l1 2 a1 1 0 0 0 1.341 0.447 l0.102 -0.058 a1 1 0 0 0 0.345 -1.283 l-0.776 -1.553 l0.776 -1.553 a1 1 0 0 0 -0.447 -1.341 m4.447 0.447 a1 1 0 0 0 -1.341 -0.447 l-0.102 0.058 a1 1 0 0 0 -0.345 1.283 l0.774 1.553 l-0.775 1.553 a1 1 0 0 0 1.788 0.894 l1 -2 a1 1 0 0 0 0 -0.894 z", "M19 7 h-4 l-0.001 -4.001 z") }
    /** Tabler `code-circle` (preenchido). */
    val Json: ImageVector by lazy { icon("Json", FILL, "M17 3.34 a10 10 0 1 1 -15 8.66 l0.005 -0.324 a10 10 0 0 1 14.995 -8.336 m-6.293 5.953 a1 1 0 0 0 -1.414 0 l-2 2 a1 1 0 0 0 0 1.414 l2 2 a1 1 0 0 0 1.414 0 l0.083 -0.094 a1 1 0 0 0 -0.083 -1.32 l-1.292 -1.293 l1.292 -1.293 a1 1 0 0 0 0 -1.414 m4 0 a1 1 0 0 0 -1.414 0 l-0.083 0.094 a1 1 0 0 0 0.083 1.32 l1.292 1.293 l-1.292 1.293 a1 1 0 0 0 1.414 1.414 l2 -2 a1 1 0 0 0 0 -1.414 z") }
    /** Tabler `bug` (preenchido). */
    val Bug: ImageVector by lazy { icon("Bug", FILL, "M12 4 a4 4 0 0 1 3.995 3.8 l0.005 0.2 a1 1 0 0 1 0.428 0.096 l3.033 -1.938 a1 1 0 1 1 1.078 1.684 l-3.015 1.931 a7.17 7.17 0 0 1 0.476 2.227 h3 a1 1 0 0 1 0 2 h-3 v1 a6.01 6.01 0 0 1 -0.195 1.525 l2.708 1.616 a1 1 0 1 1 -1.026 1.718 l-2.514 -1.501 a6.002 6.002 0 0 1 -3.973 2.56 v-5.918 a1 1 0 0 0 -2 0 v5.917 a6.002 6.002 0 0 1 -3.973 -2.56 l-2.514 1.503 a1 1 0 1 1 -1.026 -1.718 l2.708 -1.616 a6.01 6.01 0 0 1 -0.195 -1.526 v-1 h-3 a1 1 0 0 1 0 -2 h3.001 v-0.055 a7 7 0 0 1 0.474 -2.173 l-3.014 -1.93 a1 1 0 1 1 1.078 -1.684 l3.032 1.939 l0.024 -0.012 l0.068 -0.027 l0.019 -0.005 l0.016 -0.006 l0.032 -0.008 l0.04 -0.013 l0.034 -0.007 l0.034 -0.004 l0.045 -0.008 l0.015 -0.001 l0.015 -0.002 l0.087 -0.004 a4 4 0 0 1 4 -4 z M12 6 a2 2 0 0 0 -2 2 h4 a2 2 0 0 0 -2 -2 z") }
    /** Tabler `home` (preenchido). */
    val Home: ImageVector by lazy { icon("Home", FILL, "M12.707 2.293 l9 9 c0.63 0.63 0.184 1.707 -0.707 1.707 h-1 v6 a3 3 0 0 1 -3 3 h-1 v-7 a3 3 0 0 0 -2.824 -2.995 l-0.176 -0.005 h-2 a3 3 0 0 0 -3 3 v7 h-1 a3 3 0 0 1 -3 -3 v-6 h-1 c-0.89 0 -1.337 -1.077 -0.707 -1.707 l9 -9 a1 1 0 0 1 1.414 0 m0.293 11.707 a1 1 0 0 1 1 1 v7 h-4 v-7 a1 1 0 0 1 0.883 -0.993 l0.117 -0.007 z") }
    /** Tabler `filter` (preenchido). */
    val Filter: ImageVector by lazy { icon("Filter", FILL, "M20 3 h-16 a1 1 0 0 0 -1 1 v2.227 l0.008 0.223 a3 3 0 0 0 0.772 1.795 l4.22 4.641 v8.114 a1 1 0 0 0 1.316 0.949 l6 -2 l0.108 -0.043 a1 1 0 0 0 0.576 -0.906 v-6.586 l4.121 -4.12 a3 3 0 0 0 0.879 -2.123 v-2.171 a1 1 0 0 0 -1 -1 z") }
    /** Tabler `calendar` (preenchido). */
    val Calendar: ImageVector by lazy { icon("Calendar", FILL, "M16 2 a1 1 0 0 1 0.993 0.883 l0.007 0.117 v1 h1 a3 3 0 0 1 2.995 2.824 l0.005 0.176 v12 a3 3 0 0 1 -2.824 2.995 l-0.176 0.005 h-12 a3 3 0 0 1 -2.995 -2.824 l-0.005 -0.176 v-12 a3 3 0 0 1 2.824 -2.995 l0.176 -0.005 h1 v-1 a1 1 0 0 1 1.993 -0.117 l0.007 0.117 v1 h6 v-1 a1 1 0 0 1 1 -1 z M19 9 h-14 v9.625 c0 0.705 0.386 1.286 0.883 1.366 l0.117 0.009 h12 c0.513 0 0.936 -0.53 0.993 -1.215 l0.007 -0.16 v-9.625 z", "M12 12 a1 1 0 0 1 0.993 0.883 l0.007 0.117 v3 a1 1 0 0 1 -1.993 0.117 l-0.007 -0.117 v-2 a1 1 0 0 1 -0.117 -1.993 l0.117 -0.007 h1 z") }
    /** Tabler `settings` (preenchido). */
    val Settings: ImageVector by lazy { icon("Settings", FILL, "M14.647 4.081 a0.724 0.724 0 0 0 1.08 0.448 c2.439 -1.485 5.23 1.305 3.745 3.744 a0.724 0.724 0 0 0 0.447 1.08 c2.775 0.673 2.775 4.62 0 5.294 a0.724 0.724 0 0 0 -0.448 1.08 c1.485 2.439 -1.305 5.23 -3.744 3.745 a0.724 0.724 0 0 0 -1.08 0.447 c-0.673 2.775 -4.62 2.775 -5.294 0 a0.724 0.724 0 0 0 -1.08 -0.448 c-2.439 1.485 -5.23 -1.305 -3.745 -3.744 a0.724 0.724 0 0 0 -0.447 -1.08 c-2.775 -0.673 -2.775 -4.62 0 -5.294 a0.724 0.724 0 0 0 0.448 -1.08 c-1.485 -2.439 1.305 -5.23 3.744 -3.745 a0.722 0.722 0 0 0 1.08 -0.447 c0.673 -2.775 4.62 -2.775 5.294 0 z M12 9 a3 3 0 1 0 0 6 a3 3 0 0 0 0 -6") }
    /** Tabler `key` (preenchido). */
    val Key: ImageVector by lazy { icon("Key", FILL, "M14.52 2 c1.029 0 2.015 0.409 2.742 1.136 l3.602 3.602 a3.877 3.877 0 0 1 0 5.483 l-2.643 2.643 a3.88 3.88 0 0 1 -4.941 0.452 l-0.105 -0.078 l-5.882 5.883 a3 3 0 0 1 -1.68 0.843 l-0.22 0.027 l-0.221 0.009 h-1.172 c-1.014 0 -1.867 -0.759 -1.991 -1.823 l-0.009 -0.177 v-1.172 c0 -0.704 0.248 -1.386 0.73 -1.96 l0.149 -0.161 l0.414 -0.414 a1 1 0 0 1 0.707 -0.293 h1 v-1 a1 1 0 0 1 0.883 -0.993 l0.117 -0.007 h1 v-1 a1 1 0 0 1 0.206 -0.608 l0.087 -0.1 l1.468 -1.469 l-0.076 -0.103 a3.9 3.9 0 0 1 -0.678 -1.963 l-0.007 -0.236 c0 -1.029 0.409 -2.015 1.136 -2.742 l2.643 -2.643 a3.88 3.88 0 0 1 2.741 -1.136 m0.495 5 h-0.02 a2 2 0 1 0 0 4 h0.02 a2 2 0 1 0 0 -4") }
    /** Tabler `wifi` (linha). */
    val Wifi: ImageVector by lazy { icon("Wifi", LINE, "M12 18 l0.01 0", "M9.172 15.172 a4 4 0 0 1 5.656 0", "M6.343 12.343 a8 8 0 0 1 11.314 0", "M3.515 9.515 c4.686 -4.687 12.284 -4.687 17 0") }
    /** Tabler `plug-connected` (linha). */
    val Plug: ImageVector by lazy { icon("Plug", LINE, "M7 12 l5 5 l-1.5 1.5 a3.536 3.536 0 1 1 -5 -5 l1.5 -1.5", "M17 12 l-5 -5 l1.5 -1.5 a3.536 3.536 0 1 1 5 5 l-1.5 1.5", "M3 21 l2.5 -2.5", "M18.5 5.5 l2.5 -2.5", "M10 11 l-2 2", "M13 14 l-2 2") }
    /** Tabler `calculator` (preenchido). */
    val Calculator: ImageVector by lazy { icon("Calculator", FILL, "M18 2 a3 3 0 0 1 3 3 v14 a3 3 0 0 1 -3 3 h-12 a3 3 0 0 1 -3 -3 v-14 a3 3 0 0 1 3 -3 z M8 17 a1 1 0 0 0 -1 1 l0.007 0.127 a1 1 0 0 0 1.993 -0.117 l-0.007 -0.127 a1 1 0 0 0 -0.993 -0.883 z M12 17 a1 1 0 0 0 -1 1 l0.007 0.127 a1 1 0 0 0 1.993 -0.117 l-0.007 -0.127 a1 1 0 0 0 -0.993 -0.883 z M16 17 a1 1 0 0 0 -1 1 l0.007 0.127 a1 1 0 0 0 1.993 -0.117 l-0.007 -0.127 a1 1 0 0 0 -0.993 -0.883 z M8 13 a1 1 0 0 0 -1 1 l0.007 0.127 a1 1 0 0 0 1.993 -0.117 l-0.007 -0.127 a1 1 0 0 0 -0.993 -0.883 z M12 13 a1 1 0 0 0 -1 1 l0.007 0.127 a1 1 0 0 0 1.993 -0.117 l-0.007 -0.127 a1 1 0 0 0 -0.993 -0.883 z M16 13 a1 1 0 0 0 -1 1 l0.007 0.127 a1 1 0 0 0 1.993 -0.117 l-0.007 -0.127 a1 1 0 0 0 -0.993 -0.883 z M15 6 h-6 a2 2 0 0 0 -2 2 v1 a2 2 0 0 0 2 2 h6 a2 2 0 0 0 2 -2 v-1 a2 2 0 0 0 -2 -2 z") }
    /** Tabler `backspace` (preenchido). */
    val Backspace: ImageVector by lazy { icon("Backspace", FILL, "M20 5 a2 2 0 0 1 1.995 1.85 l0.005 0.15 v10 a2 2 0 0 1 -1.85 1.995 l-0.15 0.005 h-11 a1 1 0 0 1 -0.608 -0.206 l-0.1 -0.087 l-5.037 -5.04 c-0.809 -0.904 -0.847 -2.25 -0.083 -3.23 l0.12 -0.144 l5 -5 a1 1 0 0 1 0.577 -0.284 l0.131 -0.009 h11 z M12.511 9.14 a1 1 0 0 0 -1.301 1.473 l0.083 0.094 l1.292 1.293 l-1.292 1.293 l-0.083 0.094 a1 1 0 0 0 1.403 1.403 l0.094 -0.083 l1.293 -1.292 l1.293 1.292 l0.094 0.083 a1 1 0 0 0 1.403 -1.403 l-0.083 -0.094 l-1.292 -1.293 l1.292 -1.293 l0.083 -0.094 a1 1 0 0 0 -1.403 -1.403 l-0.094 0.083 l-1.293 1.292 l-1.293 -1.292 l-0.094 -0.083 l-0.102 -0.07 z") }
    /** Tabler `trash` (preenchido). */
    val Trash: ImageVector by lazy { icon("Trash", FILL, "M20 6 a1 1 0 0 1 0.117 1.993 l-0.117 0.007 h-0.081 l-0.919 11 a3 3 0 0 1 -2.824 2.995 l-0.176 0.005 h-8 c-1.598 0 -2.904 -1.249 -2.992 -2.75 l-0.005 -0.167 l-0.923 -11.083 h-0.08 a1 1 0 0 1 -0.117 -1.993 l0.117 -0.007 z M10 10 a1 1 0 0 0 -1 1 v6 a1 1 0 0 0 2 0 v-6 a1 1 0 0 0 -1 -1 m4 0 a1 1 0 0 0 -1 1 v6 a1 1 0 0 0 2 0 v-6 a1 1 0 0 0 -1 -1", "M14 2 a2 2 0 0 1 2 2 a1 1 0 0 1 -1.993 0.117 l-0.007 -0.117 h-4 l-0.007 0.117 a1 1 0 0 1 -1.993 -0.117 a2 2 0 0 1 1.85 -1.995 l0.15 -0.005 z") }
    /** Tabler `pencil` (preenchido). */
    val Edit: ImageVector by lazy { icon("Edit", FILL, "M12.085 6.5 l5.415 5.415 l-8.793 8.792 a1 1 0 0 1 -0.707 0.293 h-4 a1 1 0 0 1 -1 -1 v-4 a1 1 0 0 1 0.293 -0.707 z M17.491 3.802 a3.828 3.828 0 0 1 1.716 6.405 l-0.292 0.293 l-5.415 -5.415 l0.293 -0.292 a3.83 3.83 0 0 1 3.698 -0.991") }
    /** Tabler `link` (preenchido). */
    val Link: ImageVector by lazy { icon("Link", FILL, "M15.707 8.293 a1 1 0 0 1 0 1.414 l-6 6 a1 1 0 1 1 -1.414 -1.414 l6 -6 a1 1 0 0 1 1.414 0", "M19.242 4.757 c2.343 2.344 2.342 6.143 -0.052 8.534 l-0.534 0.464 a1 1 0 1 1 -1.312 -1.51 l0.483 -0.416 a4 4 0 0 0 0 -5.657 c-1.562 -1.563 -4.095 -1.563 -5.607 -0.054 l-0.463 0.536 a1 1 0 1 1 -1.514 -1.308 l0.513 -0.59 a6 6 0 0 1 8.486 0.001", "M6.75 10.338 a1 1 0 0 1 -0.088 1.411 l-0.483 0.425 a3.97 3.97 0 0 0 0 5.649 a4.064 4.064 0 0 0 5.678 0.038 l0.34 -0.458 a1 1 0 1 1 1.606 1.194 l-0.397 0.534 l-0.1 0.114 a6.07 6.07 0 0 1 -8.533 0 a5.97 5.97 0 0 1 -1.773 -4.247 c0 -1.595 0.638 -3.124 1.814 -4.284 l0.524 -0.463 a1 1 0 0 1 1.411 0.087") }
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
