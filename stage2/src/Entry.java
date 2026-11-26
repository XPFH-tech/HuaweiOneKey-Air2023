package dev.air2023.onekeypreview;

import android.os.Build;
import android.util.Log;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/** Settings-only preview. No theme, system_server, launcher or root operations. */
public final class Entry implements IXposedHookLoadPackage {
    private static final String TAG = "Air2023Preview";
    private static final AtomicBoolean installed = new AtomicBoolean();
    private static final String[] BASE = {"15000","30000","60000","120000","300000","600000"};
    private static final String[] EXTRA = {"1200000","1800000","3600000"};
    private static final java.util.Set<String> reported =
        java.util.Collections.synchronizedSet(new java.util.HashSet<String>());
    private static void report(String message) {
        if (!reported.add(message)) return;
        Log.i(TAG,message);
        try { XposedBridge.log("["+TAG+"] "+message); } catch(Throwable ignored) {}
    }
    static boolean target() {
        String build = Build.DISPLAY + " " + Build.VERSION.INCREMENTAL;
        return "DBY2-W00".equalsIgnoreCase(Build.MODEL)
            && build.matches("(?s).*4\\.2\\.0\\.252(?![0-9]).*");
    }
    @Override public void handleLoadPackage(XC_LoadPackage.LoadPackageParam p) {
        if (p == null || !"com.android.settings".equals(p.packageName)) return;
        report("Settings callback: process="+p.processName+"; model="+Build.MODEL
            +"; display="+Build.DISPLAY+"; incremental="+Build.VERSION.INCREMENTAL);
        if (!"com.android.settings".equals(p.processName)) {
            report("SKIP_PROCESS: not the Settings main process"); return;
        }
        if (!target()) { report("SKIP_BUILD: model or build string did not match"); return; }
        if (!installed.compareAndSet(false,true)) return;
        try {
            Class<?> c = Class.forName("com.android.settings.display.HwTimeoutPreferenceController",false,p.classLoader);
            Class<?> pref = Class.forName("androidx.preference.ListPreference",false,p.classLoader);
            Field field = c.getDeclaredField("mScreenTimeoutPreference");
            field.setAccessible(true);
            Method method;
            try { method = c.getDeclaredMethod("setScreenTimeoutEntriesAndArrow"); }
            catch(NoSuchMethodException missing) {
                for (Method candidate : c.getDeclaredMethods()) {
                    if ("setScreenTimeoutEntriesAndArrow".equals(candidate.getName()))
                        report("CANDIDATE_METHOD: "+candidate.toGenericString());
                }
                throw missing;
            }
            if (method.getReturnType() != void.class) throw new NoSuchMethodException("Unexpected return type");
            report("MATCH_METHOD: "+method.toGenericString());
            Method ge = pref.getMethod("getEntries"), gv = pref.getMethod("getEntryValues");
            Method se = pref.getMethod("setEntries",CharSequence[].class);
            Method sv = pref.getMethod("setEntryValues",CharSequence[].class);
            XposedBridge.hookMethod(method,new XC_MethodHook(){
                @Override protected void afterHookedMethod(MethodHookParam mp) {
                    report("CALLBACK: original Settings method returned");
                    if (mp.getThrowable()!=null) { report("SKIP_ORIGINAL_EXCEPTION: "+mp.getThrowable().getClass().getName()); return; }
                    try {
                        Object o = field.get(mp.thisObject);
                        if (!pref.isInstance(o)) { report("SKIP_PREFERENCE: "+(o==null?"null":o.getClass().getName())); return; }
                        CharSequence[] entries = (CharSequence[])ge.invoke(o), values = (CharSequence[])gv.invoke(o);
                        report("STOCK_VALUES: "+Arrays.toString(values)+"; entries="+(entries==null?-1:entries.length));
                        if (entries==null || values==null || entries.length!=7 || values.length!=7) {
                            report("SKIP_COUNT: expected 7 stock entries"); return;
                        }
                        for (int i=0;i<BASE.length;i++) if (values[i]==null || !BASE[i].contentEquals(values[i])) {
                            report("SKIP_VALUE: index="+i+" expected="+BASE[i]+" actual="+values[i]); return;
                        }
                        if (values[6]==null || !Arrays.asList("-1","0","2147483647").contains(values[6].toString())) {
                            report("SKIP_FINAL_VALUE: "+values[6]); return;
                        }
                        if (entries[5]==null || !entries[5].toString().contains("10")) {
                            report("SKIP_LABEL: expected stock ten-minute label"); return;
                        }
                        CharSequence[] e = new CharSequence[10], v = new CharSequence[10];
                        System.arraycopy(entries,0,e,0,6); System.arraycopy(values,0,v,0,6);
                        int[] minutes={20,30,60};
                        for(int i=0;i<3;i++) {
                            e[6+i]=entries[5].toString().replace("10",Integer.toString(minutes[i]));
                            v[6+i]=EXTRA[i];
                        }
                        e[9]=entries[6]; v[9]=values[6];
                        try { se.invoke(o,(Object)e); sv.invoke(o,(Object)v); }
                        catch(Throwable t) {
                            try { se.invoke(o,(Object)entries); sv.invoke(o,(Object)values); } catch(Throwable restore) { Log.w(TAG,"Restore failed",restore); }
                            throw t;
                        }
                        report("APPLIED: preview timeout entries installed");
                    } catch(Throwable t) { report("CALLBACK_ERROR: "+t.getClass().getName()+": "+t.getMessage()); Log.w(TAG,"Timeout UI unchanged or restored",t); }
                }
            });
            report("HOOK_INSTALLED: Settings-only preview");
        } catch(Throwable t) { report("HOOK_ERROR: "+t.getClass().getName()+": "+t.getMessage()); Log.w(TAG,"Target method unsupported; skipped",t); }
    }
}
