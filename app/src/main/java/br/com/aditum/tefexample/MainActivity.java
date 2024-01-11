package br.com.aditum.tefexample;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import net.glxn.qrgen.android.QRCode;

import java.io.File;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import br.com.aditum.tef.AditumTefApi;
import br.com.aditum.tef.v1.callback.ResponseCallback;
import br.com.aditum.tef.v1.enums.AcquirerEnum;
import br.com.aditum.tef.v1.enums.InstallmentType;
import br.com.aditum.tef.v1.enums.PaymentType;
import br.com.aditum.tef.v1.enums.QRCodeTransactionStatus;
import br.com.aditum.tef.v1.enums.ReversionReason;
import br.com.aditum.tef.v1.model.BaseResponse;
import br.com.aditum.tef.v1.model.CancelationResponse;
import br.com.aditum.tef.v1.model.Charge;
import br.com.aditum.tef.v1.model.ChargeResponse;
import br.com.aditum.tef.v1.model.ConfirmationResponse;
import br.com.aditum.tef.v1.model.InitRequest;
import br.com.aditum.tef.v1.model.InitResponse;
import br.com.aditum.tef.v1.model.MerchantData;
import br.com.aditum.tef.v1.model.PaymentRequest;
import br.com.aditum.tef.v1.model.PaymentResponse;
import br.com.aditum.tef.v1.model.PaymentScheme;
import br.com.aditum.tef.v1.model.QRCodeCharge;
import br.com.aditum.tef.v1.model.QRCodePaymentRequest;
import br.com.aditum.tef.v1.model.QRCodeResponse;
import br.com.aditum.tef.v1.model.QRCodeTransaction;
import br.com.aditum.tef.v1.model.Terminal;

public class MainActivity extends AppCompatActivity {
    public static final String TAG = "TexExample";
    private AditumTefApi tefApi;
    private String lastPaymentNSU;
    private String lastMerchantID;
    private Terminal pinpad;
    public PaymentType paymentType;
    public InstallmentType installmentType;
    public int installmentNumber;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor prefEditor;
    private HashSet<PaymentType> paymentTypesSet = new HashSet<>();
    private int screenOrientation = 0;
    LinearLayout llMainActivity;

    @Override
    protected void onCreate (Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.tbAditum);
        setSupportActionBar(toolbar);

        screenOrientation = this.getResources().getConfiguration().orientation;
        sharedPreferences = this.getSharedPreferences(getString(R.string.shared_preference_file), Context.MODE_PRIVATE);

        llMainActivity = findViewById(R.id.llMainActivity);
        final ProgressBar pbLoading = findViewById(R.id.pbLoading);
        final Spinner spPayType = findViewById(R.id.spPayType);
        final Spinner spInstType = findViewById(R.id.spInstType);
        final Spinner spInstNumber = findViewById(R.id.spInstNumber);
        final EditText etAmount = findViewById(R.id.etAmount);
        final EditText etActivationCode = findViewById(R.id.etActivationCode);
        final EditText etMerchantChargeId = findViewById(R.id.etMerchantChargeId);
        final Button btInit = findViewById(R.id.btInit);
        final Button btPay = findViewById(R.id.btPay);
        final Button btRevert = findViewById(R.id.btRevert);
        final Button btConfirm = findViewById(R.id.btConfirm);
        final Button btCancel = findViewById(R.id.btCancel);
        final Button btAbort = findViewById(R.id.btAbort);
        final Button btSeekCharge = findViewById(R.id.btSeekCharge);

        ArrayAdapter<InstallmentType> adapterCredType = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, InstallmentType.values());
        ArrayAdapter<CharSequence> adapterInstNumber = ArrayAdapter.createFromResource(this, R.array.installments_numbers, android.R.layout.simple_spinner_item);
        adapterCredType.setDropDownViewResource(androidx.appcompat.R.layout.support_simple_spinner_dropdown_item);
        adapterInstNumber.setDropDownViewResource(androidx.appcompat.R.layout.support_simple_spinner_dropdown_item);
        spInstType.setAdapter(adapterCredType);
        spInstNumber.setAdapter(adapterInstNumber);
        spPayType.setOnItemSelectedListener(new PaymentTypeSelection());
        spInstType.setOnItemSelectedListener(new InstallmentTypeSelection());
        spInstNumber.setOnItemSelectedListener(new InstallmentNumberSelection());

        etAmount.addTextChangedListener(new TextWatcher() {
            private String current = "";

            @Override
            public void onTextChanged (CharSequence s, int start, int before, int count) {
                if(!String.valueOf(s).equals(current)) {
                    etAmount.removeTextChangedListener(this);
                    String cleanString = String.valueOf(s).replaceAll("[\\D]", "");
                    double parsed = Double.parseDouble(cleanString);
                    String formatted = NumberFormat.getCurrencyInstance(new Locale("pt", "BR")).format((parsed / 100));
                    current = formatted;
                    etAmount.setText(formatted);
                    etAmount.setSelection(formatted.length());
                    etAmount.addTextChangedListener(this);
                }
            }

            @Override
            public void afterTextChanged (Editable editable) {
            }

            @Override
            public void beforeTextChanged (CharSequence charSequence, int i, int i1, int i2) {
            }
        });

        btPay.setEnabled(false);
        btRevert.setEnabled(false);
        btConfirm.setEnabled(false);
        btCancel.setEnabled(false);
        btAbort.setEnabled(false);
        btSeekCharge.setEnabled(false);
        etMerchantChargeId.setEnabled(false);
        spPayType.setEnabled(false);
        spInstType.setEnabled(false);
        spInstNumber.setEnabled(false);
        etAmount.setEnabled(false);
        spPayType.setSelection(1);
        pbLoading.setVisibility(View.INVISIBLE);

        this.tefApi = new AditumTefApi(this, message -> {
            Log.d(TAG, "callback onNotification: " + message);
            showNotificationMsg(message);
        });

        // ### Start - Init
        btInit.setOnClickListener(v -> {
            btInit.setEnabled(false);
            etActivationCode.setEnabled(false);
            btPay.setEnabled(false);
            btRevert.setEnabled(false);
            btConfirm.setEnabled(false);
            btCancel.setEnabled(false);
            btAbort.setEnabled(false);
            btSeekCharge.setEnabled(false);
            etMerchantChargeId.setEnabled(false);
            spPayType.setEnabled(false);
            spInstType.setEnabled(false);
            spInstNumber.setEnabled(false);
            etAmount.setEnabled(false);
            spPayType.setSelection(1);

            initApplication(etActivationCode.getText().toString());
        });
        // ### End - Init

        // ### Start - Payment
        btPay.setOnClickListener(v -> {
            btPay.setEnabled(false);
            btConfirm.setEnabled(false);
            btRevert.setEnabled(false);
            btCancel.setEnabled(false);
            btAbort.setEnabled(true);
            btSeekCharge.setEnabled(false);
            etMerchantChargeId.setEnabled(false);

            String amountStr = String.valueOf(etAmount.getText());
            if(amountStr.isEmpty())
                amountStr = "100";
            long paymentAmount = Long.parseLong(amountStr.replaceAll("\\D", ""));
            boolean enableQrCode = paymentType == PaymentType.WALLET || paymentType == PaymentType.PIX;

            Log.d(TAG, "paymentAmount: " + paymentAmount);
            if(!enableQrCode) {
                PaymentRequest paymentRequest = new PaymentRequest();
                paymentRequest.setAmount(paymentAmount);
                paymentRequest.setPaymentType(paymentType);
                paymentRequest.setInstallmentType(installmentType);
                paymentRequest.setInstallmentNumber(installmentNumber);
                paymentRequest.setMerchantChargeId(pinpad.getSerialNumber() + (System.currentTimeMillis() / 1000) + "0000");
                pbLoading.setVisibility(View.VISIBLE);

                this.tefApi.pay(paymentRequest, new ResponseCallback<PaymentResponse>() {
                    @Override
                    public void onFinished (PaymentResponse response) {
                        btPay.setEnabled(true);
                        btAbort.setEnabled(false);
                        btSeekCharge.setEnabled(true);
                        etMerchantChargeId.setEnabled(true);
                        pbLoading.setVisibility(View.GONE);

                        Log.d(TAG, "PAY RESPONSE: " + response.toString());
                        if(Boolean.TRUE.equals(response.getIsApproved())) {
                            btRevert.setEnabled(true);
                            btConfirm.setEnabled(true);
                            btAbort.setEnabled(false);

                            Charge charge = response.getCharge();
                            lastPaymentNSU = Objects.requireNonNull(charge).getNsu();
                            lastMerchantID = paymentRequest.getMerchantChargeId();
                            etMerchantChargeId.setText(lastMerchantID);
                            String receipts = String.join("\n",
                                    Objects.requireNonNull(charge.getMerchantReceipt()))
                                    + "\n---------------------------------------\n"
                                    + String.join("\n", Objects.requireNonNull(charge.getCardholderReceipt()));
                            Log.d(TAG, "Via Lojista: " + String.join("\n", Objects.requireNonNull(charge.getMerchantReceipt())));
                            Log.d(TAG, "Via Client: " + String.join("\n", Objects.requireNonNull(charge.getCardholderReceipt())));
                            showReqResult("Receipts", receipts);
                        } else {
                            showNotificationMsg(Objects.requireNonNull(response));
                        }
                    }
                });
            } else {
                QRCodePaymentRequest request = new QRCodePaymentRequest();
                request.setAmount(paymentAmount);
                request.setPaymentType(paymentType);
                request.setAcquirer(AcquirerEnum.SAFRAPAY);
                request.setScheme("PIX QRCODE");

                Log.d(TAG, "QR CODE: " + request);

                tefApi.qrCodePay(request, new ResponseCallback<QRCodeResponse>() {
                    @Override
                    public void onFinished (QRCodeResponse response) {
                        Log.d(TAG, "QRCODE PAY RESPONSE: " + response.toString());
                        btPay.setEnabled(true);
                        btAbort.setEnabled(false);
                        btSeekCharge.setEnabled(true);
                        etMerchantChargeId.setEnabled(true);
                        pbLoading.setVisibility(View.GONE);

                        QRCodeCharge charge = response.getCharge();

                        if(response.getSuccess()) {
                            btRevert.setEnabled(true);
                            btConfirm.setEnabled(true);
                            btAbort.setEnabled(false);
                            if(charge != null && !charge.getTransactions().isEmpty()) {
                                lastPaymentNSU = Objects.requireNonNull(charge).getNsu();
                                QRCodeTransaction transaction = charge.getTransactions().get(0);
                                if(transaction.getTransactionStatus() == QRCodeTransactionStatus.PAID) {
                                    showNotificationMsg("Transaction paid successfully");
                                } else {
                                    showNotificationMsg("QRCode transaction not paid!");
                                }
                            } else {
                                showNotificationMsg("Some error occurred!");
                            }
                        } else {
                            Log.e(TAG, "Error: " + Objects.requireNonNull(response.getErrors()));
                            showNotificationMsg(Objects.requireNonNull(response));
                        }
                        ((ImageView)findViewById(R.id.ivQrCode)).setImageResource(R.drawable.qr_code);
                    }

                    @Override
                    public void onProgress (String qrCodeString) {
                        Log.d(TAG, "QRCode String generated for Pay: " + qrCodeString);
                        Bitmap bitmap = QRCode
                                .from(qrCodeString)
                                .withSize(250, 250)
                                .withColor(0xFF48297C, 0xFFFAFAFA)
                                .bitmap();

                        ((ImageView)findViewById(R.id.ivQrCode)).setImageBitmap(bitmap);
                    }
                });
            }
        });
        // ### End - Payment

        // ### Start - Confirm
        btConfirm.setOnClickListener(view -> {
            btPay.setEnabled(false);
            btRevert.setEnabled(false);
            btConfirm.setEnabled(false);
            btCancel.setEnabled(false);
            // btQrCodePay.setEnabled(false);
            // btQrCodeCancel.setEnabled(false);
            btAbort.setEnabled(true);
            btSeekCharge.setEnabled(false);
            etMerchantChargeId.setEnabled(false);
            pbLoading.setVisibility(View.VISIBLE);

            tefApi.confirm(lastPaymentNSU, new ResponseCallback<ConfirmationResponse>() {
                @Override
                public void onFinished (ConfirmationResponse response) {
                    btPay.setEnabled(true);
                    // btQrCodePay.setEnabled(true);
                    btAbort.setEnabled(false);
                    btSeekCharge.setEnabled(true);
                    etMerchantChargeId.setEnabled(true);
                    pbLoading.setVisibility(View.GONE);

                    Log.d(TAG, "CONFIRM RESPONSE: " + response.toString());
                    if(response.getSuccess()) {
                        btCancel.setEnabled(true);
                        showNotificationMsg("TRANSAÇÃO CONFIRMADA");
                    } else {
                        btConfirm.setEnabled(true);
                        btRevert.setEnabled(true);
                        showNotificationMsg(Objects.requireNonNull(response));
                    }
                }
            });
        });
        // ### End - Confirm

        // ### Start - Revert
        btRevert.setOnClickListener(view -> {
            btPay.setEnabled(false);
            btRevert.setEnabled(false);
            btConfirm.setEnabled(false);
            btCancel.setEnabled(false);
            // btQrCodePay.setEnabled(false);
            // btQrCodeCancel.setEnabled(false);
            btAbort.setEnabled(true);
            btSeekCharge.setEnabled(false);
            etMerchantChargeId.setEnabled(false);
            pbLoading.setVisibility(View.VISIBLE);

            tefApi.revert(lastPaymentNSU, ReversionReason.TEF_TIMEOUT, new ResponseCallback<CancelationResponse>() {
                @Override
                public void onFinished (CancelationResponse response) {
                    btPay.setEnabled(true);
                    // btQrCodePay.setEnabled(true);
                    btAbort.setEnabled(false);
                    btSeekCharge.setEnabled(true);
                    etMerchantChargeId.setEnabled(true);
                    pbLoading.setVisibility(View.GONE);

                    Log.d(TAG, "REVERT RESPONSE: " + response.toString());
                    if(!response.getSuccess()) {
                        btConfirm.setEnabled(true);
                        btRevert.setEnabled(true);
                        showNotificationMsg(Objects.requireNonNull(response));
                    }
                }
            });
        });
        // ### End - Revert

        // ### Start - Cancel
        btCancel.setOnClickListener(view -> {
            btPay.setEnabled(false);
            btRevert.setEnabled(false);
            btConfirm.setEnabled(false);
            btCancel.setEnabled(false);
            // btQrCodePay.setEnabled(false);
            // btQrCodeCancel.setEnabled(false);
            btAbort.setEnabled(true);
            btSeekCharge.setEnabled(false);
            etMerchantChargeId.setEnabled(false);
            pbLoading.setVisibility(View.VISIBLE);

            PaymentType cancelType = Objects.requireNonNull(tefApi.getChargeByMerchantChargeID(lastMerchantID).getCharge()).getPaymentType();

            if(cancelType != PaymentType.PIX && cancelType != PaymentType.WALLET) {
                tefApi.cancel(lastPaymentNSU, new ResponseCallback<CancelationResponse>() {
                    @Override
                    public void onFinished (CancelationResponse response) {
                        btPay.setEnabled(true);
                        // btQrCodePay.setEnabled(true);
                        btSeekCharge.setEnabled(true);
                        etMerchantChargeId.setEnabled(true);
                        pbLoading.setVisibility(View.GONE);

                        Log.d(TAG, "CANCEL RESPONSE: " + response.toString());
                        if(!response.getSuccess()) {
                            btConfirm.setEnabled(true);
                            btRevert.setEnabled(true);
                            btCancel.setEnabled(true);
                            showNotificationMsg(Objects.requireNonNull(response));
                        }else{
                            Charge charge = Objects.requireNonNull(response.getCharge());
                            String receipts = String.join("\n",
                                    Objects.requireNonNull(charge.getMerchantReceipt()))
                                    + "\n---------------------------------------\n"
                                    + String.join("\n", Objects.requireNonNull(charge.getCardholderReceipt()));
                            Log.d(TAG, "Via Lojista: " + String.join("\n", Objects.requireNonNull(charge.getMerchantReceipt())));
                            Log.d(TAG, "Via Client: " + String.join("\n", Objects.requireNonNull(charge.getCardholderReceipt())));
                            showReqResult("Receipts", receipts);
                        }
                    }
                });
            } else {
                tefApi.qrCodeCancel(lastPaymentNSU, new ResponseCallback<QRCodeResponse>() {
                    @Override
                    public void onFinished (QRCodeResponse response) {
                        Log.d(TAG, "response: " + response.toString());
                        btPay.setEnabled(true);
                        // btQrCodePay.setEnabled(true);
                        btSeekCharge.setEnabled(true);
                        etMerchantChargeId.setEnabled(true);
                        pbLoading.setVisibility(View.GONE);

                        if(response.getSuccess()) {
                            QRCodeCharge charge = response.getCharge();
                            if(charge != null && !charge.getTransactions().isEmpty()) {
                                QRCodeTransaction transaction = charge.getTransactions().get(0);
                                if(transaction.getTransactionStatus() == QRCodeTransactionStatus.CANCELED) {
                                    showNotificationMsg("Transaction canceled with success!");
                                    return;
                                }
                                // btQrCodeCancel.setEnabled(true);
                                showNotificationMsg("QRCode transaction not canceled!");
                            } else {
                                showNotificationMsg("Some error occurred!");
                            }
                        } else {
                            btConfirm.setEnabled(true);
                            btRevert.setEnabled(true);
                            btCancel.setEnabled(true);
                            showNotificationMsg(Objects.requireNonNull(response));
                            Log.e(TAG, "Error: " + Objects.requireNonNull(response.getErrors()));
                            showNotificationMsg(Objects.requireNonNull(response));
                        }
                    }

                    @Override
                    public void onProgress (String qrCodeString) {
                        Log.d(TAG, "QRCode String generated for Cancelation: " + qrCodeString);
                        showNotificationMsg(qrCodeString);
                    }
                });
            }
        });
        // ### End - Cancel

        // ### Start - Get Charge
        btSeekCharge.setOnClickListener(view -> {
            pbLoading.setVisibility(View.VISIBLE);
            String chargeId = String.valueOf(etMerchantChargeId.getText());
            ChargeResponse chargeResp = tefApi.getChargeByMerchantChargeID(chargeId);
            if(chargeResp.getCharge() != null) {
                lastPaymentNSU = chargeResp.getCharge().getNsu();
                Log.d("SEEK CHARGE RESPONSE", chargeResp.toString());
                showReqResult(chargeId, chargeResp.toString());
            }
            pbLoading.setVisibility(View.GONE);
        });
        // ### End -Get Charge

        // ### Start - Abort
        btAbort.setOnClickListener(view -> tefApi.abort());
        // ### End - Abort
    }

    @Override
    protected void onStart () {
        super.onStart();
        String act = sharedPreferences.getString(getString(R.string.saved_activation_code_key), null);
        initApplication(act);
    }

    public void initApplication (String activationCode) {
        if(activationCode != null && activationCode.length() == 9) {
            new android.os.Handler(Looper.getMainLooper()).postDelayed(
                    () -> {
                        findViewById(R.id.btInit).setEnabled(false);
                        findViewById(R.id.etActivationCode).setEnabled(false);
                        ((TextView)findViewById(R.id.etActivationCode)).setText(activationCode);

                        InitRequest initRequest = new InitRequest();
                        initRequest.setApplicationToken("12345678910");
                        initRequest.setApplicationName("TefExample");
                        initRequest.setApplicationVersion("1.0.0");
                        initRequest.setActivationCode(activationCode);
                        Log.d(TAG, "Create InitRequest");
                        ((ProgressBar)findViewById(R.id.pbLoading)).setVisibility(View.VISIBLE);

                        tefApi.init(initRequest, new ResponseCallback<InitResponse>() {
                            @Override
                            public void onFinished (InitResponse response) {
                                Log.d(TAG, "INIT RESPONSE: " + response.toString());
                                ((ProgressBar)findViewById(R.id.pbLoading)).setVisibility(View.GONE);
                                if(response.getSuccess()) {
                                    // Success
                                    Log.d(TAG, "Init Success!");
                                    findViewById(R.id.btInit).setEnabled(false);
                                    findViewById(R.id.btPay).setEnabled(true);
                                    findViewById(R.id.btAbort).setEnabled(true);
                                    findViewById(R.id.btSeekCharge).setEnabled(true);
                                    findViewById(R.id.etActivationCode).setEnabled(false);
                                    findViewById(R.id.etAmount).setEnabled(true);
                                    findViewById(R.id.spPayType).setEnabled(true);
                                    findViewById(R.id.spPayType).setEnabled(true);

                                    MerchantData merchantData = tefApi.getMerchantData();
                                    pinpad = response.getTerminalInfo();

                                    for(PaymentScheme scheme : merchantData.getPaymentScheme()) {
                                        paymentTypesSet.add(scheme.getPaymentType());
                                    }
                                    List<PaymentType> paymentTypesList = new ArrayList<>(paymentTypesSet);
                                    Collections.sort(paymentTypesList);
                                    ArrayAdapter<PaymentType> adapterPayType = new ArrayAdapter<>(getApplicationContext(), android.R.layout.simple_spinner_item, paymentTypesList);
                                    adapterPayType.setDropDownViewResource(androidx.appcompat.R.layout.support_simple_spinner_dropdown_item);
                                    ((Spinner)findViewById(R.id.spPayType)).setAdapter(adapterPayType);

                                    setCustomerLabel(merchantData.getFantasyName());
                                    ((TextView)findViewById(R.id.btInit)).setText("ACTIVE");
                                    File imgFile = new File(merchantData.getCustomerLogoPath());
                                    if(imgFile.exists()) {
                                        Bitmap imgBmp = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                                        ((ImageView)findViewById(R.id.ivCustomerIco)).setImageBitmap(imgBmp);
                                    }
                                    showNotificationMsg("TERMINAL INICIALIZADO");

                                    prefEditor = sharedPreferences.edit();
                                    prefEditor.putString(getString(R.string.saved_activation_code_key), activationCode);
                                    prefEditor.apply();

                                } else {
                                    findViewById(R.id.btInit).setEnabled(true);
                                    findViewById(R.id.etActivationCode).setEnabled(true);
                                    showNotificationMsg(Objects.requireNonNull(response));
                                }
                            }
                        });
                    },
                    300);
        }
    }

    private void showReqResult (String winTitle, String winContent) {
        if(screenOrientation == Configuration.ORIENTATION_LANDSCAPE) {
            ((TextView)findViewById(R.id.tvResult)).setText(winContent);
            return;
        }
        View qrcodeLayout = View.inflate(this, R.layout.result_popup_layout, null);
        TextView tvResultTitle = qrcodeLayout.findViewById(R.id.tvResultTitle);
        TextView tvResult = qrcodeLayout.findViewById(R.id.tvResult);

        RelativeLayout rlQrcodeWindow = qrcodeLayout.findViewById(R.id.rlQrcodeWindow);
        LinearLayout llClosePopup = qrcodeLayout.findViewById(R.id.llClosePopup);
        int width = LinearLayout.LayoutParams.MATCH_PARENT;
        int height = LinearLayout.LayoutParams.MATCH_PARENT;
        PopupWindow qrCodeWindow = new PopupWindow(qrcodeLayout, width, height, false);

        qrCodeWindow.showAtLocation(llMainActivity, Gravity.CENTER, 0, 0);
        View.OnClickListener closeOnClick = view -> qrCodeWindow.dismiss();

        tvResultTitle.setText(winTitle);
        tvResult.setText(winContent);

        rlQrcodeWindow.setOnClickListener(closeOnClick);
        llClosePopup.setOnClickListener(closeOnClick);
    }


    private void setCustomerLabel (String customerName) {
        int nameLengthLimit = screenOrientation == Configuration.ORIENTATION_LANDSCAPE ? 30 : 15;
        if(customerName.length() > nameLengthLimit) {
            customerName = customerName.substring(0, 12) + "...";
        }
        ((TextView)findViewById(R.id.tvCustomerName)).setText(customerName);
    }

    private void showNotificationMsg (@NonNull String notification) {
        Log.d(TAG, "onNotification: " + notification);
        ((TextView)findViewById(R.id.tvMessage)).setText(notification.replaceAll("[\\s\\r\\n]+", " "));
    }

    private void showNotificationMsg (@NonNull BaseResponse baseResponse) {
        if(baseResponse.getErrors() != null && !baseResponse.getErrors().isEmpty()) {
            int respCode = Objects.requireNonNull(baseResponse.getErrors().get(0).getCode());
            String respMsg = baseResponse.getErrors().get(0).getMessage();
            String notification = (respCode + " " + respMsg).replaceAll("[\\s\\r\\n]+", " ");
            ((TextView)findViewById(R.id.tvMessage)).setText(notification);
        }
    }

    @Override
    public void onDestroy () {
        this.tefApi.stopService();
        super.onDestroy();
    }

    class PaymentTypeSelection implements AdapterView.OnItemSelectedListener {
        final Spinner spPayType = findViewById(R.id.spPayType);
        final Spinner spInstType = findViewById(R.id.spInstType);

        @Override
        public void onItemSelected (AdapterView<?> adapterView, View view, int i, long l) {
            paymentType = (PaymentType)adapterView.getItemAtPosition(i);
            if(paymentType != PaymentType.CREDIT) {
                spInstType.setEnabled(false);
                spInstType.setSelection(0);
            } else {
                spInstType.setEnabled(true);
            }
        }

        @Override
        public void onNothingSelected (AdapterView<?> adapterView) {
            spPayType.setEnabled(false);
        }
    }

    class InstallmentTypeSelection implements AdapterView.OnItemSelectedListener {

        final Spinner spInstType = findViewById(R.id.spInstType);
        final Spinner spInstNumber = findViewById(R.id.spInstNumber);

        @Override
        public void onItemSelected (AdapterView<?> adapterView, View view, int i, long l) {
            installmentType = (InstallmentType)adapterView.getItemAtPosition(i);
            if(installmentType == InstallmentType.NONE) {
                spInstNumber.setEnabled(false);
                spInstNumber.setSelection(0);
                installmentNumber = 1;
            } else {
                spInstNumber.setEnabled(true);
            }
        }

        @Override
        public void onNothingSelected (AdapterView<?> adapterView) {
            spInstType.setEnabled(false);
        }
    }

    class InstallmentNumberSelection implements AdapterView.OnItemSelectedListener {
        final Spinner spInstNumber = findViewById(R.id.spInstNumber);

        @Override
        public void onItemSelected (AdapterView<?> adapterView, View view, int i, long l) {
            installmentNumber = i + 1;
        }

        @Override
        public void onNothingSelected (AdapterView<?> adapterView) {
            spInstNumber.setEnabled(false);
        }
    }
}
