package br.com.aditum.paytrial.app

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface

/**
 * Comprovante (linhas do `Charge`) → bitmap de 384 px para o `IPrinterSdk` do terminal, em fonte monoespaçada com as
 * 32 colunas do papel. É como um integrador imprime: o serviço entrega as linhas, o app monta a imagem.
 *
 * Com [logo] (o do estabelecimento, de `getMerchantCustomization`, ou o do próprio app), ele vai centralizado no topo
 * **já em preto e branco puro** ([thermal]): a impressora térmica só tem ponto preto ou nenhum, e cada driver decide
 * sozinho o que fazer com cor e cinza. Tirar só a saturação deixava um logo verde num cinza-claro que o driver da
 * Gertec jogava para o branco — saía quase apagado (GPOS700, 25/09).
 */
object ReceiptPrinter {
    private const val LOGO_MAX_WIDTH = 0.6f
    private const val LOGO_MAX_HEIGHT_PX = 120

    fun render(lines: List<String>, logo: Bitmap? = null, widthPx: Int = 384, columns: Int = 32): Bitmap {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = Typeface.MONOSPACE; color = Color.BLACK }
        var size = 28f; paint.textSize = size
        while (paint.measureText("W".repeat(columns)) > widthPx - 8 && size > 8f) { size -= 0.5f; paint.textSize = size }
        val fm = paint.fontMetrics; val lineH = (fm.descent - fm.ascent + 2).toInt()
        val logoRect = logo?.takeIf { it.width > 0 && it.height > 0 }?.let { l ->
            val scale = minOf(widthPx * LOGO_MAX_WIDTH / l.width, LOGO_MAX_HEIGHT_PX.toFloat() / l.height)
            val w = l.width * scale; val h = l.height * scale
            RectF((widthPx - w) / 2f, 8f, (widthPx + w) / 2f, 8f + h)
        }
        val top = logoRect?.let { (it.bottom + lineH).toInt() } ?: 0
        val bmp = Bitmap.createBitmap(widthPx, top + (lines.size + 3) * lineH, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp); c.drawColor(Color.WHITE)
        if (logo != null && logoRect != null) {
            val w = logoRect.width().toInt().coerceAtLeast(1); val h = logoRect.height().toInt().coerceAtLeast(1)
            c.drawBitmap(thermal(logo, w, h), logoRect.left, logoRect.top, null)
        }
        var y = top - fm.ascent + 4
        for (l in lines) { c.drawText(l.take(columns), 4f, y, paint); y += lineH }
        return bmp
    }

    /**
     * Logo no tamanho final → preto e branco. A transparência vira o branco do papel; o limiar sai da própria imagem
     * (Otsu: o corte que melhor separa os tons em dois grupos), então um logo claro — verde, amarelo — ainda fica preto
     * onde há desenho, e um de fundo escuro com letra clara fica como é. Imagem de um tom só: corte no meio.
     */
    private fun thermal(logo: Bitmap, w: Int, h: Int): Bitmap {
        val px = IntArray(w * h)
        Bitmap.createScaledBitmap(logo, w, h, true).getPixels(px, 0, w, 0, 0, w, h)
        val hist = IntArray(256); val lum = IntArray(px.size)
        for (i in px.indices) {
            val p = px[i]; val a = Color.alpha(p) / 255f
            fun over(ch: Int) = ch * a + 255 * (1 - a)
            val l = (0.299f * over(Color.red(p)) + 0.587f * over(Color.green(p)) + 0.114f * over(Color.blue(p))).toInt().coerceIn(0, 255)
            lum[i] = l; hist[l]++
        }
        val cut = otsu(hist, px.size) ?: 127
        for (i in px.indices) px[i] = if (lum[i] <= cut) Color.BLACK else Color.WHITE
        return Bitmap.createBitmap(px, w, h, Bitmap.Config.ARGB_8888)
    }

    /** Limiar de Otsu sobre o histograma de luminância; `null` quando não há dois grupos (imagem de um tom só). */
    private fun otsu(hist: IntArray, total: Int): Int? {
        var sum = 0.0; for (t in 0..255) sum += t.toDouble() * hist[t]
        var sumB = 0.0; var wB = 0; var best = -1.0; var cut: Int? = null
        for (t in 0..254) {
            wB += hist[t]; if (wB == 0) continue
            val wF = total - wB; if (wF == 0) break
            sumB += t.toDouble() * hist[t]
            val mB = sumB / wB; val mF = (sum - sumB) / wF
            val between = wB.toDouble() * wF * (mB - mF) * (mB - mF)
            if (between > best) { best = between; cut = t }
        }
        return cut
    }
}
