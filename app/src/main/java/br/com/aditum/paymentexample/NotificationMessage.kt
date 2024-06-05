package br.com.aditum.paymentexample

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.widget.Toast

class NotificationMessage {
    companion object {
        private lateinit var mToast: Toast

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

        fun createToast(applicationContext: Context) {
            mToast = Toast.makeText(applicationContext, "", Toast.LENGTH_SHORT)
        }
    
        fun showToast(context: Context, message: String) {
            (context as Activity).runOnUiThread {
                mToast.setText(message)
                mToast.show()
            }
        }
    }
}
