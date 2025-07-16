package br.com.aditum.paymentexample

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.RemoteException
import android.util.Log
import android.view.ViewGroup
import android.widget.Toast

import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import br.com.aditum.IAditumSdkService
import br.com.aditum.data.v2.model.deactivation.DeactivationResponseCallback
import br.com.aditum.paymentexample.databinding.ActivityDeactivationBinding
import com.google.android.material.progressindicator.CircularProgressIndicator
import kotlin.concurrent.thread
import kotlin.system.exitProcess


class DeactivationActivity : AppCompatActivity() {
    public val TAG = DeactivationActivity::class.simpleName

    private lateinit var mPaymentApplication: PaymentApplication
    private lateinit var mBinding: ActivityDeactivationBinding
    private lateinit var mProgressBar: CircularProgressIndicator
    private lateinit var mDeactivationAnimationThread: Thread
    private val handler = Handler(Looper.getMainLooper())

    private val mDeactivationResponseCallback = object: DeactivationResponseCallback.Stub() {
        @SuppressLint("SetTextI18n")
        override fun onResponse(response: Boolean) {
            Log.d(TAG, "onResponse - deactivationReponse: $response");
            mDeactivationAnimationThread.interrupt()

            Thread {
                if (response) {
                    Log.d(TAG, "onResponse - deactivationReponse: Succes.")
                    runOnUiThread{
                        mBinding.deactivationLayout.layoutParams.width =  ViewGroup.LayoutParams.WRAP_CONTENT
                        mBinding.deactivationLabel.text = "Terminal desativado"
                    }
                    Thread.sleep(1000)
                    runOnUiThread{ mBinding.deactivationLabel.text = "Finalizando..."}
                    Thread.sleep(1000)

                    val intent = Intent(this@DeactivationActivity, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                }
                else {
                    Log.d(TAG, "onResponse - deactivationReponse: Deactivation error.")
                    runOnUiThread{
                        mBinding.deactivationLayout.layoutParams.width =  ViewGroup.LayoutParams.WRAP_CONTENT
                        mBinding.deactivationLabel.text = "Falha ao desativar terminal."}
                    Thread.sleep(2000)
                    finish()
                }
            }.start()
        }

    }

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "DeactivationActivity - onCreate")
        super.onCreate(savedInstanceState)

        mPaymentApplication = application as PaymentApplication

        mBinding = ActivityDeactivationBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        mDeactivationAnimationThread = Thread {
            var count: Int = 0
            try {
                do {
                    val aux: String = ".".repeat(count % 4)
                    runOnUiThread {
                        mBinding.deactivationLabel.text = "Desativando$aux"
                        mBinding.deactivationLabel.invalidate()
                    }
                    count++

                    Thread.sleep(400)

                } while (!Thread.currentThread().isInterrupted)
            }
            catch (e: InterruptedException) {
                Thread.currentThread().interrupt();
            }

        }
        mDeactivationAnimationThread.start()

        Thread {
            Thread.sleep(4000) // gives some time for progress bar animation...
            mPaymentApplication.communicationService?.let { communicationService: IAditumSdkService ->
                Log.d(TAG, "Desativando...")
                communicationService.deactivate(mDeactivationResponseCallback)
            }
        }.start()
    }
}