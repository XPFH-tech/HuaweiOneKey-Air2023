package dev.air2023.volumev1;
import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.ScrollView;
public final class MainActivity extends Activity{
 @Override public void onCreate(Bundle b){super.onCreate(b);
 TextView t=new TextView(this);t.setPadding(32,32,32,32);t.setTextSize(18);
 t.setText("Air2023 控制中心音量 V1\n\n仅 MatePad Air 2023 DBY2-W00 4.2.0.252。\nVector 启用本模块，仅勾选系统用户界面（com.android.systemui）。保持 v0.4、D3、C4R1 不变，P1 关闭。\n备份重要数据后手动软重启，打开控制中心，检查亮度条下方媒体音量条。拖动改变媒体音量，点文字静音或恢复；硬件音量键同步依赖原厂广播。\n不修改亮度控制、系统音频服务或系统分区。不移植上游复杂镜像动画。首版仅适配已知布局，结构不符则跳过。\n日志搜索 Air2023VolumeV1。V1_HOOK_READY 仅说明入口存在，V1_ROW_ADDED 才说明滑条插入。若控制中心布局异常，关闭本模块并重新初始化 SystemUI。系统界面注入仍有崩溃风险。");
 ScrollView sc=new ScrollView(this);sc.addView(t);setContentView(sc);
 }
}
