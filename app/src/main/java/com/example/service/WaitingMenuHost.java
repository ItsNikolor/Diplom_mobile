package com.example.service;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Pair;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.example.service.listViewAdapters.PairListAdapter;
import com.example.service.resources.Client;
import com.example.service.resources.GameInfo;
import com.example.service.resources.Role;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class WaitingMenuHost extends AppCompatActivity {
    private static final String TAG = "MyDebug";

    protected WifiP2pManager.Channel channel;
    private WiFiDirectBroadcastReceiver receiver;

    private WifiP2pDnsSdServiceInfo serviceInfo;


    HandlerThread handlerThread,acceptThread=null;

    public static ServerSocket listener;

    private WifiP2pManager manager;

    static PairListAdapter players_adapter;

    public void setDeviceName(String devName) {
        try {
            Class[] paramTypes = new Class[3];
            paramTypes[0] = WifiP2pManager.Channel.class;
            paramTypes[1] = String.class;
            paramTypes[2] = WifiP2pManager.ActionListener.class;
            Method setDeviceName = manager.getClass().getMethod(
                    "setDeviceName", paramTypes);
            setDeviceName.setAccessible(true);

            Object arglist[] = new Object[3];
            arglist[0] = channel;
            arglist[1] = devName.substring(0,Math.min(17,devName.length()));
            arglist[2] = new WifiP2pManager.ActionListener() {

                @Override
                public void onSuccess() {
                    System.out.println("setDeviceName succeeded");
                }

                @Override
                public void onFailure(int reason) {
                    System.out.println("setDeviceName failed");
                }
            };

            setDeviceName.invoke(manager, arglist);

        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }

    }

    public static void add_client() {
        GameInfo.game.mainHandler.post(new Runnable() {
            @Override
            public void run() {
                players_adapter.clear();
                for (Client c:GameInfo.game.clients){
                    if(c.alive) {
                        String role_name;
                        if (c.role_id.equals(Role.no_role.id))
                            role_name = Role.no_role.name;
                        else if (c.role_id.equals(Role.host_role.id))
                            role_name = Role.host_role.name;
                        else
                            role_name = GameInfo.game.roles.get(c.role_id).name;

                        players_adapter.add(new Pair<>(c.name,role_name));
                    }
                }
                players_adapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_waiting_menu);

        findViewById(R.id.spinner).setVisibility(View.GONE);
        findViewById(R.id.role_descr).setVisibility(View.GONE);

        ListView l = findViewById(R.id.players_list);

        players_adapter = new PairListAdapter(this,R.layout.adapter_view_pair,new ArrayList<>());
        l.setAdapter(players_adapter);

        manager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        channel = manager.initialize(this, getMainLooper(), null);
        receiver = new WiFiDirectBroadcastReceiver(manager, channel, this);

        setDeviceName(GameInfo.START_NAME+GameInfo.game.roomName);

        String roomName = GameInfo.game.roomName;
        String scenarioName = GameInfo.game.scenarioName;

        ((TextView)findViewById(R.id.room_name)).setText(roomName);
        ((TextView)findViewById(R.id.scenario_name)).setText(scenarioName);

        GameInfo.game.clients.add(new Client(0,"Без имени",Role.host_role.id));
        GameInfo.game.outHandlers.add(new OutHandlerThread());
        add_client();

        Map<String,String> record = new HashMap<String,String>();
        record.put("roomName", roomName);
        record.put("scenarioName", scenarioName);
        serviceInfo = WifiP2pDnsSdServiceInfo.newInstance("_rooms",
                "_presence._tcp", record);

        if (acceptThread == null) {
            acceptThread = new HandlerThread("");
            acceptThread.start();

            Handler handler = new Handler(acceptThread.getLooper());
            handler.post(accept());
        }

        Button snd_button = findViewById(R.id.send_player);
        snd_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = ((TextView) findViewById(R.id.edit_name)).getText().toString();
                GameInfo.game.clients.get(0).name = name;
                GameInfo.game.print_all(GameInfo.game.clients.get(0).toString());
                add_client();
            }
        });
        Button start_game = findViewById(R.id.start_game);
        start_game.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    GameInfo.game.start_game(v.getContext());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

    }



    Runnable accept() {
        return new Runnable() {
            @Override
            public void run() {
                try {
                    System.out.println(TAG +":  "+ "In accept before listenter");
                    listener = new ServerSocket(8888);

                    System.out.println(TAG +":  "+ "In accept after listenter");

                    while(true){
                        System.out.println(TAG +":  "+ "In accept before accept");
                        Socket client = listener.accept();
                        System.out.println(TAG +":  "+ "In accept after accept");

                        GameInfo.game.add_client(client);
                    }
                } catch (IOException e) {
                    //e.printStackTrace();
                    System.out.println(TAG +":  "+ "listener ded");
                }
                finally {
                    acceptThread.quit();
                    acceptThread = null;
                }
            }
        };
    }

    protected void startRegistration() {
        handlerThread = new HandlerThread("");
        handlerThread.start();

        Handler handler = new Handler(handlerThread.getLooper());
        handler.post(Connect.runnableDiscover(manager,channel,handler,10000));
    }

    private void stopRegistration() {
        handlerThread.quit();
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onResume() {
        super.onResume();
        System.out.println("On Resume");

        registerReceiver(receiver, MainActivity.intentFilter);

        manager.requestGroupInfo(channel, new WifiP2pManager.GroupInfoListener() {
            @Override
            public void onGroupInfoAvailable(WifiP2pGroup group) {
                if (group != null && manager != null && channel != null) {
                    manager.removeGroup(channel, new WifiP2pManager.ActionListener() {
                        @Override
                        public void onSuccess() {
                            System.out.println(TAG +":  "+ "removeGroup onSuccess");
                            manager.createGroup(channel, new WifiP2pManager.ActionListener() {
                                @Override
                                public void onSuccess() {
                                    startRegistration();
                                }

                                @Override
                                public void onFailure(int reason) {

                                }
                            });
                        }

                        @Override
                        public void onFailure(int reason) {
                            System.out.println(TAG +":  "+ "removeGroup onFailure " + reason);
                        }
                    });
                }
                else{
                    manager.createGroup(channel, new WifiP2pManager.ActionListener() {
                        @Override
                        public void onSuccess() {
                            startRegistration();
                        }

                        @Override
                        public void onFailure(int reason) {

                        }
                    });
                }
            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        System.out.println("On Pause");

        stopRegistration();
        unregisterReceiver(receiver);
    }
}