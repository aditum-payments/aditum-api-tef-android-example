package br.com.aditum.paymentexample.components

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView

import br.com.aditum.paymentexample.R

class EnumAdapter<T>(context: Context, private val enumValues: List<T>) :
    ArrayAdapter<T>(context, 0, enumValues) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val enumValue = getItem(position)
        val view = convertView ?: LayoutInflater.from(context).inflate(
            R.layout.item_payment_scheme,
            parent,
            false
        )

        val nameTextView: TextView = view.findViewById(R.id.nameTextView)
        nameTextView.text = if (enumValue?.toString().equals("Undefined"))  "" else enumValue?.toString()

        return view
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        return getView(position, convertView, parent)
    }
}
