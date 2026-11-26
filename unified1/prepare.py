#!/usr/bin/env python3
"""Integrate known isolated module implementations, excluding clone system_server in U1."""
from pathlib import Path
import subprocess, sys, shutil, xml.etree.ElementTree as ET
repo=Path(__file__).resolve().parents[1]
up=Path(sys.argv[1]).resolve()
subprocess.run([sys.executable,str(repo/"beta4/prepare.py"),str(up)],check=True)
app=up/"lsp-app/app"
java=app/"java"
old=java/"dev/air2023/onekeybeta4"
pkg="dev.air2023.unified1"
dst=java/"dev/air2023/unified1"
old.rename(dst)
for f in dst.rglob("*.java"):
    f.write_text(f.read_text(encoding="utf-8").replace("dev.air2023.onekeybeta4",pkg),encoding="utf-8")
main=dst/"MainActivity.java"
s=main.read_text(encoding="utf-8").replace("class MainActivity","class BaseActivity")
s=s.replace("v0.4 Beta · 独立开关 · 无 Root 命令","U1 · 基础优化 · v0.4 内核")
s=s.replace("先关闭 v0.3 和原版的重叠作用域。新版在 Vector 勾选设置、华为桌面、系统用户界面。","在 U1 首页开启基础优化，停用旧 v0.4 的重复作用域。")
s=s.replace("尚未移植：控制中心音量条、灵动岛、平行视界、分身、多小窗。保留 v0.3 作为回退。","控制中心音量条、灵动岛和多窗工具请回 U1 首页管理。C4R1 继续独立运行。")
(dst/"BaseActivity.java").write_text(s,encoding="utf-8")
main.unlink()

def copy_sources(folder,sub,oldpkg,names):
    target=dst/sub;target.mkdir()
    for name in names:
        s=(repo/folder/"src"/name).read_text(encoding="utf-8")
        s=s.replace("package "+oldpkg+";","package "+pkg+"."+sub+";")
        if sub=="island":
            s=s.replace('"'+oldpkg+'"','"'+pkg+'"')
            if name=="MainActivity.java":
                s=s.replace("Air2023 灵动岛 I4","U1 · 灵动岛 I4").replace("先打开本页初始化配置，再关闭 I1/I2/I3、启用 I4（仅系统用户界面）。保留其他模块。","在 U1 首页开启灵动岛，关闭旧 I1/I2/I3/I4，U1 勾选系统用户界面。")
        if sub=="window" and name=="MainActivity.java":
            s=s.replace("Air2023 多窗上限 D3","U1 · 多窗上限 D3").replace("四窗已真机验证。","四窗已真机验证。U1 不继承旧 D3 私有撤销记录，请先重新检查当前值，保留旧 D3 作回退。")
        (target/name).write_text(s,encoding="utf-8")
copy_sources("volumev1","volume","dev.air2023.volumev1",["Entry.java"])
copy_sources("islandi4","island","dev.air2023.islandi4",["Entry.java","Config.java","MainActivity.java"])
copy_sources("windowlab3","window","dev.air2023.windowlab3",["MainActivity.java"])
for f in (repo/"unified1/src").glob("*.java"):
    shutil.copyfile(f,dst/f.name)
manifest=app/"AndroidManifest.xml"
s=manifest.read_text(encoding="utf-8").replace("dev.air2023.onekeybeta4",pkg).replace('versionCode="40"','versionCode="100"').replace('versionName="0.4-beta"','versionName="1.0-U1"')
s=s.replace("<application ", '<queries><package android:name="dev.air2023.clonec4r1"/></queries>\n<application ')
s=s.replace('<meta-data android:name="xposedmodule"', '<activity android:name=".BaseActivity" android:exported="false"/>\n<activity android:name=".island.MainActivity" android:exported="false"/>\n<activity android:name=".window.MainActivity" android:exported="false"/>\n<meta-data android:name="xposedmodule"')
s=s.replace("Air2023: Settings timeout; opt-in launcher and SystemUI experiments","Air2023 U1: Settings, launcher, SystemUI; no system_server hooks")
manifest.write_text(s,encoding="utf-8");ET.parse(manifest)
(app/"assets/xposed_init").write_text(pkg+".UnifiedEntry\n",encoding="utf-8")
(app/"res/values/arrays.xml").write_text('<resources><string-array name="xposed_scope"><item>com.android.settings</item><item>com.huawei.android.launcher</item><item>com.android.systemui</item></string-array></resources>',encoding="utf-8")
strings=app/"res/values/strings.xml"
s=strings.read_text(encoding="utf-8").replace("华为一键优化 Air2023 Beta 0.4","华为一键优化 Air2023 U1").replace("华为一键优化 · Air2023 Beta 0.4","华为一键优化 · Air2023 U1")
strings.write_text(s,encoding="utf-8")
assert len(list(java.rglob("*.java")))==25,[p.name for p in java.rglob("*.java")]
assert not any("clone" in p.relative_to(dst).parts for p in dst.rglob("*.java"))
assert (dst/"island/Config.java").read_text().count('"'+pkg+'"')==1
assert "dev.air2023.onekeybeta4" not in "".join(f.read_text() for f in dst.rglob("*.java"))
print("U1 source allowlist: 25 Java files, no clone/system_server entry")
