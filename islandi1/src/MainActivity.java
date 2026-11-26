package dev.air2023.islandi1;
import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.ScrollView;
public final class MainActivity extends Activity{
 @Override public void onCreate(Bundle b){super.onCreate(b);TextView t=new TextView(this);t.setPadding(32,32,32,32);t.setTextSize(18);
 t.setText("Air2023 灵动岛 I1\n\n仅 MatePad Air 2023 DBY2-W00 4.2.0.252。\nVector 启用本模块，仅勾选系统用户界面。保留 v0.4、D3、C4R1、Volume V1，P1 关闭。\n备份重要数据后手动软重启。解锁后在桌面拔插充电线，预计状态栏中央出现5秒充电提示。播放音乐并返回桌面，观察媒体播放胶囊；暂停后应消失。测试横竖屏、锁屏和原生胶囊。\n首版只在原生状态栏空闲区域绘制，不创建悬浮窗口，不接管触摸、不隐藏原厂提示、不读取通知或媒体标题。遇到已有图标、布局或权限不符合时跳过。暂不支持点击、展开、位置和宽度设置。\n日志搜索 Air2023IslandI1：I1_HOOK_READY、I1_HOST_READY、I1_CHARGE_EVENT、I1_SHOWN CHARGE / MEDIA。若异常，关闭模块并重新初始化 SystemUI。");
 ScrollView s=new ScrollView(this);s.addView(t);setContentView(s);}
}
