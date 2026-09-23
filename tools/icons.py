#!/usr/bin/env python3
"""Gera src/main/kotlin/br/com/aditum/paytrial/designsystem/PtIcons.kt a partir do Tabler Icons (MIT).

Regra do design system do Aditum Pay Trial: ícone PREENCHIDO quando o Tabler tem a versão preenchida; em LINHA
(traço 2, pontas e junções redondas) nos glifos que são linha por natureza (setas, fechar, mais, menos, marcar) e nos
objetos que o Tabler não tem preenchidos. A caixa invisível de 24x24 que abre todo SVG do Tabler é descartada.

Uso:  python3 tools/icons.py            (baixa o pacote npm na versão fixa abaixo; precisa de rede só aqui)
O Kotlin gerado é commitado: o build do app nunca baixa nada.
"""
import io, json, os, re, tarfile, urllib.request

TABLER_VERSION = "3.48.0"
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DEST = os.path.join(ROOT, "src/main/kotlin/br/com/aditum/paytrial/designsystem/PtIcons.kt")

# nome no app -> (nome no Tabler, glifo de linha?)
ICONS = {
    "Sale": ("cash-banknote", False), "Cancel": ("square-x", False), "PreAuth": ("clock", False),
    "Confirm": ("circle-check", False), "Lookup": ("search", False), "Report": ("chart-pie", False),
    "Receipt": ("receipt", False), "More": ("category", False), "Print": ("printer", False), "Send": ("send", False),
    "Mail": ("mail", False), "Sms": ("message", False), "Copy": ("copy", False), "Share": ("share", False),
    "Undo": ("arrow-back-up", False), "Refresh": ("refresh", False), "Credit": ("credit-card", False),
    "Debit": ("building-bank", False), "Voucher": ("ticket", False), "Pix": ("qrcode", False), "Wallet": ("wallet", False),
    "Contactless": ("nfc", False), "Store": ("building-store", False), "Person": ("user", False), "Lock": ("lock", False),
    "Terminal": ("device-mobile", False), "Tables": ("download", False), "Logout": ("logout", False),
    "Success": ("circle-check", False), "Info": ("info-circle", False), "Warning": ("alert-triangle", False),
    "Error": ("circle-x", False), "Pending": ("hourglass", False), "Console": ("file-code", False), "Json": ("code-circle", False),
    "Bug": ("bug", False), "Home": ("home", False), "Filter": ("filter", False), "Calendar": ("calendar", False),
    "Settings": ("settings", False), "Key": ("key", False), "Wifi": ("wifi", False), "Plug": ("plug-connected", False),
    "Calculator": ("calculator", False), "Backspace": ("backspace", False), "Trash": ("trash", False), "Edit": ("pencil", False),
    "Link": ("link", False), "Unlink": ("unlink", False),
    "Back": ("arrow-left", True), "Forward": ("arrow-right", True), "Chevron": ("chevron-right", True),
    "ChevronDown": ("chevron-down", True), "Close": ("x", True), "Plus": ("plus", True), "Minus": ("minus", True),
    "Check": ("check", True),
}

def load():
    meta = json.load(urllib.request.urlopen(f"https://registry.npmjs.org/@tabler/icons/{TABLER_VERSION}", timeout=60))
    data = urllib.request.urlopen(meta["dist"]["tarball"], timeout=120).read()
    tar = tarfile.open(fileobj=io.BytesIO(data))
    files = {}
    for m in tar.getmembers():
        mm = re.match(r"package/icons/(outline|filled)/(.+)\.svg$", m.name)
        if mm: files[(mm.group(1), mm.group(2))] = tar.extractfile(m).read().decode()
    return files

def main():
    files = load()
    out = []
    for key, (name, glyph) in ICONS.items():
        kind = "filled" if (not glyph and ("filled", name) in files) else "outline"
        svg = files[(kind, name)]
        paths = [d for d in re.findall(r'<path[^>]*\sd="([^"]+)"', svg) if d != "M0 0h24v24H0z"]
        assert paths, name
        style = "FILL" if kind == "filled" else "LINE"
        args = ", ".join('"' + d + '"' for d in paths)
        out.append(f'    /** Tabler `{name}` ({"preenchido" if kind == "filled" else "linha"}). */\n    val {key}: ImageVector by lazy {{ icon("{key}", {style}, {args}) }}')
    kt = f'''package br.com.aditum.paytrial.designsystem

// GERADO POR tools/icons.py — NÃO EDITAR À MÃO. Tabler Icons {TABLER_VERSION} (MIT, © Paweł Kuna), caixa 24x24.

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
    ImageVector.Builder(name = name, defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply {{
        for (d in paths) {{
            if (filled) addPath(pathData = addPathNodes(d), fill = SolidColor(Color.Black))
            else addPath(pathData = addPathNodes(d), fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        }}
    }}.build()

/** Ícones do Aditum Pay Trial (Tabler). Use com `Icon(PtIcons.Print, …)`: a cor vem do `tint`. */
object PtIcons {{
{chr(10).join(out)}
}}
'''
    os.makedirs(os.path.dirname(DEST), exist_ok=True)
    open(DEST, "w").write(kt)
    nfill = kt.count("(preenchido)"); print(f"{len(ICONS)} ícones ({nfill} preenchidos, {len(ICONS) - nfill} em linha) -> {DEST}")

if __name__ == "__main__":
    main()
