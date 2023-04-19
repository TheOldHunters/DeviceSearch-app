package com.de.search.service;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.Vibrator;
import android.text.TextUtils;
import android.util.Log;
import android.widget.RemoteViews;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.de.search.R;
import com.de.search.app.APP;
import com.de.search.util.RssiAlgorithm;
import com.de.search.view.FindActivity;
import com.inuker.bluetooth.library.connect.listener.BluetoothStateListener;
import com.inuker.bluetooth.library.search.SearchRequest;
import com.inuker.bluetooth.library.search.SearchResult;
import com.inuker.bluetooth.library.search.response.SearchResponse;

import java.text.DecimalFormat;

//This is the core code of Bluetooth search function of this app, which realizes dual-mode ble and bt search function by importing inuker bluetoothkit library
//https://github.com/dingjikerbo/Android-BluetoothKit

public class FindService extends Service {

    private PowerManager.WakeLock wakeLock = null;

    private String name;
    private String mac;

    private FindBinder binder = new FindBinder();

    private RemoteViews remoteViews;
    private Vibrator vibrator;
    private long[] pattern = {100, 200, 100, 200};

    private AudioAttributes audioAttributes;
    private NotificationManager notificationManager;
    private Notification notification;

    // Whether to find it
    private boolean find;
    // bluetooth
    private SearchRequest searchRequest;
    // Bluetooth switch
    private final BluetoothStateListener mBluetoothStateListener = new BluetoothStateListener() {
        @Override
        public void onBluetoothStateChanged(boolean openOrClosed) {
            if (openOrClosed) {
                scan();
            }
        }

    };


    private int i = 0;


    /**
     * Create a Binder object that is returned to the client and used by the Activity to provide an interface for data exchange
     */
    public class FindBinder extends Binder {
        // Declare a method: getService.(Provided to client call)
        FindService getService() {
            // Returns the current object -- LocalService, so we can call the public method of Service on the client side
            return FindService.this;
        }
    }

    /**
     * Return the Binder class to the client
     */
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }


    /**
     * When the service is first created, the system calls this method to perform a one-time setup routine (before calling onStartCommand() or onBind()).
     * This method is not called if the service is already running. This method is called only once
     */
    @Override
    public void onCreate() {
        System.out.println("onCreate invoke");
        APP.isFind = true;
        vibrator = (Vibrator) getSystemService(Service.VIBRATOR_SERVICE);
        audioAttributes = new AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ALARM) // isAlarm judgment in the source code can be passed
                .build();

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, FindService.class.getName());
        wakeLock.acquire();
        super.onCreate();
    }

    /**
     * The Service is called back each time it is started through the startService() method.
     *
     * @param intent
     * @param flags
     * @param startId
     * @return
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        System.out.println("onStartCommand invoke");

        name = intent.getStringExtra("name");
        mac = intent.getStringExtra("mac");


        createMusicNotification(this);

        APP.mClient.registerBluetoothStateListener(mBluetoothStateListener);

        if (APP.mClient.isBluetoothOpened()) {
            scan();
        } else {
            APP.mClient.openBluetooth();
        }


        return super.onStartCommand(intent, flags, startId);
    }

    /**
     * The callback when the service is destroyed
     */
    @Override
    public void onDestroy() {
        Log.e("", "==================onDestroy invoke=======================");
        APP.isFind = false;


        notificationManager.cancel(0x11);


        APP.mClient.stopSearch();

        if (wakeLock != null) {
            wakeLock.release();
            wakeLock = null;
        }

        super.onDestroy();
    }


    // Scanning bluetooth
    public void scan() {
        if (!APP.isFind) {
            return;
        }

        if (!APP.mClient.isBluetoothOpened()) {
            APP.mClient.openBluetooth();
            return;
        }

        find = false;

        i += 1;
        sendData2(String.valueOf(i));
        Log.e("tvNum", "" + i);

        if (searchRequest == null) {
            searchRequest = new SearchRequest.Builder()
                    .searchBluetoothLeDevice(1000, 1) // Scan the BLE device once for 1s each time
                    .searchBluetoothClassicDevice(1000) // Then scan the classic Bluetooth 1s
                    .build();
        }
        APP.mClient.search(searchRequest, new SearchResponse() {
            @Override
            public void onSearchStarted() {
                find = false;
            }

            @SuppressLint("MissingPermission")
            @Override
            public void onDeviceFounded(SearchResult device) {
                if (find){
                    return;
                }

                BluetoothDevice bluetoothDevice = device.device;
                if (TextUtils.isEmpty(bluetoothDevice.getName()) || bluetoothDevice.getName().equals("NULL") || device.rssi == 0 || TextUtils.isEmpty(bluetoothDevice.getAddress())) {   //Add it to the list
                    return;
                }

                if (bluetoothDevice.getAddress().equals(mac)) {
                    float d = 0;

                    switch (APP.algorithm){
                        case 0:
                            d = RssiAlgorithm.calculateDistance1(device.rssi);
                            break;
                        case 1:
                            d = RssiAlgorithm.calculateDistance2(device.rssi);
                            break;
                        case 2:
                            d = RssiAlgorithm.calculateDistance3(device.rssi);
                            break;
                    }
                    DecimalFormat df = new DecimalFormat("0.000"); //Accurate to three decimal places
                    d = Float.parseFloat(df.format(d));

                    APP.location = APP.getLastKnownLocation();

                    if (APP.getDistance() >= d){
                        sendData1(String.valueOf(d), String.valueOf(device.rssi), "detected");
                        //Vibrations
                    }else {
                        sendData1(String.valueOf(d), String.valueOf(device.rssi), "not detected");
                    }


                    find = true;

                    Log.e("", "find it");
                    Log.e("getRssi", String.valueOf(device.rssi)); //record rssi value
                }
            }

            @Override
            public void onSearchStopped() {
                Log.e("onSearchStopped","onSearchStopped");

                if (!find){
                    // not find it
                    sendData1("", "", "not detected");
                }

                // Loop of scanning
                scan();

            }

            @Override
            public void onSearchCanceled() {
                Log.e("onSearchCanceled","onSearchCanceled");
            }

        });

    }


    private void sendData1(String distance, String rssi, String status) {

        if (!APP.isFind) {
            return;
        }

        if (!TextUtils.isEmpty(distance) && APP.distance >= Float.parseFloat(distance)){
            if (APP.isOpenVibrator()){
                vibrator.vibrate(pattern, -1, audioAttributes);
            }

        }

//        if (status.equals("detected")) {
//            APP.location = APP.getLastKnownLocation();
//        }

        if (!TextUtils.isEmpty(distance))
            remoteViews.setTextViewText(R.id.tv_distance, "distance(m)：" + distance);
        if (!TextUtils.isEmpty(rssi))
            remoteViews.setTextViewText(R.id.tv_rssi, "rssi：" + rssi);
        if (!TextUtils.isEmpty(status))
            remoteViews.setTextViewText(R.id.tv_status, "status：" + status);
        notificationManager.notify(0x11, notification);

        Intent intentBroadcastReceiver = new Intent();
        intentBroadcastReceiver.setAction(FindActivity.ACTION_SERVICE_NEED);
        intentBroadcastReceiver.putExtra("distance", distance);
        intentBroadcastReceiver.putExtra("rssi", rssi);
        intentBroadcastReceiver.putExtra("status", status);

        sendBroadcast(intentBroadcastReceiver);
    }

    private void sendData2(String num) {

        if (!APP.isFind) {
            return;
        }

        remoteViews.setTextViewText(R.id.tv_num, "scan times：" + num);
        notificationManager.notify(0x11, notification);

        Intent intentBroadcastReceiver = new Intent();
        intentBroadcastReceiver.setAction(FindActivity.ACTION_SERVICE_NEED);
        intentBroadcastReceiver.putExtra("num", num);

        sendBroadcast(intentBroadcastReceiver);
    }

    // Create notification bar
    public void createMusicNotification(Context context) {

        notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        //For android 8.0, the NotificationChannel needs to be added
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("qqq", "qqq",
                    NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "qqq");

        builder.setSmallIcon(R.mipmap.ic_launcher);
        builder.setOngoing(true);//resident

        remoteViews = new RemoteViews(context.getPackageName(), R.layout.item_notice);

        remoteViews.setTextViewText(R.id.tv_name, "name：" + name);
        remoteViews.setTextViewText(R.id.tv_mac, "mac：" + mac);


        builder.setContent(remoteViews);
        notification = builder.build();
        //0x11 is the notification id
        notificationManager.notify(0x11, notification);

    }



}