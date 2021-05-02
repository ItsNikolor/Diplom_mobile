package com.example.service;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.HandlerThread;

import com.example.service.resources.GameInfo;
import com.example.service.resources.Image;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;

public class OutHandlerThread {
    public HandlerThread handlerThread;
    Handler handler;
    public DataOutputStream dos;

    Socket socket;

    public OutHandlerThread(Socket client) throws IOException {
        this.socket = client;
        handlerThread = new HandlerThread("");
        handlerThread.start();

        handler = new Handler(handlerThread.getLooper());
        dos = new DataOutputStream(client.getOutputStream());
    }

    public OutHandlerThread() {
    }

    public void kill(){
        System.out.println("Host print error");
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void print(String s){
        handler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    dos.writeUTF(s);
                    dos.flush();

                    if (s.length()<120)
                        System.out.println("Host printed " + s);
                    else
                        System.out.println("Host printed " + s.substring(0,110));

                } catch (IOException e) {
                    kill();
                }
            }
        });
    }

    public void print_image(Image image){
        handler.post(new Runnable() {
            @Override
            public void run() {
                image.bm.compress(Bitmap.CompressFormat.PNG, 100, dos);
                try {
                    dos.flush();
                } catch (IOException e) {
                    kill();
                }
            }
        });
    }
}
