package dev.air2023.themeprobe1r1;
import android.util.Log;
import android.os.Process;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
/** Generic entry smoke test only; no target class resolution or method hooks. */
public final class Entry implements IXposedHookLoadPackage {
 private static final String TAG="Air2023ThemeT1R1";
 private static final String SELF="dev.air2023.themeprobe1r1",TARGET="com.huawei.android.thememanager";
 private static void log(String s){
   try{Log.i(TAG,s);}catch(Throwable ignored){}
   try{XposedBridge.log("["+TAG+"] "+s);}catch(Throwable ignored){}
 }
 public Entry(){
   log("T1R1_ENTRY pid="+Process.myPid()+" uid="+Process.myUid());
 }
 @Override public void handleLoadPackage(XC_LoadPackage.LoadPackageParam p){
   if(p==null||(!SELF.equals(p.packageName)&&!TARGET.equals(p.packageName)))return;
   log("T1R1_CALLBACK package="+p.packageName+" process="+p.processName+" pid="+Process.myPid());
   if(SELF.equals(p.packageName))log("T1R1_SELF hooks=0");
   else log("T1R1_TARGET hooks=0");
 }
}
