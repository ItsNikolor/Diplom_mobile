package com.example.service.pageView;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.service.R;
import com.example.service.resources.GameInfo;
import com.example.service.resources.Image;

public class FragmentTabImage extends Fragment {
    private ImageView mImage;
    private Image image;

    public FragmentTabImage(Image image) {
        this.image = image;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_tab_images, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {

        mImage = view.findViewById(R.id.image);
        mImage.setImageBitmap(image.bm);


        Button button = view.findViewById(R.id.sendImageButton);
        if(GameInfo.game.isHost &&
                !image.visibility &&
                !GameInfo.game.tabs.get(image.tab_id).visibility) {
            button.setVisibility(View.VISIBLE);
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(GameInfo.game.game_ended) return;

                    GameInfo game = GameInfo.game;
                    image.visibility = true;//Это ссылка
                    GameInfo.game.tabs.get(image.tab_id).add_visible();
                    game.print_all(game.tabs.get(image.tab_id).toString());
                    game.print_all(image.toString());
                    game.print_all_image(image);
                    button.setVisibility(View.GONE);
                }
            });
        }
        else{
            button.setVisibility(View.GONE);
        }
    }
}
