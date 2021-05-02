package com.example.service;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Pair;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.service.resources.GameInfo;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


public class WiFiDirectBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = "MyDebug";
    
    private final WifiP2pManager _manager;
    private final WifiP2pManager.Channel _channel;
    private final AppCompatActivity _activity;

    boolean isHost = true;
    boolean isMenuClient = false;
    public WiFiDirectBroadcastReceiver(WifiP2pManager manager, WifiP2pManager.Channel channel, AppCompatActivity activity) {
        super();
        _manager = manager;
        _channel = channel;
        _activity = activity;
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        System.out.println(TAG +":  "+"Action is "+action);

        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
            int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
            if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                System.out.println(TAG +":  "+"Wifi is on   " + state);
                Connect.wifi = true;
            } else {
                System.out.println(TAG +":  "+"Wifi is off  " + state);

                Toast.makeText(_activity.getApplicationContext(),
                        "Включите WiFi",Toast.LENGTH_LONG).show();
                Connect.wifi = false;
            }
        } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
            _manager.requestPeers(_channel, new WifiP2pManager.PeerListListener() {
                @Override
                public void onPeersAvailable(WifiP2pDeviceList peers) {
                    Collection<WifiP2pDevice> l = peers.getDeviceList();
                    List<String> devices = new ArrayList<>();
                    List<String> devicesIP = new ArrayList<>();

                    for (WifiP2pDevice device: l){
                        System.out.println(device.deviceName);
                        if(device.deviceName.startsWith(GameInfo.START_NAME)) {
                            devices.add(device.deviceName.substring(GameInfo.START_NAME.length()));
                            devicesIP.add(device.deviceAddress);
                        }
                    }
                    System.out.println(TAG +":  "+"Peers  " + devices.toString());


                    if(!GameInfo.game.isHost && GameInfo.game.isConnecting){
                        Connect activity = (Connect) _activity;

                        activity.currentAdrs.clear();
                        activity.currentAdrs.addAll(devicesIP);

                        activity.currentRooms.clear();
                        for (String d:devices){
                            activity.currentRooms.add(new Pair<>(d,""));
                        }

                        activity.deviceAdapter.notifyDataSetChanged();
                    }
                }
            });
        } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
            _manager.requestConnectionInfo(_channel, new WifiP2pManager.ConnectionInfoListener() {
                @Override
                public void onConnectionInfoAvailable(WifiP2pInfo info) {
                    System.out.println(TAG +":  "+ "onConnectionInfoAvailable: " + info.toString());


                    if (!GameInfo.game.isHost && GameInfo.game.isConnecting && info.groupFormed){
                        final String groupOwnerAddress=info.groupOwnerAddress.getHostAddress();
                        Intent intent = new Intent(_activity.getApplicationContext(),WaitingMenuClient.class);
                        GameInfo.game.hostAdr = groupOwnerAddress;
                        GameInfo.game.isConnecting = false;
                        GameInfo.game.clear();
                        _activity.startActivity(intent);
                    }

                }
            });

            NetworkInfo net = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
            System.out.println(TAG +":  "+ "Network: " + net.toString());
            System.out.println(TAG +":  "+ "Network State: " + net.getDetailedState().toString());


            if (isMenuClient){
                if(!net.isConnected()) {
                    Intent goIntent = new Intent(_activity, Connect.class);
                    _activity.startActivity(goIntent);
                }
            }
        } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {

        }
    }
}
