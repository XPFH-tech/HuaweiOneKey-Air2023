package dev.air2023.onekeybeta4.hook;

import java.util.Map;
import java.util.HashMap;
import de.robv.android.xposed.XposedBridge;

/** Single read-only, module-private XSharedPreferences snapshot; no old flags or root fallback. */
public final class HookFlags {
    public static final String K_ADV_CELLS="cells", K_ADV_DEDUPE="dedupe", K_ADV_DRAWER="drawer",
        K_ADV_DOCK="dock", K_ADV_PORTRAIT="portrait", K_ADV_NOTEXT="notext",
        K_ADV_CARD_FOLDER="excluded_card", K_ADV_FORM_TITLE="form_title",
        K_BADGE="excluded_badge", K_APP_DETAILS="app_details", K_MEMINFO="excluded_meminfo";
    private static Map<?,?> data = new HashMap<Object,Object>();
    public static boolean load() {
        try {
            Class<?> c=Class.forName("de.robv.android.xposed.XSharedPreferences");
            Object x=c.getConstructor(String.class,String.class).newInstance("dev.air2023.onekeybeta4","air2023_b04");
            c.getMethod("reload").invoke(x);
            Object all=c.getMethod("getAll").invoke(x);
            if (!(all instanceof Map)) throw new IllegalStateException("not a map");
            Map<?,?> m=(Map<?,?>)all;
            if (!Integer.valueOf(4).equals(m.get("schema"))) throw new IllegalStateException("configuration not readable or not initialized");
            data=new HashMap<Object,Object>(m);
            boolean safe=Boolean.TRUE.equals(data.get("safe"));
            XposedBridge.log("[Air2023B04] CONFIG_OK; safe="+safe+"; pid="+android.os.Process.myPid());
            return !safe;
        } catch(Throwable t) {
            data=new HashMap<Object,Object>();
            try { XposedBridge.log("[Air2023B04] CONFIG_READ_FAILED: "+t); } catch(Throwable ignored){}
            return false;
        }
    }
    public static boolean enabled(String key,boolean ignoredDefault) {
        return !key.startsWith("excluded_") && Boolean.TRUE.equals(data.get(key)) && !Boolean.TRUE.equals(data.get("safe"));
    }
    public static boolean enabled(String key) { return enabled(key,false); }
    public static int radius() {
        Object v=data.get("radius");
        return v instanceof Integer?Math.max(1,Math.min(100,(Integer)v)):46;
    }
}
