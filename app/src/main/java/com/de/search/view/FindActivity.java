package com.de.search.view;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.os.IBinder;
import android.text.TextUtils;
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

    // Map correlation
    private BingMapsView bingMapsView;
    private GPSManager _GPSManager;
    private EntityLayer _gpsLayer;

    //Declares an operation constant string
    public static final String ACTION_SERVICE_NEED = "action.ServiceNeed";
    //Declare an internal broadcast instance
    public ServiceNeedBroadcastReceiver broadcastReceiver;


    private TextView tvName, tvDistance, tvBack, tvRssi, tvMac, tvStatus, tvNum, tvDetail;
    private Button bt;
    private String name;
    private String mac;
    private int type;
    private int btType;

    private DeviceBean deviceBean;

    private boolean stop = false;



    private Intent sIntent;
    private final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            FindService.FindBinder findBinder = (FindService.FindBinder) service;
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

    @SuppressLint("SetTextI18n")
    @Override
    protected void initData() {

        Initialize();

        /**
         * Register a broadcast instance (at initialization time)
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
        tvBack.setOnClickListener(view -> {
            startToActivity(HomeActivity.class);
            finish();
        });

        //button of 'Find it', click this the service of find will be stopped
        bt.setOnClickListener(view -> {

            if (!stop) {
                unbindService(connection);
                unregisterReceiver(broadcastReceiver);
                stopService(sIntent);
                APP.isFind = false;

                stop = true;

                tvStatus.setText("status：detected");


                deviceBean.setFind(1);
                deviceBean.setFindTime(APP.formatter.format(new Date(System.currentTimeMillis()))); //record the time while finding
                if (APP.location != null) {
                    deviceBean.setLongitude(APP.location.getLongitude() + "");
                    deviceBean.setLatitude(APP.location.getLatitude() + "");
                }

                deviceBean.save();

                startToActivity(HomeActivity.class);
                finish();
            }

        });

        //'Detail' button inside the device's information, click this the rssi and mac info will be displayed
        tvDetail.setOnClickListener(view -> {
            if (tvMac.getVisibility() == View.GONE) {
                tvMac.setVisibility(View.VISIBLE);
                tvRssi.setVisibility(View.VISIBLE);
            } else {
                tvMac.setVisibility(View.GONE);
                tvRssi.setVisibility(View.GONE);
            }
        });
    }

    @Override
    public void onBackPressed() {
        startToActivity(HomeActivity.class);
        finish();
    }

    @Override
    protected void onDestroy() {
        if (!stop) {
            unbindService(connection);
            unregisterReceiver(broadcastReceiver);
            stopService(sIntent);
            APP.isFind = false;
        }

        super.onDestroy();

    }

    private void Initialize() {

        _GPSManager = new GPSManager(this, new FindActivity.GPSLocationListener());

        bingMapsView = (BingMapsView) findViewById(R.id.mapView);

        // Add a map loaded event handler
        bingMapsView.setMapLoadedListener(new MapLoadedListener() {
            public void onAvailableChecked() {

                // Add GPS layer
                _gpsLayer = new EntityLayer(Constants.DataLayers.GPS);
                bingMapsView.getLayerManager().addLayer(_gpsLayer);
                UpdateGPSPin();
                updateMarker();

                if (!TextUtils.isEmpty(deviceBean.getLatitude()) && !TextUtils.isEmpty(deviceBean.getLongitude())) {
                    List<DeviceBean> deviceBeans = new ArrayList<>();
                    deviceBeans.add(deviceBean);
                    addDevice(deviceBeans);
                }
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
        bingMapsView.setCenterAndZoom(coordinate, 15);

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

    private void addDevice(List<DeviceBean> deviceBeans) {
        EntityLayer entityLayer = (EntityLayer) bingMapsView.getLayerManager()
                .getLayerByName(Constants.DataLayers.Search);
        if (entityLayer == null) {
            entityLayer = new EntityLayer(Constants.DataLayers.Search);
        }
        entityLayer.clear();

        for (int i = 0; i < deviceBeans.size(); i++) {
            double longitude = Double.parseDouble(deviceBeans.get(i).getLongitude());
            double latitude = Double.parseDouble(deviceBeans.get(i).getLatitude());

            Coordinate coord = new Coordinate(latitude, longitude);
            // Use Pushpin to mark on the map
            // PushpinOptions is used to set attributes for Pushpin
            // opt.Icon - The icon of PushPin, opt.Anchor - The position to display Pushpin
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

    /**
     * Defines the broadcast sink that performs the requirements of the Service service (inner class)
     */
    private class ServiceNeedBroadcastReceiver extends BroadcastReceiver {
        @SuppressLint("SetTextI18n")
        @Override
        public void onReceive(Context context, Intent intent) {
            //Here is the code to execute in the Activity
            String distance = intent.getStringExtra("distance");
            String rssi = intent.getStringExtra("rssi");
            String status = intent.getStringExtra("status");
            String num = intent.getStringExtra("num");

            if (!TextUtils.isEmpty(distance)){
                deviceBean.setDistance(distance);
                tvDistance.setText("distance(m)：" + distance);
            }

            if (!TextUtils.isEmpty(rssi)){
                deviceBean.setRssi(Integer.parseInt(rssi));
                tvRssi.setText("rssi：" + rssi);
            }

            if (!TextUtils.isEmpty(status))
                tvStatus.setText("status：" + status);
            if (!TextUtils.isEmpty(num))
                tvNum.setText("scan times：" + num);

            // find the device, record the find time and find location of that device
            if ("detected".equals(status)) {
                deviceBean.setRssi(Integer.parseInt(rssi));
                deviceBean.setFind(1);
                deviceBean.setFindTime(APP.formatter.format(new Date(System.currentTimeMillis()))); //record the time while finding
                deviceBean.setDistance(distance);
                if (APP.location != null) {
                    deviceBean.setLongitude(APP.location.getLongitude() + "");
                    deviceBean.setLatitude(APP.location.getLatitude() + "");
                }

                deviceBean.save();

                if (!TextUtils.isEmpty(deviceBean.getLatitude()) && !TextUtils.isEmpty(deviceBean.getLongitude())) {
                    // mark the location while finding
                    //Since the device to be found at this time should be near the phone
                    //thus the geographical location of the phone at this time is the approximate location of the device to be found
                    List<DeviceBean> deviceBeans = new ArrayList<>();
                    deviceBeans.add(deviceBean);
                    addDevice(deviceBeans);
                }
            }
        }
    }
}