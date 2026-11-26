package dev.air2023.unified1;
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
   label("1.0-U1  |  "+Build.MODEL+"  |  "+Build.DISPLAY,13,false);
   boolean ok=initialize(false);
   status=label("",14,false);
   updateStatus(ok);
   label("迁移第一阶段",18,true);
   label("首次启用：在 Vector 勾选设置、华为桌面、系统用户界面；不要勾选系统框架。强行停止 U1 应用再重新打开，发布共享配置。三组默认关闭，避免旧模块同时注入。请先按旧设置配置，再停用对应旧模块、开启对应 U1 开关，备份后手动软重启。旧 C4R1 必须保持开启。",14,false);
   button("发布 / 重新写入共享配置",new View.OnClickListener(){public void onClick(View v){shared=true;boolean success=initialize(true);updateStatus(success);Toast.makeText(MainActivity.this,success?"配置已写入，需日志确认共享读取":"共享模式或写入未确认，请先启用 Vector 后重开应用",Toast.LENGTH_LONG).show();}});
   section("基础优化 · v0.4","base","休眠时长、桌面、状态栏秒数等。设置沿用 v0.4 内核，不自动导入旧开关。");
   button("基础优化设置",new View.OnClickListener(){public void onClick(View v){startActivity(new Intent(MainActivity.this,BaseActivity.class));}});
   section("控制中心音量条 · V1","volume","保持已验证的独立音量条实现。");
   section("灵动岛 · I4","island","充电、音乐、音量、蓝牙、点击交互及独立配置。");
   button("灵动岛设置",new View.OnClickListener(){public void onClick(View v){startActivity(new Intent(MainActivity.this,dev.air2023.unified1.island.MainActivity.class));}});
   label("多窗上限 · D3",18,true);
   label("属性不会自动修改。新 APK 的撤销记录独立，旧 D3 保留。",14,false);
   button("检查与调整多窗上限",new View.OnClickListener(){public void onClick(View v){startActivity(new Intent(MainActivity.this,dev.air2023.unified1.window.MainActivity.class));}});
   label("原生应用分身 · C4R1（独立）",18,true);
   label("保留现有白名单和分身数据。U1 不注入 system_server，不同步或改变分身名单。",14,false);
   button("打开现有 C4R1 管理工具",new View.OnClickListener(){public void onClick(View v){
     Intent i=getPackageManager().getLaunchIntentForPackage("dev.air2023.clonec4r1");
     if(i!=null)try{startActivity(i);return;}catch(ActivityNotFoundException ignored){}
     Toast.makeText(MainActivity.this,"未发现 C4R1，请保留或安装已验证版本",Toast.LENGTH_LONG).show();
   }});
   button("关闭 U1 全部 Hook（下次加载）",new View.OnClickListener(){public void onClick(View v){
     new AlertDialog.Builder(MainActivity.this).setTitle("停止 U1 Hook").setMessage("仅关闭 U1 三组注入，不恢复 D3 属性、不修改 C4R1 分身，也不会自动重启。").setNegativeButton("取消",null).setPositiveButton("确认",new DialogInterface.OnClickListener(){public void onClick(DialogInterface d,int w){boolean ok=prefs.edit().putBoolean("base",false).putBoolean("volume",false).putBoolean("island",false).commit();if(ok)recreate();else Toast.makeText(MainActivity.this,"保存失败",Toast.LENGTH_SHORT).show();}}).show();
   }});
   label("日志：Air2023U1 / U1_CONFIG_OK；各功能保留 Air2023B04、Air2023VolumeV1、Air2023IslandI4 标签。配置不可读时跳过全部 U1 Hook。",13,false);
   ScrollView scroll=new ScrollView(this);scroll.addView(body);setContentView(scroll);
 }
 private void updateStatus(boolean ok){status.setText(ok?"配置通道：框架允许共享模式；实际读取以 U1_CONFIG_OK 为准。":"配置通道：私有模式或保存失败；先启用模块、强停 U1 并重新打开，再发布。");}
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
