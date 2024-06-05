package br.com.aditum.paymentexample

import android.app.Activity
import android.content.Intent
import android.os.Bundle
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

import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.elevation.SurfaceColors
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.textfield.TextInputLayout

import br.com.aditum.data.v2.INotificationCallback
import br.com.aditum.data.v2.enums.AbecsCommands
import br.com.aditum.data.v2.enums.PaymentType
import br.com.aditum.data.v2.enums.InstallmentType
import br.com.aditum.data.v2.enums.PayOperationType
import br.com.aditum.data.v2.enums.Acquirer
import br.com.aditum.data.v2.model.MerchantData
import br.com.aditum.data.v2.model.payment.PaymentRequest
import br.com.aditum.data.v2.model.payment.PaymentScheme
import br.com.aditum.data.v2.model.payment.PaymentResponse
import br.com.aditum.data.v2.model.payment.PaymentResponseCallback

import br.com.aditum.IAditumSdkService
import br.com.aditum.paymentexample.databinding.ActivityPaymentBinding
import br.com.aditum.paymentexample.components.EnumAdapter
import br.com.aditum.paymentexample.components.PaymentTypeAdapter
import br.com.aditum.paymentexample.components.WalletPaymentTypeAdapter
import kotlin.concurrent.thread

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

    private val mPayResponseCallback = object : PaymentResponseCallback.Stub() {
        override fun onResponse(paymentResponse: PaymentResponse?) {
            Log.d(TAG, "onResponse - paymentResponse: $paymentResponse")

            paymentResponse?.let {
                if (paymentResponse.isApproved) {
                    NotificationMessage.showMessageBox(this@PaymentActivity, "Success", "onResponse - paymentResponse: $paymentResponse")
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

    private val mNotificationCallback = object : INotificationCallback.Stub() {
        override fun onNotification(message: String?, command: AbecsCommands?) {
            Log.d(TAG, "PaymentActivity::onNotification - message: $message, command: $command")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate")
        
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)
        
        mPaymentApplication = application as PaymentApplication
        mPaymentApplication.communicationService?.registerNotificationCallback(mNotificationCallback);

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
                    openUpdateEmvTablesActivity()
                    true
                }
                R.id.openCancelActivity -> {
                    Log.d(TAG, "Open CancelationActivity")
                    openCancelationActivity()
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

    private fun onPayButtonClick() {
        mPayButton.isEnabled = false
        mAbortOperationButton.visibility = Button.VISIBLE

        mPaymentApplication.communicationService?.let { communicationService: IAditumSdkService ->
            Log.d(TAG, "CommunicationService: $communicationService")
            showProgressBar()
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

            val currencyString = Currency.getAvailableCurrencies().firstOrNull { it.numericCode == mPaymentRequest.currency } ?.currencyCode
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

    private fun openUpdateEmvTablesActivity() {
        val intent = Intent(this, UpdateEmvTablesActivity::class.java)
        startActivity(intent)
    }

    private fun openCancelationActivity() {
        val intent = Intent(this, CancelationActivity::class.java)
        startActivity(intent)
    }
}
