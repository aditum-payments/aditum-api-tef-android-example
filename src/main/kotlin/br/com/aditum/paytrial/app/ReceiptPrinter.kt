package br.com.aditum.paytrial.app

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface

/**
 * Comprovante (linhas do `Charge`) → bitmap de 384 px para o `IPrinterSdk` do terminal, em fonte monoespaçada com as
 * 32 colunas do papel. É como um integrador imprime: o serviço entrega as linhas, o app monta a imagem.
 */
object ReceiptPrinter {
    fun render(lines: List<String>, widthPx: Int = 384, columns: Int = 32): Bitmap {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = Typeface.MONOSPACE; color = Color.BLACK }
        var size = 28f; paint.textSize = size
        while (paint.measureText("W".repeat(columns)) > widthPx - 8 && size > 8f) { size -= 0.5f; paint.textSize = size }
        val fm = paint.fontMetrics; val lineH = (fm.descent - fm.ascent + 2).toInt()
        val bmp = Bitmap.createBitmap(widthPx, (lines.size + 3) * lineH, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp); c.drawColor(Color.WHITE)
        var y = -fm.ascent + 4
        for (l in lines) { c.drawText(l.take(columns), 4f, y, paint); y += lineH }
        return bmp
    }
}
