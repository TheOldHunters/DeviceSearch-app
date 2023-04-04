package com.de.search.bean;

import com.orm.SugarRecord;

import java.io.Serializable;

public class DeviceBean extends SugarRecord implements Serializable {
    private String name;
    private String mac;
    private String type = "0";  // 0：蓝牙设备，1：wifi设备，2：wifiP2P
    private String btType = "0";  // 0：低功耗蓝牙，1：经典蓝牙
    private int rssi;

    private int me = 0;  // 是否是自己设备   0：不是自己  1：是自己
    private String userId = "";    // 设备拥有者Id（蓝牙名称）   添加到我的设备时会设置
    private String userName = "";  // 设备拥有者名称（蓝牙名称/自定义名称）   添加到我的设备时会设置
    private String messengerId = "";   // 设备传递者Id（蓝牙名称）  传递给朋友时会设置
    private String messengerName = "";   // 设备传递者名称（蓝牙名称/自定义名称）   传递给朋友时会设置
    private int find = 0;   // 0：没找到  1：找到
    private String findTime = "";   // 找到到时间
    private String longitude = "";   // 经度
    private String latitude = "";   // 纬度
    private String findPlace = "";   // 地点

    private boolean c;

    public DeviceBean() {
    }

    public DeviceBean(String name, String mac, String type, String btType, int rssi, int me, String userId, String userName, String messengerId, String messengerName, int find, String findTime, String longitude, String latitude, String findPlace) {
        this.name = name;
        this.mac = mac;
        this.type = type;
        this.btType = btType;
        this.rssi = rssi;
        this.me = me;
        this.userId = userId;
        this.userName = userName;
        this.messengerId = messengerId;
        this.messengerName = messengerName;
        this.find = find;
        this.findTime = findTime;
        this.longitude = longitude;
        this.latitude = latitude;
        this.findPlace = findPlace;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMac() {
        return mac;
    }

    public void setMac(String mac) {
        this.mac = mac;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getRssi() {
        return rssi;
    }

    public void setRssi(int rssi) {
        this.rssi = rssi;
    }

    public String getBtType() {
        return btType;
    }

    public void setBtType(String btType) {
        this.btType = btType;
    }



    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getMessengerId() {
        return messengerId;
    }

    public void setMessengerId(String messengerId) {
        this.messengerId = messengerId;
    }

    public String getMessengerName() {
        return messengerName;
    }

    public void setMessengerName(String messengerName) {
        this.messengerName = messengerName;
    }

    public int getMe() {
        return me;
    }

    public void setMe(int me) {
        this.me = me;
    }

    public int getFind() {
        return find;
    }

    public void setFind(int find) {
        this.find = find;
    }

    public String getFindTime() {
        return findTime;
    }

    public void setFindTime(String findTime) {
        this.findTime = findTime;
    }

    public String getLongitude() {
        return longitude;
    }

    public void setLongitude(String longitude) {
        this.longitude = longitude;
    }

    public String getLatitude() {
        return latitude;
    }

    public void setLatitude(String latitude) {
        this.latitude = latitude;
    }

    public String getFindPlace() {
        return findPlace;
    }

    public void setFindPlace(String findPlace) {
        this.findPlace = findPlace;
    }

    public boolean isC() {
        return c;
    }

    public void setC(boolean c) {
        this.c = c;
    }
}
