/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.bluetoothchat;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import com.example.android.common.logger.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.UUID;

/**
 * This class does all the work for setting up and managing Bluetooth
 * connections with other devices. It has a thread that listens for
 * incoming connections, a thread for connecting with a device, and a
 * thread for performing data transmissions when connected.
 */
public class BluetoothChatService {
    // Debugging
    private static final String TAG = "BluetoothChatService";

    // Name for the SDP record when creating server socket
    private static final String NAME_SECURE = "BluetoothChatSecure";
    private static final String NAME_INSECURE = "BluetoothChatInsecure";

    // Unique UUID for this application
    //private static final UUID MY_UUID_SECURE =
    //        UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");
    private static final UUID MY_UUID_SECURE =
            UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
    private static final UUID MY_UUID_INSECURE =
            UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
    //private static final UUID MY_UUID_INSECURE =
    //        UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66");

    // Member fields
    private final BluetoothAdapter mAdapter;
    private final Handler mHandler;
    private AcceptThread mSecureAcceptThread;
    private AcceptThread mInsecureAcceptThread;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;
    private int mState;

    /* multi */
    private ArrayList<String> mDeviceAddresses;
    private ArrayList<ConnectedThread> mConnThreads;
    private ArrayList<BluetoothSocket> mSockets;

    // Constants that indicate the current connection state
    public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_LISTEN = 1;     // now listening for incoming connections
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 3;  // now connected to a remote device

    private Context mcontext;

    /**
     * Constructor. Prepares a new BluetoothChat session.
     *
     * @param context The UI Activity Context
     * @param handler A Handler to send messages back to the UI Activity
     */
    public BluetoothChatService(Context context, Handler handler) {
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mState = STATE_NONE;
        mHandler = handler;
        mDeviceAddresses = new ArrayList<String>();
        mConnThreads = new ArrayList<ConnectedThread>();
        mSockets = new ArrayList<BluetoothSocket>();
        mcontext = context;
    }

    /**
     * Set the current state of the chat connection
     *
     * @param state An integer defining the current connection state
     */
    private synchronized void setState(int state) {
        Log.d(TAG, "setState() " + mState + " -> " + state);
        mState = state;

        // Give the new state to the Handler so the UI Activity can update
        mHandler.obtainMessage(Constants.MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
    }

    /**
     * Return the current connection state.
     */
    public synchronized int getState() {
        return mState;
    }

    /**
     * Start the chat service. Specifically start AcceptThread to begin a
     * session in listening (server) mode. Called by the Activity onResume()
     */
    public synchronized void start() {
        Log.d(TAG, "start");

        // Cancel any thread attempting to make a connection
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        setState(STATE_LISTEN);

        // Start the thread to listen on a BluetoothServerSocket
        if (mSecureAcceptThread == null) {
            mSecureAcceptThread = new AcceptThread(true);
            mSecureAcceptThread.start();
        }
        if (mInsecureAcceptThread == null) {
            mInsecureAcceptThread = new AcceptThread(false);
            mInsecureAcceptThread.start();
        }
    }

    /**
     * Start the ConnectThread to initiate a connection to a remote device.
     *
     * @param device The BluetoothDevice to connect
     * @param secure Socket Security type - Secure (true) , Insecure (false)
     */
    public synchronized void connect(BluetoothDevice device, boolean secure) {
        Log.d(TAG, "connect to: " + device);

        // Cancel any thread attempting to make a connection

        if (mState == STATE_CONNECTING) {
            if (mConnectThread != null) {
                /*multi*/
                //mConnectThread.cancel();
                mConnectThread = null;
            }
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            /*multi*/
            //mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // Start the thread to connect with the given device

        mConnectThread = new ConnectThread(device, secure);
        mConnectThread.start();
        setState(STATE_CONNECTING);

    }

    /**
     * Start the ConnectedThread to begin managing a Bluetooth connection
     *
     * @param socket The BluetoothSocket on which the connection was made
     * @param device The BluetoothDevice that has been connected
     */
    public synchronized void connected(BluetoothSocket socket, BluetoothDevice
            device, final String socketType) {
        Log.d(TAG, "connected, Socket Type:" + socketType);

        // Cancel the thread that completed the connection
        /*multi*/
        /*
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // Cancel the accept thread because we only want to connect to one device
        if (mSecureAcceptThread != null) {
            mSecureAcceptThread.cancel();
            mSecureAcceptThread = null;
        }
        if (mInsecureAcceptThread != null) {
            mInsecureAcceptThread.cancel();
            mInsecureAcceptThread = null;
        }
        */
        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = new ConnectedThread(socket, socketType, device.getName());
        mConnectedThread.start();
        /*multi*/
        mConnThreads.add(mConnectedThread);


        // Send the name of the connected device back to the UI Activity
        Message msg = mHandler.obtainMessage(Constants.MESSAGE_DEVICE_NAME);
        Bundle bundle = new Bundle();
        bundle.putString(Constants.DEVICE_NAME, device.getName());
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        /*multi*/
        setState(STATE_CONNECTED);
    }

    /**
     * Stop all threads
     */
    public synchronized void stop() {
        Log.d(TAG, "stop");

        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        if (mSecureAcceptThread != null) {
            mSecureAcceptThread.cancel();
            mSecureAcceptThread = null;
        }

        if (mInsecureAcceptThread != null) {
            mInsecureAcceptThread.cancel();
            mInsecureAcceptThread = null;
        }
        setState(STATE_NONE);
    }

    /**
     * Write to the ConnectedThread in an unsynchronized manner
     *
     * @param out The bytes to write
     * @see ConnectedThread#write(byte[])
     */
    public void write(byte[] out) {
        // Create temporary object
        /* Single
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (mState != STATE_CONNECTED) return;
            r = mConnectedThread;
        }
        // Perform the write unsynchronized
        r.write(out);
        */
        // When writing, try to write out to all connected threads
        for (int i = 0; i < mConnThreads.size(); i++) {
            try {
                // Create temporary object
                ConnectedThread r;
                // Synchronize a copy of the ConnectedThread
                synchronized (this) {
                    if (mState != STATE_CONNECTED) return;
                    r = mConnThreads.get(i);
                }
                // Perform the write unsynchronized
                r.write(out);
            } catch (Exception e) {
            }
        }
    }

    /**
     * Indicate that the connection attempt failed and notify the UI Activity.
     */
    private void connectionFailed() {
        // Send a failure message back to the Activity
        Message msg = mHandler.obtainMessage(Constants.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(Constants.TOAST, "Unable to connect device");
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        // Start the service over to restart listening mode
        BluetoothChatService.this.start();
    }

    /**
     * Indicate that the connection was lost and notify the UI Activity.
     */
    private void connectionLost() {
        // Send a failure message back to the Activity
        Message msg = mHandler.obtainMessage(Constants.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(Constants.TOAST, "Device connection was lost");
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        // Start the service over to restart listening mode
        BluetoothChatService.this.start();
    }

    /**
     * This thread runs while listening for incoming connections. It behaves
     * like a server-side client. It runs until a connection is accepted
     * (or until cancelled).
     */
    private class AcceptThread extends Thread {
        // The local server socket
        private final BluetoothServerSocket mmServerSocket;
        private String mSocketType;

        public AcceptThread(boolean secure) {

            BluetoothServerSocket tmp = null;
            mSocketType = secure ? "Secure" : "Insecure";

            // Create a new listening server socket
            try {
                if (secure) {
                    tmp = mAdapter.listenUsingRfcommWithServiceRecord(NAME_SECURE,
                            MY_UUID_SECURE);
                } else {
                    tmp = mAdapter.listenUsingInsecureRfcommWithServiceRecord(
                            NAME_INSECURE, MY_UUID_INSECURE);
                }
            } catch (IOException e) {
                Log.e(TAG, "Socket Type: " + mSocketType + "listen() failed", e);
            }
            mmServerSocket = tmp;

        }

        public void run() {
            Log.d(TAG, "Socket Type: " + mSocketType +
                    "BEGIN mAcceptThread" + this);
            setName("AcceptThread" + mSocketType);

            BluetoothSocket socket = null;

            // Listen to the server socket if we're not connected
            while (mState != STATE_CONNECTED) {
                try {
                    // This is a blocking call and will only return on a
                    // successful connection or an exception
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    Log.e(TAG, "Socket Type: " + mSocketType + "accept() failed", e);
                    break;
                }

                // If a connection was accepted
                if (socket != null) {
                    synchronized (BluetoothChatService.this) {
                        switch (mState) {
                            case STATE_LISTEN:
                            case STATE_CONNECTING:
                                /* multiple */
                                String address = socket.getRemoteDevice().getAddress();
                                mSockets.add(socket);
                                mDeviceAddresses.add(address);

                                // Situation normal. Start the connected thread.
                                connected(socket, socket.getRemoteDevice(),
                                        mSocketType);

                                break;
                            case STATE_NONE:
                            case STATE_CONNECTED:
                                // Either not ready or already connected. Terminate new socket.
                                /* multi
                                try {
                                    socket.close();

                                } catch (IOException e) {
                                    Log.e(TAG, "Could not close unwanted socket", e);
                                }
                                */
                                break;
                        }
                    }
                }
            }
            Log.i(TAG, "END mAcceptThread, socket Type: " + mSocketType);

        }

        public void cancel() {
            Log.d(TAG, "Socket Type" + mSocketType + "cancel " + this);
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Socket Type" + mSocketType + "close() of server failed", e);
            }
        }
    }


    /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     */
    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;
        private String mSocketType;
        SoundPoolManager spm = new SoundPoolManager();

        public ConnectThread(BluetoothDevice device, boolean secure) {
            mmDevice = device;
            BluetoothSocket tmp = null;
            mSocketType = secure ? "Secure" : "Insecure";

            // Get a BluetoothSocket for a connection with the
            // given BluetoothDevice
            try {
                if (secure) {
                    tmp = device.createRfcommSocketToServiceRecord(
                            MY_UUID_SECURE);
                } else {
                    tmp = device.createInsecureRfcommSocketToServiceRecord(
                            MY_UUID_INSECURE);
                }
            } catch (IOException e) {
                Log.e(TAG, "Socket Type: " + mSocketType + "create() failed", e);
            }
            mmSocket = tmp;

        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectThread SocketType:" + mSocketType);
            setName("ConnectThread" + mSocketType);

            // Always cancel discovery because it will slow down a connection
            mAdapter.cancelDiscovery();

            // Make a connection to the BluetoothSocket
            try {
                // This is a blocking call and will only return on a
                // successful connection or an exception
                mmSocket.connect();

            } catch (IOException e) {
                // Close the socket
                try {
                    mmSocket.close();
                } catch (IOException e2) {
                    Log.e(TAG, "unable to close() " + mSocketType +
                            " socket during connection failure", e2);
                }
                Log.e(TAG, "mmSocket.connect: " + e.getMessage());
                connectionFailed();
                return;
            }

            // Reset the ConnectThread because we're done
            synchronized (BluetoothChatService.this) {
                mConnectThread = null;
            }

            // Start the connected thread
            connected(mmSocket, mmDevice, mSocketType);
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect " + mSocketType + " socket failed", e);
            }
        }
    }

    /**
     * This thread runs during a connection with a remote device.
     * It handles all incoming and outgoing transmissions.
     */
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        private String deviceName;
        SoundPoolManager spm;
        // audioTrack;
        //boolean loaded = false;
        //int soundID;
        //SoundPool soundPool;
        //boolean plays = false;

        public ConnectedThread(BluetoothSocket socket, String socketType, String name) {
            Log.d(TAG, "create ConnectedThread: " + socketType);
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = socket.getInputStream();
                byte[] tmpbuffer = new byte[1024];
                //code hangs here if included, until the first character is read.
                //tmpIn.read(tmpbuffer);

                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "temp sockets not created", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
            deviceName = name;

            try {
                initSoundPoolManager();
            } catch (Exception e){e.printStackTrace();}


        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectedThread");
            byte[] buffer = new byte[1024];
            int bytes = 1;
            byte ch;
            // Keep listening to the InputStream while connected
            while (true) {
                try {
                    // Read from the InputStream
                    //bytes = 0;
                    /*
                    while ((ch = (byte) mmInStream.read()) != '\n') {
                        bytes++;
                        buffer[bytes - 1] = ch;
                    }
                    */
                    buffer[0] = (byte) mmInStream.read();
                    //Log.i(TAG, "receive message: "+buffer);
                    // Send the name of the connected device back to the UI Activity
                    Message msg = mHandler.obtainMessage(Constants.MESSAGE_CHANGE_DEVICE_NAME);
                    Bundle bundle = new Bundle();
                    bundle.putString(Constants.DEVICE_NAME, deviceName);
                    msg.setData(bundle);
                    mHandler.sendMessage(msg);
                    // Send the obtained bytes to the UI Activity
                    mHandler.obtainMessage(Constants.MESSAGE_READ, 1, -1, buffer)
                            .sendToTarget();


                    //AudioPlayManager APM = new AudioPlayManager((char) buffer[0]);
                    //APM.play();

                    /* SoundPool using a method defined below within the same class */
                    //playNote((char) buffer[0]);

                    /* Use SoundPoolManager class*/
                    playSound((char) buffer[0]);

                } catch (IOException e) {
                    Log.e(TAG, "disconnected", e);
                    connectionLost();
                    // Start the service over to restart listening mode
                    BluetoothChatService.this.start();
                    break;
                }
            }
        }

        /**
         * Write to the connected OutStream.
         *
         * @param buffer The bytes to write
         */
        public void write(byte[] buffer) {
            try {
                mmOutStream.write(buffer);
                // Share the sent message back to the UI Activity
                mHandler.obtainMessage(Constants.MESSAGE_WRITE, -1, -1, buffer)
                        .sendToTarget();
            } catch (IOException e) {
                Log.e(TAG, "Exception during write", e);
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
                spm.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }


        /* SoundPoolManager functions */

        private void initSoundPoolManager() {
            spm = new SoundPoolManager();
            spm.initSounds(mcontext);
            /* piano */
            /*
            spm.addSound(0, R.raw.piano_c);
            spm.addSound(1, R.raw.piano_d);
            spm.addSound(2, R.raw.piano__e);
            spm.addSound(3, R.raw.piano__f);
            spm.addSound(4, R.raw.piano__g);
            spm.addSound(5, R.raw.piano__a);
            spm.addSound(6, R.raw.piano__b);
            spm.addSound(7, R.raw.piano_c5);
            */
            spm.addSound(0, R.raw.piano_c4);
            spm.addSound(1, R.raw.piano_d4);
            spm.addSound(2, R.raw.piano_e4);
            spm.addSound(3, R.raw.piano_f4);
            spm.addSound(4, R.raw.piano_g4);
            spm.addSound(5, R.raw.piano_a4);
            spm.addSound(6, R.raw.piano_b4);
            spm.addSound(7, R.raw.piano_c5);

            /* drums */
            spm.addSound(8, R.raw.drum_bada);
            spm.addSound(9, R.raw.drum_kick);
            spm.addSound(10, R.raw.drum_snare);
            spm.addSound(11, R.raw.drum_steel6);
            /*xylophone */
            spm.addSound(12, R.raw.xylophone_c);
            spm.addSound(13, R.raw.xylophone_d);
            spm.addSound(14, R.raw.xylophone_e);
            spm.addSound(15, R.raw.xylophone_f);
            spm.addSound(16, R.raw.xylophone_g);
            spm.addSound(17, R.raw.xylophone_a);
            spm.addSound(18, R.raw.xylophone_b);
            spm.addSound(19, R.raw.xylophone_c2);
        }

        private void playSound(char c) {
            //spm = new SoundPoolManager();
            if (spm != null){
                switch (c) {
                    case 'c':
                        spm.playSound(0);
                        break;
                    case 'd':
                        spm.playSound(1);
                        break;
                    case 'e':
                        spm.playSound(2);
                        break;
                    case 'f':
                        spm.playSound(3);
                        break;
                    case 'g':
                        spm.playSound(4);
                        break;
                    case 'a':
                        spm.playSound(5);
                        break;
                    case 'b':
                        spm.playSound(6);
                        break;
                    case 'C':
                        spm.playSound(7);
                        break;
                    /* drums */
                    case 'w':
                        spm.playSound(8);
                        break;
                    case 'x':
                        spm.playSound(9);
                        break;
                    case 'y':
                        spm.playSound(10);
                        break;
                    case 'z':
                        spm.playSound(11);
                        break;
                    /*xylophone*/
                    case '1':
                        spm.playSound(12);
                        break;
                    case '2':
                        spm.playSound(13);
                        break;
                    case '3':
                        spm.playSound(14);
                        break;
                    case '4':
                        spm.playSound(15);
                        break;
                    case '5':
                        spm.playSound(16);
                        break;
                    case '6':
                        spm.playSound(17);
                        break;
                    case '7':
                        spm.playSound(18);
                        break;
                    case '8':
                        spm.playSound(19);
                        break;
                    /*
                    default:    //rebroadcast
                        Message msg = mHandler.obtainMessage(Constants.MESSAGE_BROADCASTMSG);
                        Bundle bundle = new Bundle();
                        String tem = "";
                        tem=tem+c;

                        bundle.putString(Constants.BRDCAST_MSG, tem);
                        msg.setData(bundle);
                        mHandler.sendMessage(msg);
                        break;
                    */
                }
            }
        }
    }
}


        /* Play sounds */
        /* ref http://examples.javacodegeeks.com/android/android-soundpool-example/ */
/*
        private void playNote(char playtone){
            soundPool = new SoundPool(10, AudioManager.STREAM_MUSIC, 0);
            plays = false;
            soundPool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
                @Override
                public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
                    loaded = true;
                }
            });


            switch (playtone) {
                case 'c':
                    soundID = soundPool.load(mcontext, R.raw.piano_c, 1);
                    break;
                case 'd':
                    soundID = soundPool.load(mcontext, R.raw.piano_d, 1);
                    break;
                case 'e':
                    soundID = soundPool.load(mcontext, R.raw.piano__e, 1);
                    break;
                case 'f':
                    soundID = soundPool.load(mcontext, R.raw.piano__f, 1);
                    break;
                case 'g':
                    soundID = soundPool.load(mcontext, R.raw.piano__g, 1);
                    break;
                case 'a':
                    soundID = soundPool.load(mcontext, R.raw.piano__a, 1);
                    break;
                case 'b':
                    soundID = soundPool.load(mcontext, R.raw.piano__b, 1);
                    break;
                case 'C':
                    soundID = soundPool.load(mcontext, R.raw.piano_c5, 1);
                    break;
            }

            int volume = 1;
            int counter= 0;
            if (loaded && !plays) {
                soundPool.play(soundID, volume, volume, 1, 0, 1f);
                counter = counter++;
                //Toast.makeText(this, "Played sound", Toast.LENGTH_SHORT).show();
                plays = true;
            }
            try {
                Thread.sleep(500);
                soundPool.release();
            }catch (InterruptedException e) {e.printStackTrace();}

        }

    }
*/








