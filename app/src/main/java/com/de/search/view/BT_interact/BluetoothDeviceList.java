package com.de.search.view.BT_interact;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.de.search.R;
import com.de.search.base.BaseActivity;
import com.de.search.bean.FriendBean;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
// Some ideas are referred from 'DeviceList' in the open source project below, but it was redesigned for this app and the core part is originality.
// This class is used to display a list of devices, i.e. nearby paired or unpaired devices (mainly phones).
// https://gitee.com/liu_peilin/bluetooth-communication


public class BluetoothDeviceList extends BaseActivity {
    private TextView tvBack;
    private BluetoothAdapter mBtAdapter;
    private ArrayAdapter<String> mPairedDevicesArrayAdapter;
    private ArrayAdapter<String> mNewDevicesArrayAdapter;
    public static String ADDRESS = "device_address";  //Mac address
    public Set<String> stringSet = new HashSet<>();

    AlertDialog alertDialog;
    //Defines the broadcast receiver to process the results after scanning the Bluetooth device
    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            switch (action) {
                case BluetoothDevice.ACTION_FOUND: {
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                        int i = stringSet.size();
                        stringSet.add(device.getAddress());
                        if (i == stringSet.size()) {
                            return;
                        }

                        mNewDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                    }
                    break;
                }
                case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:
                    if (mNewDevicesArrayAdapter.getCount() == 0) {
                        String noDevices = getResources().getText(R.string.none_found).toString();
                        mNewDevicesArrayAdapter.add(noDevices);
                    }
                    break;
                case BluetoothDevice.ACTION_BOND_STATE_CHANGED: {
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    if (device.getBondState() == BluetoothDevice.BOND_BONDING) {

                        Toast.makeText(BluetoothDeviceList.this, "Pairing" + device.getName(), Toast.LENGTH_SHORT).show();
                    } else if (device.getBondState() == BluetoothDevice.BOND_BONDED) {

                        if (alertDialog != null) {
                            return;
                        }

                        //After pairing is complete, a note name is displayed
                        final EditText inputServer = new EditText(BluetoothDeviceList.this);
                        inputServer.setFocusable(true);
                        inputServer.setHint("Custom Name(Default Bluetooth name)");
                        alertDialog = new AlertDialog.Builder(BluetoothDeviceList.this)
                                .setView(inputServer)
                                //title
                                .setTitle("Nominate")
                                //content
                                .setMessage("Nominate Friend")
                                //icon
                                .setIcon(R.mipmap.ic_launcher)
                                .setPositiveButton("confirm", (dialogInterface, i) -> {
                                    List<FriendBean> deviceBeans = FriendBean.find(FriendBean.class, "mac = ?", device.getAddress());
                                    if (deviceBeans.size() > 0) {
                                        deviceBeans.get(0).delete();
                                    }

                                    String name = inputServer.getText().toString();
                                    FriendBean friendBean = new FriendBean();
                                    friendBean.setMac(device.getAddress());
                                    friendBean.setName(device.getName());
                                    friendBean.setUserId("");
                                    friendBean.setUserName(TextUtils.isEmpty(name) ? device.getName() : name);
                                    friendBean.setType("1");
                                    friendBean.save();

                                    findViewById(R.id.title_paired_devices).setVisibility(View.VISIBLE);
                                    mPairedDevicesArrayAdapter.add(name + "\n" + device.getAddress());

                                    String noDevices = getResources().getText(R.string.none_paired).toString();
                                    mPairedDevicesArrayAdapter.remove(noDevices);

                                    alertDialog = null;

                                })
                                .setNegativeButton("default name", (dialogInterface, i) -> {
                                    List<FriendBean> friendBeans = FriendBean.find(FriendBean.class, "mac = ?", device.getAddress());
                                    if (friendBeans.size() > 0) {
                                        friendBeans.get(0).delete();
                                    }

                                    FriendBean friendBean = new FriendBean();
                                    friendBean.setMac(device.getAddress());
                                    friendBean.setName(device.getName());
                                    friendBean.setUserId("");
                                    friendBean.setUserName(device.getName());
                                    friendBean.setType("1");
                                    friendBean.save();

                                    findViewById(R.id.title_paired_devices).setVisibility(View.VISIBLE);
                                    mPairedDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());

                                    String noDevices = getResources().getText(R.string.none_paired).toString();
                                    mPairedDevicesArrayAdapter.remove(noDevices);


                                    alertDialog = null;
                                })
                                .create();
                        alertDialog.setCanceledOnTouchOutside(false);
                        alertDialog.show();

                        //Successful pairing
                        Toast.makeText(BluetoothDeviceList.this, "Complete pairing" + device.getName(), Toast.LENGTH_SHORT).show();


                    } else if (device.getBondState() == BluetoothDevice.BOND_NONE) {
                        //Pairing failure
                        Toast.makeText(BluetoothDeviceList.this, "unpair" + device.getName(), Toast.LENGTH_SHORT).show();

                    }

                    break;
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setView(R.layout.device_list);
        super.onCreate(savedInstanceState);

        setResult(Activity.RESULT_CANCELED);
        init();  //Active interface
    }

    @Override
    protected void initView() {
        tvBack = findViewById(R.id.tv_back);
    }

    @Override
    protected void initData() {

        mPairedDevicesArrayAdapter = new ArrayAdapter<>(this, R.layout.device_name);
        mNewDevicesArrayAdapter = new ArrayAdapter<>(this, R.layout.device_name);

    }

    @Override
    protected void initListener() {
        Button button = findViewById(R.id.button_scan);
        button.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("MissingPermission")
            public void onClick(View v) {
                Toast.makeText(BluetoothDeviceList.this, R.string.scanning, Toast.LENGTH_LONG).show();
                findViewById(R.id.title_new_devices).setVisibility(View.VISIBLE);
                if (mBtAdapter.isDiscovering()) {
                    mBtAdapter.cancelDiscovery();
                }
                mBtAdapter.startDiscovery();
            }
        });

        tvBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }


    @SuppressLint("MissingPermission")
    private void init() {
        //List of paired Bluetooth devices
        ListView pairedListView = findViewById(R.id.paired_devices);
        pairedListView.setAdapter(mPairedDevicesArrayAdapter);
        pairedListView.setOnItemClickListener(paireDeviceClickListener);


        //List of unpaired Bluetooth devices
        ListView newDevicesListView = findViewById(R.id.new_devices);
        newDevicesListView.setAdapter(mNewDevicesArrayAdapter);
        newDevicesListView.setOnItemClickListener(newDeviceClickListener);


        //Dynamically register broadcast receivers
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(broadcastReceiver, filter);
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        registerReceiver(broadcastReceiver, filter);
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();


        @SuppressLint("MissingPermission") Set<BluetoothDevice> pairedDevices = mBtAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            findViewById(R.id.title_paired_devices).setVisibility(View.VISIBLE);
            for (BluetoothDevice device : pairedDevices) {

                List<FriendBean> friendBeans = FriendBean.find(FriendBean.class, "mac = ?", device.getAddress());
                if (friendBeans.size() > 0) {
                    if (friendBeans.get(0).getStatus() == 0) {
                        mPairedDevicesArrayAdapter.add(friendBeans.get(0).getUserName() + "\n" + device.getAddress());
                    } else {
                        mPairedDevicesArrayAdapter.add(friendBeans.get(0).getUserName() + "(send)" + "\n" + device.getAddress());
                    }

                } else {
                    mPairedDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                }

            }
        } else {
            String noDevices = getResources().getText(R.string.none_paired).toString();
            mPairedDevicesArrayAdapter.add(noDevices);
        }
    }

    @SuppressLint("MissingPermission")
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mBtAdapter != null) {
            mBtAdapter.cancelDiscovery();
        }
        this.unregisterReceiver(broadcastReceiver);
    }


    private final AdapterView.OnItemClickListener paireDeviceClickListener = new AdapterView.OnItemClickListener() {
        @SuppressLint("MissingPermission")
        public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {
            mBtAdapter.cancelDiscovery();
            String info = ((TextView) v).getText().toString();
            String address = info.substring(info.length() - 17); //cut 17 characters, that's the mac address
            Intent intent = new Intent();
            intent.putExtra(ADDRESS, address);  //Mac address
            setResult(Activity.RESULT_OK, intent);
            finish();
        }
    };
    private final AdapterView.OnItemClickListener newDeviceClickListener = new AdapterView.OnItemClickListener() {
        @SuppressLint("MissingPermission")
        public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {
            mBtAdapter.cancelDiscovery();
            String info = ((TextView) v).getText().toString();
            String address = info.substring(info.length() - 17); //cut 17 characters, that's the mac address
            BluetoothDevice bluetoothDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(address);

            bluetoothDevice.createBond();

        }
    };
}