package dev.air2023.clonec4;
import android.app.Application;
import android.os.Build;
import android.util.Log;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public final class Entry implements IXposedHookLoadPackage {
 private static final AtomicBoolean installed=new AtomicBoolean();
 private static void report(String s){try{XposedBridge.log("[Air2023CloneC4] "+s);}catch(Throwable ignored){}try{Log.i("Air2023CloneC4",s);}catch(Throwable ignored){}}
 public void handleLoadPackage(XC_LoadPackage.LoadPackageParam p){
   if(p==null||!p.packageName.equals(p.processName))return;
   if(!"android".equals(p.packageName)&&!"com.android.settings".equals(p.packageName))return;
   if(!"DBY2-W00".equals(Build.MODEL)||!"DBY2-W00 4.2.0.252(C00E100R3P5)".equals(Build.DISPLAY)||Build.VERSION.SDK_INT!=31){report("C4_SKIP_BUILD");return;}
   if(!installed.compareAndSet(false,true))return;
   try{if("android".equals(p.packageName))server(p.classLoader);else settings(p.classLoader);}
   catch(Throwable t){report("C4_INSTALL_ERROR "+t);}
 }
 private static void server(ClassLoader loader)throws Exception{
   Class<?> cls=Class.forName("com.android.server.pm.HwPackageManagerService",false,loader);
   final Field list=cls.getDeclaredField("SUPPORT_CLONE_APPS"),instance=cls.getDeclaredField("sHwPackageManagerService");
   Method init=cls.getDeclaredMethod("initCloneAppsFromCust");
   if(list.getType()!=Set.class||!Modifier.isStatic(list.getModifiers())||instance.getType()!=cls||!Modifier.isStatic(instance.getModifiers())||init.getReturnType()!=void.class||!Modifier.isStatic(init.getModifiers()))throw new NoSuchMethodException("C4 signature");
   list.setAccessible(true);instance.setAccessible(true);
   XposedBridge.hookMethod(init,new XC_MethodHook(){
     @Override protected void afterHookedMethod(MethodHookParam mp){
       if(mp.getThrowable()!=null)return;
       try{
         if(instance.get(null)!=null){report("C4_SKIP_LATE_INIT");return;}
         Object raw=list.get(null);
         if(raw==null||raw.getClass()!=HashSet.class){report("C4_SKIP_SET_TYPE");return;}
         @SuppressWarnings("unchecked") Set<String> actual=(Set<String>)raw;
         if(!actual.equals(Policy.STOCK)){report("C4_SKIP_STOCK_MISMATCH size="+actual.size());return;}
         Set<String> extra;
         try{extra=Policy.readSnapshot();}catch(Throwable t){
           // Preserve the previously verified C3 package if staging was not readable.
           extra=new TreeSet<String>();extra.add(Policy.TEST);report("C4_SNAPSHOT_FAILED_C3_ONLY "+t.getClass().getName());}
         extra.add(Policy.TEST);
         actual.addAll(extra);
         if(!actual.containsAll(Policy.STOCK)||!actual.containsAll(extra)||actual.size()!=Policy.STOCK.size()+extra.size()){
           actual.removeAll(extra);report("C4_VERIFY_FAILED");return;
         }
         report("C4_ALLOWLIST_OK size="+actual.size()+" added="+extra.size());
       }catch(Throwable t){report("C4_CALLBACK_ERROR "+t);}
     }
   });
   report("C4_SERVER_HOOK_READY");
 }
 private static void settings(ClassLoader loader)throws Exception{
   Class<?> alias=Class.forName("com.android.settingslib.l",false,loader);
   Class<?> hw=Class.forName("com.huawei.settingslib.HwUtils",false,loader);
   Method method=alias.getDeclaredMethod("getXmlFromCust"),original=hw.getDeclaredMethod("getXmlFromCust");
   if(method.getReturnType()!=Map.class||original.getReturnType()!=Map.class||!Modifier.isStatic(method.getModifiers())||!Modifier.isStatic(original.getModifiers()))throw new NoSuchMethodException("C4 settings signature");
   Class.forName("com.android.settings.applications.AppCloneSettings",false,loader);
   XposedBridge.hookMethod(method,new XC_MethodHook(){
     @Override protected void afterHookedMethod(MethodHookParam mp){
       if(mp.getThrowable()!=null||!(mp.getResult() instanceof Map))return;
       try{
         boolean clone=false;
         for(StackTraceElement f:Thread.currentThread().getStackTrace())if(f.getClassName().startsWith("com.android.settings.applications.AppCloneSettings")||f.getClassName().equals("com.android.settings.applications.I")){clone=true;break;}
         if(!clone||android.os.Process.myUid()/100000!=0)return;
         Map<?,?> base=(Map<?,?>)mp.getResult();
         if(!base.keySet().containsAll(Policy.STOCK)){report("C4_SETTINGS_BASELINE_SKIP");return;}
         Map<String,String> merged=new HashMap<String,String>();
         for(Map.Entry<?,?> e:base.entrySet()){
           if(!(e.getKey() instanceof String)||!(e.getValue() instanceof String))return;
           merged.put((String)e.getKey(),(String)e.getValue());
         }
         Application app=(Application)Class.forName("android.app.ActivityThread").getMethod("currentApplication").invoke(null);
         if(app==null||!"com.android.settings".equals(app.getPackageName()))return;
         Set<String> approved;
         try{approved=Policy.readSnapshot();}catch(Throwable t){
           // Settings can be denied direct access to /data/system by SELinux.
           // Retain C2's candidate discovery as UI fallback, not evidence of server support.
           approved=Policy.candidates(app);report("C4_SETTINGS_UI_FALLBACK");
         }
         approved.add(Policy.TEST);
         Set<String> installed=Policy.candidates(app);int n=0;
         for(String pkg:approved)if(installed.contains(pkg)&&!merged.containsKey(pkg)){merged.put(pkg,"0");n++;}
         mp.setResult(merged);report("C4_SETTINGS_OK added="+n);
       }catch(Throwable t){report("C4_SETTINGS_ERROR "+t);}
     }
   });
   report("C4_SETTINGS_HOOK_READY");
 }
}
