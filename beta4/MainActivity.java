package dev.air2023.onekeybeta4;

import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.widget.*;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.button.MaterialButton;

public final class MainActivity extends AppCompatActivity {
    private static final String[] KEYS={"timeout","doubletap","seconds","app_details","cells","dedupe","dock","portrait","drawer","notext","form_title","blur"};
    private static final String[] NAMES={"休眠 20/30/60 分钟","桌面双击锁屏","状态栏长按显示秒","长按图标应用详情","新增 7×5 / 8×5 网格","布局去重","Dock 横屏11 / 竖屏8","竖屏 Dock 保留右侧","抽屉隐藏应用建议","桌面图标文字隐藏","服务卡片标题隐藏","桌面转场模糊"};
    private SharedPreferences prefs;
    private boolean sharedMode;
    private LinearLayout panel;
    private TextView note;
    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_main);
        try { prefs=getSharedPreferences("air2023_b04",MODE_WORLD_READABLE); sharedMode=true; }
        catch(SecurityException e) { prefs=getSharedPreferences("air2023_b04",MODE_PRIVATE); }
        if (!prefs.contains("schema")) {
            SharedPreferences.Editor ed=prefs.edit().putInt("schema",4).putBoolean("safe",false).putInt("radius",46);
            for(int i=0;i<KEYS.length;i++) ed.putBoolean(KEYS[i],i<3);
            if(!ed.commit()) Toast.makeText(this,"配置保存失败",Toast.LENGTH_LONG).show();
        }
        ((TextView)findViewById(R.id.tv_root)).setText("设备："+Build.MODEL+" / "+Build.DISPLAY);
        ((TextView)findViewById(R.id.tv_xposed)).setText("配置通道："+(sharedMode?"框架允许共享模式":"私有模式；需检查 Vector 日志"));
        ((TextView)findViewById(R.id.tv_module)).setText("v0.4 Beta · 独立开关 · 无 Root 命令");
        note=findViewById(R.id.beta_note);
        note.setText("先关闭 v0.3 和原版的重叠作用域。新版在 Vector 勾选设置、华为桌面、系统用户界面。开关保存后需重新加载对应进程（必要时手动软重启）。日志搜索 Air2023B04，必须有 CONFIG_OK；读不到配置则不注入。新功能每次只测试一项。网格/Dock 可能改变布局，先备份桌面。安全模式阻止下次加载，不会撤销已发生的布局或正在运行的 Hook。");
        panel=(LinearLayout)note.getParent();
        MaterialSwitch safe=new MaterialSwitch(this);
        safe.setText("安全模式：关闭所有 Hook（下次加载）");
        safe.setChecked(prefs.getBoolean("safe",false));
        add(safe);
        safe.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener(){ public void onCheckedChanged(CompoundButton v,boolean checked){save("safe",checked);} });
        for(int i=0;i<KEYS.length;i++){
            final String key=KEYS[i];
            MaterialSwitch sw=new MaterialSwitch(this);
            sw.setText(NAMES[i]+(i<3?" · v0.3 已验证":" · 实验"));
            sw.setChecked(prefs.getBoolean(key,false));
            add(sw);
            sw.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener(){ public void onCheckedChanged(CompoundButton v,boolean checked){save(key,checked);} });
        }
        TextView radius=new TextView(this);
        radius.setText("模糊半径："+prefs.getInt("radius",46));
        add(radius);
        SeekBar bar=new SeekBar(this);
        bar.setMax(99); bar.setProgress(prefs.getInt("radius",46)-1);
        add(bar);
        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            public void onProgressChanged(SeekBar b,int p,boolean user){radius.setText("模糊半径："+(p+1));}
            public void onStartTrackingTouch(SeekBar b){}
            public void onStopTrackingTouch(SeekBar b){saved(prefs.edit().putInt("radius",b.getProgress()+1).commit());}
        });
        MaterialButton reset=new MaterialButton(this);
        reset.setText("恢复 v0.3 三项功能配置");
        add(reset);
        reset.setOnClickListener(new View.OnClickListener(){ public void onClick(View v){
            SharedPreferences.Editor ed=prefs.edit().putBoolean("safe",false).putInt("radius",46);
            for(int i=0;i<KEYS.length;i++)ed.putBoolean(KEYS[i],i<3);
            saved(ed.commit());recreate();
        }});
        TextView pending=new TextView(this);
        pending.setText("尚未移植：控制中心音量条、灵动岛、平行视界、分身、多小窗。保留 v0.3 作为回退。");
        add(pending);
    }
    private void add(View v){
        int m=(int)(16*getResources().getDisplayMetrics().density);
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);
        p.setMargins(m,m/2,m,m/2);panel.addView(v,p);
    }
    private void save(String key,boolean value){saved(prefs.edit().putBoolean(key,value).commit());}
    private void saved(boolean ok){Toast.makeText(this,ok?"已保存，下次加载生效":"保存失败",Toast.LENGTH_SHORT).show();}
}
