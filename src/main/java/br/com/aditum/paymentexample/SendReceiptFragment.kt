package br.com.aditum.paymentexample

import android.util.Log
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.text.Editable
import android.text.TextWatcher
import android.widget.ArrayAdapter

import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import androidx.fragment.app.Fragment
import androidx.core.content.res.ResourcesCompat

import br.com.aditum.IAditumSdkService
import br.com.aditum.data.v2.enums.SendReceiptType
import br.com.aditum.data.v2.model.transactions.SendReceiptRequest
import br.com.aditum.paymentexample.components.Utils

import kotlin.concurrent.thread

class SendReceiptFragment : Fragment() {
    private lateinit var context: Context
    private lateinit var cancelCallback: ICancelCallback
    private lateinit var transactionId: String

    private lateinit var emailOrPhoneEditText: TextInputEditText
    private lateinit var ddSendReceiptType: TextInputLayout
    private lateinit var formatDropdown: MaterialAutoCompleteTextView
    private lateinit var btnSendReceipt: Button
    private lateinit var btnClose: Button

    private lateinit var aditumSdkService: IAditumSdkService

    interface ICancelCallback {
        fun onCancel()
    }

    companion object {
        val TAG = SendReceiptFragment::class.simpleName

        fun newInstance(context: Context,
        aditumSdkService: IAditumSdkService,
        transactionId: String,
        cancelCallback: ICancelCallback): SendReceiptFragment {
            val fragment = SendReceiptFragment()
            fragment.context = context
            fragment.aditumSdkService = aditumSdkService
            fragment.transactionId = transactionId
            fragment.cancelCallback = cancelCallback
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_send_receipt, container, false)

        ddSendReceiptType = view.findViewById(R.id.sendReceiptTypeDropdownLayout)
        formatDropdown = view.findViewById(R.id.formatDropdown)
        emailOrPhoneEditText = view.findViewById(R.id.emailOrPhoneEditText)
        btnSendReceipt = view.findViewById(R.id.btnSendReceipt)
        btnClose = view.findViewById(R.id.btnClose)

        emailOrPhoneEditText.isEnabled = false
        emailOrPhoneEditText.visibility = View.GONE
        btnSendReceipt.isEnabled = false

        val options = SendReceiptType.values().map { it.name }
        val adapter = ArrayAdapter(context, android.R.layout.simple_list_item_1, options)
        formatDropdown.setAdapter(adapter)

        formatDropdown.setOnItemClickListener { _, _, position, _ ->
            val selected = SendReceiptType.values()[position]
            emailOrPhoneEditText.isEnabled = selected == SendReceiptType.Email || selected == SendReceiptType.SMS
            emailOrPhoneEditText.visibility = if (emailOrPhoneEditText.isEnabled) View.VISIBLE else View.GONE
            emailOrPhoneEditText.text?.clear()
            btnSendReceipt.isEnabled = false

            when (selected) {
                SendReceiptType.Email -> {
                    emailOrPhoneEditText.hint = "Email"
                    emailOrPhoneEditText.removeTextChangedListener(phoneTextWatcher)
                    emailOrPhoneEditText.addTextChangedListener(emailTextWatcher)
                }
                SendReceiptType.SMS -> {
                    emailOrPhoneEditText.hint = "Phone"
                    emailOrPhoneEditText.removeTextChangedListener(emailTextWatcher)
                    emailOrPhoneEditText.addTextChangedListener(phoneTextWatcher)
                }
                else -> {
                    emailOrPhoneEditText.hint = ""
                    emailOrPhoneEditText.removeTextChangedListener(emailTextWatcher)
                    emailOrPhoneEditText.removeTextChangedListener(phoneTextWatcher)
                }
            }
        }

        btnSendReceipt.setOnClickListener {
            val sendType = if (formatDropdown.text.toString() == SendReceiptType.SMS.name) SendReceiptType.SMS else SendReceiptType.Email
            var emailOrPhone = emailOrPhoneEditText.text.toString()
            if (sendType == SendReceiptType.SMS) {
                emailOrPhone = emailOrPhone.unmask()
            }

            val request = SendReceiptRequest(
                sendType,
                emailOrPhone,
                transactionId
            )
            Log.d(TAG, "Send receipt by email $request")

            Utils.runOnUiThread {
                btnSendReceipt.isEnabled = false
                btnClose.isEnabled = false
                emailOrPhoneEditText.isEnabled = false
                ddSendReceiptType.isEnabled = false
            }

            thread {
                val success = aditumSdkService.sendReceipt(request)
                Log.d(TAG, "Send receipt result $success")
                if (success) {
                    NotificationMessage.showMessageBox(context, "Success", "Receipt sent successfully")
                } else {
                    NotificationMessage.showMessageBox(context, "Error", "Error sending receipt")
                }

                Utils.runOnUiThread {
                    btnSendReceipt.isEnabled = true
                    btnClose.isEnabled = true
                    emailOrPhoneEditText.isEnabled = true
                    ddSendReceiptType.isEnabled = true
                }
            }
        }

        btnClose.setOnClickListener {
            cancelCallback.onCancel()
        }

        return view
    }

    private val phoneTextWatcher = object : TextWatcher {
        var isUpdating = false

        override fun beforeTextChanged(charSequence: CharSequence?, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(charSequence: CharSequence?, start: Int, before: Int, count: Int) {}

        override fun afterTextChanged(editable: Editable?) {
            if (isUpdating) {
                isUpdating = false
                return
            }

            val str = editable.toString().unmask()

            isUpdating = true
            val formatted = str.formatPhone()
            editable?.replace(0, editable.length, formatted)
            btnSendReceipt.isEnabled = emailOrPhoneEditText.text.toString().isNotEmpty()
        }
    }

    private val emailTextWatcher = object : TextWatcher {
        override fun beforeTextChanged(charSequence: CharSequence?, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(charSequence: CharSequence?, start: Int, before: Int, count: Int) {}

        override fun afterTextChanged(editable: Editable?) {
            val email = editable.toString()
            if (!email.isValidEmail()) {
                emailOrPhoneEditText.error = "Invalid email"
                btnSendReceipt.isEnabled = false
            } else {
                emailOrPhoneEditText.error = null
                btnSendReceipt.isEnabled = true
            }
        }
    }

    private fun String.unmask(): String {
        return this.replace("[^\\d]".toRegex(), "")
    }

    private fun String.formatPhone(): String {
        val unmasked = this.unmask()
        return when {
            unmasked.length > 10 -> "(${unmasked.substring(0, 2)}) ${unmasked.substring(2, 7)}-${unmasked.substring(7, 11)}"
            unmasked.length > 5 -> "(${unmasked.substring(0, 2)}) ${unmasked.substring(2, 6)}-${unmasked.substring(6, unmasked.length)}"
            unmasked.length > 2 -> "(${unmasked.substring(0, 2)}) ${unmasked.substring(2, unmasked.length)}"
            unmasked.length > 0 -> "(${unmasked.substring(0, unmasked.length)})"
            else -> ""
        }
    }

    private fun String.isValidEmail(): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(this).matches()
    }
}
