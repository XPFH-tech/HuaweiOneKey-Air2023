package dev.air2023.clonec2;
import android.app.Application;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.util.Log;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public final class Entry implements IXposedHookLoadPackage {
 private static final AtomicBoolean installed=new AtomicBoolean();
 private static final String TAG="Air2023CloneC2";
 private static void report(String s){try{XposedBridge.log("["+TAG+"] "+s);}catch(Throwable ignored){}try{Log.i(TAG,s);}catch(Throwable ignored){}}
 public Entry(){report("C2_ENTRY_CONSTRUCTED");}
 public void handleLoadPackage(XC_LoadPackage.LoadPackageParam p){
   if(p==null||!"com.android.settings".equals(p.packageName)||!"com.android.settings".equals(p.processName))return;
   if(!"DBY2-W00".equals(Build.MODEL)||!"DBY2-W00 4.2.0.252(C00E100R3P5)".equals(Build.DISPLAY)){report("C2_SKIP_BUILD "+Build.DISPLAY);return;}
   if(!installed.compareAndSet(false,true))return;
   try{
     Class<?> alias=Class.forName("com.android.settingslib.l",false,p.classLoader);
     Class<?> hw=Class.forName("com.huawei.settingslib.HwUtils",false,p.classLoader);
     Method method=alias.getDeclaredMethod("getXmlFromCust");
     Method original=hw.getDeclaredMethod("getXmlFromCust");
     if(method.getReturnType()!=Map.class||original.getReturnType()!=Map.class||!Modifier.isStatic(method.getModifiers())||!Modifier.isStatic(original.getModifiers()))throw new NoSuchMethodException("Unexpected clone XML signatures");
     Class.forName("com.android.settings.applications.AppCloneSettings",false,p.classLoader);
     XposedBridge.hookMethod(method,new XC_MethodHook(){
       @Override protected void afterHookedMethod(MethodHookParam param){
         if(param.getThrowable()!=null||!(param.getResult() instanceof Map))return;
         try{
           Map<?,?> base=(Map<?,?>)param.getResult();
           if(base.isEmpty()||!base.containsKey("com.tencent.mm")||!base.containsKey("com.tencent.mobileqq")){report("C2_SKIP_BASELINE");return;}
           Map<String,String> merged=new HashMap<String,String>();
           for(Map.Entry<?,?> e:base.entrySet()){
             if(!(e.getKey() instanceof String)||!(e.getValue() instanceof String)){report("C2_SKIP_MAP_TYPE");return;}
             merged.put((String)e.getKey(),(String)e.getValue());
           }
           // This alias is also used by other Settings pages. Restrict to the native clone call path.
           boolean clone=false;
           for(StackTraceElement frame:Thread.currentThread().getStackTrace()){
             String n=frame.getClassName();
             if(n.startsWith("com.android.settings.applications.AppCloneSettings")||n.equals("com.android.settings.applications.I")){clone=true;break;}
           }
           if(!clone)return;
           if(android.os.Process.myUid()/100000!=0)return;
           Application app=(Application)Class.forName("android.app.ActivityThread").getMethod("currentApplication").invoke(null);
           if(app==null||!"com.android.settings".equals(app.getPackageName())){report("C2_NO_CONTEXT");return;}
           Intent launcher=new Intent(Intent.ACTION_MAIN);launcher.addCategory(Intent.CATEGORY_LAUNCHER);
           List<ResolveInfo> activities=app.getPackageManager().queryIntentActivities(launcher,0);
           int added=0;
           for(ResolveInfo r:activities){
             if(r==null||r.activityInfo==null||r.activityInfo.applicationInfo==null)continue;
             ApplicationInfo ai=r.activityInfo.applicationInfo;
             String name=ai.packageName;
             if(name==null||!name.matches("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)+")||!ai.enabled||!r.activityInfo.enabled)continue;
             if((ai.flags&(ApplicationInfo.FLAG_SYSTEM|ApplicationInfo.FLAG_UPDATED_SYSTEM_APP))!=0||name.startsWith("dev.air2023.")||name.equals("com.topjohnwu.magisk"))continue;
             if(!merged.containsKey(name)){merged.put(name,"0");added++;}
           }
           if(added>0)param.setResult(merged);
           report("C2_MAP_OK original="+base.size()+" added="+added);
         }catch(Throwable t){report("C2_MAP_ERROR "+t.getClass().getName()+": "+t.getMessage());}
       }
     });
     report("C2_HOOK_READY "+method.toString());
   }catch(Throwable t){report("C2_INSTALL_ERROR "+t);}
 }
}
