package dev.air2023.unified2;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.*;
import android.os.Bundle;
import android.os.Build;
import android.view.View;
import android.widget.*;
import android.graphics.Typeface;

public final class MainActivity extends Activity {
 private SharedPreferences prefs;
 private boolean shared=true;
 private LinearLayout body;
 private TextView status;
 private static final String[] BASE={"timeout","doubletap","seconds","app_details","cells","dedupe","dock","portrait","drawer","notext","form_title","blur"};
 private static final String[] ISLAND={"enabled","charge","media","metadata","volume","bluetooth","touch"};
 private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
 private SharedPreferences get(String name){
   try{return getSharedPreferences(name,MODE_WORLD_READABLE);}
   catch(SecurityException e){shared=false;return getSharedPreferences(name,MODE_PRIVATE);}
 }
 private boolean initialize(boolean publish){
   prefs=get("air2023_u1");
   SharedPreferences b=get("air2023_b04"),i=get("air2023_i4");
   SharedPreferences.Editor u=prefs.edit().putInt("schema",1),be=b.edit().putInt("schema",4),ie=i.edit().putInt("schema",4);
   for(String k:new String[]{"base","volume","island"})if(!prefs.contains(k))u.putBoolean(k,false);
   for(int j=0;j<BASE.length;j++)if(!b.contains(BASE[j]))be.putBoolean(BASE[j],j<3);
   for(String k:ISLAND)if(!i.contains(k))ie.putBoolean(k,true);
   if(!b.contains("safe"))be.putBoolean("safe",false);
   if(!b.contains("radius"))be.putInt("radius",46);
   if(!i.contains("offset"))ie.putInt("offset",0);
   if(!i.contains("width"))ie.putInt("width",100);
   if(publish){
     long rev=System.currentTimeMillis();
     u.putLong("published",rev);be.putLong("published",rev);ie.putLong("published",rev);
   }
   boolean a=u.commit(),bb=be.commit(),ii=ie.commit();
   return a&&bb&&ii&&shared;
 }
 @Override public void onCreate(Bundle s){
   super.onCreate(s);
   body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(dp(20),dp(18),dp(20),dp(24));
   label("华为一键优化 · Air2023",25,true);
   label("2.0-U2  |  "+Build.MODEL+"  |  "+Build.DISPLAY,13,false);
   boolean ok=initialize(false);
   status=label("",14,false);
   updateStatus(ok);
   label("迁移与安全检查",18,true);
   button("迁移检查 / 导入 U1 开关",new View.OnClickListener(){public void onClick(View v){startActivity(new Intent(MainActivity.this,MigrationActivity.class));}});
   label("先备份重要分身数据，完成迁移检查（确认原 C4R1 名单可读），不要重新扫描或覆盖名单。首次启用 U2：Vector 勾选设置、华为桌面、系统用户界面、系统框架。强停 U2 应用再打开，导入或手动配置并发布；停用旧 U1 与 C4R1 的全部作用域，再手动软重启。切换中不要打开分身设置；不要在两套模块同时启用时重启。",14,false);
   button("发布 / 重新写入共享配置",new View.OnClickListener(){public void onClick(View v){shared=true;boolean success=initialize(true);updateStatus(success);Toast.makeText(MainActivity.this,success?"配置已写入，需日志确认共享读取":"共享模式或写入未确认，请先启用 Vector 后重开应用",Toast.LENGTH_LONG).show();}});
   section("基础优化 · v0.4","base","休眠时长、桌面、状态栏秒数等。设置沿用 v0.4 内核，可通过迁移页手动导入 U1 开关。");
   button("基础优化设置",new View.OnClickListener(){public void onClick(View v){startActivity(new Intent(MainActivity.this,BaseActivity.class));}});
   section("控制中心音量条 · V1","volume","保持已验证的独立音量条实现。");
   section("灵动岛 · I4","island","充电、音乐、音量、蓝牙、点击交互及独立配置。");
   button("灵动岛设置",new View.OnClickListener(){public void onClick(View v){startActivity(new Intent(MainActivity.this,dev.air2023.unified2.island.MainActivity.class));}});
   label("多窗上限 · D3",18,true);
   label("属性不会自动修改。新 APK 的撤销记录独立，旧 D3 保留。",14,false);
   button("检查与调整多窗上限",new View.OnClickListener(){public void onClick(View v){startActivity(new Intent(MainActivity.this,dev.air2023.unified2.window.MainActivity.class));}});
   label("原生应用分身 · C4R1",18,true);
   label("原白名单原样复用，不自动同步。系统框架和设置作用域必须成对保留，关闭可能使非原厂分身失去支持。",14,false);
   button("原生应用分身管理",new View.OnClickListener(){public void onClick(View v){startActivity(new Intent(MainActivity.this,dev.air2023.unified2.clone.MainActivity.class));}});
   label("运行内存显示 · M1",18,true);
   label("只修改临时显示属性，不增加物理 RAM。",14,false);
   button("检查与调整运行内存显示",new View.OnClickListener(){public void onClick(View v){startActivity(new Intent(MainActivity.this,dev.air2023.unified2.meminfo.MainActivity.class));}});
   label("电池健康 · B1",18,true);
   label("读取电量计 FCC / 设计容量、循环次数、电压和温度，不写电池参数。",14,false);
   button("电池健康检测",new View.OnClickListener(){public void onClick(View v){startActivity(new Intent(MainActivity.this,dev.air2023.unified2.battery.MainActivity.class));}});
   button("关闭三组基础 Hook（保留分身）",new View.OnClickListener(){public void onClick(View v){
     new AlertDialog.Builder(MainActivity.this).setTitle("停止三组 Hook").setMessage("仅关闭基础优化、音量条、灵动岛三组注入，不停止分身 Hook、不恢复 D3/M1 属性、不自动重启。").setNegativeButton("取消",null).setPositiveButton("确认",new DialogInterface.OnClickListener(){public void onClick(DialogInterface d,int w){boolean ok=prefs.edit().putBoolean("base",false).putBoolean("volume",false).putBoolean("island",false).commit();if(ok)recreate();else Toast.makeText(MainActivity.this,"保存失败",Toast.LENGTH_SHORT).show();}}).show();
   }});
   label("日志：Air2023U2 / U2_CONFIG_OK；各功能保留 Air2023B04、Air2023VolumeV1、Air2023IslandI4 标签。配置不可读时跳过三组基础 Hook（分身独立）。",13,false);
   ScrollView scroll=new ScrollView(this);scroll.addView(body);setContentView(scroll);
 }
 private void updateStatus(boolean ok){status.setText(ok?"配置通道：框架允许共享模式；实际读取以 U2_CONFIG_OK 为准。":"配置通道：私有模式或保存失败；先启用模块、强停 U2 并重新打开，再发布。");}
 private TextView label(String text,int size,boolean bold){TextView t=new TextView(this);t.setText(text);t.setTextSize(size);if(bold)t.setTypeface(null,Typeface.BOLD);t.setPadding(0,dp(6),0,dp(6));body.addView(t);return t;}
 private void button(String text,View.OnClickListener l){Button b=new Button(this);b.setAllCaps(false);b.setText(text);b.setOnClickListener(l);body.addView(b);}
 private void section(String title,final String key,String desc){
   Switch sw=new Switch(this);sw.setText(title);sw.setTextSize(17);sw.setChecked(prefs.getBoolean(key,false));body.addView(sw);label(desc,13,false);
   sw.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener(){public void onCheckedChanged(CompoundButton v,boolean checked){
     boolean ok=prefs.edit().putBoolean(key,checked).commit();
     Toast.makeText(MainActivity.this,ok?"已保存，下次加载生效；确认旧模块不重复启用":"保存失败",Toast.LENGTH_SHORT).show();
   }});
 }
}
