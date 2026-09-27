package com.kavir.goblinland;

import android.graphics.*;
import android.view.View;
import android.content.Context;

public class HudView extends View {
    private final GameView game;
    private final Paint p=new Paint(3);
    public HudView(Context c,GameView g){super(c);game=g;setLayerType(View.LAYER_TYPE_SOFTWARE,null);}
    @Override protected void onDraw(Canvas c){
        super.onDraw(c); GameView.Renderer3D r=game.getRenderer3D(); int w=getWidth(),h=getHeight();
        p.setTypeface(Typeface.DEFAULT_BOLD);p.setStyle(Paint.Style.FILL);
        if(r.menu){shade(c);center(c,"GOBLINS",w*.50f,h*.27f,52,Color.WHITE);center(c,"VS KAVIRLAND",w*.50f,h*.34f,22,Color.LTGRAY);button(c,w*.5f-150,h*.45f,w*.5f+150,h*.54f,"PLAY",Color.rgb(48,150,86));button(c,w*.5f-125,h*.59f,w*.5f+125,h*.67f,"SETTINGS",Color.rgb(65,76,96));center(c,"REAL 3D • MOBILE FPS",w*.5f,h*.76f,13,Color.GRAY);return;}
        if(r.settings){shade(c);text(c,"SETTINGS",30,58,30,Color.WHITE);text(c,"Look sensitivity  "+String.format(java.util.Locale.US,"%.4f",r.sensitivity),40,125,17,Color.LTGRAY);button(c,40,150,w-40,205,"SENSITIVITY +",Color.rgb(55,66,84));button(c,40,225,w-40,280,"AUTO NEXT WAVE: "+(r.autoWave?"ON":"OFF"),Color.rgb(55,66,84));button(c,40,h-90,w-40,h-35,"BACK",Color.rgb(70,100,145));return;}
        p.setColor(Color.argb(190,8,12,18));c.drawRoundRect(14,12,Math.min(w-14,610),98,18,18,p);text(c,"GOBLINS vs KAVIRLAND",28,40,20,Color.WHITE);text(c,"BETA 0.0.3  •  WAVE "+r.wave+"  •  KILLS "+r.kills+"  •  GOLD "+r.gold,28,63,13,Color.LTGRAY);
        p.setColor(Color.rgb(65,28,32));c.drawRoundRect(28,76,220,90,6,6,p);p.setColor(Color.rgb(235,62,62));c.drawRoundRect(28,76,28+192*r.hp/(float)r.maxHp,90,6,6,p);text(c,"HP "+r.hp+"/"+r.maxHp,230,88,12,Color.WHITE);
        p.setColor(Color.argb(180,8,12,18));c.drawRoundRect(w-215,14,w-14,70,15,15,p);text(c,"LV "+r.level+"   XP "+r.xp+"/"+(r.level*100),w-200,39,13,Color.WHITE);text(c,"SHOTS "+r.shots,w-200,59,11,Color.LTGRAY);
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(3);p.setColor(Color.argb(115,255,255,255));c.drawCircle(95,h-105,68,p);c.drawCircle(95+r.joyX*36,h-105+r.joyY*36,28,p);p.setStyle(Paint.Style.FILL);
        p.setColor(r.firing?Color.rgb(255,75,55):Color.argb(180,185,50,42));c.drawCircle(w-100,h-110,72,p);center(c,"FIRE",w-100,h-103,19,Color.WHITE);
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2);p.setColor(Color.argb(145,255,255,255));c.drawCircle(w*.5f,h*.49f,13,p);c.drawLine(w*.5f-22,h*.49f,w*.5f-7,h*.49f,p);c.drawLine(w*.5f+7,h*.49f,w*.5f+22,h*.49f,p);c.drawLine(w*.5f,h*.49f-22,w*.5f,h*.49f-7,p);c.drawLine(w*.5f,h*.49f+7,w*.5f,h*.49f+22,p);p.setStyle(Paint.Style.FILL);
        if(r.waveBanner>System.nanoTime()) {p.setColor(Color.argb(190,8,12,18));c.drawRoundRect(w*.5f-150,h*.18f,w*.5f+150,h*.18f+70,18,18,p);center(c,"WAVE "+r.wave,w*.5f,h*.18f+45,32,Color.rgb(255,214,70));}
        if(r.dead){shade(c);center(c,"YOU DIED",w*.5f,h*.37f,44,Color.rgb(255,85,75));center(c,"WAVE "+r.wave+"  •  "+r.kills+" KILLS",w*.5f,h*.45f,17,Color.WHITE);button(c,w*.5f-135,h*.53f,w*.5f+135,h*.62f,"RESTART",Color.rgb(175,55,55));}
        else if(r.goblins.isEmpty()&&r.wave>0){p.setColor(Color.argb(205,8,12,18));c.drawRoundRect(w*.5f-210,h*.31f,w*.5f+210,h*.31f+145,20,20,p);center(c,"WAVE CLEARED",w*.5f,h*.31f+45,30,Color.rgb(100,220,125));button(c,w*.5f-130,h*.31f+85,w*.5f+130,h*.31f+135,"NEXT WAVE",Color.rgb(55,135,85));}
    }
    void shade(Canvas c){p.setColor(Color.argb(175,3,6,10));c.drawRect(0,0,getWidth(),getHeight(),p);}
    void text(Canvas c,String s,float x,float y,float size,int col){p.setTextAlign(Paint.Align.LEFT);p.setTextSize(size);p.setColor(col);c.drawText(s,x,y,p);}
    void center(Canvas c,String s,float x,float y,float size,int col){p.setTextAlign(Paint.Align.CENTER);p.setTextSize(size);p.setColor(col);c.drawText(s,x,y,p);p.setTextAlign(Paint.Align.LEFT);}
    void button(Canvas c,float l,float t,float rr,float b,String s,int col){p.setColor(Color.argb(80,0,0,0));c.drawRoundRect(l+3,t+5,rr+3,b+5,18,18,p);p.setColor(col);c.drawRoundRect(l,t,rr,b,18,18,p);center(c,s,(l+rr)/2,t+(b-t)*.65f,Math.min(20,(b-t)*.34f),Color.WHITE);}
}