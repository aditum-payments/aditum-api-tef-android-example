package br.com.aditum.paytrial.app

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbManager
import android.os.Build
import br.com.aditum.tef.escpos.EscPos
import br.com.aditum.tef.escpos.MonoBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

/** Uma impressora USB vista pelo aparelho: [key] identifica a escolhida (VID:PID) entre aberturas do app. */
data class UsbPrinterInfo(val key: String, val name: String, val device: UsbDevice)

/**
 * Impressora térmica **externa** pela USB do aparelho (host/OTG), genérica: qualquer impressora de classe USB padrão
 * (classe 7, como a Elgin/Bematech i9 `20d1:7008`) recebe o comprovante como imagem ESC/POS (`GS v 0`) pelo endpoint
 * bulk de saída — sem SDK de fabricante. É assim que um integrador imprime fora da impressora do terminal.
 *
 * Fora daqui: impressora USB-serial (CH340/PL2303) e rede/Bluetooth, que pedem outro transporte com o mesmo ESC/POS.
 */
class UsbPrinter(private val context: Context) {
    private val usb = context.getSystemService(Context.USB_SERVICE) as UsbManager

    /** Dispositivos USB conectados com interface de impressora. */
    fun list(): List<UsbPrinterInfo> = usb.deviceList.values.filter { printerInterface(it) != null }.map { d ->
        val key = "%04x:%04x".format(d.vendorId, d.productId)
        val name = listOfNotNull(runCatching { d.manufacturerName }.getOrNull(), runCatching { d.productName }.getOrNull()).joinToString(" ").ifBlank { "Impressora USB $key" }
        UsbPrinterInfo(key, name, d)
    }

    fun find(key: String): UsbPrinterInfo? = list().firstOrNull { it.key == key }

    /** Pede a permissão do Android para usar o dispositivo (o diálogo do sistema); true se concedida. */
    suspend fun ensurePermission(device: UsbDevice): Boolean {
        if (usb.hasPermission(device)) return true
        return suspendCancellableCoroutine { cont ->
            val action = "${context.packageName}.USB_PRINTER_PERMISSION"
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(c: Context, i: Intent) {
                    runCatching { context.unregisterReceiver(this) }
                    if (cont.isActive) cont.resume(i.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false))
                }
            }
            val filter = IntentFilter(action)
            if (Build.VERSION.SDK_INT >= 33) context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED) else context.registerReceiver(receiver, filter)
            // o UsbManager escreve a resposta nos extras do PendingIntent: precisa ser mutável no Android 12+
            val flags = if (Build.VERSION.SDK_INT >= 31) PendingIntent.FLAG_MUTABLE else 0
            usb.requestPermission(device, PendingIntent.getBroadcast(context, 0, Intent(action).setPackage(context.packageName), flags))
            cont.invokeOnCancellation { runCatching { context.unregisterReceiver(receiver) } }
        }
    }

    /** Envia as vias (bitmaps do [ReceiptPrinter]) como imagem ESC/POS, com corte entre elas. */
    suspend fun print(key: String, vias: List<Bitmap>): Result<Unit> = withContext(Dispatchers.IO) {
        val p = find(key) ?: return@withContext Result.failure(IllegalStateException("Impressora USB não encontrada: ligue o cabo (e a fonte da impressora)"))
        if (!ensurePermission(p.device)) return@withContext Result.failure(SecurityException("Sem permissão para usar a impressora USB"))
        val bytes = EscPos.receipt(vias.map { it.toMono() })
        send(p.device, bytes)
    }

    private fun send(device: UsbDevice, bytes: ByteArray): Result<Unit> {
        val (intf, out) = printerInterface(device) ?: return Result.failure(IllegalStateException("O dispositivo não tem saída de impressora"))
        val conn = usb.openDevice(device) ?: return Result.failure(IllegalStateException("Não abriu a impressora USB"))
        try {
            if (!conn.claimInterface(intf, true)) return Result.failure(IllegalStateException("Impressora USB ocupada"))
            var off = 0
            while (off < bytes.size) {
                val n = minOf(CHUNK, bytes.size - off)
                val sent = conn.bulkTransfer(out, bytes, off, n, TIMEOUT_MS)
                if (sent <= 0) return Result.failure(IllegalStateException("A impressora não recebeu os dados (papel? tampa?)"))
                off += sent
            }
            return Result.success(Unit)
        } finally {
            runCatching { conn.releaseInterface(intf) }; conn.close()
        }
    }

    /** Interface de classe impressora (7) com endpoint bulk de saída. */
    private fun printerInterface(d: UsbDevice): Pair<UsbInterface, UsbEndpoint>? {
        for (i in 0 until d.interfaceCount) {
            val intf = d.getInterface(i)
            if (intf.interfaceClass != UsbConstants.USB_CLASS_PRINTER) continue
            for (e in 0 until intf.endpointCount) {
                val ep = intf.getEndpoint(e)
                if (ep.type == UsbConstants.USB_ENDPOINT_XFER_BULK && ep.direction == UsbConstants.USB_DIR_OUT) return intf to ep
            }
        }
        return null
    }

    private fun Bitmap.toMono(): MonoBitmap {
        val px = IntArray(width * height); getPixels(px, 0, width, 0, 0, width, height)
        return MonoBitmap.fromArgb(px, width, height)
    }

    private companion object { const val CHUNK = 16 * 1024; const val TIMEOUT_MS = 5_000 }
}
