package br.com.aditum.paymentexample

import android.util.Log
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Button
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix

class QRCodeBottomSheetDialogFragment : BottomSheetDialogFragment() {
    public val TAG = QRCodeBottomSheetDialogFragment::class.simpleName

    private var qrCodeContent: String? = null
    private var progressTimeInSeconds: Int = -1
    private lateinit var progressBar: ProgressBar
    private val handler = Handler(Looper.getMainLooper())
    private var onCancelCallback: (() -> Unit)? = null

    fun setQRCodeContent(content: String) {
        qrCodeContent = content
    }

    fun setProgressTimeInSeconds(timeInSeconds: Int) {
        progressTimeInSeconds = timeInSeconds
    }

    fun setOnCancelCallback(callback: () -> Unit) {
        onCancelCallback = callback
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        Log.d(TAG, "onCreateView")

        setCancelable(false)

        val view = inflater.inflate(R.layout.qrcode_bottom_sheet, container, false)
        val qrCodeImageView = view.findViewById<ImageView>(R.id.qrCodeView)
        val cancelButton: Button = view.findViewById(R.id.cancelButton)
        progressBar = view.findViewById(R.id.expirationTime)

        qrCodeContent?.let {
            val bitmap = generateQRCode(it)
            qrCodeImageView.setImageBitmap(bitmap)
        }

        startProgressBar()

        cancelButton.setOnClickListener {
            onCancelCallback?.invoke()
            dismiss()
        }

        return view
    }

    private fun generateQRCode(content: String): Bitmap? {
        return try {
            val bitMatrix: BitMatrix = MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, 200, 200)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bmp.setPixel(x, y, if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE)
                }
            }
            bmp
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun startProgressBar() {
        progressBar.max = progressTimeInSeconds
        progressBar.progress = 0
        var progress = 0

        val runnable = object : Runnable {
            override fun run() {
                if (progress < progressTimeInSeconds) {
                    Log.d(TAG, "progress: $progress, progressTimeInSeconds: $progressTimeInSeconds")
                    progress += 1
                    progressBar.progress = progress
                    handler.postDelayed(this, 1000)
                } else {
                    onCancelCallback?.invoke()
                    dismiss()
                }
            }
        }

        handler.post(runnable)
    }

    override fun onDestroyView() {
        Log.d(TAG, "onDestroyView")
        super.onDestroyView()
        handler.removeCallbacksAndMessages(null)
    }
}

