package com.de.search.util;


import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

//It is used to process the information transmission between two mobile phones in wifi-p2p mode
//This class provides functions for handling Socket connections, ServerSockets, P2P groups and closing connections.

public class SocketHandler {
    private static WifiP2pManager mManager;
    private static WifiP2pManager.Channel mChannel;

    private static ServerSocket serverSocket;
    private static Socket socket;
    private static int type = 0; //0: server, 1: client

    public static synchronized Socket getSocket(){
        return socket;
    }

    public static synchronized void setSocket(Socket socket){
        SocketHandler.socket = socket;
    }


    public static ServerSocket getServerSocket() {
        return serverSocket;
    }

    public static void setServerSocket(ServerSocket serverSocket) {
        SocketHandler.serverSocket = serverSocket;
    }

    public static int getType() {
        return type;
    }

    public static void setType(int type) {
        SocketHandler.type = type;
    }

    public static WifiP2pManager getmManager() {
        return mManager;
    }

    public static void setmManager(WifiP2pManager mManager) {
        SocketHandler.mManager = mManager;
    }

    public static WifiP2pManager.Channel getmChannel() {
        return mChannel;
    }

    public static void setmChannel(WifiP2pManager.Channel mChannel) {
        SocketHandler.mChannel = mChannel;
    }

    //Used to close socket and serverSocket connections.
    //It will call the closeP2p() method to close the P2P group before closing the connection.
    public static synchronized void closeSocket(){

        closeP2p();

        if (socket != null){
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (type == 0){
            if (serverSocket != null){
                try {
                    serverSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        socket = null;
        serverSocket = null;
    }

    public static synchronized void closeP2p(){

        if (mManager != null && mChannel != null){
            mManager.removeGroup(mChannel, new WifiP2pManager.ActionListener() {
                public void onSuccess() {
                    Log.e("---", "remove group success");

                }
                public void onFailure(int reason) {
                    Log.e("---" , "remove group fail");

                }
            });
        }


    }
}

