package com.de.search.view;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.alibaba.fastjson.JSONObject;
import com.de.search.R;
import com.de.search.adapter.SelectDeviceRecycleViewAdapter;
import com.de.search.app.APP;
import com.de.search.base.BaseActivity;
import com.de.search.bean.DeviceBean;

import java.util.ArrayList;
import java.util.List;

//this class is the activity that allows user to select their preferred devices that they want their friend to help them find, then send to their friend's phone

public class SelectActivity extends BaseActivity {

    private TextView tvBack, tvName, tvInfo;
    private SwipeRefreshLayout swipe;
    private Button bt;

    private RecyclerView mRecycleView;
    private SelectDeviceRecycleViewAdapter mAdapter;//adapter
    private List<DeviceBean> deviceBeanList = new ArrayList<>();




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setView(R.layout.activity_select);
        super.onCreate(savedInstanceState);

    }


    @Override
    protected void initView() {
        swipe = findViewById(R.id.swipe);
        tvBack = findViewById(R.id.tv_back);
        tvName = findViewById(R.id.tv_name);
        tvInfo = findViewById(R.id.tv_info);
        mRecycleView = findViewById(R.id.rv_list);
        bt = findViewById(R.id.bt);


    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    protected void initData() {

        swipe.setEnabled(true);
        swipe.setOnRefreshListener(() -> new Thread(() -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            runOnUiThread(() -> {
                deviceBeanList.clear();
                deviceBeanList.addAll(DeviceBean.find(DeviceBean.class, "type = ?", String.valueOf(APP.getType())));
                mAdapter.notifyDataSetChanged();

                swipe.setRefreshing(false);
            });

        }).start());

        //Check Bluetooth devices in the local database
        deviceBeanList = DeviceBean.find(DeviceBean.class, "type = ?", String.valueOf(APP.getType()));

        //Create a layout manager, set vertically in 'LinearLayoutManager.VERTICAL'; set horizontally in 'LinearLayoutManager.HORIZONTAL'
        LinearLayoutManager mLinearLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        //Create the adapter and pass the data to the adapter
        mAdapter = new SelectDeviceRecycleViewAdapter(deviceBeanList);
        //Set up the layout manager
        mRecycleView.setLayoutManager(mLinearLayoutManager);
        //Setup adapter
        mRecycleView.setAdapter(mAdapter);
    }

    @Override
    protected void initListener() {

        tvBack.setOnClickListener(view -> finish());

        bt.setOnClickListener(view -> {
            List<DeviceBean> deviceBeans = mAdapter.getDeviceBeanList();
            if (deviceBeans.size() == 0){
                Toast.makeText(SelectActivity.this, "No choice", Toast.LENGTH_SHORT).show();
                return;
            }

            String data = JSONObject.toJSONString(deviceBeans);
            Intent intent = new Intent();
            intent.putExtra("data", data);  //data
            setResult(Activity.RESULT_OK, intent);
            finish();
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}