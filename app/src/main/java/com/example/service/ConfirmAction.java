package com.example.service;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.example.service.resources.Action;
import com.example.service.resources.GameInfo;


public class ConfirmAction extends AppCompatActivity {
    public static int cur_round;
    public static Action action;
    private String cur_ans="";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirm_action);

        String[] ans = action.ans;
        String[] ans_id = action.ans_id;

        ((TextView) findViewById(R.id.action_descr)).setText(action.descr);
        RadioGroup radioGroup = findViewById(R.id.radioGroup);

        radioGroup.setOrientation(LinearLayout.VERTICAL);
        for (int i = 0; i < ans.length; i++) {
            RadioButton rdbtn = new RadioButton(this);
            rdbtn.setId(View.generateViewId());
            rdbtn.setText(ans[i]);
            int finalI = i;
            rdbtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    cur_ans = ans_id[finalI];
                }
            });
            rdbtn.setEnabled(action.current_ans.equals(""));
            radioGroup.addView(rdbtn);
            if(action.current_ans.equals(ans_id[finalI])) {
                radioGroup.check(rdbtn.getId());
                cur_ans = ans_id[i];
            }
        }

        findViewById(R.id.cancel_action).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(v.getContext(),GameStart.class);
                startActivity(intent);
            }
        });

        findViewById(R.id.send_action).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (GameInfo.game.game_ended){
                    Intent intent = new Intent(v.getContext(),GameStart.class);
                    startActivity(intent);
                }

                if(ans.length>0&&cur_ans.equals("")){
                    Toast.makeText(v.getContext(),"Выберите вариант",Toast.LENGTH_SHORT).show();
                    return;
                }
                GameInfo.send_action(action.id,cur_ans,cur_round,false);
                Intent intent = new Intent(v.getContext(),GameStart.class);
                startActivity(intent);
            }
        });
        if (GameInfo.game.isHost){
            findViewById(R.id.send_action_now).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (GameInfo.game.game_ended){
                        Intent intent = new Intent(v.getContext(),GameStart.class);
                        startActivity(intent);
                    }

                    if(ans.length>0&&cur_ans.equals("")){
                        Toast.makeText(v.getContext(),"Выберите вариант",Toast.LENGTH_SHORT).show();
                        return;
                    }
                    GameInfo.send_action(action.id,cur_ans,cur_round,true);
                    Intent intent = new Intent(v.getContext(),GameStart.class);
                    startActivity(intent);
                }
            });
        }
        else findViewById(R.id.send_action_now).setVisibility(View.GONE);
    }
}