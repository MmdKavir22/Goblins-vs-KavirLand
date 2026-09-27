package com.kavir.goblinland;

import android.content.Context;
import android.graphics.*;
import android.view.MotionEvent;
import android.view.View;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Random;

public class GameView extends View {
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Random random = new Random();
    private final ArrayList<Goblin> goblins = new ArrayList<>();
    private final ArrayList<Particle> particles = new ArrayList<>();

    private float px=0, py=0, yaw=0, joyX=0, joyY=0, lastLookX=0;
    private boolean moving=false, firing=false, started=false, dead=false, settings=false;
    private boolean autoWave=true, showFps=false;
    private float lookSensitivity=.008f;
    private long last=0, shotAt=0, waveBannerUntil=0, waveClearUntil=0;
    private int hp=100, maxHp=100, xp=0, level=1, gold=0, kills=0, wave=0, fps=60;
    private int shots=0, damageDealt=0;
    private float recoil=0;

    public GameView(Context c) {
        super(c);
        setFocusable(true);
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }

    @Override protected void onDraw(Canvas c) {
        super.onDraw(c);
        long now=System.currentTimeMillis();
        if(last==0) last=now;
        float dt=Math.min(.04f,(now-last)/1000f);
        last=now;
        if(!dead && started && !settings) update(dt,now);
        updateParticles(dt);
        render(c,now);
        postInvalidateDelayed(16);
    }

    private void startWave() {
        if(dead) return;
        if(goblins.size()>0) return;
        wave++;
        int count=4 + wave*2;
        for(int i=0;i<count;i++) {
            double a=random.nextDouble()*Math.PI*2;
            float d=7 + random.nextFloat()*8;
            int type=(wave>=3 && i%6==0)?2:(wave>=2 && i%4==0?1:0);
            goblins.add(new Goblin((float)Math.cos(a)*d,(float)Math.sin(a)*d,1.7f+wave*.10f+type*.35f,type));
        }
        waveBannerUntil=System.currentTimeMillis()+1500;
    }

    private void update(float dt,long now) {
        float speed=5.0f;
        px += joyX*speed*dt;
        py += joyY*speed*dt;
        recoil=Math.max(0,recoil-dt*5);

        for(Iterator<Goblin> it=goblins.iterator();it.hasNext();) {
            Goblin g=it.next();
            float dx=px-g.x, dy=py-g.y, d=(float)Math.hypot(dx,dy);
            if(d>.9f) {
                g.x += dx/d*g.speed*dt;
                g.y += dy/d*g.speed*dt;
            } else if(now-g.hitAt>450) {
                hp-=g.type==2?14:8;
                g.hitAt=now;
                burst((float)(getWidth()*.5),getHeight()*.5f,8,Color.rgb(220,60,50));
                if(hp<=0){hp=0;dead=true;firing=false;}
            }
        }

        if(firing && now-shotAt>190) {
            shoot(now);
            shotAt=now;
        }

        if(goblins.isEmpty() && started && wave>0) {
            if(waveClearUntil==0) waveClearUntil=now+900;
            if(autoWave && now>waveClearUntil) {
                waveClearUntil=0;
                startWave();
            }
        }
    }

    private void shoot(long now) {
        shots++;
        recoil=1;
        Goblin target=null;
        float best=Float.MAX_VALUE;
        for(Goblin g:goblins) {
            float dx=g.x-px, dy=g.y-py, dist=(float)Math.hypot(dx,dy);
            float diff=Math.abs(normalize((float)Math.atan2(dy,dx)-yaw));
            if(diff<.16f && dist<16 && dist<best){best=dist;target=g;}
        }
        if(target!=null) {
            int damage=target.type==2?28:35;
            target.hp-=damage;
            damageDealt+=damage;
            target.hitAt=now;
            burst(getWidth()*.5f,getHeight()*.48f,10,target.type==2?Color.rgb(255,150,50):Color.rgb(110,220,80));
            if(target.hp<=0) {
                goblins.remove(target);
                kills++;
                xp+=target.type==2?45:(target.type==1?30:20);
                gold+=target.type==2?7:(2+random.nextInt(4));
                if(xp>=level*100) {
                    xp-=level*100; level++; maxHp+=5; hp=Math.min(maxHp,hp+20);
                    burst(getWidth()*.5f,getHeight()*.4f,24,Color.rgb(255,220,70));
                }
            }
        }
    }

    private float normalize(float a){
        while(a>Math.PI)a-=Math.PI*2;
        while(a<-Math.PI)a+=Math.PI*2;
        return a;
    }

    private void updateParticles(float dt) {
        for(Iterator<Particle> it=particles.iterator();it.hasNext();) {
            Particle q=it.next();
            q.x+=q.vx*dt; q.y+=q.vy*dt; q.life-=dt;
            if(q.life<=0) it.remove();
        }
        while(particles.size()>160) particles.remove(0);
    }

    private void burst(float x,float y,int n,int color) {
        for(int i=0;i<n;i++) {
            double a=random.nextDouble()*Math.PI*2;
            float s=20+random.nextFloat()*80;
            particles.add(new Particle(x,y,(float)Math.cos(a)*s,(float)Math.sin(a)*s,.35f+random.nextFloat()*.45f,color));
        }
    }

    private void render(Canvas c,long now) {
        int w=getWidth(),h=getHeight();
        drawWorld(c,w,h,now);
        drawGoblins(c,w,h);
        drawParticles(c);
        drawWeapon(c,w,h);
        if(settings) drawSettings(c,w,h);
        else {
            drawHud(c,w,h);
            drawControls(c,w,h);
            if(!started) drawStartScreen(c,w,h);
            else if(goblins.isEmpty() && wave>0) drawWaveClear(c,w,h);
            if(waveBannerUntil>now) drawWaveBanner(c,w,h);
            if(dead) drawDead(c,w,h);
        }
    }

    private void drawWorld(Canvas c,int w,int h,long now) {
        LinearGradient sky=new LinearGradient(0,0,0,h*.5f,Color.rgb(20,28,45),Color.rgb(88,105,125),Shader.TileMode.CLAMP);
        p.setShader(sky); c.drawRect(0,0,w,h*.55f,p); p.setShader(null);
        p.setColor(Color.rgb(47,66,44)); c.drawRect(0,h*.47f,w,h,p);
        p.setColor(Color.rgb(61,83,51));
        for(int i=0;i<22;i++){
            float x=(i*137+35)%w;
            float y=h*.52f+(i*53)%Math.max(1,(int)(h*.42f));
            float s=8+(i%5)*4;
            c.drawCircle(x,y,s,p);
            c.drawRect(x-2,y,x+2,y+12,p);
        }
        p.setColor(Color.argb(38,255,255,255));
        for(int i=1;i<9;i++) {
            float y=h*.47f+i*i*(h*.48f/82f);
            c.drawLine(0,y,w,y,p);
        }
        p.setColor(Color.argb(22,0,0,0));
        c.drawCircle(w*.5f,h*.5f,Math.min(w,h)*.38f,p);
    }

    private void drawGoblins(Canvas c,int w,int h) {
        ArrayList<Goblin> sorted=new ArrayList<>(goblins);
        sorted.sort(Comparator.comparingDouble(this::distance).reversed());
        for(Goblin g:sorted) drawGoblin(c,g,w,h);
    }

    private float distance(Goblin g){return (float)Math.hypot(g.x-px,g.y-py);}

    private void drawGoblin(Canvas c,Goblin g,int w,int h) {
        float dx=g.x-px,dy=g.y-py,dist=Math.max(.8f,(float)Math.hypot(dx,dy));
        float rel=normalize((float)Math.atan2(dy,dx)-yaw);
        if(Math.abs(rel)>1.35f)return;
        float sx=w*.5f+(rel/(float)(Math.PI/2))*(w*.5f);
        float size=Math.max(22,Math.min(190,300/dist));
        float base=h*.49f+Math.min(h*.43f,dist*9);
        p.setShadowLayer(size*.10f,0,size*.08f,Color.argb(100,0,0,0));
        p.setColor(g.type==2?Color.rgb(100,55,145):(g.type==1?Color.rgb(55,125,165):Color.rgb(70,145,68)));
        c.drawOval(sx-size*.38f,base-size*.95f,sx+size*.38f,base-size*.05f,p);
        p.clearShadowLayer();
        p.setColor(g.type==2?Color.rgb(145,82,180):(g.type==1?Color.rgb(75,155,190):Color.rgb(105,180,78)));
        c.drawCircle(sx,base-size*.86f,size*.36f,p);
        Path ears=new Path();
        ears.moveTo(sx-size*.35f,base-size*.9f); ears.lineTo(sx-size*.68f,base-size*1.15f); ears.lineTo(sx-size*.25f,base-size*1.05f); ears.close();
        c.drawPath(ears,p);
        ears.reset(); ears.moveTo(sx+size*.35f,base-size*.9f); ears.lineTo(sx+size*.68f,base-size*1.15f); ears.lineTo(sx+size*.25f,base-size*1.05f); ears.close(); c.drawPath(ears,p);
        p.setColor(Color.rgb(40,30,25)); c.drawOval(sx-size*.45f,base-size*1.16f,sx+size*.45f,base-size*.94f,p);
        p.setColor(Color.YELLOW); c.drawCircle(sx-size*.13f,base-size*.88f,size*.055f,p); c.drawCircle(sx+size*.13f,base-size*.88f,size*.055f,p);
        p.setColor(Color.rgb(45,30,25)); c.drawCircle(sx,base-size*.72f,size*.12f,p);
        p.setColor(Color.rgb(30,30,30)); c.drawRect(sx-size*.48f,base-size*1.32f,sx+size*.48f,base-size*1.25f,p);
        p.setColor(g.type==2?Color.rgb(240,90,210):Color.rgb(235,60,55));
        c.drawRect(sx-size*.48f,base-size*1.32f,sx-size*.48f+size*.96f*(g.hp/g.maxHp),base-size*1.25f,p);
        p.setColor(Color.WHITE); p.setTextAlign(Paint.Align.CENTER); p.setTextSize(Math.max(10,size*.11f));
        c.drawText(g.type==2?"ELITE":g.type==1?"RUNNER":"GOBLIN",sx,base-size*1.42f,p); p.setTextAlign(Paint.Align.LEFT);
    }

    private void drawWeapon(Canvas c,int w,int h) {
        float r=recoil*10;
        p.setShadowLayer(18,0,8,Color.argb(130,0,0,0));
        p.setColor(Color.rgb(28,30,36));
        Path gun=new Path(); gun.moveTo(w*.60f,h+10+r); gun.lineTo(w*.82f,h+10+r); gun.lineTo(w*.73f,h*.67f+r); gun.lineTo(w*.64f,h*.67f+r); gun.close(); c.drawPath(gun,p);
        p.setColor(Color.rgb(105,112,124)); c.drawRoundRect(w*.675f,h*.53f+r,w*.71f,h*.76f+r,8,8,p);
        p.clearShadowLayer();
        if(recoil>.1f){p.setColor(Color.rgb(255,190,70));c.drawCircle(w*.692f,h*.49f+r,18*recoil,p);}
    }

    private void drawHud(Canvas c,int w,int h) {
        p.setTypeface(Typeface.DEFAULT_BOLD); p.setTextAlign(Paint.Align.LEFT);
        p.setColor(Color.argb(185,10,14,20)); c.drawRoundRect(14,12,Math.min(w-14,560),94,18,18,p);
        p.setColor(Color.WHITE); p.setTextSize(20); c.drawText("GOBLINS vs KAVIRLAND",28,38,p);
        p.setTextSize(13); p.setColor(Color.LTGRAY); c.drawText("BETA 0.0.2  •  WAVE "+wave+"  •  KILLS "+kills+"  •  GOLD "+gold,28,61,p);
        p.setColor(Color.rgb(70,30,35)); c.drawRoundRect(28,70,220,84,7,7,p);
        p.setColor(Color.rgb(235,65,65)); c.drawRoundRect(28,70,28+192*(hp/(float)maxHp),84,7,7,p);
        p.setColor(Color.WHITE); p.setTextSize(12); c.drawText("HP "+hp+"/"+maxHp,230,82,p);
        p.setColor(Color.argb(180,10,14,20)); c.drawRoundRect(w-245,14,w-14,72,16,16,p);
        p.setColor(Color.WHITE); p.setTextSize(13); c.drawText("LV "+level+"   XP "+xp+"/"+(level*100),w-230,38,p);
        p.setColor(Color.rgb(70,45,20)); c.drawRoundRect(w-230,48,w-28,59,5,5,p);
        p.setColor(Color.rgb(255,205,60)); c.drawRoundRect(w-230,48,w-230+202*(xp/(float)(level*100)),59,5,5,p);
        p.setTextSize(11); p.setColor(Color.LTGRAY); c.drawText("DMG "+damageDealt+"  SHOTS "+shots,w-230,70,p);
        if(showFps){p.setColor(Color.WHITE);p.setTextSize(12);c.drawText("FPS "+fps,28,112,p);}
        p.setColor(Color.argb(185,10,14,20)); c.drawRoundRect(w-78,82,w-14,126,13,13,p);
        p.setColor(Color.WHITE); p.setTextSize(22); p.setTextAlign(Paint.Align.CENTER); c.drawText("⚙",w-46,112,p); p.setTextAlign(Paint.Align.LEFT);
    }

    private void drawControls(Canvas c,int w,int h) {
        p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(3);
        p.setColor(Color.argb(100,255,255,255)); c.drawCircle(95,h-105,68,p); c.drawCircle(95+joyX*36,h-105+joyY*36,28,p);
        p.setStyle(Paint.Style.FILL); p.setColor(firing?Color.rgb(255,70,55):Color.argb(170,205,55,45)); c.drawCircle(w-98,h-108,70,p);
        p.setColor(Color.WHITE); p.setTextAlign(Paint.Align.CENTER); p.setTextSize(19); p.setTypeface(Typeface.DEFAULT_BOLD); c.drawText("FIRE",w-98,h-102,p); p.setTextAlign(Paint.Align.LEFT);
        p.setColor(Color.argb(120,255,255,255)); p.setStyle(Paint.Style.STROKE); c.drawCircle(w*.5f,h*.49f,13,p); c.drawLine(w*.5f-21,h*.49f,w*.5f-7,h*.49f,p); c.drawLine(w*.5f+7,h*.49f,w*.5f+21,h*.49f,p); c.drawLine(w*.5f,h*.49f-21,w*.5f,h*.49f-7,p); c.drawLine(w*.5f,h*.49f+7,w*.5f,h*.49f+21,p); p.setStyle(Paint.Style.FILL);
    }

    private void drawStartScreen(Canvas c,int w,int h) {
        p.setColor(Color.argb(175,5,8,13)); c.drawRect(0,0,w,h,p);
        p.setTextAlign(Paint.Align.CENTER); p.setTypeface(Typeface.DEFAULT_BOLD); p.setColor(Color.WHITE); p.setTextSize(42); c.drawText("KAVIRLAND",w/2f,h*.29f,p);
        p.setTextSize(18); p.setColor(Color.LTGRAY); c.drawText("GOBLINS ARE WAITING",w/2f,h*.35f,p);
        button(c,w*.5f-145,h*.48f,w*.5f+145,h*.48f+72,"START WAVE",Color.rgb(45,150,85));
        button(c,w*.5f-110,h*.61f,w*.5f+110,h*.61f+58,"SETTINGS",Color.rgb(65,75,95));
        p.setTextSize(13);p.setColor(Color.GRAY);c.drawText("Move • Look • Hold FIRE",w/2f,h*.75f,p);p.setTextAlign(Paint.Align.LEFT);
    }

    private void drawWaveBanner(Canvas c,int w,int h) {
        p.setColor(Color.argb(180,8,12,18)); c.drawRoundRect(w*.5f-190,h*.18f,w*.5f+190,h*.18f+78,18,18,p);
        p.setTextAlign(Paint.Align.CENTER);p.setTypeface(Typeface.DEFAULT_BOLD);p.setColor(Color.rgb(255,215,75));p.setTextSize(34);c.drawText("WAVE "+wave,w/2f,h*.18f+48,p);p.setTextAlign(Paint.Align.LEFT);
    }

    private void drawWaveClear(Canvas c,int w,int h) {
        p.setColor(Color.argb(205,8,12,18));c.drawRoundRect(w*.5f-220,h*.32f,w*.5f+220,h*.32f+155,20,20,p);
        p.setTextAlign(Paint.Align.CENTER);p.setTypeface(Typeface.DEFAULT_BOLD);p.setColor(Color.rgb(100,220,125));p.setTextSize(30);c.drawText("WAVE CLEARED",w/2f,h*.32f+45,p);
        p.setTextSize(15);p.setColor(Color.WHITE);c.drawText("Kills: "+kills+"     Gold: "+gold+"     Level: "+level,w/2f,h*.32f+78,p);
        button(c,w*.5f-135,h*.32f+95,w*.5f+135,h*.32f+140,"NEXT WAVE",Color.rgb(55,135,85));
        p.setTextAlign(Paint.Align.LEFT);
    }

    private void drawSettings(Canvas c,int w,int h) {
        p.setColor(Color.rgb(12,17,25));c.drawRect(0,0,w,h,p);
        p.setColor(Color.WHITE);p.setTypeface(Typeface.DEFAULT_BOLD);p.setTextSize(32);c.drawText("SETTINGS",28,55,p);
        p.setTypeface(Typeface.DEFAULT);p.setTextSize(16);p.setColor(Color.LTGRAY);
        c.drawText("Look sensitivity",40,115,p);c.drawText(String.format(java.util.Locale.US,"%.3f",lookSensitivity),w-90,115,p);
        button(c,40,135,w-40,190,"SENSITIVITY  "+(lookSensitivity>.009f?"HIGH":lookSensitivity<.007f?"LOW":"MEDIUM"),Color.rgb(55,65,82));
        button(c,40,210,w-40,265,"AUTO NEXT WAVE: "+(autoWave?"ON":"OFF"),Color.rgb(55,65,82));
        button(c,40,285,w-40,340,"SHOW FPS: "+(showFps?"ON":"OFF"),Color.rgb(55,65,82));
        button(c,40,h-100,w-40,h-45,"BACK TO GAME",Color.rgb(75,105,145));
    }

    private void button(Canvas c,float l,float t,float r,float b,String text,int color) {
        p.setColor(Color.argb(90,0,0,0));c.drawRoundRect(l+3,t+5,r+3,b+5,18,18,p);
        p.setColor(color);c.drawRoundRect(l,t,r,b,18,18,p);
        p.setColor(Color.WHITE);p.setTypeface(Typeface.DEFAULT_BOLD);p.setTextSize(Math.min(20,(b-t)*.34f));p.setTextAlign(Paint.Align.CENTER);c.drawText(text,(l+r)/2,t+(b-t)*.64f,p);p.setTextAlign(Paint.Align.LEFT);
    }

    private void drawDead(Canvas c,int w,int h) {
        p.setColor(Color.argb(205,0,0,0));c.drawRect(0,0,w,h,p);
        p.setTextAlign(Paint.Align.CENTER);p.setTypeface(Typeface.DEFAULT_BOLD);p.setColor(Color.rgb(255,90,80));p.setTextSize(44);c.drawText("YOU DIED",w/2f,h*.37f,p);
        p.setColor(Color.WHITE);p.setTextSize(17);c.drawText("Wave "+wave+"  •  "+kills+" kills  •  "+gold+" gold",w/2f,h*.45f,p);
        button(c,w*.5f-135,h*.54f,w*.5f+135,h*.54f+65,"RESTART",Color.rgb(175,55,55));
        p.setTextSize(13);p.setColor(Color.LTGRAY);c.drawText("Tap RESTART to fight again",w/2f,h*.65f,p);p.setTextAlign(Paint.Align.LEFT);
    }

    @Override public boolean onTouchEvent(MotionEvent e) {
        float x=e.getX(),y=e.getY();int w=getWidth(),h=getHeight();int a=e.getActionMasked();
        if(settings) {
            if(a==MotionEvent.ACTION_DOWN) {
                if(y>135&&y<190){lookSensitivity+=.002f;if(lookSensitivity>.012f)lookSensitivity=.006f;}
                else if(y>210&&y<265)autoWave=!autoWave;
                else if(y>285&&y<340)showFps=!showFps;
                else if(y>h-110){settings=false;}
            }
            return true;
        }
        if(dead) {
            if(a==MotionEvent.ACTION_DOWN && y>h*.50f && y<h*.65f) restart();
            return true;
        }
        if(!started) {
            if(a==MotionEvent.ACTION_DOWN) {
                if(y>h*.47f&&y<h*.57f){started=true;startWave();}
                else if(y>h*.60f&&y<h*.70f)settings=true;
            }
            return true;
        }
        if(goblins.isEmpty() && wave>0 && a==MotionEvent.ACTION_DOWN && y>h*.30f&&y<h*.60f){startWave();return true;}
        if(a==MotionEvent.ACTION_DOWN) {
            if(x<w*.38f&&y>h*.55f){moving=true;setJoy(x,y,w,h);}
            else if(x>w*.62f&&y>h*.55f){firing=true;}
            else if(x>w-90&&y<95){settings=true;}
            else lastLookX=x;
            return true;
        }
        if(a==MotionEvent.ACTION_MOVE) {
            if(moving)setJoy(x,y,w,h);
            else if(x>=w*.42f&&x<=w*.9f&&y<h*.8f){yaw+=(x-lastLookX)*lookSensitivity;lastLookX=x;}
            return true;
        }
        if(a==MotionEvent.ACTION_UP||a==MotionEvent.ACTION_CANCEL){moving=false;joyX=joyY=0;firing=false;return true;}
        return true;
    }

    private void setJoy(float x,float y,int w,int h){
        float dx=(x-95)/68f,dy=(y-(h-105))/68f,len=(float)Math.hypot(dx,dy);
        if(len>1){dx/=len;dy/=len;}joyX=dx;joyY=dy;
    }

    private void restart(){
        goblins.clear();particles.clear();px=py=0;yaw=0;hp=100;maxHp=100;xp=0;level=1;gold=0;kills=0;wave=0;dead=false;started=true;waveClearUntil=0;startWave();
    }

    private static class Goblin {
        float x,y,speed,hp,maxHp;int type;long hitAt;
        Goblin(float x,float y,float speed,int type){this.x=x;this.y=y;this.speed=speed;this.type=type;this.maxHp=type==2?180:(type==1?120:100);this.hp=maxHp;}
    }

    private static class Particle {
        float x,y,vx,vy,life;int color;
        Particle(float x,float y,float vx,float vy,float life,int color){this.x=x;this.y=y;this.vx=vx;this.vy=vy;this.life=life;this.color=color;}
    }

    private void drawParticles(Canvas c) {
        for(Particle q:particles){p.setColor(q.color);p.setAlpha((int)Math.max(0,255*Math.min(1,q.life*2)));c.drawCircle(q.x,q.y,3+q.life*4,p);}
        p.setAlpha(255);
    }
}
