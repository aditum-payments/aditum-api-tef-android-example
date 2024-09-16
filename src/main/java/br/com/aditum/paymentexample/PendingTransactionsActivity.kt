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
import br.com.aditum.data.v2.model.Charge
import br.com.aditum.data.v2.model.transactions.PendingTransactionsCallback

import br.com.aditum.IAditumSdkService
import br.com.aditum.paymentexample.databinding.ActivityPendingTransactionsBinding
import kotlin.concurrent.thread

class PendingTransactionsActivity : AppCompatActivity() {
    public val TAG = PendingTransactionsActivity::class.simpleName

    private lateinit var mAppBarConfiguration: AppBarConfiguration
    private lateinit var mBinding: ActivityPendingTransactionsBinding
    private lateinit var mPaymentApplication: PaymentApplication

    private lateinit var mButton: Button
    private lateinit var mProgressBar: CircularProgressIndicator

    private val mPaymentCallback = object : PaymentCallback() {
        override fun notification(message: String?, transactionStatus: TransactionStatus?, command: AbecsCommands?) {
            Log.d(TAG, "${TAG}::notification - message: $message, transactionStatus: $transactionStatus, command: $command")
            message?.let { msg ->
                NotificationMessage.showToast(this@PendingTransactionsActivity, msg)
            }
        }
    }

    private val mPendingTransactionsCallback = object : PendingTransactionsCallback.Stub() {
        override fun onResponse(pendingTransactions: List<Charge>?) {
            Log.d(TAG, "onResponse - pendingTransactions: $pendingTransactions")
            NotificationMessage.showMessageBox(this@PendingTransactionsActivity, "PendingTransactions", "onResponse - pendingTransactions: $pendingTransactions")
            runOnUiThread {
                hideProgressBar()
                mButton.isEnabled = true
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

        mBinding = ActivityPendingTransactionsBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        mBinding.toolbar.setBackgroundColor(color)

        setSupportActionBar(mBinding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        mButton = mBinding.pendingTransactionsButton
        mProgressBar = mBinding.progressBar

        mButton.setOnClickListener { onPendingTransactionsButtonClick() }
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

    private fun onPendingTransactionsButtonClick() {
        mPaymentApplication.communicationService?.let { communicationService: IAditumSdkService ->
            showProgressBar()
            thread { communicationService.pendingTransactions(mPendingTransactionsCallback) }
        } ?: run {
            NotificationMessage.showMessageBox(this, "Error", "Communication service not available.")
        }
    }
}
