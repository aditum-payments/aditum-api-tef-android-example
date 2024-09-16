package br.com.aditum.paymentexample.components

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView

import br.com.aditum.data.v2.enums.PaymentType
import br.com.aditum.data.v2.enums.InstallmentType
import br.com.aditum.paymentexample.R

class WalletPaymentTypeAdapter(context: Context, paymentType: List<WalletPaymentTypeAdapter.PaymentInstallmentType?>) :
    ArrayAdapter<WalletPaymentTypeAdapter.PaymentInstallmentType?>(context, 0, paymentType) {

    data class PaymentInstallmentType(
        val paymentType: PaymentType?,
        val installmentType: InstallmentType?,
    ) {
        override fun toString(): String {
            val result = if (installmentType != InstallmentType.Undefined && installmentType != InstallmentType.None) {
                "${paymentType.toString()} - ${installmentType.toString()}"
            } else {
                paymentType.toString()
            }
            return result
        }
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val item = getItem(position)
        val view = convertView ?: LayoutInflater.from(context).inflate(
            R.layout.item_payment_scheme,
            parent,
            false
        )

        val nameTextView: TextView = view.findViewById(R.id.nameTextView)
        nameTextView.text = item?.toString() ?: ""

        return view
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        return getView(position, convertView, parent)
    }
}
