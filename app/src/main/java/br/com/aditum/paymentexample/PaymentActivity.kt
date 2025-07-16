package br.com.aditum.paymentexample

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Build
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AdapterView
import android.widget.Button
import android.widget.Spinner
import android.widget.AutoCompleteTextView

import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.navigation.ui.AppBarConfiguration
import android.widget.TextView

import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.elevation.SurfaceColors
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.textfield.TextInputLayout

import br.com.aditum.data.v2.model.callbacks.GetClearDataRequest
import br.com.aditum.data.v2.model.callbacks.GetClearDataFinishedCallback
import br.com.aditum.data.v2.model.callbacks.GetMenuSelectionRequest
import br.com.aditum.data.v2.model.callbacks.GetMenuSelectionFinishedCallback

import br.com.aditum.data.v2.enums.TransactionStatus
import br.com.aditum.data.v2.enums.AbecsCommands
import br.com.aditum.data.v2.enums.PaymentType
import br.com.aditum.data.v2.enums.InstallmentType
import br.com.aditum.data.v2.enums.PayOperationType
import br.com.aditum.data.v2.enums.Acquirer
import br.com.aditum.data.v2.enums.SendReceiptType
import br.com.aditum.data.v2.model.MerchantData
import br.com.aditum.data.v2.model.Charge
import br.com.aditum.data.v2.model.payment.PaymentRequest
import br.com.aditum.data.v2.model.payment.PaymentScheme
import br.com.aditum.data.v2.model.payment.PaymentResponse
import br.com.aditum.data.v2.model.payment.PaymentResponseCallback
import br.com.aditum.data.v2.model.transactions.SendReceiptRequest

import br.com.aditum.IAditumSdkService
import br.com.aditum.device.IDeviceSdk
import br.com.aditum.device.IPrinterSdk

import br.com.aditum.paymentexample.databinding.ActivityPaymentBinding
import br.com.aditum.paymentexample.components.EnumAdapter
import br.com.aditum.paymentexample.components.PaymentTypeAdapter
import br.com.aditum.paymentexample.components.WalletPaymentTypeAdapter
import br.com.aditum.paymentexample.components.Utils
import kotlin.concurrent.thread
import java.util.UUID

class PaymentActivity : AppCompatActivity() {
    public val TAG = PaymentActivity::class.simpleName

    private lateinit var mAppBarConfiguration: AppBarConfiguration
    private lateinit var mBinding: ActivityPaymentBinding
    private lateinit var mPaymentApplication: PaymentApplication

    private lateinit var mTopAppBar: MaterialToolbar
    private lateinit var mAmountTextField: TextInputLayout
    private lateinit var mOperationTypeComboBox: TextInputLayout
    private lateinit var mOriginNsuTextField: TextInputLayout
    private lateinit var mPaymentSchemeComboBox: TextInputLayout
    private lateinit var mWalletPaymentSchemeComboBox: TextInputLayout
    private lateinit var mInstallmentTypeComboBox: TextInputLayout
    private lateinit var mInstallmentNumberTextField: TextInputLayout
    private lateinit var mManualEntrySwitch: MaterialSwitch
    private lateinit var mPayButton: Button
    private lateinit var mAbortOperationButton: Button
    private lateinit var mProgressBar: CircularProgressIndicator

    private val mPaymentRequest: PaymentRequest = PaymentRequest()
    private var mQrCodeDialog: QRCodeBottomSheetDialogFragment? = null
    private var mPinDialog: BottomSheetDialog? = null
    private var mPrinterSdk: IPrinterSdk? = null
    public var nsu: String? = null

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

    private val mPayResponseCallback = object : PaymentResponseCallback.Stub() {
        override fun onResponse(paymentResponse: PaymentResponse?) {
            Log.d(TAG, "onResponse - paymentResponse: $paymentResponse")

            paymentResponse?.let {
                if (paymentResponse.isApproved) {
                    nsu = paymentResponse.charge?.nsu
                    if (mPaymentApplication.useOnlySdk) {
                        mPrinterSdk?.let { printerSdk: IPrinterSdk ->
                            val bottomSheetDialog = ReceiptsBottomSheetDialogFragment()
                            val merchantReceipt = paymentResponse.charge?.merchantReceipt ?: emptyList()
                            val cardholderReceipt = paymentResponse.charge?.cardholderReceipt ?: emptyList()
                            val cancelCallback = object: ReceiptsFragment.ICancelCallback {
                                override fun onCancel() {
                                    bottomSheetDialog.dismiss()
                                }
                            }
                            val fragment = ReceiptsFragment.newInstance(this@PaymentActivity, merchantReceipt, cardholderReceipt, printerSdk, cancelCallback)
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
                                val transactionId = paymentResponse.charge?.transactionId ?: ""
                                val fragment = SendReceiptFragment.newInstance(this@PaymentActivity, communicationService, transactionId, cancelCallback)

                                runOnUiThread {
                                    bottomSheetDialog.setFragment(fragment)
                                    bottomSheetDialog.show(supportFragmentManager, ReceiptsBottomSheetDialogFragment.TAG)
                                }
                            }
                        }
                    } else {
                        NotificationMessage.showMessageBox(this@PaymentActivity, "Success", "onResponse - paymentResponse: $paymentResponse")
                    }
                } else {
                    NotificationMessage.showMessageBox(this@PaymentActivity, "Error", "onResponse - paymentResponse: $paymentResponse")
                }
            } ?: run {
                NotificationMessage.showMessageBox(this@PaymentActivity, "Error", "onResponse - paymentResponse is null")
            }

            runOnUiThread {
                hideProgressBar()
                mPayButton.isEnabled = true
                mAbortOperationButton.visibility = Button.GONE
            }
        }
    }

    private val mPaymentCallback = object : PaymentCallback() {
        override fun notification(message: String?, transactionStatus: TransactionStatus?, command: AbecsCommands?) {
            Log.d(TAG, "PaymentActivity::notification - message: $message, transactionStatus: $transactionStatus, command: $command")

            // kip notification for non-SDK mode
            if (!mPaymentApplication.useOnlySdk)
            return

            message?.let { msg ->
                NotificationMessage.showToast(this@PaymentActivity, msg)
            }

            // close QrCode dialog
            mQrCodeDialog?.dismiss()
            mQrCodeDialog = null

            // close Pin dialog
            mPinDialog?.dismiss()
            mPinDialog = null
        }

        override fun pinNotification(message: String, length: Int) {
            Log.d(TAG, "PaymentActivity::pinNotification - message: $message, length: $length")

            if (mPinDialog != null) {
                runOnUiThread {
                    val pinDataHint: TextView? = mPinDialog?.findViewById(R.id.pinDataHint)
                    pinDataHint?.text = " * ".repeat(length)
                }
                return
            }

            val bottomSheetCreatedCallback = object: Utils.BottomSheetCallback {
                override fun onBottomSheetCreated(bottomSheetDialog: BottomSheetDialog) {
                    Log.d(TAG, "PaymentActivity::pinNotification - onBottomSheetCreated - bottomSheetDialog: $bottomSheetDialog")
                    mPinDialog = bottomSheetDialog
                }
            }
            Utils.showPinNotificationBottomSheet(this@PaymentActivity, message, length, bottomSheetCreatedCallback)
        }

        override fun startGetClearData(clearDataRequest: GetClearDataRequest?, finished: GetClearDataFinishedCallback?) {
            Log.d(TAG, "startGetClearData - clearDataRequest: $clearDataRequest")
            Utils.showGetClearDataBottomSheet(this@PaymentActivity, clearDataRequest, finished)
        }

        override fun startGetMenuSelection(menuSelectionRequest: GetMenuSelectionRequest?, finished: GetMenuSelectionFinishedCallback?) {
            Log.d(TAG, "startGetMenuSelection - menuSelectionRequest: $menuSelectionRequest")
            Utils.showMenuListBottomSheet(this@PaymentActivity, menuSelectionRequest, finished)
        }

        override fun qrCodeGenerated(qrCode: String, expirationTime: Int) {
            Log.d(TAG, "qrCodeGenerated - qrCode: $qrCode, expirationTime: $expirationTime")

            runOnUiThread {
                mQrCodeDialog = QRCodeBottomSheetDialogFragment()
                mQrCodeDialog?.setQRCodeContent(qrCode)
                mQrCodeDialog?.setProgressTimeInSeconds(expirationTime)
                mQrCodeDialog?.setOnCancelCallback {
                    mPaymentApplication.communicationService?.abortOperation()
                }
                mQrCodeDialog?.show(supportFragmentManager, "QRCodeBottomSheetDialogFragment")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate")

        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)

        mPaymentApplication = application as PaymentApplication

        val color = SurfaceColors.SURFACE_3.getColor(this)
        window.statusBarColor = color
        window.navigationBarColor = color

        mBinding = ActivityPaymentBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        mTopAppBar = mBinding.toolbar
        mTopAppBar.subtitle = "${mPaymentApplication.merchantData?.fantasyName}"
        mTopAppBar.setBackgroundColor(color)

        mTopAppBar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.openUpdateEmvTablesActivity -> {
                    Log.d(TAG, "Open UpdateEmvTablesActivity")
                    openActivity(activity = UpdateEmvTablesActivity::class.java)
                    true
                }
                R.id.openCancelActivity -> {
                    Log.d(TAG, "Open CancelationActivity")
                    val intent = Intent(this, CancelationActivity::class.java).apply {
                        putExtra("nsu", nsu)
                    }
                    startActivity(intent)
                    true
                }
                R.id.openChargeActivity -> {
                    Log.d(TAG, "Open ChargeActivity")
                    openActivity(activity = ChargeActivity::class.java)
                    true
                }
                R.id.openConfirmActivity -> {
                    Log.d(TAG, "Open ConfirmActivity")
                    openActivity(activity = ConfirmActivity::class.java)
                    true
                }
                R.id.openPendingTransactionsActivity -> {
                    Log.d(TAG, "Open PendingTransactionsActivity")
                    openActivity(activity = PendingTransactionsActivity::class.java)
                    true
                }
                R.id.openReportActivity -> {
                    Log.d(TAG, "Open ReportActivity")
                    openActivity(activity = ReportActivity::class.java)
                    true
                }
                R.id.openDeactivationActivity -> {
                    Log.d(TAG, "Open DeactivationActivity")
                    openActivity(activity =
                    DeactivationActivity::class.java)
                    true
                }
                else -> false
            }
        }

        mAmountTextField = mBinding.amountTextField
        mOperationTypeComboBox = mBinding.operationTypeComboBox
        mOriginNsuTextField = mBinding.originNsuTextField
        mPaymentSchemeComboBox = mBinding.paymentSchemeComboBox
        mWalletPaymentSchemeComboBox = mBinding.walletPaymentSchemeComboBox
        mInstallmentTypeComboBox = mBinding.installmentTypeComboBox
        mInstallmentNumberTextField = mBinding.installmentNumberTextField
        mManualEntrySwitch = mBinding.manualEntrySwitch
        mPayButton = mBinding.payButton
        mAbortOperationButton = mBinding.abortOperationButton
        mProgressBar = mBinding.progressBar

        Log.d(TAG, "MerchantData: ${mPaymentApplication.merchantData}")
        configureOperationType()
        configureAmount()
        configurePaymentTypes()
        configureManualEntry()

        hideProgressBar()
        mPayButton.setOnClickListener { onPayButtonClick() }

        configureAbortOperationButton()

        mWalletPaymentSchemeComboBox.visibility = View.GONE
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
            NotificationMessage.showMessageBox(this, "Error", "Communication service not available. Returning to terminal Initialization") {
                backMainActivity()
            }
        }
    }

    private fun onPayButtonClick() {
        mPayButton.isEnabled = false
        mAbortOperationButton.visibility = Button.VISIBLE

        mPaymentApplication.communicationService?.let { communicationService: IAditumSdkService ->
            Log.d(TAG, "CommunicationService: $communicationService")
            showProgressBar()

            mPaymentRequest.merchantChargeId = UUID.randomUUID().toString()
            Log.d(TAG, "PaymentRequest: $mPaymentRequest");

            thread { communicationService.pay(mPaymentRequest, mPayResponseCallback) }
        } ?: run {
            NotificationMessage.showMessageBox(this, "Error", "Communication service not available. Returning to terminal Initialization") {
                backMainActivity()
            }
        }
    }

    private fun configureManualEntry() {
        mManualEntrySwitch.isChecked = false
        mManualEntrySwitch.setOnCheckedChangeListener { _, isChecked ->
            mPaymentRequest.manualEntry = isChecked
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

    private fun configureOperationType() {
        val operationTypes: List<PayOperationType> = listOf(
            PayOperationType.Authorization,
            PayOperationType.PreAuthorization,
            PayOperationType.IncrementalPreAuthorization,
            PayOperationType.PreAuthorizationCapture)

            val operationTypeAutoCompleteTextView = mOperationTypeComboBox.editText as? AutoCompleteTextView
            val operationTypeAdapter: EnumAdapter<PayOperationType> = EnumAdapter(this, operationTypes)

            operationTypeAutoCompleteTextView?.setText(PayOperationType.Authorization.toString(), false)
            operationTypeAutoCompleteTextView?.setAdapter(operationTypeAdapter)
            operationTypeAutoCompleteTextView?.setOnItemClickListener { parent, _, position, _ ->
                val operationType = parent.getItemAtPosition(position) as? PayOperationType
                mPaymentRequest.operationType = operationType ?: PayOperationType.Authorization

                if (mPaymentRequest.operationType > PayOperationType.PreAuthorization) {
                    mOriginNsuTextField.visibility = View.VISIBLE
                } else {
                    mOriginNsuTextField.visibility = View.GONE
                    mOriginNsuTextField.editText?.setText("")
                }
            }

            mOriginNsuTextField.visibility = View.GONE
            mOriginNsuTextField.editText?.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(editable: Editable?) {
                    val value = editable ?: return
                    mPaymentRequest.originalNsu = value.toString()
                }
            })
        }

        private fun configureAmount() {
            val amountInput = mAmountTextField.editText
            amountInput?.setText("")

            amountInput?.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(editable: Editable?) {
                    formatCurrency(editable, this)
                }
            })
        }

        private fun configurePaymentTypes() {
            val paymentSchemes = mPaymentApplication.merchantData?.paymentScheme
            val paymentTypesAcquirer: List<PaymentTypeAdapter.PaymentTypeAcquirer> = paymentSchemes?.map { PaymentTypeAdapter.PaymentTypeAcquirer(it.paymentType, it.acquirer) } ?: listOf()
            val paymentSchemeAdapter: PaymentTypeAdapter = PaymentTypeAdapter(this, paymentTypesAcquirer)

            val paymentTypeAutoCompleteTextView = mPaymentSchemeComboBox.editText as? AutoCompleteTextView
            paymentTypeAutoCompleteTextView?.setAdapter(paymentSchemeAdapter)
            paymentTypeAutoCompleteTextView?.setOnItemClickListener { parent, _, position, _ ->
                val paymentType = parent.getItemAtPosition(position) as? PaymentTypeAdapter.PaymentTypeAcquirer
                val paymentScheme = paymentSchemes?.firstOrNull { it.paymentType == paymentType?.paymentType && it.acquirer == paymentType?.acquirer }

                mPaymentRequest.paymentType = paymentScheme?.paymentType ?: PaymentType.Undefined
                mPaymentRequest.acquirer = paymentScheme?.acquirer ?: Acquirer.Undefined
                mPaymentRequest.scheme = paymentScheme?.scheme ?: ""
                mPaymentRequest.installmentType = InstallmentType.Undefined
                mPaymentRequest.installmentNumber = 0
                mPaymentRequest.isQrCode = paymentType?.paymentType == PaymentType.Pix

                if (paymentType?.paymentType == PaymentType.Credit) {
                    val paymentTypeRestriction = paymentScheme?.paymentTypeRestriction
                    val installmentTypes: List<InstallmentType> = if (paymentTypeRestriction?.any { it.paymentType == PaymentType.Credit } == true) {
                        paymentTypeRestriction.mapNotNull { it.installmentType.takeIf { it != InstallmentType.Undefined } }.distinct()
                    } else {
                        emptyList()
                    }

                    mInstallmentTypeComboBox.visibility = View.VISIBLE
                    mWalletPaymentSchemeComboBox.visibility = View.GONE

                    configureInstalmentType(installmentTypes)
                } else if (paymentType?.paymentType == PaymentType.Wallet) {
                    mInstallmentTypeComboBox.visibility = View.GONE
                    mInstallmentNumberTextField.visibility = View.GONE
                    configureWalletPaymentTypes(paymentScheme)
                } else {
                    mInstallmentTypeComboBox.visibility = View.GONE
                    mInstallmentNumberTextField.visibility = View.GONE
                    mWalletPaymentSchemeComboBox.visibility = View.GONE
                }
            }

            mInstallmentTypeComboBox.visibility = View.GONE
            mInstallmentNumberTextField.visibility = View.GONE
        }

        private fun configureWalletPaymentTypes(paymentScheme: PaymentScheme? = null) {
            mWalletPaymentSchemeComboBox.visibility = View.VISIBLE
            val paymentInstallmentType: List<WalletPaymentTypeAdapter.PaymentInstallmentType> = paymentScheme?.paymentTypeRestriction?.map {
                WalletPaymentTypeAdapter.PaymentInstallmentType(it.paymentType, it.installmentType)
            } ?: listOf()
            val paymentInstallmentAdapter: WalletPaymentTypeAdapter = WalletPaymentTypeAdapter(this, paymentInstallmentType)

            val walletPaymentTypeAutoCompleteTextView = mWalletPaymentSchemeComboBox.editText as? AutoCompleteTextView
            walletPaymentTypeAutoCompleteTextView?.setAdapter(paymentInstallmentAdapter)
            walletPaymentTypeAutoCompleteTextView?.setOnItemClickListener { parent, _, position, _ ->
                val paymentType = parent.getItemAtPosition(position) as? WalletPaymentTypeAdapter.PaymentInstallmentType
                val paymentTypeRestriction = paymentScheme?.paymentTypeRestriction?.firstOrNull {
                    it.paymentType == paymentType?.paymentType && it.installmentType == paymentType?.installmentType
                }

                mPaymentRequest.paymentType = paymentTypeRestriction?.paymentType ?: PaymentType.Undefined
                mPaymentRequest.acquirer = paymentScheme?.acquirer ?: Acquirer.Undefined
                mPaymentRequest.scheme = paymentScheme?.scheme ?: ""
                mPaymentRequest.installmentType = paymentTypeRestriction?.installmentType ?: InstallmentType.Undefined
                mPaymentRequest.installmentNumber = 0
                mPaymentRequest.isQrCode = true

                if (paymentTypeRestriction?.installmentType == InstallmentType.Merchant ||
                paymentTypeRestriction?.installmentType == InstallmentType.Issuer) {
                    mInstallmentNumberTextField.visibility = View.VISIBLE
                    configureInstallmentNumber()
                } else {
                    mInstallmentNumberTextField.visibility = View.GONE
                }
            }
        }

        private fun configureInstalmentType(installmentTypes: List<InstallmentType>) {
            val installmentTypeAutoCompleteTextView = mInstallmentTypeComboBox.editText as? AutoCompleteTextView
            val installmentTypeAdapter: EnumAdapter<InstallmentType> = EnumAdapter(this, installmentTypes)

            installmentTypeAutoCompleteTextView?.setText("")
            installmentTypeAutoCompleteTextView?.setAdapter(installmentTypeAdapter)

            installmentTypeAutoCompleteTextView?.setOnItemClickListener { parent, _, position, _ ->
                val installmentType = parent.getItemAtPosition(position) as? InstallmentType
                mPaymentRequest.installmentType = installmentType ?: InstallmentType.Undefined
                mPaymentRequest.installmentNumber = 0

                if (mPaymentRequest.installmentType != InstallmentType.None && mPaymentRequest.installmentType != InstallmentType.Undefined) {
                    mInstallmentNumberTextField.visibility = View.VISIBLE
                    configureInstallmentNumber()
                } else {
                    mInstallmentNumberTextField.visibility = View.GONE
                }
            }
        }

        private fun configureInstallmentNumber() {
            val installmentNumberInput = mInstallmentNumberTextField.editText
            installmentNumberInput?.setText("")
            installmentNumberInput?.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(editable: Editable?) {
                    val value = editable ?: return

                    try {
                        val installmentNumber = value.toString().toInt()
                        mPaymentRequest.installmentNumber = installmentNumber
                    } catch (e: NumberFormatException) {
                        e.printStackTrace()
                    }
                }
            })
        }

        private fun formatCurrency(editable: Editable?, thiz: TextWatcher) {
            val value = editable ?: return

            try {
                val amountInput = mAmountTextField.editText ?: return
                val amountString = value.toString().replace(Regex("[^0-9]"), "")
                val amount = amountString.toDouble()

                var currency:Currency?

                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
                    currency = Currency.getAvailableCurrencies().firstOrNull {
                        it.numericCode == mPaymentRequest.currency
                    }
                } else {
                    currency =  Currency.getAvailableCurrencies().firstOrNull {
                        mISO4217Map[it.currencyCode] == mPaymentRequest.currency
                    } ?: Currency.getInstance(DefaultCurrencyCode) // default to BRL
                }

                val currencyString = currency?.currencyCode

                val currencyFormat = NumberFormat.getCurrencyInstance(Locale.getDefault())
                currencyFormat.currency = Currency.getInstance(currencyString)

                mPaymentRequest.amount = amountString.toLong()
                val formattedAmount = currencyFormat.format(amount / 100)

                amountInput.removeTextChangedListener(thiz)
                amountInput.setText(formattedAmount)
                amountInput.setSelection(formattedAmount.length)
                amountInput.addTextChangedListener(thiz)
            } catch (e: NumberFormatException) {
                e.printStackTrace()
            }
        }

        private fun showProgressBar() {
            mProgressBar.visibility = CircularProgressIndicator.VISIBLE
        }

        private fun hideProgressBar() {
            mProgressBar.visibility = CircularProgressIndicator.INVISIBLE
        }

        private fun backMainActivity() {
            Log.d(TAG, "backMainActivity")
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }

        private fun openActivity(activity: Class<out Activity>) {
            val intent = Intent(this, activity)
            startActivity(intent)
        }
    }
