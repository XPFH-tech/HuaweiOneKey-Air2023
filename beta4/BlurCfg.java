package dev.air2023.onekeybeta4.hook.blur;
import dev.air2023.onekeybeta4.hook.HookFlags;
public final class BlurCfg {
    public static final int DEF_RADIUS=46, MAX_RADIUS=100;
    public static boolean enabled(){return HookFlags.enabled("blur",false);}
    public static int radius(){return HookFlags.radius();}
}
