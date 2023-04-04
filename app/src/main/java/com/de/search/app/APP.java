package com.de.search.app;


import android.Manifest;
import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.provider.Settings;
import android.text.TextUtils;

import androidx.core.content.ContextCompat;

import com.de.search.util.LocalStorageUtils;
import com.inuker.bluetooth.library.BluetoothClient;
import com.orm.SugarContext;

import java.text.SimpleDateFormat;
import java.util.List;


public class APP extends Application {


    private static Context context;

    public static int type = 0; // 0：蓝牙，1：wifi，2：wifip2p
    public static int distance = 1; // 提醒距离
    public static int bluetoothType = 1; // 0：低功耗蓝牙，1：经典蓝牙
    public static String pin = "";

    public static boolean isFind = false;

    public static BluetoothClient mClient;


    public static SimpleDateFormat formatter = new SimpleDateFormat("YYYY-MM-dd HH:mm:ss");

    public static BluetoothAdapter bluetoothAdapter;

    public static Location location;

    public static Context getInstance() {
        return context;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        context = this;
        mClient = new BluetoothClient(this);

        type = (int) LocalStorageUtils.getParam(this, "type", 0);
        distance = (int) LocalStorageUtils.getParam(this, "distance", 1);
        bluetoothType = (int) LocalStorageUtils.getParam(this, "bluetoothType", 1);
        pin = (String) LocalStorageUtils.getParam(this, "pin", "");

        SugarContext.init(this);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    public static int getType() {
        return type;
    }

    public static void setType(int type) {
        APP.type = type;
    }

    public static int getDistance() {
        return distance;
    }

    public static void setDistance(int distance) {
        APP.distance = distance;
    }

    public static int getBluetoothType() {
        return bluetoothType;
    }

    public static void setBluetoothType(int bluetoothType) {
        APP.bluetoothType = bluetoothType;
    }

    public static String getPin() {
        return pin;
    }

    public static void setPin(String pin) {
        APP.pin = pin;
    }

    public static String getAndroidId(Context context) {
        try {
            return Settings.Secure.getString(context.getContentResolver(),
                    Settings.Secure.ANDROID_ID);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return "";
    }

    public static String getBluetoothName() {

        return bluetoothAdapter.getName();
    }


    public static Location getLastKnownLocation() {
        //获取地理位置管理器
        LocationManager mLocationManager = (LocationManager) APP.getInstance().getSystemService(LOCATION_SERVICE);
        if (ContextCompat.checkSelfPermission(APP.getInstance(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(APP.getInstance(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO:去请求权限后再获取
            return null;
        }
        List<String> providers = mLocationManager.getProviders(true);
        Location bestLocation = null;
        for (String provider : providers) {
            Location l = mLocationManager.getLastKnownLocation(provider);
            if (l == null) {
                continue;
            }
            if (bestLocation == null || l.getAccuracy() < bestLocation.getAccuracy()) {
                bestLocation = l;
            }
        }
        // 在一些手机5.0(api21)获取为空后，采用下面去兼容获取。
        if (bestLocation==null){
            Criteria criteria = new Criteria();
            criteria.setAccuracy(Criteria.ACCURACY_COARSE);
            criteria.setAltitudeRequired(false);
            criteria.setBearingRequired(false);
            criteria.setCostAllowed(true);
            criteria.setPowerRequirement(Criteria.POWER_LOW);
            String provider = mLocationManager.getBestProvider(criteria, true);
            if (!TextUtils.isEmpty(provider)){
                bestLocation = mLocationManager.getLastKnownLocation(provider);
            }
        }
        return bestLocation;
    }

    public static BluetoothAdapter getBluetoothAdapter() {
        return bluetoothAdapter;
    }

    public static void setBluetoothAdapter(BluetoothAdapter bluetoothAdapter) {
        APP.bluetoothAdapter = bluetoothAdapter;
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        SugarContext.terminate();
    }



}
