#!/usr/bin/env python3
"""Assemble a narrow, isolated Air2023 beta from pinned upstream resources and reviewed Java."""
from pathlib import Path
import shutil, subprocess, sys, xml.etree.ElementTree as ET
PIN = "8e6acf3b2b56a3cbadde9d6c8f7265e4254669a9"
repo = Path(__file__).resolve().parents[1]
up = Path(sys.argv[1]).resolve()
assert subprocess.check_output(["git","-C",str(up),"rev-parse","HEAD"], text=True).strip() == PIN
app = up / "lsp-app/app"
java = app / "java"
old = java / "com/huawei/onekey/optimizer/hook/impl"
keep = {n: (old / (n+".java")).read_text(encoding="utf-8") for n in ("DoubleTapLockHooks","StatusBarSecondsHooks")}
shutil.rmtree(java)
dest = java / "dev/air2023/onekeybeta"
dest.mkdir(parents=True)
for name, code in keep.items():
    code = code.replace("package com.huawei.onekey.optimizer.hook.impl;", "package dev.air2023.onekeybeta;")
    (dest / (name+".java")).write_text(code, encoding="utf-8")
entry = (repo / "stage2/src/Entry.java").read_text(encoding="utf-8")
assert "R4_APPLIED:" in entry and "values.length!=6 && values.length!=7" in entry
entry = entry.replace("package dev.air2023.onekeypreview;", "package dev.air2023.onekeybeta;").replace("Air2023Preview", "Air2023Beta").replace("R4_ENTRY_CONSTRUCTED", "B03_ENTRY_CONSTRUCTED").replace("R4_LOAD_CALLBACK", "B03_LOAD_CALLBACK").replace("R4_APPLIED", "B03_TIMEOUT_APPLIED")
needle = '        if (p == null || !"com.android.settings".equals(p.packageName)) return;'
assert needle in entry
entry = entry.replace(needle, '''        if (p == null) return;
        if (!"com.android.settings".equals(p.packageName)) {
            if (!target() || !p.packageName.equals(p.processName)) return;
            if (!"com.huawei.android.launcher".equals(p.packageName)
                && !"com.android.systemui".equals(p.packageName)) return;
            if (!installed.compareAndSet(false,true)) return;
            try {
                if ("com.huawei.android.launcher".equals(p.packageName)) {
                    report("B03_OPTIONAL: launcher double tap requested by scope");
                    DoubleTapLockHooks.install(p);
                } else {
                    report("B03_OPTIONAL: SystemUI seconds requested by scope");
                    StatusBarSecondsHooks.install(p);
                }
            } catch(Throwable t) { report("B03_OPTIONAL_ERROR: "+t); }
            return;
        }''')
entry = entry.replace("Settings-only preview. No theme, system_server, launcher or root operations.", "Air2023 beta: three isolated scopes. No theme, system_server or root operations.")
(dest / "Entry.java").write_text(entry, encoding="utf-8")
shutil.copyfile(repo / "beta/MainActivity.java", dest / "MainActivity.java")
# Reuse original Material layout: retain status and three feature rows, remove all legacy controls.
A = "http://schemas.android.com/apk/res/android"
P = "http://schemas.android.com/apk/res-auto"
ET.register_namespace("android", A); ET.register_namespace("app", P)
def attr(k): return "{"+A+"}"+k
def has(node, id): return any(x.get(attr("id")) == "@+id/"+id for x in node.iter())
layout = app / "res/layout/activity_main.xml"
doc = ET.parse(layout)
content = doc.getroot().find("ScrollView/LinearLayout")
assert content is not None
children = list(content)
groups = [("tv_root", {"tv_root","tv_xposed","tv_module"}), ("sw_doubletap", {"sw_doubletap"}), ("sw_timeout", {"sw_timeout","sw_seconds"})]
keep_nodes = []
for marker, ids in groups:
    matches = [(i,x) for i,x in enumerate(children) if x.tag.endswith("MaterialCardView") and has(x,marker)]
    assert len(matches)==1, marker
    i,card=matches[0]
    assert i>0 and children[i-1].tag=="TextView"
    keep_nodes += [children[i-1], card]
    inner = card.find("LinearLayout")
    assert inner is not None
    for row in list(inner):
        if not any(has(row, id) for id in ids): inner.remove(row)
        else:
            for child in list(row):
                if child.get(attr("id"), "").startswith("@+id/sw_"): row.remove(child)
for child in children: content.remove(child)
for child in keep_nodes: content.append(child)
note=ET.SubElement(content,"TextView")
for k,v in {"id":"@+id/beta_note","layout_width":"match_parent","layout_height":"wrap_content","layout_margin":"20dp","textSize":"14sp","textColor":"?attr/colorOnSurfaceVariant"}.items(): note.set(attr(k),v)
doc.write(layout, encoding="utf-8", xml_declaration=True)
strings=app/"res/values/strings.xml"
doc=ET.parse(strings)
updates={
    "app_name":"华为一键优化 Air2023 Beta 0.3",
    "title":"华为一键优化 · Air2023 Beta 0.3",
    "sw_doubletap_desc":"实验功能：在 Vector 单独启用桌面作用域后测试双击锁屏；可能影响手势，默认不推荐勾选。",
    "sw_seconds_desc":"实验功能：在 Vector 单独启用 SystemUI 作用域后，长按状态栏时钟测试秒数；默认不推荐勾选。",
    "sw_timeout_desc":"已验证显示：原厂六档扩展为九档。启用设置作用域并重新加载后生效。"
}
for node in doc.getroot():
    if node.get("name") in updates: node.text=updates[node.get("name")]
doc.write(strings, encoding="utf-8", xml_declaration=True)
(app/"res/values/arrays.xml").write_text('<resources><string-array name="xposed_scope"><item>com.android.settings</item></string-array></resources>', encoding="utf-8")
(app/"AndroidManifest.xml").write_text('''<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="dev.air2023.onekeybeta" android:versionCode="30" android:versionName="0.3-beta">
<uses-sdk android:minSdkVersion="26" android:targetSdkVersion="33"/>
<application android:allowBackup="false" android:label="@string/app_name" android:icon="@mipmap/ic_launcher" android:theme="@style/AppTheme">
<activity android:name=".MainActivity" android:exported="true"><intent-filter><action android:name="android.intent.action.MAIN"/><category android:name="android.intent.category.LAUNCHER"/></intent-filter></activity>
<meta-data android:name="xposedmodule" android:value="true"/>
<meta-data android:name="xposeddescription" android:value="Air2023: Settings timeout; opt-in launcher and SystemUI experiments"/>
<meta-data android:name="xposedminversion" android:value="82"/>
<meta-data android:name="xposedscope" android:resource="@array/xposed_scope"/>
</application></manifest>''',encoding="utf-8")
assets=app/"assets"
shutil.rmtree(assets)
assets.mkdir()
(assets/"xposed_init").write_text("dev.air2023.onekeybeta.Entry\n", encoding="utf-8")
shutil.copyfile(up/"LICENSE", assets/"UPSTREAM-LICENSE.txt")
# Only the explicit allowlist Java sources can enter the APK. No original root scripts, smali or high-risk hooks.
assert sorted(p.name for p in java.rglob("*.java")) == ["DoubleTapLockHooks.java","Entry.java","MainActivity.java","StatusBarSecondsHooks.java"]
assert not any(x.get(attr("id"), "").startswith("@+id/sw_") for x in ET.parse(layout).getroot().iter())
print("Beta source/layout allowlists verified.")
