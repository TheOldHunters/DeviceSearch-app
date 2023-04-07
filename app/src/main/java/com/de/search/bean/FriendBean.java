package com.de.search.bean;

import com.orm.SugarRecord;

import java.io.Serializable;

public class FriendBean extends SugarRecord implements Serializable {
    private String name; // Bluetooth name
    private String mac;  // Bluetooth mac, Bluetooth mac if it's a Bluetooth friend, Bluetooth name if it's a p2p friend, because p2p doesn't have a mac
    private String userId = "";  // Friend's phone id, empty
    private String userName = "";  // Remarks Friend Name

    private String type = "1"; // Friend type, 1: Bluetooth, 2: p2p

    private int status = 0; // 0: not sent, 1: sent


    public FriendBean() {
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

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
