package com.example.service;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;

import android.os.Bundle;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_start);
        System.out.println(TAG +":  "+ "game start");

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
    }
}