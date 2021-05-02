package com.example.service;

import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;

public class EmptyActionListener implements WifiP2pManager.ActionListener {
    private static final String TAG = "MyDebug";
    @Override
    public void onSuccess() {
        System.out.println("Всё окккккккккк");;
    }

    @Override
    public void onFailure(int reason) {
        System.out.println(TAG +":  "+ "Что-то пошло не так. ААААААААААААААААААААААА");
    }
}
