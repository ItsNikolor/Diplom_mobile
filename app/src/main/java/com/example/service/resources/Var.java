package com.example.service.resources;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

public class Var {
    public String id;
    public String name;
    public double value,minValue,maxValue;
    public boolean visibility;
    List<Func> funcs = new ArrayList<>();

    public Var(String id,String name, double value, double minValue, double maxValue, boolean visibility) {
        this.id = id;
        this.name = name;
        this.value = value;
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.visibility = visibility;
    }

    public void set_value(Double v){
        if(v>maxValue)
            v = maxValue;
        else if(v<minValue)
            v = minValue;
        value = v;
    }

    @NonNull
    @Override
    public String toString() {
        String sep = GameInfo.SEP;
        return "-v"+sep+id+sep+value;
    }

    public String full_str() {
        String sep = GameInfo.SEP;
        return "-v"+sep+id+sep+value+sep+
                minValue+sep+maxValue+sep+
                (visibility?'1':'0')+sep+ name;
    }
}