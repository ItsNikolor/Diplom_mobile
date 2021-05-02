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
import com.example.service.resources.Var;

import java.util.List;

public class VarListAdapter extends ArrayAdapter<Var> {
    Context mContext;
    int mResource;


    public VarListAdapter(@NonNull Context context, int resource, @NonNull List<Var> objects) {
        super(context, resource, objects);

        mResource = resource;
        mContext = context;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        convertView = inflater.inflate(mResource,parent,false);

        ((TextView) convertView.findViewById(R.id.var_view)).setText(getItem(position).name);
        ((TextView) convertView.findViewById(R.id.value_view)).setText( Double.toString((getItem(position).value)));

        return convertView;
    }
}
