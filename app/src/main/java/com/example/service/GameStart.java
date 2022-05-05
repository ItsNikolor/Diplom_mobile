package com.example.service;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.View;
import android.view.WindowManager;

import com.example.service.pageView.FragmentAction;
import com.example.service.pageView.FragmentTab;
import com.example.service.pageView.FragmentVar;
import com.example.service.pageView.MyPagerAdapter;
import com.example.service.resources.GameInfo;

import java.util.ArrayList;

import static androidx.fragment.app.FragmentStatePagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT;

public class GameStart extends AppCompatActivity {
    private static final String TAG = "MyDebug";

    public static ArrayList<Fragment> fragments = new ArrayList<>();
    public static boolean alive = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_start);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        System.out.println(TAG +":  "+ "game start");
        System.out.println(TAG +":  "+ "GameStart onCreate");

        ViewPager pager = (ViewPager) findViewById(R.id.game_pager);
        GameInfo.game.main_pager = pager;

        fragments = new ArrayList<>();
        fragments.add(new FragmentAction());
        fragments.add(new FragmentVar());
        fragments.add(new FragmentTab());


        MyPagerAdapter pagerAdapter = new MyPagerAdapter(getSupportFragmentManager(),
                BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT,fragments);

        pager.setAdapter(pagerAdapter);
        pager.setCurrentItem(2);
        pager.setCurrentItem(1);
        System.out.println(TAG +":  "+ "game start in end");

        alive = true;
//        FragmentVar.alive = false;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull String name, @NonNull Context context, @NonNull AttributeSet attrs) {
        System.out.println(TAG +":  "+ "GameStart onCreateView");
        return super.onCreateView(name, context, attrs);
    }

    @Override
    protected void onDestroy() {
        System.out.println(TAG +":  "+ "GameStart onDestroy");
        super.onDestroy();
        alive = false;
    }

    @Override
    public void onBackPressed() {
        System.out.println("Back pressed !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
    }
}