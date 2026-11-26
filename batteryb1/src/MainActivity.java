package dev.air2023.batteryb1;
import android.app.Activity;
import android.os.Bundle;
import android.os.SystemClock;
import android.os.Build;
import android.content.ClipboardManager;
import android.content.ClipData;
import android.widget.*;
import android.view.View;
import android.graphics.Color;
import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.nio.charset.StandardCharsets;

public final class MainActivity extends Activity {
 private TextView health,details,raw,notice;
 private ProgressBar bar;
 private Button refresh,copy;
 private boolean busy;
 private String report="";
 private static final String BASE="/sys/class/power_supply/";
 private static final String CMD="echo ROOT_UID=$(id -u); echo MODEL=$(getprop ro.product.model); echo BUILD=$(getprop ro.build.display.id); echo SECTION=battery; cat /sys/class/power_supply/battery/uevent; echo SECTION=gauge; cat /sys/class/power_supply/battery_gauge/uevent; echo SECTION=backup; cat /sys/class/power_supply/bk_battery/uevent; echo SECTION=power_log; cat /sys/class/hw_power/power_log/head_all; cat /sys/class/hw_power/power_log/content_all; echo SECTION=ram; getprop ro.config.hide_meminfo; echo END_B1";
 private TextView text(LinearLayout box,String s,int size){TextView v=new TextView(this);v.setText(s);v.setTextSize(size);v.setPadding(0,8,0,8);box.addView(v);return v;}
 public void onCreate(Bundle saved){super.onCreate(saved);
  LinearLayout p=new LinearLayout(this);p.setOrientation(1);int pad=(int)(20*getResources().getDisplayMetrics().density);p.setPadding(pad,pad,pad,pad);
  text(p,"Air2023 电池健康 B1",25);
  text(p,"仅读取电池驱动 · 不注入 Vector · 不修改校准参数",14);
  health=text(p,"—",36);bar=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);bar.setMax(1000);p.addView(bar);
  details=text(p,"点击读取底层数据",17);notice=text(p,"",14);
  refresh=new Button(this);refresh.setText("读取 / 刷新电池数据");p.addView(refresh);refresh.setOnClickListener(new View.OnClickListener(){public void onClick(View v){read();}});
  copy=new Button(this);copy.setText("复制完整检测报告");copy.setEnabled(false);p.addView(copy);copy.setOnClickListener(new View.OnClickListener(){public void onClick(View v){((ClipboardManager)getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("Air2023 B1",report));Toast.makeText(MainActivity.this,"报告已复制",Toast.LENGTH_SHORT).show();}});
  raw=text(p,"原始数据将在此显示",12);raw.setTextIsSelectable(true);ScrollView scroll=new ScrollView(this);scroll.addView(p);setContentView(scroll);
 }
 private static String exec() throws Exception {
  final Process proc=new ProcessBuilder("su","-c",CMD).redirectErrorStream(true).start();
  final ByteArrayOutputStream buf=new ByteArrayOutputStream();
  Thread reader=new Thread(new Runnable(){public void run(){try(InputStream in=proc.getInputStream()){byte[] b=new byte[2048];int n;while((n=in.read(b))!=-1){synchronized(buf){if(buf.size()<48000)buf.write(b,0,Math.min(n,48000-buf.size()));}}}catch(IOException e){}}});reader.start();
  boolean done=proc.waitFor(20,TimeUnit.SECONDS);if(!done)proc.destroyForcibly();reader.join(1000);
  synchronized(buf){return "Exit="+(done?proc.exitValue():"timeout")+"\n"+new String(buf.toByteArray(),StandardCharsets.UTF_8);}
 }
 private static Map<String,String> section(String s,String name){Map<String,String> m=new HashMap<String,String>();boolean on=false;for(String l:s.split("\\r?\\n")){if(l.startsWith("SECTION=")){on=l.equals("SECTION="+name);continue;}if(on&&l.startsWith("POWER_SUPPLY_")){int i=l.indexOf('=');if(i>0)m.put(l.substring(13,i),l.substring(i+1).trim());}}return m;}
 private static long n(Map<String,String> m,String k){try{return Long.parseLong(m.get(k));}catch(Exception e){return -1;}}
 private static String value(Map<String,String> m,String k){String v=m.get(k);return v==null?"未知":v;}
 private static String mah(long v){return v>0?String.format(Locale.US,"%.0f mAh",v/1000.0):"未知";}
 private void read(){if(busy)return;busy=true;refresh.setEnabled(false);copy.setEnabled(false);health.setText("读取中");bar.setProgress(0);details.setText("");notice.setText("");raw.setText("");
  new Thread(new Runnable(){public void run(){String s;try{s=exec();}catch(Exception e){s="ERROR "+e;}final String result=s;runOnUiThread(new Runnable(){public void run(){if(isFinishing()||isDestroyed())return;busy=false;refresh.setEnabled(true);show(result);}});}}).start();
 }
 private void show(String s){
  boolean ok=s.startsWith("Exit=0\n")&&s.contains("\nROOT_UID=0\n")&&s.contains("\nMODEL=DBY2-W00\n")&&s.contains("\nEND_B1");
  Map<String,String> b=section(s,"battery"),g=section(s,"gauge"),k=section(s,"backup");
  long f=n(b,"CHARGE_FULL"),d=n(b,"CHARGE_FULL_DESIGN"),gf=n(g,"CHARGE_FULL"),c=n(b,"CYCLE_COUNT"),gc=n(g,"CYCLE_COUNT");
  boolean valid=ok&&f>=1000000&&f<=15000000&&d>=1000000&&d<=15000000&&f<=d*1.3;
  String warning="容量健康度是电量计报告的 FCC / 设计容量，不是独立放电实测。\n";
  if(valid){double h=100.0*f/d;health.setText(String.format(Locale.US,"%.2f%%",h));bar.setProgress((int)Math.min(1000,Math.round(h*10)));
   warning+=(gf>0&&Math.abs(f/1000.0-gf)<=1?"主电量计与 battery 一致（可能同一数据源）。":"电量计容量不一致或不可读，需核实。")+"\n";
  }else{health.setText("无法确认");bar.setProgress(0);warning+="读取失败、设备不匹配或容量单位异常；不生成健康度。\n";}
  if(c>=0&&c<30)warning+="循环次数偏低：若使用多年，可能不是完整历史。\n";
  if(c>=0&&gc>=0&&c!=gc)warning+="主电池与电量计循环计数不一致。\n";
  long volt=n(b,"VOLTAGE_NOW"),temp=n(b,"TEMP"),level=n(b,"CAPACITY");
  String summary="满充容量："+mah(f)+"\n设计容量："+mah(d)+"\n主电量计："+(gf>0?gf+" mAh":"未知")+"\n循环次数："+(c>=0?c+" 次":"未知")+"（电量计："+(gc>=0?gc:"未知")+"）\n电量："+(level>=0&&level<=100?level+"%":"未知")+"\n电压："+(volt>0?String.format(Locale.US,"%.3f V",volt/1000000.0):"未知")+"\n温度："+(temp>=-400&&temp<=1000&&b.containsKey("TEMP")?String.format(Locale.US,"%.1f °C",temp/10.0):"未知")+"\n电池状态："+value(b,"STATUS")+" / "+value(b,"HEALTH")+"\n电量计型号："+value(g,"MODEL_NAME")+"\n电池品牌："+value(b,"BRAND")+"\n备用接口：FCC "+mah(n(k,"CHARGE_FULL"))+"；设计 "+mah(n(k,"CHARGE_FULL_DESIGN"))+"；循环 "+value(k,"CYCLE_COUNT");
  details.setText(summary);notice.setText(warning+"备用接口不用于整机健康度计算。首版不测内阻、不重置循环、不校准。");
  report="B1_REPORT\n"+new Date()+"\n"+health.getText()+"\n"+summary+"\n"+notice.getText()+"\n"+s;raw.setText(s);copy.setEnabled(true);
 }
}
