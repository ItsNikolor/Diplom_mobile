package com.example.service.pageView;

import android.content.Intent;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.service.ChangeVar;
import com.example.service.R;
import com.example.service.listViewAdapters.VarListAdapter;
import com.example.service.resources.GameInfo;
import com.example.service.resources.Var;

import java.util.ArrayList;

import static java.lang.Math.max;

public class FragmentVar extends Fragment {
    private VarListAdapter deviceAdapter;
    private TextView log;
    private static TextView timerTextView;
    private static boolean alive = false;
    private TextView last_action;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_vars, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        deviceAdapter = new VarListAdapter(view.getContext(), R.layout.adapter_view_var, new ArrayList<>());

        ListView listView = view.findViewById(R.id.list_var);
        listView.setAdapter(deviceAdapter);

        //GameInfo.game.cur_adapter = deviceAdapter;

        log = (TextView) view.findViewById(R.id.log_journal);
        log.setMovementMethod(new ScrollingMovementMethod());

        timerTextView = view.findViewById(R.id.round_time);

        last_action = view.findViewById(R.id.cur_action);
        last_action.setMovementMethod(new ScrollingMovementMethod());

        if(GameInfo.game.isHost){
            view.findViewById(R.id.isLeader).setVisibility(View.GONE);
            last_action.setVisibility(View.GONE);
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    Var v = (Var) parent.getItemAtPosition(position);
                    ChangeVar.var_id = v.id;

                    Intent intent = new Intent(view.getContext(), ChangeVar.class);
                    startActivity(intent);
                }
            });
        }
        else if (GameInfo.game.isLeader) {
            view.findViewById(R.id.isLeader).setVisibility(View.VISIBLE);
            last_action.setVisibility(View.GONE);
        }
        else {
            view.findViewById(R.id.isLeader).setVisibility(View.GONE);
            last_action.setVisibility(View.VISIBLE);
        }
        alive = true;
    }

    private boolean isTooLarge (TextView text, String newText) {
        float textWidth = text.getPaint().measureText(newText);
        return (textWidth >= text.getMeasuredWidth ());
    }

    public String sep(){
        String sep = "";
        while (!isTooLarge(log,sep))
            sep += "-";
        return sep.substring(0,sep.length()-1);
    }

    @Override
    public void onResume() {
        super.onResume();
        System.out.println("Im in resume var");

        if(!alive) return;

        deviceAdapter.clear();
        if(GameInfo.game.isHost)
            deviceAdapter.addAll(GameInfo.game.vars.values());
        else
            for (Var v:GameInfo.game.vars.values()){
//                if(v.visibility)
                deviceAdapter.add(v);
            }
        deviceAdapter.notifyDataSetChanged();

        log.setText(GameInfo.game.log_journal);

        String descr;
        if(GameInfo.game.cur_action.equals(""))
            descr = "Действие не выбрано";
        else
            descr = GameInfo.game.roles.get(GameInfo.game.cur_action.split("_")[0]).actions.get(GameInfo.game.cur_action).descr;

        last_action.setText(descr);
    }

    @Override
    public void onPause() {
        super.onPause();
//        alive = false;
    }

    static public void update_timer(){
        if(!alive) return;

        long millis = GameInfo.game.round_length - (GameInfo.game.current_time - GameInfo.game.start_time);
        millis = max(millis,0);
        int seconds = (int) (millis / 1000);
        int minutes = seconds / 60;
        seconds = seconds % 60;

        timerTextView.setText("Время до следующего раунда " + String.format("%d:%02d", minutes, seconds));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        alive = false;
    }
}
