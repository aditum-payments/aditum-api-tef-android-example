package br.com.aditum.paymentexample

import android.app.Application
import android.content.Context
import android.content.ComponentName
import android.content.ServiceConnection
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.os.Build;
import android.util.Log

import br.com.aditum.IAditumSdkService
import br.com.aditum.data.v2.model.MerchantData

class PaymentApplication : Application() {
    public val TAG = PaymentApplication::class.simpleName

    public val PACKAGE_BASE_NAME: String = "br.com.aditum"
    public val PACKAGE_NAME: String = PACKAGE_BASE_NAME + ".smartpostef"
    public val ACTION_COMMUNICATION_SERVICE: String = PACKAGE_BASE_NAME + ".AditumSdkService"

    @Volatile
    private var mIsServiceConnected: Boolean = false
    val isServiceConnected: Boolean
    get() = mIsServiceConnected

    @Volatile
    private var mAditumSdkService: IAditumSdkService? = null
    val communicationService: IAditumSdkService?
    get() = mAditumSdkService

    @Volatile
    public var merchantData: MerchantData? = null

    @Volatile
    public var mUseOnlySdk: Boolean = false
    var useOnlySdk: Boolean
    get() = mUseOnlySdk
    set(value) {
        mUseOnlySdk = value
    }

    private val mServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(componentName: ComponentName, service: IBinder) {
            Log.d(TAG, "onServiceConnected")
            mAditumSdkService = IAditumSdkService.Stub.asInterface(service)
            setServiceConnected(true)
        }

        override fun onServiceDisconnected(componentName: ComponentName) {
            Log.d(TAG, "onServiceDisconnected")
            mAditumSdkService = null
            setServiceConnected(false)
        }
    }

    private var mServiceConnectionListener: OnServiceConnectionListener? = null
    var serviceConnectionListener: OnServiceConnectionListener?
        get() = mServiceConnectionListener
        set(listener) {
            mServiceConnectionListener = listener
        }

    public interface OnServiceConnectionListener {
        fun onServiceConnection(serviceConnected: Boolean)
    }

    override fun onCreate() {
        super.onCreate()

        Log.d(TAG, "onCreate")
    }

    override fun onTerminate() {
        super.onTerminate()

        Log.d(TAG, "onTerminate")
    }

    public fun startAditumSdkService() {
        Log.d(TAG, "startAditumSdkService")

        val intent: Intent = Intent(ACTION_COMMUNICATION_SERVICE)
        intent.setPackage(PACKAGE_NAME)
        Log.d(TAG, "ACTION_COMMUNICATION_SERVICE: $ACTION_COMMUNICATION_SERVICE")
        Log.d(TAG, "PACKAGE_NAME: $PACKAGE_NAME")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Log.d(TAG, "Android Oreo or higher: " + Build.VERSION.SDK_INT);
            startForegroundService(intent);
        } else {
            Log.d(TAG, "Android Nougat or lower: " + Build.VERSION.SDK_INT);
            startService(intent);
        }

        val bound = bindService(intent, mServiceConnection, (Context.BIND_AUTO_CREATE))
        Log.d(TAG, "startAditumSdkService - bindService returned: $bound")
    }

    private fun setServiceConnected(isConnected: Boolean) {
        Log.d(TAG, "setServiceConnected - isConnected: $isConnected")
        synchronized(this) {
            mIsServiceConnected = isConnected
            mServiceConnectionListener?.onServiceConnection(mIsServiceConnected)
        }
    }
}
