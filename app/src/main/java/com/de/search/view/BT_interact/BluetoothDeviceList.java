package com.de.search.view.BT_interact;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
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
import androidx.core.app.ActivityCompat;

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
                //When a new Bluetooth device is found (BluetoothDevice.ACTION_FOUND), the device is added to the mNewDevicesArrayAdapter if it is not already paired.
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
                //When the Bluetooth adapter has finished searching (BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
                //if no new devices are found, add a message to mNewDevicesArrayAdapter indicating that no devices were found.
                case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:
                    if (mNewDevicesArrayAdapter.getCount() == 0) {
                        String noDevices = getResources().getText(R.string.none_found).toString();
                        mNewDevicesArrayAdapter.add(noDevices);
                    }
                    break;

                //BluetoothDevice.ACTION_BOND_STATE_CHANGED: This event is triggered when the binding state of the device changes.
                //The code handles three binding states:
                //a. BluetoothDevice.BOND_BONDING: The device is pairing. A Toast is displayed to indicate that pairing is in progress.
                //b. BluetoothDevice.BOND_BONDED: The device has been successfully paired. A dialog box is displayed to allow the user to enter a custom name for the device or to use the default name. Also saves the device information to FriendBean and updates the device list.
                //c. BluetoothDevice.BOND_NONE: The device is not paired. A Toast is displayed to indicate that the device is not paired.
                case BluetoothDevice.ACTION_BOND_STATE_CHANGED: {
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    if (device.getBondState() == BluetoothDevice.BOND_BONDING) {

                        Toast.makeText(BluetoothDeviceList.this, "Pairing" + device.getName(), Toast.LENGTH_SHORT).show();
                    }
                    else if (device.getBondState() == BluetoothDevice.BOND_BONDED) {

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


                    }
                    else if (device.getBondState() == BluetoothDevice.BOND_NONE) {
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
        setupPairedDevicesListView();
        setupNewDevicesListView();
        registerBluetoothReceivers();
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        displayPairedDevices();
    }
    //Set the paired device list view.
    private void setupPairedDevicesListView() {
        ListView pairedListView = findViewById(R.id.paired_devices);
        pairedListView.setAdapter(mPairedDevicesArrayAdapter);
        pairedListView.setOnItemClickListener(paireDeviceClickListener);
    }

    //Set the unpaired device list view.
    private void setupNewDevicesListView() {
        ListView newDevicesListView = findViewById(R.id.new_devices);
        newDevicesListView.setAdapter(mNewDevicesArrayAdapter);
        newDevicesListView.setOnItemClickListener(newDeviceClickListener);
    }

    //Register a Bluetooth radio receiver.
    private void registerBluetoothReceivers() {
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(broadcastReceiver, filter);
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        registerReceiver(broadcastReceiver, filter);
    }

    //Displays a list of paired devices.
    @SuppressLint("MissingPermission")
    private void displayPairedDevices() {
        Set<BluetoothDevice> pairedDevices = mBtAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            findViewById(R.id.title_paired_devices).setVisibility(View.VISIBLE);
            for (BluetoothDevice device : pairedDevices) {
                addDeviceToList(device);
            }
        } else {
            String noDevices = getResources().getText(R.string.none_paired).toString();
            mPairedDevicesArrayAdapter.add(noDevices);
        }
    }

    //Add devices to the paired devices list based on device information
    private void addDeviceToList(BluetoothDevice device) {
        List<FriendBean> friendBeans = FriendBean.find(FriendBean.class, "mac = ?", device.getAddress());
        if (friendBeans.size() > 0) {
            if (friendBeans.get(0).getStatus() == 0) {
                mPairedDevicesArrayAdapter.add(friendBeans.get(0).getUserName() + "\n" + device.getAddress());
            } else {
                mPairedDevicesArrayAdapter.add(friendBeans.get(0).getUserName() + "(send)" + "\n" + device.getAddress());
            }
        } else {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            mPairedDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
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
        public void onItemClick(AdapterView<?> av, View v, int position, long id) {
            // Cancel Bluetooth discovery process
            mBtAdapter.cancelDiscovery();

            // Get the device information string from the TextView
            String info = ((TextView) v).getText().toString();

            // Extract the MAC address by taking the last 17 characters of the string
            String address = info.substring(info.length() - 17);

            // Create a new Intent and put the MAC address as an extra
            Intent intent = new Intent();
            intent.putExtra(ADDRESS, address);

            // Set the result and finish the activity
            setResult(Activity.RESULT_OK, intent);
            finish();
        }

    };
    private final AdapterView.OnItemClickListener newDeviceClickListener = new AdapterView.OnItemClickListener() {
        @SuppressLint("MissingPermission")
        public void onItemClick(AdapterView<?> av, View v, int position, long id) {
            // Cancel Bluetooth discovery process
            mBtAdapter.cancelDiscovery();

            // Get the device information string from the TextView
            String info = ((TextView) v).getText().toString();

            // Extract the MAC address by taking the last 17 characters of the string
            String address = info.substring(info.length() - 17);

            // Get the BluetoothDevice instance for the given MAC address
            BluetoothDevice bluetoothDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(address);

            // Initiate the pairing process with the selected Bluetooth device
            bluetoothDevice.createBond();
        }

    };
}