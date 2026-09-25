package br.com.aditum.paytrial.app

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface

/**
 * Comprovante (linhas do `Charge`) → bitmap de 384 px para o `IPrinterSdk` do terminal, em fonte monoespaçada com as
 * 32 colunas do papel. É como um integrador imprime: o serviço entrega as linhas, o app monta a imagem.
 *
 * Com [logo] (o do estabelecimento, de `getMerchantCustomization`, ou o do próprio app), ele vai centralizado no topo,
 * em tons de cinza sobre branco: a impressora térmica é monocromática e um PNG transparente sairia preto.
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
            val gray = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply { colorFilter = ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) }) }
            c.drawBitmap(logo, null, logoRect, gray)
        }
        var y = top - fm.ascent + 4
        for (l in lines) { c.drawText(l.take(columns), 4f, y, paint); y += lineH }
        return bmp
    }
}
