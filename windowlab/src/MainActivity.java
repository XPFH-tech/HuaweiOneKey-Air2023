package dev.air2023.windowlab;
import android.app.Activity;
import android.os.Bundle;
import android.content.*;
import android.widget.*;
import android.view.View;
import java.io.*;
import java.util.concurrent.TimeUnit;
import java.nio.charset.StandardCharsets;
public final class MainActivity extends Activity {
    private TextView status,out;
    private Button a,b,copy;
    private String first="",second="";
    private boolean busy;
    private static final String CMD =
      "echo IDENTITY; id -u; getprop ro.product.model; getprop ro.build.display.id; getprop ro.build.version.sdk; "+
      "echo FREEFORM; echo max_num=$(getprop hw_mc.freeform.max_num); getprop | grep -iE 'freeform|multi.?window|magic.?window|app.?clone|split.?screen' | head -n 80; "+
      "echo TASKS; dumpsys -t 8 activity activities | grep -iE 'Task\\{|RootTask|Stack #|windowingMode|freeform|visibleRequested|mVisible=|isVisible=|mResumedActivity|mBounds|mDisplayId|mMinimized' | head -n 180; "+
      "echo WINDOWS; dumpsys -t 8 window displays | grep -iE 'Task\\{|RootTask|Stack #|windowingMode|freeform|mBounds|visibleRequested|mVisible=|mMinimized|DisplayContent' | head -n 150; "+
      "echo MOUNTS; p=$(pidof system_server | awk '{print $1}'); echo system_server_pid=$p; readlink /proc/self/ns/mnt; if [ -n \"$p\" ]; then readlink /proc/$p/ns/mnt; grep -iE 'magic_window|hw_clone|freeform' /proc/$p/mountinfo | head -n 20; fi; "+
      "echo XML; for f in /hw_product/region_comm/china/xml/magic_window_application_list.xml /hw_product/region_comm/oversea/xml/magic_window_application_list.xml /product/region_comm/china/xml/magic_window_application_list.xml /product/region_comm/oversea/xml/magic_window_application_list.xml /system/emui/base/xml/magic_window_application_list.xml /system/etc/magic_window_application_list.xml /system/emui/base/xml/hw_clone_app_list.xml /hw_product/emui/base/xml/hw_clone_app_list.xml /product/emui/base/xml/hw_clone_app_list.xml /system/etc/hw_clone_app_list.xml; do if [ -r \"$f\" ]; then echo FILE=$f; wc -c < \"$f\"; head -n 15 \"$f\" | cut -c1-240; fi; done; "+
      "echo FRAMEWORK; for d in /system/framework /system_ext/framework /hw_product/framework /product/framework /vendor/framework; do if [ -d \"$d\" ]; then ls -1 \"$d\" | grep -iE 'services|multiwindow|magicwindow|freeform|hwframework|hwext' | head -n 40 | sed \"s|^|$d/|\"; fi; done; echo END";
    @Override public void onCreate(Bundle s){
      super.onCreate(s);
      LinearLayout p=new LinearLayout(this);p.setOrientation(1);p.setPadding(24,24,24,24);
      TextView t=new TextView(this);t.setTextSize(24);t.setText("Air2023 多窗诊断 D1");p.addView(t);
      TextView info=new TextView(this);info.setText("只读诊断，不是解锁补丁。不需要 Vector，保留 v0.4。点击采集才申请 Root。按钮有 8 秒准备时间，返回桌面恢复对应小窗状态。首次授权打断时重采。报告可能含包名和任务信息，请检查后发送。");p.addView(info);
      status=new TextView(this);p.addView(status);
      a=button(p,"采集 A：两个小窗",new View.OnClickListener(){public void onClick(View v){capture(false);}});
      b=button(p,"采集 B：尝试第三个小窗后",new View.OnClickListener(){public void onClick(View v){capture(true);}});
      copy=button(p,"复制报告",new View.OnClickListener(){public void onClick(View v){((ClipboardManager)getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("WindowLab",report()));}});
      button(p,"分享报告",new View.OnClickListener(){public void onClick(View v){if(busy)return;Intent i=new Intent(Intent.ACTION_SEND);i.setType("text/plain");i.putExtra(Intent.EXTRA_TEXT,report());startActivity(Intent.createChooser(i,"分享报告"));}});
      out=new TextView(this);out.setTextIsSelectable(true);out.setTextSize(12);p.addView(out);
      ScrollView scroll=new ScrollView(this);scroll.addView(p);setContentView(scroll);refresh();
    }
    private Button button(LinearLayout p,String s,View.OnClickListener l){Button x=new Button(this);x.setText(s);x.setOnClickListener(l);p.addView(x);return x;}
    private String report(){return "WindowLab D1\n--- A ---\n"+first+"\n--- B ---\n"+second;}
    private void refresh(){a.setEnabled(!busy);b.setEnabled(!busy&&!first.isEmpty());copy.setEnabled(!busy&&!first.isEmpty());out.setText(report());}
    private void capture(final boolean two){
      if(busy)return;busy=true;status.setText("8秒后采集，请返回桌面恢复场景");refresh();
      new Thread(new Runnable(){public void run(){
        String result;try{Thread.sleep(8000);result=collect();}catch(Throwable e){result="ERROR "+e;}
        final String value=result;runOnUiThread(new Runnable(){public void run(){if(two)second=value;else{first=value;second="";}busy=false;status.setText("采集完成：请确认 IDENTITY 下为 0 且报告包含 END");refresh();}});
      }}).start();
    }
    private static String collect() throws Exception {
      final Process p=new ProcessBuilder("su","-c",CMD).redirectErrorStream(true).start();
      final ByteArrayOutputStream buf=new ByteArrayOutputStream();
      Thread r=new Thread(new Runnable(){public void run(){try(InputStream in=p.getInputStream()){byte[] x=new byte[4096];int n;while((n=in.read(x))!=-1){synchronized(buf){if(buf.size()<120000)buf.write(x,0,Math.min(n,120000-buf.size()));}}}catch(IOException ignored){}}});r.start();
      boolean done=p.waitFor(35,TimeUnit.SECONDS);if(!done)p.destroyForcibly();r.join(1000);
      synchronized(buf){return "Exit="+(done?p.exitValue():"timeout")+"\n"+new String(buf.toByteArray(),StandardCharsets.UTF_8);}
    }
}