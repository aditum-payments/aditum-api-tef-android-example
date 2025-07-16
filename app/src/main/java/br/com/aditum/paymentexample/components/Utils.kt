package br.com.aditum.paymentexample.components

import android.util.Log
import android.content.Context
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.os.Handler
import android.os.Looper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.textfield.TextInputLayout
import android.widget.TextView
import android.widget.Button
import java.util.concurrent.CountDownLatch

import br.com.aditum.paymentexample.R
import br.com.aditum.data.v2.model.callbacks.GetClearDataRequest
import br.com.aditum.data.v2.model.callbacks.GetMenuSelectionRequest
import br.com.aditum.data.v2.model.callbacks.GetClearDataFinishedCallback
import br.com.aditum.data.v2.model.callbacks.GetMenuSelectionFinishedCallback
import java.text.NumberFormat
import java.util.*
import br.com.aditum.data.v2.model.Charge

object Utils {
    public val TAG = Utils::class.simpleName

    private val DefaultCurrencyCode: String  = "BRL"

    // Simple hashmap to ISO4218 String code to Int code.
    // Android API below Level 24 doesn't have support to "getNumericCode"
    private val mISO4217Map: Map<String, Int>  = hashMapOf(
        "BRL" to 986,
        "EUR" to 978,
        "USS" to 998,
        "USN" to 997,
        "CAD" to 124,
        "USD" to 840,
    )

    interface BottomSheetCallback {
        fun onBottomSheetCreated(bottomSheetDialog: BottomSheetDialog)
    }

    fun showMenuListBottomSheet(context: Context, menuSelectionRequest: GetMenuSelectionRequest?, finished: GetMenuSelectionFinishedCallback?) {
        runOnUiThread {
            menuSelectionRequest?.optionList?.let { optionList ->
                Log.d(TAG, "showMenuListBottomSheet - context: $context, items: $optionList")

                if (optionList.isNotEmpty()) {
                    val bottomSheetDialog = BottomSheetDialog(context)
                    val view = LayoutInflater.from(context).inflate(R.layout.menu_selection_bottom_sheet, null)
                    bottomSheetDialog.setContentView(view)
                    bottomSheetDialog.setCancelable(true)

                    val recyclerView: RecyclerView = view.findViewById(R.id.recyclerView)
                    recyclerView.layoutManager = LinearLayoutManager(context)
                    recyclerView.adapter = ItemAdapter(optionList) { index ->
                        Log.d(TAG, "showMenuListBottomSheet - index: $index")
                        finished?.getMenuSelectionFinished(index)
                        bottomSheetDialog.dismiss()
                    }

                    val title: TextView = view.findViewById(R.id.title)
                    title.text = menuSelectionRequest.title

                    val cancelButton: Button = view.findViewById(R.id.cancelButton)
                    cancelButton.setOnClickListener {
                        Log.d(TAG, "showMenuListBottomSheet - cancelButton")
                        finished?.getMenuSelectionAborted()
                        bottomSheetDialog.dismiss()
                    }

                    bottomSheetDialog.show()
                } else {
                    finished?.getMenuSelectionAborted()
                }
            } ?: run {
                finished?.getMenuSelectionAborted()
            }
        }
    }

    fun showGetClearDataBottomSheet(context: Context, clearDataRequest: GetClearDataRequest?, finished: GetClearDataFinishedCallback?) {
        runOnUiThread {
            clearDataRequest?.let { clearDataRequest ->
                Log.d(TAG, "showGetClearDataBottomSheet - context: $context, clearDataRequest: $clearDataRequest")

                val bottomSheetDialog = BottomSheetDialog(context)
                val view = LayoutInflater.from(context).inflate(R.layout.get_clear_data_bottom_sheet, null)
                bottomSheetDialog.setContentView(view)
                bottomSheetDialog.setCancelable(false)
                bottomSheetDialog.setCanceledOnTouchOutside(false)

                val title: TextView = view.findViewById(R.id.title)
                val editText: TextInputLayout = view.findViewById(R.id.editText)
                val submitButton: Button = view.findViewById(R.id.submitButton)
                val cancelButton: Button = view.findViewById(R.id.cancelButton)

                clearDataRequest.title?.let { title.text = it }
                clearDataRequest.description?.let { editText.hint = it }
                clearDataRequest.alphaNumeric.let { alphaNumeric ->
                    if (alphaNumeric == true) {
                        editText.editText?.inputType = android.text.InputType.TYPE_CLASS_TEXT
                    }
                }

                submitButton.setOnClickListener {
                    val data = editText.editText?.text.toString()
                    finished?.getClearDataFinished(data)
                    bottomSheetDialog.dismiss()
                }

                cancelButton.setOnClickListener {
                    finished?.getClearDataAborted()
                    bottomSheetDialog.dismiss()
                }

                bottomSheetDialog.show()
            } ?: run {
                finished?.getClearDataAborted()
            }
        }
    }

    fun showPinNotificationBottomSheet(context: Context, message: String, length: Int, createdCallback: BottomSheetCallback) {
        runOnUiThread {
            Log.d(TAG, "showPinNotificationBottomSheet - context: $context, message: $message, length: $length")

            val bottomSheetDialog = BottomSheetDialog(context)
            val view = LayoutInflater.from(context).inflate(R.layout.pin_bottom_sheet, null)
            bottomSheetDialog.setContentView(view)
            bottomSheetDialog.setCancelable(false)
            bottomSheetDialog.setCanceledOnTouchOutside(false)

            val title: TextView = view.findViewById(R.id.title)
            val pinDataHint: TextView = view.findViewById(R.id.pinDataHint)

            title.text = message
            pinDataHint.text = "* ".repeat(length).trim()

            bottomSheetDialog.show()
            createdCallback.onBottomSheetCreated(bottomSheetDialog)
        }
    }

    fun runOnUiThread(runnable: Runnable) {
        Handler(Looper.getMainLooper()).post(runnable)
    }

    fun formatCurrency(amount: Double?): String {
        val isoCurrency = 986 // BRL

        val currency: Currency? = if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
            Currency.getAvailableCurrencies().firstOrNull {
                it.numericCode == isoCurrency
            }
        } else {
            Currency.getAvailableCurrencies().firstOrNull {
                mISO4217Map[it.currencyCode] == isoCurrency
            } ?: Currency.getInstance(DefaultCurrencyCode) // default to BRL
        }

        val currencyString = currency?.currencyCode

        val currencyFormat = NumberFormat.getCurrencyInstance(Locale.getDefault())
        currencyFormat.currency = Currency.getInstance(currencyString)

        val formattedAmount = amount?.let {
            currencyFormat.format(amount / 100)
        } ?: currencyFormat.format(0)

        return formattedAmount
    }
}

