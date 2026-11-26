package dev.air2023.volumev1;
import android.content.*;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.os.*;
import android.util.Log;
import android.view.*;
import android.widget.*;
import java.lang.reflect.*;
import java.util.concurrent.atomic.AtomicBoolean;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public final class Entry implements IXposedHookLoadPackage {
 private static final String TAG="Air2023VolumeV1", ROW="air2023.volume.v1.row";
 private static final AtomicBoolean installed=new AtomicBoolean();
 private static void report(String s){try{XposedBridge.log("["+TAG+"] "+s);}catch(Throwable ignored){}try{Log.i(TAG,s);}catch(Throwable ignored){}}
 @Override public void handleLoadPackage(XC_LoadPackage.LoadPackageParam p){
   if(p==null||!"com.android.systemui".equals(p.packageName)||!"com.android.systemui".equals(p.processName))return;
   if(!"DBY2-W00".equals(Build.MODEL)||!"DBY2-W00 4.2.0.252(C00E100R3P5)".equals(Build.DISPLAY)||Build.VERSION.SDK_INT!=31){report("V1_SKIP_BUILD");return;}
   if(!installed.compareAndSet(false,true))return;
   try{
     Class<?> qs=Class.forName("com.huawei.controlcenter.qs.ControlCenterQs",false,p.classLoader);
     if(!View.class.isAssignableFrom(qs))throw new IllegalStateException("ControlCenterQs not View");
     final Field container=qs.getDeclaredField("mSliderContainer"),brightness=qs.getDeclaredField("mQsBrightnessView");
     if(Modifier.isStatic(container.getModifiers())||Modifier.isStatic(brightness.getModifiers())||!FrameLayout.class.isAssignableFrom(container.getType())||!View.class.isAssignableFrom(brightness.getType()))throw new IllegalStateException("slider fields");
     container.setAccessible(true);brightness.setAccessible(true);
     Method init=qs.getDeclaredMethod("initQsSlider");
     if(init.getReturnType()!=void.class||Modifier.isStatic(init.getModifiers()))throw new NoSuchMethodException("initQsSlider signature");
     XposedBridge.hookMethod(init,new XC_MethodHook(){
       @Override protected void afterHookedMethod(MethodHookParam mp){
         if(mp.getThrowable()!=null)return;
         try{final FrameLayout box=(FrameLayout)container.get(mp.thisObject);final View bright=(View)brightness.get(mp.thisObject);
           if(box==null||bright==null){report("V1_SKIP_NULL");return;}
           if(Looper.myLooper()!=Looper.getMainLooper()){report("V1_SKIP_THREAD");return;}
           add(box,bright);
         }catch(Throwable t){report("V1_ROW_ERROR "+t);}
       }
     });
     report("V1_HOOK_READY");
   }catch(Throwable t){report("V1_INSTALL_ERROR "+t);}
 }
 private static int dp(Context c,int n){return Math.round(c.getResources().getDisplayMetrics().density*n);}
 private static View byId(View root,String n){
   int id=root.getResources().getIdentifier(n,"id","com.android.systemui");
   return id==0?null:root.findViewById(id);
 }
 private static Drawable copy(Drawable d,Context c){
   if(d==null||d.getConstantState()==null)return null;
   return d.getConstantState().newDrawable(c.getResources(),c.getTheme()).mutate();
 }
 private static void add(FrameLayout box,View bright){
   View existing=box.findViewWithTag(ROW);
   if(existing!=null){report("V1_ALREADY_PRESENT");return;}
   Context c=box.getContext();
   View slider=byId(bright,"brightness_slider");
   View seek=byId(slider==null?bright:slider,"slider");
   if(bright.getParent()!=box||box.getChildCount()!=1||!(bright.getLayoutParams() instanceof FrameLayout.LayoutParams)||box.getLayoutParams()==null||!(seek instanceof SeekBar)){
     report("V1_SKIP_LAYOUT children="+box.getChildCount()+" bright="+bright.getClass().getName()+" seek="+(seek==null?"null":seek.getClass().getName()));return;
   }
   FrameLayout.LayoutParams oldBright=(FrameLayout.LayoutParams)bright.getLayoutParams();
   ViewGroup.LayoutParams parent=box.getLayoutParams();
   int oldHeight=parent.height;
   int res=c.getResources().getIdentifier("brightness_height","dimen","com.android.systemui");
   int h=oldBright.height>0?oldBright.height:(res==0?0:c.getResources().getDimensionPixelSize(res));
   if(h<dp(c,24)||h>dp(c,96)||oldBright.width!=ViewGroup.LayoutParams.MATCH_PARENT||
      oldBright.topMargin!=0||oldBright.bottomMargin!=0||
      (oldHeight!=h&&oldHeight!=ViewGroup.LayoutParams.WRAP_CONTENT)){
     report("V1_SKIP_GEOMETRY height="+h+" parentHeight="+oldHeight+" brightWidth="+oldBright.width+" margins="+oldBright.topMargin+","+oldBright.bottomMargin);return;
   }
   AudioManager audio=(AudioManager)c.getSystemService(Context.AUDIO_SERVICE);
   if(audio==null||audio.isVolumeFixed()){report("V1_SKIP_AUDIO");return;}
   int max=audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
   int min=audio.getStreamMinVolume(AudioManager.STREAM_MUSIC);
   if(max<=min||max>100){report("V1_SKIP_RANGE");return;}
   Row row=new Row(c,audio,(SeekBar)seek,h);
   row.setTag(ROW);
   FrameLayout.LayoutParams top=new FrameLayout.LayoutParams(oldBright);top.height=h;top.gravity=(oldBright.gravity<0?Gravity.START:oldBright.gravity&Gravity.RELATIVE_HORIZONTAL_GRAVITY_MASK)|Gravity.TOP;
   FrameLayout.LayoutParams bottom=new FrameLayout.LayoutParams(top);bottom.topMargin=h;
   boolean ok=false;
   try{
     bright.setLayoutParams(top);
     box.addView(row,bottom);
     parent.height=h*2;box.setLayoutParams(parent);
     ok=true;
     report("V1_ROW_ADDED height="+h+" range="+min+".."+max);
   }finally{
     if(!ok){if(row.getParent()==box)box.removeView(row);bright.setLayoutParams(oldBright);parent.height=oldHeight;box.setLayoutParams(parent);}
   }
 }
 private static final class Row extends LinearLayout {
   final AudioManager audio;final SeekBar bar;final TextView icon;
   boolean registered,tracking,failed;int previous=1;
   final BroadcastReceiver receiver=new BroadcastReceiver(){
     @Override public void onReceive(Context c,Intent i){if(!tracking)sync();}
   };
   Row(Context c,AudioManager a,SeekBar source,int h){
     super(c);audio=a;setOrientation(HORIZONTAL);setGravity(Gravity.CENTER_VERTICAL);
     setPadding(dp(c,8),0,dp(c,8),0);
     icon=new TextView(c);icon.setGravity(Gravity.CENTER);icon.setTextSize(13);
     icon.setContentDescription("媒体音量静音或恢复");icon.setFocusable(true);
     addView(icon,new LinearLayout.LayoutParams(dp(c,48),ViewGroup.LayoutParams.MATCH_PARENT));
     bar=new SeekBar(c);bar.setContentDescription("媒体音量");
     bar.setPadding(source.getPaddingLeft(),source.getPaddingTop(),source.getPaddingRight(),source.getPaddingBottom());
     Drawable progress=copy(source.getProgressDrawable(),c),thumb=copy(source.getThumb(),c);
     if(progress!=null)bar.setProgressDrawable(progress);
     if(thumb!=null)bar.setThumb(thumb);
     bar.setProgressTintList(source.getProgressTintList());bar.setThumbTintList(source.getThumbTintList());
     addView(bar,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.MATCH_PARENT,1));
     sync();
     bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
       public void onStartTrackingTouch(SeekBar b){tracking=true;getParent().requestDisallowInterceptTouchEvent(true);}
       public void onStopTrackingTouch(SeekBar b){tracking=false;if(getParent()!=null)getParent().requestDisallowInterceptTouchEvent(false);sync();}
       public void onProgressChanged(SeekBar b,int level,boolean fromUser){if(fromUser)apply(level);}
     });
     icon.setOnClickListener(new OnClickListener(){
       public void onClick(View v){
         try{int current=audio.getStreamVolume(AudioManager.STREAM_MUSIC);int min=audio.getStreamMinVolume(AudioManager.STREAM_MUSIC);
           if(current>min){previous=current;apply(min);}else apply(Math.max(min+1,previous));
           sync();
         }catch(Throwable t){fail(t);}
       }
     });
   }
   void label(int level,int min){icon.setText(level==min?"静音":"音量");}
   void sync(){
     if(failed)return;
     try{
       int min=audio.getStreamMinVolume(AudioManager.STREAM_MUSIC),max=audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC),level=audio.getStreamVolume(AudioManager.STREAM_MUSIC);
       if(max<=min||max>100){throw new IllegalStateException("volume range");}
       bar.setMin(0);bar.setMax(max);bar.setMin(min);bar.setProgress(level);label(level,min);
       icon.setEnabled(min==0);
       if(level>min)previous=level;
     }catch(Throwable t){fail(t);}
   }
   void apply(int value){
     if(failed)return;
     try{
       int min=audio.getStreamMinVolume(AudioManager.STREAM_MUSIC),max=audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
       int n=Math.max(min,Math.min(max,value));
       if(audio.getStreamVolume(AudioManager.STREAM_MUSIC)!=n)audio.setStreamVolume(AudioManager.STREAM_MUSIC,n,0);
       label(audio.getStreamVolume(AudioManager.STREAM_MUSIC),min);
     }catch(Throwable t){fail(t);}
   }
   void fail(Throwable t){failed=true;bar.setEnabled(false);icon.setEnabled(false);report("V1_AUDIO_ERROR "+t);}
   @Override protected void onAttachedToWindow(){
     super.onAttachedToWindow();
     if(!registered)try{
       getContext().registerReceiver(receiver,new IntentFilter("android.media.VOLUME_CHANGED_ACTION"),Context.RECEIVER_EXPORTED);
       registered=true;
     }catch(Throwable t){report("V1_RECEIVER_ERROR "+t);}
     sync();
   }
   @Override protected void onDetachedFromWindow(){
     if(registered)try{getContext().unregisterReceiver(receiver);}catch(Throwable ignored){}finally{registered=false;}
     tracking=false;super.onDetachedFromWindow();
   }
   @Override protected void onWindowVisibilityChanged(int visibility){super.onWindowVisibilityChanged(visibility);if(visibility==VISIBLE&&bar!=null&&!tracking)sync();}
 }
}
