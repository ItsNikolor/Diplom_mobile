package com.example.service.resources;

import androidx.annotation.NonNull;

import java.net.Socket;

public class Client {
    int id;
    public String name;
    public String role_id;
    Socket socket;

    public boolean alive = true;

    public Client(int id, String name, String role_id, Socket socket) {
        this.id = id;
        this.name = name;
        this.role_id = role_id;
        this.socket = socket;
    }

    public Client(int id, String name, String role_id) {
        this.id = id;
        this.name = name;
        this.role_id = role_id;
    }

    public Client(int id) {
        this.id = id;
        alive = false;
    }

    @NonNull
    @Override
    public String toString() {
        String sep = GameInfo.SEP;
        return "-p"+ sep +id + sep + role_id + sep + name;
    }

    public String ded_str() {
        return "-p" +GameInfo.SEP+ id;
    }
}
