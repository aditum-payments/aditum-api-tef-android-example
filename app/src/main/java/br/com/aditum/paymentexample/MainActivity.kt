package br.com.aditum.paymentexample

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.RemoteException
import android.util.Log
import android.widget.Toast

import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.navigation.ui.AppBarConfiguration

import br.com.aditum.data.v2.INotificationCallback
import br.com.aditum.data.v2.enums.AbecsCommands
import br.com.aditum.data.v2.model.init.InitRequest
import br.com.aditum.data.v2.model.init.InitResponse
import br.com.aditum.data.v2.model.init.InitResponseCallback
import br.com.aditum.data.v2.model.PinpadMessages
import br.com.aditum.data.v2.model.ResponseError
import br.com.aditum.IAditumSdkService
import br.com.aditum.paymentexample.databinding.ActivityMainBinding

import android.widget.Button
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.elevation.SurfaceColors
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.textfield.TextInputEditText

import kotlin.concurrent.thread
import java.util.concurrent.CountDownLatch
import java.util.concurrent.locks.ReentrantLock

class MainActivity : AppCompatActivity() {
    public val TAG = MainActivity::class.simpleName

    private lateinit var mAppBarConfiguration: AppBarConfiguration
    private lateinit var mBinding: ActivityMainBinding
    private lateinit var mProgressBar: CircularProgressIndicator
    private lateinit var mPaymentApplication: PaymentApplication
    private lateinit var mInitButton: Button
    private lateinit var mActivationCodeEditText: TextInputEditText

    private val mInitResponseCallback = object : InitResponseCallback.Stub() {
        override fun onResponse(initResponse: InitResponse?) {
            initResponse?.let {
                Log.d(TAG, "onResponse - initResponse: $initResponse")
                if (initResponse.initialized) {
                    getMertchantData()
                } else {
                    NotificationMessage.showMessageBox(this@MainActivity, "Error", "onResponse - initResponse: $initResponse")
                }
            } ?: run {
                NotificationMessage.showMessageBox(this@MainActivity, "Error", "onResponse - initResponse is null")
            }
            runOnUiThread {
                mInitButton.isEnabled = true
                mActivationCodeEditText.isEnabled = true
                hideProgressBar()
            }
        }
    }

    private val mNotificationCallback = object : INotificationCallback.Stub() {
        override fun onNotification(message: String?, command: AbecsCommands?) {
            Log.d(TAG, "MainActivity::onNotification - message: $message, command: $command")
            message?.let { msg ->
                NotificationMessage.showToast(this@MainActivity, msg)
            }
        }
    }

    private val mServiceConnected = object : PaymentApplication.OnServiceConnectionListener {
        override fun onServiceConnection(serviceConnected: Boolean) {
            Log.d(TAG, "onServiceConnection - serviceConnected: $serviceConnected")
            if (serviceConnected) {
                mInitButton.isEnabled = true
                mActivationCodeEditText.isEnabled = true
                mPaymentApplication.communicationService?.registerNotificationCallback(mNotificationCallback)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate")

        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)

        NotificationMessage.createToast(applicationContext)

        val color = SurfaceColors.SURFACE_3.getColor(this)
        window.statusBarColor = color
        window.navigationBarColor = color

        mBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        mBinding.toolbar.title = "Terminal Initialization"
        mBinding.toolbar.setBackgroundColor(color)
        setSupportActionBar(mBinding.toolbar)

        mPaymentApplication = application as PaymentApplication
        mPaymentApplication.serviceConnectionListener = mServiceConnected
        mPaymentApplication.startAditumSdkService()

        mInitButton = mBinding.initButton
        mInitButton.setOnClickListener { onInitButtonClick() }

        mActivationCodeEditText = mBinding.activationCodeEditText
        mActivationCodeEditText.setText("880776044")

        mProgressBar = mBinding.progressBar
        hideProgressBar()

        mInitButton.isEnabled = false
        mActivationCodeEditText.isEnabled = false
    }

    private fun onInitButtonClick() {
        Log.d(TAG, "onInitButtonClick")

        val activationCode: String = mActivationCodeEditText.text.toString()
        if (activationCode.isEmpty() || activationCode.length != 9) {
            NotificationMessage.showMessageBox(this, "Error", "Invalid activation code")
            return
        }

        mInitButton.isEnabled = false
        mActivationCodeEditText.isEnabled = false
        showProgressBar()

        thread {
            mPaymentApplication.communicationService?.let { communicationService: IAditumSdkService ->
                val pinpadMessages = PinpadMessages()
                pinpadMessages.mainMessage = "Payment Example"
                
                val initRequest = InitRequest()
                initRequest.pinpadMessages = pinpadMessages
                initRequest.activationCode = activationCode
                initRequest.applicationName = "PaymentExample"
                initRequest.applicationVersion = "1.0.0"
                initRequest.applicationToken = "mk_Lfq9yMzRoYaHjowfxLvoyi"
                communicationService.init(initRequest, mInitResponseCallback)
            } ?: run {
                NotificationMessage.showMessageBox(this, "Error", "Communication service not available. Trying to recreate communication with service.")
                mPaymentApplication.startAditumSdkService()
                
                runOnUiThread {
                    mInitButton.isEnabled = true
                    mActivationCodeEditText.isEnabled = true
                    hideProgressBar()
                }
            }
        }
    }

    private fun getMertchantData() {
        Log.d(TAG, "getMertchantData")
        thread {
            mPaymentApplication.communicationService?.let { communicationService: IAditumSdkService ->
                mPaymentApplication.merchantData = communicationService.getMerchantData()
                startPaymentActivity()
            } ?: run {
                NotificationMessage.showMessageBox(this, "Error", "Communication service not available. Trying to recreate communication with service.")
            }
        }
    }

    private fun startPaymentActivity() {
        Log.d(TAG, "startPaymentActivity")
        val intent = Intent(this, PaymentActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun showProgressBar() {
        mProgressBar.visibility = CircularProgressIndicator.VISIBLE
    }

    private fun hideProgressBar() {
        mProgressBar.visibility = CircularProgressIndicator.INVISIBLE
    }
}