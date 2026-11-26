#!/usr/bin/env python3
"""Prepare isolated 0.4 from previously tested 0.3 and pinned upstream Java."""
from pathlib import Path
import sys, subprocess, shutil, xml.etree.ElementTree as ET
repo=Path(__file__).resolve().parents[1]
up=Path(sys.argv[1]).resolve()
subprocess.run([sys.executable,str(repo/"beta/prepare.py"),str(up)],check=True)
app=up/"lsp-app/app"
java=app/"java"
old=java/"dev/air2023/onekeybeta"
dst=java/"dev/air2023/onekeybeta4"
old.rename(dst)
pkg="dev.air2023.onekeybeta4"
for f in dst.glob("*.java"):
    f.write_text(f.read_text().replace("dev.air2023.onekeybeta",pkg),encoding="utf-8")
def source(rel):
    path="lsp-app/app/java/com/huawei/onekey/optimizer/"+rel
    return subprocess.check_output(["git","-C",str(up),"show","HEAD:"+path],text=True).replace("com.huawei.onekey.optimizer",pkg)
selected={
    "launcher":["Kit","LCfg","HookLayoutCells","HookDockCount","HookHotseatPortrait","HookNoText","HookAppDetails","HookDrawer","HookFormTitle"],
    "blur":["LauncherBlurController","LauncherBlurHooks"]
}
for folder,names in selected.items():
    out=dst/"hook"/folder
    out.mkdir(parents=True)
    for n in names:
        code=source("hook/"+folder+"/"+n+".java")
        # Remove file logger and broad hidden-API exemption from execution paths:
        # only the individual install methods below are called, never upstream LauncherAdvanced.
        (out/(n+".java")).write_text(code,encoding="utf-8")
for f in (repo/"beta4").glob("*.java"):
    shutil.copyfile(f,dst/f.name)
shutil.move(dst/"HookFlags.java",dst/"hook/HookFlags.java")
shutil.move(dst/"BlurCfg.java",dst/"hook/blur/BlurCfg.java")
entry=dst/"Entry.java"
s=entry.read_text().replace("Air2023Beta","Air2023B04").replace("B03_","B04_")
needle='if (!installed.compareAndSet(false,true)) return;'
assert s.count(needle)==2
s=s.replace(needle,needle+'\n            if (!dev.air2023.onekeybeta4.hook.HookFlags.load()) { report("B04_CONFIG_UNAVAILABLE_OR_SAFE: no hooks installed"); return; }')
s=s.replace('DoubleTapLockHooks.install(p);','if (dev.air2023.onekeybeta4.hook.HookFlags.enabled("doubletap",false)) DoubleTapLockHooks.install(p);\n                    LauncherFeatures.install(p.classLoader);')
s=s.replace('StatusBarSecondsHooks.install(p);','if (dev.air2023.onekeybeta4.hook.HookFlags.enabled("seconds",false)) StatusBarSecondsHooks.install(p);')
s=s.replace('Class<?> c = Class.forName("com.android.settings.display.HwTimeoutPreferenceController"','if (!dev.air2023.onekeybeta4.hook.HookFlags.enabled("timeout",false)) { report("B04_TIMEOUT_DISABLED"); return; }\n            Class<?> c = Class.forName("com.android.settings.display.HwTimeoutPreferenceController"')
entry.write_text(s,encoding="utf-8")
# Keep original Material resources, provide an independent settings screen in Java.
strings=app/"res/values/strings.xml"
s=strings.read_text().replace("Air2023 Beta 0.3","Air2023 Beta 0.4")
strings.write_text(s,encoding="utf-8")
manifest=app/"AndroidManifest.xml"
s=manifest.read_text().replace("dev.air2023.onekeybeta",pkg).replace('versionCode="30"','versionCode="40"').replace('versionName="0.3-beta"','versionName="0.4-beta"').replace('android:value="82"','android:value="82"/>\n<meta-data android:name="xposedsharedprefs" android:value="true"')
manifest.write_text(s,encoding="utf-8")
(app/"assets/xposed_init").write_text(pkg+".Entry\n",encoding="utf-8")
ET.parse(manifest)
assert len(list(java.rglob("*.java")))==17, [p.name for p in java.rglob("*.java")]
assert not any(p.name in {"ThemeHooks.java","MxxHooks.java","RootShell.java","HookBadge.java","HookRoProp.java"} for p in java.rglob("*.java"))
print("B04 allowlist and package verified")
