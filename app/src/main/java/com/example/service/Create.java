package com.example.service;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.example.service.resources.GameInfo;

import java.io.File;
import java.io.IOException;

public class Create extends AppCompatActivity {
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 10) {
            if (resultCode == RESULT_OK) {


                Uri uri = data.getData();
                String path = new FileUtils(getApplicationContext()).getPath(uri);

//                    File file = new File(uri.getPath());
//                    final String[] split = file.getPath().split(":");
//                    String path = split[1];
//                    String path = file.getPath();

                ((TextView) findViewById(R.id.scenarioText)).setText(path);
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create);

        findViewById(R.id.findScenario).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("*/*");
                startActivityForResult(intent,10);
            }
        });

        findViewById(R.id.createRoom).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                TextView roomName = findViewById(R.id.roomNameInput);
                if(roomName.getText().toString().isEmpty()) {
                    Toast.makeText(Create.this,
                            "Введите название комнаты",Toast.LENGTH_LONG).show();
                    return;
                }

                TextView scenarioPath = findViewById(R.id.scenarioText);

                Intent intent = new Intent(Create.this, WaitingMenuHost.class);

                GameInfo.game.roomName = roomName.getText().toString();
                GameInfo.game.isHost = true;
                try {
                    GameInfo.game.init(scenarioPath.getText().toString());
                } catch (IOException e) {
                    e.printStackTrace();
                }

                startActivity(intent);
            }
        });

    }

}