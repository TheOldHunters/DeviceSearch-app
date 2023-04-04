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
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
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

import com.de.search.R;
import com.de.search.base.BaseActivity;
import com.de.search.bean.DeviceDataBean;
import com.de.search.bean.FriendBean;
import com.de.search.util.SocketHandler;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

//https://github.com/murtaza98/Walkie-Talkie/tree/master/app/src/main/java/com/example/murtaza/walkietalkie

public class DirectDeviceList extends BaseActivity {

    public static final int PORT = 2555;


    public BroadcastReceiver mReceiver;
    public IntentFilter mIntentFilter;
    public WifiManager wifiManager;
    public WifiP2pManager mManager;
    public WifiP2pManager.Channel mChannel;
    ArrayList<DeviceDataBean> customPeers = new ArrayList<>();


    private TextView tvBack;
    public ArrayAdapter<String> mPairedDevicesArrayAdapter;
    public static String DEVICE_NAME = "device_name";  //Mac地址


    public ServerThread serverThread;
    public ClientThread clientThread;

    private int select;
    private boolean click = false;

    private int num = 0;


    // p2p寻找设备回调
    WifiP2pManager.PeerListListener peerListListener = new WifiP2pManager.PeerListListener() {
        @Override
        public void onPeersAvailable(WifiP2pDeviceList peersList) {
            Log.d("DEVICE_NAME", "Listener called"+peersList.getDeviceList().size());
            if(peersList.getDeviceList().size() != 0){
                ArrayList<DeviceDataBean> device_already_present = new ArrayList<>();
                for(WifiP2pDevice device : peersList.getDeviceList()){
                    int idx = checkPeersListByName(device.deviceName);
                    if(idx != -1){
                        device_already_present.add(customPeers.get(idx));
                    }
                }

                if(device_already_present.size() == peersList.getDeviceList().size()){
                    return;
                }
                customPeers.clear();
                customPeers.addAll(device_already_present);
                for(WifiP2pDevice device : peersList.getDeviceList()) {
                    if (checkPeersListByName(device.deviceName) == -1) {

                        DeviceDataBean deviceDataBean = new DeviceDataBean();
                        deviceDataBean.deviceName = device.deviceName;
                        deviceDataBean.device = device;
                        customPeers.add(deviceDataBean);
                    }
                }

                for (DeviceDataBean customDevice : customPeers){
                    mPairedDevicesArrayAdapter.add("\n" + customDevice.deviceName + "\n" );
                }
            }

            if(peersList.getDeviceList().size() == 0){
                Toast.makeText(getApplicationContext(), "No Peers Found", Toast.LENGTH_SHORT).show();
            }
        }
    };


    // p2p连接回调
    WifiP2pManager.ConnectionInfoListener connectionInfoListener = new WifiP2pManager.ConnectionInfoListener() {
        @Override
        public void onConnectionInfoAvailable(WifiP2pInfo info) {
            // 连接成功后判读是什么端
            if(info.groupFormed && info.isGroupOwner){
                // 服务端
                serverThread = new ServerThread();
                serverThread.start();
            }else if(info.groupFormed){
                // 客户端
                clientThread = new ClientThread(info.groupOwnerAddress);
                clientThread.start();
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setView(R.layout.direct_device_list);
        super.onCreate(savedInstanceState);

        //在被调用活动里，设置返回结果码
        setResult(Activity.RESULT_CANCELED);
    }

    @Override
    protected void initView() {
        tvBack = findViewById(R.id.tv_back);
    }

    @Override
    protected void initData() {
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);


        mManager = (WifiP2pManager) getSystemService(WIFI_P2P_SERVICE);
        mChannel = mManager.initialize(this, getMainLooper(), null);
        
        SocketHandler.setmManager(mManager);
        SocketHandler.setmChannel(mChannel);

        set();
        
        mPairedDevicesArrayAdapter = new ArrayAdapter<String>(this, R.layout.device_name);

        //设备列表
        ListView pairedListView = findViewById(R.id.paired_devices);
        pairedListView.setAdapter(mPairedDevicesArrayAdapter);
        pairedListView.setOnItemClickListener(mPaireDeviceClickListener);

    }

    // 配置广播
    void set(){
        mReceiver = new WifiDirectBroadcastReceiver(mManager, mChannel);

        mIntentFilter = new IntentFilter();

        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
    }

    @Override
    protected void initListener() {
        tvBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        Button scanButton = findViewById(R.id.button_scan);
        scanButton.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("MissingPermission")
            public void onClick(View v) {

                mPairedDevicesArrayAdapter.clear();
                customPeers.clear();

                click = false;

                // 开始搜索设备
                mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(DirectDeviceList.this, "Discovery Started", Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onFailure(int reason) {
                        Log.e("reason", reason+ "");
                        Toast.makeText(DirectDeviceList.this, "Discovery start Failed", Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }




    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mReceiver);
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mReceiver, mIntentFilter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

    }


    private int checkPeersListByName(String deviceName){
        for(DeviceDataBean d :customPeers) {
            if (d.deviceName.equals(deviceName)) {
                return customPeers.indexOf(d);
            }
        }
        return -1;
    }

    private AdapterView.OnItemClickListener mPaireDeviceClickListener = new AdapterView.OnItemClickListener() {
        @SuppressLint("MissingPermission")
        public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {
            select = arg2;

            click = true;

            num = 0;

            connect();

        }
    };

    @SuppressLint("MissingPermission")
    public void connect(){
        final WifiP2pDevice device = customPeers.get(select).device;
        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = device.deviceAddress;


        mManager.connect(mChannel, config, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                num = 0;
                Toast.makeText(getApplicationContext(), "Connected to "+device.deviceName, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(int reason) {
                num = num + 1;
                if (num >= 3){
                    Log.e("11111", "<<<<<<<<<<<<<<<<");
                    Toast.makeText(getApplicationContext(), "Error in connecting to "+device.deviceName + ":\n Click Connect after searching for friends again", Toast.LENGTH_LONG).show();
                    return;
                }

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        connect();
                    }
                }).start();


//                    Toast.makeText(getApplicationContext(), "Error in connecting to "+device.deviceName, Toast.LENGTH_SHORT).show();
            }
        });
    }




    // p2p广播
    public class WifiDirectBroadcastReceiver extends BroadcastReceiver {
        private WifiP2pManager mManager;
        private WifiP2pManager.Channel mChannel;

        public WifiDirectBroadcastReceiver(WifiP2pManager mManager, WifiP2pManager.Channel mChannel) {
            this.mManager = mManager;
            this.mChannel = mChannel;
        }

        @SuppressLint("MissingPermission")
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
                if (mManager == null) {
                    return;
                }

                mManager.requestPeers(mChannel, peerListListener);
                Log.e("DEVICE_NAME", "WIFI P2P peers changed called");

                if (click){

                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            mManager.removeGroup(mChannel, new WifiP2pManager.ActionListener() {
                                public void onSuccess() {
                                    Log.e("---", "remove group success");

                                    try {
                                        Thread.sleep(2000);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }

                                    connect();


                                }
                                public void onFailure(int reason) {
                                    Log.e("---" , "remove group fail");


                                    try {
                                        Thread.sleep(2000);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }

                                    connect();

                                }
                            });
                        }
                    }).start();


                    click = false;
                }

            } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
                if (mManager == null) {
                    return;
                }

                NetworkInfo networkInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
                if (networkInfo != null && networkInfo.isConnected()) {
                    mManager.requestConnectionInfo(mChannel, connectionInfoListener);
                }

            }
        }
    }

    // 服务端线程
    public class ServerThread extends Thread{
        Socket socket;
        ServerSocket serverSocket;
        @Override
        public void run() {
            try {
                // 启动，等待连接
                serverSocket = new ServerSocket(PORT);
                socket = serverSocket.accept();

                SocketHandler.setSocket(socket);
                SocketHandler.setServerSocket(serverSocket);
                SocketHandler.setType(0);

                go();

            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
        }

        void go(){
            Intent intent = new Intent();

            intent.putExtra(DEVICE_NAME, customPeers.get(select).deviceName);

            setResult(Activity.RESULT_OK, intent);

            finish();
        }
    }

    // 客服端线程
    public class ClientThread extends Thread{
        Socket socket;
        String hostAddress;

        ClientThread(InetAddress address){
            this.socket = new Socket();
            this.hostAddress = address.getHostAddress();
        }

        @Override
        public void run() {
            try {
                // 去连接服务端
                socket.connect(new InetSocketAddress(hostAddress, PORT), 1000);
                SocketHandler.setSocket(socket);
                SocketHandler.setType(1);
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
        }

        void go(){
            Intent intent = new Intent();

            intent.putExtra(DEVICE_NAME, customPeers.get(select).deviceName);

            setResult(Activity.RESULT_OK, intent);

            finish();
        }
    }

    

}