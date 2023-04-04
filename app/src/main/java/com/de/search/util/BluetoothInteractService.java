package com.de.search.util;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import com.de.search.app.APP;
import com.de.search.view.BluetoothInteract;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

// 这个类是两台手机之间的蓝牙通讯，连接，收发信息，参考下面这个地址的开源代码
// https://gitee.com/liu_peilin/bluetooth-communication

public class BluetoothInteractService {
    public static final int NONE = 0;
    public static final int LISTEN = 1;
    public static final int CONNECTING = 2;
    public static final int CONNECTED = 3;

    private final Handler mHandler;
    private ReceiveThread mAcceptThread;
    private ConnectionThread mConnectThread;
    private TransceiverThread mConnectedThread;
    private int mState;


    // 创建监听线程，准备接受新连接。使用阻塞方式，调用 BluetoothServerSocket.accept()
    private class ReceiveThread extends Thread {
        private final BluetoothServerSocket mmServerSocket;

        @SuppressLint("MissingPermission")
        public ReceiveThread() {
            BluetoothServerSocket tmp = null;
            try {
                //使用射频端口（RF comm）监听
                tmp = APP.getBluetoothAdapter().listenUsingRfcommWithServiceRecord("BluetoothInteract", UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66"));
            } catch (IOException e) {
            }
            mmServerSocket = tmp;
        }

        @Override
        public void run() {
            BluetoothSocket socket = null;
            while (mState != CONNECTED) {
                try {
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    break;
                }
                if (socket != null) {
                    synchronized (BluetoothInteractService.this) {
                        switch (mState) {
                            case NONE:
                            case CONNECTED:
                                try {
                                    socket.close();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                break;
                            case LISTEN:
                            case CONNECTING:
                                connected(socket, socket.getRemoteDevice());
                                break;
                        }
                    }
                }
            }
        }

        public void cancel() {
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /*
        连接线程，专门用来对外发出连接对方蓝牙的请求和处理流程。
        构造函数里通过 BluetoothDevice.createRfcommSocketToServiceRecord() ，
        从待连接的 device 产生 BluetoothSocket. 然后在 run 方法中 connect ，
        成功后调用 BluetoothChatSevice 的 connected() 方法。定义 cancel() 在关闭线程时能够关闭相关socket 。
     */
    private class ConnectionThread extends Thread {
        private final BluetoothSocket socket;
        private final BluetoothDevice mmDevice;

        @SuppressLint("MissingPermission")
        public ConnectionThread(BluetoothDevice device) {
            mmDevice = device;
            BluetoothSocket tmp = null;
            try {
                tmp = device.createRfcommSocketToServiceRecord(UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66"));
            } catch (IOException e) {
                e.printStackTrace();
            }
            socket = tmp;
        }

        @SuppressLint("MissingPermission")
        @Override
        public void run() {
            APP.getBluetoothAdapter().cancelDiscovery();
            try {
                socket.connect();
            } catch (IOException e) {
                onConnectionFailed();
                try {
                    socket.close();
                } catch (IOException e2) {
                    e.printStackTrace();
                }
                BluetoothInteractService.this.start();
                return;
            }
            synchronized (BluetoothInteractService.this) {
                mConnectThread = null;
            }
            connected(socket, mmDevice);
        }

        public void cancel() {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /*
        双方蓝牙连接后一直运行的线程；构造函数中设置输入输出流。
        run()方法中使用阻塞模式的 InputStream.read()循环读取输入流，然后发送到 UI 线程中更新聊天消息。
        本线程也提供了 write() 将聊天消息写入输出流传输至对方，传输成功后回写入 UI 线程。最后使用cancel()关闭连接的 socket
     */
    private class TransceiverThread extends Thread {
        private final BluetoothSocket bluetoothSocket;
        private final InputStream inputStream;
        private final OutputStream outputStream;

        public TransceiverThread(BluetoothSocket socket) {
            bluetoothSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
            inputStream = tmpIn;
            outputStream = tmpOut;
        }

        @Override
        public void run() {
            byte[] buffer = new byte[1024];

            int bytes;

            while (true) {
                try {
                    bytes = inputStream.read(buffer);

                    mHandler.obtainMessage(BluetoothInteract.READ, bytes, -1, buffer).sendToTarget();
                } catch (IOException e) {
                    onConnectionLost();
                    break;
                }
            }
        }

        public void write(byte[] buffer) {
            try {
                outputStream.write(buffer);
                mHandler.obtainMessage(BluetoothInteract.WRITE, -1, -1, buffer).sendToTarget();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void cancel() {
            try {
                bluetoothSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    //构造方法，接收UI主线程传递的对象
    public BluetoothInteractService(Context context, Handler handler) {
        //构造方法完成蓝牙对象的创建
        mState = NONE;
        mHandler = handler;
    }


    public synchronized void start() {
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
        if (mAcceptThread == null) {
            mAcceptThread = new ReceiveThread();
            mAcceptThread.start();
        }
        setState(LISTEN);
    }

    //取消 CONNECTING 和 CONNECTED 状态下的相关线程，然后运行新的 mConnectThread 线程
    public synchronized void onConnect(BluetoothDevice device) {
        if (mState == CONNECTING) {
            if (mConnectThread != null) {
                mConnectThread.cancel();
                mConnectThread = null;
            }
        }
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
        mConnectThread = new ConnectionThread(device);
        mConnectThread.start();
        setState(CONNECTING);
    }

    /*
        开启一个 ConnectedThread 来管理对应的当前连接。之前先取消任意现存的 mConnectThread 、
        mConnectedThread 、 mAcceptThread 线程，然后开启新 mConnectedThread ，传入当前刚刚接受的
        socket 连接。最后通过 Handler来通知UI连接
     */
    @SuppressLint("MissingPermission")
    public synchronized void connected(BluetoothSocket socket, BluetoothDevice device) {

        destroy();

        mConnectedThread = new TransceiverThread(socket);
        mConnectedThread.start();
        Message msg = mHandler.obtainMessage(BluetoothInteract.MESSAGE_DEVICE_NAME);
        Bundle bundle = new Bundle();
        bundle.putString(BluetoothInteract.DEVICE_NAME, device.getName());
        msg.setData(bundle);
        mHandler.sendMessage(msg);
        setState(CONNECTED);
    }


    // 停止所有相关线程，设当前状态为 NONE
    public synchronized void stopThread() {
        destroy();
        setState(NONE);
    }


    public void destroy(){
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
        if (mAcceptThread != null) {
            mAcceptThread.cancel();
            mAcceptThread = null;
        }
    }



    // 当连接失去的时候，设为 LISTEN 状态并通知 ui
    private void onConnectionLost() {
        setState(LISTEN);
        Message msg = mHandler.obtainMessage(BluetoothInteract.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(BluetoothInteract.TOAST, "Device link interrupted");
        msg.setData(bundle);
        mHandler.sendMessage(msg);
    }


    // 连接失败的时候处理，通知 ui ，并设为 LISTEN 状态
    private void onConnectionFailed() {
        setState(LISTEN);
        Message msg = mHandler.obtainMessage(BluetoothInteract.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(BluetoothInteract.TOAST, "Device not found");
        msg.setData(bundle);
        mHandler.sendMessage(msg);
    }


    private void setState(int state) {
        mState = state;
        mHandler.obtainMessage(BluetoothInteract.STATE, state, -1).sendToTarget();
    }

    public int getState() {
        return mState;
    }


    // 在 CONNECTED 状态下，调用 mConnectedThread 里的 write 方法，写入 byte
    public void writeData(byte[] out) {
        synchronized (this) {
            if (mState != CONNECTED)
                return;
        }
        mConnectedThread.write(out);
    }


}
