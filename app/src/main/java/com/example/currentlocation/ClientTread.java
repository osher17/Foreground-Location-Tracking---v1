package com.example.currentlocation;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

class ClientThread implements Runnable {
    // a thread initiating socket

    private static final int SERVERPORT = 1234;
    private static final String SERVER_IP = "172.29.225.44";

    public void run() {

        try {
            InetAddress serverAddr = InetAddress.getByName(SERVER_IP);
            SockMngr.socket = new Socket(serverAddr, SERVERPORT);

        } catch (UnknownHostException e1) {
            e1.printStackTrace();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        SockMngr.notifyDone();

    }



}