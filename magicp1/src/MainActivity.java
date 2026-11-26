package dev.air2023.magicp1;
import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.ScrollView;
public final class MainActivity extends Activity {
 @Override public void onCreate(Bundle b){
   super.onCreate(b);TextView t=new TextView(this);t.setTextSize(18);t.setPadding(32,32,32,32);
   t.setText("Air2023 平行视界 P1\n\n系统框架专项小范围实验。保留 v0.4、D3、C4R1。不使用 C1。\n先确认 C4R1 名单已保存。在 Vector 启用 P1，仅勾选系统框架 android。备份重要数据后自行决定是否软重启。\n只在机主本地平板容器初始化时，尝试为最多6个已审核且未获原厂适配的应用添加默认关闭的平行视界配置。优先阅读及文献应用。不会覆盖已存在的原厂条目，不强制开启平行视界，不写系统分区，不修改分身白名单，不自动重启。\nVector 日志搜索 Air2023MagicP1。出现 P1_CONFIG_ADDED 后，再查看 P1_NATIVE_SWITCH；仅当原生设置中出现对应应用时，才逐个打开开关测试横屏。\n如果 P1 未加载或没有候选，不代表平行视界无法移植。出现异常先关闭本模块；系统框架重启仍有风险。");
   ScrollView sc=new ScrollView(this);sc.addView(t);setContentView(sc);
 }
}
