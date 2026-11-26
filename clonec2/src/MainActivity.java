package dev.air2023.clonec2;
import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.ScrollView;
public final class MainActivity extends Activity{
 @Override public void onCreate(Bundle b){
   super.onCreate(b);
   TextView t=new TextView(this);t.setTextSize(18);t.setPadding(32,32,32,32);
   t.setText("Air2023 应用分身 C2\n\n仅支持 DBY2-W00 4.2.0.252(C00E100R3P5)。\n在 Vector 启用 C2，仅勾选设置 com.android.settings；保留 v0.4 与 D3。\n重新加载设置后进入原生应用分身页面。先验证是否显示新增应用，再挑一个非关键应用测试。\n本模块只扩展设置候选列表，不修改系统文件，不主动创建分身，不修改安装权限、系统服务或存储检查。若原生创建失败请保留日志，不反复操作。\n停用模块并重新加载设置可恢复原候选列表，不会自动删除已创建的分身。\nVector 日志搜索 Air2023CloneC2。");
   ScrollView s=new ScrollView(this);s.addView(t);setContentView(s);
 }
}
