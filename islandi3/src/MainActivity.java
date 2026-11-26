package dev.air2023.islandi3;
import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.ScrollView;
public final class MainActivity extends Activity{
 @Override public void onCreate(Bundle b){super.onCreate(b);TextView t=new TextView(this);t.setPadding(32,32,32,32);t.setTextSize(18);
 t.setText("Air2023 灵动岛 I3\n\n仅 MatePad Air 2023 DBY2-W00 4.2.0.252。\nVector 停用 I1/I2，启用 I3，仅勾选系统用户界面。保留 v0.4、D3、C4R1、Volume V1，P1 关闭。备份后手动软重启。\n保留 I2 充电和媒体状态，新增歌曲标题、歌手、会话内封面、媒体音量变化条、蓝牙音频设备接入提示。优先级：充电 > 蓝牙音频 > 音量 > 媒体。长标题截断，宽胶囊遇到原厂控件退回普通胶囊。音量约2.5秒，蓝牙约3.5秒。蓝牙仅检测系统新增的音频输出设备，不检测普通蓝牙外设，也不保证是当前路由。\n锁屏隐藏，不读取聊天通知，不访问网络和封面 URI，不记录歌曲名。若无媒体会话元数据则显示通用媒体胶囊。音量通过定期读取状态检测，快速变化可能合并或漏掉。\n日志搜索 Air2023IslandI3：I3_SHOWN CHARGE/MEDIA/VOLUME/BT，I3_METADATA，I3_VOLUME_EVENT，I3_BT_AUDIO_EVENT。\n本版仍不支持点击、展开、位置宽度设置或独立开关。若异常，关闭 I3 并恢复 I2。");
 ScrollView s=new ScrollView(this);s.addView(t);setContentView(s);}
}
