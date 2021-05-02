package com.example.service.listViewAdapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.service.R;
import com.example.service.resources.Action;

import java.util.List;

public class ActionListAdapter extends ArrayAdapter<Action> {
    Context mContext;
    int mResource;


    public ActionListAdapter(@NonNull Context context, int resource, @NonNull List<Action> objects) {
        super(context, resource, objects);

        mResource = resource;
        mContext = context;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        convertView = inflater.inflate(mResource,parent,false);

        ((TextView) convertView.findViewById(R.id.player_name)).setText(getItem(position).player_name);
        ((TextView) convertView.findViewById(R.id.text_action_descr)).setText( getItem(position).descr);

        return convertView;
    }
}