package com.kavir.goblinland;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.view.MotionEvent;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

public class GameView extends GLSurfaceView {
    private final Renderer3D renderer;
    private float downX,downY,lastX,lastY;
    private boolean move,look,fire;
    public GameView(Context c){
        super(c);
        setEGLContextClientVersion(2);
        renderer=new Renderer3D(c);
        setRenderer(renderer);
        setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
        setFocusable(true);
    }
    public Renderer3D getRenderer3D(){return renderer;}
    @Override public boolean onTouchEvent(MotionEvent e){
        float x=e.getX(), y=e.getY(), w=getWidth(), h=getHeight();
        int a=e.getActionMasked();
        if(a==MotionEvent.ACTION_DOWN){
            downX=lastX=x; downY=lastY=y;
            if(renderer.menu){
                if(y>h*.43f&&y<h*.58f){renderer.menu=false;renderer.started=true;renderer.startWave();}
                else if(y>h*.59f&&y<h*.72f) renderer.settings=!renderer.settings;
                return true;
            }
            if(renderer.settings){
                if(y>h*.28f&&y<h*.42f) renderer.sensitivity+=.0015f;
                else if(y>h*.42f&&y<h*.56f) renderer.autoWave=!renderer.autoWave;
                else if(y>h*.70f) renderer.settings=false;
                return true;
            }
            if(renderer.dead){
                if(y>h*.52f&&y<h*.68f) renderer.restart();
                return true;
            }
            if(renderer.wave>0 && renderer.goblins.isEmpty() && y>h*.30f&&y<h*.65f){renderer.startWave();return true;}
            if(x<w*.38f && y>h*.52f){move=true;renderer.setJoystick(x,y,w,h);}
            else if(x>w*.68f&&y>h*.55f) fire=true;
            else if(x>w-100&&y<120) renderer.settings=true;
            else look=true;
            return true;
        }
        if(a==MotionEvent.ACTION_MOVE){
            if(move) renderer.setJoystick(x,y,w,h);
            if(look && x>w*.38f){
                renderer.yaw += (x-lastX)*renderer.sensitivity;
                renderer.pitch -= (y-lastY)*renderer.sensitivity*.75f;
                renderer.pitch=Math.max(-1.05f,Math.min(1.05f,renderer.pitch));
            }
            lastX=x;lastY=y;return true;
        }
        if(a==MotionEvent.ACTION_UP||a==MotionEvent.ACTION_CANCEL){
            move=false;look=false;fire=false;renderer.joyX=renderer.joyY=0;
        }
        renderer.firing=fire;
        return true;
    }

    public static class Renderer3D implements GLSurfaceView.Renderer {
        final Random rnd=new Random();
        final ArrayList<Enemy> goblins=new ArrayList<>();
        final ArrayList<Particle> particles=new ArrayList<>();
        final Context ctx;
        int program, mvp, model, color, lightDir, viewPos;
        int width,height;
        float[] projection=new float[16], view=new float[16], vp=new float[16], modelM=new float[16], tmp=new float[16];
        Mesh cube,sphere,ground,cone;
        float px=0,pz=0,yaw=0,pitch=0,joyX=0,joyY=0;
        float sensitivity=.0065f;
        boolean started=false,menu=true,settings=false,dead=false,firing=false,autoWave=true;
        int wave=0,hp=100,maxHp=100,xp=0,level=1,gold=0,kills=0,shots=0;
        long lastNs=0,shotNs=0,waveBanner=0,waveClear=0;
        float recoil=0,flash=0,damageFlash=0;

        Renderer3D(Context c){ctx=c;}

        @Override public void onSurfaceCreated(javax.microedition.khronos.egl.EGLConfig cfg){
            GLES20.glClearColor(.035f,.055f,.085f,1);
            GLES20.glEnable(GLES20.GL_DEPTH_TEST);
            GLES20.glEnable(GLES20.GL_CULL_FACE);
            GLES20.glCullFace(GLES20.GL_BACK);
            program=makeProgram(
                "uniform mat4 uMVP; uniform mat4 uModel; attribute vec3 aPos; attribute vec3 aNormal; varying vec3 vN; varying vec3 vP; void main(){vec4 p=uModel*vec4(aPos,1.0);vP=p.xyz;vN=mat3(uModel)*aNormal;gl_Position=uMVP*vec4(aPos,1.0);}",
                "precision mediump float; uniform vec4 uColor; uniform vec3 uLight; uniform vec3 uView; varying vec3 vN; varying vec3 vP; void main(){vec3 n=normalize(vN);float d=max(dot(n,normalize(uLight)),0.0);float rim=pow(1.0-max(dot(n,normalize(uView-vP)),0.0),2.0);vec3 c=uColor.rgb*(.25+d*.75)+vec3(.08)*rim;gl_FragColor=vec4(c,uColor.a);}"
            );
            mvp=GLES20.glGetUniformLocation(program,"uMVP");
            model=GLES20.glGetUniformLocation(program,"uModel");
            color=GLES20.glGetUniformLocation(program,"uColor");
            lightDir=GLES20.glGetUniformLocation(program,"uLight");
            viewPos=GLES20.glGetUniformLocation(program,"uView");
            cube=Mesh.cube(); sphere=Mesh.sphere(12,8); ground=Mesh.cube(); cone=Mesh.cone(10);
        }
        @Override public void onSurfaceChanged(javax.microedition.khronos.opengles.GL10 gl,int w,int h){
            width=w;height=h;GLES20.glViewport(0,0,w,h);
            Matrix.perspectiveM(projection,0,72f,(float)w/h,.08f,100f);
        }
        @Override public void onDrawFrame(javax.microedition.khronos.opengles.GL10 gl){
            long now=System.nanoTime();
            float dt=lastNs==0?0.016f:Math.min(.045f,(now-lastNs)/1e9f);lastNs=now;
            update(dt,now);
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT|GLES20.GL_DEPTH_BUFFER_BIT);
            Matrix.setLookAtM(view,0,px,1.65f,pz,px+(float)Math.sin(yaw)*10f,1.65f+(float)Math.sin(pitch)*4f,pz-(float)Math.cos(yaw)*10f,0,1,0);
            Matrix.multiplyMM(vp,0,projection,0,view,0);
            drawWorld();
            drawEnemies();
            drawWeapon();
            drawParticles();
        }
        void update(float dt,long now){
            recoil=Math.max(0,recoil-dt*5);flash=Math.max(0,flash-dt*7);damageFlash=Math.max(0,damageFlash-dt*2);
            if(!started||menu||settings||dead)return;
            float sy=(float)Math.sin(yaw),cy=(float)Math.cos(yaw);
            px += (joyX*cy + joyY*sy)*5.0f*dt;
            pz += (joyX*sy - joyY*cy)*5.0f*dt;
            px=Math.max(-45,Math.min(45,px));pz=Math.max(-45,Math.min(45,pz));
            for(Iterator<Enemy> it=goblins.iterator();it.hasNext();){
                Enemy g=it.next();float dx=px-g.x,dz=pz-g.z,d=(float)Math.hypot(dx,dz);
                if(d>1.15f){g.x+=dx/d*g.speed*dt;g.z+=dz/d*g.speed*dt;}
                else if(now-g.hit>500){hp-=g.type==2?14:8;g.hit=now;damageFlash=1;if(hp<=0){hp=0;dead=true;firing=false;}}
            }
            if(firing&&now-shotNs>170_000_000L){shoot(now);shotNs=now;}
            if(goblins.isEmpty()&&wave>0){
                if(waveClear==0)waveClear=now+900_000_000L;
                if(autoWave&&now>waveClear){waveClear=0;startWave();}
            }
            for(Iterator<Particle> it=particles.iterator();it.hasNext();){Particle p=it.next();p.life-=dt;p.x+=p.vx*dt;p.y+=p.vy*dt;p.z+=p.vz*dt;if(p.life<=0)it.remove();}
            while(particles.size()>220)particles.remove(0);
        }
        void startWave(){
            if(!goblins.isEmpty()||dead)return;
            wave++;int count=5+wave*2;
            for(int i=0;i<count;i++){
                double a=rnd.nextDouble()*Math.PI*2;float d=12+rnd.nextFloat()*18;
                int type=wave>=4&&i%7==0?2:(wave>=2&&i%4==0?1:0);
                goblins.add(new Enemy((float)Math.sin(a)*d,(float)Math.cos(a)*d,type,wave));
            }
            waveBanner=System.nanoTime()+1_600_000_000L;
        }
        void shoot(long now){
            shots++;recoil=1;flash=1;Enemy target=null;float best=999;
            float fx=(float)Math.sin(yaw),fz=-(float)Math.cos(yaw);
            for(Enemy g:goblins){
                float dx=g.x-px,dz=g.z-pz,d=(float)Math.hypot(dx,dz);
                float dot=(dx*fx+dz*fz)/Math.max(.01f,d);
                if(dot>.965f&&d<28&&d<best){best=d;target=g;}
            }
            if(target!=null){
                int dmg=target.type==2?26:36;target.hp-=dmg;
                spawnBurst(target.x,target.y,target.z,target.type==2?new float[]{1,.18f,.1f}:new float[]{.5f,1,.25f},14);
                if(target.hp<=0){
                    goblins.remove(target);kills++;gold+=target.type==2?8:2+rnd.nextInt(4);xp+=target.type==2?50:target.type==1?30:20;
                    if(xp>=level*100){xp-=level*100;level++;maxHp+=6;hp=Math.min(maxHp,hp+20);spawnBurst(px,1.4f,pz,new float[]{1,.8f,.1f},28);}
                }
            }
        }
        void spawnBurst(float x,float y,float z,float[] c,int n){
            for(int i=0;i<n;i++){double a=rnd.nextDouble()*Math.PI*2;float s=1+rnd.nextFloat()*3;particles.add(new Particle(x,y,z,(float)Math.cos(a)*s,(rnd.nextFloat()-.2f)*s,(float)Math.sin(a)*s,.25f+rnd.nextFloat()*.45f,c));}
        }
        void drawWorld(){
            // Large lit terrain tiles, rocks and trees give depth and a clear scale reference.
            drawBox(0,-.18f,0,90,.3f,90,new float[]{.12f,.20f,.12f});
            for(int x=-40;x<=40;x+=5)for(int z=-40;z<=40;z+=5) if((x*13+z*7)%17==0)drawBox(x,.02f,z,.08f,.02f,5,new float[]{.18f,.28f,.16f});
            for(int i=-40;i<=40;i+=10){drawBox(i,1.5f,-38,1.0f,3,1.0f,new float[]{.16f,.23f,.14f});drawCone(i,4.0f,-38,3.2f,5,new float[]{.08f,.25f,.12f});}
            for(int i=-35;i<=35;i+=10){drawBox(i,.9f,35,1.0f,1.8f,1.0f,new float[]{.25f,.20f,.13f});drawSphere(i,2.4f,35,1.8f,new float[]{.08f,.30f,.13f});}
            for(int i=0;i<7;i++){float x=-30+i*9;float z=-18+(i%3)*12;drawBox(x,1,z,2,2,2,new float[]{.23f,.24f,.26f});}
        }
        void drawEnemies(){
            for(Enemy g:goblins){
                float s=g.type==2?1.25f:g.type==1?.88f:1;
                float[] body=g.type==2?new float[]{.38f,.08f,.50f}:g.type==1?new float[]{.12f,.42f,.55f}:new float[]{.18f,.48f,.16f};
                drawBox(g.x,1.0f*s,g.z,.75f*s,1.5f*s,.58f*s,body);
                drawSphere(g.x,2.05f*s,g.z,.58f*s,g.type==2?new float[]{.55f,.16f,.65f}:new float[]{.25f,.58f,.18f});
                drawCone(g.x-.48f*s,2.22f*s,g.z,.38f*s,3,g.type==2?new float[]{.7f,.2f,.7f}:new float[]{.3f,.7f,.2f});
                drawCone(g.x+.48f*s,2.22f*s,g.z,.38f*s,3,g.type==2?new float[]{.7f,.2f,.7f}:new float[]{.3f,.7f,.2f});
                drawSphere(g.x-.20f*s,2.12f*s-.05f,g.z-.50f*s,.07f*s,new float[]{1,.85f,.1f});
                drawSphere(g.x+.20f*s,2.12f*s-.05f,g.z-.50f*s,.07f*s,new float[]{1,.85f,.1f});
            }
        }
        void drawWeapon(){
            float fx=(float)Math.sin(yaw),fz=-(float)Math.cos(yaw);
            float sx=px+fx*0.65f,sz=pz+fz*0.65f;
            // Weapon sits in front of the camera and recoils backward while firing.
            float k=.42f-recoil*.12f;
            drawBox(sx+0.58f,1.02f+k,sz,1.0f,.28f,.38f,new float[]{.08f,.09f,.11f});
            drawBox(sx+0.58f,1.28f+k,sz-.02f,.55f,.18f,.24f,new float[]{.22f,.24f,.27f});
            drawBox(sx+0.58f,0.72f+k,sz+.12f,.24f,.55f,.22f,new float[]{.10f,.11f,.13f});
            if(flash>.05f){drawCone(sx+0.58f,1.30f+k,sz-.45f, .45f,5,new float[]{1,.65f,.08f});}
        }
        void drawParticles(){for(Particle p:particles)drawSphere(p.x,p.y,p.z,.07f+p.life*.12f,p.c);}
        void setJoystick(float x,float y,float w,float h){float dx=(x-95)/68f,dy=(y-(h-105))/68f;float l=(float)Math.hypot(dx,dy);if(l>1){dx/=l;dy/=l;}joyX=dx;joyY=dy;}

        void drawBox(float x,float y,float z,float sx,float sy,float sz,float[] c){drawMesh(cube,x,y,z,sx,sy,sz,c);}
        void drawSphere(float x,float y,float z,float s,float[] c){drawMesh(sphere,x,y,z,s,s,s,c);}
        void drawCone(float x,float y,float z,float s,float h,float[] c){drawMesh(cone,x,y,z,s,h,s,c);}
        void drawMesh(Mesh mesh,float x,float y,float z,float sx,float sy,float sz,float[] c){
            Matrix.setIdentityM(modelM,0);Matrix.translateM(modelM,0,x,y,z);Matrix.scaleM(modelM,0,sx,sy,sz);
            Matrix.multiplyMM(tmp,0,vp,0,modelM,0);
            GLES20.glUseProgram(program);GLES20.glUniformMatrix4fv(mvp,1,false,tmp,0);GLES20.glUniformMatrix4fv(model,1,false,modelM,0);
            GLES20.glUniform4f(color,c[0],c[1],c[2],1);GLES20.glUniform3f(lightDir,-.45f,1f,.35f);GLES20.glUniform3f(viewPos,px,1.6f,pz);
            mesh.draw(program);
        }
        void restart(){goblins.clear();particles.clear();px=pz=0;yaw=pitch=0;hp=100;maxHp=100;xp=0;level=1;gold=0;kills=0;wave=0;dead=false;started=true;waveClear=0;startWave();}

        int makeProgram(String vs,String fs){
            int v=shader(GLES20.GL_VERTEX_SHADER,vs),f=shader(GLES20.GL_FRAGMENT_SHADER,fs),p=GLES20.glCreateProgram();
            GLES20.glAttachShader(p,v);GLES20.glAttachShader(p,f);GLES20.glLinkProgram(p);return p;
        }
        int shader(int type,String src){int s=GLES20.glCreateShader(type);GLES20.glShaderSource(s,src);GLES20.glCompileShader(s);return s;}
    }

    static class Enemy{
        float x,z,y=0,hp,maxHp,speed;int type;long hit;
        Enemy(float x,float z,int type,int wave){this.x=x;this.z=z;this.type=type;maxHp=type==2?190:type==1?125:100;hp=maxHp;speed=(type==2?1.25f:type==1?2.2f:1.55f)+wave*.035f;}
    }
    static class Particle{
        float x,y,z,vx,vy,vz,life;float[] c;
        Particle(float x,float y,float z,float vx,float vy,float vz,float life,float[] c){this.x=x;this.y=y;this.z=z;this.vx=vx;this.vy=vy;this.vz=vz;this.life=life;this.c=c;}
    }
    static class Mesh{
        FloatBuffer v,n;int count;
        Mesh(float[] vv,float[] nn){count=vv.length/3;v=buf(vv);n=buf(nn);}
        static FloatBuffer buf(float[] a){ByteBuffer b=ByteBuffer.allocateDirect(a.length*4).order(ByteOrder.nativeOrder());FloatBuffer f=b.asFloatBuffer();f.put(a).position(0);return f;}
        void draw(int program){
            int ap=GLES20.glGetAttribLocation(program,"aPos"),an=GLES20.glGetAttribLocation(program,"aNormal");
            v.position(0);GLES20.glEnableVertexAttribArray(ap);GLES20.glVertexAttribPointer(ap,3,GLES20.GL_FLOAT,false,0,v);
            n.position(0);GLES20.glEnableVertexAttribArray(an);GLES20.glVertexAttribPointer(an,3,GLES20.GL_FLOAT,false,0,n);
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES,0,count);GLES20.glDisableVertexAttribArray(ap);GLES20.glDisableVertexAttribArray(an);
        }
        static Mesh cube(){
            float[] p={-1,-1,1,1,-1,1,1,1,1,-1,-1,1,1,1,1,-1,1,1,-1,-1,-1,-1,1,-1,1,1,-1,-1,-1,-1,1,1,-1,1,1,1,-1,1,1,-1,-1,-1,-1,1,-1,1,1,1,1,-1,1,-1,-1,-1,1,-1,1,1,1,1,1,-1,-1,-1,-1,-1,-1,1,-1,1,1,1,1,1,-1,-1,1,1,1,-1,-1,1,-1,1,-1,-1,-1,-1,1,-1,1,-1,1,1,1,-1,1,1,1};
            float[] no={0,0,1,0,0,1,0,0,1,0,0,1,0,0,1,0,0,1,0,0,-1,0,0,-1,0,0,-1,0,0,-1,0,0,-1,1,0,0,1,0,0,0,1,0,1,0,0,1,0,0,1,0,0,-1,0,0,-1,0,0,-1,0,0,-1,0,0,-1,0,0,-1,0,1,0,0,1,0,0,1,0,0,1,0,0,1,0,0,1,0,0,-1,0,0,-1,0,0,-1,0,0,-1,0,0,-1,0,0};
            return new Mesh(p,no);
        }
        static Mesh sphere(int seg,int rings){
            ArrayList<Float> pv=new ArrayList<>(),nv=new ArrayList<>();
            for(int r=0;r<rings;r++){double t1=Math.PI*r/rings,t2=Math.PI*(r+1)/rings;
                for(int s=0;s<seg;s++){double a1=2*Math.PI*s/seg,a2=2*Math.PI*(s+1)/seg;
                    addTri(pv,nv,t1,a1,t2,a1);addTri(pv,nv,t2,a1,t2,a2);addTri(pv,nv,t1,a1,t2,a2);addTri(pv,nv,t1,a1,t1,a2);
                }}
            return new Mesh(to(pv),to(nv));
        }
        static void addTri(ArrayList<Float> p,ArrayList<Float> n,double t1,double a1,double t2,double a2){
            float x1=(float)(Math.sin(t1)*Math.cos(a1)),y1=(float)Math.cos(t1),z1=(float)(Math.sin(t1)*Math.sin(a1));
            float x2=(float)(Math.sin(t2)*Math.cos(a1)),y2=(float)Math.cos(t2),z2=(float)(Math.sin(t2)*Math.sin(a1));
            float x3=(float)(Math.sin(t2)*Math.cos(a2)),y3=(float)Math.cos(t2),z3=(float)(Math.sin(t2)*Math.sin(a2));
            p.add(x1);p.add(y1);p.add(z1);n.add(x1);n.add(y1);n.add(z1);p.add(x2);p.add(y2);p.add(z2);n.add(x2);n.add(y2);n.add(z2);p.add(x3);p.add(y3);p.add(z3);n.add(x3);n.add(y3);n.add(z3);
        }
        static Mesh cone(int seg){
            ArrayList<Float> p=new ArrayList<>(),n=new ArrayList<>();
            for(int i=0;i<seg;i++){double a=2*Math.PI*i/seg,b=2*Math.PI*(i+1)/seg;
                float[] q={(float)Math.cos(a),0,(float)Math.sin(a),(float)Math.cos(b),0,(float)Math.sin(b),0,1,0};
                for(int j=0;j<9;j++)p.add(q[j]);
                for(int j=0;j<3;j++){n.add(q[j]);n.add(q[j+3]);n.add(0f);n.add(1f);n.add(0f);}
            }return new Mesh(to(p),to(n));
        }
        static float[] to(ArrayList<Float>a){float[] r=new float[a.size()];for(int i=0;i<r.length;i++)r[i]=a.get(i);return r;}
    }
}