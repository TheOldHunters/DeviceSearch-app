package com.de.search.view;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.de.search.R;
import com.de.search.adapter.LocationDeviceRecycleViewAdapter;
import com.de.search.adapter.SelectDeviceRecycleViewAdapter;
import com.de.search.app.APP;
import com.de.search.base.BaseActivity;
import com.de.search.bean.DeviceBean;
import com.de.search.bean.DeviceLocationBean;
import com.de.search.util.Trilateration;
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
import java.util.Random;

public class TriangulationActivity extends BaseActivity implements LocationDeviceRecycleViewAdapter.LocationDeviceRecycleViewAdapterInterface {

    // Map correlation
    private BingMapsView bingMapsView;
    private GPSManager _GPSManager;
    private EntityLayer _gpsLayer;


    private TextView tvBack, tvView;

    private RecyclerView mRecycleView;
    private LocationDeviceRecycleViewAdapter mAdapter;//adapter
    private List<DeviceLocationBean> deviceBeanList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setView(R.layout.activity_triangulation);
        super.onCreate(savedInstanceState);

    }

    @Override
    protected void initView() {
        tvBack = findViewById(R.id.tv_back);
        mRecycleView = findViewById(R.id.rv_list);
        tvView = findViewById(R.id.tv_view);
    }

    @Override
    protected void initData() {
        Initialize();

        deviceBeanList = DeviceLocationBean.listAll(DeviceLocationBean.class);

        //Create a layout manager, set vertically in 'LinearLayoutManager.VERTICAL'; set horizontally in 'LinearLayoutManager.HORIZONTAL'
        LinearLayoutManager mLinearLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        //Create the adapter and pass the data to the adapter
        mAdapter = new LocationDeviceRecycleViewAdapter(deviceBeanList);
        mAdapter.setLocationDeviceRecycleViewAdapterInterface(this);
        //Set up the layout manager
        mRecycleView.setLayoutManager(mLinearLayoutManager);
        //Setup adapter
        mRecycleView.setAdapter(mAdapter);
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

        tvView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                List<DeviceLocationBean> deviceLocationBeans = mAdapter.getDeviceBeanList();
                if (deviceLocationBeans.size() != 3) {
                    showToast("Please select three devices");
                    return;
                }

                if (!(deviceLocationBeans.get(0).getMac().equals(deviceLocationBeans.get(1).getMac()) && deviceLocationBeans.get(0).getMac().equals(deviceLocationBeans.get(2).getMac()))){
                    showToast("To select the same device");
                    return;
                }

                try {
                    Location location = Trilateration.getLocationByTrilateration(deviceLocationBeans.get(0).getLatitude(), deviceLocationBeans.get(0).getLongitude(), deviceLocationBeans.get(0).getRssi(),
                            deviceLocationBeans.get(1).getLatitude(), deviceLocationBeans.get(1).getLongitude(), deviceLocationBeans.get(1).getRssi(),
                            deviceLocationBeans.get(2).getLatitude(), deviceLocationBeans.get(2).getLongitude(), deviceLocationBeans.get(2).getRssi());


                    addDevice(location.getLongitude(), location.getLatitude(), deviceLocationBeans);


                    Log.e("getLatitude", String.valueOf(location.getLatitude()));
                    Log.e("getLongitude", String.valueOf(location.getLongitude()));

                    showToast("Calculation successful");

                } catch (Exception e) {
                    e.printStackTrace();
                    showToast("Calculation failed");
                }


            }
        });
    }

    @Override
    public void onBackPressed() {
        startToActivity(HomeActivity.class);
        finish();
    }

    private void Initialize() {

        _GPSManager = new GPSManager(this, new GPSLocationListener());

        bingMapsView = (BingMapsView) findViewById(R.id.mapView);

        // Add a map loaded event handler
        bingMapsView.setMapLoadedListener(new MapLoadedListener() {
            public void onAvailableChecked() {

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

    private void addDevice(double longitude, double latitude, List<DeviceLocationBean> deviceBeans) {
        EntityLayer entityLayer = (EntityLayer) bingMapsView.getLayerManager()
                .getLayerByName(Constants.DataLayers.Search);
        if (entityLayer == null) {
            entityLayer = new EntityLayer(Constants.DataLayers.Search);
        }
        entityLayer.clear();

        for (int i = 0; i < deviceBeans.size(); i++){
            double longitude1 = Double.parseDouble(deviceBeans.get(i).getLongitude());
            double latitude1 = Double.parseDouble(deviceBeans.get(i).getLatitude());

            Coordinate coord = new Coordinate(latitude1, longitude1);

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


        Coordinate coord = new Coordinate(latitude, longitude);
        // Use Pushpin to mark on the map
        // PushpinOptions is used to set attributes for Pushpin
        // opt.Icon - The icon of PushPin, opt.Anchor - The position to display Pushpin
        PushpinOptions opt = new PushpinOptions();
        opt.Icon = "file:///android_asset/pin_red_flag.png";
        opt.Width = 20;
        opt.Height = 20;
        opt.Anchor = new Point(11, 10);
        opt.Text = deviceBeans.get(0).getName();
        Pushpin p = new Pushpin(coord, opt);
        p.Title = deviceBeans.get(0).getName();

        entityLayer.add(p);


        bingMapsView.getLayerManager().addLayer(entityLayer);
        entityLayer.updateLayer();

    }

    @Override
    public void onLongClick(int position) {
        @SuppressLint("NotifyDataSetChanged") AlertDialog alertDialog = new AlertDialog.Builder(this)
                //title
                .setTitle("Delete device location")
                //content
                .setMessage("Delete " + deviceBeanList.get(position).getName() + " to my location")
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
                .create();
        alertDialog.show();
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
}