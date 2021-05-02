package com.example.service.resources;

import android.graphics.Bitmap;

import androidx.annotation.NonNull;

import java.io.ByteArrayOutputStream;

public class Image {
    public String tab_id;
    public String image_id;
    public boolean visibility;
    public Bitmap bm;

    public Image(String tab_id, String image_id, boolean visibility,Bitmap bm) {
        this.tab_id = tab_id;
        this.image_id = image_id;
        this.visibility = visibility;
        this.bm = bm;
    }

    @NonNull
    @Override
    public String toString() {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bm.compress(Bitmap.CompressFormat.PNG, 100, stream);
        byte[] byteArray = stream.toByteArray();
        String s = new String(byteArray);
        String sep = GameInfo.SEP;
        return "-i"+sep + tab_id + sep + image_id + sep +
                (visibility?'1':'0') + sep + byteArray.length;
    }
}
