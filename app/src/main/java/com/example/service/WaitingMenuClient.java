package com.example.service;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Pair;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import com.example.service.listViewAdapters.PairListAdapter;
import com.example.service.resources.Client;
import com.example.service.resources.GameInfo;
import com.example.service.resources.Role;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class WaitingMenuClient extends AppCompatActivity {
    private static final String TAG = "MyDebug";

    static ArrayAdapter<String> spinner_adapter;
    static public List<String> role_names = new ArrayList<>(),
            role_descr = new ArrayList<>(),
            role_ids = new ArrayList<>();
    private static Socket socket;

    public static void add_role(Role r) {
        GameInfo.game.mainHandler.post(new Runnable() {
            @Override
            public void run() {
                WaitingMenuClient.role_names.add(r.name);
                WaitingMenuClient.role_descr.add(r.descr);
                WaitingMenuClient.role_ids.add(r.id);

                spinner_adapter.notifyDataSetChanged();
            }
        });

    }
    String curRole = Role.no_role.id;
    public static TextView scenario_name;

    static PairListAdapter players_adapter;

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
        GameInfo.game.main_context = this;

        findViewById(R.id.start_game).setVisibility(View.GONE);

        scenario_name = findViewById(R.id.scenario_name);

        Spinner spinner = findViewById(R.id.spinner);
        spinner_adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, role_names);

        spinner.setAdapter(spinner_adapter);

        add_role(Role.no_role);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                ((TextView) findViewById(R.id.role_descr)).setText(role_descr.get(position));
                curRole = role_ids.get(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });


        ListView l = findViewById(R.id.players_list);
        List<Pair<String,String>> players = new ArrayList<>();

        players_adapter = new PairListAdapter(this,R.layout.adapter_view_pair,new ArrayList<>());
        l.setAdapter(players_adapter);

        ((TextView) findViewById(R.id.role_descr)).setMovementMethod(new ScrollingMovementMethod());

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                Socket socket = new Socket();
                WaitingMenuClient.socket = socket;
                try {
                    socket.bind(null);
                    socket.connect((new InetSocketAddress(GameInfo.game.hostAdr, 8888)), 500);
                    GameInfo.game.outHandlers.add(new OutHandlerThread(socket));
                } catch (IOException e) {
                    e.printStackTrace();
                    GameInfo.game.mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            Intent intent = new Intent(WaitingMenuClient.this,MainActivity.class);
                            startActivity(intent);
                        }
                    });
                    return;
                }

                Thread inThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            DataInputStream in = new DataInputStream(socket.getInputStream());
                            while (true){
                                String line = in.readUTF();
                                if (line==null) break;

                                if(line.length()<200)
                                    System.out.println("Client in " + line);
                                else
                                    System.out.println("Client in " + line.substring(0,190));

                                GameInfo.parse_line(line,new GameInfo.MyInputStream(in),0);
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        finally {
                            GameInfo.game.outHandlers.get(0).handlerThread.quit();
                            try {
                                socket.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            GameInfo.game.mainHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    Intent intent = new Intent(WaitingMenuClient.this,MainActivity.class);
                                    startActivity(intent);
                                }
                            });
                        }
                    }
                });
                inThread.start();
            }
        });
        thread.start();

        Button snd_button = findViewById(R.id.send_player);
        snd_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = ((TextView) findViewById(R.id.edit_name)).getText().toString();
                Client client = new Client(0, name, curRole);
                GameInfo.game.outHandlers.get(0).print(client.toString());
            }
        });
    }
}