package com.example.service.pageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;


import android.os.Bundle;

import com.example.service.R;
import com.example.service.resources.GameInfo;
import com.example.service.resources.Image;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;

import static androidx.fragment.app.FragmentStatePagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT;

public class SwitchActivity extends AppCompatActivity {
    private ViewPager mMyViewPager;
    private TabLayout mTabLayout;

    public static String tab_id;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tab_switch);
        mTabLayout = findViewById(R.id.tab_layout);
        mMyViewPager = findViewById(R.id.view_pager);

        System.out.println(GameInfo.game.tabs);

        init();
    }

    private void init(){
        ArrayList<Fragment> fragments = new ArrayList<>();

        for (Image image: GameInfo.game.tabs.get(tab_id).images.values()){
            System.out.println("Fragemts    " + image.tab_id + ' ' +image.image_id);
            FragmentTabImage fragment = new FragmentTabImage(image);
            fragments.add(fragment);
        }

        MyPagerAdapter pagerAdapter = new MyPagerAdapter(getSupportFragmentManager(),
                BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT,fragments);

        mMyViewPager.setAdapter(pagerAdapter);
        mTabLayout.setupWithViewPager(mMyViewPager, true);
    }
}