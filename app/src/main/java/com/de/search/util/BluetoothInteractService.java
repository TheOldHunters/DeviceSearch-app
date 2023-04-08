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

// This class is the Bluetooth communication between two mobile phones' connection, sending and receiving information
// Some ideas are referred from 'ChatService' in the open source project below, but it was redesigned for this app
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


    // Create a listening thread ready to accept a new connection by using blocking mode
    // calling BluetoothServerSocket.accept()
    private class ReceiveThread extends Thread {
        private final BluetoothServerSocket mmServerSocket;

        @SuppressLint("MissingPermission")
        public ReceiveThread() {
            BluetoothServerSocket tmp = null;
            try {
                //Use RF comm to listen
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
        The connection thread is used to send requests and processes to connect to the other party's Bluetooth.
        Constructor by BluetoothDevice.createRfcommSocketToServiceRecord() ，
        Generate BluetoothSocket from the device to be connected. And then connect in the run method,
        BluetoothChatSevice's connected() method is called upon success. Define cancel() to close the socket in question when the thread is closed.
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
        The thread that runs continuously after the two sides are connected by Bluetooth; Constructor to set the input/output stream.
        The InputStream.read() loop in the run() method uses blocking mode to read the input stream and then sends it to the UI thread to update the chat message.
        This thread also provides write() to write the chat message to the other party and write it back to the UI thread after successful transmission.
        Finally, close the connected socket with cancel()
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

    //Constructor that receives the object passed by the UI main thread
    public BluetoothInteractService(Context context, Handler handler) {
        //Constructor completes the creation of the Bluetooth object
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

    //Cancel the related threads in the CONNECTING and CONNECTED states and run the new mConnectThread thread
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
        Open a ConnectedThread to manage the current connection. Cancel any existing mConnectThreads before doing so;
        mConnectedThread, mAcceptThread threads;Then open the new mConnectedThread, passing in the one you just accepted
        socket connection. Finally, the UI connection is notified via Handler
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


    // Stop all associated threads, setting the current state to NONE
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



    // When the connection is lost, set it to the LISTEN state and notify the ui
    private void onConnectionLost() {
        setState(LISTEN);
        Message msg = mHandler.obtainMessage(BluetoothInteract.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(BluetoothInteract.TOAST, "Device link interrupted");
        msg.setData(bundle);
        mHandler.sendMessage(msg);
    }


    // When the connection fails, it handles it, notifies the ui, and sets the state to LISTEN
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


    // In the CONNECTED state, call the write method in mConnectedThread and write to byte
    public void writeData(byte[] out) {
        synchronized (this) {
            if (mState != CONNECTED)
                return;
        }
        mConnectedThread.write(out);
    }


}
