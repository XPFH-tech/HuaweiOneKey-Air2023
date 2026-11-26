#!/usr/bin/env python3
"""Build-only stability baseline; no device access or deployment."""
from pathlib import Path
import subprocess, sys, difflib
PIN = "8e6acf3b2b56a3cbadde9d6c8f7265e4254669a9"
root = Path(sys.argv[1]).resolve()
rev = subprocess.check_output(["git","-C",str(root),"rev-parse","HEAD"],text=True).strip()
assert rev == PIN, "Unexpected upstream commit"
base = root / "lsp-app/app/java/com/huawei/onekey/optimizer/hook"
changes = {
"HookEntry.java": """package com.huawei.onekey.optimizer.hook;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
/** Air2023 build-only baseline: intentionally installs no hooks. */
public class HookEntry implements IXposedHookLoadPackage {
    @Override public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lp) {}
}
""",
"impl/ThemeHooks.java": """package com.huawei.onekey.optimizer.hook.impl;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
/** Preserve original theme lifecycle and licensing. */
public final class ThemeHooks {
    private ThemeHooks() {}
    public static void install(XC_LoadPackage.LoadPackageParam lp) {}
}
"""
}
diff = []
for name, new in changes.items():
    path = base / name
    old = path.read_text(encoding="utf-8")
    assert ("handleLoadPackage" if name == "HookEntry.java" else "TryOutTimeFenceService") in old
    diff.extend(difflib.unified_diff(old.splitlines(True), new.splitlines(True), fromfile="a/"+name, tofile="b/"+name))
    path.write_text(new, encoding="utf-8")
Path("air2023-baseline.diff").write_text("".join(diff), encoding="utf-8")
print("No-hook build baseline prepared; no device operation.")
