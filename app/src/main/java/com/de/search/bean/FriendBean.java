package com.de.search.bean;

import com.orm.SugarRecord;

import java.io.Serializable;

public class FriendBean extends SugarRecord implements Serializable {
    private String name; // 蓝牙名称
    private String mac;  // 蓝牙mac，如果是蓝牙朋友则用蓝牙mac，如果是p2p朋友则用蓝牙名称，因为p2p没有mac
    private String userId = "";  // 朋友手机id，空
    private String userName = "";  // 备注朋友名称

    private String type = "1"; // 朋友类型，1：蓝牙，2：p2p

    private int status = 0; // 0:未发送，1：发送过


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
