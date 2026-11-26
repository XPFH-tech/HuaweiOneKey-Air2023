# Huawei OneKey Air2023 0.3 Beta
Independent package dev.air2023.onekeybeta, using the upstream Material layout and an explicit Java source allowlist. DBY2-W00 / 4.2.0.252 only.
- Settings timeout: R4 UI options confirmed on device; actual timing and long-term reliability remain unverified.
- Launcher double-tap and SystemUI long-press clock seconds: upstream implementations, experimental, opt-in by selecting their respective scope in Vector. Only Settings is suggested by the manifest. These two features have not been device tested.
- Scope selection is the actual feature control. The app UI is informational, not a Root control panel. Disable overlapping original/R4 scopes before testing. Reuse the user's previously working soft restart only when needed.
- No theme hooks, installer/security bypass, framework injection, root command, privileged script, Magisk module, or deployment. Other features pending.
- Test signing is ephemeral; future upgrades may require uninstalling this beta only. Do not uninstall the original app or wipe data. If anything misbehaves, disable this beta in Vector and reload the affected process.
- Source and assets derived from upstream MIT-licensed HuaweiOneKeyOptimizer commit 8e6acf3b2b56a3cbadde9d6c8f7265e4254669a9; full upstream license included.
