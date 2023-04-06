package com.de.search.view;


import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.de.search.R;
import com.de.search.adapter.DeviceRecycleViewAdapter;
import com.de.search.app.APP;
import com.de.search.base.BaseActivity;
import com.de.search.bean.DeviceBean;
import com.inuker.bluetooth.library.connect.listener.BluetoothStateListener;
import com.inuker.bluetooth.library.search.SearchRequest;
import com.inuker.bluetooth.library.search.SearchResult;
import com.inuker.bluetooth.library.search.response.SearchResponse;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class HomeActivity extends BaseActivity implements DeviceRecycleViewAdapter.DeviceRecycleViewAdapterInterface {

    private TextView tvSet, tvName, tvInfo;
    private SwipeRefreshLayout swipe;

    private LinearLayout l1, l2, l3;

    private RecyclerView mRecycleView;
    private DeviceRecycleViewAdapter mAdapter;//适配器
    private List<DeviceBean> deviceBeanList = new ArrayList<>();

    private boolean open = false;

    private Thread thread;
    private Location location;

    int j = 0;
    // 蓝牙
    private SearchRequest searchRequest;
    // 蓝牙开关
    private final BluetoothStateListener mBluetoothStateListener = new BluetoothStateListener() {
        @Override
        public void onBluetoothStateChanged(boolean openOrClosed) {
            if (openOrClosed) {
                scan();
            }
        }

    };

    // 蓝牙相关
    ArrayList<BluetoothDevice> list_device = new ArrayList<>(); //蓝牙设备


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setView(R.layout.activity_home);
        super.onCreate(savedInstanceState);

    }

    @Override
    protected void initView() {
        swipe = findViewById(R.id.swipe);
        tvSet = findViewById(R.id.tv_set);
        tvName = findViewById(R.id.tv_name);
        tvInfo = findViewById(R.id.tv_info);
        mRecycleView = findViewById(R.id.rv_list);

        l1 = findViewById(R.id.ll1);
        l2 = findViewById(R.id.ll2);
        l3 = findViewById(R.id.ll3);

        Toolbar toolbar = findViewById(R.id.toolbar);
        //创建选项菜单
        toolbar.inflateMenu(R.menu.option_menu_home);
        //选项菜单监听
        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.add:
                        switch (APP.getType()) {
                            case 0:
                                APP.mClient.stopSearch();
                                startToActivity(AddBtActivity.class);
                                finish();
                                break;
                            case 1:

                                break;
                            case 2:

                                break;
                        }
                        finish();

                        return true;
                    case R.id.open:
                        if (thread == null) {

                            thread = new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    while (open) {
                                        Location location = APP.getLastKnownLocation();
                                        if (location != null) {
                                            HomeActivity.this.location = location;
                                        }

                                        scan();

                                        try {
                                            Thread.sleep(12000);
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                            return;
                                        }
                                    }
                                }
                            });
                            tvInfo.setText("open");
                            open = true;

                            thread.start();
                        }

                        return true;
                    case R.id.close:
                        tvInfo.setText("close");
                        open = false;

                        if (thread != null && thread.isAlive()) {
                            thread.interrupt();
                            thread = null;
                        }

                        return true;

                }
                return false;
            }
        });


    }

    @Override
    protected void initData() {


        // 开启蓝牙扫描
        APP.mClient.registerBluetoothStateListener(mBluetoothStateListener);

        if (APP.mClient.isBluetoothOpened()) {
            scan();
        } else {
            APP.mClient.openBluetooth();
        }


        tvName.setText("Bluetooth List");

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
        mAdapter = new DeviceRecycleViewAdapter(deviceBeanList, this);
        //设置布局管理器
        mRecycleView.setLayoutManager(mLinearLayoutManager);
        //设置适配器adapter
        mRecycleView.setAdapter(mAdapter);

//        Log.e("mac", APP.getBluetoothName());

    }

    @Override
    protected void initListener() {

        l1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                APP.mClient.stopSearch();
                startToActivity(BluetoothInteract.class);
                finish();
            }
        });

        l2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                APP.mClient.stopSearch();
                startToActivity(DirectInteract.class);
                finish();
            }
        });

        l3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                APP.mClient.stopSearch();
                startToActivity(ExplainActivity.class);
                finish();
            }
        });


        tvSet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                APP.mClient.stopSearch();
                startToActivity(SetActivity.class);
                finish();
            }
        });

    }

    @Override
    public void onBackPressed() {
//        super.onBackPressed();
        APP.mClient.stopSearch();

        open = false;
        if (thread != null && thread.isAlive()) {
            thread.interrupt();
            thread = null;
        }

        System.exit(0);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        open = false;
        if (thread != null && thread.isAlive()) {
            thread.interrupt();
            thread = null;
        }




    }

    @Override
    public void onLongClick(int position) {
        AlertDialog alertDialog = new AlertDialog.Builder(this)
                //标题
                .setTitle("Delete device")
                //内容
                .setMessage("Delete " + deviceBeanList.get(position).getName() + " to my device")
                //图标
                .setIcon(R.mipmap.ic_launcher)
                .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if (deviceBeanList.get(position).delete()) {
                            showToast("Delete succeeded");
                            deviceBeanList.remove(position);
                            mAdapter.notifyDataSetChanged();
                        } else {
                            showToast("Delete failed");
                        }

                    }
                })
                .setNegativeButton("Cancel", null)
                .create();
        alertDialog.show();
    }

    @Override
    public void onClick(int position) {
        Intent intent = new Intent(HomeActivity.this, FindActivity.class);
        intent.putExtra("name", deviceBeanList.get(position).getName());
        intent.putExtra("mac", deviceBeanList.get(position).getMac());
        intent.putExtra("type", Integer.parseInt(deviceBeanList.get(position).getType()));
        intent.putExtra("btType", Integer.parseInt(deviceBeanList.get(position).getBtType()));

        intent.putExtra("id", deviceBeanList.get(position).getId());
        intent.putExtra("data", deviceBeanList.get(position));
        APP.mClient.stopSearch();
        startToActivity(intent);
        finish();
    }

    public void scan() {
        if (!open) {
            return;
        }

        list_device.clear();

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

            @Override
            public void onDeviceFounded(SearchResult device) {
                BluetoothDevice bluetoothDevice = device.device;
                j += 1;

                String deviceName = bluetoothDevice.getName();
                String deviceHardwareAddress = bluetoothDevice.getAddress(); // MAC address

                if (!TextUtils.isEmpty(deviceName) && !deviceName.equals("NULL") && !list_device.contains(bluetoothDevice)) {   //加入到list中
                    list_device.add(bluetoothDevice);
                } else {
                    return;
                }


                for (int i = 0; i < deviceBeanList.size(); i++) {
                    if (deviceHardwareAddress.equals(deviceBeanList.get(i).getMac())) {
                        deviceBeanList.get(i).setRssi(device.rssi);
                        deviceBeanList.get(i).setFind(1);
                        deviceBeanList.get(i).setFindTime(APP.formatter.format(new Date(System.currentTimeMillis())));
                        if (location != null) {
                            deviceBeanList.get(i).setLongitude(location.getLongitude() + "");
                            deviceBeanList.get(i).setLatitude(location.getLatitude() + "");
                        }

                        deviceBeanList.get(i).save();
                        mAdapter.notifyDataSetChanged();

                        return;
                    }
                }


            }

            @Override
            public void onSearchStopped() {
                Log.e("onSearchStopped", "onSearchStopped");

            }

            @Override
            public void onSearchCanceled() {
                Log.e("onSearchCanceled", "onSearchCanceled");
            }

        });

    }


}