package br.com.aditum.paymentexample

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Build
import android.util.Log
import android.view.View
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.AutoCompleteTextView

import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.navigation.ui.AppBarConfiguration
import androidx.activity.OnBackPressedCallback

import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.elevation.SurfaceColors
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.textfield.TextInputEditText

import br.com.aditum.data.v2.model.callbacks.GetClearDataRequest
import br.com.aditum.data.v2.model.callbacks.GetClearDataFinishedCallback
import br.com.aditum.data.v2.model.callbacks.GetMenuSelectionRequest
import br.com.aditum.data.v2.model.callbacks.GetMenuSelectionFinishedCallback

import br.com.aditum.data.v2.enums.AbecsCommands
import br.com.aditum.data.v2.enums.TransactionStatus
import br.com.aditum.data.v2.model.cancelation.CancelationRequest
import br.com.aditum.data.v2.model.cancelation.CancelationResponse
import br.com.aditum.data.v2.model.cancelation.CancelationResponseCallback

import br.com.aditum.IAditumSdkService
import br.com.aditum.device.IDeviceSdk
import br.com.aditum.device.IPrinterSdk

import br.com.aditum.paymentexample.databinding.ActivityCancelationBinding
import br.com.aditum.paymentexample.components.Utils
import kotlin.concurrent.thread

class CancelationActivity : AppCompatActivity() {
    public val TAG = CancelationActivity::class.simpleName

    private lateinit var mAppBarConfiguration: AppBarConfiguration
    private lateinit var mBinding: ActivityCancelationBinding
    private lateinit var mPaymentApplication: PaymentApplication

    private lateinit var mCancelButton: Button
    private lateinit var mNsuTransactionEditText: TextInputEditText
    private lateinit var mPartialCancelSwitch: MaterialSwitch
    private lateinit var mPartialCancelAmountText: TextInputLayout
    private lateinit var mReversionSwitch: MaterialSwitch
    private lateinit var mProgressBar: CircularProgressIndicator
    private lateinit var mAbortOperationButton: Button

    private var mCancelationRequest: CancelationRequest? = null
    private var mIsReversal: Boolean = false
    private var mPrinterSdk: IPrinterSdk? = null

    private val mCancelationResponseCallback = object : CancelationResponseCallback.Stub() {
        override fun onResponse(cancelationResponse: CancelationResponse?) {
            Log.d(TAG, "onResponse - cancelationResponse: $cancelationResponse")

            cancelationResponse?.let {
                if (cancelationResponse.canceled) {
                    if (mPaymentApplication.useOnlySdk) {
                        mPrinterSdk?.let { printerSdk: IPrinterSdk ->
                            val bottomSheetDialog = ReceiptsBottomSheetDialogFragment()
                            val merchantReceipt = cancelationResponse.charge?.merchantReceipt ?: emptyList()
                            val cardholderReceipt = cancelationResponse.charge?.cardholderReceipt ?: emptyList()
                            val cancelCallback = object: ReceiptsFragment.ICancelCallback {
                                override fun onCancel() {
                                    bottomSheetDialog.dismiss()
                                }
                            }
                            val fragment = ReceiptsFragment.newInstance(this@CancelationActivity, merchantReceipt, cardholderReceipt, printerSdk, cancelCallback)
                            runOnUiThread {
                                bottomSheetDialog.setFragment(fragment)
                                bottomSheetDialog.show(supportFragmentManager, ReceiptsBottomSheetDialogFragment.TAG)
                            }
                        } ?: run {
                            mPaymentApplication.communicationService?.let { communicationService: IAditumSdkService ->
                                val bottomSheetDialog = ReceiptsBottomSheetDialogFragment()
                                val cancelCallback = object: SendReceiptFragment.ICancelCallback {
                                    override fun onCancel() {
                                        bottomSheetDialog.dismiss()
                                    }
                                }
                                val transactionId = cancelationResponse.charge?.transactionId ?: ""
                                val fragment = SendReceiptFragment.newInstance(this@CancelationActivity, communicationService, transactionId, cancelCallback)

                                runOnUiThread {
                                    bottomSheetDialog.setFragment(fragment)
                                    bottomSheetDialog.show(supportFragmentManager, ReceiptsBottomSheetDialogFragment.TAG)
                                }
                            }
                        }
                    } else {
                        NotificationMessage.showMessageBox(this@CancelationActivity, "Success", "onResponse - cancelationResponse: $cancelationResponse")
                    }
                } else {
                    NotificationMessage.showMessageBox(this@CancelationActivity, "Error", "onResponse - cancelationResponse: $cancelationResponse")
                }
            } ?: run {
                NotificationMessage.showMessageBox(this@CancelationActivity, "Error", "onResponse - cancelationResponse is null")
            }

            runOnUiThread {
                hideProgressBar()
                mNsuTransactionEditText.isEnabled = true
                mCancelButton.isEnabled = true
                mAbortOperationButton.visibility = Button.GONE
            }
        }
    }

    private val mPaymentCallback = object : PaymentCallback() {
        override fun notification(message: String?, transactionStatus: TransactionStatus?, command: AbecsCommands?) {
            Log.d(TAG, "CancelationActivity::notification - message: $message, transactionStatus: $transactionStatus, command: $command")

            // kip notification for non-SDK mode
            if (!mPaymentApplication.useOnlySdk)
            return

            message?.let { msg ->
                NotificationMessage.showToast(this@CancelationActivity, msg)
            }
        }

        override fun startGetClearData(clearDataRequest: GetClearDataRequest?, finished: GetClearDataFinishedCallback?) {
            Log.d(TAG, "startGetClearData - clearDataRequest: $clearDataRequest")
            Utils.showGetClearDataBottomSheet(this@CancelationActivity, clearDataRequest, finished)
        }

        override fun startGetMenuSelection(menuSelectionRequest: GetMenuSelectionRequest?, finished: GetMenuSelectionFinishedCallback?) {
            Log.d(TAG, "startGetMenuSelection - menuSelectionRequest: $menuSelectionRequest")
            Utils.showMenuListBottomSheet(this@CancelationActivity, menuSelectionRequest, finished)
        }
    }

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

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate")

        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)

        mPaymentApplication = application as PaymentApplication
        val nsu: String? = intent.getStringExtra("nsu")

        val color = SurfaceColors.SURFACE_3.getColor(this)
        window.statusBarColor = color
        window.navigationBarColor = color

        mBinding = ActivityCancelationBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        mBinding.toolbar.setBackgroundColor(color)

        setSupportActionBar(mBinding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        mNsuTransactionEditText = mBinding.nsuTransactionEditText
        mCancelButton = mBinding.cancelButton
        mPartialCancelSwitch = mBinding.partialCancelSwitch
        mPartialCancelAmountText = mBinding.partialAmountTextField
        mReversionSwitch = mBinding.reversionSwitch
        mProgressBar = mBinding.progressBar
        mAbortOperationButton = mBinding.abortOperationButton
        mNsuTransactionEditText.setText(nsu)
        configureReversion()
        mCancelButton.setOnClickListener { onCancelButtonClick() }
        hideProgressBar()

        val backCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                Log.d(TAG, "handleOnBackPressed")
                finish()
            }
        }

        this.onBackPressedDispatcher.addCallback(this, backCallback)
        mBinding.toolbar.setNavigationOnClickListener { backCallback.handleOnBackPressed() }

        configureAbortOperationButton()

        mPaymentApplication.merchantData?.let {
            if (isPartialCancelEnabled()) {
                configurePartialCancel()
            }
        } ?: run {
            NotificationMessage.showMessageBox(this, "Error", "Merchant data not available.")
        }
    }

    override fun onResume() {
        Log.d(TAG, "onResume")
        super.onResume()

        mPaymentApplication.communicationService?.let { communicationService: IAditumSdkService ->
            communicationService.registerPaymentCallback(mPaymentCallback)
            if (mPaymentApplication.useOnlySdk) {
                val deviceSdk: IDeviceSdk? = communicationService.deviceSdk
                mPrinterSdk = deviceSdk?.printerSdk
                Log.d(TAG, "PrinterSdk: $mPrinterSdk")
            }
        } ?: run {
            NotificationMessage.showMessageBox(this, "Error", "Merchant data not available.")
        }
    }

    private fun isPartialCancelEnabled(): Boolean {
        return mPaymentApplication.merchantData?.allowPartialCancel ?: false
    }

    private fun configurePartialCancel() {
        mPartialCancelSwitch.visibility = MaterialSwitch.VISIBLE
        mPartialCancelSwitch.isChecked = false
        mPartialCancelSwitch.setOnCheckedChangeListener { _, isChecked ->
            mPartialCancelAmountText.visibility = if (isChecked) TextInputLayout.VISIBLE else TextInputLayout.GONE
        }
        configureAmount()
    }

    private fun configureReversion() {
        mReversionSwitch.isChecked = false
        mReversionSwitch.setOnCheckedChangeListener { _, isChecked ->
            mIsReversal = isChecked
        }
    }

    private fun showProgressBar() {
        mProgressBar.visibility = CircularProgressIndicator.VISIBLE
    }

    private fun hideProgressBar() {
        mProgressBar.visibility = CircularProgressIndicator.INVISIBLE
    }

    private fun onCancelButtonClick() {
        val nsu: String = mNsuTransactionEditText.text.toString()
        val isPartialCancelEnabled: Boolean = mPartialCancelSwitch.isChecked && isPartialCancelEnabled()
        val partialAmount: Long = if (isPartialCancelEnabled) {
            try {
                mPartialCancelAmountText.editText?.text.toString().replace(Regex("[^0-9]"), "").toLong()
            } catch (e: NumberFormatException) {
                0
            }
        } else {
            0
        }

        mNsuTransactionEditText.isEnabled = false
        mCancelButton.isEnabled = false
        mAbortOperationButton.visibility = Button.VISIBLE

        mCancelationRequest = CancelationRequest(nsu, mIsReversal, isPartialCancelEnabled, partialAmount)
        mPaymentApplication.communicationService?.let { communicationService: IAditumSdkService ->
            showProgressBar()
            Log.d(TAG, "CancelationRequest: $mCancelationRequest");
            thread { communicationService.cancel(mCancelationRequest, mCancelationResponseCallback) }
        } ?: run {
            NotificationMessage.showMessageBox(this, "Error", "Communication service not available.")
        }
    }

    private fun configureAbortOperationButton() {
        mAbortOperationButton.visibility = Button.GONE
        mAbortOperationButton.setOnClickListener {
            mPaymentApplication.communicationService?.let { communicationService: IAditumSdkService ->
                thread { communicationService.abortOperation() }
            }
        }
    }

    private fun configureAmount() {
        val amountInput = mPartialCancelAmountText.editText
        amountInput?.setText("")

        amountInput?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(editable: Editable?) {
                formatCurrency(editable, this)
            }
        })
    }

    private fun formatCurrency(editable: Editable?, thiz: TextWatcher) {
        val value = editable ?: return

        try {
            val amountInput = mPartialCancelAmountText.editText ?: return
            val amountString = value.toString().replace(Regex("[^0-9]"), "")
            val amount = amountString.toDouble()
            val isoCurrency = 986 // BRL

            var currency:Currency?

            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
                currency = Currency.getAvailableCurrencies().firstOrNull {
                    it.numericCode == isoCurrency
                }
            } else {
                currency =  Currency.getAvailableCurrencies().firstOrNull {
                    mISO4217Map[it.currencyCode] == isoCurrency
                } ?: Currency.getInstance(DefaultCurrencyCode) // default to BRL
            }

            val currencyString = currency?.currencyCode

            val currencyFormat = NumberFormat.getCurrencyInstance(Locale.getDefault())
            currencyFormat.currency = Currency.getInstance(currencyString)

            val formattedAmount = currencyFormat.format(amount / 100)

            amountInput.removeTextChangedListener(thiz)
            amountInput.setText(formattedAmount)
            amountInput.setSelection(formattedAmount.length)
            amountInput.addTextChangedListener(thiz)
        } catch (e: NumberFormatException) {
            e.printStackTrace()
        }
    }

}

