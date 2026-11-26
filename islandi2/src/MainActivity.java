package dev.air2023.islandi2;
import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.ScrollView;
public final class MainActivity extends Activity{
 @Override public void onCreate(Bundle b){super.onCreate(b);TextView t=new TextView(this);t.setPadding(32,32,32,32);t.setTextSize(18);
 t.setText("Air2023 灵动岛 I2\n\n仅 MatePad Air 2023 DBY2-W00 4.2.0.252。\nVector 启用本模块，仅勾选系统用户界面。保留 v0.4、D3、C4R1、Volume V1，P1 关闭。\n备份重要数据后手动软重启。解锁后在桌面拔插充电线，预计状态栏中央出现5秒充电提示。播放音乐并返回桌面，观察媒体播放胶囊；暂停后应消失。测试横竖屏、锁屏和原生胶囊。\n新版按真实状态栏时钟文字定位，支持较高平板容器；音乐会使用会话和实际媒体音频状态双重检测。仅在原生状态栏空闲区域绘制。只在原生状态栏空闲区域绘制，不创建悬浮窗口，不接管触摸、不隐藏原厂提示、不读取通知或媒体标题。遇到已有图标、布局或权限不符合时跳过。暂不支持点击、展开、位置和宽度设置。\n日志搜索 Air2023IslandI2：I2_HOOK_READY、I2_HOST_READY、I2_GEOMETRY_OK、I2_MEDIA_STATE、I2_CHARGE_EVENT、I2_SHOWN CHARGE / MEDIA。若异常，关闭模块并重新初始化 SystemUI。");
 ScrollView s=new ScrollView(this);s.addView(t);setContentView(s);}
}
