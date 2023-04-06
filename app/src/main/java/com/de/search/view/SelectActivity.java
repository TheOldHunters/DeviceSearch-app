package com.de.search.view;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
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

public class SelectActivity extends BaseActivity {

    private TextView tvBack, tvName, tvInfo;
    private SwipeRefreshLayout swipe;
    private Button bt;

    private RecyclerView mRecycleView;
    private SelectDeviceRecycleViewAdapter mAdapter;//适配器
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

    @Override
    protected void initData() {

        swipe.setEnabled(true);
        swipe.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                deviceBeanList.clear();
                                deviceBeanList.addAll(DeviceBean.find(DeviceBean.class, "type = ?", String.valueOf(APP.getType())));
                                mAdapter.notifyDataSetChanged();

                                swipe.setRefreshing(false);
                            }
                        });

                    }
                }).start();

            }
        });

        // 查本地数据库的蓝牙设备
        deviceBeanList = DeviceBean.find(DeviceBean.class, "type = ?", String.valueOf(APP.getType()));

        //创建布局管理器，垂直设置LinearLayoutManager.VERTICAL，水平设置LinearLayoutManager.HORIZONTAL
        LinearLayoutManager mLinearLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        //创建适配器，将数据传递给适配器
        mAdapter = new SelectDeviceRecycleViewAdapter(deviceBeanList);
        //设置布局管理器
        mRecycleView.setLayoutManager(mLinearLayoutManager);
        //设置适配器adapter
        mRecycleView.setAdapter(mAdapter);



    }

    @Override
    protected void initListener() {

        tvBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        bt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                List<DeviceBean> deviceBeans = mAdapter.getDeviceBeanList();
                if (deviceBeans.size() == 0){
                    Toast.makeText(SelectActivity.this, "No choice", Toast.LENGTH_SHORT).show();
                    return;
                }

                String data = JSONObject.toJSONString(deviceBeans);
                Intent intent = new Intent();
                intent.putExtra("data", data);  // 数据
                setResult(Activity.RESULT_OK, intent);
                finish();
            }
        });


    }

    @Override
    protected void onDestroy() {
        super.onDestroy();



    }



}