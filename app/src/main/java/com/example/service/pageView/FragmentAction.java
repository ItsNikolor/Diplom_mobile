package com.example.service.pageView;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.service.ConfirmAction;
//import com.example.service.HostHandler;
import com.example.service.R;
import com.example.service.listViewAdapters.ActionListAdapter;
import com.example.service.listViewAdapters.VarListAdapter;
import com.example.service.resources.Action;
import com.example.service.resources.GameInfo;
import com.example.service.resources.Role;

import java.util.ArrayList;
import java.util.List;

public class FragmentAction extends Fragment {
    private ActionListAdapter deviceAdapter;
    private boolean alive;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_actions, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        deviceAdapter = new ActionListAdapter(view.getContext(), R.layout.adapter_view_action, new ArrayList<>());

        ListView listView = view.findViewById(R.id.list_action);
        listView.setAdapter(deviceAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if(GameInfo.game.game_ended) return;

                ConfirmAction.action = ((Action) parent.getItemAtPosition(position));
                ConfirmAction.cur_round = GameInfo.game.cur_round;

                if(((Action)parent.getItemAtPosition(position)).can_use) {
                    Intent intent = new Intent(view.getContext(), ConfirmAction.class);
                    startActivity(intent);
                }
                else{
                    Toast.makeText(view.getContext(),"Действие недоступно",Toast.LENGTH_SHORT).show();
                }
            }
        });
        alive = true;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!alive) return;

        deviceAdapter.clear();
        for (Role r:GameInfo.game.roles.values()){
            for (Action a:r.actions.values()) {
                deviceAdapter.add(a);
            }
        }
        for(Action a:GameInfo.game.additional_actions.values()){
            deviceAdapter.add(a);
        }
        deviceAdapter.notifyDataSetChanged();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        alive = false;
    }
}
