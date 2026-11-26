package dev.air2023.windowlab2r1;
import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.content.*;
import android.widget.*;
import android.view.View;
import java.io.*;
import java.util.concurrent.TimeUnit;
import java.nio.charset.StandardCharsets;

public final class MainActivity extends Activity {
  private TextView status; private Button inspect,apply,restore,copy;
  private boolean busy;
  private static final String KEY="hw_mc.freeform.max_num";
  private static final String CHECK="if [ \"$(id -u)\" != 0 ] || [ \"$(getprop ro.product.model)\" != DBY2-W00 ] || [ \"$(getprop ro.build.display.id)\" != 'DBY2-W00 4.2.0.252(C00E100R3P5)' ]; then echo DEVICE_OR_ROOT_MISMATCH; exit 20; fi; ";
  private static final String PROBE="rp=''; mode=''; resolve_rp() { for p in \"$(command -v resetprop 2>/dev/null)\" /data/adb/magisk/resetprop /system/bin/resetprop /sbin/resetprop /debug_ramdisk/resetprop; do if [ -n \"$p\" ] && [ -x \"$p\" ] && [ \"$(\"$p\" ro.product.model 2>/dev/null)\" = DBY2-W00 ]; then rp=$p; mode=direct; return 0; fi; done; for p in \"$(command -v magisk 2>/dev/null)\" /data/adb/magisk/magisk /data/adb/magisk/magisk64 /sbin/magisk /debug_ramdisk/magisk /system/bin/magisk; do if [ -n \"$p\" ] && [ -x \"$p\" ] && \"$p\" --list 2>/dev/null | grep -qx resetprop && [ \"$(\"$p\" resetprop ro.product.model 2>/dev/null)\" = DBY2-W00 ]; then rp=$p; mode=applet; return 0; fi; done; return 1; }; rp_exec() { if [ \"$mode\" = applet ]; then \"$rp\" resetprop \"$@\"; else \"$rp\" \"$@\"; fi; }; if resolve_rp; then echo RESET_PROP_READY; echo resetprop_mode=$mode; echo resetprop_path=$rp; else echo RESET_PROP_MISSING; echo PATH=$PATH; echo magisk_on_path=$(command -v magisk 2>/dev/null); echo su_on_path=$(command -v su 2>/dev/null); ls -ld /data/adb/magisk /sbin /debug_ramdisk 2>/dev/null; ls -l /data/adb/magisk/magisk* /sbin/magisk /debug_ramdisk/magisk 2>/dev/null; fi; ";
  private static final String RP=PROBE+"if [ -z \"$rp\" ]; then exit 21; fi; ";
  private static final String STATE="echo model=$(getprop ro.product.model); echo firmware=$(getprop ro.build.display.id); echo uid=$(id -u); echo system_server_pid=$(pidof system_server); echo boot_id=$(cat /proc/sys/kernel/random/boot_id); echo max_num=$(getprop hw_mc.freeform.max_num); echo property_entry; getprop | grep -F '[hw_mc.freeform.max_num]:'; echo tasks; dumpsys -t 8 activity activities | grep -E '^  RootTask .*hwMultiwindow-freeform|^    \\* Task\\{.*mode=hwMultiwindow-freeform' | head -n 60; echo END";
  private static final String SET=CHECK+RP+"if getprop | grep -qF '[hw_mc.freeform.max_num]:'; then echo EXISTING_PROPERTY_NOT_CHANGED; exit 22; fi; rp_exec -n "+KEY+" 4 || exit 23; if [ \"$(getprop "+KEY+")\" != 4 ]; then echo VERIFY_FAILED; exit 24; fi; echo APPLIED_4; "+STATE;
  private static final String UNSET=CHECK+RP+"if [ \"$(getprop "+KEY+")\" != 4 ]; then echo NOT_4_NO_CHANGE; exit 25; fi; rp_exec -d "+KEY+" || exit 26; if getprop | grep -qF '[hw_mc.freeform.max_num]:'; then echo RESTORE_VERIFY_FAILED; exit 27; fi; echo RESTORED_ABSENT; "+STATE;
  @Override public void onCreate(Bundle b){
    super.onCreate(b);
    LinearLayout p=new LinearLayout(this);p.setOrientation(1);int pad=(int)(16*getResources().getDisplayMetrics().density);p.setPadding(pad,pad,pad,pad);
    TextView title=new TextView(this);title.setText("Air2023 四窗实验 D2R1");title.setTextSize(24);p.addView(title);
    TextView info=new TextView(this);info.setText("实验仅修改非持久属性 hw_mc.freeform.max_num 为4。不会修改系统文件、安装Hook或自动重启。仅当属性原本不存在时才写入。先检查，关闭其他多窗解锁开关。保存工作后，可自行使用已知可用的软重启方式；软重启仍有丢失Root或系统异常风险。若重启后属性仍为4，测试三个小窗，随后检查并复制报告。恢复按钮只删除本实验记录且当前值为4的属性；缓存上限可能要在下次系统服务初始化时恢复。");p.addView(info);
    inspect=button(p,"检查属性与小窗任务",new View.OnClickListener(){public void onClick(View v){run(0);}});
    apply=button(p,"实验：将上限属性设为4",new View.OnClickListener(){public void onClick(View v){new AlertDialog.Builder(MainActivity.this).setTitle("仅修改临时属性").setMessage("确认当前设备无其他多窗解锁模块在修改此属性？不自动重启。").setNegativeButton("取消",null).setPositiveButton("确认",new DialogInterface.OnClickListener(){public void onClick(DialogInterface d,int w){run(1);}}).show();}});
    restore=button(p,"恢复实验前的空属性",new View.OnClickListener(){public void onClick(View v){new AlertDialog.Builder(MainActivity.this).setTitle("恢复").setMessage("仅在D2R1记录成功写入且当前值仍为4时删除。").setNegativeButton("取消",null).setPositiveButton("恢复",new DialogInterface.OnClickListener(){public void onClick(DialogInterface d,int w){run(2);}}).show();}});
    copy=button(p,"复制报告",new View.OnClickListener(){public void onClick(View v){((ClipboardManager)getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("WindowLab D2R1",status.getText()));}});
    status=new TextView(this);status.setTextIsSelectable(true);status.setText("尚未检查");p.addView(status);
    ScrollView scroll=new ScrollView(this);scroll.addView(p);setContentView(scroll);refresh();
  }
  private Button button(LinearLayout p,String s,View.OnClickListener l){Button x=new Button(this);x.setText(s);x.setOnClickListener(l);p.addView(x);return x;}
  private boolean owned(){return getPreferences(0).getBoolean("applied",false);}
  private void refresh(){inspect.setEnabled(!busy);apply.setEnabled(!busy&&!owned());restore.setEnabled(!busy&&owned());copy.setEnabled(!busy);}
  private void run(final int op){
    if(busy||(op==2&&!owned())||(op==1&&owned()))return;
    busy=true;refresh();status.setText("执行中");
    new Thread(new Runnable(){public void run(){
      String s;try{s=exec(op==0?CHECK+PROBE+STATE:op==1?SET:UNSET);}catch(Throwable e){s="ERROR "+e;}
      final String result=s;runOnUiThread(new Runnable(){public void run(){
        if(op==1&&result.contains("\nAPPLIED_4\n"))getPreferences(0).edit().putBoolean("applied",true).commit();
        if(op==2&&result.contains("\nRESTORED_ABSENT\n"))getPreferences(0).edit().putBoolean("applied",false).commit();
        status.setText(result);busy=false;refresh();
      }});
    }}).start();
  }
  private static String exec(String cmd)throws Exception{
    final Process p=new ProcessBuilder("su","-c",cmd).redirectErrorStream(true).start();
    final ByteArrayOutputStream buf=new ByteArrayOutputStream();
    Thread reader=new Thread(new Runnable(){public void run(){try(InputStream in=p.getInputStream()){byte[] b=new byte[4096];int n;while((n=in.read(b))!=-1){synchronized(buf){if(buf.size()<64000)buf.write(b,0,Math.min(n,64000-buf.size()));}}}catch(IOException ignored){}}});reader.start();
    boolean done=p.waitFor(35,TimeUnit.SECONDS);if(!done)p.destroyForcibly();reader.join(1000);
    synchronized(buf){return "Exit="+(done?p.exitValue():"timeout")+"\n"+new String(buf.toByteArray(),StandardCharsets.UTF_8);}
  }
}