package com.de.search.bean;

import com.orm.SugarRecord;

import java.io.Serializable;

public class DeviceBean extends SugarRecord implements Serializable {
    private String name;
    private String mac;
    private String type = "0";  // 0: Bluetooth device, 1: wifi device, 2: wifiP2P
    private String btType = "0";  // 0: Bluetooth low power, 1: Bluetooth classic
    private int rssi;

    private int me = 0;  // Whether it is your own device 0: It is not your own device 1: It is your own device
    private String userId = "";    // The device owner Id (Bluetooth name) is set when added to my device
    private String userName = "";  // Device owner name (Bluetooth name/Custom name) is set when added to my device
    private String messengerId = "";   // The device passer Id (Bluetooth name) is set when passed to friends
    private String messengerName = "";   // Device passer name (Bluetooth name/Custom name) is set when passed to friends
    private int find = 0;   // 0: not found 1: found
    private String findTime = "";   // Time when find
    private String longitude = "";   // longitude
    private String latitude = "";   // latitude
    private String findPlace = "";   // location

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
