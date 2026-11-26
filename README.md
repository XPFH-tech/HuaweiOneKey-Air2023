# 华为一键优化 · MatePad Air 2023 专版

**HuaweiOneKey-Air2023 · 2.0-U2**

基于 [fanyantao4110/HuaweiOneKeyOptimizer](https://github.com/fanyantao4110/HuaweiOneKeyOptimizer) 的适配与二次开发项目，面向 MatePad Air 2023 的 HarmonyOS 4.2 临时 Root 环境。采用独立 APK，将桌面增强、系统界面优化、原生分身和电池检测等功能集中管理。

> 非华为官方项目。不提供 Root 获取功能；不修改系统分区、不自动重启。系统框架注入仍有数据丢失及系统异常风险。

## 支持范围

| 项目 | 已测试环境 |
| --- | --- |
| 设备 | HUAWEI MatePad Air 11.5 英寸 2023（DBY2-W00） |
| 系统 | HarmonyOS 4.2.0.252(C00E100R3P5) |
| Android | 12 / API 31 |
| Root 环境 | [DBY2 RootKit v2.1](https://github.com/bytehola/dby2-rootkit/releases/tag/v2.1) 提供的临时 Root、Magisk resetprop |
| 注入框架 | Vector / Zygisk（测试环境 Vector 2.0(1)，API 102） |
| 当前版本 | 2.0-U2（versionCode 200） |
| 包名 | `dev.air2023.unified2` |

其他机型、地区版本和固件尚未验证，不建议直接套用。

## Root 来源与调试环境

本项目的临时 Root 来源为 **bytehola** 开发的 [DBY2 RootKit 2.1（v2.1）](https://github.com/bytehola/dby2-rootkit/releases/tag/v2.1)，上游发布日期为 2026-11-26。感谢 bytehola 为 MatePad Air 2023 提供临时 Root 及注入运行环境。本项目基于该方案提供的 Root 环境开展 HarmonyOS 4.2 的适配、调试和真机验证。

根据 RootKit v2.1 的发布说明，其支持 DBY2-W00、HarmonyOS 4.2.0.252(C00E100R3P5)、Android 12 / API 31、内核 5.4.86-qgki，并提供 Magisk、NyaZygisk、Vector 等组件；临时 Root 不跨完整重启保留。

本项目在此类环境下进行的调试包括：

- 通过 ADB、Root 权限检查、Vector 注入日志及进程状态核对目标固件的 Hook 兼容性。
- 验证 Magisk resetprop 的运行期属性修改与读回，用于多窗上限和最近任务内存显示。
- 检查原生应用分身白名单、权限和系统框架初始化时机，保留现有分身数据。
- 读取电池驱动及华为电源诊断接口，验证 B1 电池检测的数据来源。
- 按独立功能测试、手动软重启、整合验证的顺序开发，避免未验证功能直接进入整合版。

RootKit 是外部运行环境，不是 U2 的组成部分。U2 不包含 Root 引擎、漏洞利用代码或 RootKit 安装包，也不负责获取或恢复 Root。有关 Root 安装、恢复与版本兼容性，请以原项目的发布说明为准。上述调试记录不代表对 RootKit 所有组件、恢复流程或长期稳定性的完整验证。

## 主要功能

| 功能 | 说明 |
| --- | --- |
| 基础优化 | 屏幕休眠 20/30/60 分钟、桌面双击锁屏、状态栏时钟长按显示秒 |
| 桌面增强 | 网格布局、应用详情、图标文字、服务卡片标题、抽屉建议、Dock、转场模糊等独立开关；部分选项仍属实验 |
| 控制中心 | 独立媒体音量条，支持静音/恢复 |
| 灵动岛 | 充电、音乐及封面、媒体音量、蓝牙音频提示，音乐控制及胶囊尺寸调整 |
| 多窗解锁 | 自定义属性上限 2–64，读回检查、撤销；四窗已在此前独立版本验证，64 不代表实际并发能力 |
| 原生应用分身 | C4R1 原生白名单扩展、应用扫描与手动同步；保留已有名单 |
| 多任务内存显示 | M1 临时属性开关，显示最近任务中的运行内存，不增加物理 RAM |
| 电池健康 | B1 只读容量健康度、FCC、设计容量、循环次数、电压、温度、原始诊断报告 |
| 迁移管理 | 旧分身名单检查、尝试导入 U1 开关和共享配置发布 |

## 下载与安装

- [U2 版本说明](docs/releases/2.0-U2.md)
- [U2 已通过的 Actions 构建](https://github.com/XPFH-tech/HuaweiOneKey-Air2023/actions/runs/45896412286)
- [U2 构建产物（ZIP）](https://github.com/XPFH-tech/HuaweiOneKey-Air2023/actions/runs/45896412286/artifacts/15154376011)

Actions 产物保留 7 天，可能需要登录并具备仓库访问权限。过期后可在 Actions 手动运行 **Air2023 Unified U2** 工作流重新构建；每次构建使用临时测试签名，不能保证覆盖安装。

1. 确认机型和固件一致，已通过 [DBY2 RootKit v2.1](https://github.com/bytehola/dby2-rootkit/releases/tag/v2.1) 或经过验证的兼容环境获得临时 Root 与可用 Vector；备份重要数据，特别是非原厂分身。
2. 解压构建产物并安装 `HuaweiOneKey-Air2023-2.0-U2.apk`。从 U1/C4R1 迁移时先检查旧名单，不要重新同步。
3. 检查通过后，在 Vector 停用旧 U1/C4R1，立即启用 U2 的四个作用域，中间不要重启或打开分身设置。
4. 强行停止 U2，再打开；按需导入旧设置或手动配置，点击发布共享配置。
5. 保存工作后手动软重启，检查系统、分身与各项功能。没有旧分身名单的新安装需先明确预览和保存候选名单，不能按旧名单迁移流程处理。

### Vector 作用域

| 作用域 | 功能 |
| --- | --- |
| 系统框架（android） | C4R1 原生分身早期白名单 |
| 设置（com.android.settings） | 休眠时长、原生分身设置 |
| 华为桌面（com.huawei.android.launcher） | 双击锁屏、桌面增强 |
| 系统用户界面（com.android.systemui） | 秒数、灵动岛、控制中心音量条 |

电池检测、内存显示与多窗属性工具本身无需额外注入。不要重复启用旧模块。U2 的三组基础 Hook 开关默认关闭，分身由作用域控制。

## 验证与已知限制

- 2026-11-27：U2 云端构建、APK 签名验证通过；项目维护者在目标平板确认整合版可以正常使用。
- 仅代表当前设备的实际使用反馈，并非所有桌面实验选项、多窗极限、异常恢复或所有分身应用都经过完整验证。
- 完整重启可能丢失临时 Root 和运行期属性。软重启也有异常风险；本项目不自动执行重启。
- 分身沿用 `/data/system/air2023_clonec4.list`。不要删除此文件或在旧版执行恢复原厂；若名单读取失败，原 C4R1 逻辑可能退回原厂白名单加酷安，非原厂分身有失去支持的风险。
- U1 配置导入依赖旧共享配置可读；D3/M1 的历史撤销记录不自动迁移。
- 电池健康度为电量计 FCC 与设计容量之比，不等于独立放电实测；循环次数可能存在历史记录缺失。尚未实现实际放电积分或内阻测试。
- 不包含主题解锁、平行视界移植、计时器/秒表等未完成或未纳入的功能。

## 参考项目与致谢

本项目并非从零原创。主要上游为 **fanyantao** 的华为一键优化，固定参考版本 **v1.0.33**，提交 [`8e6acf3`](https://github.com/fanyantao4110/HuaweiOneKeyOptimizer/commit/8e6acf3b2b56a3cbadde9d6c8f7265e4254669a9)。感谢原作者及其上游模块作者的工作。

| 来源 | 本项目使用或参考内容 |
| --- | --- |
| [fanyantao4110/HuaweiOneKeyOptimizer](https://github.com/fanyantao4110/HuaweiOneKeyOptimizer) | 基础 Hook、桌面功能、资源、构建流程与适配思路 |
| HarmonyOS-FreeformUnlock（上游致谢） | 多窗数量、最近任务内存显示的系统属性方案 |
| CCVolume（上游致谢） | 控制中心音量条的 Hook 与布局参考；V1 重新实现 Java 逻辑 |
| LiveIsland（上游致谢） | 灵动岛交互及动画思路；I 系列按本机布局逐步实现 |
| HwLauncherTweak（上游致谢） | 桌面增强功能参考，经 OneKey 上游引入 |
| [bytehola / DBY2 RootKit v2.1](https://github.com/bytehola/dby2-rootkit/releases/tag/v2.1) | 本项目实际使用的临时 Root 方案来源与适配调试基础；不集成其 Root 引擎或利用代码 |
| [Magisk](https://github.com/topjohnwu/Magisk) / Vector / [LSPosed](https://github.com/LSPosed/LSPosed) / Zygisk | 临时 Root、属性操作及运行期注入环境 |
| XPFH-tech | 目标固件适配、分阶段验证、B1 电池诊断、模块整合与维护 |

对上游未提供独立作者姓名或链接的模块，保留其项目名称，不臆造署名。上游代码及资源按原版权和许可证保留；见 [上游许可证](THIRD_PARTY_LICENSES/OneKey-LICENSE.txt) 及 APK 内的 UPSTREAM-LICENSE.txt。上游为 MIT License（Copyright © 2026 fanyantao）；本仓库新增代码未在本次文档更新中另行指定许可证。

## 问题反馈

反馈时请提供设备型号、完整固件版本、U2 版本及对应日志，避免上传账号、设备序列号或其他隐私信息。可搜索 `Air2023U2`、`U2_CONFIG_OK`、`C4R1_ALLOWLIST_OK`、`Air2023B04`、`Air2023VolumeV1`、`Air2023IslandI4`。

本项目与华为及上述工具作者无官方合作或背书关系。Root 与系统 Hook 存在风险，请自行备份并谨慎测试。
