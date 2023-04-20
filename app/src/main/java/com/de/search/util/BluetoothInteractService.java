package com.de.search.util;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import com.de.search.app.APP;
import com.de.search.view.BT_interact.BluetoothInteract;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

// This class is the Bluetooth communication between two mobile phones' connection, sending and receiving information
// Some ideas are referred from 'ChatService' in the open source project below, but it was redesigned for this app and the core part is originality.
// Because many of the methods are called and used in a very official Android way of writing, so some methods may not differ much
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
                //Use RF comm to listen and the standard UUID is used
                tmp = APP.getBluetoothAdapter().listenUsingRfcommWithServiceRecord("BluetoothInteract", UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66"));
            } catch (IOException e) {
            }
            mmServerSocket = tmp;
        }

        @Override
        public void run() {
            BluetoothSocket socket;
            while (mState != CONNECTED) {
                socket = acceptSocketConnection();
                if (socket != null) {
                    synchronized (BluetoothInteractService.this) {
                        handleSocketConnection(socket);
                    }
                }
            }
        }

        private BluetoothSocket acceptSocketConnection() {
            try {
                return mmServerSocket.accept();
            } catch (IOException e) {
                return null;
            }
        }

        private void handleSocketConnection(BluetoothSocket socket) {
            switch (mState) {
                case NONE:
                case CONNECTED:
                    closeSocket(socket);
                    break;
                case LISTEN:
                case CONNECTING:
                    connected(socket, socket.getRemoteDevice());
                    break;
            }
        }

        private void closeSocket(BluetoothSocket socket) {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
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
        Generate BluetoothSocket from the device to be connected. And then connect in the run method.
        The connected() method of the BluetoothInteractSevice is called upon success.
        Define cancel() to close the socket in question when closing the thread.
     */
    private class ConnectionThread extends Thread {
        private final BluetoothSocket socket;
        private final BluetoothDevice mmDevice;

        @SuppressLint("MissingPermission")
        public ConnectionThread(BluetoothDevice device) {
            mmDevice = device;
            BluetoothSocket tmp = null;
            try {
                //This UUID corresponds to the Serial Port Profile (SPP), which is a common protocol used for Bluetooth serial communication.
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
            boolean isConnected = false; //Tracking connection status

            try {
                socket.connect();
                isConnected = true;
            } catch (IOException e) {
                onConnectionFailed();
                try {
                    socket.close();
                } catch (IOException e2) {
                    e.printStackTrace();
                }
            }

            if (isConnected) {
                synchronized (BluetoothInteractService.this) {
                    mConnectThread = null;
                }
                connected(socket, mmDevice);
            } else {
                BluetoothInteractService.this.start();
            }
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
            InputStream tmpIn;
            OutputStream tmpOut;

            try {
                tmpIn = socket.getInputStream();
            } catch (IOException e) {
                e.printStackTrace();
                tmpIn = null;
            }

            try {
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
                tmpOut = null;
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
                    bytes = readFromInputStream(buffer);
                    sendReadMessage(bytes, buffer);
                } catch (IOException e) {
                    onConnectionLost();
                    break;
                }
            }
        }

        private int readFromInputStream(byte[] buffer) throws IOException {
            return inputStream.read(buffer);
        }

        private void sendReadMessage(int bytes, byte[] buffer) {
            mHandler.obtainMessage(BluetoothInteract.READ, bytes, -1, buffer).sendToTarget();
        }

        public void write(byte[] buffer) {
            try {
                writeToOutputStream(buffer);
                sendWriteMessage(buffer);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void writeToOutputStream(byte[] buffer) throws IOException {
            outputStream.write(buffer);
        }

        private void sendWriteMessage(byte[] buffer) {
            mHandler.obtainMessage(BluetoothInteract.WRITE, -1, -1, buffer).sendToTarget();
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
        cancelConnectThread();
        cancelConnectedThread();
        startAcceptThread();
        setState(LISTEN);
    }

    private void cancelConnectThread() {
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }
    }

    private void cancelConnectedThread() {
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
    }

    private void startAcceptThread() {
        if (mAcceptThread == null) {
            mAcceptThread = new ReceiveThread();
            mAcceptThread.start();
        }
    }


    //Cancel the related threads in the CONNECTING and CONNECTED states and run the new mConnectThread thread
    public synchronized void onConnect(BluetoothDevice device) {
        cancelConnectThreadIfNeeded();
        cancelConnectedThread();
        createAndStartConnectionThread(device);
        setState(CONNECTING);
    }

    private void cancelConnectThreadIfNeeded() {
        if (mState == CONNECTING && mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }
    }

    private void createAndStartConnectionThread(BluetoothDevice device) {
        mConnectThread = new ConnectionThread(device);
        mConnectThread.start();
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

        sendDeviceNameMessage(device.getName());
        setState(CONNECTED);
    }
    //Accepts a device name as an argument and sends a message containing the device name
    private void sendDeviceNameMessage(String deviceName) {
        Message msg = mHandler.obtainMessage(BluetoothInteract.MESSAGE_DEVICE_NAME);
        Bundle bundle = new Bundle();
        bundle.putString(BluetoothInteract.DEVICE_NAME, deviceName);
        msg.setData(bundle);
        mHandler.sendMessage(msg);
    }



    // Stop all associated threads, setting the current state to NONE
    public synchronized void stopThread() {
        destroy();
        setState(NONE);
    }

    //Accepts a thread as an argument and then performs the appropriate cancel operation depending on the type of thread
    public void destroy() {
        cancelAndNullifyThread(mConnectThread);
        cancelAndNullifyThread(mConnectedThread);
        cancelAndNullifyThread(mAcceptThread);
    }

    private void cancelAndNullifyThread(Thread thread) {
        if (thread != null) {
            if (thread instanceof ReceiveThread) {
                ((ReceiveThread) thread).cancel();
            } else if (thread instanceof ConnectionThread) {
                ((ConnectionThread) thread).cancel();
            } else if (thread instanceof TransceiverThread) {
                ((TransceiverThread) thread).cancel();
            }
        }
    }


    // When the connection is lost, set it to the LISTEN state and notify the ui
    private void onConnectionLost() {
        handleConnectionEvent(LISTEN, "Device link interrupted");
    }
    // When the connection fails, it handles it, notifies the ui, and sets the state to LISTEN
    private void onConnectionFailed() {
        handleConnectionEvent(LISTEN, "Device not found");
    }
    //Accepts a status and a prompt message as parameters and then performs the corresponding action
    private void handleConnectionEvent(int state, String toastMessage) {
        setState(state);
        Message msg = mHandler.obtainMessage(BluetoothInteract.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(BluetoothInteract.TOAST, toastMessage);
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
