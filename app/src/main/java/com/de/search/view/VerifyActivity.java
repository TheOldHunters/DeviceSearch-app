package com.de.search.view;

import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;

import com.de.search.R;
import com.de.search.app.APP;
import com.de.search.base.BaseActivity;
import com.de.search.util.LocalStorageUtils;

import java.util.concurrent.Executor;

public class VerifyActivity extends BaseActivity {

    private EditText etPin;
    private ImageView iv;
    private Button btDefine, btUnlocking;


    private String pin = "";

    private BiometricManager manager;

    private Handler verifyHandler;
    private Executor executor;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setView(R.layout.activity_verify);
        super.onCreate(savedInstanceState);

    }

    @Override
    protected void initView() {
        etPin = findViewById(R.id.et_pin);
        iv = findViewById(R.id.iv);
        btDefine = findViewById(R.id.bt_define);
        btUnlocking = findViewById(R.id.bt_unlocking);
    }

    @Override
    protected void initData() {
        manager = BiometricManager.from(VerifyActivity.this);

        verifyHandler = new Handler();

        executor = new Executor() {
            @Override
            public void execute(Runnable command) {
                verifyHandler.post(command);
            }
        };


        pin = (String) LocalStorageUtils.getParam(this, "pin", "");

        if (TextUtils.isEmpty(pin)) {
            showToast("未设PIN");
            openDialog();
        }


    }

    @Override
    protected void initListener() {

        btDefine.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (TextUtils.isEmpty(etPin.getText().toString())) {
                    showToast("PIN is null");
                    return;
                }

                if (!etPin.getText().toString().equals(pin)) {
                    showToast("PIN incorrect");
                    return;
                }

                APP.pin = pin;

                startToActivity(HomeActivity.class);
                finish();
            }
        });

        btUnlocking.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int type = manager.canAuthenticate();
                if (type == BiometricManager.BIOMETRIC_SUCCESS) {
                    verify();
                } else if (type == BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE) {
                    showToast("No biometric features are available on this device");
                } else if (type == BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE) {
                    showToast("The biometric feature is currently unavailable");
                } else {
                    showToast("The user did not enter biometric data");
                }

            }
        });


    }

    public void openDialog() {
        final EditText inputServer = new EditText(this);
        inputServer.setFocusable(true);
        inputServer.setHint("PIN");

        AlertDialog alertDialog = new AlertDialog.Builder(this)
                .setView(inputServer)
                //标题
                .setTitle("set PIN")
                //内容
                .setMessage("Please enter your PIN")
                //图标
                .setIcon(R.mipmap.ic_launcher)
                .setPositiveButton("confirm", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        String pin = inputServer.getText().toString();
                        if (TextUtils.isEmpty(pin)) {
                            showToast("Please enter your PIN");
                            openDialog();
                        } else {
                            LocalStorageUtils.setParam(VerifyActivity.this, "pin", pin);
                            VerifyActivity.this.pin = pin;
                        }
                    }
                })
                .setNegativeButton("cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        openDialog();
                    }
                })
                .create();
        alertDialog.setCanceledOnTouchOutside(false);
        alertDialog.show();
    }

    private void verify() {
        BiometricPrompt.PromptInfo build =
                new BiometricPrompt.PromptInfo.Builder()
                        .setTitle("authentication")
                        .setSubtitle("Sign in using your biometric authentication")
                        .setDeviceCredentialAllowed(true)
                        .build();

        BiometricPrompt prompt = new BiometricPrompt(VerifyActivity.this,
                executor, new BiometricPrompt.AuthenticationCallback() {

            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence charSequence) {
                super.onAuthenticationError(errorCode, charSequence);
                showToast("verify error: " + charSequence.toString());

            }

            //成功
            @Override
            public void onAuthenticationSucceeded(
                    @NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);

                startToActivity(HomeActivity.class);
                finish();
            }

            //失败
            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                showToast("verify failed");
            }
        });

        prompt.authenticate(build);
    }

}