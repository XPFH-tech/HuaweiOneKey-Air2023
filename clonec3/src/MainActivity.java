package dev.air2023.clonec3;
import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.ScrollView;
public final class MainActivity extends Activity {
 @Override public void onCreate(Bundle b){
   super.onCreate(b);
   TextView t=new TextView(this);t.setTextSize(18);t.setPadding(32,32,32,32);
   t.setText("Air2023 酷安分身实验 C3\n\n仅 DBY2-W00 4.2.0.252(C00E100R3P5)。\n仅扩展酷安 com.coolapk.market，保留七项原厂白名单。C2 继续负责设置页面。C3 只勾选系统框架 android。\n警告：注入 system_server 存在启动异常、临时Root丢失及非原厂分身清理风险。测试前备份重要数据；不自动重启，不自动安装、不启用组件、不删除分身、不修改权限或用户ID。\nVector 日志搜索 Air2023CloneC3。只有 C3_ALLOWLIST_OK 才继续验证。未命中或跳过均不是成功。\n停用 C3 并重新初始化系统框架会恢复原厂内存白名单，但原生清理逻辑可能移除非原厂分身。先备份数据，不要直接删除分身用户。");
   ScrollView s=new ScrollView(this);s.addView(t);setContentView(s);
 }
}
