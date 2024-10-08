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
    public static synchronized void closeSocket() {
        // Close the P2P connection before closing the socket.
        closeP2p();

        // Close the socket if it is not null.
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // If the socket type is a server socket, close the server socket if it is not null.
        if (type == 0) {
            if (serverSocket != null) {
                try {
                    serverSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        // Set the socket and serverSocket objects to null.
        socket = null;
        serverSocket = null;
    }


    public static synchronized void closeP2p() {
        // Check if mManager and mChannel are not null before attempting to remove the P2P group.
        if (mManager != null && mChannel != null) {
            // Call removeGroup() to remove the current P2P group.
            mManager.removeGroup(mChannel, new WifiP2pManager.ActionListener() {
                // If the group is successfully removed, log a success message.
                public void onSuccess() {
                    Log.e("---", "remove group success");
                }
                // If the group removal fails, log an error message.
                public void onFailure(int reason) {
                    Log.e("---" , "remove group fail");
                }
            });
        }
    }

}

