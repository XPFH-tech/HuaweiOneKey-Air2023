package dev.air2023.onekeypreview;
import android.app.Activity;
import android.os.Bundle;
import android.os.Build;
import android.widget.TextView;
import android.widget.ScrollView;
public final class MainActivity extends Activity {
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        TextView t=new TextView(this);
        t.setTextSize(17);
        t.setPadding(32,32,32,32);
        t.setText("Air 2023 设置功能预览\n\n仅测试屏幕休眠 20/30/60 分钟档位。\n不获取 Root，不修改系统分区，不包含主题解锁。\n\n型号："+Build.MODEL+"\n版本："+Build.DISPLAY+"\n增量版本："+Build.VERSION.INCREMENTAL+"\n\n须在兼容的注入框架中仅选择系统设置作用域后才会加载。未经真机验证；若无效果或出现异常，停用该预览模块。");
        ScrollView scroll=new ScrollView(this); scroll.addView(t); setContentView(scroll);
    }
}
