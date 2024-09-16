package br.com.aditum.paymentexample

import android.util.Log
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import androidx.fragment.app.Fragment
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class ReceiptsBottomSheetDialogFragment : BottomSheetDialogFragment() {
    private var fragment: Fragment? = null

    companion object {
        val TAG = ReceiptsBottomSheetDialogFragment::class.simpleName
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_receipts_bottom_sheet_dialog, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fragment?.let {
            childFragmentManager.beginTransaction().replace(R.id.childFragmentContainer, it).commit()
        } ?: run {
            Log.e(TAG, "Fragment not set")
            dismiss()
        }
    }

    fun setFragment(fragment: Fragment) {
        Log.d(TAG, "setFragment: $fragment")
        this.fragment = fragment
    }
}

