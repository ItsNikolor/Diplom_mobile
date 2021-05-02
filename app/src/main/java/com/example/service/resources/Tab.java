package com.example.service.resources;

import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;

import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Tab {
    public String id;
    public String name;
    public boolean visibility;
    public HashMap<String,Image> images = new HashMap<>();

    int count_visible = 0;
    public boolean add_visible(){
        count_visible += 1;
        if (count_visible == images.size())
            visibility = true;

        return visibility;
    }

    public Tab(String id,String name, boolean visibility) {
        this.id = id;
        this.name = name;
        this.visibility = visibility;
    }

    @NonNull
    @Override
    public String toString() {
        String sep = GameInfo.SEP;
        return "-t"+sep + id + sep + (visibility?'1':'0') + sep + name;
    }
}
