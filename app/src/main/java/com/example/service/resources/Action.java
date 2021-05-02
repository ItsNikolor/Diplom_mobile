package com.example.service.resources;

import android.text.TextUtils;
import android.util.Pair;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Action {
    public String id;
    int use_n;
    public String descr;
    String [] req;
    public String [] ans;
    public String [] ans_id;
    List<String[]> action = new ArrayList<>(),cond=new ArrayList<>();

    int count_use=0;
    public String current_ans = "";

    public String player_name = "";
    public boolean can_use = true;

    public Action(String id, int use_n, String descr, String[] req, String[] ans, String[] ans_id) {
        this.id = id;
        this.use_n = use_n;
        this.descr = descr;
        this.req = req;
        this.ans = ans;
        this.ans_id = ans_id;
    }


    @NonNull
    @Override
    public String toString() {
        String sep = GameInfo.SEP;
        String enter = GameInfo.ENTER;
        return "-a"+sep+id+sep+use_n+sep+
                descr.replace("\n",enter)+sep+
                TextUtils.join(" ",req)+sep+
                TextUtils.join(",",ans)+sep+
                TextUtils.join(",",ans_id)+sep+
                0;
    }

    public boolean available(){
        Pair<Double, HashMap<String, Double>> t = GameInfo.game.compute_action(req);
        return (use_n == 0 || count_use < use_n) && (t.first != 0);
    }

    public String available_string(){
        String sep = GameInfo.SEP;
        return "-available"+sep+id+sep+Boolean.toString(available());
    }
}
