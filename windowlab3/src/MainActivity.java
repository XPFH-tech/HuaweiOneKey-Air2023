package dev.air2023.windowlab3;
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
  private TextView status, summary, selectedText;
  private Button inspect,apply,max,rollback,stock,copy;
  private SeekBar slider;
  private boolean busy=false;
  private String observed=null, observedPid="", observedBoot="";
  private boolean ready=false;
  private int selected=64;
  private static final String KEY="hw_mc.freeform.max_num";
  private static final String CHECK="if [ \"$(id -u)\" != 0 ] || [ \"$(getprop ro.product.model)\" != DBY2-W00 ] || [ \"$(getprop ro.build.display.id)\" != 'DBY2-W00 4.2.0.252(C00E100R3P5)' ]; then echo DEVICE_OR_ROOT_MISMATCH; exit 20; fi; ";
  private static final String PROBE="rp=''; mode=''; resolve_rp() { for p in \"$(command -v resetprop 2>/dev/null)\" /data/adb/magisk/resetprop /system/bin/resetprop /sbin/resetprop /debug_ramdisk/resetprop; do if [ -n \"$p\" ] && [ -x \"$p\" ] && [ \"$(\"$p\" ro.product.model 2>/dev/null)\" = DBY2-W00 ]; then rp=$p; mode=direct; return 0; fi; done; for p in \"$(command -v magisk 2>/dev/null)\" /data/adb/magisk/magisk /data/adb/magisk/magisk64 /sbin/magisk /debug_ramdisk/magisk /system/bin/magisk; do if [ -n \"$p\" ] && [ -x \"$p\" ] && \"$p\" --list 2>/dev/null | grep -qx resetprop && [ \"$(\"$p\" resetprop ro.product.model 2>/dev/null)\" = DBY2-W00 ]; then rp=$p; mode=applet; return 0; fi; done; return 1; }; rp_exec() { if [ \"$mode\" = applet ]; then \"$rp\" resetprop \"$@\"; else \"$rp\" \"$@\"; fi; }; if resolve_rp; then echo RESET_PROP_READY; echo resetprop_mode=$mode; echo resetprop_path=$rp; else echo RESET_PROP_MISSING; echo PATH=$PATH; echo magisk_on_path=$(command -v magisk 2>/dev/null); echo su_on_path=$(command -v su 2>/dev/null); ls -ld /data/adb/magisk /sbin /debug_ramdisk 2>/dev/null; ls -l /data/adb/magisk/magisk* /sbin/magisk /debug_ramdisk/magisk 2>/dev/null; fi; ";
  private static final String RP=PROBE+"if [ -z \"$rp\" ]; then exit 21; fi; ";
  private static final String STATE="echo model=$(getprop ro.product.model); echo firmware=$(getprop ro.build.display.id); echo uid=$(id -u); echo system_server_pid=$(pidof system_server); echo boot_id=$(cat /proc/sys/kernel/random/boot_id); echo max_num=$(getprop hw_mc.freeform.max_num); echo property_entry; getprop | grep -F '[hw_mc.freeform.max_num]:'; echo tasks; dumpsys -t 8 activity activities | grep -E '^  RootTask .*hwMultiwindow-freeform|^    \\* Task\\{.*mode=hwMultiwindow-freeform' | head -n 60; echo END";

  // Read exact state. Distinguish a missing property from an existing empty one.
  private static final String OBS="snapshot() { if getprop | grep -qF '[hw_mc.freeform.max_num]:'; then actual=$(getprop hw_mc.freeform.max_num); if [ -z \"$actual\" ]; then actual=EMPTY; fi; else actual=ABSENT; fi; }; snapshot; echo OBSERVED=$actual; ";
  private static final String CHECK_STATE=CHECK+PROBE+OBS+STATE;
  @Override public void onCreate(Bundle saved){
    super.onCreate(saved);
    LinearLayout p=new LinearLayout(this);p.setOrientation(1);int pad=(int)(16*getResources().getDisplayMetrics().density);p.setPadding(pad,pad,pad,pad);
    TextView title=new TextView(this);title.setText("Air2023 多窗上限 D3");title.setTextSize(24);p.addView(title);
    TextView info=new TextView(this);info.setText("四窗已真机验证。64 是原版解锁值，不代表硬件能同时运行64个窗口。仅修改临时属性，不写系统分区、不安装Hook、不自动重启。先检查；设置后如需软重启，请保存工作并关闭多余窗口。完整重启可能清除属性和临时Root。");p.addView(info);
    summary=new TextView(this);summary.setTextSize(17);p.addView(summary);
    inspect=button(p,"检查当前属性与系统进程",new View.OnClickListener(){public void onClick(View v){run(0,null);}});
    selectedText=new TextView(this);p.addView(selectedText);
    slider=new SeekBar(this);slider.setMax(62);slider.setProgress(62);p.addView(slider);
    slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar b,int value,boolean user){selected=value+2;selectedText.setText("自定义上限："+selected);}public void onStartTrackingTouch(SeekBar b){}public void onStopTrackingTouch(SeekBar b){}});
    selectedText.setText("自定义上限：64");
    max=button(p,"最大解锁：64",new View.OnClickListener(){public void onClick(View v){confirm("64","将当前属性改为64。仅为配置上限，不保证64窗同时可用。");}});
    apply=button(p,"应用自定义上限",new View.OnClickListener(){public void onClick(View v){confirm(String.valueOf(selected),"将当前属性改为"+selected+"。");}});
    rollback=button(p,"撤销最近一次修改",new View.OnClickListener(){public void onClick(View v){String prev=getPreferences(0).getString("previous","");if(valid(prev))confirm(prev,"恢复最近一次修改之前的属性："+prev+"。");}});
    stock=button(p,"恢复原厂默认（删除属性）",new View.OnClickListener(){public void onClick(View v){confirm("ABSENT","D1 已确认此固件实验前属性不存在。删除该属性，恢复默认逻辑；需要系统框架重新初始化。");}});
    copy=button(p,"复制报告",new View.OnClickListener(){public void onClick(View v){((ClipboardManager)getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("WindowLab D3",summary.getText()+"\n"+status.getText()));}});
    status=new TextView(this);status.setTextIsSelectable(true);status.setText("尚未检查");p.addView(status);
    ScrollView scroll=new ScrollView(this);scroll.addView(p);setContentView(scroll);refresh();
  }
  private static boolean valid(String value){
    if("ABSENT".equals(value))return true;
    if(value==null||!value.matches("[0-9]{1,2}"))return false;
    int n=Integer.parseInt(value);return n>=2&&n<=64&&String.valueOf(n).equals(value);
  }
  private Button button(LinearLayout p,String s,View.OnClickListener l){Button x=new Button(this);x.setText(s);x.setOnClickListener(l);p.addView(x);return x;}
  private void confirm(final String target,String message){
    if(busy||!ready||!valid(observed)||!valid(target))return;
    if(target.equals(observed)){Toast.makeText(this,"当前已是该属性值",Toast.LENGTH_SHORT).show();return;}
    new AlertDialog.Builder(this).setTitle("确认修改临时属性").setMessage(message+"\n当前："+observed+"\n请确认 D2R1 或其他模块不会同时修改此属性。不自动软重启。").setNegativeButton("取消",null).setPositiveButton("确认",new DialogInterface.OnClickListener(){public void onClick(DialogInterface d,int w){run(1,target);}}).show();
  }
  private static String line(String s,String key){for(String l:s.split("\n"))if(l.startsWith(key+"="))return l.substring(key.length()+1).trim();return "";}
  private void refresh(){
    boolean can=!busy&&ready&&valid(observed);
    inspect.setEnabled(!busy);apply.setEnabled(can);max.setEnabled(can);slider.setEnabled(can);
    stock.setEnabled(can&&!"ABSENT".equals(observed));copy.setEnabled(!busy);
    String previous=getPreferences(0).getString("previous",""),last=getPreferences(0).getString("last","");
    rollback.setEnabled(can&&valid(previous)&&last.equals(observed)&&!previous.equals(observed));
    String msg=observed==null?"先检查设备和属性":"当前属性："+observed+"（ABSENT=原厂默认）";
    String pid=getPreferences(0).getString("change_pid",""),boot=getPreferences(0).getString("change_boot","");
    if(!last.isEmpty()&&last.equals(observed)&&!pid.isEmpty()&&!observedPid.isEmpty()&&!boot.isEmpty()&&!observedBoot.isEmpty()){
      if(pid.equals(observedPid)&&boot.equals(observedBoot))msg+="\n属性已写入，系统进程尚未重新初始化；可能需要软重启。";
      else if(boot.equals(observedBoot))msg+="\n系统进程已变化，属性仍保留；请验证实际小窗数量。";
      else msg+="\n启动标识变化，当前属性已重新检查；实际效果待验证。";
    }
    if(!ready&&observed!=null)msg+="\n未确认可用执行入口或检查未完整结束，禁止写入。";
    if(valid(previous))msg+="\n上次修改前："+previous;
    if(getPreferences(0).getBoolean("pending",false))msg+="\n存在未确认操作，请重新检查；不要盲目恢复。";
    summary.setText(msg);
  }
  private void run(final int op,final String target){
    if(busy)return;
    final String expected=observed;
    final String pid=observedPid,boot=observedBoot;
    if(op==1&&(!ready||!valid(expected)||!valid(target)))return;
    String command=CHECK_STATE;
    if(op==1){
      // Only constants or strictly validated canonical integers can enter the shell.
      String action="ABSENT".equals(target)?"rp_exec -d "+KEY:"rp_exec -n "+KEY+" "+target;
      command=CHECK+RP+OBS+"if [ \"$actual\" != '"+expected+"' ] || [ \"$(pidof system_server)\" != '"+pid+"' ] || [ \"$(cat /proc/sys/kernel/random/boot_id)\" != '"+boot+"' ]; then echo STALE_CHECK; exit 22; fi; "+action+" || exit 23; snapshot; if [ \"$actual\" != '"+target+"' ]; then echo VERIFY_FAILED; exit 24; fi; echo APPLIED_OK; echo OBSERVED=$actual; "+STATE;
      if(!getPreferences(0).edit().putBoolean("pending",true).putString("pending_from",expected).putString("pending_to",target).putString("pending_pid",pid).putString("pending_boot",boot).commit()){status.setText("无法保存恢复记录，操作取消");return;}
    }
    final String cmd=command;busy=true;refresh();status.setText("执行中");
    new Thread(new Runnable(){public void run(){
      String s;try{s=exec(cmd);}catch(Throwable e){s="ERROR "+e;}
      final String result=s;runOnUiThread(new Runnable(){public void run(){
        String read=line(result,"OBSERVED"); // write has two OBSERVED lines, use verified final one
        for(String l:result.split("\n"))if(l.startsWith("OBSERVED="))read=l.substring(9).trim();
        observed=read.isEmpty()?null:read;
        observedPid=line(result,"system_server_pid");observedBoot=line(result,"boot_id");
        boolean ids=observedPid.matches("[0-9]+")&&observedBoot.matches("[0-9a-fA-F-]{36}");
        ready=result.startsWith("Exit=0\n")&&result.contains("\nRESET_PROP_READY\n")&&result.contains("\nEND")&&ids&&valid(observed);
        SharedPreferences prefs=getPreferences(0);
        if(op==1&&result.contains("\nAPPLIED_OK\n")&&target.equals(observed)){
          prefs.edit().putString("previous",expected).putString("last",target).putString("change_pid",pid).putString("change_boot",boot).putBoolean("pending",false).commit();
        }else if(op==0&&ready&&prefs.getBoolean("pending",false)){
          String from=prefs.getString("pending_from",""),to=prefs.getString("pending_to","");
          if(observed.equals(to)){prefs.edit().putString("previous",from).putString("last",to).putString("change_pid",prefs.getString("pending_pid","")).putString("change_boot",prefs.getString("pending_boot","")).putBoolean("pending",false).commit();}
          else if(observed.equals(from))prefs.edit().putBoolean("pending",false).commit();
        }
        if(prefs.getBoolean("pending",false))ready=false;
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