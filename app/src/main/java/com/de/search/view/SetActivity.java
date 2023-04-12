package com.de.search.view;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.de.search.R;
import com.de.search.app.APP;
import com.de.search.base.BaseActivity;
import com.de.search.util.LocalStorageUtils;

//this class is the 'Set' button on the top left side in 'Home' page, used to edit the reminder distance and the PIN password

public class SetActivity extends BaseActivity {


    private EditText et1, et2;
    private Button bt1;
    private TextView tvBack;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setView(R.layout.activity_set);
        super.onCreate(savedInstanceState);

    }

    @Override
    protected void initView() {


        et1 = findViewById(R.id.et1);
        et2 = findViewById(R.id.et2);
        bt1 = findViewById(R.id.bt1);

        tvBack = findViewById(R.id.tv_back);

    }

    @Override
    protected void initData() {


        et1.setText(String.valueOf(APP.getDistance()));
        et2.setText(String.valueOf(APP.getPin()));

    }

    @Override
    protected void initListener() {

        //'back' button to return 'Home'
        tvBack.setOnClickListener(view -> {
            startToActivity(HomeActivity.class);
            finish();
        });

        //'save' button
        bt1.setOnClickListener(view -> {
            if (TextUtils.isEmpty(et1.getText().toString())){
                showToast("Please enter reminder distance"); //If the correct reminder distance is not entered, this entry is displayed
                return;
            }

            if (TextUtils.isEmpty(et2.getText().toString())){
                showToast("Please enter PIN");//If the correct PIN is not entered, this entry is displayed
                return;
            }

            APP.setDistance(Integer.parseInt(et1.getText().toString()));
            APP.setPin(et2.getText().toString());
            LocalStorageUtils.setParam(SetActivity.this, "distance", APP.getDistance());
            LocalStorageUtils.setParam(SetActivity.this, "pin", APP.getPin());

            showToast("Saved successfully");
        });

    }

    @Override
    public void onBackPressed() {
        startToActivity(HomeActivity.class);
        finish();
    }
}