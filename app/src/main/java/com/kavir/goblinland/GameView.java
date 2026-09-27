package com.kavir.goblinland;

import android.content.Context;
import android.graphics.*;
import android.view.MotionEvent;
import android.view.View;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

public class GameView extends View {
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Random random = new Random();
    private final ArrayList<Goblin> goblins = new ArrayList<>();

    private float px=0, py=0, yaw=0;
    private float joyX=0, joyY=0;
    private boolean moving=false, firing=false;
    private float lastLookX=0;
    private long last=0, waveDelay=0, shotAt=0;
    private int hp=100, maxHp=100, xp=0, level=1, gold=0, kills=0, wave=0;
    private boolean dead=false;

    public GameView(Context c) {
        super(c);
        setFocusable(true);
        spawnWave();
    }

    private void spawnWave() {
        wave++;
        int count=4 + wave*2;
        for(int i=0;i<count;i++){
            double a=random.nextDouble()*Math.PI*2;
            float d=7 + random.nextFloat()*7;
            goblins.add(new Goblin((float)Math.cos(a)*d,(float)Math.sin(a)*d, 1.8f+wave*.12f));
        }
    }

    @Override protected void onDraw(Canvas c) {
        super.onDraw(c);
        long now=System.currentTimeMillis();
        if(last==0) last=now;
        float dt=Math.min(.04f,(now-last)/1000f);
        last=now;
        if(!dead) update(dt,now);
        render(c);
        postInvalidateDelayed(16);
    }

    private void update(float dt,long now) {
        float speed=5.0f;
        px += joyX*speed*dt;
        py += joyY*speed*dt;

        for(Iterator<Goblin> it=goblins.iterator();it.hasNext();) {
            Goblin g=it.next();
            float dx=px-g.x, dy=py-g.y, d=(float)Math.hypot(dx,dy);
            if(d>.8f){
                g.x += dx/d*g.speed*dt;
                g.y += dy/d*g.speed*dt;
            } else if(now-g.hitAt>450) {
                hp-=8;
                g.hitAt=now;
                if(hp<=0){ hp=0; dead=true; }
            }
        }

        if(firing && now-shotAt>220) {
            shoot(now);
            shotAt=now;
        }

        if(goblins.isEmpty()){
            if(waveDelay==0) waveDelay=now+900;
            if(now>waveDelay){ waveDelay=0; spawnWave(); }
        }
    }

    private void shoot(long now) {
        Goblin target=null;
        float best=Float.MAX_VALUE;
        for(Goblin g:goblins){
            float dx=g.x-px, dy=g.y-py;
            float dist=(float)Math.hypot(dx,dy);
            float a=(float)Math.atan2(dy,dx);
            float diff=Math.abs(normalize(a-yaw));
            if(diff<0.14f && dist<14 && dist<best){
                best=dist; target=g;
            }
        }
        if(target!=null){
            target.hp-=35;
            target.hitAt=now;
            if(target.hp<=0){
                goblins.remove(target);
                kills++;
                xp+=20;
                gold+=2+random.nextInt(4);
                if(xp>=level*100){
                    xp-=level*100;
                    level++;
                    maxHp+=5;
                    hp=Math.min(maxHp,hp+20);
                }
            }
        }
    }

    private float normalize(float a){
        while(a>Math.PI)a-=Math.PI*2;
        while(a<-Math.PI)a+=Math.PI*2;
        return a;
    }

    private void render(Canvas c) {
        int w=getWidth(), h=getHeight();
        p.setStyle(Paint.Style.FILL);
        p.setColor(Color.rgb(24,29,38));
        c.drawRect(0,0,w,h*.48f,p);
        p.setColor(Color.rgb(56,43,31));
        c.drawRect(0,h*.48f,w,h,p);

        p.setColor(Color.argb(35,255,255,255));
        for(int i=0;i<12;i++){
            float y=h*.48f+i*i*(h*.52f/120f);
            c.drawLine(0,y,w,y,p);
        }

        ArrayList<Goblin> sorted=new ArrayList<>(goblins);
        sorted.sort((a,b)->Float.compare(distance(b),distance(a)));
        for(Goblin g:sorted) drawGoblin(c,g,w,h);

        drawWeapon(c,w,h);
        drawHud(c,w,h);
        drawControls(c,w,h);
        if(dead) drawDead(c,w,h);
    }

    private float distance(Goblin g){ return (float)Math.hypot(g.x-px,g.y-py); }

    private void drawGoblin(Canvas c,Goblin g,int w,int h) {
        float dx=g.x-px, dy=g.y-py;
        float dist=(float)Math.hypot(dx,dy);
        float rel=normalize((float)Math.atan2(dy,dx)-yaw);
        if(Math.abs(rel)>1.35f) return;
        float sx=w*.5f+(rel/(float)(Math.PI/2))*(w*.5f);
        float size=Math.max(20,Math.min(170,260/dist));
        float base=h*.49f+Math.min(h*.43f,dist*9);
        p.setColor(Color.rgb(71,140,65));
        c.drawOval(sx-size*.34f,base-size*.95f,sx+size*.34f,base-size*.05f,p);
        p.setColor(Color.rgb(103,176,79));
        c.drawCircle(sx,base-size*.85f,size*.34f,p);
        p.setColor(Color.rgb(36,27,20));
        c.drawOval(sx-size*.42f,base-size*1.13f,sx+size*.42f,base-size*.93f,p);
        p.setColor(Color.YELLOW);
        c.drawCircle(sx-size*.12f,base-size*.88f,size*.045f,p);
        c.drawCircle(sx+size*.12f,base-size*.88f,size*.045f,p);
        p.setColor(Color.DKGRAY);
        c.drawRect(sx-size*.42f,base-size*1.28f,sx+size*.42f,base-size*1.20f,p);
        p.setColor(Color.rgb(190,45,45));
        c.drawRect(sx-size*.45f,base-size*1.38f,sx-size*.45f+size*.9f*(g.hp/100f),base-size*1.32f,p);
    }

    private void drawWeapon(Canvas c,int w,int h) {
        p.setColor(Color.rgb(30,30,35));
        Path gun=new Path();
        gun.moveTo(w*.62f,h); gun.lineTo(w*.78f,h);
        gun.lineTo(w*.70f,h*.67f); gun.lineTo(w*.64f,h*.67f); gun.close();
        c.drawPath(gun,p);
        p.setColor(Color.rgb(110,110,120));
        c.drawRect(w*.675f,h*.55f,w*.705f,h*.75f,p);
    }

    private void drawHud(Canvas c,int w,int h) {
        p.setTypeface(Typeface.DEFAULT_BOLD);
        p.setTextSize(22);
        p.setColor(Color.WHITE);
        c.drawText("GOBLINS vs KAVIRLAND",20,32,p);
        p.setTextSize(16);
        c.drawText("BETA 0.0.1",20,54,p);
        c.drawText("WAVE "+wave+"   KILLS "+kills+"   GOLD "+gold,20,78,p);
        p.setColor(Color.rgb(90,30,30));
        c.drawRect(20,h-38,220,h-20,p);
        p.setColor(Color.rgb(225,55,55));
        c.drawRect(20,h-38,20+200*(hp/(float)maxHp),h-20,p);
        p.setColor(Color.WHITE);
        c.drawText("HP "+hp+"/"+maxHp+"   LV "+level+"   XP "+xp+"/"+(level*100),235,h-22,p);
    }

    private void drawControls(Canvas c,int w,int h) {
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(4);
        p.setColor(Color.argb(100,255,255,255));
        c.drawCircle(95,h-105,65,p);
        c.drawCircle(95+joyX*35,h-105+joyY*35,28,p);
        p.setStyle(Paint.Style.FILL);
        p.setColor(firing?Color.rgb(255,90,70):Color.argb(150,220,70,55));
        c.drawCircle(w-95,h-105,65,p);
        p.setColor(Color.WHITE);
        p.setTextSize(18);
        p.setTypeface(Typeface.DEFAULT_BOLD);
        c.drawText("FIRE",w-120,h-99,p);
        p.setStyle(Paint.Style.FILL);
    }

    private void drawDead(Canvas c,int w,int h) {
        p.setColor(Color.argb(190,0,0,0));
        c.drawRect(0,0,w,h,p);
        p.setColor(Color.WHITE);
        p.setTextAlign(Paint.Align.CENTER);
        p.setTextSize(42);
        c.drawText("YOU DIED",w/2f,h*.42f,p);
        p.setTextSize(20);
        c.drawText("Tap anywhere to restart",w/2f,h*.52f,p);
        p.setTextAlign(Paint.Align.LEFT);
    }

    @Override public boolean onTouchEvent(MotionEvent e) {
        float x=e.getX(), y=e.getY();
        int w=getWidth(), h=getHeight();
        if(dead && e.getAction()==MotionEvent.ACTION_DOWN){ restart(); return true; }

        if(e.getAction()==MotionEvent.ACTION_DOWN){
            if(x<w*.38f && y>h*.55f){ moving=true; setJoy(x,y,w,h); }
            else if(x>w*.62f && y>h*.55f){ firing=true; }
            else lastLookX=x;
            return true;
        }
        if(e.getAction()==MotionEvent.ACTION_MOVE){
            if(moving) setJoy(x,y,w,h);
            else if(x>=w*.42f && x<=w*.9f && y<h*.8f){
                yaw+= (x-lastLookX)*.008f;
                lastLookX=x;
            }
            return true;
        }
        if(e.getAction()==MotionEvent.ACTION_UP || e.getAction()==MotionEvent.ACTION_CANCEL){
            moving=false; joyX=joyY=0; firing=false; return true;
        }
        return true;
    }

    private void setJoy(float x,float y,int w,int h){
        float dx=(x-95)/65f, dy=(y-(h-105))/65f;
        float len=(float)Math.hypot(dx,dy);
        if(len>1){dx/=len;dy/=len;}
        joyX=dx; joyY=dy;
    }

    private void restart(){
        goblins.clear(); px=py=0; yaw=0; hp=100; maxHp=100; xp=0; level=1; gold=0; kills=0; wave=0; dead=false; waveDelay=0; spawnWave();
    }

    private static class Goblin {
        float x,y,speed,hp=100;
        long hitAt=0;
        Goblin(float x,float y,float speed){this.x=x;this.y=y;this.speed=speed;}
    }
}
