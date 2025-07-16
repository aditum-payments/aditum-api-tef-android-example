package br.com.aditum.paymentexample

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AutoCompleteTextView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.util.Pair
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import br.com.aditum.IAditumSdkService
import br.com.aditum.data.v2.enums.ChargeStatus
import br.com.aditum.data.v2.model.Charge
import br.com.aditum.data.v2.model.payment.SummarizedPayment
import br.com.aditum.data.v2.model.report.ReportRequest
import br.com.aditum.data.v2.model.report.ReportResponse
import br.com.aditum.data.v2.model.report.ReportResponseCallback
import br.com.aditum.data.v2.model.transactions.ChargeRequest
import br.com.aditum.data.v2.model.transactions.ChargeResponse
import br.com.aditum.data.v2.model.transactions.ChargeResponseCallback
import br.com.aditum.device.IPrinterSdk
import br.com.aditum.paymentexample.components.EnumAdapter
import br.com.aditum.paymentexample.components.Utils
import br.com.aditum.paymentexample.databinding.ActivityReportBinding
import br.com.aditum.paymentexample.databinding.ReportBottomSheetBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.elevation.SurfaceColors
import com.google.android.material.textfield.TextInputEditText
import java.text.SimpleDateFormat
import java.time.*
import java.util.*
import kotlin.concurrent.thread


class ReportActivity : AppCompatActivity() {
    private val myCalendar: Calendar = Calendar.getInstance()
    private val TAG = ReportActivity::class.simpleName
    private lateinit var mBinding: ActivityReportBinding
    private lateinit var mPaymentApplication: PaymentApplication
    private var mReportRequest: ReportRequest? = null
    private var dateFormat: String = "dd/MM/yy"
    private var beginDate: Date? = null
    private var endDate: Date? = null
    private var selectedStatus: ChargeStatus? = ChargeStatus.Undefined

    private val builder: MaterialDatePicker.Builder<Pair<Long, Long>> =
        MaterialDatePicker.Builder.dateRangePicker()

    @SuppressLint("SetTextI18n")
    private fun setDate(text: TextInputEditText, firstDate: Date, endDate: Date) {
        val dateFormat = SimpleDateFormat(dateFormat, Locale.getDefault())
        text.setText("${dateFormat.format(firstDate)} - ${dateFormat.format(endDate)}")
    }

    private fun getOnDateSetListener(text: TextInputEditText ): DatePickerDialog.OnDateSetListener {
        return DatePickerDialog.OnDateSetListener { _, year, month, day ->
            Log.d(TAG, "onDateSet")
            setDate(text, beginDate!!, endDate!!)
        }
    }

    private fun setStatusType() {
        val chargeStatusTypes: List<ChargeStatus> = listOf(
            ChargeStatus.Undefined,
            ChargeStatus.Authorized,
            ChargeStatus.PreAuthorized,
            ChargeStatus.Canceled,
            ChargeStatus.Partial,
            ChargeStatus.NotAuthorized,
            ChargeStatus.Pending
        )

        val chargeStatusTypeAutoCompleteTextView = mBinding.statusType as? AutoCompleteTextView
        val chargeStatusTypeAdapter: EnumAdapter<ChargeStatus> = EnumAdapter(this, chargeStatusTypes)

        chargeStatusTypeAutoCompleteTextView?.setAdapter(chargeStatusTypeAdapter)

        chargeStatusTypeAutoCompleteTextView?.setOnItemClickListener { parent, _, position, _ ->
            selectedStatus = parent.getItemAtPosition(position) as? ChargeStatus
            Log.d(TAG, "setOnItemClickListener - selectedStatus: $selectedStatus")
            val displayText = if (selectedStatus == ChargeStatus.Undefined) "" else selectedStatus.toString()
            chargeStatusTypeAutoCompleteTextView.setText(displayText, false)
        }
    }

    inner class PaymentAdapter(private val data: List<SummarizedPayment>) : RecyclerView.Adapter<PaymentAdapter.ViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.report_transaction_table, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = data[position]
            holder.bind(item)
        }

        override fun getItemCount(): Int {
            return data.size
        }

        private fun formatToISO8601(date: Date?): String {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            return date?.let { sdf.format(date) } ?: ""
        }

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val nsu: TextView = itemView.findViewById(R.id.nsu)
            private val dateTime: TextView = itemView.findViewById(R.id.date_time)
            private val paymentType: TextView = itemView.findViewById(R.id.payment_type)
            private val installmentType: TextView = itemView.findViewById(R.id.installment_type)
            private val installment: TextView = itemView.findViewById(R.id.installment_number)
            private val brand: TextView = itemView.findViewById(R.id.brand)
            private val chargeStatus: TextView = itemView.findViewById(R.id.charge_status)
            private val amount: TextView = itemView.findViewById(R.id.amount)
            private val reprintButton: MaterialButton = itemView.findViewById(R.id.reprintButton)

            fun bind(item: SummarizedPayment) {
                nsu.text = item.nsu
                dateTime.text = formatToISO8601(item.dateTime)
                paymentType.text = item.paymentType.toString()
                installmentType.text = item.installmentType.toString()
                installment.text = item.installmentNumber.toString()
                brand.text = item.brand.toString()
                chargeStatus.text = item.chargeStatus.toString()
                amount.text = Utils.formatCurrency(item.amount.toDouble())

                reprintButton.setOnClickListener {
                    Log.d(TAG, "Reprinting transaction with NSU: ${nsu.text} ...")
                    onRerint(nsu.text.toString())
                }
            }
        }

        private fun onRerint(nsu: String) {
            mPaymentApplication.communicationService?.let { communicationService: IAditumSdkService ->
                val mChargeResponseCallback = object : ChargeResponseCallback.Stub() {
                    override fun onResponse(chargeResponse: ChargeResponse?) {
                        Log.d(TAG, "onResponse - charge: $chargeResponse")
                        chargeResponse?.charge?.let { charge: Charge ->
                            communicationService?.deviceSdk?.printerSdk?.let { printerSdk: IPrinterSdk ->
                                val bottomSheetDialog = ReceiptsBottomSheetDialogFragment()
                                val cancelCallback = object : ReceiptsFragment.ICancelCallback {
                                    override fun onCancel() {
                                        bottomSheetDialog.dismiss()
                                    }
                                }
                                val cardholderReceipt = charge.cardholderReceipt ?: emptyList()
                                val merchantReceipt = charge.merchantReceipt ?: emptyList()
                                val fragment = ReceiptsFragment.newInstance(this@ReportActivity, merchantReceipt, cardholderReceipt, printerSdk, cancelCallback)
                                runOnUiThread {
                                    bottomSheetDialog.setFragment(fragment)
                                    bottomSheetDialog.show(supportFragmentManager, ReceiptsBottomSheetDialogFragment.TAG)
                                }
                            }
                        }
                    }
                }
                communicationService.charge(ChargeRequest(nsu), mChargeResponseCallback)
            } ?: run {
                NotificationMessage.showMessageBox(
                    this@ReportActivity,
                    "Error",
                    "Communication service not available."
                )
            }
        }

    }

    private fun showBottomSheetReport(response: ReportResponse?) {
        Log.d(TAG, "showBottomSheetReport")
        val inflater = LayoutInflater.from(this)
        val mReportBottomSheetBinding: ReportBottomSheetBinding = ReportBottomSheetBinding.inflate(inflater)
        val bottomSheetDialog = BottomSheetDialog(this)
        bottomSheetDialog.setContentView(mReportBottomSheetBinding.root)
        bottomSheetDialog.setCanceledOnTouchOutside(false)

        mReportBottomSheetBinding.totalAuthorizedAmount.text = Utils.formatCurrency(response?.totalAuthorizedAmount?.toDouble())
        mReportBottomSheetBinding.totalPreAuthorizedAmount.text = Utils.formatCurrency(response?.totalPreAuthorizedAmount?.toDouble())
        mReportBottomSheetBinding.totalCanceledAmount.text = Utils.formatCurrency(response?.totalCanceledAmount?.toDouble())
        mReportBottomSheetBinding.totalAuthorized.text = response?.totalNumberAuthorized.toString()
        mReportBottomSheetBinding.totalPreAuthorized.text = response?.totalNumberPreAuthorized.toString()
        mReportBottomSheetBinding.totalCanceled.text = response?.totalNumberCanceled.toString()
        mReportBottomSheetBinding.reportTransactionTable.adapter = PaymentAdapter(response?.transactions!!)
        var layout = GridLayoutManager(this, 1)
        mReportBottomSheetBinding.reportTransactionTable.layoutManager = layout
        mReportBottomSheetBinding.cancelButton.setOnClickListener{
                bottomSheetDialog.dismiss()
        }
        bottomSheetDialog.show()
    }


    private fun setDefaultDate() {
        myCalendar.clear(Calendar.DAY_OF_YEAR)
        myCalendar.add(Calendar.DAY_OF_YEAR, -6)

        beginDate = myCalendar.time
        beginDate?.hours = 0
        beginDate?.minutes= 0
        beginDate?.seconds= 0

        endDate = Date()
        endDate?.hours = 23
        endDate?.minutes = 59
        endDate?.seconds = 59
        setDate(mBinding.beginDate, beginDate!!, endDate!!)
    }

    private val mReportResponseCallback = object : ReportResponseCallback.Stub() {
        override fun onResponse(response: ReportResponse?) {
            Log.d(TAG, "onResponse - reportResponse: $response")
            runOnUiThread{
                Log.d(TAG, "Openning bottomsheet")
                showBottomSheetReport(response)
            }
        }
    }

    private fun onGenerateReport() {
        val statusList = if( selectedStatus == ChargeStatus.Undefined)
            arrayListOf(ChargeStatus.Authorized, ChargeStatus.PreAuthorized, ChargeStatus.Canceled, ChargeStatus.Partial, ChargeStatus.NotAuthorized, ChargeStatus.Pending, ChargeStatus.PendingCancel)
        else arrayListOf(selectedStatus) as ArrayList<ChargeStatus>

        mReportRequest = ReportRequest(
            beginDate,
            endDate,
            statusList
        )

        mPaymentApplication.communicationService?.let { communicationService: IAditumSdkService ->
            Log.d(TAG, "GenerateReport: $mReportRequest");
            thread { communicationService.generateReport(mReportRequest, mReportResponseCallback) }
        } ?: run {
            NotificationMessage.showMessageBox(this, "Error", "Communication service not available.")
        }

    }

    private fun setGoBackActionBar() {
        setSupportActionBar(mBinding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        val backCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                Log.d(TAG, "handleOnBackPressed")
                finish()
            }
        }
        this.onBackPressedDispatcher.addCallback(this, backCallback)
        mBinding.toolbar.setNavigationOnClickListener { backCallback.handleOnBackPressed() }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate")
        super.onCreate(savedInstanceState)

        mPaymentApplication = application as PaymentApplication
        mBinding = ActivityReportBinding.inflate(layoutInflater)
        val color = SurfaceColors.SURFACE_3.getColor(this)
        window.statusBarColor = color
        window.navigationBarColor = color
        mBinding.toolbar.setBackgroundColor(color)
        setContentView(mBinding.root)
        setDefaultDate()
        setGoBackActionBar()

        mBinding.beginDate.setOnClickListener {
            Log.d(TAG, "setOnClickListener")

            var datePickerRange = builder.build()

            datePickerRange.addOnPositiveButtonClickListener { selection ->
                Log.d(TAG, "Timezone ${TimeZone.getDefault().toString()}")
                val calendar = Calendar.getInstance(TimeZone.getDefault())
                calendar.timeInMillis = selection.first

                beginDate = Date(calendar.get(Calendar.YEAR) - 1900,
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH) + 1,0,0,0)

                calendar.timeInMillis = selection.second
                endDate = Date(calendar.get(Calendar.YEAR) - 1900,
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH) + 1,23,59,59)

                setDate(mBinding.beginDate, beginDate!!, endDate!!)
            }
            datePickerRange.show(supportFragmentManager, "DATE_PICKER")
        }

        setStatusType()

        mBinding.button.setOnClickListener { onGenerateReport() }
    }
}