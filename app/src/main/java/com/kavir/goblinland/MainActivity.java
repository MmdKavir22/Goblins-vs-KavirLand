package com.kavir.goblinland;

import android.app.Activity;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;

public class MainActivity extends Activity {
    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        GameView game=new GameView(this);
        FrameLayout root=new FrameLayout(this);
        root.addView(game,new FrameLayout.LayoutParams(-1,-1));
        root.addView(new HudView(this,game),new FrameLayout.LayoutParams(-1,-1));
        setContentView(root);
    }
}