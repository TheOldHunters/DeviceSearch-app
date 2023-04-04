package com.de.search.view;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.de.search.R;
import com.de.search.app.APP;
import com.de.search.base.BaseActivity;
import com.de.search.bean.DeviceBean;
import com.de.search.bean.FriendBean;
import com.orm.query.Select;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
// https://gitee.com/liu_peilin/bluetooth-communication
public class DeviceList extends BaseActivity {
    private TextView tvBack;
    private BluetoothAdapter mBtAdapter;
    private ArrayAdapter<String> mPairedDevicesArrayAdapter;
    private ArrayAdapter<String> mNewDevicesArrayAdapter;
    public static String ADDRESS = "device_address";  //Mac地址
    public Set<String> stringSet = new HashSet<>();

    AlertDialog alertDialog;
    //定义广播接收者，用于处理扫描蓝牙设备后的结果
    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(BluetoothDevice.ACTION_FOUND)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    int i = stringSet.size();
                    stringSet.add(device.getAddress());
                    if (i == stringSet.size()) {
                        return;
                    }

                    mNewDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                }
            } else if (action.equals(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)) {
                if (mNewDevicesArrayAdapter.getCount() == 0) {
                    String noDevices = getResources().getText(R.string.none_found).toString();
                    mNewDevicesArrayAdapter.add(noDevices);
                }
            } else if (action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device.getBondState() == BluetoothDevice.BOND_BONDING) {

                    Toast.makeText(DeviceList.this, "Pairing" + device.getName(), Toast.LENGTH_SHORT).show();
                } else if (device.getBondState() == BluetoothDevice.BOND_BONDED) {

                    if (alertDialog != null) {
                        return;
                    }

                    // 配对完成后，弹出备注名称
                    final EditText inputServer = new EditText(DeviceList.this);
                    inputServer.setFocusable(true);
                    inputServer.setHint("Custom Name(Default Bluetooth name)");
                    alertDialog = new AlertDialog.Builder(DeviceList.this)
                            .setView(inputServer)
                            //标题
                            .setTitle("Nominate")
                            //内容
                            .setMessage("Nominate Friend")
                            //图标
                            .setIcon(R.mipmap.ic_launcher)
                            .setPositiveButton("confirm", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
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

                                }
                            })
                            .setNegativeButton("default name", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
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
                                }
                            })
                            .create();
                    alertDialog.setCanceledOnTouchOutside(false);
                    alertDialog.show();

                    Toast.makeText(DeviceList.this, "Complete pairing" + device.getName(), Toast.LENGTH_SHORT).show();


                } else if (device.getBondState() == BluetoothDevice.BOND_NONE) {
                    Toast.makeText(DeviceList.this, "取消配对" + device.getName(), Toast.LENGTH_SHORT).show();

                }

            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setView(R.layout.device_list);
        super.onCreate(savedInstanceState);

        setResult(Activity.RESULT_CANCELED);
        init();  //活动界面
    }

    @Override
    protected void initView() {
        tvBack = findViewById(R.id.tv_back);
    }

    @Override
    protected void initData() {

        mPairedDevicesArrayAdapter = new ArrayAdapter<String>(this, R.layout.device_name);
        mNewDevicesArrayAdapter = new ArrayAdapter<String>(this, R.layout.device_name);

    }

    @Override
    protected void initListener() {
        Button button = findViewById(R.id.button_scan);
        button.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("MissingPermission")
            public void onClick(View v) {
                Toast.makeText(DeviceList.this, R.string.scanning, Toast.LENGTH_LONG).show();
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
        //已配对蓝牙设备列表
        ListView pairedListView = findViewById(R.id.paired_devices);
        pairedListView.setAdapter(mPairedDevicesArrayAdapter);
        pairedListView.setOnItemClickListener(paireDeviceClickListener);


        //未配对蓝牙设备列表
        ListView newDevicesListView = findViewById(R.id.new_devices);
        newDevicesListView.setAdapter(mNewDevicesArrayAdapter);
        newDevicesListView.setOnItemClickListener(newDeviceClickListener);


        //动态注册广播接收者
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


    private AdapterView.OnItemClickListener paireDeviceClickListener = new AdapterView.OnItemClickListener() {
        @SuppressLint("MissingPermission")
        public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {
            mBtAdapter.cancelDiscovery();
            String info = ((TextView) v).getText().toString();
            String address = info.substring(info.length() - 17); // 截取17个字符，就是mac
            Intent intent = new Intent();
            intent.putExtra(ADDRESS, address);  //Mac地址
            setResult(Activity.RESULT_OK, intent);
            finish();
        }
    };
    private AdapterView.OnItemClickListener newDeviceClickListener = new AdapterView.OnItemClickListener() {
        @SuppressLint("MissingPermission")
        public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {
            mBtAdapter.cancelDiscovery();
            String info = ((TextView) v).getText().toString();
            String address = info.substring(info.length() - 17); // 截取17个字符，就是mac
            BluetoothDevice bluetoothDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(address);

            bluetoothDevice.createBond();

        }
    };


}