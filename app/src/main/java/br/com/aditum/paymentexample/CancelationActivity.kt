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

import br.com.aditum.data.v2.INotificationCallback
import br.com.aditum.data.v2.enums.AbecsCommands
import br.com.aditum.data.v2.model.cancelation.CancelationRequest
import br.com.aditum.data.v2.model.cancelation.CancelationResponse
import br.com.aditum.data.v2.model.cancelation.CancelationResponseCallback

import br.com.aditum.IAditumSdkService
import br.com.aditum.paymentexample.databinding.ActivityCancelationBinding
import kotlin.concurrent.thread

class CancelationActivity : AppCompatActivity() {
    public val TAG = CancelationActivity::class.simpleName

    private lateinit var mAppBarConfiguration: AppBarConfiguration
    private lateinit var mBinding: ActivityCancelationBinding
    private lateinit var mPaymentApplication: PaymentApplication

    private lateinit var mCancelButton: Button
    private lateinit var mNsuTransactionEditText: TextInputEditText
    private lateinit var mProgressBar: CircularProgressIndicator

    private var mCancelationRequest: CancelationRequest? = null

    private val mCancelationResponseCallback = object : CancelationResponseCallback.Stub() {
        override fun onResponse(cancelationResponse: CancelationResponse?) {
            Log.d(TAG, "onResponse - cancelationResponse: $cancelationResponse")

            cancelationResponse?.let {
                if (cancelationResponse.canceled) {
                    NotificationMessage.showMessageBox(this@CancelationActivity, "Success", "onResponse - cancelationResponse: $cancelationResponse")
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
            }
        }
    }

    private val mNotificationCallback = object : INotificationCallback.Stub() {
        override fun onNotification(message: String?, command: AbecsCommands?) {
            Log.d(TAG, "CancelationActivity::onNotification - message: $message, command: $command")
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

        mBinding = ActivityCancelationBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        mBinding.toolbar.setBackgroundColor(color)

        setSupportActionBar(mBinding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        
        mNsuTransactionEditText = mBinding.nsuTransactionEditText
        mCancelButton = mBinding.cancelButton
        mProgressBar = mBinding.progressBar

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
    }

    private fun showProgressBar() {
        mProgressBar.visibility = CircularProgressIndicator.VISIBLE
    }

    private fun hideProgressBar() {
        mProgressBar.visibility = CircularProgressIndicator.INVISIBLE
    }

    private fun onCancelButtonClick() {
        val nsu: String = mNsuTransactionEditText.text.toString()

        mNsuTransactionEditText.isEnabled = false
        mCancelButton.isEnabled = false
        mCancelationRequest = CancelationRequest(nsu)

        mPaymentApplication.communicationService?.let { communicationService: IAditumSdkService ->
            showProgressBar()
            Log.d(TAG, "CancelationRequest: $mCancelationRequest");
            thread { communicationService.cancel(mCancelationRequest, mCancelationResponseCallback) }
        } ?: run {
            NotificationMessage.showMessageBox(this, "Error", "Communication service not available.")
        }
    }
}
