package dev.air2023.islandi4;
import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.*;
import android.view.View;
public final class MainActivity extends Activity{
 SharedPreferences prefs;LinearLayout body;
 @Override public void onCreate(Bundle b){super.onCreate(b);
   boolean shared=false;
   try{prefs=getSharedPreferences(Config.FILE,MODE_WORLD_READABLE);shared=true;}
   catch(SecurityException e){prefs=getSharedPreferences(Config.FILE,MODE_PRIVATE);}
   if(!prefs.contains("schema")){
     SharedPreferences.Editor ed=prefs.edit().putInt("schema",4).putInt("offset",0).putInt("width",100);
     for(String key:Config.KEYS)ed.putBoolean(key,true);
     if(!ed.commit())Toast.makeText(this,"配置初始化失败",Toast.LENGTH_LONG).show();
   }
   body=new LinearLayout(this);body.setOrientation(1);body.setPadding(32,24,32,24);
   TextView title=new TextView(this);title.setTextSize(24);title.setText("Air2023 灵动岛 I4");body.addView(title);
   TextView info=new TextView(this);info.setText("配置通道："+(shared?"框架允许共享":"私有模式，需检查 Vector 日志")+"\n开关及尺寸保存后，重新加载 SystemUI 生效。先打开本页初始化配置，再关闭 I1/I2/I3、启用 I4（仅系统用户界面）。保留其他模块。\n点击音乐胶囊打开控制面板；仅在兼容布局和标准媒体会话下可用。暂停时面板内可恢复播放，关闭面板后暂停胶囊消失。状态栏胶囊区域触摸可能影响从该区域开始的下拉手势。其他区域不修改。备份重要数据后手动软重启。");body.addView(info);
   for(int i=0;i<Config.KEYS.length;i++){
     final String k=Config.KEYS[i];
     Switch sw=new Switch(this);sw.setText(Config.NAMES[i]);sw.setChecked(prefs.getBoolean(k,true));body.addView(sw);
     sw.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener(){public void onCheckedChanged(CompoundButton v,boolean checked){save(prefs.edit().putBoolean(k,checked).commit());}});
   }
   slider("水平偏移（dp，负左正右）","offset",-120,120,0);
   slider("胶囊宽度（%）","width",80,125,100);
   Button reset=new Button(this);reset.setText("恢复默认设置");body.addView(reset);
   reset.setOnClickListener(new View.OnClickListener(){public void onClick(View v){SharedPreferences.Editor ed=prefs.edit().putInt("schema",4).putInt("offset",0).putInt("width",100);for(String k:Config.KEYS)ed.putBoolean(k,true);save(ed.commit());recreate();}});
   TextView log=new TextView(this);log.setText("日志：Air2023IslandI4 / I4_CONFIG_OK / I4_TOUCH_READY / I4_POPUP_SHOWN / I4_MEDIA_CONTROL。配置不可读时不注入。暂不支持秒表或计时器。");body.addView(log);
   ScrollView scroll=new ScrollView(this);scroll.addView(body);setContentView(scroll);
 }
 void slider(final String label,final String key,final int min,int max,int def){
   final TextView t=new TextView(this);int old=Math.max(min,Math.min(max,prefs.getInt(key,def)));t.setText(label+"："+old);body.addView(t);
   SeekBar bar=new SeekBar(this);bar.setMax(max-min);bar.setProgress(old-min);body.addView(bar);
   bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onStartTrackingTouch(SeekBar b){}public void onProgressChanged(SeekBar b,int n,boolean user){t.setText(label+"："+(n+min));}public void onStopTrackingTouch(SeekBar b){save(prefs.edit().putInt(key,b.getProgress()+min).commit());}});
 }
 void save(boolean ok){Toast.makeText(this,ok?"已保存，下次加载生效":"保存失败",Toast.LENGTH_SHORT).show();}
}
