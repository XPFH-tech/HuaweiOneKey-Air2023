package dev.air2023.islandi3;
import android.app.KeyguardManager;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.media.session.*;
import android.media.AudioManager;
import android.media.AudioDeviceInfo;
import android.media.AudioDeviceCallback;
import android.media.MediaMetadata;
import android.text.TextUtils;
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
 private static final String TAG="Air2023IslandI3";
 private static final AtomicBoolean installed=new AtomicBoolean();
 private static Controller active;
 private static void report(String s){try{XposedBridge.log("["+TAG+"] "+s);}catch(Throwable ignored){}try{Log.i(TAG,s);}catch(Throwable ignored){}}
 @Override public void handleLoadPackage(XC_LoadPackage.LoadPackageParam p){
   if(p==null||!"com.android.systemui".equals(p.packageName)||!"com.android.systemui".equals(p.processName))return;
   if(!"DBY2-W00".equals(Build.MODEL)||!"DBY2-W00 4.2.0.252(C00E100R3P5)".equals(Build.DISPLAY)||Build.VERSION.SDK_INT!=31){report("I3_SKIP_BUILD");return;}
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
         }catch(Throwable t){report("I3_ATTACH_ERROR "+t);}
       }
     });
     report("I3_HOOK_READY");
   }catch(Throwable t){report("I3_INSTALL_ERROR "+t);}
 }
 private static void attach(final View clock,final int attempt){
   try{
     if(!clock.isAttachedToWindow()||Looper.myLooper()!=Looper.getMainLooper())return;
     View root=clock.getRootView();
     ViewGroup.LayoutParams p=root.getLayoutParams();
     if(!(p instanceof WindowManager.LayoutParams)||root.getWidth()==0||root.getHeight()==0){
       if(attempt<5)clock.postDelayed(new Runnable(){public void run(){attach(clock,attempt+1);}},300);
       else report("I3_SKIP_ROOT_UNREADY");
       return;
     }
     // 2000 = TYPE_STATUS_BAR. No overlay window or hidden flag is created.
     if(((WindowManager.LayoutParams)p).type!=2000||root.getDisplay()==null||root.getDisplay().getDisplayId()!=Display.DEFAULT_DISPLAY||!(root instanceof ViewGroup)){
       report("I3_SKIP_ROOT type="+((WindowManager.LayoutParams)p).type);return;
     }
     if(active!=null&&active.root==root&&!active.closed){
       if(!active.clock.isShown()||!active.clock.isAttachedToWindow())active.clock=(TextView)clock;
       active.update();return;
     }
     if(active!=null)active.close();
     Controller next=new Controller(root,(TextView)clock);active=next;
     try{next.start();}catch(Throwable t){next.close();throw t;}
   }catch(Throwable t){report("I3_ATTACH_ERROR "+t);}
 }
 private static final class Controller{
   final View root;TextView clock;final Context ctx;final Handler h=new Handler(Looper.getMainLooper());
   final PowerManager power;final KeyguardManager keyguard;final MediaSessionManager media;final AudioManager audio;
   final Pill pill=new Pill();final ArrayList<MediaController> sessions=new ArrayList<MediaController>();
   boolean closed,registered,listener,shown,knownBattery,plugged,playing,mediaDenied,collisionLogged,geometryLogged;
   long chargeUntil;int battery=-1;String lastMode="",lastMedia="",lastGeometry="",blocker="",lastBlocker="";boolean audioDenied;
   boolean deviceRegistered,deviceBaseline,volumeBaseline;
   final Set<Integer> btIds=new HashSet<Integer>();
   int oldVolume=-1,oldMax=-1,volumePct;
   long volumeUntil,btUntil,mediaRefreshAt;
   MediaController selected;
   MediaMetadata cachedMetadata;
   Bitmap cover;
   String song="",artist="";
   boolean metadataDirty=true;
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
         if(Intent.ACTION_SCREEN_OFF.equals(i.getAction())){chargeUntil=volumeUntil=btUntil=0;volumeBaseline=false;clearMetadata();hide();h.removeCallbacks(tick);return;}
         update();
       }catch(Throwable t){report("I3_EVENT_ERROR "+t.getClass().getName());hide();}
     }
   };
   final MediaController.Callback cb=new MediaController.Callback(){
     @Override public void onPlaybackStateChanged(PlaybackState state){readPlaying();update();}
     @Override public void onMetadataChanged(MediaMetadata m){metadataDirty=true;update();}
     @Override public void onSessionDestroyed(){refreshSessions();}
   };
   final MediaSessionManager.OnActiveSessionsChangedListener changed=new MediaSessionManager.OnActiveSessionsChangedListener(){
     @Override public void onActiveSessionsChanged(List<MediaController> list){replaceSessions(list);}
   };
   final Runnable tick=new Runnable(){public void run(){update();}};
   final AudioDeviceCallback devices=new AudioDeviceCallback(){
     @Override public void onAudioDevicesAdded(AudioDeviceInfo[] a){deviceChanged();}
     @Override public void onAudioDevicesRemoved(AudioDeviceInfo[] a){deviceChanged();}
   };
   void deviceChanged(){
     if(closed)return;
     try{
       Set<Integer> next=bluetoothOutputs();
       Set<Integer> fresh=new HashSet<Integer>(next);fresh.removeAll(btIds);
       if(deviceBaseline&&!fresh.isEmpty()&&canShow()){
         btUntil=SystemClock.elapsedRealtime()+3500;report("I3_BT_AUDIO_EVENT");
       }
       if(next.isEmpty())btUntil=0;
       btIds.clear();btIds.addAll(next);deviceBaseline=true;
       update();
     }catch(Throwable t){report("I3_BT_QUERY_ERROR "+t.getClass().getName());}
   }
   Set<Integer> bluetoothOutputs(){
     Set<Integer> out=new HashSet<Integer>();
     if(audio==null)return out;
     for(AudioDeviceInfo d:audio.getDevices(AudioManager.GET_DEVICES_OUTPUTS)){
       int type=d.getType();
       // A2DP, SCO, hearing aid, BLE headset/speaker (API31).
       if(type==AudioDeviceInfo.TYPE_BLUETOOTH_A2DP||type==AudioDeviceInfo.TYPE_BLUETOOTH_SCO||
          type==AudioDeviceInfo.TYPE_HEARING_AID||type==AudioDeviceInfo.TYPE_BLE_HEADSET||
          type==AudioDeviceInfo.TYPE_BLE_SPEAKER)out.add(d.getId());
     }
     return out;
   }
   void readVolume(long now){
     if(audio==null)return;
     try{
       int v=audio.getStreamVolume(AudioManager.STREAM_MUSIC),max=audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC),min=audio.getStreamMinVolume(AudioManager.STREAM_MUSIC);
       if(max<=min||v<min||v>max)return;
       volumePct=Math.round((v-min)*100f/(max-min));
       if(volumeBaseline&&oldMax==max&&oldVolume!=v){
         volumeUntil=now+2500;report("I3_VOLUME_EVENT percent="+volumePct);
       }
       oldVolume=v;oldMax=max;volumeBaseline=true;
     }catch(Throwable ignored){}
   }
   void clearMetadata(){
     selected=null;cachedMetadata=null;cover=null;song=artist="";metadataDirty=true;
     pill.clearMedia();
   }
   static String clean(CharSequence raw){
     if(raw==null)return "";
     // Strip controls, format/bidi overrides and line breaks; bound before drawing.
     String s=raw.toString();if(s.length()>512)s=s.substring(0,512);
     StringBuilder b=new StringBuilder();
     for(int i=0;i<s.length()&&b.length()<96;){
       int cp=s.codePointAt(i);i+=Character.charCount(cp);
       int type=Character.getType(cp);
       if(Character.isISOControl(cp)||type==Character.FORMAT||type==Character.LINE_SEPARATOR||type==Character.PARAGRAPH_SEPARATOR)continue;
       b.appendCodePoint(cp);
     }
     return b.toString().trim();
   }
   void readMetadata(MediaController c,long now){
     if(c==null){if(selected!=null||cover!=null||!song.isEmpty())clearMetadata();return;}
     if(selected!=c){clearMetadata();selected=c;}
     if(!metadataDirty&&now-mediaRefreshAt<5000)return;
     mediaRefreshAt=now;metadataDirty=false;
     try{
       MediaMetadata m=c.getMetadata();
       if(m==cachedMetadata)return;
       cachedMetadata=m;song=artist="";cover=null;
       if(m==null)return;
       song=clean(m.getText(MediaMetadata.METADATA_KEY_DISPLAY_TITLE));
       if(song.isEmpty())song=clean(m.getText(MediaMetadata.METADATA_KEY_TITLE));
       artist=clean(m.getText(MediaMetadata.METADATA_KEY_ARTIST));
       if(artist.isEmpty())artist=clean(m.getText(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE));
       Bitmap b=m.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON);
       if(b==null)b=m.getBitmap(MediaMetadata.METADATA_KEY_ART);
       if(b==null)b=m.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART);
       // No URI or network fetching, no file writes, never recycle a foreign bitmap.
       if(b!=null&&!b.isRecycled()&&b.getWidth()>0&&b.getHeight()>0&&b.getWidth()<=2048&&b.getHeight()<=2048&&b.getConfig()!=Bitmap.Config.HARDWARE){
         int w=b.getWidth(),h=b.getHeight();float scale=Math.min(1f,128f/Math.max(w,h));
         cover=Bitmap.createScaledBitmap(b,Math.max(1,Math.round(w*scale)),Math.max(1,Math.round(h*scale)),true);
       }
       report("I3_METADATA title="+(!song.isEmpty())+" artist="+(!artist.isEmpty())+" cover="+(cover!=null));
     }catch(Throwable t){cover=null;report("I3_METADATA_ERROR "+t.getClass().getName());}
   }
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
       }catch(Throwable t){mediaDenied=true;report("I3_MEDIA_UNAVAILABLE "+t.getClass().getName());}
     }
     if(audio!=null)try{
       btIds.addAll(bluetoothOutputs());deviceBaseline=true;
       audio.registerAudioDeviceCallback(devices,h);deviceRegistered=true;
     }catch(Throwable t){report("I3_BT_UNAVAILABLE "+t.getClass().getName());}
     report("I3_HOST_READY width="+root.getWidth()+" height="+root.getHeight()+" media="+listener);
     update();
   }
   void battery(Intent i,boolean event){
     int l=i.getIntExtra(BatteryManager.EXTRA_LEVEL,-1),s=i.getIntExtra(BatteryManager.EXTRA_SCALE,0);
     int p=i.getIntExtra(BatteryManager.EXTRA_PLUGGED,-1);
     if(l<0||s<=0||l>s||p<0)return;
     battery=Math.round(l*100f/s);
     boolean next=p!=0;
     if(event&&knownBattery&&!plugged&&next){chargeUntil=SystemClock.elapsedRealtime()+5000;report("I3_CHARGE_EVENT");}
     if(!next)chargeUntil=0;
     knownBattery=true;plugged=next;
   }
   void refreshSessions(){
     if(closed||mediaDenied||media==null)return;
     try{replaceSessions(media.getActiveSessions(null));}
     catch(Throwable t){mediaDenied=true;replaceSessions(null);report("I3_MEDIA_UNAVAILABLE "+t.getClass().getName());}
   }
   void replaceSessions(List<MediaController> list){
     if(closed)return;
     for(MediaController c:sessions)try{c.unregisterCallback(cb);}catch(Throwable ignored){}
     sessions.clear();
     if(list!=null)for(MediaController c:list){
       if(c==null||sessions.size()>=16)continue;
       try{c.registerCallback(cb,h);sessions.add(c);}catch(Throwable ignored){}
     }
     metadataDirty=true;report("I3_SESSIONS count="+sessions.size());readPlaying();update();
   }
   MediaController readPlaying(){
     playing=false;
     for(MediaController c:sessions)try{
       PlaybackState state=c.getPlaybackState();
       if(state!=null&&state.getState()==PlaybackState.STATE_PLAYING){playing=true;return c;}
     }catch(Throwable ignored){}
     return null;
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
         clearMetadata();volumeBaseline=false;volumeUntil=btUntil=0;hide();
         if(power!=null&&power.isInteractive()&&root.isAttachedToWindow()&&root.getWindowVisibility()==View.VISIBLE)
           h.postDelayed(tick,2000);
         return;
       }
       MediaController current=readPlaying();
       readMetadata(current,now);readVolume(now);
       boolean music=false;
       if(!audioDenied&&audio!=null)try{music=audio.isMusicActive();}catch(Throwable t){audioDenied=true;report("I3_AUDIO_QUERY_ERROR "+t.getClass().getName());}
       String source=playing?"SESSION":music?"AUDIO":"NONE";
       if(!source.equals(lastMedia)){lastMedia=source;report("I3_MEDIA_STATE "+source);}
       boolean bt=now<btUntil,vol=now<volumeUntil;
       String mode=charge?"CHARGE":bt?"BT":vol?"VOLUME":(playing||music)?"MEDIA":"";
       if(mode.isEmpty()){hide();h.postDelayed(tick,2000);return;}
       int w=root.getWidth(),height=root.getHeight();
       if(w<dp(360)||height<dp(18)||height>dp(256)||clock.getRootView()!=root||!clock.isAttachedToWindow()||!clock.isShown()||clock.getHeight()<=0){
         if(!geometryLogged){geometryLogged=true;report("I3_SKIP_GEOMETRY "+w+"x"+height+" clock="+clock.getHeight()+" density="+root.getResources().getDisplayMetrics().density);}
         hide();h.postDelayed(tick,1000);return;
       }
       int[] rc=new int[2],cc=new int[2];root.getLocationOnScreen(rc);clock.getLocationOnScreen(cc);
       Paint.FontMetrics fm=clock.getPaint().getFontMetrics();
       int baseline=clock.getBaseline();
       if(baseline<0){hide();h.postDelayed(tick,1000);return;}
       int cy=cc[1]-rc[1]+baseline+Math.round((fm.ascent+fm.descent)/2);
       int ph=Math.max(dp(18),Math.min(dp(30),Math.round(fm.descent-fm.ascent)+dp(4))),pw=Math.min(dp(164),w/3);
       boolean rich="MEDIA".equals(mode)&&!song.isEmpty();
       if(rich)pw=Math.min(dp(224),w/3);
       Rect area=new Rect((w-pw)/2,cy-ph/2,(w+pw)/2,cy-ph/2+ph);
       if(area.top<0||area.bottom>height||cy>dp(96)){
         if(!geometryLogged){geometryLogged=true;report("I3_SKIP_ANCHOR center="+cy+" root="+height+" clock="+clock.getHeight());}
         hide();h.postDelayed(tick,1000);return;
       }
       geometryLogged=false;
       String geo=w+"x"+height+" clock="+clock.getWidth()+"x"+clock.getHeight()+" centerY="+cy+" pill="+area.toShortString()+" density="+root.getResources().getDisplayMetrics().density;
       if(!geo.equals(lastGeometry)){lastGeometry=geo;report("I3_GEOMETRY_OK "+geo);}
       Rect screen=new Rect(area);int[] loc=new int[2];root.getLocationOnScreen(loc);screen.offset(loc[0],loc[1]);
       blocker="";
       boolean occupied=blocked(root,screen,0,new int[]{0});
       if(occupied&&rich){
         pw=Math.min(dp(164),w/3);area.set((w-pw)/2,cy-ph/2,(w+pw)/2,cy-ph/2+ph);
         screen.set(area);screen.offset(loc[0],loc[1]);blocker="";rich=false;
         occupied=blocked(root,screen,0,new int[]{0});
       }
       if(occupied){
         if(!collisionLogged||!blocker.equals(lastBlocker)){collisionLogged=true;lastBlocker=blocker;report("I3_SKIP_OCCUPIED_CENTER "+blocker);}
         hide();h.postDelayed(tick,1000);return;
       }
       collisionLogged=false;
       String msg=charge?"充电中  "+battery+"%":bt?"蓝牙音频已连接":vol?"媒体音量  "+volumePct+"%":rich?song:"媒体播放";
       pill.setContent(mode,msg,rich?artist:"",rich?cover:null,volumePct,area,root.getResources().getDisplayMetrics().scaledDensity);
       if(!shown){root.getOverlay().add(pill);shown=true;}
       if(!mode.equals(lastMode)){lastMode=mode;report("I3_SHOWN "+mode);}
       long until=charge?chargeUntil:bt?btUntil:vol?volumeUntil:now+1000;
       h.postDelayed(tick,Math.max(50,Math.min(1000,until-now)));
     }catch(Throwable t){report("I3_RENDER_ERROR "+t.getClass().getName());hide();}
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
     pill.clearMedia();lastMode="";
   }
   void close(){
     if(closed)return;closed=true;h.removeCallbacksAndMessages(null);hide();
     try{root.removeOnAttachStateChangeListener(detach);}catch(Throwable ignored){}
     if(registered)try{ctx.unregisterReceiver(rx);}catch(Throwable ignored){}
     if(listener)try{media.removeOnActiveSessionsChangedListener(changed);}catch(Throwable ignored){}
     if(deviceRegistered)try{audio.unregisterAudioDeviceCallback(devices);}catch(Throwable ignored){}
     clearMetadata();
     for(MediaController c:sessions)try{c.unregisterCallback(cb);}catch(Throwable ignored){}
     sessions.clear();if(active==this)active=null;
   }
 }
 private static final class Pill extends Drawable{
   final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
   final android.text.TextPaint tp=new android.text.TextPaint(Paint.ANTI_ALIAS_FLAG);
   String mode="",msg="",artist="";float sp=1;Bitmap art;int pct;
   void setContent(String m,String s,String a,Bitmap b,int v,Rect r,float scale){
     if(m.equals(mode)&&s.equals(msg)&&a.equals(artist)&&b==art&&v==pct&&r.equals(getBounds())&&scale==sp)return;
     mode=m;msg=s;artist=a;art=b;pct=v;sp=scale;setBounds(r);invalidateSelf();
   }
   void clearMedia(){art=null;msg=artist=mode="";}
   void text(Canvas c,String s,float x,float center,float size,float width,int color){
     if(width<=0)return;
     tp.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));tp.setTextSize(size);tp.setColor(color);
     CharSequence clipped=TextUtils.ellipsize(s,tp,width,TextUtils.TruncateAt.END);
     Paint.FontMetrics fm=tp.getFontMetrics();c.drawText(clipped.toString(),x,center-(fm.ascent+fm.descent)/2,tp);
   }
   @Override public void draw(Canvas c){
     Rect b=getBounds();if(b.isEmpty()||mode.isEmpty())return;
     float h=b.height(),cy=b.exactCenterY(),r=h/2;
     int save=c.save();c.clipRect(b);
     try{
       p.setColor(0xff111216);p.setStyle(Paint.Style.FILL);c.drawRoundRect(new RectF(b),r,r,p);
       float pad=h*.35f,left=b.left+pad,right=b.right-pad;
       int color="CHARGE".equals(mode)?0xff76e2a0:0xff91b9ff;
       if("MEDIA".equals(mode)&&(!artist.isEmpty()||art!=null)){
         float size=h*.76f;RectF tile=new RectF(b.left+h*.12f,cy-size/2,b.left+h*.12f+size,cy+size/2);
         if(art!=null&&!art.isRecycled()){
           int n=c.save();
           try{Path path=new Path();path.addRoundRect(tile,h*.14f,h*.14f,Path.Direction.CW);c.clipPath(path);c.drawBitmap(art,null,tile,p);}finally{c.restoreToCount(n);}
         }else{p.setColor(0xff344b70);c.drawRoundRect(tile,h*.14f,h*.14f,p);text(c,"♪",tile.left+size*.22f,cy,size*.65f,size*.7f,0xffe1ebff);}
         left=tile.right+h*.2f;float bars=h*.7f;right-=bars;
         float width=right-left;
         if(artist.isEmpty())text(c,msg,left,cy,Math.min(12*sp,h*.5f),width,0xfff6f7fa);
         else{text(c,msg,left,cy-h*.22f,Math.min(11*sp,h*.43f),width,0xfff6f7fa);text(c,artist,left,cy+h*.23f,Math.min(9*sp,h*.34f),width,0xffb6c3d6);}
         p.setColor(color);
         for(int i=0;i<4;i++){float bx=right+h*.12f+i*h*.13f, bh=h*(i%2==0?.34f:.56f);c.drawRoundRect(new RectF(bx,cy-bh/2,bx+h*.07f,cy+bh/2),h*.035f,h*.035f,p);}
       }else if("VOLUME".equals(mode)){
         float size=Math.min(11*sp,h*.45f),barX=left+h*.78f,barEnd=right-h*1.12f;
         text(c,"♪",left,cy,h*.58f,h*.6f,color);
         if(barEnd>barX){
           float bh=h*.17f;p.setColor(0xff40434a);c.drawRoundRect(new RectF(barX,cy-bh/2,barEnd,cy+bh/2),bh/2,bh/2,p);
           p.setColor(color);float end=barX+(barEnd-barX)*Math.max(0,Math.min(100,pct))/100f;
           if(end>barX)c.drawRoundRect(new RectF(barX,cy-bh/2,end,cy+bh/2),bh/2,bh/2,p);
         }
         text(c,pct+"%",barEnd+h*.15f,cy,size,h*.98f,0xfff6f7fa);
       }else{
         float size=Math.min(12*sp,h*.52f),dot=Math.min(h*.14f,4*sp),gap=Math.max(3,sp*5);
         tp.setTextSize(size);tp.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));
         float width=Math.max(1,b.width()-2*pad-2*dot-gap);
         String label=TextUtils.ellipsize(msg,tp,width,TextUtils.TruncateAt.END).toString();
         float total=2*dot+gap+tp.measureText(label),x=b.exactCenterX()-total/2;
         p.setColor(color);c.drawCircle(x+dot,cy,dot,p);
         text(c,label,x+2*dot+gap,cy,size,width,0xfff6f7fa);
       }
     }catch(Throwable ignored){}finally{c.restoreToCount(save);}
   }
   @Override public void setAlpha(int a){}
   @Override public void setColorFilter(ColorFilter f){}
   @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
 }
}
