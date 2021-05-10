package com.example.currentlocation;

import android.content.Intent;
import android.util.Log;

import java.net.Socket;


public class SockMngr {

    public static  Socket socket; // Client's socket
    public static String text2send; // message that should be sent to the server
    public static final int TIMEOUT = 1000; // time to wait
    public static String response; // server response
    private final static Object syncObj = new Object(); // object for synchronizing threads
    private static boolean waitDone = false; // has the thread ended?


    // initiate thread for initiating socket
    public static void initiate()
    {
        ClientThread clntThrd = new ClientThread();
        new Thread(clntThrd).start();
        waitForSock();
        Log.d("initiate", "finished waiting");
    }

    // send and receive messages
    public static void sendAndReceive(String msg)
    {
        // socket cant be used on main thread
        // Starting new thread for socket management
        text2send = msg;
        new Thread(new SendThread()).start();
        waitForSock();

    }

    // wait until the thread has ended
    private static void waitForSock()
    {
        try {
            synchronized (syncObj) {
                waitDone = false;
                //Wait the current Thread for 1 second
                while (!waitDone)
                    syncObj.wait(TIMEOUT);
            }
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    // notify the thread has ended
    public static synchronized void notifyDone()
    {
        synchronized(syncObj) {
            waitDone = true;
            syncObj.notify();
        }
    }
}
