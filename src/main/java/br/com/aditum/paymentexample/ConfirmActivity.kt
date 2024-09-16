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
import br.com.aditum.data.v2.model.transactions.ConfirmTransactionCallback

import br.com.aditum.IAditumSdkService
import br.com.aditum.paymentexample.databinding.ActivityConfirmBinding
import kotlin.concurrent.thread

class ConfirmActivity : AppCompatActivity() {
    public val TAG = ConfirmActivity::class.simpleName

    private lateinit var mAppBarConfiguration: AppBarConfiguration
    private lateinit var mBinding: ActivityConfirmBinding
    private lateinit var mPaymentApplication: PaymentApplication

    private lateinit var mConfirmButton: Button
    private lateinit var mNsuTransactionEditText: TextInputEditText
    private lateinit var mProgressBar: CircularProgressIndicator

    private val mConfirmResponseCallback = object : ConfirmTransactionCallback.Stub() {
        override fun onResponse(confirmed: Boolean) {
            Log.d(TAG, "onResponse - confirmed: $confirmed")

            NotificationMessage.showMessageBox(this@ConfirmActivity, "Confirm", "confirmed: $confirmed")

            runOnUiThread {
                hideProgressBar()
                mNsuTransactionEditText.isEnabled = true
                mConfirmButton.isEnabled = true
            }
        }
    }

    private val mPaymentCallback = object : PaymentCallback() {
        override fun notification(message: String?, transactionStatus: TransactionStatus?, command: AbecsCommands?) {
            Log.d(TAG, "${TAG}::notification - message: $message, transactionStatus: $transactionStatus, command: $command")
            message?.let { msg ->
                NotificationMessage.showToast(this@ConfirmActivity, msg)
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

        mBinding = ActivityConfirmBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        mBinding.toolbar.setBackgroundColor(color)

        setSupportActionBar(mBinding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        mNsuTransactionEditText = mBinding.nsuTransactionEditText
        mConfirmButton = mBinding.confirmButton
        mProgressBar = mBinding.progressBar

        mConfirmButton.setOnClickListener { onConfirmButtonClick() }
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

    private fun onConfirmButtonClick() {
        val nsu: String = mNsuTransactionEditText.text.toString()

        mNsuTransactionEditText.isEnabled = false
        mConfirmButton.isEnabled = false

        mPaymentApplication.communicationService?.let { communicationService: IAditumSdkService ->
            showProgressBar()
            thread { communicationService.confirmTransaction(nsu, mConfirmResponseCallback) }
        } ?: run {
            NotificationMessage.showMessageBox(this, "Error", "Communication service not available.")
        }
    }
}
