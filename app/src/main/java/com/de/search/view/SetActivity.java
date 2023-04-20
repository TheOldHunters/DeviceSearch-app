package com.de.search.view;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import com.de.search.R;
import com.de.search.app.APP;
import com.de.search.base.BaseActivity;
import com.de.search.util.LocalStorageUtils;

import java.util.ArrayList;
import java.util.List;

//this class is the 'Set' button on the top left side in 'Home' page, used to edit the reminder distance and the PIN password

public class SetActivity extends BaseActivity {


    private EditText et1, et2, etTime;
    private Button bt1;
    private TextView tvBack;
    private Switch sVibration;
    private Spinner sAlgorithm;

    private List dataList;
    private ArrayAdapter adapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setView(R.layout.activity_set);
        super.onCreate(savedInstanceState);

    }

    @Override
    protected void initView() {


        et1 = findViewById(R.id.et1);
        et2 = findViewById(R.id.et2);
        etTime = findViewById(R.id.et_time);
        bt1 = findViewById(R.id.bt1);

        tvBack = findViewById(R.id.tv_back);
        sVibration = findViewById(R.id.s_vibration);
        sAlgorithm = findViewById(R.id.s_algorithm);

    }

    @Override
    protected void initData() {


        et1.setText(String.valueOf(APP.getDistance()));
        et2.setText(String.valueOf(APP.getPin()));
        etTime.setText(String.valueOf(APP.time));

        sVibration.setChecked(APP.isOpenVibrator());


        //Choose different algorithms in set
        dataList = new ArrayList();
        dataList.add("Basic RSSI algorithms");
        dataList.add("Kalman filter RSSI");
        dataList.add("Recursive Moving Average Filter RSSI");


        adapter = new ArrayAdapter(this,android.R.layout.simple_spinner_item,dataList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sAlgorithm.setAdapter(adapter);

        sAlgorithm.setSelection(APP.algorithm);
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
            APP.setOpenVibrator(sVibration.isChecked());
            APP.setPin(et2.getText().toString());
            APP.algorithm = sAlgorithm.getSelectedItemPosition();
            APP.time = Integer.parseInt(etTime.getText().toString());
            LocalStorageUtils.setParam(SetActivity.this, "distance", APP.getDistance());
            LocalStorageUtils.setParam(SetActivity.this, "time", APP.time);
            LocalStorageUtils.setParam(SetActivity.this, "pin", APP.getPin());
            LocalStorageUtils.setParam(SetActivity.this, "openVibrator", sVibration.isChecked());
            LocalStorageUtils.setParam(SetActivity.this, "algorithm", sAlgorithm.getSelectedItemPosition());

            showToast("Saved successfully");
        });

    }

    @Override
    public void onBackPressed() {
        startToActivity(HomeActivity.class);
        finish();
    }
}