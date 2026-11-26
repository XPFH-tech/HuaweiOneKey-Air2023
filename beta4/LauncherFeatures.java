package dev.air2023.onekeybeta4;
import dev.air2023.onekeybeta4.hook.HookFlags;
import dev.air2023.onekeybeta4.hook.launcher.*;
import dev.air2023.onekeybeta4.hook.blur.LauncherBlurHooks;
import android.os.Build;
import de.robv.android.xposed.XposedBridge;

final class LauncherFeatures {
    private interface Step { void run(); }
    private static void step(String key, boolean on, Step s) {
        if (!on) return;
        try { XposedBridge.log("[Air2023B04] INSTALL "+key); s.run(); }
        catch(Throwable t) { XposedBridge.log("[Air2023B04] FEATURE_ERROR "+key+": "+t); }
    }
    static void install(final ClassLoader cl) {
        final LCfg c=LCfg.load();
        step("layout",c.cells||c.dedupe,()->HookLayoutCells.install(cl,c));
        step("drawer",c.drawer,()->HookDrawer.install(cl));
        step("dock",c.dock,()->HookDockCount.install(cl));
        step("portrait",c.portrait,()->HookHotseatPortrait.install(cl));
        step("notext",c.notext,()->HookNoText.install(cl));
        step("app_details",c.app_details,()->HookAppDetails.install(cl));
        step("form_title",c.form_title,()->HookFormTitle.install(cl));
        if (HookFlags.enabled("blur",false)&&Build.VERSION.SDK_INT<31) {
            XposedBridge.log("[Air2023B04] BLUR_SKIP_API: "+Build.VERSION.SDK_INT);
        } else step("blur",HookFlags.enabled("blur",false),()->LauncherBlurHooks.install(cl));
    }
}
