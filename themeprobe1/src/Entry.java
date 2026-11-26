package dev.air2023.themeprobe1;
import android.os.Build;
import android.util.Log;
import java.util.concurrent.atomic.AtomicBoolean;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/** Only reads class presence. Never installs a method hook, initializes target classes,
 * changes a return value, writes files, or enters system_server. */
public final class Entry implements IXposedHookLoadPackage {
 private static final String PKG="com.huawei.android.thememanager";
 private static final String TAG="Air2023ThemeT1";
 private static final AtomicBoolean once=new AtomicBoolean();
 private static void log(String msg){try{XposedBridge.log("["+TAG+"] "+msg);}catch(Throwable ignored){}try{Log.i(TAG,msg);}catch(Throwable ignored){}}
 private static String presence(ClassLoader cl,String name){
   try{Class.forName(name,false,cl);return "PRESENT";}
   catch(ClassNotFoundException e){return "NOT_FOUND";}
   catch(Throwable t){return "UNAVAILABLE_"+t.getClass().getSimpleName();}
 }
 @Override public void handleLoadPackage(XC_LoadPackage.LoadPackageParam p){
   if(p==null||!PKG.equals(p.packageName)||!PKG.equals(p.processName))return;
   if(!"DBY2-W00".equals(Build.MODEL)||!"DBY2-W00 4.2.0.252(C00E100R3P5)".equals(Build.DISPLAY)||Build.VERSION.SDK_INT!=31){log("T1_SKIP_BUILD");return;}
   if(!once.compareAndSet(false,true))return;
   log("T1_START process="+p.processName+" sdk="+Build.VERSION.SDK_INT);
   log("T1_FRAMEWORK_CLASS="+presence(p.classLoader,"android.hwtheme.HwThemeManager"));
   log("T1_LOCAL_PREVIEW="+presence(p.classLoader,"com.huawei.android.thememanager.mvp.view.activity.onlinetheme.LocalThemePreviewActivity"));
   log("T1_ONLINE_PREVIEW="+presence(p.classLoader,"com.huawei.android.thememanager.mvp.view.activity.onlinetheme.OnlineThemePreviewActivity"));
   log("T1_DONE read_only=true hooks=0");
 }
}
