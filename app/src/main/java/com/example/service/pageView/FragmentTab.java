package com.example.service.pageView;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.service.R;
import com.example.service.resources.GameInfo;
import com.example.service.resources.Tab;

import java.util.ArrayList;
import java.util.List;

public class FragmentTab extends Fragment {
    private ArrayAdapter<String> deviceAdapter;
    private List<String> tabsId;
    private boolean alive;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_tabs, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        deviceAdapter = new ArrayAdapter<>(view.getContext(), android.R.layout.simple_list_item_1, new ArrayList<>());
        tabsId = new ArrayList<>();

        ListView listView = view.findViewById(R.id.list_tab);
        listView.setAdapter(deviceAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String tab_id = tabsId.get(position);
                Intent intent = new Intent(view.getContext(), SwitchActivity.class);

                SwitchActivity.tab_id = tab_id;
                startActivity(intent);
            }
        });
        alive = true;
    }

    @Override
    public void onResume() {
        super.onResume();
        if(!alive) return;
        deviceAdapter.clear();
        tabsId.clear();
        for (Tab tab : GameInfo.game.tabs.values()) {
            deviceAdapter.add(tab.name);
            tabsId.add(tab.id);
        }
        deviceAdapter.notifyDataSetChanged();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        alive = false;
    }
}
