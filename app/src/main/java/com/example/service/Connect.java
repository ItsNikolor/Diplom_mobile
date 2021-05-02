package com.example.service;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Pair;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.example.service.listViewAdapters.PairListAdapter;
import com.example.service.resources.GameInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@SuppressWarnings("SpellCheckingInspection")
public class Connect extends AppCompatActivity {
    private static final String TAG = "MyDebug";
    volatile static boolean wifi = true;

    private HandlerThread handlerThread;

    List<Pair<String,String>> currentRooms = new ArrayList<>();
    List<String> currentAdrs = new ArrayList<String>();

    ListView listView;;
    PairListAdapter deviceAdapter;

    static private WifiP2pManager.Channel channel;
    private WiFiDirectBroadcastReceiver receiver;
    private WifiP2pManager manager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connect);
        System.out.println(TAG + ":  " + "onCreate: Connect");

        manager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        channel = manager.initialize(this, getMainLooper(), null);
        receiver = new WiFiDirectBroadcastReceiver(
                manager, channel, this);
        receiver.isHost = false;

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
                String adr = currentAdrs.get(position);

                WifiP2pConfig config = new WifiP2pConfig();
                config.deviceAddress = adr;

                GameInfo.game.roomName = p.first;
                GameInfo.game.scenarioName = p.second;

                manager.requestGroupInfo(channel, new WifiP2pManager.GroupInfoListener() {
                    @Override
                    public void onGroupInfoAvailable(WifiP2pGroup group) {
                        if (group != null && manager != null && channel != null) {
                            manager.removeGroup(channel, new WifiP2pManager.ActionListener() {
                                @Override
                                public void onSuccess() {
                                    System.out.println(TAG +":  "+ "removeGroup onSuccess");
                                    connect(channel,config);
                                }
                                @Override
                                public void onFailure(int reason) {
                                    System.out.println(TAG +":  "+ "removeGroup onFailure " + reason);
                                }
                            });
                        }
                        else{
                            connect(channel,config);
                        }
                    }
                });
            }
        });

        Button button  = findViewById(R.id.refreshButton);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                refresh();
            }
        });
    }

    @SuppressLint("MissingPermission")
    private void connect(WifiP2pManager.Channel channel, WifiP2pConfig config) {
        manager.connect(channel, config, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                System.out.println(TAG + ":  " + "onSuccess: Connect");
            }

            @Override
            public void onFailure(int reason) {
                System.out.println(TAG + ":  " + "onFailure: Connect  " + reason);
            }
        });
    }


    @SuppressLint("MissingPermission")
    public static void disconnect(WifiP2pManager manager, WifiP2pManager.Channel channel) {
        if (manager != null && channel != null) {
            manager.requestGroupInfo(channel, new WifiP2pManager.GroupInfoListener() {
                @Override
                public void onGroupInfoAvailable(WifiP2pGroup group) {
                    if (group != null && manager != null && channel != null) {
                        manager.removeGroup(channel, new WifiP2pManager.ActionListener() {
                            @Override
                            public void onSuccess() {
                                System.out.println(TAG +":  "+ "removeGroup onSuccess");
                            }

                            @Override
                            public void onFailure(int reason) {
                                System.out.println(TAG +":  "+ "removeGroup onFailure " + reason);
                            }
                        });
                    }
                }
            });
        }
        else {
            System.out.println(TAG +":  "+ "WTFFFF");
        }
    }

    static Runnable runnableDiscover(WifiP2pManager manager, WifiP2pManager.Channel channel, Handler handler, int delay){
        return new Runnable() {
            @SuppressLint("MissingPermission")
            @Override
            public void run() {
                manager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
                            @Override
                            public void onSuccess() {
                                handler.postDelayed(Connect.runnableDiscover(manager, channel, handler, delay), delay);
                            }

                            @Override
                            public void onFailure(int reason) {
                                handler.postDelayed(Connect.runnableDiscover(manager, channel, handler, delay), delay);
                            }
                        });
            }
        };
    };

    private void refresh() {
        GameInfo.game.isConnecting = true;
        if(!Connect.wifi) Toast.makeText(this,
                "Включите WiFi",Toast.LENGTH_LONG).show();

        handlerThread.quit();

        currentRooms.clear();
        currentAdrs.clear();
        deviceAdapter.notifyDataSetChanged();

        discoverService();
    }

    private void discoverService() {
        handlerThread = new HandlerThread("");
        handlerThread.start();

        Handler handler = new Handler(handlerThread.getLooper());
        handler.post(runnableDiscover(manager,channel,handler,10000));
    }

    private void stopDiscoverService() {
        handlerThread.quit();
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onResume() {
        super.onResume();
        GameInfo.game.isConnecting = true;

        manager.requestGroupInfo(channel, new WifiP2pManager.GroupInfoListener() {
            @Override
            public void onGroupInfoAvailable(WifiP2pGroup group) {
                if (group != null && manager != null && channel != null) {
                    manager.removeGroup(channel, new WifiP2pManager.ActionListener() {
                        @Override
                        public void onSuccess() {
                            System.out.println(TAG +":  "+ "removeGroup onSuccess");

                            registerReceiver(receiver, MainActivity.intentFilter);
                            discoverService();
                        }

                        @Override
                        public void onFailure(int reason) {
                            System.out.println(TAG +":  "+ "removeGroup onFailure " + reason);
                        }
                    });
                }
                else{
                    registerReceiver(receiver, MainActivity.intentFilter);
                    discoverService();

                }
            }
        });


    }

    @Override
    public void onPause() {
        super.onPause();

        stopDiscoverService();
        unregisterReceiver(receiver);
    }
}