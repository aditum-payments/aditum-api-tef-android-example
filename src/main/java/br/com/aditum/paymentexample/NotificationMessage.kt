package br.com.aditum.paymentexample

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.widget.Toast

class NotificationMessage {
    companion object {
        private var mToast: Toast? = null

        fun showMessageBox(context: Context, title: String, message: String) {
            (context as Activity).runOnUiThread {
                val alertDialog = AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("Ok") { dialog, _ -> dialog.dismiss() }
                .create()
                alertDialog.show()
            }
        }

        fun showMessageBox(context: Context, title: String, message: String, callback: () -> Unit) {
            (context as Activity).runOnUiThread {
                val alertDialog = AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("Ok") { dialog, _ ->
                    callback.invoke()
                    dialog.dismiss()
                }
                .create()
                alertDialog.show()
            }
        }

        fun showToast(context: Context, message: String) {
            try {
                (context as Activity).runOnUiThread {
                    mToast?.cancel()
                    mToast = Toast.makeText(context, message, Toast.LENGTH_SHORT)
                    mToast?.show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
