package com.example.service;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.example.service.resources.Action;
import com.example.service.resources.GameInfo;
import com.example.service.resources.Var;

public class ChangeVar extends AppCompatActivity {
    public static String var_id = "";
    public static TextView var_value;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_var);

        Var var = GameInfo.game.vars.get(var_id);

        var_value = findViewById(R.id.var_name);
        var_value.setText(var.name + "  " + var.value);

        findViewById(R.id.cancel_change).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(v.getContext(), GameStart.class);
                startActivity(intent);
                var_id = "";
            }
        });

        findViewById(R.id.send_change).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String s = ((TextView) findViewById(R.id.edit_var)).getText().toString();
                double value = 0;
                try {
                    value = Double.parseDouble(s);
                }catch (NumberFormatException e) {
                    Toast.makeText(v.getContext(),"Введите подходящее значение",Toast.LENGTH_SHORT).show();
                    return;
                }

                var.set_value(value);
                if(var.visibility){
                    GameInfo.game.print_all(var.toString());
                }

                for (int i=1;i<GameInfo.game.clients.size();i++){
                    if(GameInfo.game.clients.get(i).alive){
                        for (Action a: GameInfo.game.roles.get(GameInfo.game.clients.get(i).role_id).actions.values()) {
                            GameInfo.game.outHandlers.get(i).print(a.available_string());
                        }
                    }
                }

                for (String key:GameInfo.game.additional_actions.keySet()){
                    if(!GameInfo.game.additional_actions.get(key).available())
                        GameInfo.game.outHandlers.get(GameInfo.game.leader_id).print("-ka"+GameInfo.SEP+key);
                }

                GameInfo.game.print_all("-d"+GameInfo.SEP+GameInfo.game.cur_round);

                Intent intent = new Intent(v.getContext(), GameStart.class);
                startActivity(intent);
                var_id = "";
            }
        });
    }
}