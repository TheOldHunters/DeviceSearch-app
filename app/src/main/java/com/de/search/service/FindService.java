package com.de.search.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioAttributes;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
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
import com.de.search.view.FindActivity;
import com.inuker.bluetooth.library.beacon.Beacon;
import com.inuker.bluetooth.library.connect.listener.BluetoothStateListener;
import com.inuker.bluetooth.library.search.SearchRequest;
import com.inuker.bluetooth.library.search.SearchResult;
import com.inuker.bluetooth.library.search.response.SearchResponse;

import java.util.ArrayList;
import java.util.List;

public class FindService extends Service {

    private PowerManager.WakeLock wakeLock = null;

    private String name;
    private String mac;


    private FindBinder binder = new FindBinder();

    private RemoteViews remoteViews;


    private AudioAttributes audioAttributes;
    private NotificationManager notificationManager;
    private Notification notification;

    // 是否找到
    private boolean find;
    // 蓝牙
    private SearchRequest searchRequest;
    // 蓝牙开关
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
     * 创建Binder对象，返回给客户端即Activity使用，提供数据交换的接口
     */
    public class FindBinder extends Binder {
        // 声明一个方法，getService。（提供给客户端调用）
        FindService getService() {
            // 返回当前对象LocalService,这样我们就可在客户端端调用Service的公共方法了
            return FindService.this;
        }
    }

    /**
     * 把Binder类返回给客户端
     */
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }


    /**
     * 首次创建服务时，系统将调用此方法来执行一次性设置程序（在调用 onStartCommand() 或 onBind() 之前）。
     * 如果服务已在运行，则不会调用此方法。该方法只被调用一次
     */
    @Override
    public void onCreate() {
        System.out.println("onCreate invoke");
        APP.isFind = true;
        audioAttributes = new AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ALARM) // 源码中isAlarm判断可通过
                .build();

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, FindService.class.getName());
        wakeLock.acquire();
        super.onCreate();
    }

    /**
     * 每次通过startService()方法启动Service时都会被回调。
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
     * 服务销毁时的回调
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



    // 扫描蓝牙
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
                    .searchBluetoothLeDevice(5000, 1) // 先扫 BLE 设备 1 次，每次 5s
                    .searchBluetoothClassicDevice(5000) // 再扫经典蓝牙 5s
                    .build();
        }
        APP.mClient.search(searchRequest, new SearchResponse() {
            @Override
            public void onSearchStarted() {
                find = false;
            }

            @Override
            public void onDeviceFounded(SearchResult device) {
                if (find){
                    return;
                }

                BluetoothDevice bluetoothDevice = device.device;
                if (TextUtils.isEmpty(bluetoothDevice.getName()) || bluetoothDevice.getName().equals("NULL") || device.rssi == 0 || TextUtils.isEmpty(bluetoothDevice.getAddress())) {   //加入到list中
                    return;
                }

                if (bluetoothDevice.getAddress().equals(mac)) {
                    float d = (float) Math.pow(10, ((Math.abs(device.rssi) - 60) / (10 * 2.0f)));
                    int i = (int) (d * 100);
                    d = (float) i / 100;

                    sendData1(String.valueOf(d), String.valueOf(device.rssi), "detected");
//                    scan();
                    find = true;

                    Log.e("", "找到了");
                    Log.e("getRssi", String.valueOf(device.rssi));
                }
            }

            @Override
            public void onSearchStopped() {
                Log.e("onSearchStopped","onSearchStopped");

                if (!find){
                    // 没找到
                    sendData1("", "", "not detected");
                }

                // 循环扫
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

        if (status.equals("detected")) {
            APP.location = APP.getLastKnownLocation();
        }

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

    // 创建通知栏
    public void createMusicNotification(Context context) {

        notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        //android 8.0的判断、需要加入NotificationChannel
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("qqq", "qqq",
                    NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "qqq");
        //自定义布局必须加上、否则布局会有显示问题、可以自己try try
        builder.setSmallIcon(R.mipmap.ic_launcher);
        builder.setOngoing(true);//代表是常驻的，主要是配合服务

        remoteViews = new RemoteViews(context.getPackageName(), R.layout.item_notice);

        remoteViews.setTextViewText(R.id.tv_name, "name：" + name);
        remoteViews.setTextViewText(R.id.tv_mac, "mac：" + mac);

        //自定义点击事件、会在Service. onStartCommand中回调
//        Intent stopIntent = new Intent(context, MediaService.class);
//        stopIntent.setAction(STOP_PLAY_SERVICE);
//
//        PendingIntent startOrPauseP = PendingIntent.getService(context, MediaService.RELEASE, stopIntent, 0);
//        remoteViews.setOnClickPendingIntent(R.id.ivStop, startOrPauseP);
        builder.setContent(remoteViews);
        notification = builder.build();
        //0x11 为通知id 自定义可
        notificationManager.notify(0x11, notification);

    }



}