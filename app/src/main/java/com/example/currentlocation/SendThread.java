package com.example.currentlocation;

import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.UnknownHostException;
import java.security.acl.LastOwnerException;

class SendThread implements Runnable
{
    // a thread sending and receiving
     private static final int BUFFER_SIZE = 100000; // buffer size
    // send and receive messages
    @Override
    public void run() {
        // a thread writing to socket
        try {
            // actual writing to socket
            PrintWriter out = new PrintWriter(new BufferedWriter(
                    new OutputStreamWriter(SockMngr.socket.getOutputStream())),
                    true); // creates a place to write to in the network card
            String msg_len = String.format("%04d", SockMngr.text2send.toString().length());
            Log.d("MESSAGE: ", SockMngr.text2send);
            out.print(msg_len + SockMngr.text2send); // write
            out.flush(); // flushing to force write
            // if the user hasn't asked to quit
            if (!SockMngr.text2send.contains("QUIT"))
            {
                // receiving response
                char[] buff = new char[100000];
                BufferedReader input = new BufferedReader(new InputStreamReader(SockMngr.socket.getInputStream()));
                int numOfBytes = input.read(buff, 0, 4);
                int len = Integer.parseInt(new String(buff, 0, numOfBytes));
                numOfBytes = input.read(buff, 0, len);
                String rsp = new String(buff, 0, numOfBytes);
                Log.i("SendThread", "run: result " + rsp);
                SockMngr.response = rsp;
            }
            else
            {
                Log.d("QUIT", "Sent quit, closing socket");
                // close socket
                SockMngr.socket.close();
            }
            if(SockMngr.response.equals(""))
            {
                SockMngr.response = "CRASH";
            }


        } catch (UnknownHostException e) {
            e.printStackTrace();
            Log.d("Exception", "UnknownHostException");
        } catch (IOException e) {
            e.printStackTrace();
            Log.d("Exception", "IOException");
            SockMngr.response = "CRASH";
        } catch (Exception e) {
            Log.e("SendThread", "onClick: ", e);
            e.printStackTrace();
            Log.d("Exception", "Exception");
        }
        SockMngr.notifyDone();
    }

}