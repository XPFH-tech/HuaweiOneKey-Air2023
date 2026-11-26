package dev.air2023.islandi2;
import android.app.KeyguardManager;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.media.session.*;
import android.media.AudioManager;
import android.widget.TextView;
import android.os.*;
import android.util.Log;
import android.view.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public final class Entry implements IXposedHookLoadPackage{
 private static final String TAG="Air2023IslandI2";
 private static final AtomicBoolean installed=new AtomicBoolean();
 private static Controller active;
 private static void report(String s){try{XposedBridge.log("["+TAG+"] "+s);}catch(Throwable ignored){}try{Log.i(TAG,s);}catch(Throwable ignored){}}
 @Override public void handleLoadPackage(XC_LoadPackage.LoadPackageParam p){
   if(p==null||!"com.android.systemui".equals(p.packageName)||!"com.android.systemui".equals(p.processName))return;
   if(!"DBY2-W00".equals(Build.MODEL)||!"DBY2-W00 4.2.0.252(C00E100R3P5)".equals(Build.DISPLAY)||Build.VERSION.SDK_INT!=31){report("I2_SKIP_BUILD");return;}
   if(!installed.compareAndSet(false,true))return;
   try{
     Class<?> c=Class.forName("com.android.systemui.time.HwStatusBarClock",false,p.classLoader);
     if(!android.widget.TextView.class.isAssignableFrom(c))throw new IllegalStateException("clock type");
     Method m=c.getDeclaredMethod("onAttachedToWindow");
     if(m.getReturnType()!=void.class||Modifier.isStatic(m.getModifiers()))throw new NoSuchMethodException("clock signature");
     XposedBridge.hookMethod(m,new XC_MethodHook(){
       @Override protected void afterHookedMethod(MethodHookParam mp){
         if(mp.getThrowable()!=null||!(mp.thisObject instanceof View))return;
         try{
           final View clock=(View)mp.thisObject;
           clock.post(new Runnable(){public void run(){attach(clock,0);}});
         }catch(Throwable t){report("I2_ATTACH_ERROR "+t);}
       }
     });
     report("I2_HOOK_READY");
   }catch(Throwable t){report("I2_INSTALL_ERROR "+t);}
 }
 private static void attach(final View clock,final int attempt){
   try{
     if(!clock.isAttachedToWindow()||Looper.myLooper()!=Looper.getMainLooper())return;
     View root=clock.getRootView();
     ViewGroup.LayoutParams p=root.getLayoutParams();
     if(!(p instanceof WindowManager.LayoutParams)||root.getWidth()==0||root.getHeight()==0){
       if(attempt<5)clock.postDelayed(new Runnable(){public void run(){attach(clock,attempt+1);}},300);
       else report("I2_SKIP_ROOT_UNREADY");
       return;
     }
     // 2000 = TYPE_STATUS_BAR. No overlay window or hidden flag is created.
     if(((WindowManager.LayoutParams)p).type!=2000||root.getDisplay()==null||root.getDisplay().getDisplayId()!=Display.DEFAULT_DISPLAY||!(root instanceof ViewGroup)){
       report("I2_SKIP_ROOT type="+((WindowManager.LayoutParams)p).type);return;
     }
     if(active!=null&&active.root==root&&!active.closed){
       if(!active.clock.isShown()||!active.clock.isAttachedToWindow())active.clock=(TextView)clock;
       active.update();return;
     }
     if(active!=null)active.close();
     Controller next=new Controller(root,(TextView)clock);active=next;
     try{next.start();}catch(Throwable t){next.close();throw t;}
   }catch(Throwable t){report("I2_ATTACH_ERROR "+t);}
 }
 private static final class Controller{
   final View root;TextView clock;final Context ctx;final Handler h=new Handler(Looper.getMainLooper());
   final PowerManager power;final KeyguardManager keyguard;final MediaSessionManager media;final AudioManager audio;
   final Pill pill=new Pill();final ArrayList<MediaController> sessions=new ArrayList<MediaController>();
   boolean closed,registered,listener,shown,knownBattery,plugged,playing,mediaDenied,collisionLogged,geometryLogged;
   long chargeUntil;int battery=-1;String lastMode="",lastMedia="",lastGeometry="",blocker="",lastBlocker="";boolean audioDenied;
   Controller(View root,TextView clock){
     this.root=root;this.clock=clock;ctx=root.getContext().getApplicationContext();
     if(ctx==null)throw new IllegalStateException("no app context");
     power=(PowerManager)ctx.getSystemService(Context.POWER_SERVICE);
     keyguard=(KeyguardManager)ctx.getSystemService(Context.KEYGUARD_SERVICE);
     media=(MediaSessionManager)ctx.getSystemService(Context.MEDIA_SESSION_SERVICE);
     audio=(AudioManager)ctx.getSystemService(Context.AUDIO_SERVICE);
   }
   final View.OnAttachStateChangeListener detach=new View.OnAttachStateChangeListener(){
     public void onViewAttachedToWindow(View v){}
     public void onViewDetachedFromWindow(View v){close();}
   };
   final BroadcastReceiver rx=new BroadcastReceiver(){
     @Override public void onReceive(Context c,Intent i){
       if(closed)return;
       try{
         if(Intent.ACTION_BATTERY_CHANGED.equals(i.getAction()))battery(i,true);
         if(Intent.ACTION_SCREEN_OFF.equals(i.getAction())){chargeUntil=0;hide();h.removeCallbacks(tick);return;}
         update();
       }catch(Throwable t){report("I2_EVENT_ERROR "+t.getClass().getName());hide();}
     }
   };
   final MediaController.Callback cb=new MediaController.Callback(){
     @Override public void onPlaybackStateChanged(PlaybackState state){readPlaying();update();}
     @Override public void onSessionDestroyed(){refreshSessions();}
   };
   final MediaSessionManager.OnActiveSessionsChangedListener changed=new MediaSessionManager.OnActiveSessionsChangedListener(){
     @Override public void onActiveSessionsChanged(List<MediaController> list){replaceSessions(list);}
   };
   final Runnable tick=new Runnable(){public void run(){update();}};
   void start(){
     root.addOnAttachStateChangeListener(detach);
     IntentFilter f=new IntentFilter(Intent.ACTION_BATTERY_CHANGED);f.addAction(Intent.ACTION_SCREEN_ON);f.addAction(Intent.ACTION_SCREEN_OFF);f.addAction(Intent.ACTION_USER_PRESENT);
     // System broadcasts only; no custom externally callable command receiver.
     Intent sticky=ctx.registerReceiver(rx,f,null,h);registered=true;
     if(sticky!=null&&Intent.ACTION_BATTERY_CHANGED.equals(sticky.getAction()))battery(sticky,false);
     if(media!=null){
       try{
         media.addOnActiveSessionsChangedListener(changed,null,h);listener=true;
         refreshSessions();
       }catch(Throwable t){mediaDenied=true;report("I2_MEDIA_UNAVAILABLE "+t.getClass().getName());}
     }
     report("I2_HOST_READY width="+root.getWidth()+" height="+root.getHeight()+" media="+listener);
     update();
   }
   void battery(Intent i,boolean event){
     int l=i.getIntExtra(BatteryManager.EXTRA_LEVEL,-1),s=i.getIntExtra(BatteryManager.EXTRA_SCALE,0);
     int p=i.getIntExtra(BatteryManager.EXTRA_PLUGGED,-1);
     if(l<0||s<=0||l>s||p<0)return;
     battery=Math.round(l*100f/s);
     boolean next=p!=0;
     if(event&&knownBattery&&!plugged&&next){chargeUntil=SystemClock.elapsedRealtime()+5000;report("I2_CHARGE_EVENT");}
     if(!next)chargeUntil=0;
     knownBattery=true;plugged=next;
   }
   void refreshSessions(){
     if(closed||mediaDenied||media==null)return;
     try{replaceSessions(media.getActiveSessions(null));}
     catch(Throwable t){mediaDenied=true;replaceSessions(null);report("I2_MEDIA_UNAVAILABLE "+t.getClass().getName());}
   }
   void replaceSessions(List<MediaController> list){
     if(closed)return;
     for(MediaController c:sessions)try{c.unregisterCallback(cb);}catch(Throwable ignored){}
     sessions.clear();
     if(list!=null)for(MediaController c:list){
       if(c==null||sessions.size()>=16)continue;
       try{c.registerCallback(cb,h);sessions.add(c);}catch(Throwable ignored){}
     }
     report("I2_SESSIONS count="+sessions.size());readPlaying();update();
   }
   void readPlaying(){
     playing=false;
     for(MediaController c:sessions)try{
       PlaybackState state=c.getPlaybackState();
       if(state!=null&&state.getState()==PlaybackState.STATE_PLAYING){playing=true;break;}
     }catch(Throwable ignored){}
   }
   int dp(float n){return Math.round(root.getResources().getDisplayMetrics().density*n);}
   boolean canShow(){
     return !closed&&root.isAttachedToWindow()&&root.isShown()&&root.getWindowVisibility()==View.VISIBLE&&power!=null&&power.isInteractive()&&keyguard!=null&&!keyguard.isKeyguardLocked()&&!keyguard.isDeviceLocked();
   }
   void update(){
     h.removeCallbacks(tick);
     if(closed)return;
     try{
       long now=SystemClock.elapsedRealtime();
       boolean charge=plugged&&battery>=0&&now<chargeUntil;
       if(!canShow()){
         hide();
         if(power!=null&&power.isInteractive()&&root.isAttachedToWindow()&&root.getWindowVisibility()==View.VISIBLE)
           h.postDelayed(tick,2000);
         return;
       }
       readPlaying();
       boolean music=false;
       if(!audioDenied&&audio!=null)try{music=audio.isMusicActive();}catch(Throwable t){audioDenied=true;report("I2_AUDIO_QUERY_ERROR "+t.getClass().getName());}
       String source=playing?"SESSION":music?"AUDIO":"NONE";
       if(!source.equals(lastMedia)){lastMedia=source;report("I2_MEDIA_STATE "+source);}
       String mode=charge?"CHARGE":(playing||music)?"MEDIA":"";
       if(mode.isEmpty()){hide();h.postDelayed(tick,2000);return;}
       int w=root.getWidth(),height=root.getHeight();
       if(w<dp(360)||height<dp(18)||height>dp(256)||clock.getRootView()!=root||!clock.isAttachedToWindow()||!clock.isShown()||clock.getHeight()<=0){
         if(!geometryLogged){geometryLogged=true;report("I2_SKIP_GEOMETRY "+w+"x"+height+" clock="+clock.getHeight()+" density="+root.getResources().getDisplayMetrics().density);}
         hide();h.postDelayed(tick,1000);return;
       }
       int[] rc=new int[2],cc=new int[2];root.getLocationOnScreen(rc);clock.getLocationOnScreen(cc);
       Paint.FontMetrics fm=clock.getPaint().getFontMetrics();
       int baseline=clock.getBaseline();
       if(baseline<0){hide();h.postDelayed(tick,1000);return;}
       int cy=cc[1]-rc[1]+baseline+Math.round((fm.ascent+fm.descent)/2);
       int ph=Math.max(dp(18),Math.min(dp(30),Math.round(fm.descent-fm.ascent)+dp(4))),pw=Math.min(dp(164),w/3);
       Rect area=new Rect((w-pw)/2,cy-ph/2,(w+pw)/2,cy-ph/2+ph);
       if(area.top<0||area.bottom>height||cy>dp(96)){
         if(!geometryLogged){geometryLogged=true;report("I2_SKIP_ANCHOR center="+cy+" root="+height+" clock="+clock.getHeight());}
         hide();h.postDelayed(tick,1000);return;
       }
       geometryLogged=false;
       String geo=w+"x"+height+" clock="+clock.getWidth()+"x"+clock.getHeight()+" centerY="+cy+" pill="+area.toShortString()+" density="+root.getResources().getDisplayMetrics().density;
       if(!geo.equals(lastGeometry)){lastGeometry=geo;report("I2_GEOMETRY_OK "+geo);}
       Rect screen=new Rect(area);int[] loc=new int[2];root.getLocationOnScreen(loc);screen.offset(loc[0],loc[1]);
       blocker="";
       if(blocked(root,screen,0,new int[]{0})){
         if(!collisionLogged||!blocker.equals(lastBlocker)){collisionLogged=true;lastBlocker=blocker;report("I2_SKIP_OCCUPIED_CENTER "+blocker);}
         hide();h.postDelayed(tick,1000);return;
       }
       collisionLogged=false;
       String msg=charge?"充电中  "+battery+"%":"媒体播放";
       pill.setContent(mode,msg,area,root.getResources().getDisplayMetrics().scaledDensity);
       if(!shown){root.getOverlay().add(pill);shown=true;}
       if(!mode.equals(lastMode)){lastMode=mode;report("I2_SHOWN "+mode);}
       h.postDelayed(tick,charge?Math.max(50,Math.min(1000,chargeUntil-now)):1000);
     }catch(Throwable t){report("I2_RENDER_ERROR "+t.getClass().getName());hide();}
   }
   // Never replace native capsules or icons. Check all visible leaf bounds, conservatively.
   boolean blocked(View v,Rect area,int depth,int[] count){
     if(++count[0]>512||depth>24){blocker="traversal_limit";return true;}
     if(v.getVisibility()!=View.VISIBLE||v.getAlpha()<=0.01f||v.getWidth()==0||v.getHeight()==0)return false;
     if(v!=root){
       Rect r=new Rect();if(!v.getGlobalVisibleRect(r))return false;
       // getGlobalVisibleRect is in root coordinates on Android; compare with screen bounds
       // derived from actual location, clipped to the view's bounds.
       int[] xy=new int[2];v.getLocationOnScreen(xy);
       Rect b=new Rect(xy[0],xy[1],xy[0]+v.getWidth(),xy[1]+v.getHeight());
       if(!Rect.intersects(b,area))return false;
       int cap=ctx.getResources().getIdentifier("capsule_root","id","com.android.systemui");
       if(cap!=0&&v.getId()==cap){blocker=describe(v,b);return true;}
     }
     if(v instanceof ViewGroup){
       ViewGroup g=(ViewGroup)v;
       if(g.getChildCount()==0){if(v!=root&&(v.getBackground()!=null||v.getContentDescription()!=null)){blocker=describe(v,null);return true;}return false;}
       for(int i=0;i<g.getChildCount();i++)if(blocked(g.getChildAt(i),area,depth+1,count))return true;
       return false;
     }
     if(v!=root){blocker=describe(v,null);return true;}return false;
   }
   String describe(View v,Rect bounds){
     String id="none";try{if(v.getId()!=View.NO_ID)id=v.getResources().getResourceEntryName(v.getId());}catch(Throwable ignored){}
     if(bounds==null){int[] p=new int[2];v.getLocationOnScreen(p);bounds=new Rect(p[0],p[1],p[0]+v.getWidth(),p[1]+v.getHeight());}
     return "class="+v.getClass().getName()+" id="+id+" bounds="+bounds.toShortString();
   }
   void hide(){
     if(shown)try{root.getOverlay().remove(pill);}catch(Throwable ignored){}finally{shown=false;}
     lastMode="";
   }
   void close(){
     if(closed)return;closed=true;h.removeCallbacksAndMessages(null);hide();
     try{root.removeOnAttachStateChangeListener(detach);}catch(Throwable ignored){}
     if(registered)try{ctx.unregisterReceiver(rx);}catch(Throwable ignored){}
     if(listener)try{media.removeOnActiveSessionsChangedListener(changed);}catch(Throwable ignored){}
     for(MediaController c:sessions)try{c.unregisterCallback(cb);}catch(Throwable ignored){}
     sessions.clear();if(active==this)active=null;
   }
 }
 private static final class Pill extends Drawable{
   final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);String mode="",msg="";float sp=1;
   void setContent(String m,String s,Rect r,float scale){mode=m;msg=s;sp=scale;setBounds(r);invalidateSelf();}
   @Override public void draw(Canvas c){
     Rect b=getBounds();if(b.isEmpty())return;float radius=b.height()/2f;
     p.setColor(0xff111216);p.setStyle(Paint.Style.FILL);c.drawRoundRect(new RectF(b),radius,radius,p);
     float text=Math.min(12*sp,b.height()*0.52f);
     p.setTextSize(text);p.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));
     float dot=Math.min(b.height()*0.14f,4*sp),gap=Math.max(3,sp*5);
     float total=2*dot+gap+p.measureText(msg);
     if(total>b.width()-8){p.setTextSize(Math.max(8,text*(b.width()-8)/total));total=2*dot+gap+p.measureText(msg);}
     float x=b.exactCenterX()-total/2;
     p.setColor("CHARGE".equals(mode)?0xff76e2a0:0xff91b9ff);c.drawCircle(x+dot,b.exactCenterY(),dot,p);
     p.setColor(0xfff6f7fa);Paint.FontMetrics fm=p.getFontMetrics();c.drawText(msg,x+2*dot+gap,b.exactCenterY()-(fm.ascent+fm.descent)/2,p);
   }
   @Override public void setAlpha(int a){}
   @Override public void setColorFilter(ColorFilter f){}
   @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
 }
}
