package com.example.service;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.example.service.listViewAdapters.PairListAdapter;
import com.example.service.resources.Client;
import com.example.service.resources.GameInfo;
import com.example.service.resources.Role;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class WaitingMenuHost extends AppCompatActivity {
    private static final String TAG = "MyDebug";

    HandlerThread acceptThread=null;

    public static ServerSocket listener;


    static PairListAdapter players_adapter;


    private NsdManager.RegistrationListener registrationListener;
    private String serviceName;
    private NsdManager nsdManager;


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

    public void initializeRegistrationListener() {
        registrationListener = new NsdManager.RegistrationListener() {

            @Override
            public void onServiceRegistered(NsdServiceInfo NsdServiceInfo) {
                // Save the service name. Android may have changed it in order to
                // resolve a conflict, so update the name you initially requested
                // with the name Android actually used.
                serviceName = NsdServiceInfo.getServiceName();
                Log.d(TAG, "Service registered with name " + serviceName);
            }

            @Override
            public void onRegistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
                // Registration failed! Put debugging code here to determine why.
                Log.d(TAG, "Service registration failed");
            }

            @Override
            public void onServiceUnregistered(NsdServiceInfo arg0) {
                // Service has been unregistered. This only happens when you call
                // NsdManager.unregisterService() and pass in this listener.
                Log.d(TAG, "Service Unregistered");
            }

            @Override
            public void onUnregistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
                // Unregistration failed. Put debugging code here to determine why.
                Log.d(TAG, "Service Unregistered failed");
            }
        };
    }

    public void registerService(String name, int port) {
        initializeRegistrationListener();
        // Create the NsdServiceInfo object, and populate it.
        NsdServiceInfo serviceInfo = new NsdServiceInfo();

        // The name is subject to change based on conflicts
        // with other services advertised on the same network.
        serviceInfo.setServiceName(GameInfo.game.SERVICE_NAME + name);
        serviceInfo.setServiceType(GameInfo.game.SERVICE_TYPE);
        serviceInfo.setPort(port);

        nsdManager = (NsdManager) getApplicationContext().getSystemService(Context.NSD_SERVICE);

        nsdManager.registerService(
                serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_waiting_menu);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        findViewById(R.id.spinner).setVisibility(View.GONE);
        findViewById(R.id.role_descr).setVisibility(View.GONE);

        ListView l = findViewById(R.id.players_list);

        players_adapter = new PairListAdapter(this,R.layout.adapter_view_pair,new ArrayList<>());
        l.setAdapter(players_adapter);

        if (acceptThread == null) {
            acceptThread = new HandlerThread("");
            acceptThread.start();

            Handler handler = new Handler(acceptThread.getLooper());
            handler.post(accept());
        }

        String roomName = GameInfo.game.roomName;
        String scenarioName = GameInfo.game.scenarioName;

        ((TextView)findViewById(R.id.room_name)).setText(roomName);
        ((TextView)findViewById(R.id.scenario_name)).setText(scenarioName);

        GameInfo.game.clients.add(new Client(0,"Без имени",Role.host_role.id));
        GameInfo.game.outHandlers.add(new OutHandlerThread());
        add_client();

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
                    listener = new ServerSocket(0);
                    System.out.println(TAG +":  "+ "Listening port " + listener.getLocalPort());

                    registerService(GameInfo.game.roomName+GameInfo.game.SEP+GameInfo.game.scenarioName+" ", listener.getLocalPort());

                    System.out.println(TAG +":  "+ "In accept after listenter");

                    while(true){
                        System.out.println(TAG +":  "+ "In accept before accept");
                        Socket client = listener.accept();
                        System.out.println(TAG +":  "+ "In accept after accept");

                        GameInfo.game.add_client(client);
                    }
                } catch (IOException e) {
                    System.out.println(TAG +":  "+ "listener ded");
                }
                finally {
                    acceptThread.quit();
                    acceptThread = null;
                }
            }
        };
    }

    @Override
    public void onResume() {
        super.onResume();
        System.out.println("On Resume");

    }

    @Override
    public void onPause() {
        super.onPause();
        System.out.println("On Pause");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        System.out.println("On Destroy");
        nsdManager.unregisterService(registrationListener);
        try {
            listener.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}