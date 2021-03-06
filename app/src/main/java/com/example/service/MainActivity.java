package com.example.service;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.Display;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.service.resources.GameInfo;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MyDebug";
    String[] permissions = new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.CHANGE_NETWORK_STATE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    static final IntentFilter intentFilter = new IntentFilter();
    private WifiP2pManager manager;
    private WifiP2pManager.Channel channel;

    final Handler handler = new Handler();

    public static final int EXTERNAL_STORAGE_REQ_CODE = 100 ;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);



        GameInfo.getInstance();
        GameInfo.game.mainHandler =  new Handler(getMainLooper());

        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        GameInfo.game.width = size.x;
        GameInfo.game.height = size.y;

        String[] l = new String[0];

        System.out.println(TAG +":  "+"?? ?????????????????????? ?????? ????");
        handler.removeCallbacksAndMessages(null);


        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        Button connectButton = findViewById(R.id.connectButton);
        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this,Connect.class);
                startActivity(intent);
            }
        });

        Button createButton = findViewById(R.id.createButton);
        createButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, Create.class);
                startActivity(intent);
            }
        });

        ((TextView) findViewById(R.id.textView2)).setText("");
    }

    boolean checkPerm(){
        for(int i=0;i<permissions.length;i++){
            String perm = permissions[i];

            if (ActivityCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{perm},
                        i);

                handler.postDelayed(new Runnable() {
                    public void run() {
                        Toast.makeText(MainActivity.this, "?????????????? ???????????? ????????????????????," +
                                " ?????????? ???????????????????? ???? ?????????? ????????????????", Toast.LENGTH_LONG).show();
                        handler.postDelayed(this, 3500);
                    }
                }, 3500);
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 1: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                } else {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setMessage("???????????????? ???????????????????????????? ?? ????????????????????, ?????????? ???????????????????? ???? ?????????? ????????????????");
                    builder.create().show();
                }
                return;
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(checkPerm()) handler.removeCallbacksAndMessages(null);

        manager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        channel = manager.initialize(this, getMainLooper(), null);

        if(manager!=null&&channel!=null) {
            manager.stopPeerDiscovery(channel, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    System.out.println(TAG + ":  " + "???????????????????? ??????????????????");
                }

                @Override
                public void onFailure(int reason) {
                    System.out.println(TAG + ":  " + "???? ???????????? ???????????????????? ??????????????????");
                }
            });
            Connect.disconnect(manager, channel);
        }
    }


    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacksAndMessages(null);
    }
}