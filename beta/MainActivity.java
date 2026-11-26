package dev.air2023.onekeybeta;

import android.os.Build;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public final class MainActivity extends AppCompatActivity {
    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        setContentView(R.layout.activity_main);
        text(R.id.tv_root,"设备："+Build.MODEL+" / "+Build.DISPLAY);
        text(R.id.tv_xposed,"注入状态：请以 Vector 日志为准，本页不申请 Root。");
        text(R.id.tv_module,"Beta 0.3：三个独立作用域；本页为说明，不自动修改系统。");
        text(R.id.beta_note,"使用方法：停用旧版 R4 和原版一键优化的重叠作用域。Vector 先仅勾选设置，必要时使用已验证的软重启方式。\n\n桌面双击锁屏、状态栏秒数尚未在真机验证；如要测试，单独手动添加对应作用域，出现异常立即停用并重新加载。不要勾选系统框架。\n\n未纳入本版：会员主题、安装器安全校验修改、灵动岛、控制中心音量条、桌面布局/模糊、分身、平行视界、多小窗及 Magisk 脚本。\n\n上游：HuaweiOneKeyOptimizer v1.0.33（MIT；许可见 APK assets/UPSTREAM-LICENSE.txt）。");
    }
    private void text(int id,String value) { ((TextView)findViewById(id)).setText(value); }
}
