package com.example.service.resources;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Role {
    public String id;
    public String name;
    public String descr;
    public HashMap<String,Action> actions = new HashMap<>();

    public Role(String id,String name, String descr) {
        this.id = id;
        this.name = name;
        this.descr = descr;
    }

    public static Role no_role = new Role("r-1","Без роли","Роль не выбрана");
    public static Role host_role = new Role("rhost","Хост","Создатель игры");
    public static Role leader_role = new Role("rleader","Лидер","Принимает окончательное решение");

    @NonNull
    @Override
    public String toString() {
        String sep = GameInfo.SEP;
        return "-r"+sep+id+sep+
                descr.replace("\n",GameInfo.ENTER)+
                sep+name;
    }
}
