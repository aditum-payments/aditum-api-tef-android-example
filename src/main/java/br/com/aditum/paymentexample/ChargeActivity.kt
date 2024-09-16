package br.com.aditum.paymentexample

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View

import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.navigation.ui.AppBarConfiguration
import androidx.activity.OnBackPressedCallback

import android.widget.Button
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.elevation.SurfaceColors
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.textfield.TextInputEditText

import br.com.aditum.data.v2.enums.AbecsCommands
import br.com.aditum.data.v2.enums.TransactionStatus
import br.com.aditum.data.v2.model.transactions.ChargeRequest
import br.com.aditum.data.v2.model.transactions.ChargeResponse
import br.com.aditum.data.v2.model.transactions.ChargeResponseCallback

import br.com.aditum.IAditumSdkService
import br.com.aditum.paymentexample.databinding.ActivityChargeBinding
import kotlin.concurrent.thread

class ChargeActivity : AppCompatActivity() {
    public val TAG = ChargeActivity::class.simpleName

    private lateinit var mAppBarConfiguration: AppBarConfiguration
    private lateinit var mBinding: ActivityChargeBinding
    private lateinit var mPaymentApplication: PaymentApplication

    private lateinit var mChargeButton: Button
    private lateinit var mNsuTransactionEditText: TextInputEditText
    private lateinit var mMerchantChargeIdEditText: TextInputEditText
    private lateinit var mProgressBar: CircularProgressIndicator

    private var mChargeRequest: ChargeRequest? = null

    private val mChargeResponseCallback = object : ChargeResponseCallback.Stub() {
        override fun onResponse(chargeResponse: ChargeResponse?) {
            Log.d(TAG, "onResponse - chargeResponse: $chargeResponse")

            chargeResponse?.let {
                if (chargeResponse.charge != null) {
                    NotificationMessage.showMessageBox(this@ChargeActivity, "Success", "onResponse - chargeResponse: $chargeResponse")
                } else {
                    NotificationMessage.showMessageBox(this@ChargeActivity, "Error", "onResponse - chargeResponse: $chargeResponse")
                }
            } ?: run {
                NotificationMessage.showMessageBox(this@ChargeActivity, "Error", "onResponse - chargeResponse is null")
            }
            runOnUiThread {
                hideProgressBar()
                mNsuTransactionEditText.isEnabled = true
                mMerchantChargeIdEditText.isEnabled = true
                mChargeButton.isEnabled = true
            }
        }
    }

    private val mPaymentCallback = object : PaymentCallback() {
        override fun notification(message: String?, transactionStatus: TransactionStatus?, command: AbecsCommands?) {
            Log.d(TAG, "${TAG}::notification - message: $message, transactionStatus: $transactionStatus, command: $command")
            message?.let { msg ->
                NotificationMessage.showToast(this@ChargeActivity, msg)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate")

        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)

        mPaymentApplication = application as PaymentApplication
        mPaymentApplication.communicationService?.registerPaymentCallback(mPaymentCallback);

        val color = SurfaceColors.SURFACE_3.getColor(this)
        window.statusBarColor = color
        window.navigationBarColor = color

        mBinding = ActivityChargeBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        mBinding.toolbar.setBackgroundColor(color)

        setSupportActionBar(mBinding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        mNsuTransactionEditText = mBinding.nsuTransactionEditText
        mMerchantChargeIdEditText = mBinding.merchantChargeIdEditText

        mChargeButton = mBinding.chargeButton
        mProgressBar = mBinding.progressBar

        mChargeButton.setOnClickListener { onChargeButtonClick() }
        hideProgressBar()

        val backCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                Log.d(TAG, "handleOnBackPressed")
                finish()
            }
        }

        this.onBackPressedDispatcher.addCallback(this, backCallback)
        mBinding.toolbar.setNavigationOnClickListener { backCallback.handleOnBackPressed() }
    }

    private fun showProgressBar() {
        mProgressBar.visibility = CircularProgressIndicator.VISIBLE
    }

    private fun hideProgressBar() {
        mProgressBar.visibility = CircularProgressIndicator.INVISIBLE
    }

    private fun onChargeButtonClick() {
        val nsu: String = mNsuTransactionEditText.text.toString()
        val merchantChargeId: String = mMerchantChargeIdEditText.text.toString()

        if (nsu.isEmpty() && merchantChargeId.isEmpty()) {
            NotificationMessage.showMessageBox(this, "Error", "NSU or Merchant Charge ID are required.")
            return
        }

        mNsuTransactionEditText.isEnabled = false
        mMerchantChargeIdEditText.isEnabled = false

        mChargeButton.isEnabled = false
        mChargeRequest = ChargeRequest(nsu, merchantChargeId)

        mPaymentApplication.communicationService?.let { communicationService: IAditumSdkService ->
            showProgressBar()
            Log.d(TAG, "ChargeRequest: $mChargeRequest");
            thread { communicationService.charge(mChargeRequest, mChargeResponseCallback) }
        } ?: run {
            NotificationMessage.showMessageBox(this, "Error", "Communication service not available.")
        }
    }
}
