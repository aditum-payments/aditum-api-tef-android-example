package br.com.aditum.paymentexample

import android.util.Log

import br.com.aditum.data.v2.IPaymentCallback
import br.com.aditum.data.v2.model.callbacks.GetClearDataRequest
import br.com.aditum.data.v2.model.callbacks.GetClearDataFinishedCallback
import br.com.aditum.data.v2.model.callbacks.GetMenuSelectionRequest
import br.com.aditum.data.v2.model.callbacks.GetMenuSelectionFinishedCallback

import br.com.aditum.data.v2.enums.AbecsCommands
import br.com.aditum.data.v2.enums.TransactionStatus

abstract class PaymentCallback : IPaymentCallback.Stub() {
    public val TAG = PaymentCallback::class.simpleName

    override fun notification(message: String?, transactionStatus: TransactionStatus?, command: AbecsCommands?) {}

    override fun pinNotification(message: String, length: Int) {}

    override fun startGetClearData(clearDataRequest: GetClearDataRequest?, finished: GetClearDataFinishedCallback?) {}

    override fun startGetMenuSelection(menuSelectionRequest: GetMenuSelectionRequest?, finished: GetMenuSelectionFinishedCallback?) {}

    override fun qrCodeGenerated(qrCode: String, expirationTime: Int) {}
}

