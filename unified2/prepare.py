#!/usr/bin/env python3
from pathlib import Path
import sys, subprocess, shutil, xml.etree.ElementTree as ET
repo=Path(__file__).resolve().parents[1]
up=Path(sys.argv[1]).resolve()
subprocess.run([sys.executable,str(repo/"unified1/prepare.py"),str(up)],check=True)
app=up/"lsp-app/app"
java=app/"java"
old="dev.air2023.unified1"
pkg="dev.air2023.unified2"
src=java/"dev/air2023/unified1"
dst=java/"dev/air2023/unified2"
src.rename(dst)
for f in dst.rglob("*.java"):
    s=f.read_text(encoding="utf-8").replace(old,pkg)
    s=s.replace("U1 ·","U2 ·").replace("U1 首页","U2 首页").replace("C4R1 继续独立运行。","C4R1 已整合。").replace("U1 不继承","U2 不继承")
    f.write_text(s,encoding="utf-8")
for folder,sub,names in [("clonec4r1","clone",["Entry.java","Policy.java","MainActivity.java"]),("batteryb1","battery",["MainActivity.java"]),("meminfom1","meminfo",["MainActivity.java"])]:
    target=dst/sub
    target.mkdir()
    for name in names:
        s=(repo/folder/"src"/name).read_text(encoding="utf-8").replace("package dev.air2023."+folder+";","package "+pkg+"."+sub+";")
        if sub=="clone" and name=="MainActivity.java":
            s=s.replace("Air2023 原生应用分身 C4R1","U2 · 原生应用分身 C4R1")
            s=s.replace("首次先预览并保存名单，再启用 C4R1（设置+系统框架），停用 C2/C3/C4 后手动软重启。","现有 C4R1 名单原样复用，首次迁移只需检查，不要重新同步。U2 设置+系统框架必须同时启用，停用旧 C4R1 后手动软重启。")
            s=s.replace("首次安装请停用 C2/C3、启用 C4R1 两个作用域，再备份后软重启。","确认旧 C4R1 已停用，U2 设置和系统框架作用域已启用，再备份后软重启。")
            needle=" private static void checkExcludedClones()throws Exception{"
            assert needle in s
            addition=r''' public static String preflight()throws Exception{
   String r=exec(READ,null);int sep=r.indexOf('\n');
   if(sep<0||!r.startsWith("STATE=")||r.startsWith("STATE=ABSENT\n"))throw new IOException("original snapshot missing");
   Set<String> set=Policy.parse(r.substring(sep+1));
   if(!set.contains(Policy.TEST))throw new IOException("original snapshot missing test package");
   String context=exec(CHECK+"ls -Zd /data/system/air2023_clonec4.list",null);
   if(!context.trim().startsWith("u:object_r:system_data_file:s0 "))throw new IOException("snapshot SELinux context");
   checkExcludedClones();
   return "C4R1 名单只读检查通过： "+set.size()+" 项新增（另保留7项原厂）。\n文件、权限、格式和排除项检查通过；尚未验证 U2 system_server 实际读取。";
 }
'''
            s=s.replace(needle,addition+needle)
        (target/name).write_text(s,encoding="utf-8")
for f in (repo/"unified2/src").glob("*.java"):
    shutil.copyfile(f,dst/f.name)
manifest=app/"AndroidManifest.xml"
s=manifest.read_text(encoding="utf-8").replace(old,pkg).replace('versionCode="100"','versionCode="200"').replace('versionName="1.0-U1"','versionName="2.0-U2"')
s=s.replace('<queries><package android:name="dev.air2023.clonec4r1"/></queries>','')
if 'android.permission.QUERY_ALL_PACKAGES' not in s:
    s=s.replace("<application ",'<uses-permission android:name="android.permission.QUERY_ALL_PACKAGES"/>\n<application ')
s=s.replace('<meta-data android:name="xposedmodule"','<activity android:name=".clone.MainActivity" android:exported="false"/>\n<activity android:name=".battery.MainActivity" android:exported="false"/>\n<activity android:name=".meminfo.MainActivity" android:exported="false"/>\n<activity android:name=".MigrationActivity" android:exported="false"/>\n<meta-data android:name="xposedmodule"')
s=s.replace("Air2023 U1: Settings, launcher, SystemUI; no system_server hooks","Air2023 U2: guarded C4R1 clone hook, base, volume, island, root tools")
manifest.write_text(s,encoding="utf-8");ET.parse(manifest)
(app/"assets/xposed_init").write_text(pkg+".UnifiedEntry\n",encoding="utf-8")
(app/"res/values/arrays.xml").write_text('<resources><string-array name="xposed_scope"><item>com.android.settings</item><item>com.huawei.android.launcher</item><item>com.android.systemui</item><item>android</item></string-array></resources>',encoding="utf-8")
strings=app/"res/values/strings.xml"
strings.write_text(strings.read_text(encoding="utf-8").replace("Air2023 U1","Air2023 U2"),encoding="utf-8")
assert len(list(java.rglob("*.java")))==31,[p.name for p in java.rglob("*.java")]
assert len(list((dst/"clone").glob("*.java")))==3
assert 'dev.air2023.unified1' not in "".join(f.read_text() for f in dst.rglob("*.java") if f.name!="MigrationActivity.java")
assert not any(p.name in {"ThemeHooks.java","MxxHooks.java","RootShell.java"} for p in java.rglob("*.java"))
print("U2 source allowlist: 31 Java files, only C4R1 system_server hook")
