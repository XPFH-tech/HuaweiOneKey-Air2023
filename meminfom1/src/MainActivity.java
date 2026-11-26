package dev.air2023.meminfom1;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.*;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

public final class MainActivity extends Activity {
 private static final String KEY="ro.config.hide_meminfo";
 private static final String GUARD="if [ $(id -u) != 0 ] || [ $(getprop ro.product.model) != DBY2-W00 ] || [ \"$(getprop ro.build.display.id)\" != 'DBY2-W00 4.2.0.252(C00E100R3P5)' ]; then echo DEVICE_OR_ROOT_MISMATCH; exit 20; fi; ";
 private static final String PROBE="if [ ! -x /system/bin/magisk ] || [ $(/system/bin/magisk resetprop ro.product.model 2>/dev/null) != DBY2-W00 ]; then echo RESET_PROP_UNAVAILABLE; exit 21; fi; ";
 private static final String SNAP="snapshot() { if getprop | grep -qF '[ro.config.hide_meminfo]:'; then actual=$(getprop ro.config.hide_meminfo); if [ -z \"$actual\" ]; then actual=EMPTY; fi; else actual=ABSENT; fi; }; snapshot; ";
 private static final String STATE="snapshot; echo VALUE=$actual; echo BOOT=$(cat /proc/sys/kernel/random/boot_id); echo SYSTEM=$(pidof system_server); echo UI=$(pidof com.android.systemui); echo END_M1";
 private TextView summary,log;
 private Button inspect,enable,restore,copy;
 private String value="",boot="",system="",ui="";
 private boolean busy,ready;
 public void onCreate(Bundle b){super.onCreate(b);LinearLayout p=new LinearLayout(this);p.setOrientation(1);int pad=(int)(16*getResources().getDisplayMetrics().density);p.setPadding(pad,pad,pad,pad);
 TextView title=new TextView(this);title.setText("Air2023 多任务内存 M1");title.setTextSize(25);p.addView(title);
 TextView help=new TextView(this);help.setText("仅适配已验证的 DBY2-W00 4.2.0.252 临时 Root 环境。使用 Magisk resetprop 修改运行期属性 ro.config.hide_meminfo。仅改变最近任务内存显示，不增加 RAM。不写系统分区、不自动重启、不注入 Vector。先检查，再明确确认应用。");p.addView(help);
 summary=new TextView(this);summary.setTextSize(18);p.addView(summary);
 inspect=button(p,"检查当前属性",new View.OnClickListener(){public void onClick(View v){run(null);}});
 enable=button(p,"启用多任务运行内存显示",new View.OnClickListener(){public void onClick(View v){confirm("false");}});
 restore=button(p,"恢复修改前的属性",new View.OnClickListener(){public void onClick(View v){confirm(getPreferences(0).getString("previous",""));}});
 copy=button(p,"复制检查报告",new View.OnClickListener(){public void onClick(View v){((ClipboardManager)getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("M1",summary.getText()+"\n"+log.getText()));}});
 log=new TextView(this);log.setTextIsSelectable(true);p.addView(log);ScrollView scroll=new ScrollView(this);scroll.addView(p);setContentView(scroll);refresh();}
 private Button button(LinearLayout p,String s,View.OnClickListener l){Button b=new Button(this);b.setText(s);b.setOnClickListener(l);p.addView(b);return b;}
 private static boolean valid(String s){return "true".equals(s)||"false".equals(s)||"ABSENT".equals(s);}
 private static String line(String s,String key){for(String l:s.split("\n"))if(l.startsWith(key+"="))return l.substring(key.length()+1).trim();return "";}
 private void refresh(){SharedPreferences pref=getPreferences(0);boolean pending=pref.getBoolean("pending",false);boolean can=ready&&!busy&&!pending;inspect.setEnabled(!busy);enable.setEnabled(can&&!"false".equals(value));restore.setEnabled(can&&boot.equals(pref.getString("saved_boot",""))&&value.equals(pref.getString("last",""))&&valid(pref.getString("previous",""))&&!value.equals(pref.getString("previous","")));copy.setEnabled(!busy);
 String text=value.isEmpty()?"尚未检查":"当前属性："+value+"（ABSENT=原来不存在）";
 if("false".equals(value))text+="\n属性为显示状态，实际最近任务界面仍待验证。可能需重新初始化 SystemUI，应用不会自动执行。";
 if(pending)text+="\n存在未确认操作，请重新检查。";
 if(!ready&&!value.isEmpty())text+="\n设备或属性检查未通过，禁止修改。";
 summary.setText(text);}
 private void confirm(final String target){if(!ready||busy||!valid(target)||target.equals(value))return;new AlertDialog.Builder(this).setTitle("确认修改临时属性").setMessage("当前："+value+"\n目标："+target+"\n仅修改本次启动的属性，不自动重启。请关闭其他会修改此属性的模块。").setNegativeButton("取消",null).setPositiveButton("确认",new DialogInterface.OnClickListener(){public void onClick(DialogInterface d,int w){run(target);}}).show();}
 private void run(final String target){if(busy)return;final String before=value,oldBoot=boot,oldSystem=system,oldUi=ui;final SharedPreferences pref=getPreferences(0);String cmd=GUARD+PROBE+SNAP+STATE;
 if(target!=null){if(!ready||!valid(target)||!valid(before)||pref.getBoolean("pending",false))return;
 if(!pref.edit().putBoolean("pending",true).putString("pending_from",before).putString("pending_to",target).putString("pending_boot",oldBoot).commit()){log.setText("无法保存恢复记录，取消");return;}
 String action="ABSENT".equals(target)?"/system/bin/magisk resetprop -d "+KEY:"/system/bin/magisk resetprop -n "+KEY+" "+target;
 cmd=GUARD+PROBE+SNAP+"if [ \"$actual\" != '"+before+"' ] || [ \"$(cat /proc/sys/kernel/random/boot_id)\" != '"+oldBoot+"' ] || [ \"$(pidof system_server)\" != '"+oldSystem+"' ] || [ \"$(pidof com.android.systemui)\" != '"+oldUi+"' ]; then echo STALE_CHECK; exit 22; fi; "+action+" || exit 23; snapshot; if [ \"$actual\" != '"+target+"' ]; then echo VERIFY_FAILED; exit 24; fi; echo APPLIED_OK; "+STATE;
 }
 final String command=cmd;busy=true;refresh();log.setText("执行中");new Thread(new Runnable(){public void run(){String s;try{s=exec(command);}catch(Exception e){s="ERROR "+e;}final String out=s;runOnUiThread(new Runnable(){public void run(){if(isFinishing()||isDestroyed())return;value=line(out,"VALUE");boot=line(out,"BOOT");system=line(out,"SYSTEM");ui=line(out,"UI");
 ready=out.startsWith("Exit=0\n")&&out.contains("\nEND_M1")&&valid(value)&&boot.matches("[0-9a-fA-F-]{36}")&&system.matches("[0-9]+")&&ui.matches("[0-9]+");
 if(ready&&pref.getBoolean("pending",false)){String pb=pref.getString("pending_boot",""),from=pref.getString("pending_from",""),to=pref.getString("pending_to","");if(!boot.equals(pb)){pref.edit().putBoolean("pending",false).remove("last").remove("previous").remove("saved_boot").commit();}else if(value.equals(to)&&valid(from)){pref.edit().putBoolean("pending",false).putString("previous",from).putString("last",to).putString("saved_boot",boot).commit();}else if(value.equals(from)){pref.edit().putBoolean("pending",false).commit();}}
 busy=false;log.setText(out);refresh();}});}}).start();
 }
 private static String exec(String cmd)throws Exception{final Process p=new ProcessBuilder("su","-c",cmd).redirectErrorStream(true).start();final ByteArrayOutputStream buf=new ByteArrayOutputStream();Thread reader=new Thread(new Runnable(){public void run(){try(InputStream in=p.getInputStream()){byte[] a=new byte[2048];int n;while((n=in.read(a))!=-1){synchronized(buf){if(buf.size()<32000)buf.write(a,0,Math.min(n,32000-buf.size()));}}}catch(IOException e){}}});reader.start();boolean done=p.waitFor(20,TimeUnit.SECONDS);if(!done)p.destroyForcibly();reader.join(1000);synchronized(buf){return "Exit="+(done?p.exitValue():"timeout")+"\n"+new String(buf.toByteArray(),StandardCharsets.UTF_8);}}
}
