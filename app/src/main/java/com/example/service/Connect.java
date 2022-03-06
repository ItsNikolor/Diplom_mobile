package com.example.service;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.example.service.listViewAdapters.PairListAdapter;
import com.example.service.resources.GameInfo;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@SuppressWarnings("SpellCheckingInspection")
public class Connect extends AppCompatActivity {
    private static final String TAG = "MyDebug";
    private static final String SERVICE_TYPE = "_nsdchat._tcp.";
    volatile static boolean wifi = true;

    private HandlerThread handlerThread;

    List<Pair<String,String>> currentRooms = new ArrayList<>();
    List<String> currentAdrs = new ArrayList<String>();

    ListView listView;;
    PairListAdapter deviceAdapter;

    private NsdManager nsdManager;
    private String serviceName;
    private NsdManager.DiscoveryListener discoveryListener;

    private class MyResolveListener implements NsdManager.ResolveListener {
        @Override
        public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
            // Called when the resolve fails. Use the error code to debug.
            Log.e(TAG, "Resolve failed: " + errorCode);
        }

        @Override
        public void onServiceResolved(NsdServiceInfo serviceInfo) {
            Log.e(TAG, "Resolve Succeeded. " + serviceInfo);

            if (serviceInfo.getServiceName().equals(serviceName)) {
                Log.d(TAG, "Same IP.");
                return;
            }
//            int port = serviceInfo.getPort();
            InetAddress host = serviceInfo.getHost();
            String name = serviceInfo.getServiceName();
            String ip = host.getHostAddress();

            boolean has = false;
            for (String s: currentAdrs){
                if(ip.equals(s))
                    has = true;
            }
            if (!has) {
                currentAdrs.add(ip);
                currentRooms.add(new Pair<>(name,""));

                deviceAdapter.notifyDataSetChanged();
            }
        }
    }

    private class RemoveResolveListener implements NsdManager.ResolveListener {
        @Override
        public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
            // Called when the resolve fails. Use the error code to debug.
            Log.e(TAG, "Resolve failed: " + errorCode);
        }

        @Override
        public void onServiceResolved(NsdServiceInfo serviceInfo) {
            Log.e(TAG, "Remove Resolve Succeeded. " + serviceInfo);

            if (serviceInfo.getServiceName().equals(serviceName)) {
                Log.d(TAG, "Same IP.");
                return;
            }
//            int port = serviceInfo.getPort();
            InetAddress host = serviceInfo.getHost();
            String ip = host.getHostAddress();

            for(int i=0;i<currentAdrs.size();i++){
                if(ip.equals(currentAdrs.get(i))){
                    currentAdrs.remove(i);
                    currentRooms.remove(i);
                    deviceAdapter.notifyDataSetChanged();
                    break;
                }
            }
        }
    }

    public void initializeDiscoveryListener() {

        // Instantiate a new DiscoveryListener
        discoveryListener = new NsdManager.DiscoveryListener() {

            // Called as soon as service discovery begins.
            @Override
            public void onDiscoveryStarted(String regType) {
                Log.d(TAG, "Service discovery started");
            }

            @Override
            public void onServiceFound(NsdServiceInfo service) {
                // A service was found! Do something with it.
                Log.d(TAG, "Service discovery success " + service);
                if (!service.getServiceType().equals(SERVICE_TYPE)) {
                    // Service type is the string containing the protocol and
                    // transport layer for this service.
                    Log.d(TAG, "Unknown Service Type: " + service.getServiceType());
                } else if (service.getServiceName().equals(serviceName)) {
                    // The name of the service tells the user what they'd be
                    // connecting to. It could be "Bob's Chat App".
                    Log.d(TAG, "Same machine: " + serviceName);
                } else if (service.getServiceName().contains("NsdChat")){
                    nsdManager.resolveService(service, new MyResolveListener());
                }
            }

            @Override
            public void onServiceLost(NsdServiceInfo service) {
                // When the network service is no longer available.
                // Internal bookkeeping code goes here.
                Log.e(TAG, "service lost: " + service);
                nsdManager.resolveService(service, new RemoveResolveListener());
            }

            @Override
            public void onDiscoveryStopped(String serviceType) {
                Log.i(TAG, "Discovery stopped: " + serviceType);
            }

            @Override
            public void onStartDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(TAG, "Discovery failed: Error code:" + errorCode);
                nsdManager.stopServiceDiscovery(this);
            }

            @Override
            public void onStopDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(TAG, "Discovery failed: Error code:" + errorCode);
                nsdManager.stopServiceDiscovery(this);
            }
        };
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connect);
        System.out.println(TAG + ":  " + "onCreate: Connect");


        nsdManager = (NsdManager) getApplicationContext().getSystemService(Context.NSD_SERVICE);
        initializeDiscoveryListener();

        nsdManager.discoverServices(
                SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener);

        deviceAdapter = new PairListAdapter(this,R.layout.adapter_view_pair,currentRooms);

        listView = findViewById(R.id.availableRooms);
        listView.setAdapter(deviceAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @SuppressLint("MissingPermission")
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position,
                                    long id) {
                Pair<String, String> p =  (Pair<String,String>) parent.getItemAtPosition(position);
                System.out.println(TAG + ":  " + "roomName = " + p.first);

                GameInfo.game.roomName = p.first;
                GameInfo.game.scenarioName = p.second;


                Intent intent = new Intent(getApplicationContext(),WaitingMenuClient.class);
                GameInfo.game.hostAdr = currentAdrs.get(position);
                GameInfo.game.isConnecting = false;
                GameInfo.game.clear();
                startActivity(intent);
            }
        });

        Button button  = findViewById(R.id.refreshButton);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                refresh();
            }
        });
    }

//    @SuppressLint("MissingPermission")
//    private void connect(WifiP2pManager.Channel channel, WifiP2pConfig config) {
//        manager.connect(channel, config, new WifiP2pManager.ActionListener() {
//            @Override
//            public void onSuccess() {
//                System.out.println(TAG + ":  " + "onSuccess: Connect");
//            }
//
//            @Override
//            public void onFailure(int reason) {
//                System.out.println(TAG + ":  " + "onFailure: Connect  " + reason);
//            }
//        });
//    }


//    @SuppressLint("MissingPermission")
//    public static void disconnect(WifiP2pManager manager, WifiP2pManager.Channel channel) {
//        if (manager != null && channel != null) {
//            manager.requestGroupInfo(channel, new WifiP2pManager.GroupInfoListener() {
//                @Override
//                public void onGroupInfoAvailable(WifiP2pGroup group) {
//                    if (group != null && manager != null && channel != null) {
//                        manager.removeGroup(channel, new WifiP2pManager.ActionListener() {
//                            @Override
//                            public void onSuccess() {
//                                System.out.println(TAG +":  "+ "removeGroup onSuccess");
//                            }
//
//                            @Override
//                            public void onFailure(int reason) {
//                                System.out.println(TAG +":  "+ "removeGroup onFailure " + reason);
//                            }
//                        });
//                    }
//                }
//            });
//        }
//        else {
//            System.out.println(TAG +":  "+ "WTFFFF");
//        }
//    }

//    static Runnable runnableDiscover(WifiP2pManager manager, WifiP2pManager.Channel channel, Handler handler, int delay){
//        return new Runnable() {
//            @SuppressLint("MissingPermission")
//            @Override
//            public void run() {
//                manager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
//                            @Override
//                            public void onSuccess() {
//                                handler.postDelayed(Connect.runnableDiscover(manager, channel, handler, delay), delay);
//                            }
//
//                            @Override
//                            public void onFailure(int reason) {
//                                handler.postDelayed(Connect.runnableDiscover(manager, channel, handler, delay), delay);
//                            }
//                        });
//            }
//        };
//    };

    private void refresh() {
//        GameInfo.game.isConnecting = true;
//        if(!Connect.wifi) Toast.makeText(this,
//                "Включите WiFi",Toast.LENGTH_LONG).show();
//
//        handlerThread.quit();
//
//        currentRooms.clear();
//        currentAdrs.clear();
//        deviceAdapter.notifyDataSetChanged();
//
//        discoverService();
    }

    private void discoverService() {
//        handlerThread = new HandlerThread("");
//        handlerThread.start();
//
//        Handler handler = new Handler(handlerThread.getLooper());
//        handler.post(runnableDiscover(manager,channel,handler,10000));
    }

    private void stopDiscoverService() {
//        handlerThread.quit();
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onResume() {
        super.onResume();
        GameInfo.game.isConnecting = true;

//        manager.requestGroupInfo(channel, new WifiP2pManager.GroupInfoListener() {
//            @Override
//            public void onGroupInfoAvailable(WifiP2pGroup group) {
//                if (group != null && manager != null && channel != null) {
//                    manager.removeGroup(channel, new WifiP2pManager.ActionListener() {
//                        @Override
//                        public void onSuccess() {
//                            System.out.println(TAG +":  "+ "removeGroup onSuccess");
//
////                            registerReceiver(receiver, MainActivity.intentFilter);
//                            discoverService();
//                        }
//
//                        @Override
//                        public void onFailure(int reason) {
//                            System.out.println(TAG +":  "+ "removeGroup onFailure " + reason);
//                        }
//                    });
//                }
//                else{
////                    registerReceiver(receiver, MainActivity.intentFilter);
//                    discoverService();
//
//                }
//            }
//        });
    }

    @Override
    public void onPause() {
        super.onPause();

//        stopDiscoverService();
//        unregisterReceiver(receiver);
    }
}