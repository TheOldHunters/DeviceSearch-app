package com.de.search.view.BT_interact;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.pm.PackageManager;
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
import androidx.core.app.ActivityCompat;

import com.alibaba.fastjson.JSONObject;
import com.de.search.R;
import com.de.search.app.APP;
import com.de.search.base.BaseActivity;
import com.de.search.bean.DeviceBean;
import com.de.search.bean.FriendBean;
import com.de.search.util.BluetoothInteractService;
import com.de.search.util.maps.Constants;
import com.de.search.util.maps.GPSManager;
import com.de.search.view.HomeActivity;
import com.de.search.view.SelectActivity;

import org.bingmaps.sdk.BingMapsView;
import org.bingmaps.sdk.Coordinate;
import org.bingmaps.sdk.EntityLayer;
import org.bingmaps.sdk.MapLoadedListener;
import org.bingmaps.sdk.Point;
import org.bingmaps.sdk.Polyline;
import org.bingmaps.sdk.PolylineOptions;
import org.bingmaps.sdk.Pushpin;
import org.bingmaps.sdk.PushpinOptions;

import java.util.ArrayList;
import java.util.List;


public class BluetoothInteract extends BaseActivity {

    private TextView textView;
    private String mConnectedDeviceName = null;
    private StringBuffer mOutStringBuffer;
    private BluetoothAdapter mBluetoothAdapter = null;
    private BluetoothInteractService mBluetoothInteractService = null;
    private Button bt1, bt2; //two buttons control send and request
    private LinearLayout ll;
    private BluetoothAdapter bluetoothAdapter;

    //Map correlation
    private BingMapsView bingMapsView;
    private GPSManager _GPSManager;
    private EntityLayer _gpsLayer;

    public static final int STATE = 1;
    public static final int READ = 2;
    public static final int WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";
    private static final int REQUEST_CONNECT_DEVICE = 1;  //Request connection device
    private static final int REQUEST_ENABLE_BT = 2;
    private static final int SELECT_BT = 3;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        setView(R.layout.activity_bluetooth_chat);
        super.onCreate(savedInstanceState);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        //Judge Bluetooth on
        if (mBluetoothAdapter.isEnabled()) {
            if (mBluetoothInteractService == null) {
                //Create a service object
                mBluetoothInteractService = new BluetoothInteractService(this, mHandler);
                mOutStringBuffer = new StringBuffer("");
            }
        }
    }

    @Override
    protected void initView() {
        bt1 = findViewById(R.id.bt1);
        bt2 = findViewById(R.id.bt2);
        ll = findViewById(R.id.ll);
        bingMapsView = findViewById(R.id.mapView);
        textView = findViewById(R.id.title_right_text);
    }

    @Override
    protected void initData() {

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        Initialize();
    }

    @Override
    protected void initListener() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        //Create options menu
        toolbar.inflateMenu(R.menu.option_menu);
        //Option menu listening
        toolbar.setOnMenuItemClickListener(new MyMenuItemClickListener());

        //'Send' button, send your un-find devices to your friend and let them find for you
        bt1.setOnClickListener(view -> {

            Intent serverIntent = new Intent(BluetoothInteract.this, SelectActivity.class);
            //jump to devices select page
            startActivityForResult(serverIntent, SELECT_BT);


        });

        //'Request' button, ask a friend to send me the device they found for me
        bt2.setOnClickListener(view -> {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("type", "0"); //jump to case0 and process the request
            jsonObject.put("id", APP.getBluetoothName());

            ///Ask a friend to send me the device they found for me
            BluetoothInteract.this.sendMessage(jsonObject.toJSONString());

        });
    }


    @Override
    public synchronized void onResume() {
        super.onResume();
        if (mBluetoothInteractService != null) {
            if (mBluetoothInteractService.getState() == BluetoothInteractService.NONE) {
                mBluetoothInteractService.start();
            }
        }
    }

    @Override
    public void onBackPressed() {
        startActivity(new Intent(BluetoothInteract.this, HomeActivity.class));
        finish();
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mBluetoothInteractService != null)
            mBluetoothInteractService.stopThread();
    }

    //Modify the visibility of native Bluetooth devices -> set visible
    private void ensureDiscoverable() {
        //After turning on the phone's Bluetooth, the time that it can be scanned by other Bluetooth devices is not permanent
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        if (mBluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            //Set visible for 100 seconds (can be scanned)
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 100); //100s
            startActivity(discoverableIntent);
            Toast.makeText(this, "The visibility of the local Bluetooth device has been set, and the other party can search.", Toast.LENGTH_SHORT).show();
        }
    }

    private void sendMessage(String message) {
        if (mBluetoothInteractService.getState() != BluetoothInteractService.CONNECTED) {
            Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
            return;
        }
        if (message.length() > 0) {
            byte[] send = message.getBytes();
            mBluetoothInteractService.writeData(send);
            mOutStringBuffer.setLength(0);
        }
    }


    //Use the Handler object to pass messages between the main UI thread and its child threads
    @SuppressLint("HandlerLeak")
    private final Handler mHandler = new Handler() {   //消息处理
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case STATE:
                    switch (msg.arg1) {
                        case BluetoothInteractService.CONNECTED:
                            textView.setText(R.string.title_connected_to);
                            break;
                        case BluetoothInteractService.CONNECTING:
                            textView.setText(R.string.title_connecting);
                            break;
                        case BluetoothInteractService.LISTEN:
                        case BluetoothInteractService.NONE:
                            textView.setText(R.string.title_not_connected);
                            break;
                    }
                    break;

                case WRITE:

                    break;

                case READ:
                    //Received data
                    byte[] readBuf = (byte[]) msg.obj;
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    //Data analysis
                    JSONObject jsonObject = JSONObject.parseObject(readMessage);
                    String type = jsonObject.getString("type");
                    String id = jsonObject.getString("id");
                    switch (type) {
                        case "0":
                            //Send back devices you've already found for someone else, and start assembling, looking for data that already find
                            List<DeviceBean> deviceBeans = DeviceBean.find(DeviceBean.class, "(user_id = ? or messenger_id = ?) and find = ?", id, id, "1");
                            JSONObject jsonObject1 = new JSONObject();
                            jsonObject1.put("type", "1"); //send a '1' to your friend, your friend's app will process in case1
                            jsonObject1.put("data", deviceBeans);

                            //Send back devices you've already found for them
                            BluetoothInteract.this.sendMessage(jsonObject1.toJSONString());

                            Toast.makeText(BluetoothInteract.this, "Send the device to help find to the other party", Toast.LENGTH_LONG).show();

                            break;

                        case "1":
                            // They sent us the equipment they help to find
                            List<DeviceBean> deviceBeans1 = JSONObject.parseArray(jsonObject.getString("data"), DeviceBean.class);

                            // Save the information about the found device
                            for (DeviceBean deviceBean : deviceBeans1) {
                                List<DeviceBean> deviceBeans3 = DeviceBean.find(DeviceBean.class, "mac = ?", deviceBean.getMac());
                                if (deviceBeans3.size() > 0) {
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


                            Toast.makeText(BluetoothInteract.this, "Received the equipment sent by the other party to help find it", Toast.LENGTH_LONG).show();


                            break;
                        case "2":
                            //Storing devices sent by friends who asked us to help find them
                            List<DeviceBean> deviceBeans2 = JSONObject.parseArray(jsonObject.getString("data"), DeviceBean.class);
                            for (DeviceBean deviceBean : deviceBeans2) {
                                //Query whether the file has been saved
                                List<DeviceBean> deviceBeans3 = DeviceBean.find(DeviceBean.class, "mac = ?", deviceBean.getMac());
                                //If saved, it will not be saved
                                if (deviceBeans3.size() == 0) {
                                    //The custom name of the friend can be queried through the passed mac, because the friend's mac will be saved when matching friends, so it can be queried
                                    List<FriendBean> friendBeans = FriendBean.find(FriendBean.class, "name = ?", deviceBean.getMessengerId());
                                    if (friendBeans.size() > 0) {
                                        //Find and set the custom name of the device passer in the database, the passer is currently in connection with you
                                        deviceBean.setMessengerName(friendBeans.get(0).getUserName());
                                    }

                                    //The custom name of the friend can be queried by the owner's mac, because the friend's mac will be saved when matching friends, so it can be queried
                                    List<FriendBean> friendBeans2 = FriendBean.find(FriendBean.class, "name = ?", deviceBean.getUserId());
                                    if (friendBeans2.size() > 0) {
                                        //If I have paired the device owner in the app, I will take out the note name of the device owner in the database and set it
                                        deviceBean.setUserName(friendBeans2.get(0).getUserName());
                                    }

                                    deviceBean.setMe(0);
                                    deviceBean.setLongitude("");
                                    deviceBean.setLatitude("");
                                    deviceBean.setFindTime("");
                                    deviceBean.setFind(0);

                                    //Save it to my device
                                    deviceBean.save();
                                }
                            }

                            Toast.makeText(BluetoothInteract.this, "Received the equipment sent by the other party to help find", Toast.LENGTH_LONG).show();
                            Log.i("receive time mark", "receiving successfully");

                            JSONObject jsonObject3 = new JSONObject();
                            jsonObject3.put("type", "3");
                            jsonObject3.put("mac", APP.getBluetoothName());

                            // Reply that you received the device they were looking for
                            BluetoothInteract.this.sendMessage(jsonObject3.toJSONString());


                            break;
                        case "3":
                            //Notification: The message you sent to the other guy has been successfully received and saved
                            Toast.makeText(BluetoothInteract.this, "Send a device that needs help finding to a friend, friend has saved your devices", Toast.LENGTH_LONG).show();
                            Log.i("send time mark", "sending successfully");

                            String mac = jsonObject.getString("mac");
                            List<FriendBean> friendBeans = FriendBean.find(FriendBean.class, "name = ?", mac);
                            if (friendBeans.size() > 0) {
                                friendBeans.get(0).setStatus(1);
                                friendBeans.get(0).update();

                            }
                            break;

                    }

                    break;
                case MESSAGE_DEVICE_NAME:
                    mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                    Toast.makeText(getApplicationContext(), "link to " + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                    break;
                case MESSAGE_TOAST:
                    Toast.makeText(getApplicationContext(),
                            msg.getData().getString(TOAST), Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    //Returns the number of callback methods after entering the buddy list operation
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE:
                //Bluetooth connection
                if (resultCode == Activity.RESULT_OK) {
                    String address = data.getExtras().getString(BluetoothDeviceList.ADDRESS);
                    BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
                    mBluetoothInteractService.onConnect(device);
                }
                break;
            case REQUEST_ENABLE_BT:
                if (resultCode == Activity.RESULT_OK) {
                    //Create a service object
                    mBluetoothInteractService = new BluetoothInteractService(this, mHandler);
                    mOutStringBuffer = new StringBuffer("");
                } else {
                    Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(BluetoothInteract.this, HomeActivity.class));
                    finish();
                }
            case SELECT_BT:
                if (resultCode == Activity.RESULT_OK) {

                    String datas = data.getExtras().getString("data");
                    List<DeviceBean> deviceBeans = JSONObject.parseArray(datas, DeviceBean.class);

                    //The bearer all changes to yourself.
                    for (int i = 0; deviceBeans.size() > i; i++) {
                        deviceBeans.get(i).setMessengerId(APP.getBluetoothName());
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
                        deviceBeans.get(i).setMessengerName(bluetoothAdapter.getName());
                    }
                    JSONObject jsonObject1 = new JSONObject();
                    jsonObject1.put("type", "2"); //send a '2' to your friend, you friend will in case2 and process the data you send
                    jsonObject1.put("data", deviceBeans); //data of your devices

                    //Send a friend a device you need help finding
                    BluetoothInteract.this.sendMessage(jsonObject1.toJSONString());

                } else {
                    Toast.makeText(BluetoothInteract.this, "No choice send device", Toast.LENGTH_SHORT).show();
                }
        }
    }


    // -------------------------------------Map related, using third-party bingMaps---------------------------------------
    private void Initialize() {

        _GPSManager = new GPSManager(this, new GPSLocationListener());
        
        bingMapsView = (BingMapsView) findViewById(R.id.mapView);
        
        bingMapsView.setMapLoadedListener(new MapLoadedListener() {
            public void onAvailableChecked() {

                _gpsLayer = new EntityLayer(Constants.DataLayers.GPS);
                bingMapsView.getLayerManager().addLayer(_gpsLayer);
                UpdateGPSPin();
                updateMarker();
            }
        });
        
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
        // EntityLayer is used for map overlay
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
            p.Title = deviceBeans.get(i).getName();//infobox will not display without title property

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
        @Override
        public boolean onMenuItemClick(MenuItem item) {
            switch (item.getItemId()) {
                case R.id.scan:
                    Intent serverIntent = new Intent(BluetoothInteract.this, BluetoothDeviceList.class);
                    startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
                    return true;
                case R.id.discoverable:
                    ensureDiscoverable();
                    return true;
                case R.id.back:
                    startActivity(new Intent(BluetoothInteract.this, HomeActivity.class));
                    finish();
                    return true;
            }
            return false;
        }
    }
}
