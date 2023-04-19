package com.de.search.view;


import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.de.search.R;
import com.de.search.adapter.DeviceRecycleViewAdapter;
import com.de.search.app.APP;
import com.de.search.base.BaseActivity;
import com.de.search.bean.DeviceBean;
import com.de.search.view.BT_interact.BluetoothInteract;
import com.de.search.view.P2P_interact.WifiDirectInteract;
import com.inuker.bluetooth.library.connect.listener.BluetoothStateListener;
import com.inuker.bluetooth.library.search.SearchRequest;
import com.inuker.bluetooth.library.search.SearchResult;
import com.inuker.bluetooth.library.search.response.SearchResponse;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

//same as the 'FindService' class, here import the inuker bluetoothkit library to implement 'open search' function

public class HomeActivity extends BaseActivity implements DeviceRecycleViewAdapter.DeviceRecycleViewAdapterInterface {

    private TextView tvSet, tvName, tvInfo;
    private SwipeRefreshLayout swipe;

    private LinearLayout l1, l2, l3;

    private RecyclerView mRecycleView;
    private DeviceRecycleViewAdapter mAdapter;//adapter
    private List<DeviceBean> deviceBeanList = new ArrayList<>();

    private boolean open = false;

    private Thread thread;
    private Location location;

    int j = 0;
    // Bluetooth
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
        setView(R.layout.activity_home);
        super.onCreate(savedInstanceState);

    }

    @SuppressLint({"NonConstantResourceId", "SetTextI18n"})
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
        //Create options menu
        toolbar.inflateMenu(R.menu.option_menu_home);
        //Option menu listening
        toolbar.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case R.id.add:
                    switch (APP.getType()) {
                        case 0:
                            APP.mClient.stopSearch();
                            startToActivity(AddBtActivity.class);
                            finish();
                            break;
                        case 1:
                        case 2:
                            break;
                    }
                    finish();

                    return true;
                case R.id.open:
                    if (thread == null) {

                        thread = new Thread(() -> {
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
        });


    }

    @SuppressLint("SetTextI18n")
    @Override
    protected void initData() {


        // Enable Bluetooth scanning
        APP.mClient.registerBluetoothStateListener(mBluetoothStateListener);

        if (APP.mClient.isBluetoothOpened()) {
            scan();
        } else {
            APP.mClient.openBluetooth();
        }


        tvName.setText("Bluetooth List");

        swipe.setEnabled(true);
        swipe.setOnRefreshListener(() -> new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                runOnUiThread(new Runnable() {
                    @SuppressLint("NotifyDataSetChanged")
                    @Override
                    public void run() {
                        deviceBeanList.clear();
                        deviceBeanList.addAll(DeviceBean.find(DeviceBean.class, "type = ?", String.valueOf(APP.getType())));
                        mAdapter.notifyDataSetChanged();

                        swipe.setRefreshing(false);
                    }
                });

            }
        }).start());

        // Check Bluetooth devices in the local database
        deviceBeanList = DeviceBean.find(DeviceBean.class, "type = ?", String.valueOf(APP.getType()));

        //Create a layout manager, set vertically in 'LinearLayoutManager.VERTICAL'; set horizontally in 'LinearLayoutManager.HORIZONTAL'
        LinearLayoutManager mLinearLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        //Create the adapter and pass the data to the adapter
        mAdapter = new DeviceRecycleViewAdapter(deviceBeanList, this);
        //Set up the layout manager
        mRecycleView.setLayoutManager(mLinearLayoutManager);
        //Set adapter
        mRecycleView.setAdapter(mAdapter);

    }

    @Override
    protected void initListener() {

        //l1 is the 'BT' button in the below toolbar, click this will jump to BT interact function, which belong to BluetoothInteract.class
        l1.setOnClickListener(view -> {
            APP.mClient.stopSearch();
            startToActivity(BluetoothInteract.class);
            finish();
        });

        //l2 is the 'P2P' button in the below toolbar, click this will jump to BT interact function, which belong to WifiDirectInteract.class
        l2.setOnClickListener(view -> {
            APP.mClient.stopSearch();
            startToActivity(WifiDirectInteract.class);
            finish();
        });

        //l3 is the 'Introduction' button in the below toolbar, click this will jump to BT interact function, which belong to WifiDirectInteract.class
        l3.setOnClickListener(view -> {
            APP.mClient.stopSearch();
            startToActivity(IntroductionActivity.class);
            finish();
        });

        //the 'set' button oon the top left side
        tvSet.setOnClickListener(view -> {
            APP.mClient.stopSearch();
            startToActivity(SetActivity.class);
            finish();
        });

    }

    @Override
    public void onBackPressed() {
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
                //title
                .setTitle("Delete device")
                //content
                .setMessage("Delete " + deviceBeanList.get(position).getName() + " to my device")
                //icon
                .setIcon(R.mipmap.ic_launcher)
                .setPositiveButton("Delete", (dialogInterface, i) -> {
                    if (deviceBeanList.get(position).delete()) {
                        showToast("Delete succeeded");
                        deviceBeanList.remove(position);
                        mAdapter.notifyDataSetChanged();
                    } else {
                        showToast("Delete failed");
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
                    .searchBluetoothLeDevice(4000, 1) //Scan the BLE device once for 4s each time
                    .searchBluetoothClassicDevice(4000) //Then scan the classic Bluetooth 4s
                    .build();
        }

        APP.mClient.search(searchRequest, new SearchResponse() {
            @Override
            public void onSearchStarted() {

            }

            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onDeviceFounded(SearchResult device) {
                BluetoothDevice bluetoothDevice = device.device;
                j += 1;

                @SuppressLint("MissingPermission") String deviceName = bluetoothDevice.getName();
                String deviceHardwareAddress = bluetoothDevice.getAddress(); // MAC address

                if (!TextUtils.isEmpty(deviceName) && !deviceName.equals("NULL") && !list_device.contains(bluetoothDevice)) {   //Add it to the list
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