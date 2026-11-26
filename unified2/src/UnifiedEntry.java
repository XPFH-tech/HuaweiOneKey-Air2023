package dev.air2023.unified2;
import android.os.Build;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
public final class UnifiedEntry implements IXposedHookLoadPackage{
 private static final AtomicBoolean installed=new AtomicBoolean();
 private static void log(String s){try{XposedBridge.log("[Air2023U2] "+s);}catch(Throwable ignored){}}
 @Override public void handleLoadPackage(XC_LoadPackage.LoadPackageParam p){
   if(p==null||p.packageName==null||!p.packageName.equals(p.processName))return;
   String name=p.packageName;
   if(!"com.android.settings".equals(name)&&!"com.huawei.android.launcher".equals(name)&&!"com.android.systemui".equals(name)&&!"android".equals(name))return;
   if(!"DBY2-W00".equals(Build.MODEL)||!"DBY2-W00 4.2.0.252(C00E100R3P5)".equals(Build.DISPLAY)||Build.VERSION.SDK_INT!=31){log("U2_SKIP_BUILD");return;}
   if(!installed.compareAndSet(false,true))return;
   // C4R1 runs before any shared preference reads, including early system_server startup.
   if("android".equals(name)||"com.android.settings".equals(name)){
     try{new dev.air2023.unified2.clone.Entry().handleLoadPackage(p);}catch(Throwable t){log("U2_CLONE_ERROR "+t);}
     if("android".equals(name))return;
   }
   try{
     Class<?> c=Class.forName("de.robv.android.xposed.XSharedPreferences");
     Object x=c.getConstructor(String.class,String.class).newInstance("dev.air2023.unified2","air2023_u1");
     c.getMethod("reload").invoke(x);
     Object all=c.getMethod("getAll").invoke(x);
     if(!(all instanceof Map))throw new IllegalStateException("not map");
     Map<?,?> m=(Map<?,?>)all;
     if(!Integer.valueOf(1).equals(m.get("schema"))||!(m.get("base") instanceof Boolean)||!(m.get("volume") instanceof Boolean)||!(m.get("island") instanceof Boolean))throw new IllegalStateException("configuration unavailable");
     boolean base=Boolean.TRUE.equals(m.get("base")),volume=Boolean.TRUE.equals(m.get("volume")),island=Boolean.TRUE.equals(m.get("island"));
     log("U2_CONFIG_OK process="+name+" base="+base+" volume="+volume+" island="+island);
     if(base)try{new Entry().handleLoadPackage(p);}catch(Throwable t){log("U2_BASE_ERROR "+t.getClass().getName());}
     if("com.android.systemui".equals(name)){
       if(volume)try{new dev.air2023.unified2.volume.Entry().handleLoadPackage(p);}catch(Throwable t){log("U2_VOLUME_ERROR "+t.getClass().getName());}
       if(island)try{new dev.air2023.unified2.island.Entry().handleLoadPackage(p);}catch(Throwable t){log("U2_ISLAND_ERROR "+t.getClass().getName());}
     }
   }catch(Throwable t){log("U2_CONFIG_ERROR "+t);}
 }
}
