package com.de.search.view;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.MenuItem;
import android.view.View;
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

import org.bingmaps.sdk.BingMapsView;
import org.bingmaps.sdk.Coordinate;
import org.bingmaps.sdk.EntityLayer;
import org.bingmaps.sdk.MapLoadedListener;
import org.bingmaps.sdk.Point;
import org.bingmaps.sdk.Polyline;
import org.bingmaps.sdk.PolylineOptions;
import org.bingmaps.sdk.Pushpin;
import org.bingmaps.sdk.PushpinOptions;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;


public class DirectInteract extends BaseActivity {

    // 地图相关
    private BingMapsView bingMapsView;
    private GPSManager _GPSManager;
    private EntityLayer _gpsLayer;


    public static final int STATE = 1;
    public static final int READ = 2;
    public static final int WRITE = 3;
    private static final int REQUEST_CONNECT_DEVICE = 1;  //请求连接设备
    private static final int REQUEST_ENABLE_BT = 2;
    private static final int SELECT_BT = 3;
    private TextView mTitle;
    //    private ListView mConversationView;
//    private EditText mOutEditText;
//    private Button mSendButton;
    private String mConnectedDeviceName = null;
    //    private ArrayAdapter<String> mConversationArrayAdapter;
    private StringBuffer mOutStringBuffer;
    private BluetoothAdapter mBluetoothAdapter = null;


    private Button bt1, bt2;
    private LinearLayout ll;

    private BluetoothAdapter bluetoothAdapter;

    private Thread readThread = null;
    private boolean run = false;

    private String deviceName;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setView(R.layout.activity_direct_chat);
        super.onCreate(savedInstanceState);


        Toolbar toolbar = findViewById(R.id.toolbar);
        //创建选项菜单
        toolbar.inflateMenu(R.menu.option_menu2);
        //选项菜单监听
        toolbar.setOnMenuItemClickListener(new MyMenuItemClickListener());
        mTitle = findViewById(R.id.title_left_text);
        mTitle.setText("Interact(p2p)");
        mTitle = findViewById(R.id.title_right_text);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "蓝牙不可用", Toast.LENGTH_LONG).show();
            startActivity(new Intent(DirectInteract.this, HomeActivity.class));
            finish();
        }

    }

    @Override
    protected void initView() {
        bt1 = findViewById(R.id.bt1);
        bt2 = findViewById(R.id.bt2);
        ll = findViewById(R.id.ll);
        bingMapsView = findViewById(R.id.mapView);
    }

    @Override
    protected void initData() {

        // 接收数据线程
        readThread = new Thread(new Runnable() {
            @Override
            public void run() {
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
            }
        });

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        Initialize();
    }

    @Override
    protected void initListener() {
        // 发送需要帮忙寻找的设备给朋友
        bt1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent serverIntent = new Intent(DirectInteract.this, SelectActivity.class);
                startActivityForResult(serverIntent, SELECT_BT);


            }
        });

        // 叫朋友发送帮我寻找到的设备过来
        bt2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("type", "0");
                jsonObject.put("id", APP.getBluetoothName());

                // 叫朋友发送帮我寻找到的设备过来
                sendMessage(jsonObject.toJSONString());

            }
        });
    }




    @Override
    public synchronized void onResume() {
        super.onResume();

    }

    @Override
    public void onBackPressed() {
//        super.onBackPressed();
        startActivity(new Intent(DirectInteract.this, HomeActivity.class));
        finish();
    }

    private void setupChat() {


        mOutStringBuffer = new StringBuffer("");
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
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (SocketHandler.getSocket() != null && SocketHandler.getSocket().isConnected() && SocketHandler.getSocket().getOutputStream() != null){
                        byte[] send = message.getBytes();
                        SocketHandler.getSocket().getOutputStream().write(send);
                        SocketHandler.getSocket().getOutputStream().flush();
                    }else {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(DirectInteract.this, R.string.not_connected, Toast.LENGTH_SHORT).show();
                            }
                        });

                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(DirectInteract.this, R.string.not_connected, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        }).start();


    }


    //使用Handler对象在UI主线程与子线程之间传递消息
    private final Handler mHandler = new Handler() {   //消息处理
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case STATE:
                    switch (msg.arg1) {
                        case BluetoothInteractService.CONNECTED:
//                            mTitle.setText(R.string.title_connected_to);
//                            mTitle.append(mConnectedDeviceName);
                            mTitle.setText(R.string.title_connected_to);
//                            mConversationArrayAdapter.clear();
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
                    // 发送数据
//                    byte[] writeBuf = (byte[]) msg.obj;
//                    String writeMessage = new String(writeBuf);
//                    mConversationArrayAdapter.add("我:  " + writeMessage);


                    break;
                case READ:
                    // 接收到数据
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
                            // 对方获取帮忙寻找到的设备，查找数据库
                            List<DeviceBean> deviceBeans = DeviceBean.find(DeviceBean.class, "(user_id = ? or messenger_id = ?) and find = ?", id, id, "1");
                            JSONObject jsonObject1 = new JSONObject();
                            jsonObject1.put("type", "1");
                            jsonObject1.put("data", deviceBeans);

                            // 发送帮忙寻找到的设备给对方
                            DirectInteract.this.sendMessage(jsonObject1.toJSONString());

                            Toast.makeText(DirectInteract.this, "Send the device to help find to the other party", Toast.LENGTH_LONG).show();

                            break;
                        case "1":
                            // 对方发来帮忙寻找到的设备
                            List<DeviceBean> deviceBeans1 = JSONObject.parseArray(jsonObject.getString("data"), DeviceBean.class);

                            // 保存找到的设备信息
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

                            // 渲染地图
                            addDevice(deviceBeans1);


                            Toast.makeText(DirectInteract.this, "Received the equipment sent by the other party to help find it", Toast.LENGTH_LONG).show();



                            break;
                        case "2":
                            // 对方发来要帮忙寻找的设备，保存到数据库
                            List<DeviceBean> deviceBeans2 = JSONObject.parseArray(jsonObject.getString("data"), DeviceBean.class);
                            for (DeviceBean deviceBean : deviceBeans2){
                                // 查询是否已经保存过
                                List<DeviceBean> deviceBeans3 = DeviceBean.find(DeviceBean.class, "mac = ?", deviceBean.getMac());
                                // 保存过则不再保存
                                if (deviceBeans3.size() == 0){
//                                    // 通过传递着的mac查询朋友自定义名称，因为在配对朋友的时候会把朋友的mac也一起保存，所以可以查到
//                                    List<FriendBean> friendBeans = FriendBean.find(FriendBean.class, "name = ?", deviceBean.getMessengerId());
//                                    if (friendBeans.size() > 0){
//                                        // 在数据库查出设备传递者的自定义名称并设置，传递者即是当前连接者
//                                        deviceBean.setMessengerName(friendBeans.get(0).getUserName());
//                                    }
//
//                                    // 通过拥有者的mac查询朋友自定义名称，因为在配对朋友的时候会把朋友的mac也一起保存，所以可以查到
//                                    List<FriendBean> friendBeans2 = FriendBean.find(FriendBean.class, "name = ?", deviceBean.getUserId());
//                                    if (friendBeans2.size() > 0){
//                                        // 如果我在app配对过设备拥有者，则在数据库取出设备拥有者的备注名称并设置
//                                        deviceBean.setUserName(friendBeans2.get(0).getUserName());
//                                    }

                                    deviceBean.setMe(0);
                                    deviceBean.setLongitude("");
                                    deviceBean.setLatitude("");
                                    deviceBean.setFindTime("");
                                    deviceBean.setFind(0);

                                    // 保存到我的设备
                                    deviceBean.save();
                                }
                            }

                            Toast.makeText(DirectInteract.this, "Received the equipment sent by the other party to help find", Toast.LENGTH_LONG).show();

                            JSONObject jsonObject3 = new JSONObject();
                            jsonObject3.put("type", "3");
                            jsonObject3.put("mac", APP.getBluetoothName());

                            // 回复，收到帮忙寻找的设备
                            DirectInteract.this.sendMessage(jsonObject3.toJSONString());


                            break;
                        case "3":
                            Toast.makeText(DirectInteract.this, "Send a device that needs help finding to a friend", Toast.LENGTH_LONG).show();

                            String mac = jsonObject.getString("mac");
                            List<FriendBean> friendBeans = FriendBean.find(FriendBean.class, "name = ?", mac);
                            if (friendBeans.size() > 0){
                                friendBeans.get(0).setStatus(1);
                                friendBeans.get(0).update();
                            }
                            break;

                    }


//                    mConversationArrayAdapter.add(mConnectedDeviceName + ":  "
//                            + readMessage);

                    break;

            }
        }
    };

    //返回进入好友列表操作后的数回调方法
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE:
                if (resultCode == Activity.RESULT_OK) {
                    mHandler.obtainMessage(BluetoothInteract.STATE, 3, -1).sendToTarget();

                    deviceName = data.getExtras().getString(DirectDeviceList.DEVICE_NAME);



                    run = true;
                    readThread = new Thread(new Runnable() {
                        @Override
                        public void run() {
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

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mTitle.setText(R.string.title_not_connected);
                                }
                            });
                        }
                    });
                    readThread.start();

                } else if (resultCode == Activity.RESULT_CANCELED) {

                    mHandler.obtainMessage(BluetoothInteract.STATE, 0, -1).sendToTarget();

//                    Toast.makeText(this, "未选择任何好友！", Toast.LENGTH_SHORT).show();
                }
                break;
            case REQUEST_ENABLE_BT:
                if (resultCode == Activity.RESULT_OK) {
                    setupChat();
                } else {
                    Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(DirectInteract.this, HomeActivity.class));
                    finish();
                }
            case SELECT_BT:
                if (resultCode == Activity.RESULT_OK) {

                    String datas = data.getExtras().getString("data");
                    List<DeviceBean> deviceBeans = JSONObject.parseArray(datas, DeviceBean.class);

//                    List<DeviceBean> deviceBeans = Select.from(DeviceBean.class).list();
                    // 传递人全部改为自己
                    for (int i = 0; deviceBeans.size() > i; i ++){
                        deviceBeans.get(i).setMessengerId(APP.getBluetoothName());
                        deviceBeans.get(i).setMessengerName(bluetoothAdapter.getName());
                    }
                    JSONObject jsonObject1 = new JSONObject();
                    jsonObject1.put("type", "2");
                    jsonObject1.put("data", deviceBeans);

                    // 发送需要帮忙寻找的设备给朋友
                    DirectInteract.this.sendMessage(jsonObject1.toJSONString());

                } else {
                    Toast.makeText(DirectInteract.this, "No choice send device", Toast.LENGTH_SHORT).show();
                }
        }
    }

    // -------------------------------------地图相关，用了第三方bingMaps地图---------------------------------------
    private void Initialize() {

        _GPSManager = new GPSManager(this, new GPSLocationListener());


        bingMapsView = (BingMapsView) findViewById(R.id.mapView);

        bingMapsView.setMapLoadedListener(new MapLoadedListener() {
            public void onAvailableChecked() {
                // hide splash screen and go to map
//                viewHandler.sendEmptyMessage(0);

                // Add GPS layer
                _gpsLayer = new EntityLayer(Constants.DataLayers.GPS);
                bingMapsView.getLayerManager().addLayer(_gpsLayer);
                UpdateGPSPin();
                updateMarker();
            }
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
    // --------------------------------------地图相关，用了第三方bingMaps地图------------------------------------

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
            p.Title = deviceBeans.get(i).getName();//不设置title属性，不会显示infobox(吹出框)

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

    //内部类，选项菜单的单击事件处理
    private class MyMenuItemClickListener implements Toolbar.OnMenuItemClickListener {
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

                    //启动DirectDeviceList这个Activity
                    Intent serverIntent = new Intent(DirectInteract.this, DirectDeviceList.class);
                    startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
                    return true;
                case R.id.back:
                    startActivity(new Intent(DirectInteract.this, HomeActivity.class));
                    finish();
                    return true;
            }
            return false;
        }
    }

}
