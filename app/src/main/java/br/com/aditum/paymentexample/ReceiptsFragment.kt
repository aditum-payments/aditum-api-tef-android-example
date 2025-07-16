package br.com.aditum.paymentexample

import android.util.Log
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.core.content.res.ResourcesCompat

import kotlin.concurrent.thread

import br.com.aditum.device.IPrinterSdk
import br.com.aditum.device.callbacks.IPrintStatusCallback
import br.com.aditum.data.v2.enums.PrintStatus
import br.com.aditum.paymentexample.components.Utils

class ReceiptsFragment : Fragment() {
    private lateinit var context: Context
    private lateinit var receiptContent: TextView
    private lateinit var btnPrint: Button
    private lateinit var btnToggleReceipt: Button
    private lateinit var btnClose: Button

    private lateinit var receiptsMerchant: List<String>
    private lateinit var receiptsCustomer: List<String>
    private var showingMerchant = true

    private lateinit var printerSdk: IPrinterSdk
    private lateinit var cancelCallback: ICancelCallback

    interface ICancelCallback {
        fun onCancel()
    }

    companion object {
        val TAG = ReceiptsFragment::class.simpleName
        private const val ARG_RECEIPTS_MERCHANT = "receipts_merchant"
        private const val ARG_RECEIPTS_CUSTOMER = "receipts_customer"
        private const val ARG_PRINTER_SDK = "printer_sdk"

        fun newInstance(context: Context,
        receiptsMerchant: List<String>,
        receiptsCustomer: List<String>,
        printerSdk: IPrinterSdk,
        cancelCallback: ICancelCallback): ReceiptsFragment {
            val fragment = ReceiptsFragment()
            val args = Bundle()
            args.putStringArrayList(ARG_RECEIPTS_MERCHANT, ArrayList(receiptsMerchant))
            args.putStringArrayList(ARG_RECEIPTS_CUSTOMER, ArrayList(receiptsCustomer))
            fragment.context = context
            fragment.arguments = args
            fragment.printerSdk = printerSdk
            fragment.cancelCallback = cancelCallback
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_receipts, container, false)
        val typeFace: Typeface? = ResourcesCompat.getFont(context, R.font.ubuntu_mono_b)
        receiptContent = view.findViewById(R.id.receiptContent)
        receiptContent.setTypeface(typeFace)

        btnPrint = view.findViewById(R.id.btnPrint)
        btnToggleReceipt = view.findViewById(R.id.btnToggleReceipt)
        btnClose = view.findViewById(R.id.btnClose)

        receiptsMerchant = arguments?.getStringArrayList(ARG_RECEIPTS_MERCHANT) ?: emptyList()
        receiptsCustomer = arguments?.getStringArrayList(ARG_RECEIPTS_CUSTOMER) ?: emptyList()

        displayReceipt()

        btnPrint.setOnClickListener {
            Utils.runOnUiThread {
                btnPrint.isEnabled = false
                btnToggleReceipt.isEnabled = false
                btnClose.isEnabled = false
            }

            printReceipt()
        }

        btnToggleReceipt.setOnClickListener {
            showingMerchant = !showingMerchant
            btnToggleReceipt.text = if (showingMerchant) "Show Customer" else "Show Merchant"
            displayReceipt()
        }

        btnClose.setOnClickListener {
            Log.d(TAG, "Printing canceled")
            cancelCallback.onCancel()
        }

        return view
    }

    private fun displayReceipt() {
        val receipt = if (showingMerchant) receiptsMerchant.joinToString("\n") else receiptsCustomer.joinToString("\n")
        receiptContent.text = receipt
    }

    private fun printReceipt() {
        val bitmap = getBitmapFromView(receiptContent)

        thread {
            printerSdk.print(bitmap, object : IPrintStatusCallback.Stub() {
                override fun finished(status: PrintStatus) {
                    Utils.runOnUiThread {
                        btnPrint.isEnabled = true
                        btnToggleReceipt.isEnabled = true
                        btnClose.isEnabled = true
                    }

                    Log.d(TAG, "finished: $status")
                    if (status == PrintStatus.Ok) {
                        NotificationMessage.showMessageBox(context, "Success", "Receipt printed successfully!")
                    } else {
                        NotificationMessage.showMessageBox(context, "Error", "Error printing receipt: $status")
                    }
                }
            })
        }
    }

    private fun getBitmapFromView(view: View): Bitmap {
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        view.draw(canvas)
        return bitmap
    }
}
