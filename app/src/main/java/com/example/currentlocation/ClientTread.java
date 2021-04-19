package com.example.currentlocation;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

class ClientThread implements Runnable {
    // a thread initiating socket

    private static final int SERVERPORT = 1234; // server port
    private static final String SERVER_IP = "10.100.102.10"; // server ip

    // initiate socket
    public void run()
    {
        try {
            InetAddress serverAddr = InetAddress.getByName(SERVER_IP);
            SockMngr.socket = new Socket(serverAddr, SERVERPORT);

        } catch (UnknownHostException e1) {
            e1.printStackTrace();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        // notify thread ended
        SockMngr.notifyDone();

    }



}