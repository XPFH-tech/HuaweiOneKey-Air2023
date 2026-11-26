package dev.air2023.clonec3;
import android.os.Build;
import android.util.Log;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/** Single package experiment, no method replacement, no permission or user ID changes. */
public final class Entry implements IXposedHookLoadPackage {
    private static final String TAG="Air2023CloneC3";
    private static final String TEST="com.coolapk.market";
    private static final Set<String> STOCK=new HashSet<String>(Arrays.asList(
        "com.tencent.mm","com.tencent.mobileqq","com.facebook.katana","com.whatsapp",
        "jp.naver.line.android","com.snapchat.android","com.facebook.orca"));
    private static final AtomicBoolean installed=new AtomicBoolean();
    private static void report(String s){
        try{XposedBridge.log("["+TAG+"] "+s);}catch(Throwable ignored){}
        try{Log.i(TAG,s);}catch(Throwable ignored){}
    }
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam p){
        if(p==null||!"android".equals(p.packageName)||!"android".equals(p.processName))return;
        if(!"DBY2-W00".equals(Build.MODEL)||!"DBY2-W00 4.2.0.252(C00E100R3P5)".equals(Build.DISPLAY)||Build.VERSION.SDK_INT!=31){
            report("C3_SKIP_BUILD");return;
        }
        if(!installed.compareAndSet(false,true))return;
        try{
            Class<?> cls=Class.forName("com.android.server.pm.HwPackageManagerService",false,p.classLoader);
            final Field list=cls.getDeclaredField("SUPPORT_CLONE_APPS");
            final Field instance=cls.getDeclaredField("sHwPackageManagerService");
            final Method init=cls.getDeclaredMethod("initCloneAppsFromCust");
            if(list.getType()!=Set.class||!Modifier.isStatic(list.getModifiers())||
               instance.getType()!=cls||!Modifier.isStatic(instance.getModifiers())||
               init.getReturnType()!=void.class||!Modifier.isStatic(init.getModifiers())){
                report("C3_SKIP_SIGNATURE");return;
            }
            list.setAccessible(true);instance.setAccessible(true);
            XposedBridge.hookMethod(init,new XC_MethodHook(){
                @Override protected void afterHookedMethod(MethodHookParam param){
                    if(param.getThrowable()!=null){report("C3_SKIP_ORIGINAL_ERROR");return;}
                    try{
                        // getInstance calls init before constructing/publishing the PMS singleton.
                        // Never change a live, published package manager's HashSet.
                        if(instance.get(null)!=null){report("C3_SKIP_LATE_INIT");return;}
                        Object raw=list.get(null);
                        if(raw==null||raw.getClass()!=HashSet.class){report("C3_SKIP_SET_TYPE");return;}
                        @SuppressWarnings("unchecked") Set<String> actual=(Set<String>)raw;
                        if(!actual.equals(STOCK)){report("C3_SKIP_STOCK_MISMATCH size="+actual.size());return;}
                        actual.add(TEST);
                        if(actual.size()==STOCK.size()+1&&actual.containsAll(STOCK)&&actual.contains(TEST)){
                            report("C3_ALLOWLIST_OK package="+TEST+" size="+actual.size());
                        }else{
                            actual.remove(TEST);report("C3_VERIFY_FAILED");
                        }
                    }catch(Throwable t){report("C3_CALLBACK_ERROR "+t.getClass().getName());}
                }
            });
            report("C3_HOOK_READY initCloneAppsFromCust");
        }catch(Throwable t){report("C3_INSTALL_ERROR "+t);}
    }
}
