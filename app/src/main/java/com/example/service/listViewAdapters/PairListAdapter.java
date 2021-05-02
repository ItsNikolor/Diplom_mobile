package com.example.service.listViewAdapters;

import android.content.Context;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.service.R;

import java.util.List;

public class PairListAdapter extends ArrayAdapter<Pair<String,String>> {

    private final int mResource;
    private final Context mContext;

    public PairListAdapter(@NonNull Context context, int resource, @NonNull List<Pair<String, String>> objects) {
        super(context, resource, objects);

        mResource = resource;
        mContext = context;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        convertView = inflater.inflate(mResource,parent,false);

        ((TextView) convertView.findViewById(R.id.pair_first)).setText(getItem(position).first);
        ((TextView) convertView.findViewById(R.id.pair_second)).setText( getItem(position).second);

        return convertView;
    }
}
