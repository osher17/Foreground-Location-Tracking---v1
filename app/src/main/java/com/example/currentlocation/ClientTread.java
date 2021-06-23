package com.example.currentlocation;
import android.util.Log;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

class ClientThread implements Runnable {
    // a thread initiating socket

    private static final int SERVERPORT = 1234; // server port
    private static final String SERVER_IP = "192.168.171.171"; // server ip

    // initiate socket
    public void run()
    {
        try {
            InetAddress serverAddr = InetAddress.getByName(SERVER_IP);
            SockMngr.socket = new Socket(serverAddr, SERVERPORT); // connect
            SockMngr.response = "ACCEPTED";

        } catch (UnknownHostException e1) {
            e1.printStackTrace();
            Log.d("Exception", "UnknownHostException");
        } catch (IOException e1) {
            e1.printStackTrace();
            SockMngr.response = "FAILURE";
            Log.d("Exception", "IO EXCEPTION");
        }
        // notify thread ended
        SockMngr.notifyDone();
        Log.d("ClientThread", "Notify");

    }



}