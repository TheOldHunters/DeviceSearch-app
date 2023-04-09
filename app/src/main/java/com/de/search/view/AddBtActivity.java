package com.de.search.view;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.de.search.R;
import com.de.search.adapter.AddDeviceRecycleViewAdapter;
import com.de.search.app.APP;
import com.de.search.base.BaseActivity;
import com.de.search.bean.DeviceBean;
import com.inuker.bluetooth.library.connect.listener.BluetoothStateListener;
import com.inuker.bluetooth.library.search.SearchRequest;
import com.inuker.bluetooth.library.search.SearchResult;
import com.inuker.bluetooth.library.search.response.SearchResponse;


import java.util.ArrayList;
import java.util.List;

public class AddBtActivity extends BaseActivity implements AddDeviceRecycleViewAdapter.DeviceRecycleViewAdapterInterface {

    private SwipeRefreshLayout swipe;
    private TextView tvBack, tvName;

    private RecyclerView mRecycleView;
    private AddDeviceRecycleViewAdapter mAdapter;//adapter
    private final List<DeviceBean> deviceBeanList = new ArrayList<>();

    // bluetooth
    private SearchRequest searchRequest;
    // Bluetooth switch
    private final BluetoothStateListener mBluetoothStateListener = new BluetoothStateListener() {
        @Override
        public void onBluetoothStateChanged(boolean openOrClosed) {
            if (openOrClosed) {
                scan();
            }
        }

    };

    // Bluetooth correlation
    ArrayList<BluetoothDevice> list_device = new ArrayList<>(); //Bluetooth device



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setView(R.layout.activity_add);
        super.onCreate(savedInstanceState);

    }

    @Override
    protected void initView() {
        swipe = findViewById(R.id.swipe);
        tvBack = findViewById(R.id.tv_switch);
        tvName = findViewById(R.id.tv_name);
        mRecycleView = findViewById(R.id.rv_list);

    }

    @SuppressLint("SetTextI18n")
    @Override
    protected void initData() {


        tvName.setText("Add Bluetooth");


        swipe.setEnabled(true);
        swipe.setOnRefreshListener(() -> new Thread(() -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            runOnUiThread(() -> {
                if (APP.mClient.isBluetoothOpened()) {
                    scan();
                } else {
                    APP.mClient.openBluetooth();
                }

                swipe.setRefreshing(false);
            });

        }).start());

        //创建布局管理器，垂直设置LinearLayoutManager.VERTICAL，水平设置LinearLayoutManager.HORIZONTAL
        LinearLayoutManager mLinearLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        //创建适配器，将数据传递给适配器
        mAdapter = new AddDeviceRecycleViewAdapter(deviceBeanList, this);
        //设置布局管理器
        mRecycleView.setLayoutManager(mLinearLayoutManager);
        //设置适配器adapter
        mRecycleView.setAdapter(mAdapter);


        // 开启蓝牙扫描
        APP.mClient.registerBluetoothStateListener(mBluetoothStateListener);

        if (APP.mClient.isBluetoothOpened()) {
            scan();
        } else {
            APP.mClient.openBluetooth();
        }


    }

    @Override
    protected void initListener() {
        tvBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startToActivity(HomeActivity.class);
                finish();
            }
        });
    }

    @Override
    public void onLongClick(int position) {

    }

    @Override
    public void onClick(int position) {
        if (deviceBeanList.get(position).getRssi() == 0) {
            showToast("This device does not support rssi");
            return;
        }

        final EditText inputServer = new EditText(this);
        inputServer.setFocusable(true);
        inputServer.setHint("Custom Name(Default Bluetooth name)");


        AlertDialog alertDialog = new AlertDialog.Builder(this)
                .setView(inputServer)
                //标题
                .setTitle("Add device")
                //内容
                .setMessage("Add " + deviceBeanList.get(position).getName() + " to my device")
                //图标
                .setIcon(R.mipmap.ic_launcher)
                .setPositiveButton("confirm", new DialogInterface.OnClickListener() {
                    @SuppressLint("MissingPermission")
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        List<DeviceBean> deviceBeans = DeviceBean.find(DeviceBean.class, "mac = ?", deviceBeanList.get(position).getMac());
                        if (deviceBeans.size() > 0) {
                            showToast("Device already exists");
                        } else {
                            String name = inputServer.getText().toString();
                            if (TextUtils.isEmpty(name)){
                                deviceBeanList.get(position).setMe(1);
                                deviceBeanList.get(position).setUserId(APP.getBluetoothName());
                                deviceBeanList.get(position).setUserName(APP.bluetoothAdapter.getName());

                                deviceBeanList.get(position).save();
                            }else {
                                DeviceBean deviceBean = new DeviceBean();
                                deviceBean.setName(name);
                                deviceBean.setBtType(deviceBeanList.get(position).getBtType());
                                deviceBean.setType(deviceBeanList.get(position).getType());
                                deviceBean.setRssi(deviceBeanList.get(position).getRssi());
                                deviceBean.setMac(deviceBeanList.get(position).getMac());

                                deviceBean.setMe(1);
                                deviceBean.setUserId(APP.getBluetoothName());
                                deviceBean.setUserName(APP.bluetoothAdapter.getName());


                                deviceBean.save();
                            }

                            showToast("Successfully added");
                        }

                    }
                })
                .setNegativeButton("cancel", null)
                .create();
        alertDialog.show();

    }

    @Override
    public void onBackPressed() {
//        super.onBackPressed();
        startToActivity(HomeActivity.class);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        APP.mClient.stopSearch();

    }


    public void scan() {

        list_device.clear();
        deviceBeanList.clear();
        mAdapter.notifyDataSetChanged();

        APP.mClient.stopSearch();

        if (searchRequest == null) {
            searchRequest = new SearchRequest.Builder()
                    .searchBluetoothLeDevice(5000, 1) // 先扫 BLE 设备 1 次，每次 5s
                    .searchBluetoothClassicDevice(5000) // 再扫经典蓝牙 5s
                    .build();
        }
        APP.mClient.search(searchRequest, new SearchResponse() {
            @Override
            public void onSearchStarted() {

            }

            @SuppressLint("MissingPermission")
            @Override
            public void onDeviceFounded(SearchResult device) {
                BluetoothDevice bluetoothDevice = device.device;
                if (device.rssi == 0 || TextUtils.isEmpty(bluetoothDevice.getName()) || bluetoothDevice.getName().equals("NULL") || TextUtils.isEmpty(bluetoothDevice.getAddress()) || list_device.contains(bluetoothDevice)) {   //加入到list中
                    return;
                }

                list_device.add(bluetoothDevice);

                Log.e("getName", bluetoothDevice.getName() + "");
                Log.e("getAddress", bluetoothDevice.getAddress());
                Log.e("getRssi", String.valueOf(device.rssi));

                Log.e("", "------------------------");

                DeviceBean bean = new DeviceBean();
                bean.setName(bluetoothDevice.getName() + "");
                bean.setMac(bluetoothDevice.getAddress());
                bean.setRssi(device.rssi);
                bean.setType(String.valueOf(APP.getType()));
                bean.setBtType("0");

                deviceBeanList.add(bean);
                mAdapter.notifyDataSetChanged();

            }

            @Override
            public void onSearchStopped() {
                Log.e("onSearchStopped","onSearchStopped");

            }

            @Override
            public void onSearchCanceled() {
                Log.e("onSearchCanceled","onSearchCanceled");
            }

        });


    }


}