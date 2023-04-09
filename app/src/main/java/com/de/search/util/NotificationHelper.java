package com.de.search.util;


import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.de.search.R;

import java.text.SimpleDateFormat;

public class NotificationHelper {
    private static final String CHANNEL_ID="channel_id";   //Channel channel id
    public static final String  CHANEL_NAME="chanel_name"; //Channel name
    @SuppressLint("SimpleDateFormat")
    private static final SimpleDateFormat sdf=new SimpleDateFormat("HH:mm:ss");


    @TargetApi(Build.VERSION_CODES.O)
    public static  void  show(Context context){
        NotificationChannel channel = null;
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O){
            //Creating the channel channelid and channelname is mandatory
            channel = new NotificationChannel(CHANNEL_ID, CHANEL_NAME, NotificationManager.IMPORTANCE_DEFAULT);
            channel.enableLights(true);//Whether to display a small red dot in the upper right corner of the icon on the desktop
            channel.setLightColor(Color.GREEN);//Little red dot color
            channel.setShowBadge(false); //Whether to display notifications for this channel when long pressed on the desktop icon
        }
        Notification notification;

        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O){

            notification = new Notification.Builder(context,CHANNEL_ID)
                    .setContentTitle("channel name 1")
                    .setContentText("info1")
                    .setWhen(System.currentTimeMillis())
                    .setSmallIcon(R.drawable.ic_launcher_background)
                    .setColor(Color.parseColor("#FEDA26"))
                    .setLargeIcon(BitmapFactory.decodeResource(context.getResources(),R.mipmap.ic_launcher))
                    .setTicker("gate1")
                    .build();
        }else {

            notification = new NotificationCompat.Builder(context)
                    .setContentTitle("channel name 1")
                    .setContentText("info 1")
                    .setWhen(System.currentTimeMillis())
                    .setSmallIcon(R.drawable.ic_launcher_background)
                    .setColor(Color.parseColor("#FEDA26"))
                    .setLargeIcon(BitmapFactory.decodeResource(context.getResources(),R.mipmap.ic_launcher))
                    .setTicker("gate 1")
                    .build();

        }
        notification.flags=Notification.FLAG_AUTO_CANCEL;//cancel

        //send notification
        int  notifiId=1;
        //Create a notification manager
        NotificationManager   notificationManager= (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O){
            notificationManager.createNotificationChannel(channel);
        }
        notificationManager.notify(notifiId,notification);
    }
}


