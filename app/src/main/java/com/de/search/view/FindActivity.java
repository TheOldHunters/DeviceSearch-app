package com.de.search.view;

import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.location.Location;
import android.location.LocationListener;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.de.search.R;
import com.de.search.app.APP;
import com.de.search.base.BaseActivity;
import com.de.search.bean.DeviceBean;
import com.de.search.service.FindService;
import com.de.search.util.maps.Constants;
import com.de.search.util.maps.GPSManager;
//import com.inuker.bluetooth.library.connect.listener.BluetoothStateListener;
//import com.inuker.bluetooth.library.search.SearchRequest;
//import com.inuker.bluetooth.library.search.SearchResult;
//import com.inuker.bluetooth.library.search.response.SearchResponse;

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
import java.util.Date;
import java.util.List;

public class FindActivity extends BaseActivity {

    // 地图相关
    private BingMapsView bingMapsView;
    private GPSManager _GPSManager;
    private EntityLayer _gpsLayer;

    //声明一个操作常量字符串
    public static final String ACTION_SERVICE_NEED = "action.ServiceNeed";
    //声明一个内部广播实例
    public ServiceNeedBroadcastReceiver broadcastReceiver;

    private FindService.FindBinder findBinder;


    private TextView tvName, tvDistance, tvBack, tvRssi, tvMac, tvStatus, tvNum, tvDetail;
    private Button bt;
    private String name;
    private String mac;
    private int type;
    private int btType;
    
    private DeviceBean deviceBean;

    private boolean stop = false;


    private Intent sIntent;
    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            findBinder = (FindService.FindBinder) service;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setView(R.layout.activity_find);
        super.onCreate(savedInstanceState);

    }

    @Override
    protected void initView() {
        tvName = findViewById(R.id.tv_name);
        tvDistance = findViewById(R.id.tv_distance);
        tvBack = findViewById(R.id.tv_back);
        tvRssi = findViewById(R.id.tv_rssi);
        tvMac = findViewById(R.id.tv_mac);
        tvStatus = findViewById(R.id.tv_status);
        tvNum = findViewById(R.id.tv_num);
        tvDetail = findViewById(R.id.tv_detail);
        bt = findViewById(R.id.bt);
    }

    @Override
    protected void initData() {

        Initialize();

        /**
         * 注册广播实例（在初始化的时候）
         */
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_SERVICE_NEED);
        broadcastReceiver = new ServiceNeedBroadcastReceiver();
        registerReceiver(broadcastReceiver, filter);


        Intent intent = getIntent();
        name = intent.getStringExtra("name");
        mac = intent.getStringExtra("mac");
        type = intent.getIntExtra("type", 0);
        btType = intent.getIntExtra("btType", 0);
        deviceBean = (DeviceBean) intent.getSerializableExtra("data");
        Long id = intent.getLongExtra("id", 0);
        deviceBean.setId(id);
        tvName.setText("name：" + name);
        tvMac.setText("mac：" + mac);

        sIntent = new Intent(this, FindService.class);
        sIntent.putExtra("name", name);
        sIntent.putExtra("mac", mac);
        sIntent.putExtra("type", type);
        sIntent.putExtra("btType", btType);
        startService(sIntent);
        bindService(sIntent, connection, BIND_AUTO_CREATE);


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

        bt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                if (from.equals("0")){
//                    startToActivity(HomeActivity.class);
//                }else {
//                    startToActivity(UserDeviceActivity.class);
//                }
//                finish();

                if (!stop){
                    unbindService(connection);
                    unregisterReceiver(broadcastReceiver);
                    stopService(sIntent);
                    APP.isFind = false;

                    stop = true;
                }

            }
        });

        tvDetail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (tvMac.getVisibility() == View.GONE){
                    tvMac.setVisibility(View.VISIBLE);
                    tvRssi.setVisibility(View.VISIBLE);
                }else {
                    tvMac.setVisibility(View.GONE);
                    tvRssi.setVisibility(View.GONE);
                }
            }
        });
    }

    @Override
    public void onBackPressed() {
//        super.onBackPressed();
        startToActivity(HomeActivity.class);
        finish();
    }

    @Override
    protected void onDestroy() {
        if (!stop){
            unbindService(connection);
            unregisterReceiver(broadcastReceiver);
            stopService(sIntent);
            APP.isFind = false;
        }


        super.onDestroy();
//        APP.mClient.stopSearch();

    }

    private void Initialize() {

        _GPSManager = new GPSManager(this, new FindActivity.GPSLocationListener());

        // Add more data layers here
//        _dataLayers = new String[]{getString(R.string.traffic)};
//        _dataLayerSelections = new boolean[_dataLayers.length];
//
//        _loadingScreen = new ProgressDialog(this);
//        _loadingScreen.setCancelable(false);
//        _loadingScreen.setMessage(this.getString(R.string.loading) + "...");

        bingMapsView = (BingMapsView) findViewById(R.id.mapView);

        // Create handler to switch out of Splash screen mode
//        final Handler viewHandler = new Handler() {
//            public void handleMessage(Message msg) {
//                ((ViewFlipper) findViewById(R.id.flipper)).setDisplayedChild(1);
//            }
//        };

        // Add a map loaded event handler
        bingMapsView.setMapLoadedListener(new MapLoadedListener() {
            public void onAvailableChecked() {
                // hide splash screen and go to map
//                viewHandler.sendEmptyMessage(0);

                // Add GPS layer
                _gpsLayer = new EntityLayer(Constants.DataLayers.GPS);
                bingMapsView.getLayerManager().addLayer(_gpsLayer);
                UpdateGPSPin();
                updateMarker();

                if (!TextUtils.isEmpty(deviceBean.getLatitude()) && !TextUtils.isEmpty(deviceBean.getLongitude())){
                    List<DeviceBean> deviceBeans = new ArrayList<>();
                    deviceBeans.add(deviceBean);
                    addDevice(deviceBeans);
                }
            }
        });

        // Add a entity clicked event handler
//        bingMapsView.setEntityClickedListener(new EntityClickedListener() {
//            public void onAvailableChecked(String layerName, int entityId) {
//                HashMap<String, Object> metadata = bingMapsView
//                        .getLayerManager().GetMetadataByID(layerName, entityId);
//                DialogLauncher.LaunchEntityDetailsDialog(_baseActivity,
//                        metadata);
//            }
//        });

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
        // EntityLayer is used for map overlay
        EntityLayer entityLayer = (EntityLayer) bingMapsView.getLayerManager()
                .getLayerByName(Constants.DataLayers.Search);
        if (entityLayer == null) {
            entityLayer = new EntityLayer(Constants.DataLayers.Search);
        }
        entityLayer.clear();
        // Use Pushpin to mark on the map
        // PushpinOptions is used to set attributes for Pushpin
        // opt.Icon - The icon of PushPin, opt.Anchor - The position to display Pushpin
        PushpinOptions opt = new PushpinOptions();
        opt.Icon = Constants.PushpinIcons.RedFlag;
        opt.Width = 20;
        opt.Height = 35;
        opt.Anchor = new Point(11, 10);

        // Add the entityLayer to mapView's LayerManager
        bingMapsView.getLayerManager().addLayer(entityLayer);
        entityLayer.updateLayer();

        // set the center location and zoom level of map
        Coordinate coordinate = _GPSManager.GetCoordinate();
        bingMapsView.setCenterAndZoom(coordinate,15);

        // Polyline used to draw lines on the MapView
        // PolylineOptions have multiple attributes for the line
        // polylineOptions.StrokeThickness
        // polylineOptions.StrokeColor
        Polyline routeLine = new Polyline(listCoord);
        PolylineOptions polylineOptions = new PolylineOptions();
        polylineOptions.StrokeThickness = 3;
        routeLine.Options = polylineOptions;
        entityLayer.add(routeLine);
    }

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
            // 实现标记必须用到 Pushpin 来做标记。
            // PushpinOptions可以对 Pushpin所要标记的设置属性
            // opt.Icon图标 opt.Anchor点的位置
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

    /**
     * 定义广播接收器，用于执行Service服务的需求（内部类）
     */
    private class ServiceNeedBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            //这里是要在Activity活动里执行的代码
            String distance = intent.getStringExtra("distance");
            String rssi = intent.getStringExtra("rssi");
            String status = intent.getStringExtra("status");
            String num = intent.getStringExtra("num");

            if (!TextUtils.isEmpty(distance))
                tvDistance.setText("distance(m)：" + distance);
            if (!TextUtils.isEmpty(rssi))
                tvRssi.setText("rssi：" + rssi);
            if (!TextUtils.isEmpty(status))
                tvStatus.setText("status：" + status);
            if (!TextUtils.isEmpty(num))
                tvNum.setText("scan times：" + num);

            // 找到
            if ("detected".equals(status)){
                deviceBean.setRssi(Integer.parseInt(rssi));
                deviceBean.setFind(1);
                deviceBean.setFindTime(APP.formatter.format(new Date(System.currentTimeMillis())));
                if (APP.location != null){
                    deviceBean.setLongitude(APP.location.getLongitude()+"");
                    deviceBean.setLatitude(APP.location.getLatitude()+"");
                }

                deviceBean.save();

                if (!TextUtils.isEmpty(deviceBean.getLatitude()) && !TextUtils.isEmpty(deviceBean.getLongitude())){
                    // 标记位置
                    List<DeviceBean> deviceBeans = new ArrayList<>();
                    deviceBeans.add(deviceBean);
                    addDevice(deviceBeans);
                }
            }

        }
    }


}