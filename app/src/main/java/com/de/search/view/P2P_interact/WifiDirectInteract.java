package com.de.search.view.P2P_interact;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;

import com.alibaba.fastjson.JSONObject;
import com.de.search.R;
import com.de.search.app.APP;
import com.de.search.base.BaseActivity;
import com.de.search.bean.DeviceBean;
import com.de.search.bean.FriendBean;
import com.de.search.util.BluetoothInteractService;
import com.de.search.util.SocketHandler;
import com.de.search.util.maps.Constants;
import com.de.search.util.maps.GPSManager;
import com.de.search.view.BT_interact.BluetoothInteract;
import com.de.search.view.HomeActivity;
import com.de.search.view.SelectActivity;

import org.bingmaps.sdk.BingMapsView;
import org.bingmaps.sdk.Coordinate;
import org.bingmaps.sdk.EntityLayer;
import org.bingmaps.sdk.Point;
import org.bingmaps.sdk.Polyline;
import org.bingmaps.sdk.PolylineOptions;
import org.bingmaps.sdk.Pushpin;
import org.bingmaps.sdk.PushpinOptions;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

//This class's core ideas and design are quite same as the 'BluetoothInteract' class

public class WifiDirectInteract extends BaseActivity {

    //Map correlation
    private BingMapsView bingMapsView;
    private GPSManager _GPSManager;
    private EntityLayer _gpsLayer;
    public static final int STATE = 1;
    public static final int READ = 2;
    public static final int WRITE = 3;
    private static final int REQUEST_CONNECT_DEVICE = 1;  //Request connection device
    private static final int REQUEST_ENABLE_BT = 2;
    private static final int SELECT_BT = 3;
    private TextView mTitle;
    private Button bt1, bt2;
    private BluetoothAdapter bluetoothAdapter;
    private Thread readThread = null;
    private boolean run = false;

    @SuppressLint("SetTextI18n")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        setView(R.layout.activity_direct_chat);
        super.onCreate(savedInstanceState);


        Toolbar toolbar = findViewById(R.id.toolbar);
        //Create options menu
        toolbar.inflateMenu(R.menu.option_menu2);
        //Option menu listening
        toolbar.setOnMenuItemClickListener(new MyMenuItemClickListener());
        mTitle = findViewById(R.id.title_left_text);
        mTitle.setText("Interact(p2p)");
        mTitle = findViewById(R.id.title_right_text);

    }

    @Override
    protected void initView() {
        bt1 = findViewById(R.id.bt1);
        bt2 = findViewById(R.id.bt2);
        LinearLayout ll = findViewById(R.id.ll);
        bingMapsView = findViewById(R.id.mapView);
    }

    @Override
    protected void initData() {

        //Receiving data thread
        readThread = new Thread(() -> {
            InputStream mmInStream = null;
            try {
                mmInStream = SocketHandler.getSocket().getInputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
            while (run){
                if (mmInStream  == null){
                    run = false;
                    return;
                }

                try {
                    byte[] buffer = new byte[1024];
                    int bytes;
                    bytes = mmInStream.read(buffer);
                    mHandler.obtainMessage(BluetoothInteract.READ, bytes, -1, buffer).sendToTarget();
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }
            }
        });

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        Initialize();
    }

    @Override
    protected void initListener() {
        //'Send' button, send your un-find devices to your friend and let them find for you
        bt1.setOnClickListener(view -> {

            Intent serverIntent = new Intent(WifiDirectInteract.this, SelectActivity.class);
            startActivityForResult(serverIntent, SELECT_BT);


        });

        //'Request' button, ask a friend to send me the device they found for me
        bt2.setOnClickListener(view -> {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("type", "0");
            jsonObject.put("id", APP.getBluetoothName());

            //ask a friend to send me the device they found for me
            sendMessage(jsonObject.toJSONString());

        });
    }


    @Override
    public synchronized void onResume() {
        super.onResume();

    }

    @Override
    public void onBackPressed() {
        startActivity(new Intent(WifiDirectInteract.this, HomeActivity.class));
        finish();
    }

    private void setupChat() {
        StringBuffer mOutStringBuffer = new StringBuffer("");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        run = false;
        if (readThread != null && readThread.isAlive()){
            readThread.interrupt();
        }

        readThread = null;

        SocketHandler.closeSocket();

    }


    private void sendMessage(String message) {
        new Thread(() -> {
            try {
                if (SocketHandler.getSocket() != null && SocketHandler.getSocket().isConnected() && SocketHandler.getSocket().getOutputStream() != null){
                    byte[] send = message.getBytes();
                    SocketHandler.getSocket().getOutputStream().write(send);
                    SocketHandler.getSocket().getOutputStream().flush();
                }else {
                    runOnUiThread(() -> Toast.makeText(WifiDirectInteract.this, R.string.not_connected, Toast.LENGTH_SHORT).show());

                }
            } catch (IOException e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(WifiDirectInteract.this, R.string.not_connected, Toast.LENGTH_SHORT).show());
            }
        }).start();


    }


    //Use the Handler object to pass messages between the main UI thread and its child threads
    @SuppressLint("HandlerLeak")
    private final Handler mHandler = new Handler() {   //Message processing
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case STATE:
                    switch (msg.arg1) {
                        case BluetoothInteractService.CONNECTED:
                            mTitle.setText(R.string.title_connected_to);
                            break;
                        case BluetoothInteractService.CONNECTING:
                            mTitle.setText(R.string.title_connecting);
                            break;
                        case BluetoothInteractService.LISTEN:
                        case BluetoothInteractService.NONE:
                            mTitle.setText(R.string.title_not_connected);
                            break;
                    }
                    break;
                case WRITE:

                    break;
                case READ:
                    //Received data
                    if (msg.arg1 == -1){
                        run = false;
                        mTitle.setText(R.string.title_not_connected);
                        break;
                    }

                    byte[] readBuf = (byte[]) msg.obj;
                    String readMessage = new String(readBuf, 0, msg.arg1);

                    JSONObject jsonObject = JSONObject.parseObject(readMessage);
                    String type = jsonObject.getString("type");
                    String id = jsonObject.getString("id");
                    switch (type) {
                        case "0":
                            //Send back devices you've already found for someone else, and start assembling, looking for data that already find
                            List<DeviceBean> deviceBeans = DeviceBean.find(DeviceBean.class, "(user_id = ? or messenger_id = ?) and find = ?", id, id, "1");
                            JSONObject jsonObject1 = new JSONObject();
                            jsonObject1.put("type", "1");//send a '1' to your friend, your friend's app will process in case1
                            jsonObject1.put("data", deviceBeans);

                            //Send back devices you've already found for them
                            WifiDirectInteract.this.sendMessage(jsonObject1.toJSONString());

                            Toast.makeText(WifiDirectInteract.this, "Send the items someone is looking for", Toast.LENGTH_LONG).show();

                            break;
                        case "1":
                            // They sent us the equipment they help to find
                            List<DeviceBean> deviceBeans1 = JSONObject.parseArray(jsonObject.getString("data"), DeviceBean.class);

                            // Save the information about the found device
                            for (DeviceBean deviceBean : deviceBeans1){
                                List<DeviceBean> deviceBeans3 = DeviceBean.find(DeviceBean.class, "mac = ?", deviceBean.getMac());
                                if (deviceBeans3.size() > 0){
                                    deviceBeans3.get(0).setRssi(deviceBean.getRssi());
                                    deviceBeans3.get(0).setFind(deviceBean.getFind());
                                    deviceBeans3.get(0).setFindTime(deviceBean.getFindTime());
                                    deviceBeans3.get(0).setLatitude(deviceBean.getLatitude());
                                    deviceBeans3.get(0).setLongitude(deviceBean.getLongitude());
                                    deviceBeans3.get(0).save();
                                }

                            }

                            //Render map
                            addDevice(deviceBeans1);


                            Toast.makeText(WifiDirectInteract.this, "Receiving items that other person found for me", Toast.LENGTH_LONG).show();



                            break;
                        case "2":
                            // Storing devices sent by friends who asked us to help find them
                            List<DeviceBean> deviceBeans2 = JSONObject.parseArray(jsonObject.getString("data"), DeviceBean.class);
                            for (DeviceBean deviceBean : deviceBeans2){
                                // Query whether the file has been saved
                                List<DeviceBean> deviceBeans3 = DeviceBean.find(DeviceBean.class, "mac = ?", deviceBean.getMac());
                                // If saved, it will not be saved
                                if (deviceBeans3.size() == 0){
                                    deviceBean.setMe(0);
                                    deviceBean.setLongitude("");
                                    deviceBean.setLatitude("");
                                    deviceBean.setFindTime("");
                                    deviceBean.setFind(0);

                                    // Save it to my device
                                    deviceBean.save();
                                    Log.i("receive time mark", "receiving successfully");
                                }
                            }

                            Toast.makeText(WifiDirectInteract.this, "Received the equipment sent by the other party to help find", Toast.LENGTH_LONG).show();

                            JSONObject jsonObject3 = new JSONObject();
                            jsonObject3.put("type", "3");
                            jsonObject3.put("mac", APP.getBluetoothName());

                            //  Reply that you received the device they were looking for
                            WifiDirectInteract.this.sendMessage(jsonObject3.toJSONString());


                            break;
                        case "3":
                            Toast.makeText(WifiDirectInteract.this, "Send a device that needs help finding to a friend, friend has saved your devices", Toast.LENGTH_LONG).show();

                            String mac = jsonObject.getString("mac");
                            List<FriendBean> friendBeans = FriendBean.find(FriendBean.class, "name = ?", mac);
                            if (friendBeans.size() > 0){
                                friendBeans.get(0).setStatus(1);
                                friendBeans.get(0).update();
                            }
                            break;

                    }

                    break;

            }
        }
    };

    //Returns the number of callback methods after entering the buddy list operation
    @SuppressLint("MissingPermission")
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE:
                if (resultCode == Activity.RESULT_OK) {
                    mHandler.obtainMessage(BluetoothInteract.STATE, 3, -1).sendToTarget();

                    String deviceName = data.getExtras().getString(WifiDirectDeviceList.DEVICE_NAME);

                    run = true;
                    readThread = new Thread(() -> {
                        InputStream mmInStream = null;
                        try {
                            mmInStream = SocketHandler.getSocket().getInputStream();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        while (run){
                            if (mmInStream  == null){
                                run = false;
                                return;
                            }

                            try {
                                byte[] buffer = new byte[1024];
                                int bytes;
                                bytes = mmInStream.read(buffer);
                                mHandler.obtainMessage(READ, bytes, -1, buffer).sendToTarget();
                                Thread.sleep(1000);
                            } catch (IOException | InterruptedException e) {
                                e.printStackTrace();
                                break;
                            }
                        }

                        runOnUiThread(() -> mTitle.setText(R.string.title_not_connected));
                    });
                    readThread.start();

                } else if (resultCode == Activity.RESULT_CANCELED) {

                    mHandler.obtainMessage(BluetoothInteract.STATE, 0, -1).sendToTarget();
                }
                break;

            case REQUEST_ENABLE_BT:
                if (resultCode == Activity.RESULT_OK) {
                    setupChat();
                } else {
                    Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(WifiDirectInteract.this, HomeActivity.class));
                    finish();
                }
            case SELECT_BT:
                if (resultCode == Activity.RESULT_OK) {

                    String datas = data.getExtras().getString("data");
                    List<DeviceBean> deviceBeans = JSONObject.parseArray(datas, DeviceBean.class);
                    // The bearer all changes to yourself.
                    for (int i = 0; deviceBeans.size() > i; i ++){
                        deviceBeans.get(i).setMessengerId(APP.getBluetoothName());
                        deviceBeans.get(i).setMessengerName(bluetoothAdapter.getName());
                    }
                    JSONObject jsonObject1 = new JSONObject();
                    jsonObject1.put("type", "2");
                    jsonObject1.put("data", deviceBeans);

                    //Send a friend a device you need help finding
                    WifiDirectInteract.this.sendMessage(jsonObject1.toJSONString());
                    Log.i("send time mark", "sending successfully");

                } else {
                    Toast.makeText(WifiDirectInteract.this, "No choice send device", Toast.LENGTH_SHORT).show();
                }
        }
    }

    // -------------------------------------Map related, using third-party bingMaps---------------------------------------
    private void Initialize() {

        _GPSManager = new GPSManager(this, new GPSLocationListener());


        bingMapsView = findViewById(R.id.mapView);

        bingMapsView.setMapLoadedListener(() -> {
            _gpsLayer = new EntityLayer(Constants.DataLayers.GPS);
            bingMapsView.getLayerManager().addLayer(_gpsLayer);
            UpdateGPSPin();
            updateMarker();
        });


        // Load the map
        bingMapsView.loadMap(Constants.BingMapsKey,
                _GPSManager.GetCoordinate(), Constants.DefaultGPSZoomLevel);

    }

    private void UpdateGPSPin() {
        PushpinOptions opt = new PushpinOptions();
        opt.Icon = Constants.PushpinIcons.GPS;
        Pushpin p = new Pushpin(_GPSManager.GetCoordinate(), opt);
        if (p.Location != null && _gpsLayer != null) {
            _gpsLayer.clear();
            _gpsLayer.add(p);
            _gpsLayer.updateLayer();
        }
    }

    public void updateMarker() {
        List<Coordinate> listCoord = new ArrayList<>();

        EntityLayer entityLayer = (EntityLayer) bingMapsView.getLayerManager()
                .getLayerByName(Constants.DataLayers.Search);
        if (entityLayer == null) {
            entityLayer = new EntityLayer(Constants.DataLayers.Search);
        }
        entityLayer.clear();

        PushpinOptions opt = new PushpinOptions();
        opt.Icon = Constants.PushpinIcons.RedFlag;
        opt.Width = 20;
        opt.Height = 35;
        opt.Anchor = new Point(11, 10);


        bingMapsView.getLayerManager().addLayer(entityLayer);
        entityLayer.updateLayer();


        Coordinate coordinate = _GPSManager.GetCoordinate();
        bingMapsView.setCenterAndZoom(coordinate,15);


        Polyline routeLine = new Polyline(listCoord);
        PolylineOptions polylineOptions = new PolylineOptions();
        polylineOptions.StrokeThickness = 3;
        routeLine.Options = polylineOptions;
        entityLayer.add(routeLine);
    }
    // --------------------------------------Map related, using third-party bingMaps------------------------------------

    private void addDevice(List<DeviceBean> deviceBeans){
        EntityLayer entityLayer = (EntityLayer) bingMapsView.getLayerManager()
                .getLayerByName(Constants.DataLayers.Search);
        if (entityLayer == null) {
            entityLayer = new EntityLayer(Constants.DataLayers.Search);
        }
        entityLayer.clear();

        for (int i = 0; i < deviceBeans.size(); i++){
            double longitude = Double.parseDouble(deviceBeans.get(i).getLongitude());
            double latitude = Double.parseDouble(deviceBeans.get(i).getLatitude());

            Coordinate coord = new Coordinate(latitude, longitude);

            PushpinOptions opt = new PushpinOptions();
            opt.Icon = "file:///android_asset/pin_red_flag.png";
            opt.Width = 20;
            opt.Height = 20;
            opt.Text = deviceBeans.get(i).getName();
            opt.Anchor = new Point(11, 10);
            Pushpin p = new Pushpin(coord, opt);
            p.Title = deviceBeans.get(i).getName();//If the title attribute is not set, infobox will not be displayed

            entityLayer.add(p);
        }

        bingMapsView.getLayerManager().addLayer(entityLayer);
        entityLayer.updateLayer();
    }

    public class GPSLocationListener implements LocationListener {
        public void onLocationChanged(Location arg0) {
            UpdateGPSPin();
        }

        public void onProviderDisabled(String arg0) {
        }

        public void onProviderEnabled(String arg0) {
        }

        public void onStatusChanged(String arg0, int arg1, Bundle arg2) {
        }
    }

    //Inner class, click event handling for the options menu
    private class MyMenuItemClickListener implements Toolbar.OnMenuItemClickListener {
        @SuppressLint("NonConstantResourceId")
        @Override
        public boolean onMenuItemClick(MenuItem item) {
            switch (item.getItemId()) {
                case R.id.scan:
                    run = false;
                    if (readThread != null && readThread.isAlive()){
                        readThread.interrupt();
                    }
                    readThread = null;
                    SocketHandler.closeSocket();

                    //Start the DirectDeviceList Activity
                    Intent serverIntent = new Intent(WifiDirectInteract.this, WifiDirectDeviceList.class);
                    startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
                    return true;
                case R.id.back:
                    startActivity(new Intent(WifiDirectInteract.this, HomeActivity.class));
                    finish();
                    return true;
            }
            return false;
        }
    }

}
